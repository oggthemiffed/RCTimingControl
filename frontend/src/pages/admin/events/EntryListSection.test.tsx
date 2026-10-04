import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';

import EntryListSection from './EntryListSection';
import { useAuth } from '@/hooks/useAuth';
import type { AuthContextValue, AuthUser } from '@/providers/AuthProvider';
import type { EventClassDto } from '@/lib/adminApi';

vi.mock('@/hooks/useAuth', () => ({ useAuth: vi.fn() }));
vi.mock('@/hooks/admin/useAdminEntries', () => ({
  useEntriesForClass: () => ({ data: [], isLoading: false }),
  useWithdrawEntry: () => ({ mutate: vi.fn(), isPending: false }),
}));
vi.mock('./AddWalkInEntryDialog', () => ({ default: () => null }));

const classes = [{ id: 11, racingClassId: 3 }] as EventClassDto[];

function signInAs(roles: AuthUser['roles']) {
  vi.mocked(useAuth).mockReturnValue({
    user: { id: '1', email: 'staff@example.com', firstName: 'Staff', lastName: 'User', roles },
  } as AuthContextValue);
}

describe('EntryListSection', () => {
  beforeEach(() => vi.resetAllMocks());

  it.each([['ADMIN'], ['RACE_DIRECTOR']] as const)('shows Add entry to %s', role => {
    signInAs([role]);
    render(<EntryListSection eventId={5} classes={classes} />);
    expect(screen.getByRole('button', { name: 'Add entry' })).toBeInTheDocument();
  });

  it('hides Add entry from a referee', () => {
    signInAs(['REFEREE']);
    render(<EntryListSection eventId={5} classes={classes} />);
    expect(screen.queryByRole('button', { name: 'Add entry' })).not.toBeInTheDocument();
  });
});
