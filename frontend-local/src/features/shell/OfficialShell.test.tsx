import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import OfficialShell from './OfficialShell';

vi.mock('@/lib/api', () => ({
  closeDay: vi.fn(),
}));

vi.mock('@/lib/auth', () => ({
  clearSession: vi.fn(),
}));

// RaceControl/CheckInDesk pull in dependencies (camera/scanner stack) unrelated to the Close Day
// flow under test here — stub them out, same rationale as LoginPage.test.tsx stubbing
// OfficialShell itself.
vi.mock('@/features/race-control/RaceControl', () => ({
  default: () => <div>Race Control (mocked)</div>,
}));
vi.mock('@/features/checkin/CheckInDesk', () => ({
  default: () => <div>Check-in Desk (mocked)</div>,
}));

import { closeDay } from '@/lib/api';
import { clearSession } from '@/lib/auth';

beforeEach(() => {
  vi.mocked(closeDay).mockReset();
  vi.mocked(clearSession).mockReset();
  vi.mocked(clearSession).mockResolvedValue(undefined);
});

describe('OfficialShell', () => {
  it('does not call closeDay when the confirmation is declined', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    render(<OfficialShell />);
    fireEvent.click(screen.getByRole('button', { name: /close day/i }));

    expect(closeDay).not.toHaveBeenCalled();
  });

  it('clears the session and calls onDayClosed when close succeeds', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(closeDay).mockResolvedValue({ status: 'closed', pendingSyncCount: 0 });
    const onDayClosed = vi.fn();

    render(<OfficialShell onDayClosed={onDayClosed} />);
    fireEvent.click(screen.getByRole('button', { name: /close day/i }));

    await waitFor(() => expect(clearSession).toHaveBeenCalled());
    expect(onDayClosed).toHaveBeenCalled();
  });

  it('shows the pending sync count and does not clear the session when close is pending', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(closeDay).mockResolvedValue({ status: 'pending', pendingSyncCount: 3 });
    const onDayClosed = vi.fn();

    render(<OfficialShell onDayClosed={onDayClosed} />);
    fireEvent.click(screen.getByRole('button', { name: /close day/i }));

    await screen.findByText(/3 items still pending/i);
    expect(clearSession).not.toHaveBeenCalled();
    expect(onDayClosed).not.toHaveBeenCalled();
  });

  it('shows the persistent split-brain warning banner when the prop is set', () => {
    render(<OfficialShell splitBrainWarning />);
    expect(screen.getByRole('alert')).toHaveTextContent(/could not confirm exclusive control/i);
  });

  it('does not show the split-brain warning banner by default', () => {
    render(<OfficialShell />);
    expect(screen.queryByRole('alert')).toBeNull();
  });
});
