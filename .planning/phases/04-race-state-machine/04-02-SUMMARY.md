---
phase: 04-race-state-machine
plan: 02
subsystem: domain
tags: [schema, domain, state-machine, race-control, flyway, jooq]
dependency_graph:
  requires:
    - 04-01 (wave-0 test stubs — RaceStateMachineServiceTest stub)
  provides:
    - V17/V18 Flyway migrations (rounds, races, race_entries, marshal/referee tables)
    - Race domain entities and repositories (plans 03/05/06 depend on these)
    - RaceStateMachineService for CTRL-05 state enforcement (plans 05/06 use this)
    - EventClass finals config columns (plan 03 round generator uses finalsCount/carsPerFinal/bumpCount)
    - jOOQ generated sources for all new tables
  affects:
    - EventClass.java (added 3 nullable Integer fields)
tech_stack:
  added: []
  patterns:
    - EnumMap state machine pattern (mirrors EventStateMachineService exactly)
    - JPA entity with Long FK columns (no @ManyToOne associations — write-side boundary)
    - Spring Data JPA derived query methods
    - Flyway plain-SQL migrations with CHECK constraints and timestamptz
key_files:
  created:
    - app/src/main/resources/db/migration/V17__phase4_race_schema.sql
    - app/src/main/resources/db/migration/V18__phase4_marshal_referee_schema.sql
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceStatus.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RoundStatus.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RoundType.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/StartType.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/Round.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RoundRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/Race.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceEntry.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceEntryRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/MarshalAdjustment.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/MarshalAdjustmentRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/MarshalAbsence.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/MarshalAbsenceRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/MarshalPenalty.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/MarshalPenaltyRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/IncidentReport.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/IncidentReportRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/Penalty.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/PenaltyRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/UnknownTransponderLink.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/UnknownTransponderLinkRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceStateMachineService.java
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/EventClass.java
    - app/src/test/java/dev/monkeypatch/rctiming/domain/race/RaceStateMachineServiceTest.java
decisions:
  - "Race.formatOverrides mapped as String with columnDefinition=jsonb (not @Type(JsonType.class)) to avoid hypersistence dependency in new package; consistent with column-level JSONB storage"
  - "race_format_templates used as FK target in races.format_id (confirmed from V5 migration — not race_formats)"
  - "UNIQUE (race_id, transponder_number) added to unknown_transponder_links for server-side dedup per Pitfall 6"
metrics:
  duration: "~6 minutes"
  completed: "2026-04-24T08:19:00Z"
  tasks_completed: 3
  files_created: 25
  files_modified: 2
---

# Phase 4 Plan 02: Schema Foundation and Race State Machine Summary

**One-liner:** V17/V18 Flyway migrations + 9 JPA entities + 9 repositories + EnumMap race state machine with 4 passing unit tests covering CTRL-05.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Flyway migrations V17/V18 and EventClass finals config columns | 338551c | 3 created/modified |
| 2 | Race domain entities, enums, and repositories | e273bf7 | 22 created |
| 3 | RaceStateMachineService + enable RaceStateMachineServiceTest | b263c83 | 2 created/modified |

## What Was Built

### Flyway Migrations (Task 1)

- **V17**: Adds `rounds`, `races`, `race_entries` tables with CHECK constraints on status/type columns, timestamptz audit columns, and 5 indexes. Also adds `finals_count`, `cars_per_final`, `bump_count` columns to `event_classes`.
- **V18**: Adds `marshal_adjustments`, `marshal_absences`, `marshal_penalties`, `incident_reports`, `penalties`, `unknown_transponder_links` tables. Includes `UNIQUE (race_id, transponder_number)` on `unknown_transponder_links` for server-side deduplication (Pitfall 6 prevention). All 5 indexes added.
- **EventClass**: Three nullable `Integer` fields (`finalsCount`, `carsPerFinal`, `bumpCount`) with getters/setters. Null = use system defaults at round generation time.
- jOOQ codegen regenerated: `Races.java`, `Rounds.java`, `RaceEntries.java`, `MarshalAdjustments.java`, `Penalties.java`, `IncidentReports.java`, `UnknownTransponderLinks.java` all present in generated sources.

### Domain Entities (Task 2)

4 enums: `RaceStatus` (PENDING/GRID/RUNNING/STOPPED/FINISHED), `RoundStatus` (PENDING/RUNNING/COMPLETED), `RoundType` (PRACTICE/QUALIFIER/FINAL), `StartType` (STAGGER/GRID).

9 JPA entities following the `Event.java` pattern — plain getters/setters, no Lombok, `@Enumerated(EnumType.STRING)` for status columns, Long FK columns (no `@ManyToOne` associations — write-side boundary maintained).

9 Spring Data JPA repositories with required derived query methods including `MarshalAbsenceRepository.countByEntryIdAndEventId`, `RoundRepository.existsByEventId`, and `UnknownTransponderLinkRepository.findByRaceIdAndTransponderNumber`.

### State Machine (Task 3)

`RaceStateMachineService` uses `EnumMap<RaceStatus, Set<RaceStatus>>` pattern exactly mirroring `EventStateMachineService`. Imports and throws `dev.monkeypatch.rctiming.domain.event.IllegalStateTransitionException` — already mapped to HTTP 409 by `GlobalExceptionHandler`. No new exception class needed.

Transition table enforced:
- PENDING → {GRID}
- GRID → {RUNNING, PENDING}
- RUNNING → {STOPPED, FINISHED}
- STOPPED → {RUNNING, FINISHED}
- FINISHED → {} (terminal)

`RaceStateMachineServiceTest`: `@Disabled` removed, all 4 tests enabled and green (0 failures, 0 errors, 0 skipped).

## Deviations from Plan

None — plan executed exactly as written. The task noted to check the FK target for `race_formats` vs `race_format_templates` — confirmed `race_format_templates` from V5 migration and used correctly in V17.

## Known Stubs

None — all entities are fully wired with correct column mappings. No placeholder data.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: new-schema | V17/V18 | races and marshal tables are new trust-boundary surfaces; race state transitions enforced by RaceStateMachineService (T-04-02 mitigated); concurrent double-submit addressed in plan 05 with transactional updates |

## Self-Check: PASSED

- [x] `V17__phase4_race_schema.sql` — FOUND
- [x] `V18__phase4_marshal_referee_schema.sql` — FOUND
- [x] `RaceStateMachineService.java` — FOUND
- [x] All 22 entity/enum/repository files — FOUND
- [x] `RaceStateMachineServiceTest.java` — no @Disabled, 4 tests passing
- [x] Commit 338551c — FOUND
- [x] Commit e273bf7 — FOUND
- [x] Commit b263c83 — FOUND
- [x] `./gradlew :app:compileJava` — PASSED
- [x] `./gradlew :app:generateJooq` — PASSED (Races.java, Rounds.java, RaceEntries.java, MarshalAdjustments.java, Penalties.java, IncidentReports.java, UnknownTransponderLinks.java all present)
- [x] `./gradlew :app:test --tests '*.RaceStateMachineServiceTest'` — 4 tests, 0 failures
