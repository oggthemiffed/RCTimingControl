import { useMemo, useState } from 'react';
import { computeProximityAlerts, type ProximityRow } from './alerts';

/**
 * OFFICIAL-01: the entries closing on the car ahead, comparing each timing update with the last non-empty one
 * for the same race. Changing race starts again, so highlights never carry over from another race.
 */
export function useProximityAlerts<T extends ProximityRow>(raceId: number | null, rows: T[]): Set<number> {
  const [seen, setSeen] = useState<{ raceId: number | null; rows: T[]; previous: T[] }>({
    raceId,
    rows,
    previous: [],
  });

  // Adjusted while rendering rather than in an effect, so the alerts belong to the rows being shown
  if (seen.raceId !== raceId || seen.rows !== rows) {
    const sameRace = seen.raceId === raceId;
    const previous = !sameRace ? [] : seen.rows.length > 0 ? seen.rows : seen.previous;
    setSeen({ raceId, rows, previous });
  }

  const { previous } = seen;
  return useMemo(() => computeProximityAlerts(rows, previous.length > 0 ? previous : null), [rows, previous]);
}
