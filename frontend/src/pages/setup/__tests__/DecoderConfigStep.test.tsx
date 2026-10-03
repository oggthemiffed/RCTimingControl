import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DecoderConfigStep from '../steps/DecoderConfigStep';
import * as raceControlApi from '@/lib/raceControlApi';

vi.mock('@/lib/raceControlApi', () => ({
  fetchDecoderStatus: vi.fn(),
}));

vi.mock('@/lib/setupApi', () => ({
  getDecoderConfig: vi.fn().mockResolvedValue({ decoderHost: null, decoderPort: null, decoderProtocol: null }),
  updateDecoderConfig: vi.fn(),
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
  vi.mocked(raceControlApi.fetchDecoderStatus).mockResolvedValue({ decoderState: 'DISCONNECTED' });
});

afterEach(() => {
  vi.clearAllMocks();
  vi.useRealTimers();
});

describe('DecoderConfigStep', () => {
  it('Test Connection polls every 2s up to 15 attempts (30s timeout per D-17)', async () => {
    vi.useFakeTimers();

    renderStep();

    await act(async () => {
      fireEvent.click(screen.getByText('Test Connection'));
    });

    for (let i = 0; i < 16; i++) {
      await act(async () => {
        await vi.advanceTimersByTimeAsync(2000);
      });
    }

    const callCount = vi.mocked(raceControlApi.fetchDecoderStatus).mock.calls.length;
    expect(callCount).toBeGreaterThanOrEqual(14);
    expect(callCount).toBeLessThanOrEqual(16);
  });

  it('shows Connected when the decoder reports CONNECTED, with no forwarder involved', async () => {
    vi.mocked(raceControlApi.fetchDecoderStatus).mockResolvedValue({ decoderState: 'CONNECTED' });

    renderStep();

    fireEvent.click(screen.getByText('Test Connection'));

    await waitFor(() => expect(screen.getByText('Connected')).toBeInTheDocument());
    expect(screen.getByText(/Connection confirmed/i)).toBeInTheDocument();
  });

  it('shows timeout alert after 15 failed attempts', async () => {
    vi.useFakeTimers();

    renderStep();

    await act(async () => {
      fireEvent.click(screen.getByText('Test Connection'));
    });

    for (let i = 0; i < 16; i++) {
      await act(async () => {
        await vi.advanceTimersByTimeAsync(2000);
      });
    }

    expect(screen.getByText(/Decoder not yet connected/i)).toBeInTheDocument();
  });

  it('has no forwarder token or forwarder.env download', () => {
    renderStep();

    expect(screen.queryByText(/forwarder/i)).not.toBeInTheDocument();
  });
});
