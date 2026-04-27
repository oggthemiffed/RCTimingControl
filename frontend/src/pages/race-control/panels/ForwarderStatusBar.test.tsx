import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ForwarderStatusBar } from './ForwarderStatusBar';

// Mock useStomp hook
const mockUseStomp = vi.fn();
vi.mock('@/hooks/race-control/useStomp', () => ({
  useStomp: () => mockUseStomp(),
}));

// Mock the REST status fetch so useQuery doesn't hit the network
vi.mock('@/lib/raceControlApi', () => ({
  getForwarderStatus: vi.fn().mockResolvedValue(null),
}));

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('ForwarderStatusBar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders both DECODER and FORWARDER pills', () => {
    mockUseStomp.mockReturnValue({ data: null, status: 'disconnected' });
    render(<ForwarderStatusBar />, { wrapper });

    expect(screen.getByLabelText(/DECODER connection status/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/FORWARDER connection status/i)).toBeInTheDocument();
  });

  it('shows green styling when CONNECTED', () => {
    mockUseStomp.mockReturnValue({
      data: { decoderState: 'CONNECTED', forwarderState: 'CONNECTED' },
      status: 'connected',
    });
    render(<ForwarderStatusBar />, { wrapper });

    const decoderPill = screen.getByLabelText(/DECODER connection status: CONNECTED/i);
    expect(decoderPill).toHaveClass('text-[var(--flag-green)]');
    expect(screen.getByText('DECODER connected')).toBeInTheDocument();
  });

  it('shows red styling when DISCONNECTED', () => {
    mockUseStomp.mockReturnValue({
      data: { decoderState: 'DISCONNECTED', forwarderState: 'DISCONNECTED' },
      status: 'connected',
    });
    render(<ForwarderStatusBar />, { wrapper });

    const decoderPill = screen.getByLabelText(/DECODER connection status: DISCONNECTED/i);
    expect(decoderPill).toHaveClass('text-[var(--flag-red)]');
    expect(screen.getByText('DECODER disconnected')).toBeInTheDocument();
  });

  it('shows amber styling when RECONNECTING', () => {
    mockUseStomp.mockReturnValue({
      data: { decoderState: 'RECONNECTING', forwarderState: 'CONNECTED' },
      status: 'connected',
    });
    render(<ForwarderStatusBar />, { wrapper });

    const decoderPill = screen.getByLabelText(/DECODER connection status: RECONNECTING/i);
    expect(decoderPill).toHaveClass('text-[var(--flag-yellow)]');
    expect(screen.getByText('DECODER reconnecting…')).toBeInTheDocument();
  });

  it('shows red styling and dash when no STOMP data received (null state)', () => {
    mockUseStomp.mockReturnValue({ data: null, status: 'connecting' });
    render(<ForwarderStatusBar />, { wrapper });

    const decoderPill = screen.getByLabelText(/DECODER connection status: unknown/i);
    expect(decoderPill).toHaveClass('text-[var(--flag-red)]');
    expect(screen.getByText('DECODER —')).toBeInTheDocument();
  });
});
