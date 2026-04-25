# Plan 04-05 Summary

**Status:** Complete  
**Requirements:** CTRL-02, CTRL-07, OFFICIAL-01, OFFICIAL-02

## What was built

### Frontend — pre-race readiness panel (CTRL-02, CTRL-07)
- `frontend/src/lib/raceControlApi.ts` — `PreRaceReadinessDto`, `GridCallSlotDto`, `MarshalDutyRowDto` types + `getPreRaceReadiness()` Axios call
- `frontend/src/hooks/race-control/raceControlQueryKeys.ts` — query key factory for preRaceReadiness, runOrder, resultSnapshot
- `frontend/src/hooks/race-control/usePreRaceReadiness.ts` — TanStack Query hook, enabled when raceId is non-null, staleTime 5 s
- `frontend/src/pages/race-control/panels/PreRaceReadinessPanel.tsx` — two-column view: Marshal Duty (missed count hidden when 0, destructive when ≥ 2) and Grid Call (position-ordered list)

### Frontend — referee proximity + backmarker logic (OFFICIAL-01, OFFICIAL-02)
- `frontend/src/pages/race-control/referee/alerts.ts` — pure functions `computeProximityAlerts` and `computeBackmarkers`, constants `PROXIMITY_CLOSING_DELTA_MS = 500`, `PROXIMITY_ABSOLUTE_MAX_MS = 3000`
- `frontend/src/pages/race-control/referee/alerts.test.ts` — 6 Vitest tests covering alert fire/no-fire edge cases and backmarker detection; all passing
- `frontend/src/pages/race-control/referee/RefereeTimingTable.tsx` — table with `bg-chart-3/20` row highlight for proximity alerts and `LAPPED` badge (chart-3 outline) for backmarkers

## Key decisions
- Proximity alert fires only when gap has closed by ≥ 500 ms AND current gap ≤ 3 000 ms (prevents trivially large gaps from triggering)
- Backmarker logic uses `Math.max` over lapsCompleted; no threshold — any driver with fewer laps than leader is flagged
- Both algorithms are stateless pure functions consuming two snapshots; no WebSocket dependency for unit testing
