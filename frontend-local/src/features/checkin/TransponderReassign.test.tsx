import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import TransponderReassign from './TransponderReassign';

vi.mock('@/lib/api', () => ({
  checkinSearch: vi.fn(),
  reassignTransponder: vi.fn(),
}));

import { checkinSearch, reassignTransponder } from '@/lib/api';

const sampleEntry = {
  cachedEntryId: 5,
  racerName: 'Jane Doe',
  carName: 'TC-01',
  className: 'Touring Stock',
  transponderNumber: '1234567',
  checkedIn: false,
  checkedInAt: null,
};

beforeEach(() => {
  vi.mocked(checkinSearch).mockReset();
  vi.mocked(reassignTransponder).mockReset();
});

async function selectSampleEntry() {
  vi.mocked(checkinSearch).mockResolvedValue([sampleEntry]);
  fireEvent.change(screen.getByPlaceholderText('Racer name…'), { target: { value: 'doe' } });
  const entryButton = await screen.findByRole('button', { name: /jane doe/i });
  fireEvent.click(entryButton);
}

describe('TransponderReassign', () => {
  it('shows the old and new transponder numbers on a successful reassignment', async () => {
    vi.mocked(reassignTransponder).mockResolvedValue({
      cachedEntryId: 5,
      oldTransponderNumber: '1234567',
      newTransponderNumber: '7654321',
    });

    render(<TransponderReassign />);
    await selectSampleEntry();

    fireEvent.change(screen.getByLabelText(/new transponder number/i), {
      target: { value: '7654321' },
    });
    fireEvent.click(screen.getByRole('button', { name: /^reassign$/i }));

    await screen.findByText(/reassigned #1234567 → #7654321/i);
    expect(reassignTransponder).toHaveBeenCalledWith(5, '7654321');
  });

  it('shows a specific "already assigned to someone else" message on a 409, distinct from a 404', async () => {
    vi.mocked(reassignTransponder).mockRejectedValue({
      response: { status: 409, data: { error: 'transponder_already_assigned' } },
    });

    render(<TransponderReassign />);
    await selectSampleEntry();

    fireEvent.change(screen.getByLabelText(/new transponder number/i), {
      target: { value: '7654321' },
    });
    fireEvent.click(screen.getByRole('button', { name: /^reassign$/i }));

    await screen.findByText(/already assigned to someone else/i);
    expect(screen.queryByText('Entry not found.')).toBeNull();
  });

  it('shows "Entry not found." on a 404, distinct from the 409 conflict message', async () => {
    vi.mocked(reassignTransponder).mockRejectedValue({
      response: { status: 404, data: { error: 'entry_not_found' } },
    });

    render(<TransponderReassign />);
    await selectSampleEntry();

    fireEvent.change(screen.getByLabelText(/new transponder number/i), {
      target: { value: '7654321' },
    });
    fireEvent.click(screen.getByRole('button', { name: /^reassign$/i }));

    await screen.findByText('Entry not found.');
    expect(screen.queryByText(/already assigned to someone else/i)).toBeNull();
  });
});
