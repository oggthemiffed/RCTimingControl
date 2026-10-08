import type { RaceClockDto } from '@/lib/boardsApi';

/**
 * When the race clock reaches zero, as epoch milliseconds, or null when the race has no set length or is not
 * counting. {@code fetchedAtMs} is when the clock was read: the server's remaining time counts from then.
 * Taking it from the server's clock, not the start time plus the length, allows for stoppages.
 */
export function raceEndsAt(
  clock: Pick<RaceClockDto, 'running' | 'remainingMs'> | undefined,
  fetchedAtMs: number,
): number | null {
  if (!clock || !clock.running || clock.remainingMs == null) return null;
  return fetchedAtMs + clock.remainingMs;
}

/**
 * Whether the lap just counted was a new personal best, for the lap beep. {@code previousBestMs} is the
 * driver's best before this passing. The row's best already includes the lap, so comparing the last lap with
 * it (as the cockpit did) can never find an improvement; a first timed lap has nothing to improve on.
 */
export function isImprovingLap(previousBestMs: number | undefined, row: { bestLapMs: number | null }): boolean {
  return previousBestMs !== undefined && row.bestLapMs !== null && row.bestLapMs < previousBestMs;
}
