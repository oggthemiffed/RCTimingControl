# Plan 04-07 Summary

**Status:** Complete  
**Requirements:** CTRL-04, OFFICIAL-03, OFFICIAL-04, CTRL-08 (result persistence), D-22

## What was built

### Backend
- `V19__phase4_result_snapshots.sql` — `result_snapshots` table with JSONB positions and lap_history columns
- `ResultSnapshot` entity + `ResultSnapshotRepository` — JPA persistence
- `ResultSnapshotService` — called by `RaceStateMachineService` on RUNNING/STOPPED → FINISHED transition; calculates final positions including TIME penalties
- `ResultSnapshotQuery` — jOOQ read projection returning `ResultSnapshotDto`
- `ResultSnapshotController` — `GET /api/v1/race-control/race/{raceId}/result-snapshot`, accessible to all authenticated users
- `RefereeController` — `POST .../incident-report` (OFFICIAL-03), `POST .../penalty` (OFFICIAL-04, LAP type updates live state + rebroadcasts), `POST .../marshal-absent` + `POST .../apply-marshal-penalty` (D-22)
- DTOs: `IncidentReportRequest`, `PenaltyRequest`, `MarshalAbsenceRequest`

### Frontend — API layer
- `raceControlApi.ts` extended with: `RunOrderItemDto`, `ResultSnapshotDto` (with nested `ResultRow`, `PositionAtLap`, `ClubBrandingDto`), `LiveTimingRowDto`, `RaceStateChangeDto`, mutation request types; API functions for all lifecycle commands + referee endpoints

### Frontend — hooks
- `useRunOrder.ts` — TanStack Query, 10 s stale, 15 s refetch interval
- `useResultSnapshot.ts` — TanStack Query, 30 s stale
- `useRaceStateMutations.ts` — mutations: callGrid, start, stop, abandon (invalidates runOrder + snapshot), marshalAdj, incident, penalty, marshalAbsent
- `useStomp.ts` — `@stomp/stompjs` Client wrapper; JWT passed in STOMP CONNECT headers; exposes `{ data, status }` where status is `disconnected | connecting | connected | error`; reconnects at 5 s intervals

### Frontend — pages and panels
- `RaceControlLayout.tsx` — full-height header layout at `/race-control/event/:eventId`; Cockpit + Referee nav links; ProtectedRoute for RACE_DIRECTOR/REFEREE/ADMIN
- `CockpitPage.tsx` — run order sidebar (w-56) + state-dependent main panel: PENDING → Call Grid button, GRID → GridEditorPanel, RUNNING → LiveTimingPanel + Stop/Abandon, STOPPED → LiveTimingPanel + Resume/Abandon, FINISHED → FinishedPanel; auto-selects first non-FINISHED race on load
- `RefereePage.tsx` — same run order sidebar + RefereeTimingTable (STOMP-fed) + Raise Incident / Apply Penalty buttons; auto-selects first active race
- `PrintResultsPage.tsx` — print-friendly results table at `/race-control/event/:eventId/results/:raceId`; club branding header; browser print button
- `RunOrderPanel.tsx` — clickable race list with status badges (LIVE/GRID/STOPPED/DONE)
- `GridEditorPanel.tsx` — wraps PreRaceReadinessPanel with a Start Race button
- `LiveTimingPanel.tsx` — STOMP-powered position table with WebSocket status indicator dot; formats lap times as M:SS.mmm
- `FinishedPanel.tsx` — result snapshot table + Print link to PrintResultsPage
- `IncidentDialog.tsx` — form (entryId, incident type select, description) validated with Zod
- `PenaltyDialog.tsx` — form (entryId, LAP/TIME radio, value, reason) validated with Zod
- `RoundGeneratorWizard.tsx` — placeholder dialog directing users to Admin panel (round generation is an admin-phase feature)

### Routes wired in App.tsx
```
/race-control/event/:eventId          → CockpitPage
/race-control/event/:eventId/referee  → RefereePage
/race-control/event/:eventId/results/:raceId → PrintResultsPage
```

## Key decisions
- STOMP JWT is passed in CONNECT headers (not HTTP upgrade) per the CLAUDE.md architecture note and Pitfall 1 in RESEARCH.md
- Live positions are never stored during a race; result snapshot is only persisted on FINISHED transition
- LAP penalty immediately applies to in-memory LiveRaceState and rebroadcasts; TIME penalty is deferred to snapshot computation
- Chart.js was not installed — the print results page uses a plain HTML table; position-by-lap chart is deferred to Phase 7 (results & standings)
- RoundGeneratorWizard is a stub — round generation has no backend endpoint in Phase 4 and is deferred to the Admin panel phase
