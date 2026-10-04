import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactElement } from 'react';
import CheckInDesk from './CheckInDesk';

// Ported from frontend-local's CheckInDesk tests (L11).

vi.mock('@/lib/raceControlApi', () => ({
  checkInResolve: vi.fn(),
  checkInSearch: vi.fn(),
  checkInConfirm: vi.fn(),
}));

// Stand-in for barcode-detector/ponyfill (which loads a WASM binary and needs a real camera)
// so BarcodeScanner's own decode path can be driven from the test, independently of the
// keyboard-wedge path. vi.mock factories are hoisted, so the mock lives behind vi.hoisted.
const { mockDetect } = vi.hoisted(() => ({ mockDetect: vi.fn() }));

vi.mock('barcode-detector/ponyfill', () => {
  class MockBarcodeDetector {
    detect(...args: unknown[]) {
      return mockDetect(...args);
    }
    static getSupportedFormats() {
      return Promise.resolve(['qr_code']);
    }
  }
  return {
    BarcodeDetector: MockBarcodeDetector,
    prepareZXingModule: vi.fn(),
  };
});

import { checkInResolve, checkInSearch, checkInConfirm } from '@/lib/raceControlApi';

const EVENT_ID = 7;

const sampleEntry = {
  entryId: 5,
  competitorName: 'Jane Doe',
  className: 'Touring Stock',
  transponderNumber: '1234567',
  secondaryTransponderNumber: null,
  checkedIn: false,
  checkedInAt: null,
  racehubArrival: 'ARRIVED' as const,
};

const freshConfirm = {
  entry: { ...sampleEntry, checkedIn: true, checkedInAt: '2026-10-04T10:00:00Z' },
  alreadyCheckedIn: false,
};

const getUserMedia = vi.fn();

function fakeStream(): MediaStream {
  return { getTracks: () => [{ stop: vi.fn() }] } as unknown as MediaStream;
}

function renderDesk(ui: ReactElement = <CheckInDesk eventId={EVENT_ID} />) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

beforeEach(() => {
  vi.mocked(checkInResolve).mockReset();
  vi.mocked(checkInSearch).mockReset();
  vi.mocked(checkInConfirm).mockReset();
  mockDetect.mockReset();
  mockDetect.mockResolvedValue([]);

  Object.defineProperty(navigator, 'mediaDevices', {
    value: { getUserMedia },
    configurable: true,
  });
  getUserMedia.mockReset();
  // Default to "no camera" so tests that aren't about the camera path don't run a scan loop.
  getUserMedia.mockRejectedValue(new Error('no camera'));
});

function wedgeInput() {
  return screen.getByPlaceholderText('Scan transponder or type its number…');
}

function scan(code: string) {
  const input = wedgeInput();
  fireEvent.change(input, { target: { value: code } });
  fireEvent.submit(input.closest('form')!);
}

describe('CheckInDesk: keyboard-wedge path', () => {
  it('resolves the entry from a keyboard-wedge scan and confirms check-in', async () => {
    vi.mocked(checkInResolve).mockResolvedValue([sampleEntry]);
    vi.mocked(checkInConfirm).mockResolvedValue(freshConfirm);

    renderDesk();
    scan('1234567');

    await screen.findByText('Jane Doe');
    expect(checkInResolve).toHaveBeenCalledWith(EVENT_ID, '1234567');
    expect(screen.getByText('RaceHub: arrived')).toBeInTheDocument();
    expect(screen.getByText('Not checked in')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /confirm check-in/i }));

    await screen.findByText('Checked in!');
    expect(checkInConfirm).toHaveBeenCalledWith(EVENT_ID, 5);
    expect(screen.getByText('Checked in')).toBeInTheDocument();
  });

  it('lists each entry when a competitor shares the transponder across classes', async () => {
    vi.mocked(checkInResolve).mockResolvedValue([
      sampleEntry,
      { ...sampleEntry, entryId: 6, className: 'Buggy Stock' },
    ]);

    renderDesk();
    scan('1234567');

    expect(await screen.findAllByRole('button', { name: /confirm check-in/i })).toHaveLength(2);
    expect(screen.getByText(/Buggy Stock/)).toBeInTheDocument();
  });
});

describe('CheckInDesk: camera scan path', () => {
  it('independently resolves the entry via a camera decode and confirms check-in', async () => {
    getUserMedia.mockResolvedValue(fakeStream());
    mockDetect.mockResolvedValue([{ rawValue: '7654321', format: 'qr_code' }]);
    vi.mocked(checkInResolve).mockResolvedValue([{ ...sampleEntry, transponderNumber: '7654321' }]);
    vi.mocked(checkInConfirm).mockResolvedValue(freshConfirm);

    renderDesk();

    await waitFor(() => expect(checkInResolve).toHaveBeenCalledWith(EVENT_ID, '7654321'), {
      timeout: 2000,
    });
    await screen.findByText('Jane Doe');

    fireEvent.click(screen.getByRole('button', { name: /confirm check-in/i }));
    await screen.findByText('Checked in!');
  });
});

describe('CheckInDesk: camera unavailable', () => {
  it('replaces the camera with a fallback message and keeps keyboard and search usable', async () => {
    getUserMedia.mockRejectedValue(new DOMException('Permission denied', 'NotAllowedError'));

    const { container } = renderDesk();

    await screen.findByText(/camera unavailable/i);
    expect(container.querySelector('video')).toBeNull();
    expect(wedgeInput()).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Competitor name…')).toBeInTheDocument();
  });
});

describe('CheckInDesk: unmatched scan', () => {
  it('falls back to search pre-filled with the scanned code instead of a dead end', async () => {
    vi.mocked(checkInResolve).mockRejectedValue({
      response: { status: 404, data: { error: 'not_found' } },
    });
    vi.mocked(checkInSearch).mockResolvedValue([]);

    renderDesk();
    scan('UNKNOWNCODE');

    await screen.findByText(/no match for/i);
    const searchInput = screen.getByPlaceholderText('Competitor name…') as HTMLInputElement;
    expect(searchInput.value).toBe('UNKNOWNCODE');
  });
});

describe('CheckInDesk: duplicate check-in', () => {
  it('shows a message distinguishable from a fresh confirmation', async () => {
    vi.mocked(checkInResolve).mockResolvedValue([sampleEntry]);
    vi.mocked(checkInConfirm).mockResolvedValue({
      entry: { ...sampleEntry, checkedIn: true, checkedInAt: '2026-10-04T09:00:00Z' },
      alreadyCheckedIn: true,
    });

    renderDesk();
    scan('1234567');

    await screen.findByText('Jane Doe');
    fireEvent.click(screen.getByRole('button', { name: /confirm check-in/i }));

    await screen.findByText(/already checked in at/i);
    expect(screen.queryByText('Checked in!')).toBeNull();
  });
});

describe('CheckInDesk: withdrawn entry', () => {
  it('says the entry was withdrawn when the confirm is refused', async () => {
    vi.mocked(checkInResolve).mockResolvedValue([sampleEntry]);
    vi.mocked(checkInConfirm).mockRejectedValue({
      response: { status: 409, data: { error: 'entry_withdrawn' } },
    });

    renderDesk();
    scan('1234567');

    await screen.findByText('Jane Doe');
    fireEvent.click(screen.getByRole('button', { name: /confirm check-in/i }));

    await screen.findByText(/was withdrawn/i);
  });
});
