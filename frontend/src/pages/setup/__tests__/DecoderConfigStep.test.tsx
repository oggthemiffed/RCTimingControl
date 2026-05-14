import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DecoderConfigStep from '../steps/DecoderConfigStep';
import * as raceControlApi from '@/lib/raceControlApi';

vi.mock('@/lib/raceControlApi', () => ({
  fetchForwarderStatus: vi.fn(),
  getForwarderTokenStatus: vi.fn(),
  generateForwarderToken: vi.fn(),
  revokeForwarderToken: vi.fn(),
}));

function makeClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

function renderStep() {
  return render(
    <QueryClientProvider client={makeClient()}>
      <DecoderConfigStep onNext={vi.fn()} onBack={vi.fn()} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.mocked(raceControlApi.getForwarderTokenStatus).mockResolvedValue({
    status: 'NONE',
    generatedAt: null,
  });
  vi.mocked(raceControlApi.fetchForwarderStatus).mockResolvedValue({
    forwarderState: 'DISCONNECTED',
    decoderState: 'DISCONNECTED',
  });
});

afterEach(() => {
  vi.clearAllMocks();
  vi.useRealTimers();
});

describe('DecoderConfigStep (Wave 0 stub — enabled in Plan 06)', () => {
  it('Test Connection polls every 2s up to 15 attempts (30s timeout per D-17)', async () => {
    vi.useFakeTimers();
    vi.mocked(raceControlApi.fetchForwarderStatus).mockResolvedValue({
      forwarderState: 'DISCONNECTED',
      decoderState: 'DISCONNECTED',
    });

    renderStep();

    await act(async () => {
      fireEvent.click(screen.getByText('Test Connection'));
    });

    for (let i = 0; i < 16; i++) {
      await act(async () => {
        await vi.advanceTimersByTimeAsync(2000);
      });
    }

    const callCount = vi.mocked(raceControlApi.fetchForwarderStatus).mock.calls.length;
    expect(callCount).toBeGreaterThanOrEqual(14);
    expect(callCount).toBeLessThanOrEqual(16);
  });

  it('shows Connected badge when forwarder status returns CONNECTED', async () => {
    vi.mocked(raceControlApi.fetchForwarderStatus).mockResolvedValue({
      forwarderState: 'CONNECTED',
      decoderState: 'CONNECTED',
    });

    renderStep();

    fireEvent.click(screen.getByText('Test Connection'));

    await waitFor(() => expect(screen.getByText('Connected')).toBeInTheDocument());
    expect(screen.getByText(/Connection confirmed/i)).toBeInTheDocument();
  });

  it('shows timeout alert after 15 failed attempts', async () => {
    vi.useFakeTimers();
    vi.mocked(raceControlApi.fetchForwarderStatus).mockResolvedValue({
      forwarderState: 'DISCONNECTED',
      decoderState: 'DISCONNECTED',
    });

    renderStep();

    await act(async () => {
      fireEvent.click(screen.getByText('Test Connection'));
    });

    for (let i = 0; i < 16; i++) {
      await act(async () => {
        await vi.advanceTimersByTimeAsync(2000);
      });
    }

    expect(screen.getByText(/Forwarder not yet connected/i)).toBeInTheDocument();
  });

  it('Download forwarder.env button disabled when no token exists', async () => {
    renderStep();

    await waitFor(() =>
      expect(screen.getByText('Download forwarder.env')).toBeInTheDocument(),
    );

    const btn = screen.getByText('Download forwarder.env').closest('button');
    expect(btn).toBeDisabled();
  });
});
