import { describe, it, expect, vi } from 'vitest';
import { renderHook } from '@testing-library/react';

import { useRoles } from './useRoles';

const auth = vi.hoisted(() => ({ roles: [] as string[] | null }));
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({ user: auth.roles ? { roles: auth.roles } : null }),
}));

describe('useRoles', () => {
  it.each([
    [['ADMIN'], true, true, true],
    [['RACE_DIRECTOR'], false, true, true],
    [['REFEREE'], false, false, true],
    [['REFEREE', 'RACE_DIRECTOR'], false, true, true],
    [['ADMIN', 'REFEREE'], true, true, true],
    [[], false, false, false],
  ])('%j: admin %s, can run an event %s, official %s', (roles, isAdmin, canRunEvent, isOfficial) => {
    auth.roles = roles;

    expect(renderHook(() => useRoles()).result.current).toEqual({ isAdmin, canRunEvent, isOfficial });
  });

  it('allows nothing when nobody is signed in', () => {
    auth.roles = null;

    expect(renderHook(() => useRoles()).result.current).toEqual({ isAdmin: false, canRunEvent: false, isOfficial: false });
  });
});
