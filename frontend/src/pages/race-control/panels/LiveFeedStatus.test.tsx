import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { LiveFeedStatus } from './LiveFeedStatus';
import { fetchLiveFeedStatus, getLiveFeedSetting, setLiveFeedSetting } from '@/lib/raceControlApi';

const mockUseStomp = vi.fn();
vi.mock('@/hooks/race-control/useStomp', () => ({
  useStomp: () => mockUseStomp(),
}));

const mockUseAuth = vi.fn();
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('@/lib/raceControlApi', () => ({
  fetchLiveFeedStatus: vi.fn(),
  getLiveFeedSetting: vi.fn(),
  setLiveFeedSetting: vi.fn(),
}));

vi.mock('sonner', () => ({ toast: { error: vi.fn() } }));

function renderStatus() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <LiveFeedStatus eventId={21} />
    </QueryClientProvider>,
  );
}

describe('LiveFeedStatus', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseStomp.mockReturnValue({ data: null, status: 'connected' });
    mockUseAuth.mockReturnValue({ user: { roles: ['RACE_DIRECTOR'] } });
    vi.mocked(getLiveFeedSetting).mockResolvedValue({ enabled: false });
  });

  it('shows nothing when no relay is set up', async () => {
    vi.mocked(fetchLiveFeedStatus).mockResolvedValue({
      state: 'NOT_SET_UP',
      relayHost: null,
      missingSettings: ['rctiming.livefeed.relay-url', 'rctiming.livefeed.token'],
    });
    const { container } = renderStatus();

    await waitFor(() => expect(fetchLiveFeedStatus).toHaveBeenCalled());
    expect(container).toBeEmptyDOMElement();
    expect(getLiveFeedSetting).not.toHaveBeenCalled();
  });

  it('shows the connection and lets a race director turn the feed on for the event', async () => {
    vi.mocked(fetchLiveFeedStatus).mockResolvedValue({ state: 'IDLE', relayHost: 'relay.example', missingSettings: [] });
    vi.mocked(setLiveFeedSetting).mockResolvedValue({ enabled: true });
    renderStatus();

    expect(await screen.findByText('LIVE FEED ready')).toBeInTheDocument();
    const toggle = screen.getByRole('switch', { name: /send this event/i });
    await waitFor(() => expect(toggle).toBeEnabled());
    expect(toggle).not.toBeChecked();

    fireEvent.click(toggle);

    await waitFor(() => expect(setLiveFeedSetting).toHaveBeenCalledWith(21, true));
    await waitFor(() => expect(toggle).toBeChecked());
  });

  it('follows the status the server pushes', async () => {
    // The first poll hasn't answered yet
    vi.mocked(fetchLiveFeedStatus).mockReturnValue(new Promise(() => {}));
    mockUseStomp.mockReturnValue({
      data: { state: 'RECONNECTING', relayHost: 'relay.example', missingSettings: [] },
      status: 'connected',
    });
    renderStatus();

    const pill = await screen.findByLabelText('Live feed status: RECONNECTING');
    expect(pill).toHaveClass('text-[var(--flag-yellow)]');
    expect(screen.getByText('LIVE FEED reconnecting…')).toBeInTheDocument();
  });

  it('shows the setting to a referee without letting them change it', async () => {
    mockUseAuth.mockReturnValue({ user: { roles: ['REFEREE'] } });
    vi.mocked(fetchLiveFeedStatus).mockResolvedValue({ state: 'CONNECTED', relayHost: 'relay.example', missingSettings: [] });
    vi.mocked(getLiveFeedSetting).mockResolvedValue({ enabled: true });
    renderStatus();

    expect(await screen.findByText('LIVE FEED sending')).toBeInTheDocument();
    const toggle = screen.getByRole('switch', { name: /send this event/i });
    await waitFor(() => expect(toggle).toBeChecked());
    expect(toggle).toBeDisabled();
  });
});
