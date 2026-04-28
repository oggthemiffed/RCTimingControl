import { useRef, useEffect, useState } from 'react';

interface HasLapInfo {
  entryId: number;
  lapsCompleted: number;
}

/**
 * Returns the set of entryIds that have been continuously lapped (lap count
 * below the leader) for at least `debounceMs` milliseconds.
 *
 * The debounce prevents the "LAPPED" badge from flashing during the brief
 * window between the leader crossing the line and the backmarker doing so
 * on the same lap (typically 1-5 seconds at club RC speeds).
 */
export function useLappedBadge(rows: HasLapInfo[], debounceMs = 5000): Set<number> {
  const firstLappedAt = useRef<Map<number, number>>(new Map());
  const [lappedSet, setLappedSet] = useState<Set<number>>(new Set());

  useEffect(() => {
    if (rows.length === 0) {
      firstLappedAt.current.clear();
      setLappedSet(new Set());
      return;
    }

    const leaderLaps = Math.max(...rows.map((r) => r.lapsCompleted));
    const now = Date.now();
    const map = firstLappedAt.current;

    // Remove stale entries for cars no longer in the race
    const currentIds = new Set(rows.map((r) => r.entryId));
    for (const id of map.keys()) {
      if (!currentIds.has(id)) map.delete(id);
    }

    for (const row of rows) {
      if (row.lapsCompleted < leaderLaps) {
        if (!map.has(row.entryId)) map.set(row.entryId, now);
      } else {
        map.delete(row.entryId);
      }
    }

    const newSet = new Set<number>();
    for (const [entryId, since] of map) {
      if (now - since >= debounceMs) newSet.add(entryId);
    }
    setLappedSet(newSet);
  }, [rows, debounceMs]);

  return lappedSet;
}
