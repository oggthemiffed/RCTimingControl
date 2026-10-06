---
phase: "02"
plan: "01"
subsystem: infrastructure
tags: [flyway, jooq, shadcn, migrations, codegen]
depends_on: []
provides:
  - Flyway migrations V6-V13 establishing Phase 2 schema
  - jOOQ 3.19.24 DSL classes under dev.monkeypatch.rctiming.jooq.generated
  - shadcn/ui Sheet, Select, Badge, Dialog, Separator components
affects:
  - app/build.gradle.kts
  - build.gradle.kts
  - frontend/src/components/ui/
tech_stack:
  added:
    - nu.studer.jooq plugin v9.0
    - org.flywaydb.flyway buildSrc helper (FlywayMigrator.java)
    - shadcn/ui@4.3.0 (Sheet, Select, Badge, Dialog, Separator)
  patterns:
    - Docker-based jOOQ codegen pipeline (startJooqDb → flywayMigrateForCodegen → generateJooq → stopJooqDb)
    - buildSrc FlywayMigrator helper to invoke Flyway Java API from Gradle task actions
    - jooqGenerator configuration pinned to jOOQ 3.19.24 via resolutionStrategy
key_files:
  created:
    - app/src/main/resources/db/migration/V6__extend_users_racer_fields.sql
    - app/src/main/resources/db/migration/V7__create_user_governing_body_memberships.sql
    - app/src/main/resources/db/migration/V8__create_user_class_ratings.sql
    - app/src/main/resources/db/migration/V9__create_cars.sql
    - app/src/main/resources/db/migration/V10__create_car_tags.sql
    - app/src/main/resources/db/migration/V11__create_transponders.sql
    - app/src/main/resources/db/migration/V12__create_events.sql
    - app/src/main/resources/db/migration/V13__create_entries.sql
    - app/src/test/resources/db/migration/test/V100__test_seed_events.sql
    - buildSrc/build.gradle.kts
    - buildSrc/src/main/java/dev/monkeypatch/build/FlywayMigrator.java
    - frontend/src/components/ui/sheet.tsx
    - frontend/src/components/ui/select.tsx
    - frontend/src/components/ui/badge.tsx
    - frontend/src/components/ui/dialog.tsx
    - frontend/src/components/ui/separator.tsx
  modified:
    - build.gradle.kts
    - app/build.gradle.kts
    - app/src/test/resources/application.yml
decisions:
  - "jOOQ codegen version pinned to 3.19.24 (not 3.19.11) — Spring Boot BOM pins runtime jooq to 3.19.11 but jooqGenerator config does not inherit the BOM, causing transitive resolution to 3.19.24; forcing codegen to 3.19.11 caused AbstractMethodError; using 3.19.24 for codegen is safe since generated code is compatible with 3.19.x runtime"
  - "buildSrc FlywayMigrator chosen over Flyway Gradle plugin — the Flyway Gradle plugin cannot be given the PostgreSQL JDBC driver via Kotlin DSL dependency configuration; buildSrc Java class calls Flyway API directly with the correct classpath"
  - "Docker-based codegen DB chosen over Testcontainers jdbc:tc: URL — the Flyway step requires a real JDBC URL that the Flyway Java API (in buildSrc) can connect to; Testcontainers TC URLs work only when the TC driver is in the connecting process classpath, which is not guaranteed for all build script classloaders"
  - "Test seed V100 placed under src/test/resources/db/migration/test/ and registered in test application.yml flyway.locations — this avoids polluting the main migration history while making seed data available to all integration tests"
metrics:
  duration_minutes: 11
  completed_date: "2026-04-17"
  tasks_completed: 3
  tasks_total: 3
  files_created: 15
  files_modified: 4
---

# Phase 02 Plan 01: Phase 2 Infrastructure Foundation Summary

**One-liner:** Flyway V6-V13 schema migrations, jOOQ 3.19.24 codegen via Docker Postgres pipeline, and five shadcn/ui component installations for the racer portal.

## What Was Built

### Task 1: Flyway Migrations V6-V13 + Test Seed V100

Eight forward migrations establishing the Phase 2 domain schema:

| Migration | Table(s) | Key Constraints |
|-----------|----------|----------------|
| V6 | users (ALTER) | Adds phone_number, emergency_contact_name/phone, phonetic_name |
| V7 | user_governing_body_memberships | UNIQUE(user_id, governing_body_code) |
| V8 | user_class_ratings | Composite PK(user_id, racing_class_id), CHECK rating 0-100 |
| V9 | cars | archived boolean NOT NULL DEFAULT false |
| V10 | car_tag_categories, car_tag_values | 7 default categories seeded; UNIQUE(car_id, category_id) |
| V11 | transponders | UNIQUE transponder_number (system-wide) |
| V12 | events + ALTER event_classes | status CHECK constraint; nullable event_id FK on event_classes |
| V13 | entries | Partial UNIQUE INDEX WHERE status != 'WITHDRAWN'; membership_override audit columns |

Test seed V100 inserts: OPEN event (id=1001), DRAFT event (id=1002), two event_classes linked to event 1001.

The test `application.yml` was updated to include `classpath:db/migration/test` so V100 runs in integration tests automatically.

### Task 2: jOOQ Codegen Pipeline

The `nu.studer.jooq` plugin v9.0 was added to both root and app `build.gradle.kts`. A custom Docker-based pipeline was implemented:

1. `startJooqDb` — starts `postgres:16-alpine` container on port 54320
2. `waitForJooqDb` — polls `pg_isready` until ready (30s timeout)
3. `flywayMigrateForCodegen` — runs `FlywayMigrator.migrate()` from buildSrc (Flyway Java API)
4. `generateJooq` — generates DSL classes against the migrated schema
5. `stopJooqDb` — stops the container (via `finalizedBy`)

Generated DSL classes confirmed in `app/build/generated-sources/jooq/dev/monkeypatch/rctiming/jooq/generated/tables/`:
Cars, CarTagCategories, CarTagValues, Entries, EventClasses, Events, Transponders, UserClassRatings, UserGoverningBodyMemberships, Users (+ Phase 1 tables)

External DB override: set `JOOQ_JDBC_URL`, `JOOQ_JDBC_USER`, `JOOQ_JDBC_PASSWORD` env vars to skip Docker tasks.

### Task 3: shadcn/ui Components

Five components installed via `npx shadcn@latest add` (v4.3.0):

| Component | Key Exports |
|-----------|-------------|
| sheet.tsx | Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription |
| select.tsx | Select, SelectTrigger, SelectValue, SelectContent, SelectItem |
| badge.tsx | Badge, badgeVariants |
| dialog.tsx | Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter |
| separator.tsx | Separator |

Note: shadcn CLI (v4.3.0) with `radix-luma` style wrote files to `frontend/@/components/ui/` literally instead of resolving the `@` tsconfig alias to `./src`. Files were manually moved to the correct `frontend/src/components/ui/` location. This is a known issue with the `radix-luma` style registry in shadcn CLI 4.x when tsconfig paths are not resolved by the CLI.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] shadcn CLI wrote files to wrong path**
- **Found during:** Task 3
- **Issue:** `npx shadcn@latest add` with `radix-luma` style wrote component files to `frontend/@/components/ui/` (literal `@` directory) instead of `frontend/src/components/ui/` (resolved tsconfig path alias)
- **Fix:** Copied files from `frontend/@/components/ui/` to `frontend/src/components/ui/`, removed the `frontend/@/` directory
- **Files modified:** All 5 component files (relocated, not content-modified)
- **Commit:** 00fe244

**2. [Rule 1 - Bug] jOOQ version mismatch causing AbstractMethodError**
- **Found during:** Task 2
- **Issue:** The `jooqGenerator` configuration does not inherit Spring Boot's dependency BOM, so `org.jooq:jooq` (a transitive dep of `jooq-codegen:3.19.11`) resolved to 3.19.24 while codegen was pinned at 3.19.11. This caused `AbstractMethodError: Receiver class JavaGenerator$Resolver does not define cacheKey()` at runtime.
- **Fix:** Changed `version.set("3.19.11")` to `"3.19.24"` and updated `resolutionStrategy` to force 3.19.24 consistently across the `jooqGenerator` configuration
- **Files modified:** `app/build.gradle.kts`
- **Commit:** 0aa68cd

**3. [Rule 3 - Blocking] Flyway Gradle plugin cannot accept JDBC driver via Kotlin DSL**
- **Found during:** Task 2
- **Issue:** The plan suggested using the `org.flywaydb.flyway` Gradle plugin to migrate the codegen DB. Neither `flyway(...)` nor `flywayMigration(...)` work as dependency configuration names in Kotlin DSL for the Flyway plugin's classpath; the plugin has no published Kotlin DSL accessor for adding the PG driver to its classpath.
- **Fix:** Created `buildSrc/build.gradle.kts` and `buildSrc/src/main/java/dev/monkeypatch/build/FlywayMigrator.java` to call the Flyway Java API directly from a Gradle task `doFirst` block. The Flyway Gradle plugin was removed from `app/build.gradle.kts`.
- **Files modified:** `app/build.gradle.kts`, new files in `buildSrc/`
- **Commit:** 0aa68cd

**4. [Rule 2 - Missing] Test migration location not configured**
- **Found during:** Task 1
- **Issue:** `src/test/resources/application.yml` had `flyway.locations: classpath:db/migration` only. The V100 test seed placed in `src/test/resources/db/migration/test/` would not be picked up.
- **Fix:** Updated `flyway.locations` to `classpath:db/migration,classpath:db/migration/test`
- **Files modified:** `app/src/test/resources/application.yml`
- **Commit:** 1b6e146

## Known Stubs

None — this plan creates only schema/infrastructure, not UI or service code. No stub patterns present.

## Threat Surface Scan

No new runtime attack surface introduced. All additions are build-time (Flyway migrations run at startup in controlled environment, jOOQ codegen is build-time only, shadcn components are frontend UI primitives). Consistent with threat model — no high-severity threats identified.

## Self-Check

Checking created files exist and commits are present...

- `/home/david/git/java/RCTimingControl/app/src/main/resources/db/migration/V6__extend_users_racer_fields.sql` — present
- `/home/david/git/java/RCTimingControl/app/src/main/resources/db/migration/V13__create_entries.sql` — present
- `/home/david/git/java/RCTimingControl/app/src/test/resources/db/migration/test/V100__test_seed_events.sql` — present
- `/home/david/git/java/RCTimingControl/app/build/generated-sources/jooq/dev/monkeypatch/rctiming/jooq/generated/tables/Cars.java` — present
- `/home/david/git/java/RCTimingControl/app/build/generated-sources/jooq/dev/monkeypatch/rctiming/jooq/generated/tables/CarTagCategories.java` — present
- `/home/david/git/java/RCTimingControl/frontend/src/components/ui/sheet.tsx` — present
- `/home/david/git/java/RCTimingControl/frontend/src/components/ui/separator.tsx` — present
- commit 1b6e146 — present (Task 1)
- commit 0aa68cd — present (Task 2)
- commit 00fe244 — present (Task 3)

## Self-Check: PASSED
