import { useAuth } from '@/hooks/useAuth';

/**
 * What the signed-in official may do, from their roles, so a screen can hide a control the server would
 * refuse. The server is what enforces it: this only keeps the screen honest.
 *
 * - {@code isAdmin}: changes club config, tracks, classes, formats, events and championships (#132).
 * - {@code canRunEvent}: also moves an event through its day (publish, open, close entries, start, complete),
 *   adds entries and runs the races (generate rounds, call grid, start, finish).
 *
 * - {@code isOfficial}: holds any official role (admin, race director or referee). Every account the server
 *   creates has one, so this only guards a control against a session without roles.
 */
export function useRoles() {
  const roles = useAuth().user?.roles ?? [];
  const isAdmin = roles.includes('ADMIN');
  const canRunEvent = isAdmin || roles.includes('RACE_DIRECTOR');
  return { isAdmin, canRunEvent, isOfficial: canRunEvent || roles.includes('REFEREE') };
}
