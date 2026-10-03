import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import ResultsBoard from './ResultsBoard';
import type { ResultsDto, ScheduleEntryDto } from '@/lib/api';

vi.mock('@/lib/api', () => ({
  getResults: vi.fn(),
}));

import { getResults } from '@/lib/api';

const finishedRace: ScheduleEntryDto = {
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

const populatedResults: ResultsDto = {
  race: finishedRace,
  results: [
    {
      entryId: 1,
      racerName: 'Jane Doe',
      transponderNumber: '1234567',
      position: 1,
      lapsCompleted: 10,
      bestLapMs: 19000,
    },
    {
      entryId: 2,
      racerName: null,
      transponderNumber: null,
      position: 2,
      lapsCompleted: 9,
      bestLapMs: null,
    },
  ],
};

beforeEach(() => {
  vi.mocked(getResults).mockReset();
});

describe('ResultsBoard — happy path', () => {
  it('renders the last-finished race identity and its results rows', async () => {
    vi.mocked(getResults).mockResolvedValue(populatedResults);

    render(<ResultsBoard />);

    await screen.findByText(/Round 1 · Heat 0 A — 1\/10 Buggy/);
    expect(await screen.findByText('Jane Doe')).toBeInTheDocument();
    expect(screen.getByText('1234567')).toBeInTheDocument();
    // Unresolved entry falls back to em-dashes rather than blank cells.
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
  });
});

describe('ResultsBoard — nothing finished yet', () => {
  it('shows a clear empty-state message instead of a blank table', async () => {
    vi.mocked(getResults).mockResolvedValue({ race: null, results: [] });

    render(<ResultsBoard />);

    expect(await screen.findByText(/no results yet/i)).toBeInTheDocument();
    expect(screen.queryByRole('table')).toBeNull();
  });
});
