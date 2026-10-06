---
phase: 03-admin-panel-event-management
plan: "01"
subsystem: backend
tags: [flyway, jooq, entities, exception-handling, championships, schema]
dependency_graph:
  requires: []
  provides:
    - V15 Flyway migration (track_id FK, racing_class_id FK, combined_race_group, logo_url, archived)
    - V16 Flyway migration (championships, championship_classes, championship_event_links, championship_points_scale, championship_exclusions)
    - Event.trackId entity field
    - EventClass.racingClassId + combinedRaceGroup entity fields
    - ClubProfile.logoUrl entity field
    - CarTagCategory.archived entity field
    - IllegalStateTransitionException domain exception
    - GlobalExceptionHandler HTTP 409 mapping for IllegalStateTransitionException
  affects:
    - jOOQ generated sources (regenerated after both migrations)
    - All Phase 3 Wave 2 plans (consume schema, entities, and exception)
tech_stack:
  added: []
  patterns:
    - Flyway ALTER TABLE pattern for safe additive column changes (nullable FKs with on delete set null)
    - jOOQ codegen regenerated from live schema via Docker Postgres container
    - RuntimeException subclass in domain package, mapped to HTTP status via GlobalExceptionHandler ProblemDetail
key_files:
  created:
    - app/src/main/resources/db/migration/V15__phase3_admin_schema.sql
    - app/src/main/resources/db/migration/V16__create_championships.sql
    - app/src/main/java/dev/monkeypatch/rctiming/domain/event/IllegalStateTransitionException.java
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/event/Event.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/EventClass.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfile.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategory.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/GlobalExceptionHandler.java
decisions:
  - "track_id added as nullable FK (on delete set null) — not every event needs a track initially; NOT NULL enforcement deferred to Phase 4"
  - "racing_class_id added as nullable FK — existing Phase 2 seed rows lack one; NOT NULL deferred to Phase 4"
  - "combined_race_group stored as nullable BIGINT on event_classes — event_classes sharing same non-null value form one combined race; no separate table needed"
  - "logo bytea column retained on club_profiles — logo_url column added alongside it for MinIO URL; data safety per RESEARCH pitfall 5"
  - "championship_points_scale is a table (position,points per championship) not a JSON column — per RESEARCH pitfall 7"
  - "championship_exclusions uses driver_id ON DELETE CASCADE — consistent with EntryAuditLog pattern"
metrics:
  duration: "6m"
  completed_date: "2026-04-21"
  tasks_completed: 3
  tasks_total: 3
  files_created: 3
  files_modified: 5
---

# Phase 03 Plan 01: DB + Entity + Exception Foundation Summary

Flyway migrations V15 and V16 added the schema required by all Phase 3 Wave 2 plans. Four existing entities were extended with new fields mapped to the new columns. `IllegalStateTransitionException` was created and wired to HTTP 409 via `GlobalExceptionHandler`.

## Tasks Completed

| Task | Name | Commit | Key Files |
|------|------|--------|-----------|
| 1 | V15 migration — column additions | 1855057 | V15__phase3_admin_schema.sql |
| 2 | V16 migration — championship tables | ac44014 | V16__create_championships.sql |
| 3 | Entity extensions + IllegalStateTransitionException | a5fb316 | Event.java, EventClass.java, ClubProfile.java, CarTagCategory.java, IllegalStateTransitionException.java, GlobalExceptionHandler.java |

## Migrations Added

### V15 — Phase 3 Column Additions

- `events.track_id BIGINT` — nullable FK to `tracks(id)` ON DELETE SET NULL (EVENT-07 enabler)
- `event_classes.racing_class_id BIGINT` — nullable FK to `racing_classes(id)` ON DELETE SET NULL (EVENT-02 enabler)
- `event_classes.combined_race_group BIGINT` — nullable; shared non-null values group classes into one combined race (EVENT-06)
- `club_profiles.logo_url VARCHAR(500)` — nullable; for MinIO object storage URL (D-22)
- `car_tag_categories.archived BOOLEAN NOT NULL DEFAULT false` — soft-delete flag (D-21)
- Indexes added: `idx_events_track_id`, `idx_event_classes_racing_class_id`, `idx_event_classes_combined_race_group`, `idx_car_tag_categories_archived`

### V16 — Championship Tables

- `championships` — name, best_x_from_y_x/y, scoring_source (CHECK: QUALIFYING/FINALS/BOTH), tq_bonus_points, afinal_winner_bonus_points (CHAMP-01,06,07,08)
- `championship_classes` — per-class standings with optional best_x_from_y override, FK to racing_classes ON DELETE RESTRICT (CHAMP-03)
- `championship_event_links` — championship-to-event round mapping with unique(championship_id, event_id) and unique(championship_id, round_number)
- `championship_points_scale` — composite PK (championship_id, position), position→points mapping per championship (CHAMP-04)
- `championship_exclusions` — audit trail with driver_id, event_id, reason, created_by FKs (CHAMP-09)
- Indexes added on all FK/filter columns

## Entity Fields Added

| Entity | New Field | Type | Column |
|--------|-----------|------|--------|
| `Event` | `trackId` | `Long` | `track_id` |
| `EventClass` | `racingClassId` | `Long` | `racing_class_id` |
| `EventClass` | `combinedRaceGroup` | `Long` | `combined_race_group` |
| `ClubProfile` | `logoUrl` | `String` | `logo_url` (length=500) |
| `CarTagCategory` | `archived` | `boolean` | `archived` (default false) |

All existing fields retained — no regressions. `ClubProfile.logo` (bytea) and `ClubProfile.logoType` remain untouched.

## Exception Handling

`IllegalStateTransitionException extends RuntimeException` created in `dev.monkeypatch.rctiming.domain.event`. `GlobalExceptionHandler` extended with `@ExceptionHandler(IllegalStateTransitionException.class)` returning `ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage())`. HTTP 409 count on `GlobalExceptionHandler` is now 2 (one for `DataIntegrityViolationException`, one new for state machine transitions).

## jOOQ Regeneration Confirmed

`./gradlew :app:generateJooq` succeeded. Generated table classes confirmed to contain:
- `Events.TRACK_ID`
- `EventClasses.RACING_CLASS_ID`, `EventClasses.COMBINED_RACE_GROUP`
- `CarTagCategories.ARCHIVED`
- `ClubProfiles.LOGO_URL`
- New tables: `Championships`, `ChampionshipClasses`, `ChampionshipEventLinks`, `ChampionshipPointsScale`, `ChampionshipExclusions`

`./gradlew :app:compileJava` succeeded with no errors.

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None — this plan adds schema and domain foundation only; no UI or data rendering involved.

## Threat Surface Scan

No new network endpoints, auth paths, or file access patterns introduced. All changes are schema/entity/exception layer within the existing trust boundary. Threat model mitigations T-03-01 through T-03-05 applied as specified:
- `track_id` nullable with ON DELETE SET NULL (T-03-01)
- `logo` bytea retained (T-03-02)
- `championship_exclusions.driver_id` ON DELETE CASCADE (T-03-03)
- `IllegalStateTransitionException` message contains only enum names (T-03-04)
- `GlobalExceptionHandler` new mapping follows existing ProblemDetail pattern (T-03-05)

## Note on Integration Test Failures

`./gradlew :app:test --tests "*TrackControllerIT*,*FormatControllerIT*,*ClubControllerIT*"` failed with `DockerClientProviderStrategy` errors (`client version 1.32 is too old. Minimum supported API version is 1.40`). This is a pre-existing Docker API version mismatch between the Testcontainers library and the Unix socket in this environment — unrelated to this plan's changes. The same failure would occur on the unmodified codebase. Schema and compilation verification via `generateJooq` and `compileJava` passed cleanly.

## Self-Check: PASSED

- V15__phase3_admin_schema.sql: FOUND
- V16__create_championships.sql: FOUND
- IllegalStateTransitionException.java: FOUND
- Commit 1855057 (V15 migration): FOUND
- Commit ac44014 (V16 migration): FOUND
- Commit a5fb316 (entity extensions + exception): FOUND
- jOOQ TRACK_ID field in Events.java: FOUND
- jOOQ Championships.java table: FOUND
- ./gradlew :app:compileJava: PASSED
