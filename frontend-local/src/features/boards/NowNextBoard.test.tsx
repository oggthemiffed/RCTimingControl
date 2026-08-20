import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import NowNextBoard from './NowNextBoard';
import type { NowNextDto, ResultsDto, ScheduleEntryDto } from '@/lib/api';

vi.mock('@/lib/api', () => ({
  getNowNext: vi.fn(),
  getResults: vi.fn(),
}));

// Component tests mock the STOMP module entirely, per its own documented convention — there is
// no jsdom WebSocket polyfill in this project (see RaceControl.test.tsx for the same pattern).
vi.mock('@/lib/stomp', () => ({
  useRaceChannel: vi.fn(),
}));

import { getNowNext, getResults } from '@/lib/api';
import { useRaceChannel } from '@/lib/stomp';

const runningRace: ScheduleEntryDto = {
  id: 1,
  cloudRaceId: 101,
  roundNumber: 1,
  heatNumber: 1,
  sequence: 1,
  className: 'Touring Stock',
  finalLetter: null,
  scheduledStartAt: null,
  status: 'RUNNING',
};

const nextRace: ScheduleEntryDto = {
  id: 2,
  cloudRaceId: 102,
  roundNumber: 1,
  heatNumber: 2,
  sequence: 2,
  className: 'Touring Stock',
  finalLetter: null,
  scheduledStartAt: null,
  status: 'PENDING',
};

const lastCompletedRace: ScheduleEntryDto = {
  id: 3,
  cloudRaceId: 100,
  roundNumber: 1,
  heatNumber: 0,
  sequence: 0,
  className: '1/10 Buggy',
  finalLetter: 'A',
  scheduledStartAt: null,
  status: 'FINISHED',
};

function nowNext(overrides: Partial<NowNextDto>): NowNextDto {
  return {
    currentRace: null,
    nextRace: null,
    lastCompletedRace: null,
    ...overrides,
  };
}

const resultsDto: ResultsDto = {
  race: lastCompletedRace,
  results: [
    { entryId: 1, racerName: 'Jane Doe', transponderNumber: '1234567', position: 1, lapsCompleted: 10, bestLapMs: 19000 },
  ],
};

beforeEach(() => {
  vi.mocked(getNowNext).mockReset();
  vi.mocked(getResults).mockReset();
  vi.mocked(useRaceChannel).mockReset();
  vi.mocked(getResults).mockResolvedValue({ race: null, results: [] });
});

describe('NowNextBoard — running heat', () => {
  it('subscribes to the current race and renders live rows from the STOMP channel', async () => {
    vi.mocked(getNowNext).mockResolvedValue(nowNext({ currentRace: runningRace }));
    vi.mocked(useRaceChannel).mockReturnValue({
      rows: [
        {
          entryId: 1,
          driverName: 'Jane Doe',
          position: 1,
          lapsCompleted: 5,
          lastPassingTimeMs: 1000,
          lastLapMs: 20000,
          bestLapMs: 19000,
          avgLapMs: 20500,
          overallFastestLapMs: 18500,
          lapsDown: 0,
          intervalLapsDown: 0,
          gapToLeaderMs: null,
          gapToAheadMs: null,
        },
      ],
      stateEvent: null,
      marshalEvent: null,
      connected: true,
    });

    render(<NowNextBoard />);

    await screen.findByText(/Round 1 · Heat 1 — Touring Stock/);
    expect(useRaceChannel).toHaveBeenCalledWith(1);
    expect(await screen.findByText('Jane Doe')).toBeInTheDocument();
    expect(screen.queryByText(/Reconnecting to local server/i)).toBeNull();
  });

  it('shows the reconnecting indicator when the STOMP channel is disconnected', async () => {
    vi.mocked(getNowNext).mockResolvedValue(nowNext({ currentRace: runningRace }));
    vi.mocked(useRaceChannel).mockReturnValue({
      rows: null,
      stateEvent: null,
      marshalEvent: null,
      connected: false,
    });

    render(<NowNextBoard />);

    await screen.findByText(/Round 1 · Heat 1 — Touring Stock/);
    expect(await screen.findByText(/Reconnecting to local server/i)).toBeInTheDocument();
  });
});

describe('NowNextBoard — idle states', () => {
  beforeEach(() => {
    vi.mocked(useRaceChannel).mockReturnValue({
      rows: null,
      stateEvent: null,
      marshalEvent: null,
      connected: true,
    });
  });

  it('before first heat: shows only next-up, no results section', async () => {
    vi.mocked(getNowNext).mockResolvedValue(nowNext({ nextRace }));

    render(<NowNextBoard />);

    await screen.findByText(/Round 1 · Heat 2 — Touring Stock/);
    expect(screen.queryByText(/Last completed/i)).toBeNull();
    expect(getResults).not.toHaveBeenCalled();
  });

  it('between rounds: shows next-up and last-completed results simultaneously', async () => {
    vi.mocked(getNowNext).mockResolvedValue(nowNext({ nextRace, lastCompletedRace }));
    vi.mocked(getResults).mockResolvedValue(resultsDto);

    render(<NowNextBoard />);

    await screen.findByText(/Round 1 · Heat 2 — Touring Stock/);
    await screen.findByText(/Last completed/i);
    expect(await screen.findByText('Jane Doe')).toBeInTheDocument();
  });

  it('after last race: shows only last-completed results, no next-up section', async () => {
    vi.mocked(getNowNext).mockResolvedValue(nowNext({ lastCompletedRace }));
    vi.mocked(getResults).mockResolvedValue(resultsDto);

    render(<NowNextBoard />);

    await screen.findByText(/Last completed/i);
    expect(await screen.findByText('Jane Doe')).toBeInTheDocument();
    expect(screen.queryByText('Next up')).toBeNull();
  });

  it('nothing scheduled or finished: shows a no-data message', async () => {
    vi.mocked(getNowNext).mockResolvedValue(nowNext({}));

    render(<NowNextBoard />);

    expect(await screen.findByText(/no race data yet/i)).toBeInTheDocument();
    expect(getResults).not.toHaveBeenCalled();
  });
});
