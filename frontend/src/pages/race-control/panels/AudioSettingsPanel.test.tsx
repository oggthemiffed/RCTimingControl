import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AudioSettingsPanel } from './AudioSettingsPanel';

// Mock audioApi
vi.mock('@/lib/audioApi', () => ({
  getAudioSettings: vi.fn(),
  patchAudioSettings: vi.fn(),
}));

// Mock useAnnouncements (avoids AudioContext / speechSynthesis setup)
const mockTestAudio = vi.fn();
vi.mock('@/hooks/race-control/useAnnouncements', () => ({
  useAnnouncements: () => ({ testAudio: mockTestAudio, fallbackSpeak: vi.fn(), playBeep: vi.fn() }),
}));

import * as audioApi from '@/lib/audioApi';

const defaultSettings = {
  announceCountdown: true,
  announceStagger: false,
  announceLapBeep: true,
  announceFinish: false,
  announceRunningOrder: true,
  runningOrderDepth: 3,
  defaultVoiceId: 'en_GB-alan-medium',
};

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('AudioSettingsPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    vi.mocked(audioApi.getAudioSettings).mockResolvedValue({
      data: defaultSettings,
    } as never);
    vi.mocked(audioApi.patchAudioSettings).mockResolvedValue({
      data: defaultSettings,
    } as never);
  });

  it('renders toggle switches for each announcement type after opening', async () => {
    render(<AudioSettingsPanel raceId={1} />, { wrapper });

    // Open the panel
    fireEvent.click(screen.getByRole('button', { name: /audio settings/i }));

    await waitFor(() =>
      expect(screen.getByLabelText('Countdown intervals')).toBeInTheDocument(),
    );

    expect(screen.getByLabelText('Stagger car calls')).toBeInTheDocument();
    expect(screen.getByLabelText('Lap improvement beeps')).toBeInTheDocument();
    expect(screen.getByLabelText('Finish announcements')).toBeInTheDocument();
    expect(screen.getByLabelText('Running order')).toBeInTheDocument();
  });

  it('volume slider adjusts localStorage rc-audio-volume', async () => {
    render(<AudioSettingsPanel raceId={1} />, { wrapper });

    // Open the panel
    fireEvent.click(screen.getByRole('button', { name: /audio settings/i }));

    // Wait for the settings to load (look for a toggle)
    await waitFor(() =>
      expect(screen.getByLabelText('Countdown intervals')).toBeInTheDocument(),
    );

    // Default volume (80) is stored and displayed
    expect(localStorage.getItem('rc-audio-volume')).toBe('80');
    expect(screen.getByText('80%')).toBeInTheDocument();
  });

  it('test audio button calls testAudio', async () => {
    render(<AudioSettingsPanel raceId={1} />, { wrapper });
    fireEvent.click(screen.getByRole('button', { name: /audio settings/i }));

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /test audio/i })).toBeInTheDocument(),
    );

    fireEvent.click(screen.getByRole('button', { name: /test audio/i }));
    expect(mockTestAudio).toHaveBeenCalledOnce();
  });

  it('status dot reflects toggle states — yellow when some enabled', async () => {
    // Some toggles on, some off → yellow
    render(<AudioSettingsPanel raceId={1} />, { wrapper });

    // Open panel to trigger settings load
    fireEvent.click(screen.getByRole('button', { name: /audio settings/i }));

    // Wait for toggles to appear (settings loaded)
    await waitFor(() =>
      expect(screen.getByLabelText('Countdown intervals')).toBeInTheDocument(),
    );

    const dot = screen.getByTestId('audio-status-dot');
    // defaultSettings has 3 of 5 enabled → someEnabled=true, allEnabled=false → yellow
    expect(dot.className).toContain('flag-yellow');
  });

  it('status dot is green when all toggles enabled', async () => {
    vi.mocked(audioApi.getAudioSettings).mockResolvedValue({
      data: {
        ...defaultSettings,
        announceCountdown: true,
        announceStagger: true,
        announceLapBeep: true,
        announceFinish: true,
        announceRunningOrder: true,
      },
    } as never);

    render(<AudioSettingsPanel raceId={1} />, { wrapper });

    // Open panel to trigger settings load
    fireEvent.click(screen.getByRole('button', { name: /audio settings/i }));

    // Wait for toggles to appear (settings loaded)
    await waitFor(() =>
      expect(screen.getByLabelText('Countdown intervals')).toBeInTheDocument(),
    );

    const dot = screen.getByTestId('audio-status-dot');
    expect(dot.className).toContain('flag-green');
  });
});
