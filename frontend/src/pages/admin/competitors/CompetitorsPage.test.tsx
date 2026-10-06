import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import CompetitorsPage from './CompetitorsPage';
import { adminApi } from '@/lib/adminApi';

vi.mock('@/lib/adminApi', () => ({
  adminApi: { competitors: { list: vi.fn(), setSpokenName: vi.fn(), previewSpeech: vi.fn() } },
}));
const mockUser = vi.fn();
vi.mock('@/hooks/useAuth', () => ({ useAuth: () => ({ user: mockUser() }) }));
vi.mock('@/context/HelpContext', () => ({ useHelp: () => ({ setHelpContent: vi.fn() }) }));

const api = vi.mocked(adminApi, true);

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <CompetitorsPage />
    </QueryClientProvider>,
  );
}

describe('CompetitorsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    mockUser.mockReturnValue({ roles: ['ADMIN'] });
  });

  it('lists competitors and filters them by name, BRCA number or club', async () => {
    api.competitors.list.mockResolvedValue([
      { id: 1, displayName: 'Ada Lovelace', brcaNumber: '12345', homeClub: 'Analytical RC', spokenName: null },
      { id: 2, displayName: 'Grace Hopper', brcaNumber: null, homeClub: 'Compiler Club', spokenName: null },
    ]);
    renderPage();

    expect(await screen.findByText('Ada Lovelace')).toBeInTheDocument();
    expect(screen.getByText('BRCA 12345 · Analytical RC')).toBeInTheDocument();
    expect(screen.getByText('Grace Hopper')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Search competitors'), { target: { value: 'compiler' } });
    expect(screen.queryByText('Ada Lovelace')).not.toBeInTheDocument();
    expect(screen.getByText('Grace Hopper')).toBeInTheDocument();
  });

  it('explains where competitors come from when there are none', async () => {
    api.competitors.list.mockResolvedValue([]);
    renderPage();

    expect(await screen.findByText('No competitors yet')).toBeInTheDocument();
  });

  describe('say as', () => {
    const siobhan = { id: 7, displayName: 'Siobhan Keane', brcaNumber: null, homeClub: null, spokenName: null };

    const play = vi.fn().mockResolvedValue(undefined);
    beforeEach(() => {
      play.mockClear();
      vi.stubGlobal('Audio', class { addEventListener() {} play = play; });
      vi.stubGlobal('URL', { ...URL, createObjectURL: () => 'blob:x', revokeObjectURL: () => {} });
    });

    it('shows a spoken name, or that the name is said as written', async () => {
      api.competitors.list.mockResolvedValue([
        siobhan,
        { id: 8, displayName: 'Sam Ito', brcaNumber: null, homeClub: null, spokenName: 'Sam Ee-toe' },
      ]);
      renderPage();

      expect(await screen.findByText('Said as written')).toBeInTheDocument();
      expect(screen.getByText('Sam Ee-toe')).toBeInTheDocument();
    });

    it('does not offer Say as… to a race director or referee', async () => {
      mockUser.mockReturnValue({ roles: ['RACE_DIRECTOR', 'REFEREE'] });
      api.competitors.list.mockResolvedValue([{ ...siobhan, spokenName: 'Shiv-awn Keen' }]);
      renderPage();

      expect(await screen.findByText('Shiv-awn Keen')).toBeInTheDocument();
      expect(screen.queryByLabelText('Edit how Siobhan Keane is said')).not.toBeInTheDocument();
    });

    it('saves what the admin types', async () => {
      api.competitors.list.mockResolvedValue([siobhan]);
      api.competitors.setSpokenName.mockResolvedValue({ ...siobhan, spokenName: 'Shiv-awn Keen' });
      renderPage();

      fireEvent.click(await screen.findByLabelText('Edit how Siobhan Keane is said'));
      fireEvent.change(screen.getByLabelText('Say Siobhan Keane as'), { target: { value: 'Shiv-awn Keen' } });
      fireEvent.click(screen.getByRole('button', { name: 'Save' }));

      await waitFor(() => expect(api.competitors.setSpokenName).toHaveBeenCalledWith(7, 'Shiv-awn Keen'));
    });

    it('clears a spoken name by saving it empty', async () => {
      api.competitors.list.mockResolvedValue([{ ...siobhan, spokenName: 'Shiv-awn Keen' }]);
      api.competitors.setSpokenName.mockResolvedValue(siobhan);
      renderPage();

      fireEvent.click(await screen.findByLabelText('Edit how Siobhan Keane is said'));
      fireEvent.click(screen.getByRole('button', { name: 'Clear' }));

      await waitFor(() => expect(api.competitors.setSpokenName).toHaveBeenCalledWith(7, ''));
    });

    it('plays the typed text in the announcer voice', async () => {
      api.competitors.list.mockResolvedValue([siobhan]);
      api.competitors.previewSpeech.mockResolvedValue(new Blob(['RIFF']));
      renderPage();

      fireEvent.click(await screen.findByLabelText('Edit how Siobhan Keane is said'));
      fireEvent.change(screen.getByLabelText('Say Siobhan Keane as'), { target: { value: 'Shiv-awn Keen' } });
      fireEvent.click(screen.getByLabelText('Play Siobhan Keane'));

      await waitFor(() => expect(api.competitors.previewSpeech).toHaveBeenCalledWith('Shiv-awn Keen'));
      await waitFor(() => expect(play).toHaveBeenCalled());
    });

    it('plays the display name when nothing is typed, and falls back to the browser voice', async () => {
      const speakFn = vi.fn();
      vi.stubGlobal('speechSynthesis', { speak: speakFn });
      vi.stubGlobal('SpeechSynthesisUtterance', class { text: string; constructor(text: string) { this.text = text; } });
      api.competitors.list.mockResolvedValue([siobhan]);
      api.competitors.previewSpeech.mockRejectedValue(new Error('503'));
      renderPage();

      fireEvent.click(await screen.findByLabelText('Edit how Siobhan Keane is said'));
      fireEvent.click(screen.getByLabelText('Play Siobhan Keane'));

      await waitFor(() => expect(api.competitors.previewSpeech).toHaveBeenCalledWith('Siobhan Keane'));
      await waitFor(() => expect(speakFn).toHaveBeenCalled());
      expect(await screen.findByText(/used the browser voice/)).toBeInTheDocument();
    });
  });
});
