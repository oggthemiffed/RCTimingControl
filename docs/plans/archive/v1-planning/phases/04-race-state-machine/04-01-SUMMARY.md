---
phase: 04-race-state-machine
plan: 01
subsystem: testing
tags: [testing, scaffolding, wave-0, race-control, referee]
dependency_graph:
  requires: []
  provides:
    - wave-0 test stubs for RaceStateMachineService (plan 02)
    - wave-0 test stubs for RoundGeneratorService (plan 03)
    - wave-0 IT stubs for RaceControlController (plans 05/06)
    - wave-0 IT stubs for RefereeController (plan 06)
  affects: []
tech_stack:
  added: []
  patterns:
    - JUnit 5 class-level @Disabled for wave 0 stubs
    - @ExtendWith(MockitoExtension.class) for unit tests
    - extends AbstractIntegrationTest for integration tests
key_files:
  created:
    - app/src/test/java/dev/monkeypatch/rctiming/domain/race/RaceStateMachineServiceTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/service/RoundGeneratorServiceTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/racecontrol/RaceControlControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/racecontrol/RefereeControllerIT.java
  modified: []
decisions:
  - "Commented out production class field declarations (not imported) since RaceStateMachineService and RoundGeneratorService do not exist yet; field stubs re-enabled in plans 02/03"
metrics:
  duration: "~6 minutes"
  completed: "2026-04-24T08:05:56Z"
  tasks_completed: 2
  files_created: 4
  files_modified: 0
---

# Phase 4 Plan 01: Wave 0 Test Scaffolding Summary

**One-liner:** Four @Disabled JUnit 5 test stubs for Nyquist compliance — unit stubs for state machine and round generator, integration stubs for race control and referee endpoints.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Unit test stubs: RaceStateMachineServiceTest + RoundGeneratorServiceTest | dff1e53 | 2 created |
| 2 | Integration test stubs: RaceControlControllerIT + RefereeControllerIT | 33ef6fa | 2 created |

## What Was Built

Four wave-0 test stub files that exist for downstream feedback sampling without running. All use class-level `@Disabled` so they compile and are discoverable but do not execute until production classes arrive in plans 02–06.

- **RaceStateMachineServiceTest** (4 tests): invalid PENDING→FINISHED, FINISHED terminal, valid PENDING→GRID, valid GRID→RUNNING — enabled in plan 02
- **RoundGeneratorServiceTest** (2 tests): 15-driver heat split (max 8/heat), bump-up seeding A/B final — enabled in plan 03
- **RaceControlControllerIT** (8 tests): callGrid, startRace, 409 conflict, marshal audit, unknown transponder, abandon race, print results, skip race — enabled in plans 05/06
- **RefereeControllerIT** (5 tests): raise incident, lap penalty, time penalty, proximity alert marker, backmarker detection — enabled in plan 06

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Field declarations for non-existent production classes removed**
- **Found during:** Task 1 compilation
- **Issue:** Plan instructed declaring `private RaceStateMachineService service;` and `private RoundGeneratorService service;` fields while also stating "do not import production classes yet (they don't exist)". These field types caused `cannot find symbol` compiler errors.
- **Fix:** Commented out field declarations with TODO comments indicating which plan re-enables them (plan 02/03).
- **Files modified:** RaceStateMachineServiceTest.java, RoundGeneratorServiceTest.java
- **Commit:** dff1e53

## Known Stubs

All four files are intentional stubs. No data-wiring stubs exist — these are test-only files.

## Threat Flags

None — test-only scaffolding with no runtime code paths.

## Self-Check: PASSED

- [x] `app/src/test/java/dev/monkeypatch/rctiming/domain/race/RaceStateMachineServiceTest.java` — FOUND
- [x] `app/src/test/java/dev/monkeypatch/rctiming/service/RoundGeneratorServiceTest.java` — FOUND
- [x] `app/src/test/java/dev/monkeypatch/rctiming/api/racecontrol/RaceControlControllerIT.java` — FOUND
- [x] `app/src/test/java/dev/monkeypatch/rctiming/api/racecontrol/RefereeControllerIT.java` — FOUND
- [x] Commit dff1e53 — FOUND
- [x] Commit 33ef6fa — FOUND
- [x] `./gradlew :app:compileTestJava` — PASSED
