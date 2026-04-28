import { useEffect, useRef, useState } from 'react';
import type { LiveTimingRowDto } from '@/lib/raceControlApi';

export type FlashColor = 'race-best' | 'personal-best' | 'improving' | 'slow';

/** How long the flash stays on the row before expiring. */
const FLASH_DURATION_MS = 2500;
/** Threshold above server-computed running average that classifies a lap as slow. */
const SLOW_THRESHOLD_MS = 2000;

/**
 * Detects new laps by watching lapsCompleted and classifies each lap time.
 * Returns a flash colour per entry that expires after FLASH_DURATION_MS.
 *
 *  race-best      — fastest lap of the race across all drivers (purple)
 *  personal-best  — new personal best for this driver, not race fastest (blue)
 *  improving      — faster than or equal to the driver's previous lap (green)
 *  slow           — more than SLOW_THRESHOLD_MS above running average (red)
 */
export function useLapFlash(rows: LiveTimingRowDto[]): Map<number, FlashColor> {
  const prevLapsRef = useRef<Map<number, number>>(new Map());
  const prevLastLapMsRef = useRef<Map<number, number>>(new Map());
  const timerRef = useRef<Map<number, ReturnType<typeof setTimeout>>>(new Map());
  const [flashMap, setFlashMap] = useState<Map<number, FlashColor>>(new Map());

  useEffect(() => {
    if (rows.length === 0) return;

    const prevLaps = prevLapsRef.current;
    const prevLastLapMs = prevLastLapMsRef.current;
    const newFlashes: Array<[number, FlashColor]> = [];

    for (const row of rows) {
      const prevCount = prevLaps.get(row.entryId) ?? -1;

      if (row.lapsCompleted > prevCount && row.lastLapMs !== null && row.lastLapMs > 0) {
        const prevLap = prevLastLapMs.get(row.entryId) ?? null;

        let color: FlashColor;
        if (row.overallFastestLapMs !== null && row.lastLapMs === row.overallFastestLapMs) {
          color = 'race-best';
        } else if (row.lastLapMs === row.bestLapMs) {
          color = 'personal-best';
        } else if (row.avgLapMs !== null && row.lastLapMs > row.avgLapMs + SLOW_THRESHOLD_MS) {
          color = 'slow';
        } else if (prevLap === null || row.lastLapMs <= prevLap) {
          color = 'improving';
        } else {
          color = 'slow';
        }

        newFlashes.push([row.entryId, color]);
      }

      prevLaps.set(row.entryId, row.lapsCompleted);
      if (row.lastLapMs !== null && row.lastLapMs > 0) {
        prevLastLapMs.set(row.entryId, row.lastLapMs);
      }
    }

    if (newFlashes.length === 0) return;

    setFlashMap((prev) => {
      const next = new Map(prev);
      for (const [id, color] of newFlashes) next.set(id, color);
      return next;
    });

    for (const [id] of newFlashes) {
      const existing = timerRef.current.get(id);
      if (existing) clearTimeout(existing);

      const timer = setTimeout(() => {
        setFlashMap((prev) => {
          const next = new Map(prev);
          next.delete(id);
          return next;
        });
        timerRef.current.delete(id);
      }, FLASH_DURATION_MS);

      timerRef.current.set(id, timer);
    }
  }, [rows]);

  useEffect(() => {
    const timers = timerRef.current;
    return () => {
      for (const timer of timers.values()) clearTimeout(timer);
    };
  }, []);

  return flashMap;
}
