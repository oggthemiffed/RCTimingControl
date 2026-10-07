import { useAuth } from '@/hooks/useAuth';

/**
 * What the signed-in official may do, from their roles, so a screen can hide a control the server would
 * refuse. The server is what enforces it: this only keeps the screen honest.
 *
 * - {@code isAdmin}: changes club config, tracks, classes, formats, events and championships (#132).
 * - {@code canRunEvent}: also moves an event through its day (publish, open, close entries, start, complete).
 */
export function useRoles() {
  const roles = useAuth().user?.roles ?? [];
  const isAdmin = roles.includes('ADMIN');
  return { isAdmin, canRunEvent: isAdmin || roles.includes('RACE_DIRECTOR') };
}
