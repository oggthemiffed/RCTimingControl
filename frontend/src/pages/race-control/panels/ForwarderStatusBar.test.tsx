import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ForwarderStatusBar } from './ForwarderStatusBar';

// Mock useStomp hook
const mockUseStomp = vi.fn();
vi.mock('@/hooks/race-control/useStomp', () => ({
  useStomp: () => mockUseStomp(),
}));

describe('ForwarderStatusBar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders both DECODER and FORWARDER pills', () => {
    mockUseStomp.mockReturnValue({ data: null, status: 'disconnected' });
    render(<ForwarderStatusBar />);

    expect(screen.getByLabelText(/DECODER connection status/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/FORWARDER connection status/i)).toBeInTheDocument();
  });

  it('shows green styling when CONNECTED', () => {
    mockUseStomp.mockReturnValue({
      data: { decoderState: 'CONNECTED', forwarderState: 'CONNECTED' },
      status: 'connected',
    });
    render(<ForwarderStatusBar />);

    const decoderPill = screen.getByLabelText(/DECODER connection status: CONNECTED/i);
    expect(decoderPill).toHaveClass('text-[var(--flag-green)]');
    expect(screen.getByText('DECODER connected')).toBeInTheDocument();
  });

  it('shows red styling when DISCONNECTED', () => {
    mockUseStomp.mockReturnValue({
      data: { decoderState: 'DISCONNECTED', forwarderState: 'DISCONNECTED' },
      status: 'connected',
    });
    render(<ForwarderStatusBar />);

    const decoderPill = screen.getByLabelText(/DECODER connection status: DISCONNECTED/i);
    expect(decoderPill).toHaveClass('text-[var(--flag-red)]');
    expect(screen.getByText('DECODER disconnected')).toBeInTheDocument();
  });

  it('shows amber styling when RECONNECTING', () => {
    mockUseStomp.mockReturnValue({
      data: { decoderState: 'RECONNECTING', forwarderState: 'CONNECTED' },
      status: 'connected',
    });
    render(<ForwarderStatusBar />);

    const decoderPill = screen.getByLabelText(/DECODER connection status: RECONNECTING/i);
    expect(decoderPill).toHaveClass('text-[var(--flag-yellow)]');
    expect(screen.getByText('DECODER reconnecting…')).toBeInTheDocument();
  });

  it('shows red styling and dash when no STOMP data received (null state)', () => {
    mockUseStomp.mockReturnValue({ data: null, status: 'connecting' });
    render(<ForwarderStatusBar />);

    const decoderPill = screen.getByLabelText(/DECODER connection status: unknown/i);
    expect(decoderPill).toHaveClass('text-[var(--flag-red)]');
    expect(screen.getByText('DECODER —')).toBeInTheDocument();
  });
});
