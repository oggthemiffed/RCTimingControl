import { describe, it, expect } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useProximityAlerts } from './useProximityAlerts';

const gaps = (...values: (number | null)[]) => values.map((gapToAheadMs, i) => ({ entryId: i + 1, gapToAheadMs }));

describe('useProximityAlerts', () => {
  it('highlights nothing on the first update', () => {
    const rows = gaps(null, 1500);
    const { result } = renderHook(() => useProximityAlerts(1, rows));
    expect(result.current.size).toBe(0);
  });

  it('highlights a car that closed on the one ahead since the last update', () => {
    const { result, rerender } = renderHook(({ raceId, rows }) => useProximityAlerts(raceId, rows), {
      initialProps: { raceId: 1, rows: gaps(null, 2500, 4000) },
    });

    rerender({ raceId: 1, rows: gaps(null, 1500, 3900) });

    expect([...result.current]).toEqual([2]);
  });

  it('compares with the last non-empty update', () => {
    const { result, rerender } = renderHook(({ raceId, rows }) => useProximityAlerts(raceId, rows), {
      initialProps: { raceId: 1, rows: gaps(null, 2500) },
    });
    rerender({ raceId: 1, rows: [] });
    rerender({ raceId: 1, rows: gaps(null, 1500) });

    expect([...result.current]).toEqual([2]);
  });

  it('starts again when the race changes', () => {
    const { result, rerender } = renderHook(({ raceId, rows }) => useProximityAlerts(raceId, rows), {
      initialProps: { raceId: 1, rows: gaps(null, 2500) },
    });
    rerender({ raceId: 2, rows: gaps(null, 1500) });
    expect(result.current.size).toBe(0);

    rerender({ raceId: 2, rows: gaps(null, 900) });
    expect([...result.current]).toEqual([2]);
  });
});
