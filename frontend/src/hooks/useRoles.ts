import { useAuth } from '@/hooks/useAuth';

/**
 * What the signed-in official may do, from their roles, so a screen can hide a control the server would
 * refuse. The server is what enforces it: this only keeps the screen honest.
 *
 * - {@code isAdmin}: changes club config, tracks, classes, formats, events and championships (#132).
 * - {@code canRunEvent}: also moves an event through its day (publish, open, close entries, start, complete),
 *   adds entries and runs the races (generate rounds, call grid, start, finish).
 *
 * Every signed-in user is an official (admin, race director or referee), so anything any official may do needs
 * no check here.
 */
export function useRoles() {
  const roles = useAuth().user?.roles ?? [];
  const isAdmin = roles.includes('ADMIN');
  return { isAdmin, canRunEvent: isAdmin || roles.includes('RACE_DIRECTOR') };
}
