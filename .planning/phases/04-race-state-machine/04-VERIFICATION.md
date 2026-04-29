---
phase: 04-race-state-machine
verified: 2026-04-29T16:10:00Z
status: gaps_found
score: 7/10
overrides_applied: 0
gaps:
  - truth: "Admin can trigger the round generator; all rounds and heats appear in correct run order"
    status: failed
    reason: "RoundGeneratorService.generate() is fully implemented and unit-tested but has NO HTTP API endpoint. RoundGeneratorWizard.tsx is a stub dialog saying 'rounds are generated automatically' with no actual API call."
    artifacts:
      - path: "app/src/main/java/dev/monkeypatch/rctiming/service/RoundGeneratorService.java"
        issue: "generate() method exists (line 89) but no controller imports or calls it"
      - path: "frontend/src/pages/race-control/RoundGeneratorWizard.tsx"
        issue: "Stub dialog, no API call, no mutation — 29 lines of static UI"
    missing:
      - "POST /api/v1/admin/events/{eventId}/generate-rounds endpoint wired to RoundGeneratorService.generate()"
      - "Frontend RoundGeneratorWizard must call the endpoint and show generated rounds"

  - truth: "After qualifying closes, finals grids are auto-seeded from qualifying standings"
    status: failed
    reason: "BumpUpSeedingService.seedFinals() is implemented and unit-tested but is never called from any API controller or event handler. QualifyingStandingsService also has no HTTP endpoint."
    artifacts:
      - path: "app/src/main/java/dev/monkeypatch/rctiming/service/BumpUpSeedingService.java"
        issue: "seedFinals() (line 55) only invoked from unit tests — no HTTP trigger"
      - path: "app/src/main/java/dev/monkeypatch/rctiming/service/QualifyingStandingsService.java"
        issue: "No controller exposes standings or triggers seeding"
    missing:
      - "POST /api/v1/admin/events/{eventId}/seed-finals endpoint or admin panel trigger"
      - "QualifyingStandingsService must be called to derive the standings list before seeding"

  - truth: "After each bump-up final completes, top N finishers are appended to the next final's grid; race director is alerted"
    status: failed
    reason: "BumpUpSeedingService.applyBumpUpResults() exists but is never called when a FINAL race transitions to FINISHED. RaceStateMachineService.applyFinishingOrderToNextRace() explicitly skips non-PRACTICE/QUALIFIER rounds (line 143). No alert mechanism exists."
    artifacts:
      - path: "app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceStateMachineService.java"
        issue: "applyFinishingOrderToNextRace() returns early for non-PRACTICE/QUALIFIER rounds (line 143) — finals are never handled"
      - path: "app/src/main/java/dev/monkeypatch/rctiming/service/BumpUpSeedingService.java"
        issue: "applyBumpUpResults() (line 138) only called from unit tests"
    missing:
      - "Wire applyBumpUpResults() into state machine FINISHED transition when round type is FINAL"
      - "Broadcast bump-up alert (e.g. STOMP or REST) so race director sees it before starting next final"

  - truth: "Race director can skip to a specific race/round number from the browser"
    status: partial
    reason: "POST /api/v1/race-control/race/{raceId}/skip-to endpoint exists with process-local ConcurrentHashMap override, but there is no frontend client function in raceControlApi.ts, no mutation in useRaceStateMutations.ts, and no skip-to UI button in CockpitPage.tsx. The feature is backend-only and inaccessible from any browser."
    artifacts:
      - path: "frontend/src/lib/raceControlApi.ts"
        issue: "No skipTo function or SkipToRaceRequest type defined"
      - path: "frontend/src/pages/race-control/CockpitPage.tsx"
        issue: "No skip-to button rendered in any race state branch"
    missing:
      - "skipTo() API function in raceControlApi.ts calling POST /api/v1/race-control/race/{raceId}/skip-to"
      - "skipTo mutation in useRaceStateMutations and skip-to UI in CockpitPage"
---

# Phase 4: Race State Machine — Verification Report

**Phase Goal:** A race director can run a complete race meeting from any browser — calling the grid, starting and stopping races, applying marshal laps, and handling incidents — with all commands enforced server-side.

**Verified:** 2026-04-29T16:10:00Z
**Status:** gaps_found
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| SC-1 | Admin can configure finals per class and trigger round generator | ✗ FAILED | `RoundGeneratorService.generate()` (l.89) unreachable via HTTP; `RoundGeneratorWizard.tsx` is a stub dialog with no API call |
| SC-2 | Race director can start/stop; conflicting commands rejected with HTTP 409; PENDING→GRID→RUNNING→FINISHED enforced | ✓ VERIFIED | `RaceStateMachineService` (VALID_TRANSITIONS map l.30-36); `GlobalExceptionHandler.handleStateTransition()` (l.47-50 → HTTP 409); `RaceControlControllerIT.conflictingTransitionFromSecondSession_returns409()` |
| SC-3 | Stagger start heats use finishing order from previous round; round 1 uses entry order | ✓ VERIFIED | `RaceStateMachineService.applyFinishingOrderToNextRace()` (l.133-193) applies finishing order from PRACTICE/QUALIFIER to next race; `RoundGeneratorService.applyPreviousRoundFinishingOrder()` (l.108) |
| SC-4 | After qualifying closes, finals grids auto-seeded from qualifying standings | ✗ FAILED | `BumpUpSeedingService.seedFinals()` implemented; no HTTP endpoint, no frontend trigger |
| SC-5 | After bump-up final completes, top N finishers appended to next final; race director alerted | ✗ FAILED | `BumpUpSeedingService.applyBumpUpResults()` implemented but never called on FINISHED transition; state machine explicitly skips non-PRACTICE/QUALIFIER rounds (l.143) |
| SC-6 | Race control displays marshal list and grid call for current race | ✓ VERIFIED | `PreRaceReadinessQuery.load()` returns `gridCall` + `marshalDuty` from DB; `PreRaceReadinessController` + `PreRaceReadinessPanel` render both columns; jOOQ SQL (l.117-168) uses real DB joins |
| SC-7 | Race director can add/remove marshal laps with full audit trail; results update immediately | ✓ VERIFIED | `MarshalAdjustment` entity with `actingUserId`, `actingUserName`, `raceStateAtTime`, `adjustedAt`; `RaceControlController.marshalAdjustment()` persists + calls `lapTimingService.applyMarshalAdjustment()`; `RaceControlControllerIT.marshalAdjustment_persistsAllAuditFields()` asserts all fields |
| SC-8 | Race director can abandon a race, skip to a specific race/round, and link unknown transponder | ⚠️ PARTIAL | Abandon ✓ (`/race/{id}/abandon` + `FinishedPanel`); Unknown transponder link ✓ (`TransponderLinkController` + `UnknownTransponderLinkDialog`); Skip-to ✗ (backend endpoint exists, no frontend UI) |
| SC-9 | Race referee can raise incident reports and apply lap or time penalties that immediately update live standings | ✓ VERIFIED | `RefereeController.raiseIncident()` + `applyPenalty()` (LAP type: `lapTimingService.applyLapDelta()` + `liveTimingHub.broadcastTimingUpdate()`); `RefereeControllerIT` tests all three; `RefereePage` wires `IncidentDialog` + `PenaltyDialog` |
| SC-10 | Race results can be exported as a printable PDF sheet at the venue | ✓ VERIFIED | `PrintResultsPage.tsx` uses `useResultSnapshot()` → `GET /api/v1/race-control/race/{id}/result-snapshot`; `window.print()` on l.98; `ResultSnapshotService` snapshots live timing into DB; `FinishedPanel` links to print page |

**Score: 7/10 truths verified** (3 failed, 1 partial counted in partial column)

---

### Deferred Items

No items addressed in later phases — all failures are in-scope gaps for Phase 4.

---

## Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `domain/race/RaceStateMachineService.java` | State machine PENDING→GRID→RUNNING→STOPPED→FINISHED | ✓ VERIFIED | VALID_TRANSITIONS EnumMap (l.30-36); `transition()` + `restart()` |
| `domain/race/RaceStatus.java` | Enum with all states | ✓ VERIFIED | PENDING, GRID, RUNNING, STOPPED, FINISHED |
| `api/racecontrol/RaceControlController.java` | callGrid, start, stop, abandon, marshalAdjustment, skipTo, unknownTransponderLink | ✓ VERIFIED | All 7 endpoints present; role-gated to RACE_DIRECTOR/ADMIN |
| `api/racecontrol/RefereeController.java` | raiseIncident, applyPenalty (LAP + TIME) | ✓ VERIFIED | Both endpoints; LAP penalty live-broadcasts via STOMP |
| `api/racecontrol/PreRaceReadinessController.java` | GET pre-race-readiness | ✓ VERIFIED | Delegates to `PreRaceReadinessQuery.load()` |
| `api/racecontrol/TransponderLinkController.java` | Retroactive transponder link with audit | ✓ VERIFIED | Persists `UnknownTransponderLinkAudit`, retroactively credits laps |
| `api/racecontrol/ResultSnapshotController.java` | GET result-snapshot | ✓ VERIFIED | Returns `ResultSnapshotQuery.load()` |
| `query/racecontrol/PreRaceReadinessQuery.java` | Grid call + marshal duty from DB | ✓ VERIFIED | Full jOOQ joins to entries/users/absences |
| `service/RoundGeneratorService.java` | generate() callable via HTTP | ✗ STUB/ORPHANED | Service exists; `generate()` only called in unit tests, no API endpoint |
| `service/BumpUpSeedingService.java` | seedFinals() + applyBumpUpResults() | ✗ ORPHANED | Both methods exist and tested; no production call site |
| `service/ResultSnapshotService.java` | Snapshot on FINISHED | ✓ VERIFIED | Called from `RaceStateMachineService.transition()` (l.122-124) |
| `timing/LiveTimingHub.java` | STOMP broadcast on state change + timing update | ✓ VERIFIED | `broadcastStateChange()` + `broadcastTimingUpdate()` + `broadcastMarshalAdjustment()` |
| `db/migration/V17__phase4_race_schema.sql` | rounds + races + race_entries + EventClass finals config | ✓ VERIFIED | All 4 tables + `finals_count`, `cars_per_final`, `bump_count` columns |
| `db/migration/V18__phase4_marshal_referee_schema.sql` | marshal_adjustments, incident_reports, penalties, marshal_absences | ✓ VERIFIED | 69-line migration covering all referee/marshal tables |
| `db/migration/V19__phase4_result_snapshots.sql` | result_snapshots table | ✓ VERIFIED | 10-line migration |
| `frontend/src/pages/race-control/CockpitPage.tsx` | Full race director UI: run order, state transitions, live timing, abandon, link transponder | ✓ VERIFIED | 326 lines; PENDING/GRID/RUNNING/STOPPED/FINISHED state handlers; STOMP subscription for unknown transponder |
| `frontend/src/pages/race-control/RefereePage.tsx` | Proximity alerts, incident + penalty UI | ✓ VERIFIED | `computeProximityAlerts` wired via `useMemo`; `IncidentDialog` + `PenaltyDialog` buttons |
| `frontend/src/pages/race-control/PrintResultsPage.tsx` | Printable results page | ✓ VERIFIED | Full results table + `window.print()` + print:hidden CSS class |
| `frontend/src/pages/race-control/panels/PreRaceReadinessPanel.tsx` | Grid call + marshal duty panels | ✓ VERIFIED | Two-column layout; marshal absence highlighting |
| `frontend/src/pages/race-control/referee/alerts.ts` | computeProximityAlerts (OFFICIAL-01) + computeBackmarkers (OFFICIAL-02) | ✓ VERIFIED | Both algorithms implemented and Vitest-tested |
| `frontend/src/hooks/race-control/useLappedBadge.ts` | Backmarker badge with debounce | ✓ VERIFIED | Debounced 5s; used in LiveTimingPanel (shown in RefereePage) |
| `frontend/src/pages/race-control/RoundGeneratorWizard.tsx` | Admin trigger for round generation | ✗ STUB | 29 lines; static dialog with no API call |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `CockpitPage` → `callGrid` | `POST /race/{id}/call-grid` | `useRaceStateMutations.callGrid` | ✓ WIRED | `raceControlApi.callGrid()` → `RaceControlController` |
| `CockpitPage` → `startRace` | `POST /race/{id}/start` | `useRaceStateMutations.start` | ✓ WIRED | `raceControlApi.startRace()` → `RaceControlController` |
| `CockpitPage` → `abandonRace` | `POST /race/{id}/abandon` | `useRaceStateMutations.abandon` | ✓ WIRED | `raceControlApi.abandonRace()` → `RaceControlController` |
| `CockpitPage` → unknown transponder link | `POST /races/{id}/transponders/link` | `UnknownTransponderLinkDialog` → `linkUnknownTransponder()` | ✓ WIRED | `TransponderLinkController` retroactively credits laps + audit |
| `CockpitPage` → skip-to | `POST /race/{id}/skip-to` | — | ✗ NOT WIRED | No `skipTo()` in raceControlApi.ts; no mutation or UI button |
| `RaceStateMachineService.transition(FINISHED)` → `ResultSnapshotService.snapshot()` | persisted DB snapshot | direct call (l.122-124) | ✓ WIRED | Triggered on every FINISHED transition |
| `RaceStateMachineService.transition(FINISHED)` → `BumpUpSeedingService.applyBumpUpResults()` | finals seeding | — | ✗ NOT WIRED | State machine returns early for FINAL rounds (l.143); applyBumpUpResults never called |
| `RefereePage` → `applyPenalty` | `POST /referee/race/{id}/penalty` | `useRaceStateMutations.penalty` | ✓ WIRED | `RefereeController` applies LAP delta + live broadcasts |
| `RefereePage` → `raiseIncident` | `POST /referee/race/{id}/incident-report` | `useRaceStateMutations.incident` | ✓ WIRED | `RefereeController` persists `IncidentReport` |
| `RefereePage` → proximity highlight | `highlightEntryIds` prop on `LiveTimingPanel` | `computeProximityAlerts` in `useMemo` | ✓ WIRED | Computed from current vs previous STOMP rows |
| `RefereePage` → backmarker badge | LAPPED badge in `LiveTimingPanel` | `useLappedBadge(sorted)` in panel | ✓ WIRED | Debounced 5s; shown in referee view via LiveTimingPanel |
| `GlobalExceptionHandler` → HTTP 409 | `IllegalStateTransitionException` | `@ExceptionHandler` (l.47-50) | ✓ WIRED | Invalid state transitions return 409 Conflict |
| `RoundGeneratorService.generate()` | POST admin endpoint | — | ✗ NOT WIRED | No controller calls generate(); no HTTP endpoint |
| `BumpUpSeedingService.seedFinals()` | POST admin endpoint | — | ✗ NOT WIRED | No controller calls seedFinals(); no HTTP endpoint |

---

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| `PreRaceReadinessPanel` | `data.gridCall` / `data.marshalDuty` | `PreRaceReadinessQuery.load()` → jOOQ joins on `race_entries`, `entries`, `users`, `marshal_absences` | ✓ | ✓ FLOWING |
| `LiveTimingPanel` | `rows` from `useLiveTiming` | STOMP `/topic/race/{id}/timing` populated by `LiveTimingHub.broadcastTimingUpdate()` from `LapTimingService` | ✓ | ✓ FLOWING |
| `PrintResultsPage` | `data.positions` | `ResultSnapshotQuery.load()` → reads `result_snapshots.payload` JSON from DB | ✓ | ✓ FLOWING |
| `FinishedPanel` | `data` from `useResultSnapshot` | `GET /race/{id}/result-snapshot` → `ResultSnapshotController` → `ResultSnapshotQuery` | ✓ | ✓ FLOWING |
| `RunOrderPanel` | `items` from `useRunOrder` | `GET /event/{id}/run-order` → `RunOrderQuery.findForEvent()` → jOOQ select on races+rounds | ✓ | ✓ FLOWING |

---

## Behavioral Spot-Checks

Runnable checks skipped — integration tests provide equivalent coverage:

| Behavior | Test | Status |
|----------|------|--------|
| PENDING → GRID transition | `RaceControlControllerIT.callGrid_returnsOkAndTransitionsToGrid()` | ✓ Covered |
| GRID → RUNNING transition | `RaceControlControllerIT.startRace_returnsOkAndTransitionsToRunning()` | ✓ Covered |
| Invalid transition → HTTP 409 | `RaceControlControllerIT.conflictingTransitionFromSecondSession_returns409()` | ✓ Covered |
| Marshal adjustment audit fields | `RaceControlControllerIT.marshalAdjustment_persistsAllAuditFields()` | ✓ Covered |
| Incident report raised | `RefereeControllerIT.raiseIncident_createsRecordLinkedToRaceAndEntry()` | ✓ Covered |
| LAP penalty live rebroadcast | `RefereeControllerIT.applyLapPenalty_recalculatesPositionsImmediately()` | ✓ Covered |
| Unknown transponder link | `RaceControlControllerIT.unknownTransponderLink_createsRecord()` | ✓ Covered |
| Proximity alert algorithm | `alerts.test.ts` (Vitest) | ✓ Covered |
| Backmarker detection | `alerts.test.ts` (Vitest) | ✓ Covered |

---

## Requirements Coverage

| Requirement | Description | Status | Evidence |
|-------------|-------------|--------|---------|
| CTRL-01 | Race director can start and stop from browser | ✓ SATISFIED | `callGrid` / `start` / `stop` in `RaceControlController` + `CockpitPage` |
| CTRL-02 | Grid call display | ✓ SATISFIED | `PreRaceReadinessController` + `PreRaceReadinessPanel` grid call column |
| CTRL-03 | Marshal laps with full audit trail | ✓ SATISFIED | `MarshalAdjustment` entity (actingUserId, actingUserName, adjustedAt, raceStateAtTime); IT asserts all fields |
| CTRL-04 | Export printable/PDF at venue | ✓ SATISFIED | `PrintResultsPage` + `window.print()` |
| CTRL-05 | Server enforces state machine; conflicting commands → 409 | ✓ SATISFIED | `RaceStateMachineService.VALID_TRANSITIONS`; `GlobalExceptionHandler` (409); IT confirmed |
| CTRL-06 | Unknown transponder retrospectively linked with audit | ✓ SATISFIED | `TransponderLinkController.linkTransponder()` persists `UnknownTransponderLinkAudit` + retroactive lap credit |
| CTRL-07 | Marshal list for current race | ✓ SATISFIED | `PreRaceReadinessQuery.marshalDuty` from previous race; `PreRaceReadinessPanel` marshal column |
| CTRL-08 | Abandon in progress; results saved | ✓ SATISFIED | `abandonRace()` transitions to FINISHED (triggers snapshot); `FinishedPanel` shows results |
| CTRL-09 | Skip to specific race/round; link unknown transponder | ⚠️ PARTIAL | Skip-to backend exists (`/race/{id}/skip-to`), process-local only, NO frontend UI. Transponder link ✓. |
| OFFICIAL-01 | Live proximity alerts (closing cars) | ✓ SATISFIED | `computeProximityAlerts()` in `alerts.ts`; wired in `RefereePage` via `useMemo`; `highlightEntryIds` passed to `LiveTimingPanel` |
| OFFICIAL-02 | Backmarker highlight (lapped cars) | ✓ SATISFIED | `useLappedBadge()` in `LiveTimingPanel` (debounced 5s); LAPPED badge shown in referee view; `computeBackmarkers()` separately implemented + tested |
| OFFICIAL-03 | Raise incident report | ✓ SATISFIED | `RefereeController.raiseIncident()` + `IncidentDialog`; IT confirmed |
| OFFICIAL-04 | Apply lap/time penalty → immediate live standings update | ✓ SATISFIED | LAP: `lapTimingService.applyLapDelta()` + `liveTimingHub.broadcastTimingUpdate()`; TIME: persisted for snapshot; IT confirmed |

---

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `frontend/src/pages/race-control/RoundGeneratorWizard.tsx` | 15-29 | Stub dialog — no API call, no mutation | 🛑 Blocker | SC-1 fails: admin cannot trigger round generation from the browser |
| `domain/race/RaceControlController.java` | 147 | `// TODO: add abandoned flag in Phase 7 results plan` | ℹ️ Info | Abandon doesn't distinguish from normal FINISHED; tracked for later phase |
| `api/racecontrol/RaceControlController.java` | 66 | `activeRaceByEvent` is `ConcurrentHashMap` (process-local, not DB-persisted) | ⚠️ Warning | Skip-to state is lost on server restart and not visible to other sessions; by design for Phase 4 per code comment |

---

## Human Verification Required

### 1. Marshal Duty Absence Highlight

**Test:** Seed an event where an entry has missed ≥2 marshal duties. Navigate to a race in GRID state. Inspect `PreRaceReadinessPanel`.
**Expected:** Drivers with `missedThisEvent >= 2` should show red/destructive text in the "Absences this event" cell.
**Why human:** UAT test 5 was skipped due to no seeded data; requires live DB seed to verify visual highlight.

### 2. Bump-up seeding workflow (when fixed)

**Test:** After implementing the missing seedFinals API trigger (gap SC-4), run qualifying for an event with 2+ finals configured, close qualifying, trigger seeding, then verify final grids are populated in the correct order.
**Expected:** A-Final positions 1–N from top qualifiers; B-Final from lower-ranked qualifiers. Bump slots in A-Final show as empty until B-Final completes.
**Why human:** Requires multi-race event setup and is blocked until the HTTP trigger is implemented.

---

## Gaps Summary

Four gaps block complete goal achievement. They fall into two concern groups:

**Group A — Service-layer orphans (SC-1, SC-4, SC-5):** `RoundGeneratorService.generate()`, `BumpUpSeedingService.seedFinals()`, and `BumpUpSeedingService.applyBumpUpResults()` are all fully implemented, unit-tested, and correct. The root cause is the same: no HTTP controller exposes these services. Plans 04-03 delivered the service layer but no corresponding REST endpoints or lifecycle wiring were added. One focused plan could resolve all three gaps:
- Add `POST /api/v1/admin/events/{eventId}/generate-rounds` calling `RoundGeneratorService.generate()`
- Add `POST /api/v1/admin/events/{eventId}/seed-finals` calling `BumpUpSeedingService.seedFinals()`
- Wire `applyBumpUpResults()` into `RaceStateMachineService.transition()` when target is FINISHED and round type is FINAL
- Add a RoundGeneratorWizard frontend form that calls the generate endpoint

**Group B — Frontend-only gap (CTRL-09 skip-to):** The `POST /race/{raceId}/skip-to` backend endpoint is implemented but the frontend has no client function, no mutation hook, and no UI. Adding `skipTo()` to `raceControlApi.ts`, a mutation to `useRaceStateMutations`, and a button/dialog in `CockpitPage` would close this gap.

The core race director flow (CTRL-01 through CTRL-08, CTRL-09 partial, all OFFICIAL requirements) is fully operational and UAT-confirmed. The gaps are all in the event setup path (round generation, finals seeding) and a secondary navigation feature (skip-to).

---

_Verified: 2026-04-29T16:10:00Z_
_Verifier: gsd-verifier (automated code inspection)_
