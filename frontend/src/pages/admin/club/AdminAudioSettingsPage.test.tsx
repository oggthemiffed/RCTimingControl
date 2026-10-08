import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminAudioSettingsPage from './AdminAudioSettingsPage';

// Mock audioApi
vi.mock('@/lib/audioApi', () => ({
  getAdminAudioSettings: vi.fn(),
  saveAdminAudioSettings: vi.fn(),
  listVoices: vi.fn(),
}));

import * as audioApi from '@/lib/audioApi';

const defaultSettings = {
  announceCountdown: true,
  announceStagger: true,
  announceLapBeep: false,
  announceFinish: true,
  announceRunningOrder: false,
  runningOrderDepth: 3,
  defaultVoiceId: 'en_GB-alan-medium',
};

const voices = [
  { voiceId: 'en_GB-alan-medium', label: 'Alan (British)', isDefault: true },
  { voiceId: 'en_GB-jenny-medium', label: 'Jenny (British)', isDefault: false },
];

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('AdminAudioSettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(audioApi.getAdminAudioSettings).mockResolvedValue(defaultSettings as never);
    vi.mocked(audioApi.saveAdminAudioSettings).mockResolvedValue(defaultSettings as never);
    vi.mocked(audioApi.listVoices).mockResolvedValue(voices as never);
  });

  it('renders default announcement toggles', async () => {
    render(<AdminAudioSettingsPage />, { wrapper });

    await waitFor(() =>
      expect(screen.getByLabelText('Countdown intervals')).toBeInTheDocument(),
    );

    expect(screen.getByLabelText('Stagger car calls')).toBeInTheDocument();
    expect(screen.getByLabelText('Lap improvement beeps')).toBeInTheDocument();
    expect(screen.getByLabelText('Finish announcements')).toBeInTheDocument();
    expect(screen.getByLabelText('Running order')).toBeInTheDocument();
  });

  it('voice selector populated from /api/v1/audio/voices', async () => {
    render(<AdminAudioSettingsPage />, { wrapper });

    await waitFor(() =>
      expect(screen.getByRole('combobox', { name: /default voice/i })).toBeInTheDocument(),
    );

    expect(vi.mocked(audioApi.listVoices)).toHaveBeenCalledOnce();
  });

  it('has no profanity blocklist (#30)', async () => {
    render(<AdminAudioSettingsPage />, { wrapper });

    await waitFor(() =>
      expect(screen.getByLabelText('Countdown intervals')).toBeInTheDocument(),
    );

    expect(screen.queryByText(/blocklist/i)).not.toBeInTheDocument();
  });
});
