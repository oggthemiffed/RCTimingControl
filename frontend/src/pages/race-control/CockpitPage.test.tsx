import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import CockpitPage from './CockpitPage';

const mockUser = vi.fn();
vi.mock('@/hooks/useAuth', () => ({ useAuth: () => ({ user: mockUser() }) }));
vi.mock('@/context/HelpContext', () => ({ useHelp: () => ({ setHelpContent: vi.fn() }) }));

const mockRunOrder = vi.fn();
vi.mock('@/hooks/race-control/useRunOrder', () => ({ useRunOrder: () => mockRunOrder() }));
vi.mock('@/hooks/race-control/useRaceStateMutations', () => ({ useRaceStateMutations: () => ({}) }));
vi.mock('@/hooks/race-control/useStomp', () => ({ useStomp: () => ({ data: null }) }));
vi.mock('@/hooks/race-control/useLiveTiming', () => ({ useLiveTiming: () => ({ rows: [] }) }));
vi.mock('@/hooks/race-control/useAnnouncements', () => ({
  useAnnouncements: () => ({ playBeep: vi.fn(), setClipMap: vi.fn() }),
}));
vi.mock('@/hooks/race-control/usePreRaceReadiness', () => ({ usePreRaceReadiness: () => ({ data: undefined }) }));
vi.mock('@/hooks/race-control/usePregeneratedClips', () => ({ usePregeneratedClips: () => undefined }));
vi.mock('@/lib/audioApi', () => ({ getAudioSettings: vi.fn().mockResolvedValue({ data: null }) }));
vi.mock('./panels/AudioSettingsPanel', () => ({ AudioSettingsPanel: () => null }));

vi.mock('@/lib/adminApi', () => ({
  adminApi: {
    getEvent: vi.fn().mockResolvedValue({
      id: 7,
      classes: [{ id: 11, racingClassId: 3, configSnapshot: { type: 'TIMED' } }],
    }),
    listRacingClasses: vi.fn().mockResolvedValue([{ id: 3, name: '13.5 Touring' }]),
    generateRounds: vi.fn().mockResolvedValue(undefined),
  },
}));

import { adminApi } from '@/lib/adminApi';

function renderCockpit() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={['/race-control/event/7']}>
        <Routes>
          <Route path="/race-control/event/:eventId" element={<CockpitPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('CockpitPage with no races', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockRunOrder.mockReturnValue({ data: [], isLoading: false });
  });

  it('lets a race director generate the rounds', async () => {
    mockUser.mockReturnValue({ roles: ['RACE_DIRECTOR'] });
    renderCockpit();

    expect(screen.getByText('No races yet')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Generate Rounds' }));

    await screen.findByText('13.5 Touring');
    fireEvent.click(screen.getByRole('button', { name: 'Generate Rounds' }));

    await waitFor(() => expect(adminApi.generateRounds).toHaveBeenCalledTimes(1));
    expect(vi.mocked(adminApi.generateRounds).mock.calls[0][0]).toBe(7);
    expect(vi.mocked(adminApi.generateRounds).mock.calls[0][1].classFinalsConfigs).toEqual([
      expect.objectContaining({ eventClassId: 11 }),
    ]);
  });

  it('tells a referee someone else has to generate them', () => {
    mockUser.mockReturnValue({ roles: ['REFEREE'] });
    renderCockpit();

    expect(screen.getByText(/race director or admin needs to generate/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Generate Rounds' })).not.toBeInTheDocument();
  });
});
