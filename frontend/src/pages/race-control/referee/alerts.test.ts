import { describe, it, expect } from 'vitest';
import {
  computeProximityAlerts,
  computeBackmarkers,
  PROXIMITY_CLOSING_DELTA_MS,
  type ProximityRow,
  type LapRow,
} from './alerts';

function makeProximityRow(overrides: Partial<ProximityRow> & { entryId: number }): ProximityRow {
  return { gapToAheadMs: null, ...overrides };
}

function makeLapRow(overrides: Partial<LapRow> & { entryId: number }): LapRow {
  return { lapsCompleted: 5, ...overrides };
}

describe('computeProximityAlerts', () => {
  it('computeProximityAlerts_returnsEmptyWhenPreviousIsNull', () => {
    const current = [makeProximityRow({ entryId: 1, gapToAheadMs: 1000 })];
    expect(computeProximityAlerts(current, null).size).toBe(0);
  });

  it('computeProximityAlerts_flagsEntryWhenGapShrunkBeyondDelta', () => {
    const current = [
      makeProximityRow({ entryId: 1, gapToAheadMs: null }),
      makeProximityRow({ entryId: 2, gapToAheadMs: 800 }),
    ];
    const previous = [
      makeProximityRow({ entryId: 1, gapToAheadMs: null }),
      makeProximityRow({ entryId: 2, gapToAheadMs: 2000 }),
    ];
    const alerts = computeProximityAlerts(current, previous);
    expect(alerts).toEqual(new Set([2]));
  });

  it('computeProximityAlerts_ignoresClosuresBeyondAbsoluteMax', () => {
    const current = [makeProximityRow({ entryId: 2, gapToAheadMs: 4000 })];
    const previous = [makeProximityRow({ entryId: 2, gapToAheadMs: 5000 })];
    const alerts = computeProximityAlerts(current, previous);
    expect(alerts.size).toBe(0);
  });

  it('computeProximityAlerts_ignoresSmallShrinks', () => {
    const current = [makeProximityRow({ entryId: 2, gapToAheadMs: 900 })];
    const previous = [makeProximityRow({ entryId: 2, gapToAheadMs: 1000 })];
    const alerts = computeProximityAlerts(current, previous);
    expect(alerts.size).toBe(0);
    expect(PROXIMITY_CLOSING_DELTA_MS).toBe(500);
  });
});

describe('computeBackmarkers', () => {
  it('computeBackmarkers_flagsCarsOnLowerLapCount', () => {
    const current = [
      makeLapRow({ entryId: 1, lapsCompleted: 12 }),
      makeLapRow({ entryId: 2, lapsCompleted: 11 }),
      makeLapRow({ entryId: 3, lapsCompleted: 10 }),
    ];
    const backmarkers = computeBackmarkers(current);
    expect(backmarkers).toEqual(new Set([2, 3]));
  });

  it('computeBackmarkers_excludesLeaderAndCarsOnLeadLap', () => {
    const current = [
      makeLapRow({ entryId: 1, lapsCompleted: 15 }),
      makeLapRow({ entryId: 2, lapsCompleted: 15 }),
    ];
    const backmarkers = computeBackmarkers(current);
    expect(backmarkers.size).toBe(0);
  });
});
