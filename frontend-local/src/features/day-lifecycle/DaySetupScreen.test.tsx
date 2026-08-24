import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import DaySetupScreen from './DaySetupScreen';
import type { DayLifecycleStatus } from '@/lib/api';

vi.mock('@/lib/api', () => ({
  preCacheDay: vi.fn(),
  openDay: vi.fn(),
}));

import { preCacheDay, openDay } from '@/lib/api';

const notSetUp: DayLifecycleStatus = {
  status: 'NOT_SET_UP',
  eventId: null,
  generation: null,
  splitBrainWarning: false,
  pendingSyncCount: 0,
  lastPreCachedAt: null,
};

const preCached: DayLifecycleStatus = {
  status: 'PRE_CACHED',
  eventId: 42,
  generation: 1,
  splitBrainWarning: false,
  pendingSyncCount: 0,
  lastPreCachedAt: '2026-08-23T20:00:00Z',
};

beforeEach(() => {
  vi.mocked(preCacheDay).mockReset();
  vi.mocked(openDay).mockReset();
});

function fillCredentials() {
  fireEvent.change(screen.getByLabelText(/event id/i), { target: { value: '42' } });
  fireEvent.change(screen.getByLabelText(/cloud email/i), {
    target: { value: 'official@example.com' },
  });
  fireEvent.change(screen.getByLabelText(/cloud password/i), { target: { value: 'hunter2' } });
}

describe('DaySetupScreen', () => {
  it('pre-caches successfully and stays on the setup screen without opening', async () => {
    vi.mocked(preCacheDay).mockResolvedValue(preCached);
    const onStatusChange = vi.fn();

    render(<DaySetupScreen status={notSetUp} onStatusChange={onStatusChange} />);
    fillCredentials();

    fireEvent.click(screen.getByRole('button', { name: /pre-cache now/i }));

    await screen.findByText(/pre-cached successfully/i);
    expect(preCacheDay).toHaveBeenCalledWith(42, 'official@example.com', 'hunter2');
    expect(onStatusChange).toHaveBeenCalledWith(preCached);
    // Still on the setup screen — pre-caching alone must not open the day.
    expect(screen.getByRole('button', { name: /pre-cache now/i })).toBeInTheDocument();
  });

  it('opens the day online successfully', async () => {
    const openedStatus: DayLifecycleStatus = {
      status: 'OPEN',
      eventId: 42,
      generation: 2,
      splitBrainWarning: false,
      pendingSyncCount: 0,
      lastPreCachedAt: '2026-08-23T20:00:00Z',
    };
    vi.mocked(openDay).mockResolvedValue(openedStatus);
    const onStatusChange = vi.fn();

    render(<DaySetupScreen status={preCached} onStatusChange={onStatusChange} />);
    fireEvent.change(screen.getByLabelText(/cloud email/i), {
      target: { value: 'official@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/cloud password/i), { target: { value: 'hunter2' } });

    fireEvent.click(screen.getByRole('button', { name: /^open day \(start the event day\)$/i }));

    await waitFor(() =>
      expect(openDay).toHaveBeenCalledWith(42, 'official@example.com', 'hunter2'),
    );
    expect(onStatusChange).toHaveBeenCalledWith(openedStatus);
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('opens the day offline with no credentials and surfaces the split-brain warning', async () => {
    const openedOfflineStatus: DayLifecycleStatus = {
      status: 'OPEN',
      eventId: 42,
      generation: 2,
      splitBrainWarning: true,
      pendingSyncCount: 0,
      lastPreCachedAt: '2026-08-23T20:00:00Z',
    };
    vi.mocked(openDay).mockResolvedValue(openedOfflineStatus);
    const onStatusChange = vi.fn();

    render(<DaySetupScreen status={preCached} onStatusChange={onStatusChange} />);

    fireEvent.click(screen.getByRole('button', { name: /open day \(offline/i }));

    await waitFor(() => expect(openDay).toHaveBeenCalledWith(42));
    expect(onStatusChange).toHaveBeenCalledWith(openedOfflineStatus);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/could not confirm exclusive control/i);
  });

  it('shows a wrong-credentials message on a 401 response', async () => {
    vi.mocked(openDay).mockRejectedValue({ response: { status: 401 } });

    render(<DaySetupScreen status={notSetUp} onStatusChange={vi.fn()} />);
    fillCredentials();

    fireEvent.click(screen.getByRole('button', { name: /^open day \(start the event day\)$/i }));

    await screen.findByText(/incorrect cloud email or password/i);
  });

  it('shows an unreachable-cloud message on a network error (no response)', async () => {
    vi.mocked(openDay).mockRejectedValue(new Error('Network Error'));

    render(<DaySetupScreen status={notSetUp} onStatusChange={vi.fn()} />);
    fillCredentials();

    fireEvent.click(screen.getByRole('button', { name: /^open day \(start the event day\)$/i }));

    await screen.findByText(/could not reach the cloud service/i);
  });

  it('shows the conflict message the backend supplied on a 409 response', async () => {
    vi.mocked(openDay).mockRejectedValue({
      response: { status: 409, data: { message: 'Day already open on another device.' } },
    });

    render(<DaySetupScreen status={notSetUp} onStatusChange={vi.fn()} />);
    fillCredentials();

    fireEvent.click(screen.getByRole('button', { name: /^open day \(start the event day\)$/i }));

    await screen.findByText('Day already open on another device.');
  });

  it('pre-fills the event id from status when already pre-cached', () => {
    render(<DaySetupScreen status={preCached} onStatusChange={vi.fn()} />);
    expect(screen.getByLabelText(/event id/i)).toHaveValue(42);
  });
});
