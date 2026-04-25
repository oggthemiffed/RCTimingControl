export const PROXIMITY_CLOSING_DELTA_MS = 500;
export const PROXIMITY_ABSOLUTE_MAX_MS = 3000;

export type LiveRacePositionDto = {
  entryId: number;
  position: number;
  lapsCompleted: number;
  lastPassingTimeMs: number;
  bestLapMs: number | null;
  gapToLeaderMs: number | null;
  gapToAheadMs: number | null;
};

/** OFFICIAL-01: entryIds whose gapToAheadMs has shrunk by >= PROXIMITY_CLOSING_DELTA_MS since previous. */
export function computeProximityAlerts(
  current: LiveRacePositionDto[],
  previous: LiveRacePositionDto[] | null,
): Set<number> {
  if (!previous || previous.length === 0) return new Set<number>();

  const prevById = new Map(previous.map((p) => [p.entryId, p]));
  const result = new Set<number>();

  for (const c of current) {
    const p = prevById.get(c.entryId);
    if (!p) continue;
    if (
      c.gapToAheadMs !== null &&
      p.gapToAheadMs !== null &&
      c.gapToAheadMs !== 0 &&
      p.gapToAheadMs !== 0 &&
      p.gapToAheadMs > c.gapToAheadMs + PROXIMITY_CLOSING_DELTA_MS &&
      c.gapToAheadMs <= PROXIMITY_ABSOLUTE_MAX_MS
    ) {
      result.add(c.entryId);
    }
  }

  return result;
}

/** OFFICIAL-02: entryIds whose lapsCompleted < leader's lapsCompleted. */
export function computeBackmarkers(current: LiveRacePositionDto[]): Set<number> {
  if (current.length === 0) return new Set<number>();

  const leaderLaps = Math.max(...current.map((c) => c.lapsCompleted));
  const result = new Set<number>();

  for (const c of current) {
    if (c.lapsCompleted < leaderLaps) {
      result.add(c.entryId);
    }
  }

  return result;
}
