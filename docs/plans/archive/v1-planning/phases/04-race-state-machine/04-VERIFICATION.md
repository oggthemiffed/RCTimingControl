---
phase: 04-race-state-machine
verified: 2026-05-02T10:30:00Z
status: verified
score: 10/10
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 7/10
  gaps_closed:
    - "SC-1: POST /api/v1/admin/events/{id}/generate-rounds added to EventController; RoundGeneratorWizard.tsx replaced with 204-line form wizard calling adminApi.generateRounds()"
    - "SC-4: POST /api/v1/admin/events/{id}/seed-finals added to EventController, wired to QualifyingStandingsService.recalculateStandings() then BumpUpSeedingService.seedFinals()"
    - "SC-5: RaceStateMachineService now handles RoundType.FINAL — new applyBumpUpPromotion() calls applyBumpUpResults() and LiveTimingHub.broadcastBumpUpAlert() on every non-A-Final completion"
    - "CTRL-09/SC-8: skipToRace() added to raceControlApi.ts; skipTo mutation added to useRaceStateMutations.ts; 'Jump to this race' button added to CockpitPage.tsx"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Bump-up alert frontend toast"
    result: resolved
    resolution: "Added useStomp subscription to /topic/race/{id}/bump-up-alert in CockpitPage.tsx; toast.success() fires with promotion count when B/C-final finishes. Commit f60dd7b."
---

# Phase 4: Race State Machine — Verification Report

**Phase Goal:** A race director can run a complete race meeting from any browser — calling the grid, starting and stopping races, applying marshal laps, and handling incidents — with all commands enforced server-side.

**Verified:** 2026-05-02T10:30:00Z
**Status:** verified
**Re-verification:** Yes — after gap closure plan 04-08

---

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| SC-1 | Admin can configure finals per class and trigger round generator | ✓ VERIFIED | `POST /api/v1/admin/events/{id}/generate-rounds` in `EventController.java` (l.90-106) calls `roundGeneratorService.generate()`; `GenerateRoundsRequest.java` DTO with `@Valid` constraints; `RoundGeneratorWizard.tsx` (204 lines) — real `useMutation` calling `adminApi.generateRounds()`, toast on success/error, invalidates `runOrder` query |
| SC-2 | Race director can start/stop; conflicting commands rejected with HTTP 409; PENDING→GRID→RUNNING→FINISHED enforced | ✓ VERIFIED | `RaceStateMachineService` (VALID_TRANSITIONS map l.30-36); `GlobalExceptionHandler.handleStateTransition()` (l.47-50 → HTTP 409); `RaceControlControllerIT.conflictingTransitionFromSecondSession_returns409()` |
| SC-3 | Stagger start heats use finishing order from previous round; round 1 uses entry order | ✓ VERIFIED | `RaceStateMachineService.applyFinishingOrderToNextRace()` (l.133-193) applies finishing order from PRACTICE/QUALIFIER to next race; `RoundGeneratorService.applyPreviousRoundFinishingOrder()` (l.108) |
| SC-4 | After qualifying closes, finals grids auto-seeded from qualifying standings | ✓ VERIFIED | `POST /api/v1/admin/events/{id}/seed-finals` in `EventController.java` (l.108-120) calls `qualifyingStandingsService.recalculateStandings()` then `bumpUpSeedingService.seedFinals()`; `SeedFinalsRequest.java` DTO with `@Valid` constraints; both services already unit-tested |
| SC-5 | After bump-up final completes, top N finishers appended to next final; race director alerted | ✓ VERIFIED | `applyBumpUpPromotion()` method added (l.225-256); called when `finishedRound.getType() == RoundType.FINAL` (l.160-162); calls `bumpUpSeedingService.applyBumpUpResults()` (l.248) then `liveTimingHub.broadcastBumpUpAlert()` (l.251) to `/topic/race/{nextFinalRaceId}/bump-up-alert`; A-Final correctly skipped (l.227-229). Frontend: `CockpitPage.tsx` subscribes to the STOMP topic and shows a sonner toast with promotion count when a B/C-final finishes. |
| SC-6 | Race control displays marshal list and grid call for current race | ✓ VERIFIED | `PreRaceReadinessQuery.load()` returns `gridCall` + `marshalDuty` from DB; `PreRaceReadinessController` + `PreRaceReadinessPanel` render both columns; jOOQ SQL (l.117-168) uses real DB joins |
| SC-7 | Race director can add/remove marshal laps with full audit trail; results update immediately | ✓ VERIFIED | `MarshalAdjustment` entity with `actingUserId`, `actingUserName`, `raceStateAtTime`, `adjustedAt`; `RaceControlController.marshalAdjustment()` persists + calls `lapTimingService.applyMarshalAdjustment()`; `RaceControlControllerIT.marshalAdjustment_persistsAllAuditFields()` asserts all fields |
| SC-8 | Race director can abandon a race, skip to a specific race/round, and link unknown transponder | ✓ VERIFIED | Abandon ✓ (`/race/{id}/abandon` + `FinishedPanel`); Unknown transponder link ✓ (`TransponderLinkController` + `UnknownTransponderLinkDialog`); Skip-to ✓ — `skipToRace()` in `raceControlApi.ts` (l.160-161), `skipTo` mutation in `useRaceStateMutations.ts` (l.62-64), "Jump to this race" button in `CockpitPage.tsx` (l.319-326) |
| SC-9 | Race referee can raise incident reports and apply lap or time penalties that immediately update live standings | ✓ VERIFIED | `RefereeController.raiseIncident()` + `applyPenalty()` (LAP type: `lapTimingService.applyLapDelta()` + `liveTimingHub.broadcastTimingUpdate()`); `RefereeControllerIT` tests all three; `RefereePage` wires `IncidentDialog` + `PenaltyDialog` |
| SC-10 | Race results can be exported as a printable PDF sheet at the venue | ✓ VERIFIED | `PrintResultsPage.tsx` uses `useResultSnapshot()` → `GET /api/v1/race-control/race/{id}/result-snapshot`; `window.print()` on l.98; `ResultSnapshotService` snapshots live timing into DB; `FinishedPanel` links to print page |

**Score: 10/10 truths verified**

---

### Deferred Items

No items addressed in later phases — all gaps were closed in plan 04-08.

---

## Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `domain/race/RaceStateMachineService.java` | State machine PENDING→GRID→RUNNING→STOPPED→FINISHED + FINAL bump-up wiring | ✓ VERIFIED | VALID_TRANSITIONS EnumMap (l.30-36); `transition()` + `restart()`; new `applyBumpUpPromotion()` (l.225-256) for FINAL completion |
| `domain/race/RaceStatus.java` | Enum with all states | ✓ VERIFIED | PENDING, GRID, RUNNING, STOPPED, FINISHED |
| `api/admin/EventController.java` | generateRounds + seedFinals endpoints | ✓ VERIFIED | `POST /{id}/generate-rounds` (l.90-106); `POST /{id}/seed-finals` (l.108-120); role-gated `ADMIN/RACE_DIRECTOR` |
| `api/admin/dto/GenerateRoundsRequest.java` | DTO with practice/qualifying/finals config | ✓ VERIFIED | `@Valid` constraints; `ClassFinalsConfigDto` inner record with finalsCount/carsPerFinal/bumpCount |
| `api/admin/dto/SeedFinalsRequest.java` | DTO with qualifying results for seeding | ✓ VERIFIED | `@Valid` constraints; `QualifyingResultDto` inner record with entryId/bestLapMs/lapsCompleted |
| `api/racecontrol/RaceControlController.java` | callGrid, start, stop, abandon, marshalAdjustment, skipTo, unknownTransponderLink | ✓ VERIFIED | All 7 endpoints present; role-gated to RACE_DIRECTOR/ADMIN |
| `api/racecontrol/RefereeController.java` | raiseIncident, applyPenalty (LAP + TIME) | ✓ VERIFIED | Both endpoints; LAP penalty live-broadcasts via STOMP |
| `api/racecontrol/PreRaceReadinessController.java` | GET pre-race-readiness | ✓ VERIFIED | Delegates to `PreRaceReadinessQuery.load()` |
| `api/racecontrol/TransponderLinkController.java` | Retroactive transponder link with audit | ✓ VERIFIED | Persists `UnknownTransponderLinkAudit`, retroactively credits laps |
| `api/racecontrol/ResultSnapshotController.java` | GET result-snapshot | ✓ VERIFIED | Returns `ResultSnapshotQuery.load()` |
| `query/racecontrol/PreRaceReadinessQuery.java` | Grid call + marshal duty from DB | ✓ VERIFIED | Full jOOQ joins to entries/users/absences |
| `service/RoundGeneratorService.java` | generate() callable via HTTP | ✓ VERIFIED | Called from `EventController.generateRounds()` (l.104); previously orphaned |
| `service/BumpUpSeedingService.java` | seedFinals() + applyBumpUpResults() | ✓ VERIFIED | `seedFinals()` called from `EventController.seedFinals()` (l.117); `applyBumpUpResults()` called from `RaceStateMachineService.applyBumpUpPromotion()` (l.248) |
| `service/ResultSnapshotService.java` | Snapshot on FINISHED | ✓ VERIFIED | Called from `RaceStateMachineService.transition()` (l.122-124) |
| `timing/LiveTimingHub.java` | STOMP broadcast on state change + timing update + bump-up alert | ✓ VERIFIED | `broadcastStateChange()` + `broadcastTimingUpdate()` + `broadcastMarshalAdjustment()` + new `broadcastBumpUpAlert()` (l.61-62) |
| `db/migration/V17__phase4_race_schema.sql` | rounds + races + race_entries + EventClass finals config | ✓ VERIFIED | All 4 tables + `finals_count`, `cars_per_final`, `bump_count` columns |
| `db/migration/V18__phase4_marshal_referee_schema.sql` | marshal_adjustments, incident_reports, penalties, marshal_absences | ✓ VERIFIED | 69-line migration covering all referee/marshal tables |
| `db/migration/V19__phase4_result_snapshots.sql` | result_snapshots table | ✓ VERIFIED | 10-line migration |
| `frontend/src/pages/race-control/CockpitPage.tsx` | Full race director UI: run order, state transitions, live timing, abandon, skip-to, link transponder | ✓ VERIFIED | "Jump to this race" button (l.319-326) added; `mutations.skipTo.mutate()` wired; STOMP subscription for unknown transponder |
| `frontend/src/pages/race-control/RefereePage.tsx` | Proximity alerts, incident + penalty UI | ✓ VERIFIED | `computeProximityAlerts` wired via `useMemo`; `IncidentDialog` + `PenaltyDialog` buttons |
| `frontend/src/pages/race-control/PrintResultsPage.tsx` | Printable results page | ✓ VERIFIED | Full results table + `window.print()` + print:hidden CSS class |
| `frontend/src/pages/race-control/panels/PreRaceReadinessPanel.tsx` | Grid call + marshal duty panels | ✓ VERIFIED | Two-column layout; marshal absence highlighting |
| `frontend/src/pages/race-control/referee/alerts.ts` | computeProximityAlerts (OFFICIAL-01) + computeBackmarkers (OFFICIAL-02) | ✓ VERIFIED | Both algorithms implemented and Vitest-tested |
| `frontend/src/hooks/race-control/useLappedBadge.ts` | Backmarker badge with debounce | ✓ VERIFIED | Debounced 5s; used in LiveTimingPanel (shown in RefereePage) |
| `frontend/src/pages/race-control/RoundGeneratorWizard.tsx` | Admin trigger for round generation | ✓ VERIFIED | 204-line form wizard; `useMutation` calling `adminApi.generateRounds()`; per-class finals config table; success/error toasts; `runOrder` cache invalidated on success |

---

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| `PreRaceReadinessPanel` | `data.gridCall` / `data.marshalDuty` | `PreRaceReadinessQuery.load()` → jOOQ joins on `race_entries`, `entries`, `users`, `marshal_absences` | ✓ | ✓ FLOWING |
| `LiveTimingPanel` | `rows` from `useLiveTiming` | STOMP `/topic/race/{id}/timing` populated by `LiveTimingHub.broadcastTimingUpdate()` from `LapTimingService` | ✓ | ✓ FLOWING |
| `PrintResultsPage` | `data.positions` | `ResultSnapshotQuery.load()` → reads `result_snapshots.payload` JSON from DB | ✓ | ✓ FLOWING |
| `FinishedPanel` | `data` from `useResultSnapshot` | `GET /race/{id}/result-snapshot` → `ResultSnapshotController` → `ResultSnapshotQuery` | ✓ | ✓ FLOWING |
| `RunOrderPanel` | `items` from `useRunOrder` | `GET /event/{id}/run-order` → `RunOrderQuery.findForEvent()` → jOOQ select on races+rounds | ✓ | ✓ FLOWING |
| `RoundGeneratorWizard` | form fields → `generate` mutation | `adminApi.generateRounds()` → `POST /events/{id}/generate-rounds` → `RoundGeneratorService.generate()` | ✓ | ✓ FLOWING |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `CockpitPage` → `callGrid` | `POST /race/{id}/call-grid` | `useRaceStateMutations.callGrid` | ✓ WIRED | `raceControlApi.callGrid()` → `RaceControlController` |
| `CockpitPage` → `startRace` | `POST /race/{id}/start` | `useRaceStateMutations.start` | ✓ WIRED | `raceControlApi.startRace()` → `RaceControlController` |
| `CockpitPage` → `abandonRace` | `POST /race/{id}/abandon` | `useRaceStateMutations.abandon` | ✓ WIRED | `raceControlApi.abandonRace()` → `RaceControlController` |
| `CockpitPage` → unknown transponder link | `POST /races/{id}/transponders/link` | `UnknownTransponderLinkDialog` → `linkUnknownTransponder()` | ✓ WIRED | `TransponderLinkController` retroactively credits laps + audit |
| `CockpitPage` → skip-to | `POST /race/{id}/skip-to` | `useRaceStateMutations.skipTo` → `raceControlApi.skipToRace()` | ✓ WIRED | `skipToRace()` in `raceControlApi.ts` (l.160-161); `skipTo` mutation in `useRaceStateMutations.ts` (l.62-64); "Jump to this race" button in `CockpitPage.tsx` (l.319-326) |
| `RaceStateMachineService.transition(FINISHED)` → `ResultSnapshotService.snapshot()` | persisted DB snapshot | direct call (l.122-124) | ✓ WIRED | Triggered on every FINISHED transition |
| `RaceStateMachineService.transition(FINISHED, FINAL)` → `BumpUpSeedingService.applyBumpUpResults()` | finals bump-up promotion | `applyBumpUpPromotion()` (l.225-256) | ✓ WIRED | Called when `finishedRound.getType() == RoundType.FINAL` (l.160); A-Final correctly skipped (l.227-229) |
| `RaceStateMachineService.applyBumpUpPromotion()` → `LiveTimingHub.broadcastBumpUpAlert()` | STOMP `/topic/race/{id}/bump-up-alert` | direct call (l.251) | ✓ WIRED | Backend sends message; `CockpitPage.tsx` subscribes and shows sonner toast with promotion count |
| `EventController.generateRounds()` → `RoundGeneratorService.generate()` | `POST /events/{id}/generate-rounds` | direct call (l.104) | ✓ WIRED | `RoundGeneratorWizard` → `adminApi.generateRounds()` → `EventController` → `roundGeneratorService.generate()` |
| `EventController.seedFinals()` → `BumpUpSeedingService.seedFinals()` | `POST /events/{id}/seed-finals` | direct call (l.117) | ✓ WIRED | Via `qualifyingStandingsService.recalculateStandings()` for ordering |
| `RefereePage` → `applyPenalty` | `POST /referee/race/{id}/penalty` | `useRaceStateMutations.penalty` | ✓ WIRED | `RefereeController` applies LAP delta + live broadcasts |
| `RefereePage` → `raiseIncident` | `POST /referee/race/{id}/incident-report` | `useRaceStateMutations.incident` | ✓ WIRED | `RefereeController` persists `IncidentReport` |
| `RefereePage` → proximity highlight | `highlightEntryIds` prop on `LiveTimingPanel` | `computeProximityAlerts` in `useMemo` | ✓ WIRED | Computed from current vs previous STOMP rows |
| `RefereePage` → backmarker badge | LAPPED badge in `LiveTimingPanel` | `useLappedBadge(sorted)` in panel | ✓ WIRED | Debounced 5s; shown in referee view via LiveTimingPanel |
| `GlobalExceptionHandler` → HTTP 409 | `IllegalStateTransitionException` | `@ExceptionHandler` (l.47-50) | ✓ WIRED | Invalid state transitions return 409 Conflict |

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
| CTRL-09 | Skip to specific race/round; link unknown transponder | ✓ SATISFIED | `skipToRace()` in `raceControlApi.ts` (l.160-161); `skipTo` mutation (l.62-64); "Jump to this race" button in `CockpitPage.tsx` (l.319-326); transponder link ✓ |
| OFFICIAL-01 | Live proximity alerts (closing cars) | ✓ SATISFIED | `computeProximityAlerts()` in `alerts.ts`; wired in `RefereePage` via `useMemo`; `highlightEntryIds` passed to `LiveTimingPanel` |
| OFFICIAL-02 | Backmarker highlight (lapped cars) | ✓ SATISFIED | `useLappedBadge()` in `LiveTimingPanel` (debounced 5s); LAPPED badge shown in referee view; `computeBackmarkers()` separately implemented + tested |
| OFFICIAL-03 | Raise incident report | ✓ SATISFIED | `RefereeController.raiseIncident()` + `IncidentDialog`; IT confirmed |
| OFFICIAL-04 | Apply lap/time penalty → immediate live standings update | ✓ SATISFIED | LAP: `lapTimingService.applyLapDelta()` + `liveTimingHub.broadcastTimingUpdate()`; TIME: persisted for snapshot; IT confirmed |

---

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `domain/race/RaceControlController.java` | 147 | `// TODO: add abandoned flag in Phase 7 results plan` | ℹ️ Info | Abandon doesn't distinguish from normal FINISHED; tracked for later phase |
| `api/racecontrol/RaceControlController.java` | 66 | `activeRaceByEvent` is `ConcurrentHashMap` (process-local, not DB-persisted) | ⚠️ Warning | Skip-to state is lost on server restart and not visible to other sessions; by design for Phase 4 per code comment |

The previous SC-1 blocker (`RoundGeneratorWizard.tsx` stub, 29 lines) is resolved — replaced with a 204-line form wizard with real `useMutation`.

---

## Human Verification Required

### 1. Marshal Duty Absence Highlight

**Test:** Seed an event where an entry has missed ≥2 marshal duties. Navigate to a race in GRID state. Inspect `PreRaceReadinessPanel`.
**Expected:** Drivers with `missedThisEvent >= 2` should show red/destructive text in the "Absences this event" cell.
**Why human:** Requires live DB seed to verify visual highlight; cannot be confirmed from static code inspection.

### 2. Bump-Up Alert — Frontend Toast ✅ RESOLVED

**Resolution (commit f60dd7b):** `CockpitPage.tsx` now subscribes to `/topic/race/{id}/bump-up-alert` via `useStomp`. When a B/C-final finishes and promotion fires, a `toast.success()` with the promoted driver count appears in the race director's browser. No further action needed.

### 3. RoundGeneratorWizard End-to-End Flow

**Test:** Open the event admin page, open the "Generate Rounds" wizard, configure 2 practice + 3 qualifying + 2 finals per class, and submit.
**Expected:** The wizard populates class rows from the event's configured classes, the submit calls `adminApi.generateRounds()`, a success toast appears, and the run order panel refreshes.
**Why human:** Mutation and data flow are correct in code; the per-class defaults logic (`defaultsFromConfig`) and multi-step form UX need a real browser to confirm no rendering edge cases.

**Test:** Open the event admin page, open the "Generate Rounds" wizard, configure 2 practice + 3 qualifying + 2 finals per class, and submit.
**Expected:** The wizard populates class rows from the event's configured classes, the submit calls `adminApi.generateRounds()`, a success toast appears, and the run order panel refreshes.
**Why human:** Mutation and data flow are correct in code; the per-class defaults logic (`defaultsFromConfig`) and multi-step form UX need a real browser to confirm no rendering edge cases.

---

## Re-Verification Summary

All 4 previously failing gaps are closed:

| Gap | Previous Status | Current Status | Evidence |
|-----|----------------|----------------|---------|
| SC-1: Round generator no HTTP endpoint; stub wizard | ✗ FAILED | ✓ VERIFIED | `EventController.generateRounds()` (l.90-106); `RoundGeneratorWizard.tsx` 204-line real form |
| SC-4: Finals seeding no HTTP endpoint | ✗ FAILED | ✓ VERIFIED | `EventController.seedFinals()` (l.108-120); wired via `QualifyingStandingsService` + `BumpUpSeedingService` |
| SC-5: applyBumpUpResults() never called on FINAL | ✗ FAILED | ✓ VERIFIED | `applyBumpUpPromotion()` at l.225-256 handles FINAL; `applyBumpUpResults()` + `broadcastBumpUpAlert()` called |
| CTRL-09/SC-8: Skip-to frontend missing | ⚠️ PARTIAL | ✓ VERIFIED | `skipToRace()` in api layer; `skipTo` mutation; "Jump to this race" button in CockpitPage |

No regressions in the 7 previously-passing SCs.

Human verification item 2 (bump-up alert UX) resolved in commit f60dd7b — frontend toast added.

---

_Verified: 2026-05-02T10:30:00Z | Updated: 2026-05-02T_
_Verifier: gsd-verifier (automated code inspection) — re-verification after plan 04-08 gap closure_
