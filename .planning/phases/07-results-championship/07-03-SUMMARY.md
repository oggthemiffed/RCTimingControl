---
phase: 07-results-championship
plan: "03"
subsystem: query-module, championship-standings, result-history
tags: [wave-1, jooq, championship, standings, result-history, idor-safety]
dependency_graph:
  requires: [07-01]
  provides:
    - ChampionshipStandingsQuery.computeStandings() — full best-X-from-Y + bonuses + DNS semantics
    - RacerResultHistoryQuery.findForUser() — per-user race result history with IDOR scope guard
    - RacerResultHistoryDto — DTO for racer portal Results tab
  affects:
    - app/src/main/java/dev/monkeypatch/rctiming/query/championship/ (standings fully computed)
    - app/src/main/java/dev/monkeypatch/rctiming/query/results/ (stub replaced with full impl)
    - app/src/test/java/.../query/championship/ (4 integration tests enabled)
    - app/src/test/java/.../query/results/ (2 integration tests enabled)
tech_stack:
  added:
    - RacerResultHistoryDto (new record in query.results package)
  patterns:
    - Pure jOOQ DSL join chain — no Hibernate/JPA involvement in query module
    - ENTRIES.USER_ID.eq(userId) IDOR scope guard pattern
    - ObjectMapper JSONB deserialization via TypeReference (established in ResultSnapshotQuery)
    - jOOQ insertInto(...).returning(ID) for test data setup bypassing JPA config_snapshot complexity
key_files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryDto.java
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQuery.java
    - app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQuery.java
    - app/src/test/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQueryTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQueryTest.java
decisions:
  - "ChampionshipRepository removed from ChampionshipStandingsQuery constructor; replaced with dsl.fetchExists(CHAMPIONSHIPS, CHAMPIONSHIPS.ID.eq(championshipId)) — maintains the CQRS-lite seam"
  - "DNS assumption documented with ASSUMED comment per research note A1: DNS driver (in race_entries but absent from positions_json) scores 0 pts and round counts toward Y; club confirmation pending"
  - "Test data insertion for event_classes uses jOOQ dsl.insertInto rather than JPA EventClassRepository to avoid config_snapshot sealed-interface serialization complexity in test context"
  - "TQ bonus tracked per-driver per-championship (not per-round): any driver with position=1 in any QUALIFIER race in the championship's events gets tqBonusPoints added once to totalPoints"
metrics:
  duration: "~45 minutes"
  completed: "2026-05-02"
  tasks_completed: 2
  files_created: 1
  files_modified: 4
---

# Phase 07 Plan 03: Championship Standings and Racer Result History Summary

Full championship standings computation with best-X-from-Y drop logic, TQ/A-final bonuses, DNS semantics; plus per-user race result history query with IDOR scope guard.

## What Was Built

### Task 1: ChampionshipStandingsQuery.computeStandings()

The `ChampionshipStandingsQuery` was rewritten from a Phase 3 stub (returned empty list) to a full 10-step standings algorithm:

| Step | Description |
|------|-------------|
| 1 | `dsl.fetchExists()` existence check — replaces `ChampionshipRepository.existsById()` |
| 2 | Load championship header (name, scoring_source, bonus points, best-X-from-Y defaults) |
| 3 | Load per-class best-X-from-Y overrides from `championship_classes` |
| 4 | Load ordered event links from `championship_event_links` |
| 5 | Build points scale `Map<Integer position, Integer points>` |
| 6 | Build exclusion key set `"driverId:eventId"` strings |
| 7 | Per event: find FINISHED races (filtered by `scoring_source`), join to `result_snapshots`, deserialize `positions_json`, collect per-driver best position; also load `race_entries` for DNS detection |
| 8 | Per driver: build `RoundResultDto` list; DNS drivers (in race_entries, absent from positions_json) get position=0, points=0 |
| 9 | Apply best-X-from-Y: sort rounds by points DESC, mark bottom (Y-X) as `dropped=true`; sum non-dropped non-excluded points |
| 10 | Apply TQ bonus (position=1 in QUALIFIER) and A-final winner bonus (position=1, final_letter='A') |

**Architecture constraint enforced:** ChampionshipRepository completely removed from the class. Constructor accepts `DSLContext dsl, ObjectMapper objectMapper` only.

**DNS assumption documented:** Per RESEARCH.md Assumption A1: DNS drivers (race_entries present but absent from positions_json) score 0 points and the round counts toward Y. Comment added: `// ASSUMED: DNS counts toward Y rounds (club confirmation pending — see STATE.md)`.

Four integration tests enabled in `ChampionshipStandingsQueryTest`:
- `bestXFromYDropsWorstRounds`: 3-round dataset with best 2 from 3; verifies worst round dropped
- `tqBonusApplied`: QUALIFYING scoring_source; position=1 driver gets tqBonusPoints added
- `afinalWinnerBonusApplied`: FINALS scoring_source; A-final winner gets afinalWinnerBonusPoints
- `dnsDriverScoresZeroAndRoundCountsTowardY`: best 1 from 2 with one DNS round; total = 8 not 0

### Task 2: RacerResultHistoryQuery + RacerResultHistoryDto

The stub `RacerResultHistoryQuery` was replaced with a full implementation:

`findForUser(Long userId)`:
1. Queries `ENTRIES` scoped by `ENTRIES.USER_ID.eq(userId)` (IDOR guard)
2. Joins to `EVENTS` for event grouping; ordered by `EVENT_DATE DESC`
3. For each entry: joins `RACE_ENTRIES → RACES → ROUNDS → EVENT_CLASSES → RACING_CLASSES → RESULT_SNAPSHOTS` to collect race results
4. Deserializes `positions_json` to extract position, lapsCompleted, bestLapMs for the entry
5. Groups results into `RacerResultHistoryDto` records grouped by event

`RacerResultHistoryDto` created as a new Java record:
- Outer record: `(Long eventId, String eventName, LocalDate eventDate, List<RaceResult> races)`
- Inner record: `RaceResult(Long raceId, String raceLabel, int position, int lapsCompleted, Long bestLapMs)`

Two integration tests enabled in `RacerResultHistoryQueryTest`:
- `returnsOnlyResultsForRequestingUser`: User A has entries; User B has none. User A sees 1 event with race result; User B sees empty list (IDOR isolation verified)
- `emptyListWhenUserHasNoResults`: Brand-new user with no entries returns empty list without exception

## Deviations from Plan

### Auto-fixed Issues

None — plan executed exactly as written with one test infrastructure adaptation.

### Test Infrastructure Adaptation (not a deviation)

The plan specified using `new RaceFormatConfig()` for `EventClass.configSnapshot` in tests. `RaceFormatConfig` is a sealed interface — cannot be instantiated directly. Instead, jOOQ `dsl.insertInto(EVENT_CLASSES).set(...CONFIG_SNAPSHOT, JSONB.valueOf("{\"type\":\"TIMED\"}"))` was used to insert `event_classes` rows directly, bypassing JPA serialization entirely. This is consistent with the "jOOQ for test data in query-module tests" approach seen in other Phase 7 integration tests.

## Threat Surface

| Threat ID | Mitigation Status |
|-----------|------------------|
| T-07-03-01 (IDOR: RacerResultHistoryQuery) | Mitigated — `ENTRIES.USER_ID.eq(userId)` enforced in query; two tests verify user isolation |
| T-07-03-02 (EntityNotFoundException leakage) | Mitigated — unknown championshipId throws `EntityNotFoundException`; GlobalExceptionHandler maps to 404 |
| T-07-03-03 (Points tampering) | Accepted — points computed on demand from immutable result_snapshots |

## Known Stubs

None — both query classes are fully implemented.

## Self-Check

Files verified:
- FOUND: `app/src/main/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQuery.java`
- FOUND: `app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQuery.java`
- FOUND: `app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryDto.java`
- FOUND: `app/src/test/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQueryTest.java`
- FOUND: `app/src/test/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQueryTest.java`

Commits verified:
- FOUND: 067f200 (Task 1 — ChampionshipStandingsQuery + test)
- FOUND: 840e3b7 (Task 2 — RacerResultHistoryQuery + DTO + test)

Acceptance criteria verified:
- `grep -c "ChampionshipRepository" ...ChampionshipStandingsQuery.java` → 0
- `grep -c "fetchExists" ...ChampionshipStandingsQuery.java` → 1
- `grep -c "@Disabled" ...ChampionshipStandingsQueryTest.java` → 0
- `grep -c "ASSUMED" ...ChampionshipStandingsQuery.java` → 1
- `RacerResultHistoryDto.java` → EXISTS
- `RacerResultHistoryQuery.java` → EXISTS
- `grep -c "USER_ID.eq" ...RacerResultHistoryQuery.java` → 2
- `grep -c "@Disabled" ...RacerResultHistoryQueryTest.java` → 0
- Compilation: main sources BUILD SUCCESSFUL; test sources error-free (pre-existing audio errors excluded)

## Self-Check: PASSED
