import { useEffect, useRef, useState } from 'react';
import type { LiveTimingRowDto } from '@/lib/raceControlApi';

export type FlashColor = 'best' | 'on-pace' | 'slow';

/** How long the flash stays on the row before expiring. */
const FLASH_DURATION_MS = 2500;
/** Threshold above server-computed running average that classifies a lap as slow. */
const SLOW_THRESHOLD_MS = 2000;

/**
 * Detects new laps by watching lapsCompleted, classifies each lap time
 * against the server-provided running average, and returns a flash colour
 * for each entry that just completed a lap. Colours expire after FLASH_DURATION_MS.
 *
 *  best     — new personal best (lastLapMs === bestLapMs)
 *  on-pace  — within SLOW_THRESHOLD_MS of server running average (or no history yet)
 *  slow     — more than SLOW_THRESHOLD_MS above server running average
 */
export function useLapFlash(rows: LiveTimingRowDto[]): Map<number, FlashColor> {
  const prevLapsRef = useRef<Map<number, number>>(new Map());
  const timerRef = useRef<Map<number, ReturnType<typeof setTimeout>>>(new Map());
  const [flashMap, setFlashMap] = useState<Map<number, FlashColor>>(new Map());

  useEffect(() => {
    if (rows.length === 0) return;

    const prevLaps = prevLapsRef.current;
    const newFlashes: Array<[number, FlashColor]> = [];

    for (const row of rows) {
      const prevCount = prevLaps.get(row.entryId) ?? -1;

      if (row.lapsCompleted > prevCount && row.lastLapMs !== null && row.lastLapMs > 0) {
        let color: FlashColor;
        if (row.lastLapMs === row.bestLapMs) {
          color = 'best';
        } else if (row.avgLapMs !== null && row.lastLapMs > row.avgLapMs + SLOW_THRESHOLD_MS) {
          color = 'slow';
        } else {
          color = 'on-pace';
        }
        newFlashes.push([row.entryId, color]);
      }

      prevLaps.set(row.entryId, row.lapsCompleted);
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
