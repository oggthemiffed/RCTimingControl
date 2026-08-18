import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import CheckInDesk from './CheckInDesk';

vi.mock('@/lib/api', () => ({
  checkinResolve: vi.fn(),
  checkinSearch: vi.fn(),
  checkinConfirm: vi.fn(),
  reassignTransponder: vi.fn(),
}));

// Stand-in for the real barcode-detector/ponyfill module (which loads a WASM binary and needs
// a real camera feed) so BarcodeScanner's own decode path can be driven deterministically from
// the test, independent of the keyboard-wedge path. `vi.mock` factories are hoisted above
// other top-level code, so the mock detect function and class both have to live inside the
// factory (or behind `vi.hoisted`) rather than as separate top-level consts.
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

import { checkinResolve, checkinSearch, checkinConfirm } from '@/lib/api';

const sampleEntry = {
  cachedEntryId: 5,
  racerName: 'Jane Doe',
  carName: 'TC-01',
  className: 'Touring Stock',
  transponderNumber: '1234567',
  checkedIn: false,
  checkedInAt: null,
};

const freshConfirm = {
  cachedEntryId: 5,
  racerName: 'Jane Doe',
  checkedIn: true,
  checkedInAt: '2026-08-18T10:00:00Z',
  alreadyCheckedIn: false,
};

function fakeStream(): MediaStream {
  return { getTracks: () => [{ stop: vi.fn() }] } as unknown as MediaStream;
}

beforeEach(() => {
  vi.mocked(checkinResolve).mockReset();
  vi.mocked(checkinSearch).mockReset();
  vi.mocked(checkinConfirm).mockReset();
  mockDetect.mockReset();
  mockDetect.mockResolvedValue([]);

  vi.mocked(navigator.mediaDevices.getUserMedia).mockReset();
  // Default to "no camera" so tests that aren't about the camera path aren't slowed down by a
  // live (mocked) scan loop running in the background.
  vi.mocked(navigator.mediaDevices.getUserMedia).mockRejectedValue(new Error('no camera'));
});

function wedgeInput() {
  return screen.getByPlaceholderText('Scan transponder or type its number…');
}

describe('CheckInDesk — keyboard-wedge path', () => {
  it('resolves the pre-entered racer from a keyboard-wedge scan and confirms check-in', async () => {
    vi.mocked(checkinResolve).mockResolvedValue(sampleEntry);
    vi.mocked(checkinConfirm).mockResolvedValue(freshConfirm);

    render(<CheckInDesk />);

    const input = wedgeInput();
    fireEvent.change(input, { target: { value: '1234567' } });
    fireEvent.submit(input.closest('form')!);

    await screen.findByText('Jane Doe');
    expect(checkinResolve).toHaveBeenCalledWith('1234567');

    fireEvent.click(screen.getByRole('button', { name: /confirm check-in/i }));

    await screen.findByText('Checked in!');
    expect(checkinConfirm).toHaveBeenCalledWith(5);
  });
});

describe('CheckInDesk — camera scan path', () => {
  it('independently resolves the racer via a camera decode and confirms check-in', async () => {
    vi.mocked(navigator.mediaDevices.getUserMedia).mockResolvedValue(fakeStream());
    mockDetect.mockResolvedValue([{ rawValue: '7654321', format: 'qr_code' }]);

    vi.mocked(checkinResolve).mockResolvedValue({
      ...sampleEntry,
      transponderNumber: '7654321',
    });
    vi.mocked(checkinConfirm).mockResolvedValue(freshConfirm);

    render(<CheckInDesk />);

    await waitFor(() => expect(checkinResolve).toHaveBeenCalledWith('7654321'), {
      timeout: 2000,
    });
    await screen.findByText('Jane Doe');

    fireEvent.click(screen.getByRole('button', { name: /confirm check-in/i }));
    await screen.findByText('Checked in!');
  });
});

describe('CheckInDesk — camera unavailable', () => {
  it('replaces the camera UI with a fallback message and keeps keyboard/search usable', async () => {
    vi.mocked(navigator.mediaDevices.getUserMedia).mockRejectedValue(
      new DOMException('Permission denied', 'NotAllowedError'),
    );

    const { container } = render(<CheckInDesk />);

    await screen.findByText(/camera unavailable/i);
    expect(container.querySelector('video')).toBeNull();

    expect(wedgeInput()).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Racer name…')).toBeInTheDocument();
  });
});

describe('CheckInDesk — unmatched scan', () => {
  it('falls back to manual search pre-filled with the scanned code instead of a dead end', async () => {
    vi.mocked(checkinResolve).mockRejectedValue({
      response: { status: 404, data: { error: 'not_found' } },
    });
    vi.mocked(checkinSearch).mockResolvedValue([]);

    render(<CheckInDesk />);

    const input = wedgeInput();
    fireEvent.change(input, { target: { value: 'UNKNOWNCODE' } });
    fireEvent.submit(input.closest('form')!);

    await screen.findByText(/no match for/i);
    const searchInput = screen.getByPlaceholderText('Racer name…') as HTMLInputElement;
    expect(searchInput.value).toBe('UNKNOWNCODE');
  });
});

describe('CheckInDesk — duplicate check-in', () => {
  it('shows a message distinguishable from a fresh confirmation when alreadyCheckedIn is true', async () => {
    vi.mocked(checkinResolve).mockResolvedValue(sampleEntry);
    vi.mocked(checkinConfirm).mockResolvedValue({
      cachedEntryId: 5,
      racerName: 'Jane Doe',
      checkedIn: true,
      checkedInAt: '2026-08-18T09:00:00Z',
      alreadyCheckedIn: true,
    });

    render(<CheckInDesk />);

    const input = wedgeInput();
    fireEvent.change(input, { target: { value: '1234567' } });
    fireEvent.submit(input.closest('form')!);

    await screen.findByText('Jane Doe');
    fireEvent.click(screen.getByRole('button', { name: /confirm check-in/i }));

    await screen.findByText(/already checked in at 2026-08-18T09:00:00Z/i);
    expect(screen.queryByText('Checked in!')).toBeNull();
  });
});
