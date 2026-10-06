---
phase: 04-race-state-machine
plan: 03
subsystem: service
tags: [java, spring, heat-generation, seeding, qualifying, bump-up, mockito, junit5]

requires:
  - phase: 04-race-state-machine/plan-02
    provides: Round, Race, RaceEntry JPA entities and repositories; RoundType/RoundStatus/RaceStatus/StartType enums

provides:
  - RoundGeneratorService: heat-splitting + snake-draft + round sequencing + empty finals grid creation
  - BumpUpSeedingService: full finals seeding from qualifying standings + bump-up slot fill after lower final
  - QualifyingStandingsService: FTQ (fastest-time-quality) standings sort from pre-computed results
  - RoundGenerationRequest + RoundPreviewDto DTOs with validation annotations
  - RoundGeneratorServiceTest: 2 unit tests (heat split, bump-up seeding) — both passing

affects: [04-race-state-machine/plan-05, 04-race-state-machine/plan-07, round-generator-wizard, race-control-api]

tech-stack:
  added: []
  patterns:
    - "Snake-draft heat assignment: seed 1 → heat 1, seed 2 → heat 2, ..., heat N, N-1, ..., 1 (zig-zag)"
    - "Finals seeding: lowest final gets worst N qualifiers; higher finals get next block + bump slots"
    - "BumpUpSeedingService.applyBumpUpResults: C→B→A promotion chain using finalLetter char arithmetic"
    - "QualifyingStandingsService accepts pre-computed results (lapsCompleted DESC, bestLapMs ASC) — caller extracts from LiveRaceState"

key-files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/service/RoundGeneratorService.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/BumpUpSeedingService.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/QualifyingStandingsService.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/dto/RoundGenerationRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/dto/RoundPreviewDto.java
  modified:
    - app/src/test/java/dev/monkeypatch/rctiming/service/RoundGeneratorServiceTest.java

key-decisions:
  - "QualifyingStandingsService takes pre-computed QualifyingResult list rather than querying DB — caller (plan 05 state machine) extracts from LiveRaceState result snapshots. Defers DB result schema to plan 05."
  - "BumpUpSeedingService.seedFinals deletes existing placeholder RaceEntry rows before reinserting seeded ones, avoiding partial-state race conditions."
  - "Snake draft implemented as zig-zag pass: even passes fill heats 0..N-1 left-to-right; odd passes fill N-1..0 right-to-left."
  - "finalLetter promotion uses char arithmetic: 'C' - 1 = 'B', 'B' - 1 = 'A'; throws on 'A' (no higher final)."

patterns-established:
  - "Heat assignment fixed at generation time — same drivers in Heat 1 across all Practice and Qualifying rounds"
  - "Finals grids created empty (entryId=0 placeholder) by RoundGeneratorService; seeded by BumpUpSeedingService after qualifying closes"
  - "Bump slots created with bumped=true, entryId=0 at positions (carsPerFinal - bumpCount + 1)..carsPerFinal"

requirements-completed: [CTRL-01]

duration: 35min
completed: 2026-04-24
---

# Phase 4 Plan 03: Round Generator + Bump-Up Seeding Summary

**Snake-draft heat generator, bump-up finals seeder, and FTQ qualifying standings service — three pure service-layer classes with two passing Mockito unit tests**

## Performance

- **Duration:** ~35 min
- **Started:** 2026-04-24T (continuation — Task 1 was already committed)
- **Completed:** 2026-04-24
- **Tasks:** 2 (Task 1 pre-completed; Task 2 executed this session)
- **Files modified:** 3 new + 2 modified (Task 1); 1 new + 1 modified (Task 2)

## Accomplishments

- `QualifyingStandingsService` with `recalculateStandings(eventClassId, results)` — FTQ sort (laps DESC, best lap ASC per FORMAT-09)
- `BumpUpSeedingService` fully implemented: `seedFinals` fills all finals grids from qualifying standings in lowest-to-highest order; `applyBumpUpResults` chains C→B→A bump promotions using char arithmetic
- `RoundGeneratorServiceTest` enabled with full Mockito wiring — both test methods pass:
  - `heatSplit_fifteenDriversMaxEightPerHeat_createsTwoHeats` verifies ceil(15/8)=2 heats totalling 15 drivers
  - `bumpUpSeeding_topNofBFinal_appendedToAFinal` verifies bump slots 9+10 filled with entryIds 501/502

## Task Commits

1. **Task 1: RoundGeneratorService + DTOs** - `6f78ed3` (feat — pre-committed to master before this session)
2. **Task 2: QualifyingStandingsService + enable RoundGeneratorServiceTest** - `b085c19` (feat)

## Files Created/Modified

- `app/src/main/java/dev/monkeypatch/rctiming/service/RoundGeneratorService.java` — snake-draft heat assignment, round/race/entry generation, preview without persist
- `app/src/main/java/dev/monkeypatch/rctiming/service/BumpUpSeedingService.java` — seedFinals (qualifying→finals) + applyBumpUpResults (C→B→A promotion)
- `app/src/main/java/dev/monkeypatch/rctiming/service/QualifyingStandingsService.java` — FTQ standings sort, nested QualifyingResult record
- `app/src/main/java/dev/monkeypatch/rctiming/service/dto/RoundGenerationRequest.java` — input DTO with @Min/@Max/@NotNull validation
- `app/src/main/java/dev/monkeypatch/rctiming/service/dto/RoundPreviewDto.java` — preview row record
- `app/src/test/java/dev/monkeypatch/rctiming/service/RoundGeneratorServiceTest.java` — enabled, fully wired, 2 tests passing

## Decisions Made

- `QualifyingStandingsService` accepts pre-computed `QualifyingResult` list (not a DB query) — the plan 05 state machine will extract these from `LiveRaceState` result snapshots and pass them in. This keeps the service pure/testable without requiring result persistence to be designed now.
- `seedFinals` deletes existing RaceEntry placeholder rows before reinserting seeded entries to avoid stale placeholder records in the grid.
- `bumpUpSeeding_topNofBFinal_appendedToAFinal` test instantiates `BumpUpSeedingService` directly with its own `@Mock` repos (not via `@InjectMocks`) to avoid coupling the bump-up test to the full `RoundGeneratorService` dependency graph.

## Deviations from Plan

None — plan executed exactly as written. The BumpUpSeedingService stub created in Task 1 was already a full implementation (not left as stub), so Task 2 required only QualifyingStandingsService creation and test enablement.

## Issues Encountered

- jOOQ codegen triggered on every test run (no up-to-date check on migration changes). Resolved by starting a temporary postgres:16-alpine container and passing `JOOQ_JDBC_URL` env var to skip Docker lifecycle. Existing `PreRaceReadinessQuery.java` has unrelated jOOQ column reference errors that appear as noise — these are pre-existing issues outside this plan's scope and logged to deferred items.

## Known Stubs

- `QualifyingStandingsService.recalculateStandings` accepts caller-provided results rather than reading from DB. This is intentional — plan 05 (race state machine integration) will extract results from `LiveRaceState` and pass them in. The method signature is final.
- `BumpUpSeedingService.seedFinals` uses `entryId=0L` as a placeholder in bump slot rows (created by `RoundGeneratorService`). These are replaced by real entryIds when `applyBumpUpResults` fires after the lower final completes.

## Next Phase Readiness

- Round generator and bump-up seeding are ready for plan 05 (race control API) to wire into state machine transitions
- `applyPreviousRoundFinishingOrder(raceId, finishingOrder)` on `RoundGeneratorService` is ready to be called from the QUALIFIER→FINISHED transition
- `BumpUpSeedingService.seedFinals` ready to be called after last qualifying round closes
- `BumpUpSeedingService.applyBumpUpResults` ready to be called after each lower final finishes

---
*Phase: 04-race-state-machine*
*Completed: 2026-04-24*
