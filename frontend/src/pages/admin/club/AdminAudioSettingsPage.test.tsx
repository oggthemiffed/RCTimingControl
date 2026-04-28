import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminAudioSettingsPage from './AdminAudioSettingsPage';

// Mock audioApi
vi.mock('@/lib/audioApi', () => ({
  getAdminAudioSettings: vi.fn(),
  saveAdminAudioSettings: vi.fn(),
  listVoices: vi.fn(),
  getBlocklist: vi.fn(),
  addBlocklistTerm: vi.fn(),
  removeBlocklistTerm: vi.fn(),
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

const blocklist = [
  { id: 1, word: 'badword', addedAt: '2024-01-01T00:00:00Z' },
];

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('AdminAudioSettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(audioApi.getAdminAudioSettings).mockResolvedValue({ data: defaultSettings } as never);
    vi.mocked(audioApi.saveAdminAudioSettings).mockResolvedValue({ data: defaultSettings } as never);
    vi.mocked(audioApi.listVoices).mockResolvedValue({ data: voices } as never);
    vi.mocked(audioApi.getBlocklist).mockResolvedValue({ data: blocklist } as never);
    vi.mocked(audioApi.addBlocklistTerm).mockResolvedValue({ data: { id: 2, word: 'test', addedAt: '2024-01-01T00:00:00Z' } } as never);
    vi.mocked(audioApi.removeBlocklistTerm).mockResolvedValue({ data: undefined } as never);
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

  it('profanity blocklist displays custom terms', async () => {
    render(<AdminAudioSettingsPage />, { wrapper });

    await waitFor(() =>
      expect(screen.getByText('badword')).toBeInTheDocument(),
    );
  });

  it('add term fires addBlocklistTerm', async () => {
    render(<AdminAudioSettingsPage />, { wrapper });

    await waitFor(() =>
      expect(screen.getByLabelText('New blocklist word')).toBeInTheDocument(),
    );

    fireEvent.change(screen.getByLabelText('New blocklist word'), {
      target: { value: 'newbadword' },
    });
    fireEvent.submit(screen.getByLabelText('New blocklist word').closest('form')!);

    await waitFor(() =>
      expect(vi.mocked(audioApi.addBlocklistTerm)).toHaveBeenCalledWith('newbadword'),
    );
  });

  it('remove term fires removeBlocklistTerm', async () => {
    render(<AdminAudioSettingsPage />, { wrapper });

    await waitFor(() =>
      expect(screen.getByLabelText(/remove "badword"/i)).toBeInTheDocument(),
    );

    fireEvent.click(screen.getByLabelText(/remove "badword"/i));

    await waitFor(() =>
      expect(vi.mocked(audioApi.removeBlocklistTerm)).toHaveBeenCalledWith(1),
    );
  });
});
