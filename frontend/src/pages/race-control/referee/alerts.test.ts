import { describe, it, expect } from 'vitest';
import {
  computeProximityAlerts,
  computeBackmarkers,
  PROXIMITY_CLOSING_DELTA_MS,
  type LiveRacePositionDto,
} from './alerts';

function makePos(overrides: Partial<LiveRacePositionDto> & { entryId: number }): LiveRacePositionDto {
  return {
    position: 1,
    lapsCompleted: 5,
    lastPassingTimeMs: 1000,
    bestLapMs: null,
    gapToLeaderMs: null,
    gapToAheadMs: null,
    ...overrides,
  };
}

describe('computeProximityAlerts', () => {
  it('computeProximityAlerts_returnsEmptyWhenPreviousIsNull', () => {
    const current = [makePos({ entryId: 1, gapToAheadMs: 1000 })];
    expect(computeProximityAlerts(current, null).size).toBe(0);
  });

  it('computeProximityAlerts_flagsEntryWhenGapShrunkBeyondDelta', () => {
    const current = [
      makePos({ entryId: 1, position: 1, gapToAheadMs: null }),
      makePos({ entryId: 2, position: 2, gapToAheadMs: 800 }),
    ];
    const previous = [
      makePos({ entryId: 1, position: 1, gapToAheadMs: null }),
      makePos({ entryId: 2, position: 2, gapToAheadMs: 2000 }),
    ];
    const alerts = computeProximityAlerts(current, previous);
    expect(alerts).toEqual(new Set([2]));
  });

  it('computeProximityAlerts_ignoresClosuresBeyondAbsoluteMax', () => {
    // Gap shrunk from 5000 to 4000 (1000 ms shrink) but current gap 4000 > 3000
    const current = [makePos({ entryId: 2, position: 2, gapToAheadMs: 4000 })];
    const previous = [makePos({ entryId: 2, position: 2, gapToAheadMs: 5000 })];
    const alerts = computeProximityAlerts(current, previous);
    expect(alerts.size).toBe(0);
  });

  it('computeProximityAlerts_ignoresSmallShrinks', () => {
    // Gap shrunk by only 100 ms — less than PROXIMITY_CLOSING_DELTA_MS (500)
    const current = [makePos({ entryId: 2, position: 2, gapToAheadMs: 900 })];
    const previous = [makePos({ entryId: 2, position: 2, gapToAheadMs: 1000 })];
    const alerts = computeProximityAlerts(current, previous);
    expect(alerts.size).toBe(0);
    expect(PROXIMITY_CLOSING_DELTA_MS).toBe(500);
  });
});

describe('computeBackmarkers', () => {
  it('computeBackmarkers_flagsCarsOnLowerLapCount', () => {
    const current = [
      makePos({ entryId: 1, lapsCompleted: 12 }),
      makePos({ entryId: 2, lapsCompleted: 11 }),
      makePos({ entryId: 3, lapsCompleted: 10 }),
    ];
    const backmarkers = computeBackmarkers(current);
    expect(backmarkers).toEqual(new Set([2, 3]));
  });

  it('computeBackmarkers_excludesLeaderAndCarsOnLeadLap', () => {
    const current = [
      makePos({ entryId: 1, lapsCompleted: 15 }),
      makePos({ entryId: 2, lapsCompleted: 15 }),
    ];
    const backmarkers = computeBackmarkers(current);
    expect(backmarkers.size).toBe(0);
  });
});
