import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { PracticeSessionPage } from './PracticeSessionPage';

// Mock practiceApi
vi.mock('@/lib/practiceApi', () => ({
  getSession: vi.fn(),
  startSession: vi.fn(),
  stopSession: vi.fn(),
  getSnapshot: vi.fn().mockResolvedValue({ data: [] }),
}));

// Mock usePracticeTiming (avoids double STOMP client creation in tests)
const mockUsePracticeTiming = vi.fn();
vi.mock('@/hooks/race-control/usePracticeTiming', () => ({
  usePracticeTiming: () => mockUsePracticeTiming(),
}));

import * as practiceApi from '@/lib/practiceApi';

const idleSession = {
  id: 42,
  name: 'Sunday Practice',
  eventId: null,
  eventName: null,
  status: 'IDLE' as const,
  bestLapN: 3,
  startedAt: null,
  stoppedAt: null,
};

const runningSession = {
  ...idleSession,
  status: 'RUNNING' as const,
  startedAt: '2024-01-01T10:00:00Z',
};

const timingRow = {
  position: 1,
  transponderNumber: 'T001',
  userId: 1,
  racerName: 'John Smith',
  laps: 5,
  bestLapMs: 35000,
  bestConsecutiveNMs: 108000,
  lastLapMs: 36000,
  isUnknown: false,
};

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={['/race-control/practice/42']}>
        <Routes>
          <Route path="/race-control/practice/:sessionId" element={children} />
          <Route path="/race-control/practice/:sessionId/print" element={<div>Print page</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('PracticeSessionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUsePracticeTiming.mockReturnValue({
      rows: [],
      unknownTransponders: [],
      isLoading: false,
    });
  });

  it('shows IDLE empty state before session starts', async () => {
    vi.mocked(practiceApi.getSession).mockResolvedValue({ data: idleSession } as never);

    render(<PracticeSessionPage />, { wrapper });

    await waitFor(() =>
      expect(screen.getByText('Session not started')).toBeInTheDocument(),
    );
    expect(screen.getByText(/press start to begin timing/i)).toBeInTheDocument();
  });

  it('STOMP subscription updates live table on running session', async () => {
    vi.mocked(practiceApi.getSession).mockResolvedValue({ data: runningSession } as never);
    mockUsePracticeTiming.mockReturnValue({
      rows: [timingRow],
      unknownTransponders: [],
      isLoading: false,
    });

    render(<PracticeSessionPage />, { wrapper });

    await waitFor(() =>
      expect(screen.getByText('John Smith')).toBeInTheDocument(),
    );
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('unknown transponder banner appears when unknown detected', async () => {
    vi.mocked(practiceApi.getSession).mockResolvedValue({ data: runningSession } as never);
    mockUsePracticeTiming.mockReturnValue({
      rows: [],
      unknownTransponders: ['T999'],
      isLoading: false,
    });

    render(<PracticeSessionPage />, { wrapper });

    await waitFor(() =>
      expect(screen.getByText(/1 unknown transponder/i)).toBeInTheDocument(),
    );
    expect(screen.getByText(/T999/)).toBeInTheDocument();
  });

  it('best N consecutive column displays formatted lap time', async () => {
    vi.mocked(practiceApi.getSession).mockResolvedValue({ data: runningSession } as never);
    mockUsePracticeTiming.mockReturnValue({
      rows: [timingRow],
      unknownTransponders: [],
      isLoading: false,
    });

    render(<PracticeSessionPage />, { wrapper });

    await waitFor(() =>
      expect(screen.getByText('Best 3 Laps')).toBeInTheDocument(),
    );
    // bestConsecutiveNMs=108000, bestLapN=3 → avg = 36000ms → 36.000
    expect(screen.getByText('36.000 avg')).toBeInTheDocument();
  });

  it('stop button triggers stopSession mutation', async () => {
    vi.mocked(practiceApi.getSession).mockResolvedValue({ data: runningSession } as never);
    vi.mocked(practiceApi.stopSession).mockResolvedValue({ data: { ...runningSession, status: 'STOPPED' } } as never);

    render(<PracticeSessionPage />, { wrapper });

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /stop practice/i })).toBeInTheDocument(),
    );

    fireEvent.click(screen.getByRole('button', { name: /stop practice/i }));

    await waitFor(() =>
      expect(vi.mocked(practiceApi.stopSession)).toHaveBeenCalledWith(42),
    );
  });
});
