import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import BoardsRouter from './BoardsRouter';

vi.mock('@/lib/api', () => ({
  getNowNext: vi.fn(),
  getResults: vi.fn(),
}));

vi.mock('@/lib/stomp', () => ({
  useRaceChannel: vi.fn(),
}));

import { getNowNext, getResults } from '@/lib/api';
import { useRaceChannel } from '@/lib/stomp';

beforeEach(() => {
  vi.mocked(getNowNext).mockReset();
  vi.mocked(getResults).mockReset();
  vi.mocked(useRaceChannel).mockReset();
  // Both boards render fine with empty/idle data — the router test only cares which board mounts.
  vi.mocked(getNowNext).mockResolvedValue({ currentRace: null, nextRace: null, lastCompletedRace: null });
  vi.mocked(getResults).mockResolvedValue({ race: null, results: [] });
  vi.mocked(useRaceChannel).mockReturnValue({
    rows: null,
    stateEvent: null,
    marshalEvent: null,
    connected: true,
  });
});

describe('BoardsRouter', () => {
  it('renders NowNextBoard for /boards/now-next', async () => {
    render(<BoardsRouter pathname="/boards/now-next" />);
    expect(await screen.findByText(/no race data yet/i)).toBeInTheDocument();
  });

  it('renders ResultsBoard for /boards/results', async () => {
    render(<BoardsRouter pathname="/boards/results" />);
    expect(await screen.findByText(/no results yet/i)).toBeInTheDocument();
  });

  it('renders a fallback message for an unrecognized board path', () => {
    render(<BoardsRouter pathname="/boards/nonsense" />);
    expect(screen.getByText(/unknown board/i)).toBeInTheDocument();
  });
});
