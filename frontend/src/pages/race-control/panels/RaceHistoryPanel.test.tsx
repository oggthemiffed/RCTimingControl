import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RaceHistoryPanel } from './RaceHistoryPanel';

vi.mock('@/lib/raceControlApi', () => ({ getRaceHistory: vi.fn() }));
import { getRaceHistory } from '@/lib/raceControlApi';

function renderPanel() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <RaceHistoryPanel raceId={5} status="FINISHED" />
    </QueryClientProvider>,
  );
}

describe('RaceHistoryPanel', () => {
  beforeEach(() => vi.mocked(getRaceHistory).mockReset());

  it('lists what happened with the driver and the official', async () => {
    vi.mocked(getRaceHistory).mockResolvedValue([
      { at: '2026-10-08T10:00:00Z', kind: 'LIFECYCLE', actor: 'Race Director', driver: null, summary: 'Started heat 1' },
      { at: '2026-10-08T10:03:00Z', kind: 'PENALTY', actor: 'Race Referee', driver: 'Alex Rowe', summary: 'LAP penalty of 1: Jumped start' },
    ]);
    renderPanel();

    expect(await screen.findByText('Started heat 1')).toBeInTheDocument();
    expect(screen.getByText(/LAP penalty of 1/)).toBeInTheDocument();
    expect(screen.getByText(/Alex Rowe/)).toBeInTheDocument();
    expect(screen.getByText('Race Referee')).toBeInTheDocument();
    expect(screen.getByText('Penalty')).toBeInTheDocument();
  });

  it('says so when nothing has been recorded', async () => {
    vi.mocked(getRaceHistory).mockResolvedValue([]);
    renderPanel();

    expect(await screen.findByText(/Nothing has been recorded/)).toBeInTheDocument();
  });

  it('shows an error with a retry', async () => {
    vi.mocked(getRaceHistory).mockRejectedValueOnce(new Error('boom'));
    renderPanel();

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load the race history');

    vi.mocked(getRaceHistory).mockResolvedValue([]);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByText(/Nothing has been recorded/)).toBeInTheDocument();
  });
});
