---
phase: 04-race-state-machine
plan: 04
subsystem: race-control-api
tags: [race-control, readonly, jooq, rest-api, integration-test]
requires: [04-02]
provides: [pre-race-readiness-endpoint]
affects: []
tech-stack:
  added: []
  patterns: [jooq-read-side-query, spring-preauthorize, testcontainers-it]
key-files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/dto/PreRaceReadinessDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/dto/GridCallSlotDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/dto/MarshalDutyRowDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/query/racecontrol/PreRaceReadinessQuery.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/PreRaceReadinessController.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/racecontrol/PreRaceReadinessControllerIT.java
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/Race.java
    - app/src/main/java/dev/monkeypatch/rctiming/query/racecontrol/PreRaceReadinessQuery.java
decisions:
  - "Used fully-qualified domain names for two same-named StartType enums (domain.format vs domain.race) to avoid import ambiguity in test"
  - "marshal_absences UNIQUE(race_id, entry_id) constraint requires absences across different races to accumulate event-level count; test uses race0+race1 for 2 absences"
  - "Added @JdbcTypeCode(SqlTypes.JSON) to Race.formatOverrides — required by Hibernate 6 for null String mapped to jsonb column"
  - "Fixed PreRaceReadinessQuery gridCall join order: RACES join must precede EVENT_CLASSES join in FROM clause"
metrics:
  duration: "~35 minutes"
  completed: "2026-04-24T13:14:59Z"
  tasks_completed: 2
  files_changed: 6
---

# Phase 04 Plan 04: Pre-Race Readiness API Summary

**One-liner:** jOOQ read-side endpoint returning grid-call and marshal-duty lists for the race control cockpit, role-gated to RACE_DIRECTOR/ADMIN, with 4 passing integration tests.

## Tasks Completed

| Task | Name | Commit | Key Files |
|------|------|--------|-----------|
| 1 | DTOs + PreRaceReadinessQuery (jOOQ read-side) | 5b43ddc / 294b161 | PreRaceReadinessDto, GridCallSlotDto, MarshalDutyRowDto, PreRaceReadinessQuery, PreRaceReadinessController |
| 2 | Integration test (4 tests) | 021c44c | PreRaceReadinessControllerIT, Race.java fix, PreRaceReadinessQuery join fix |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Race.formatOverrides missing @JdbcTypeCode(SqlTypes.JSON)**
- **Found during:** Task 2 (first test run — saveRace threw SQLGrammarException)
- **Issue:** Hibernate 6 binds a null String mapped to a `jsonb` column as `character varying`, causing PostgreSQL to reject the INSERT with "column is of type jsonb but expression is of type character varying"
- **Fix:** Added `@JdbcTypeCode(SqlTypes.JSON)` annotation to `Race.formatOverrides` field in addition to the existing `@Column(columnDefinition = "jsonb")`
- **Files modified:** `app/src/main/java/dev/monkeypatch/rctiming/domain/race/Race.java`
- **Commit:** 021c44c

**2. [Rule 1 - Bug] PreRaceReadinessQuery gridCall join order — "missing FROM-clause entry for table races"**
- **Found during:** Task 2 (endpoint returned 500 on first test)
- **Issue:** The gridCall SELECT joined `EVENT_CLASSES ON EVENT_CLASSES.ID = RACES.EVENT_CLASS_ID` before joining `RACES`, so PostgreSQL rejected the query with "missing FROM-clause entry for table races"
- **Fix:** Moved the `RACES` join to be the first join after `from(RACE_ENTRIES)`, before `ENTRIES`, `USERS`, `EVENT_CLASSES`, and `RACING_CLASSES`
- **Files modified:** `app/src/main/java/dev/monkeypatch/rctiming/query/racecontrol/PreRaceReadinessQuery.java`
- **Commit:** 021c44c

**3. [Rule 1 - Bug] marshal_absences UNIQUE(race_id, entry_id) prevents two absences for same driver in same race**
- **Found during:** Task 2 (second test — duplicate key violation)
- **Issue:** The schema has `UNIQUE(race_id, entry_id)` on `marshal_absences` — one absence record per (race, entry) pair. The test tried to insert two absences for the same (race1.id, driverA.entryId), violating the constraint.
- **Fix:** Restructured the second test to seed a `race0` (prior race, seq=1) and use `race1` (seq=2) as the "previous race". Driver A gets one absence in `race0` and one in `race1`, giving `missedThisEvent == 2` without violating the unique constraint.
- **Files modified:** `app/src/test/java/dev/monkeypatch/rctiming/api/racecontrol/PreRaceReadinessControllerIT.java`
- **Commit:** 021c44c

**4. [Rule 1 - Bug] Duplicate import of two `StartType` enums caused silent test compile failure**
- **Found during:** Task 2 (test class produced no .class file)
- **Issue:** Initial test draft imported both `domain.format.StartType` and `domain.race.StartType` — Java compiler silently drops the class when there is a duplicate type-name import conflict
- **Fix:** Removed the `domain.format.StartType` import and used the fully-qualified name `dev.monkeypatch.rctiming.domain.format.StartType.ROLLING` inline in `saveEventClass`; imported only `domain.race.StartType` for `saveRace`
- **Files modified:** `app/src/test/java/dev/monkeypatch/rctiming/api/racecontrol/PreRaceReadinessControllerIT.java`
- **Commit:** 021c44c

## Known Stubs

None — the endpoint is fully wired to live jOOQ queries against the test database. `carNumber` is intentionally `null` in both `GridCallSlotDto` and `MarshalDutyRowDto` because the `entries` table has no `car_number` column (documented in `PreRaceReadinessQuery` Javadoc).

## Threat Flags

None — all surfaces were in the plan's threat model (T-04-04-01 through T-04-04-04).

## Self-Check: PASSED

- SUMMARY.md: FOUND
- Commit 021c44c: FOUND
- PreRaceReadinessControllerIT.java: FOUND
- 4 tests passed (0 failures, 0 errors)
