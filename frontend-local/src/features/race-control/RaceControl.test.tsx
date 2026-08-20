import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import RaceControl from './RaceControl';
import type {
  GridEntryDto,
  LiveTimingRowDto,
  ScheduleEntryDetailDto,
  ScheduleEntryDto,
} from '@/lib/api';

vi.mock('@/lib/api', () => ({
  listRaces: vi.fn(),
  getRaceDetail: vi.fn(),
  getLiveSnapshot: vi.fn(),
  transitionRace: vi.fn(),
  recordMarshalAdjustment: vi.fn(),
  advanceRound: vi.fn(),
}));

// Component tests mock the STOMP module entirely, per its own documented convention — there is
// no jsdom WebSocket polyfill in this project. Tests exercise the getLiveSnapshot() fallback
// path by always returning "no STOMP message yet".
vi.mock('@/lib/stomp', () => ({
  useRaceChannel: vi.fn(),
}));

import {
  listRaces,
  getRaceDetail,
  getLiveSnapshot,
  transitionRace,
  recordMarshalAdjustment,
  advanceRound,
} from '@/lib/api';
import { useRaceChannel } from '@/lib/stomp';

const raceA: ScheduleEntryDto = {
  id: 1,
  cloudRaceId: 101,
  roundNumber: 1,
  heatNumber: 1,
  sequence: 1,
  className: 'Touring Stock',
  finalLetter: null,
  scheduledStartAt: null,
  status: 'PENDING',
};

const raceB: ScheduleEntryDto = {
  id: 2,
  cloudRaceId: 102,
  roundNumber: 1,
  heatNumber: 2,
  sequence: 2,
  className: 'Touring Stock',
  finalLetter: null,
  scheduledStartAt: null,
  status: 'STOPPED',
};

const gridA: GridEntryDto[] = [
  {
    cachedEntryId: 10,
    racerName: 'Jane Doe',
    transponderNumber: '1234567',
    carNumber: '5',
    gridPosition: 1,
    bumped: false,
  },
  {
    cachedEntryId: null,
    racerName: null,
    transponderNumber: null,
    carNumber: null,
    gridPosition: 2,
    bumped: true,
  },
];

const gridB: GridEntryDto[] = [
  {
    cachedEntryId: 10,
    racerName: 'Jane Doe',
    transponderNumber: '1234567',
    carNumber: '5',
    gridPosition: 1,
    bumped: false,
  },
  {
    cachedEntryId: 11,
    racerName: 'John Smith',
    transponderNumber: '7654321',
    carNumber: '9',
    gridPosition: 2,
    bumped: false,
  },
];

function liveRow(overrides: Partial<LiveTimingRowDto>): LiveTimingRowDto {
  return {
    entryId: 10,
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
    ...overrides,
  };
}

const liveRowsB: LiveTimingRowDto[] = [
  liveRow({ entryId: 10, driverName: 'Jane Doe', position: 1, lapsCompleted: 5 }),
  liveRow({ entryId: 11, driverName: 'John Smith', position: 2, lapsCompleted: 5, gapToLeaderMs: 500 }),
];

beforeEach(() => {
  vi.mocked(listRaces).mockReset();
  vi.mocked(getRaceDetail).mockReset();
  vi.mocked(getLiveSnapshot).mockReset();
  vi.mocked(transitionRace).mockReset();
  vi.mocked(recordMarshalAdjustment).mockReset();
  vi.mocked(advanceRound).mockReset();
  vi.mocked(useRaceChannel).mockReset();

  // Default: no STOMP message has arrived yet, so components fall back to getLiveSnapshot().
  vi.mocked(useRaceChannel).mockReturnValue({
    rows: null,
    stateEvent: null,
    marshalEvent: null,
    connected: true,
  });
});

async function selectRace(race: ScheduleEntryDto) {
  const label = `Round ${race.roundNumber} · Heat ${race.heatNumber} — ${race.className}`;
  const button = await screen.findByRole('button', { name: new RegExp(label) });
  fireEvent.click(button);
}

describe('RaceControl — state transitions', () => {
  it('shows a pending indicator on click, then reflects the new status once the call resolves', async () => {
    vi.mocked(listRaces).mockResolvedValue([raceA]);
    vi.mocked(getRaceDetail).mockResolvedValue({ race: raceA, grid: gridA });
    vi.mocked(getLiveSnapshot).mockResolvedValue({ scheduleId: 1, rows: [] });

    // A mock that resolves on the very next microtask can settle before findByText's first poll
    // even runs, making the pending state unobservable. A manually-controlled promise lets the
    // test assert the pending label is showing before the call resolves.
    let resolveTransition: (value: ScheduleEntryDto) => void = () => {};
    vi.mocked(transitionRace).mockReturnValue(
      new Promise((resolve) => {
        resolveTransition = resolve;
      }),
    );

    render(<RaceControl />);

    await selectRace(raceA);
    await screen.findByText('Jane Doe');

    const button = screen.getByRole('button', { name: /call to grid/i });
    fireEvent.click(button);

    expect(await screen.findByText('Calling to grid…')).toBeInTheDocument();

    resolveTransition({ ...raceA, status: 'GRID' });

    await screen.findByText('Status: GRID');
    expect(transitionRace).toHaveBeenCalledWith(1, 'GRID');
  });

  it('disables the control immediately and ignores a rapid second click', async () => {
    vi.mocked(listRaces).mockResolvedValue([raceA]);
    vi.mocked(getRaceDetail).mockResolvedValue({ race: raceA, grid: gridA });
    vi.mocked(getLiveSnapshot).mockResolvedValue({ scheduleId: 1, rows: [] });

    let resolveTransition: (value: ScheduleEntryDto) => void = () => {};
    vi.mocked(transitionRace).mockReturnValue(
      new Promise((resolve) => {
        resolveTransition = resolve;
      }),
    );

    render(<RaceControl />);

    await selectRace(raceA);
    await screen.findByText('Jane Doe');

    const button = screen.getByRole('button', { name: /call to grid/i });
    fireEvent.click(button);
    fireEvent.click(button);
    fireEvent.click(button);

    expect(transitionRace).toHaveBeenCalledTimes(1);

    resolveTransition({ ...raceA, status: 'GRID' });
    await screen.findByText('Status: GRID');
  });

  it('shows an inline error and re-fetches the race detail on a 409 illegal_transition', async () => {
    vi.mocked(listRaces).mockResolvedValue([raceA]);
    vi.mocked(getRaceDetail).mockResolvedValue({ race: raceA, grid: gridA });
    vi.mocked(getLiveSnapshot).mockResolvedValue({ scheduleId: 1, rows: [] });
    vi.mocked(transitionRace).mockRejectedValue({
      response: { status: 409, data: { error: 'illegal_transition' } },
    });

    render(<RaceControl />);

    await selectRace(raceA);
    await screen.findByText('Jane Doe');

    fireEvent.click(screen.getByRole('button', { name: /call to grid/i }));

    await screen.findByText(/no longer valid/i);
    // Initial load + the refetch triggered by the failed transition.
    await waitFor(() => expect(getRaceDetail).toHaveBeenCalledTimes(2));
    expect(screen.getByText('Status: PENDING')).toBeInTheDocument();
  });
});

describe('RaceControl — marshal adjustment', () => {
  it('calls recordMarshalAdjustment with the right cachedEntryId/lapDelta and updates the live table', async () => {
    vi.mocked(listRaces).mockResolvedValue([raceB]);
    vi.mocked(getRaceDetail).mockResolvedValue({ race: raceB, grid: gridB });
    vi.mocked(getLiveSnapshot).mockResolvedValue({ scheduleId: 2, rows: liveRowsB });
    vi.mocked(recordMarshalAdjustment).mockResolvedValue({
      raceId: 2,
      entryId: 10,
      transponderNumber: '1234567',
      lapDelta: 1,
      actingUserName: 'Official',
    });

    render(<RaceControl />);

    await selectRace(raceB);
    await screen.findAllByText('Jane Doe');

    const rows = screen.getAllByRole('row');
    const janeGridRow = rows.find((row) => within(row).queryByText('Jane Doe'));
    expect(janeGridRow).toBeDefined();

    const addLapButton = within(janeGridRow as HTMLElement).getByLabelText('Add marshal lap');
    fireEvent.click(addLapButton);

    await waitFor(() => expect(recordMarshalAdjustment).toHaveBeenCalledWith(2, 10, 1));

    await screen.findByText('6');
  });
});

describe('RaceControl — advance to next round', () => {
  it('calls advanceRound with entryIdsInFinishingOrder matching live row order and renders the returned grid', async () => {
    vi.mocked(listRaces).mockResolvedValue([raceA, raceB]);
    vi.mocked(getRaceDetail).mockResolvedValue({ race: raceB, grid: gridB });
    vi.mocked(getLiveSnapshot).mockResolvedValue({ scheduleId: 2, rows: liveRowsB });

    const nextDetail: ScheduleEntryDetailDto = {
      race: { ...raceA, status: 'GRID' },
      grid: [
        { ...gridB[0], gridPosition: 1 },
        { ...gridB[1], gridPosition: 2 },
      ],
    };
    vi.mocked(advanceRound).mockResolvedValue(nextDetail);

    render(<RaceControl />);

    await selectRace(raceB);
    await screen.findByLabelText('Next race');

    fireEvent.change(screen.getByLabelText('Next race'), { target: { value: String(raceA.id) } });
    fireEvent.click(screen.getByRole('button', { name: /advance to next round/i }));

    await waitFor(() => expect(advanceRound).toHaveBeenCalledWith(2, 1, [10, 11]));
    await screen.findByText('Advanced — next race grid set.');
  });

  it('shows a no-live-data message and does not call advanceRound when the live snapshot is empty', async () => {
    vi.mocked(listRaces).mockResolvedValue([raceA, raceB]);
    vi.mocked(getRaceDetail).mockResolvedValue({ race: raceB, grid: gridB });
    vi.mocked(getLiveSnapshot).mockResolvedValue({ scheduleId: 2, rows: [] });

    render(<RaceControl />);

    await selectRace(raceB);
    await screen.findByLabelText('Next race');

    await screen.findByText(/no live data available/i);

    fireEvent.change(screen.getByLabelText('Next race'), { target: { value: String(raceA.id) } });
    fireEvent.click(screen.getByRole('button', { name: /advance to next round/i }));

    expect(advanceRound).not.toHaveBeenCalled();
  });
});
