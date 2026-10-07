import { describe, it, expect, vi } from 'vitest';
import { renderHook } from '@testing-library/react';

import { useRoles } from './useRoles';

const auth = vi.hoisted(() => ({ roles: [] as string[] | null }));
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({ user: auth.roles ? { roles: auth.roles } : null }),
}));

describe('useRoles', () => {
  it.each([
    [['ADMIN'], true, true],
    [['RACE_DIRECTOR'], false, true],
    [['REFEREE'], false, false],
    [['REFEREE', 'RACE_DIRECTOR'], false, true],
    [['ADMIN', 'REFEREE'], true, true],
  ])('%j: admin %s, can run an event %s', (roles, isAdmin, canRunEvent) => {
    auth.roles = roles;

    expect(renderHook(() => useRoles()).result.current).toEqual({ isAdmin, canRunEvent });
  });

  it('allows nothing when nobody is signed in', () => {
    auth.roles = null;

    expect(renderHook(() => useRoles()).result.current).toEqual({ isAdmin: false, canRunEvent: false });
  });
});
