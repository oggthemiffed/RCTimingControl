import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AxiosError, AxiosHeaders } from 'axios';
import { toast } from 'sonner';

import AddWalkInEntryDialog from './AddWalkInEntryDialog';
import { adminApi } from '@/lib/adminApi';

vi.mock('sonner', () => ({ toast: { success: vi.fn(), warning: vi.fn(), error: vi.fn() } }));

vi.mock('@/lib/adminApi', () => ({
  adminApi: {
    createWalkInEntry: vi.fn(),
    competitors: { list: vi.fn() },
  },
}));

const api = vi.mocked(adminApi, true);

function renderDialog(onOpenChange = vi.fn()) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <AddWalkInEntryDialog eventId={5} classId={11} open onOpenChange={onOpenChange} />
    </QueryClientProvider>,
  );
  return onOpenChange;
}

function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

function conflict(detail: string) {
  const headers = new AxiosHeaders();
  return new AxiosError('Conflict', 'ERR_BAD_REQUEST', { headers }, null, {
    status: 409, statusText: 'Conflict', data: { detail }, headers, config: { headers },
  });
}

describe('AddWalkInEntryDialog', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    api.competitors.list.mockResolvedValue([
      { id: 7, displayName: 'Ada Lovelace', brcaNumber: null, homeClub: 'Analytical RC' },
      { id: 8, displayName: 'Grace Hopper', brcaNumber: null, homeClub: null },
    ]);
  });

  it('adds a walk-in for an existing driver picked from the search', async () => {
    api.createWalkInEntry.mockResolvedValue({
      entry: { id: 99, status: 'CONFIRMED', transponderNumberSnapshot: '1234' }, warnings: [],
    });
    const onOpenChange = renderDialog();

    type('Driver', 'ada');
    fireEvent.click(await screen.findByRole('button', { name: /Ada Lovelace/ }));
    type('Primary transponder', '1234');
    type('Secondary transponder (optional)', '5678');
    fireEvent.click(screen.getByRole('button', { name: 'Add entry' }));

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
    expect(api.createWalkInEntry).toHaveBeenCalledWith({
      eventId: 5, eventClassId: 11, competitorId: 7, primaryTransponder: '1234', secondaryTransponder: '5678',
    });
    expect(toast.success).toHaveBeenCalledWith('Entry added for Ada Lovelace.');
  });

  it('adds a new driver by name and shows transponder warnings', async () => {
    api.createWalkInEntry.mockResolvedValue({
      entry: { id: 100, status: 'CONFIRMED', transponderNumberSnapshot: '4321' },
      warnings: ['Transponder 4321 is already used by another entry in this event.'],
    });
    renderDialog();

    type('Driver', 'Wendy Walkin');
    expect(await screen.findByText(/will be added as a new driver/)).toBeInTheDocument();
    type('Primary transponder', '4321');
    fireEvent.click(screen.getByRole('button', { name: 'Add entry' }));

    await waitFor(() => expect(api.createWalkInEntry).toHaveBeenCalledWith({
      eventId: 5, eventClassId: 11, competitorName: 'Wendy Walkin', primaryTransponder: '4321',
    }));
    await waitFor(() =>
      expect(toast.warning).toHaveBeenCalledWith('Transponder 4321 is already used by another entry in this event.'),
    );
  });

  it('shows the duplicate-entry message from the server and stays open', async () => {
    api.createWalkInEntry.mockRejectedValue(conflict('Ada Lovelace already has an entry in this class'));
    const onOpenChange = renderDialog();

    type('Driver', 'ada');
    fireEvent.click(await screen.findByRole('button', { name: /Ada Lovelace/ }));
    type('Primary transponder', '1234');
    fireEvent.click(screen.getByRole('button', { name: 'Add entry' }));

    expect(await screen.findByText('Ada Lovelace already has an entry in this class')).toBeInTheDocument();
    expect(onOpenChange).not.toHaveBeenCalled();
  });

  it('needs a driver and a primary transponder before calling the server', async () => {
    renderDialog();

    fireEvent.click(screen.getByRole('button', { name: 'Add entry' }));
    expect(await screen.findByText('Choose a driver or enter a name.')).toBeInTheDocument();

    type('Driver', 'New Person');
    fireEvent.click(screen.getByRole('button', { name: 'Add entry' }));
    expect(await screen.findByText('Enter the primary transponder number.')).toBeInTheDocument();
    expect(api.createWalkInEntry).not.toHaveBeenCalled();
  });
});
