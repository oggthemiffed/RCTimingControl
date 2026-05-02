---
phase: 07-results-championship
plan: "02"
subsystem: domain-entities, flyway-migration, service-layer
tags: [wave-1, car-number, result-snapshot, flyway, jpa]
dependency_graph:
  requires:
    - 07-01 (Wave 0 test scaffold)
  provides:
    - V24 Flyway migration — race_entries.car_number and club_profiles.show_car_tags_in_results columns
    - RaceEntry.carNumber field with getter/setter
    - ClubProfile.showCarTagsInResults field with getter/setter
    - car_number assignment in RoundGeneratorService (qualifying heats)
    - car_number assignment in BumpUpSeedingService (finals seeding from qualifying standings)
    - carNumber wired through ResultSnapshotService.resolveEntryInfo()
  affects:
    - app/src/main/resources/db/migration (new V24 file)
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceEntry.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfile.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/RoundGeneratorService.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/BumpUpSeedingService.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java
tech_stack:
  added: []
  patterns:
    - Flyway ALTER TABLE migration pattern (matching V23 header/comment style)
    - JPA nullable Integer column for optional car identifier
    - qualifyingStandings.indexOf(entryId)+1 for stable car number from standings position
key_files:
  created:
    - app/src/main/resources/db/migration/V24__phase7_results_championship.sql
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceEntry.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfile.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/RoundGeneratorService.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/BumpUpSeedingService.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java
decisions:
  - "car_number assigned as pos+1 (heat slot order) for ALL practice/qualifying rounds — not just first round — so carNumber is stable per heat membership across rounds (each entry's position in the heat is consistent)"
  - "bump slots in finals receive carNumber=null at creation; bump-up drivers get a number only after applyBumpUpResults assigns their entryId (deferred assignment)"
  - "ResultSnapshotService.resolveEntryInfo() now returns carNumber as String (or null) — backwards-compatible with existing snapshots that stored null"
metrics:
  duration: "~15 minutes"
  completed: "2026-05-02"
  tasks_completed: 2
  files_created: 1
  files_modified: 5
---

# Phase 07 Plan 02: V24 Migration + Car Number Assignment Summary

V24 Flyway migration adds `race_entries.car_number` and `club_profiles.show_car_tags_in_results`; entity fields added and car_number assignment wired through the generator services and result snapshot service — closing the null carNumber gap in ResultSnapshotDto.

## What Was Built

### Task 1: V24 Flyway migration + domain entity updates

`V24__phase7_results_championship.sql` adds two schema columns:

| Column | Table | Type | Default | Purpose |
|--------|-------|------|---------|---------|
| `car_number` | `race_entries` | `int` nullable | null | Car number assigned by generators; null for existing rows |
| `show_car_tags_in_results` | `club_profiles` | `boolean NOT NULL` | `false` | Admin toggle for RESULT-04 car tag display |

Both columns include `COMMENT ON COLUMN` documentation per the existing V23 style.

`RaceEntry.java` gains:
- `@Column(name = "car_number") private Integer carNumber`
- `getCarNumber()` / `setCarNumber(Integer)`

`ClubProfile.java` gains:
- `@Column(name = "show_car_tags_in_results", nullable = false) private boolean showCarTagsInResults = false`
- `isShowCarTagsInResults()` / `setShowCarTagsInResults(boolean)`

### Task 2: car_number assignment in services

**RoundGeneratorService.persistRound():** Added `entry.setCarNumber(pos + 1)` immediately after `entry.setGridPosition(...)`. This assigns carNumber = 1-N (heat slot order) for every RaceEntry created in practice and qualifying rounds. The assignment is stable per heat membership — the same driver always has the same car number within a phase.

**BumpUpSeedingService.seedFinals():** Added carNumber assignment for regular slot entries using `qualifyingStandings.indexOf(entryId) + 1` — so carNumber reflects the driver's qualifying standing position in the finals phase. Bump slots receive `carNumber = null` at creation; they are filled by `applyBumpUpResults()` when the lower final finishes.

**ResultSnapshotService.resolveEntryInfo():** Changed the return from `new String[]{name, null}` to `new String[]{name, carNum}` where `carNum = re.getCarNumber() != null ? re.getCarNumber().toString() : null`. This closes the null carNumber gap in result snapshots going forward. Historical snapshots retain `carNumber=null` (accepted per plan).

## Deviations from Plan

### Auto-fixed Issues

None — plan executed exactly as written, with one minor implementation note:

The plan action text referenced `re.setCarNumber(gridPosition)` as the variable name, but the actual variable in `persistRound()` is `pos + 1` (not a named `gridPosition` variable). Used `entry.setCarNumber(pos + 1)` to match the actual code structure. The semantics are identical.

## Known Stubs

None — all fields are wired. Historical result snapshots have `carNumber=null` but that is documented as an accepted gap per the plan's migration comment.

## Verification Status

- Docker unavailable in execution environment — full `./gradlew :app:test` could not run
- `./gradlew :app:compileJava :app:compileTestJava -x startJooqDb -x generateJooq` fails only on pre-existing jOOQ codegen errors (same 100 errors pre-dating this plan, all `static import only from classes and interfaces` for jOOQ-generated table classes requiring Docker to regenerate)
- None of the 5 modified files (RaceEntry, ClubProfile, RoundGeneratorService, BumpUpSeedingService, ResultSnapshotService) appear in any compile error
- Grep acceptance criteria verified: all counts meet or exceed required minimums
- No unexpected file deletions in either commit

## Self-Check: PASSED

**Files verified:**
- FOUND: `app/src/main/resources/db/migration/V24__phase7_results_championship.sql`
- FOUND: `app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceEntry.java` (carNumber field)
- FOUND: `app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfile.java` (showCarTagsInResults field)
- FOUND: `app/src/main/java/dev/monkeypatch/rctiming/service/RoundGeneratorService.java` (setCarNumber)
- FOUND: `app/src/main/java/dev/monkeypatch/rctiming/service/BumpUpSeedingService.java` (setCarNumber x2)
- FOUND: `app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java` (getCarNumber)

**Commits verified:**
- FOUND: 4d6ea93 (Task 1 — V24 migration + entity fields)
- FOUND: 0832928 (Task 2 — car_number assignment in generators)
