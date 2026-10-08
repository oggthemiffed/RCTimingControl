import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AxiosError, type AxiosResponse } from 'axios';

import OfficialsPage from './OfficialsPage';
import { adminApi, type OfficialDto } from '@/lib/adminApi';
import { HelpProvider } from '@/context/HelpContext';
import { toast } from 'sonner';

vi.mock('@/lib/adminApi', () => ({
  OFFICIAL_CHANGES_PAGE_SIZE: 50,
  adminApi: {
    officials: {
      list: vi.fn(),
      changes: vi.fn(),
      add: vi.fn(),
      changeRoles: vi.fn(),
      setPassword: vi.fn(),
      disable: vi.fn(),
      enable: vi.fn(),
    },
  },
}));
vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({ user: { id: '1', email: 'admin@example.com', firstName: 'Ann', lastName: 'Admin', roles: ['ADMIN'] } }),
}));

const api = vi.mocked(adminApi, true);

const ann: OfficialDto = {
  id: 1, email: 'admin@example.com', firstName: 'Ann', lastName: 'Admin', roles: ['ADMIN'],
  enabled: true, disabledAt: null, createdAt: '2026-10-01T10:00:00Z',
};
const rob: OfficialDto = {
  id: 2, email: 'rob@example.com', firstName: 'Rob', lastName: 'Director', roles: ['RACE_DIRECTOR', 'REFEREE'],
  enabled: true, disabledAt: null, createdAt: '2026-10-02T10:00:00Z',
};
const kim: OfficialDto = {
  id: 3, email: 'kim@example.com', firstName: 'Kim', lastName: 'Former', roles: ['REFEREE'],
  enabled: false, disabledAt: '2026-10-03T10:00:00Z', createdAt: '2026-09-01T10:00:00Z',
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <HelpProvider>
        <OfficialsPage />
      </HelpProvider>
    </QueryClientProvider>,
  );
}

function conflict(detail: string) {
  return new AxiosError('Conflict', '409', undefined, undefined,
    { status: 409, data: { detail } } as AxiosResponse);
}

describe('OfficialsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    api.officials.list.mockResolvedValue([ann, rob, kim]);
    api.officials.changes.mockResolvedValue([]);
  });

  it('lists officials with their roles and whether they can sign in', async () => {
    renderPage();

    const table = await screen.findByRole('table', { name: 'Officials' });
    expect(within(table).getByText('(you)')).toBeInTheDocument();
    expect(within(table).getByText('Race director')).toBeInTheDocument();
    expect(within(table).getAllByText('Can sign in')).toHaveLength(2);
    expect(within(table).getByText(/^Disabled/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Enable Kim Former' })).toBeInTheDocument();
  });

  it("doesn't let an admin disable themselves", async () => {
    renderPage();

    expect(await screen.findByRole('button', { name: 'Disable Ann Admin' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Disable Rob Director' })).toBeEnabled();
  });

  it('adds an official', async () => {
    api.officials.add.mockResolvedValue({ ...rob, id: 4, firstName: 'Sam', lastName: 'New', roles: ['RACE_DIRECTOR'] });
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Add official' }));
    fireEvent.change(screen.getByLabelText('First name'), { target: { value: 'Sam' } });
    fireEvent.change(screen.getByLabelText('Last name'), { target: { value: 'New' } });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'sam@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'long-enough' } });
    const dialog = screen.getByRole('dialog');
    fireEvent.click(within(dialog).getByRole('button', { name: 'Add official' }));

    await waitFor(() => expect(api.officials.add).toHaveBeenCalledWith({
      firstName: 'Sam', lastName: 'New', email: 'sam@example.com', password: 'long-enough', roles: ['RACE_DIRECTOR'],
    }));
    await waitFor(() => expect(toast.success).toHaveBeenCalledWith('Added Sam New'));
  });

  it('only sets a password once it is typed the same twice', async () => {
    api.officials.setPassword.mockResolvedValue(undefined);
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Set a new password for Rob Director' }));
    fireEvent.change(screen.getByLabelText('New password'), { target: { value: 'new-password' } });
    fireEvent.change(screen.getByLabelText('Type it again'), { target: { value: 'new-passw' } });
    expect(screen.getByText("The passwords don't match.")).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Set password' })).toBeDisabled();

    fireEvent.change(screen.getByLabelText('Type it again'), { target: { value: 'new-password' } });
    fireEvent.click(screen.getByRole('button', { name: 'Set password' }));

    await waitFor(() => expect(api.officials.setPassword).toHaveBeenCalledWith(2, 'new-password'));
  });

  it("shows the server's reason when a roles change is refused", async () => {
    api.officials.changeRoles.mockRejectedValue(conflict('The club needs at least one admin who can sign in.'));
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Change roles for Rob Director' }));
    fireEvent.click(screen.getByLabelText('Admin'));
    fireEvent.click(screen.getByRole('button', { name: 'Save roles' }));

    await waitFor(() => expect(api.officials.changeRoles).toHaveBeenCalledWith(2, ['RACE_DIRECTOR', 'REFEREE', 'ADMIN']));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('The club needs at least one admin who can sign in.'));
  });

  it('disables an official after confirming, and enables one', async () => {
    api.officials.disable.mockResolvedValue({ ...rob, enabled: false });
    api.officials.enable.mockResolvedValue({ ...kim, enabled: true });
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Disable Rob Director' }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Disable' }));
    await waitFor(() => expect(api.officials.disable).toHaveBeenCalledWith(2));

    fireEvent.click(screen.getByRole('button', { name: 'Enable Kim Former' }));
    await waitFor(() => expect(api.officials.enable).toHaveBeenCalledWith(3));
  });

  it('shows recent changes and who made them', async () => {
    api.officials.changes.mockResolvedValue([
      { id: 2, at: '2026-10-04T10:00:00Z', officialId: 3, officialName: 'Kim Former', action: 'DISABLED',
        detail: null, actorId: 1, actorName: 'Ann Admin' },
      { id: 1, at: '2026-10-03T10:00:00Z', officialId: 1, officialName: 'Ann Admin', action: 'PASSWORD_SET',
        detail: 'From the command line', actorId: null, actorName: null },
    ]);
    renderPage();

    const list = await screen.findByRole('list', { name: 'Recent changes' });
    expect(within(list).getByText(/by Ann Admin/)).toBeInTheDocument();
    expect(within(list).getByText(/from the command line ·/)).toBeInTheDocument();
    expect(within(list).getByText('Password set')).toBeInTheDocument();
  });

  it('offers older changes when a full page came back, and stops when a short one does', async () => {
    const change = (id: number) => ({
      id, at: '2026-10-04T10:00:00Z', officialId: 3, officialName: 'Kim Former', action: 'DISABLED' as const,
      detail: null, actorId: 1, actorName: 'Ann Admin',
    });
    const firstPage = Array.from({ length: 50 }, (_, i) => change(100 - i));
    api.officials.changes.mockResolvedValueOnce(firstPage).mockResolvedValueOnce([change(50)]);
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Show older changes' }));
    expect(api.officials.changes).toHaveBeenNthCalledWith(1, undefined);

    await waitFor(() => expect(api.officials.changes).toHaveBeenLastCalledWith(51));
    await waitFor(() =>
      expect(within(screen.getByRole('list', { name: 'Recent changes' })).getAllByRole('listitem')).toHaveLength(51));
    expect(screen.queryByRole('button', { name: 'Show older changes' })).not.toBeInTheDocument();
  });
});
