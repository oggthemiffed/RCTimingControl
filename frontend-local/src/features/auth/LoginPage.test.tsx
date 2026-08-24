import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import LoginPage from './LoginPage';

vi.mock('@/lib/api', () => ({
  listOfficials: vi.fn(),
  login: vi.fn(),
  recover: vi.fn(),
  getDayLifecycleStatus: vi.fn(),
}));

vi.mock('@/lib/auth', () => ({
  getStoredSession: vi.fn(),
  storeSession: vi.fn(),
}));

// OfficialShell pulls in RaceControl and CheckInDesk (which itself pulls in the camera/scanner
// stack — getUserMedia, barcode-detector, WASM), none of which is relevant to LoginPage's own
// behavior and isn't available in jsdom — stub it so these tests stay focused on the
// login/session flow. Coverage of OfficialShell's own tab-switching behavior and RaceControl's
// behavior live in their own feature directories, not here.
vi.mock('@/features/shell/OfficialShell', () => ({
  default: () => <div>Official Shell (mocked)</div>,
}));

import { getDayLifecycleStatus, listOfficials, login, recover } from '@/lib/api';
import { getStoredSession, storeSession } from '@/lib/auth';

const officials = [
  { credentialId: 1, officialName: 'Jane Doe', recovery: false },
  { credentialId: 2, officialName: 'John Smith', recovery: true },
];

const openStatus = {
  status: 'OPEN' as const,
  eventId: 42,
  generation: 1,
  splitBrainWarning: false,
  pendingSyncCount: 0,
  lastPreCachedAt: '2026-08-24T08:00:00Z',
};

beforeEach(() => {
  vi.mocked(getStoredSession).mockReset();
  vi.mocked(storeSession).mockReset();
  vi.mocked(listOfficials).mockReset();
  vi.mocked(login).mockReset();
  vi.mocked(recover).mockReset();
  vi.mocked(getDayLifecycleStatus).mockReset();

  vi.mocked(getStoredSession).mockResolvedValue(null);
  vi.mocked(storeSession).mockResolvedValue(undefined);
  vi.mocked(listOfficials).mockResolvedValue(officials);
  // Existing tests in this file exercise the officials picker/login flow, which only renders
  // once the day is OPEN — see LoginPage's day-lifecycle branching, covered separately.
  vi.mocked(getDayLifecycleStatus).mockResolvedValue(openStatus);
});

function selectOfficial(credentialId: number) {
  fireEvent.change(screen.getByLabelText('Official'), { target: { value: String(credentialId) } });
}

describe('LoginPage', () => {
  it('logs in successfully and shows the confirmation state', async () => {
    vi.mocked(login).mockResolvedValue({
      sessionToken: 'tok-1',
      officialName: 'Jane Doe',
      credentialId: 1,
    });

    render(<LoginPage />);

    await screen.findByText('Jane Doe');
    selectOfficial(1);
    fireEvent.change(screen.getByLabelText('PIN'), { target: { value: '1234' } });
    fireEvent.click(screen.getByRole('button', { name: /log in/i }));

    await screen.findByText('Official Shell (mocked)');

    expect(login).toHaveBeenCalledWith(1, '1234');
    expect(storeSession).toHaveBeenCalledWith({
      sessionToken: 'tok-1',
      officialName: 'Jane Doe',
      credentialId: 1,
    });
  });

  it('shows "incorrect PIN" on a 401 invalid_credential response', async () => {
    vi.mocked(login).mockRejectedValue({
      response: { status: 401, data: { error: 'invalid_credential' } },
    });

    render(<LoginPage />);

    await screen.findByText('Jane Doe');
    selectOfficial(1);
    fireEvent.change(screen.getByLabelText('PIN'), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: /log in/i }));

    await screen.findByText('Incorrect PIN.');
    expect(screen.queryByText(/too many attempts/i)).toBeNull();
  });

  it('shows the lockout message with retryAfterSeconds on a 423 locked response', async () => {
    vi.mocked(login).mockRejectedValue({
      response: { status: 423, data: { error: 'locked', retryAfterSeconds: 42 } },
    });

    render(<LoginPage />);

    await screen.findByText('Jane Doe');
    selectOfficial(1);
    fireEvent.change(screen.getByLabelText('PIN'), { target: { value: '1234' } });
    fireEvent.click(screen.getByRole('button', { name: /log in/i }));

    await screen.findByText(/try again in 42 seconds/i);
    expect(screen.queryByText('Incorrect PIN.')).toBeNull();
  });

  it('shows the recovery option on mount, without prior interaction', async () => {
    render(<LoginPage />);
    await screen.findByText('Jane Doe');

    expect(screen.getByRole('button', { name: /use recovery credential/i })).toBeVisible();
  });

  it('submits the recovery form with the right argument shape', async () => {
    vi.mocked(recover).mockResolvedValue({ unlocked: true });

    render(<LoginPage />);
    await screen.findByText('Jane Doe');

    fireEvent.click(screen.getByRole('button', { name: /use recovery credential/i }));

    fireEvent.change(screen.getByLabelText(/recovery official credential id/i), {
      target: { value: '2' },
    });
    fireEvent.change(screen.getByLabelText(/recovery secret/i), {
      target: { value: '5678' },
    });
    fireEvent.change(screen.getByLabelText(/official to unlock/i), {
      target: { value: '1' },
    });

    fireEvent.click(screen.getByRole('button', { name: /unlock/i }));

    await waitFor(() => expect(recover).toHaveBeenCalledWith(2, '5678', 1));
    await screen.findByText('Credential unlocked.');
  });

  it('renders the login form, not race control, when no session is stored (AE4)', async () => {
    render(<LoginPage />);

    await screen.findByLabelText('Official');
    expect(screen.queryByText('Official Shell (mocked)')).toBeNull();
  });

  it('shows the logged-in state immediately when a session is already stored', async () => {
    vi.mocked(getStoredSession).mockResolvedValue({
      sessionToken: 'tok-existing',
      officialName: 'Existing Official',
      credentialId: 3,
    });

    render(<LoginPage />);

    await screen.findByText('Official Shell (mocked)');
    expect(screen.queryByLabelText('Official')).toBeNull();
    expect(listOfficials).not.toHaveBeenCalled();
  });

  it('shows the day setup screen instead of the officials picker when NOT_SET_UP', async () => {
    vi.mocked(getDayLifecycleStatus).mockResolvedValue({
      status: 'NOT_SET_UP',
      eventId: null,
      generation: null,
      splitBrainWarning: false,
      pendingSyncCount: 0,
      lastPreCachedAt: null,
    });

    render(<LoginPage />);

    await screen.findByText('Set up this event day');
    expect(screen.queryByLabelText('Official')).toBeNull();
    expect(listOfficials).not.toHaveBeenCalled();
  });

  it('shows the day setup screen with open as the primary action when PRE_CACHED', async () => {
    vi.mocked(getDayLifecycleStatus).mockResolvedValue({
      status: 'PRE_CACHED',
      eventId: 7,
      generation: null,
      splitBrainWarning: false,
      pendingSyncCount: 0,
      lastPreCachedAt: '2026-08-24T08:00:00Z',
    });

    render(<LoginPage />);

    await screen.findByText('Open this event day');
    expect(screen.queryByLabelText('Official')).toBeNull();
    expect(listOfficials).not.toHaveBeenCalled();
  });

  it('shows a closed message with no picker or setup form when CLOSED', async () => {
    vi.mocked(getDayLifecycleStatus).mockResolvedValue({
      status: 'CLOSED',
      eventId: null,
      generation: null,
      splitBrainWarning: false,
      pendingSyncCount: 0,
      lastPreCachedAt: null,
    });

    render(<LoginPage />);

    await screen.findByText('This event day is closed');
    expect(screen.queryByLabelText('Official')).toBeNull();
    expect(screen.queryByText('Set up this event day')).toBeNull();
    expect(listOfficials).not.toHaveBeenCalled();
  });

  it('falls back to the officials picker if the day-lifecycle status fetch fails', async () => {
    vi.mocked(getDayLifecycleStatus).mockRejectedValue(new Error('network error'));

    render(<LoginPage />);

    await screen.findByText('Jane Doe');
    expect(screen.getByLabelText('Official')).toBeInTheDocument();
  });
});
