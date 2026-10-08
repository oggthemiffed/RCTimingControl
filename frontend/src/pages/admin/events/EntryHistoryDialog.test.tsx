import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';

import EntryHistoryDialog from './EntryHistoryDialog';
import type { AdminEntryDto, EntryHistoryItem } from '@/lib/adminApi';

type HistoryState = {
  data?: EntryHistoryItem[];
  isPending: boolean;
  isError: boolean;
  refetch: () => void;
};
let state: HistoryState;
vi.mock('@/hooks/admin/useAdminEntries', () => ({ useEntryHistory: () => state }));

const entry = { id: 1, displayName: 'Ada Lovelace' } as AdminEntryDto;

describe('EntryHistoryDialog', () => {
  beforeEach(() => {
    state = { isPending: false, isError: false, refetch: vi.fn() };
  });

  it('lists what happened with the reason and who did it', () => {
    state.data = [
      { at: '2026-10-06T09:00:00Z', actor: 'Race Director', summary: 'Added by hand as a walk-in', reason: null },
      { at: '2026-10-06T10:00:00Z', actor: 'Club Admin', summary: 'Withdrawn', reason: 'Car broke' },
    ];

    render(<EntryHistoryDialog entry={entry} onClose={vi.fn()} />);

    expect(screen.getByText('Added by hand as a walk-in')).toBeInTheDocument();
    expect(screen.getByText('Reason: Car broke')).toBeInTheDocument();
    expect(screen.getByText(/by Club Admin/)).toBeInTheDocument();
  });

  it('says so when nothing was recorded', () => {
    state.data = [];
    render(<EntryHistoryDialog entry={entry} onClose={vi.fn()} />);
    expect(screen.getByText(/Nothing has been recorded/)).toBeInTheDocument();
  });

  it('shows an error with a retry', () => {
    state.isError = true;
    render(<EntryHistoryDialog entry={entry} onClose={vi.fn()} />);

    expect(screen.getByRole('alert')).toHaveTextContent('Could not load the history');
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(state.refetch).toHaveBeenCalled();
  });
});
