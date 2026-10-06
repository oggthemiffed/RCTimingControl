---
phase: 01-domain-foundation
plan: 01
subsystem: infra
tags: [gradle, spring-boot, flyway, postgresql, docker, jjwt, jooq, hypersistence]

# Dependency graph
requires: []
provides:
  - Gradle 8.14.2 multi-module project scaffold (:app + :forwarder)
  - Spring Boot 3.4.7 application entry point with all Phase 1 dependencies
  - Docker Compose dev infrastructure (postgres:16-alpine + mailpit)
  - Flyway migration set V1-V5 defining complete Phase 1 schema
  - application.yml with ddl-auto=validate, open-in-view=false, JWT config
  - application-dev.yml with Docker Compose datasource and MailPit SMTP
  - JacksonConfig YAML mapper bean (named, not @Primary)
  - .gitignore for Gradle build artifacts
affects:
  - 01-02 (auth entities and security config build on this schema and build scaffold)
  - 01-03 (club/track/raceclass APIs build on V2/V3/V4 migrations)
  - 01-04 (format config builds on V5 migration)
  - All subsequent plans in all phases

# Tech tracking
tech-stack:
  added:
    - Gradle 8.14.2 (via wrapper — system Gradle 9.3.1 blocked by Spring Boot 3.4.x incompatibility)
    - Spring Boot 3.4.7
    - Spring Boot starters: web, data-jpa, security, validation, mail, actuator
    - JJWT 0.12.6 (jjwt-api, jjwt-impl, jjwt-jackson)
    - Flyway 10.x (flyway-core + flyway-database-postgresql — both required)
    - jOOQ 3.19.x
    - Hypersistence Utils hibernate-63:3.9.11 (JSONB mapping)
    - jackson-dataformat-yaml (format config YAML export/import)
    - PostgreSQL 16 driver
    - Testcontainers (postgresql, spring-boot-testcontainers) for integration tests
    - Docker Compose: postgres:16-alpine, axllent/mailpit
  patterns:
    - Gradle Kotlin DSL multi-module project (settings.gradle.kts with include)
    - Spring Boot @SpringBootApplication entry point in package root
    - Named YAML ObjectMapper bean (@Bean("yamlObjectMapper") — NOT @Primary)
    - Flyway V{n}__{description}.sql naming, lowercase SQL, BIGSERIAL PKs, TIMESTAMPTZ columns
    - JSONB columns for polymorphic config (race_format_templates.config, event_classes.config_snapshot/config_override)
    - application.yml with ddl-auto=validate (Flyway owns schema — never update/create-drop)
    - Spring profile separation: shared application.yml + application-dev.yml

key-files:
  created:
    - settings.gradle.kts
    - build.gradle.kts
    - app/build.gradle.kts
    - forwarder/build.gradle.kts
    - gradle/wrapper/gradle-wrapper.properties
    - docker-compose.yml
    - app/src/main/java/dev/monkeypatch/rctiming/RcTimingApplication.java
    - app/src/main/java/dev/monkeypatch/rctiming/config/JacksonConfig.java
    - app/src/main/resources/application.yml
    - app/src/main/resources/application-dev.yml
    - app/src/main/resources/db/migration/V1__create_users_and_roles.sql
    - app/src/main/resources/db/migration/V2__create_club.sql
    - app/src/main/resources/db/migration/V3__create_tracks.sql
    - app/src/main/resources/db/migration/V4__create_racing_classes.sql
    - app/src/main/resources/db/migration/V5__create_race_formats.sql
    - .gitignore
  modified: []

key-decisions:
  - "Gradle wrapper pinned to 8.14.2 — system Gradle 9.3.1 is incompatible with Spring Boot 3.4.x (confirmed at execution time)"
  - "Both flyway-core AND flyway-database-postgresql declared — Flyway 10.x requires explicit database module"
  - "hypersistence-utils-hibernate-63:3.9.11 (not -hibernate-62) — must match Hibernate 6.3.x included with Spring Boot 3.4.x"
  - "yamlObjectMapper named bean, NOT @Primary — prevents hijacking Spring MVC JSON serialization"
  - "JWT secret has dev default in application.yml via ${JWT_SECRET:base64...} — production must override via JWT_SECRET env var"
  - "Refresh tokens stored in database (refresh_tokens table) — enables immediate revocation on password reset"

patterns-established:
  - "Gradle Kotlin DSL: rootProject includes :app and :forwarder; Spring Boot plugin applied false at root, applied at module level"
  - "Flyway SQL convention: lowercase keywords, BIGSERIAL PKs, TIMESTAMPTZ, JSONB for config, explicit ON DELETE behavior on all FKs"
  - "Spring Boot profile split: application.yml (shared, safe defaults) + application-dev.yml (dev infrastructure)"
  - "JSONB config columns: race_format_templates.config, event_classes.config_snapshot + config_override (snapshot+override pattern)"

requirements-completed:
  - AUTH-01
  - AUTH-02
  - AUTH-03
  - AUTH-05
  - CLUB-01
  - CLUB-02
  - TRACK-01
  - TRACK-02
  - TRACK-03
  - TRACK-04
  - RACECLASS-01
  - FORMAT-01
  - FORMAT-02
  - FORMAT-04
  - FORMAT-05
  - FORMAT-06
  - FORMAT-07

# Metrics
duration: 5min
completed: 2026-04-16
---

# Phase 01 Plan 01: Gradle scaffold + Flyway migrations + Docker Compose dev infrastructure

**Gradle 8.14.2 multi-module scaffold (:app, :forwarder), Spring Boot 3.4.7 app entry point, postgres:16 + mailpit Docker Compose, and 5 Flyway migrations defining all Phase 1 tables (users/roles/tokens, club, tracks, racing classes, JSONB race format templates)**

## Performance

- **Duration:** 5 min
- **Started:** 2026-04-16T14:02:37Z
- **Completed:** 2026-04-16T14:07:17Z
- **Tasks:** 2
- **Files modified:** 16 created, 0 modified

## Accomplishments

- Gradle wrapper pinned to 8.14.2 (system Gradle 9.3.1 blocked by Spring Boot 3.4.x incompatibility); `./gradlew projects` lists both `:app` and `:forwarder`
- `./gradlew :app:compileJava` succeeds — all Phase 1 dependencies declared and resolved (Spring Boot 3.4.7, JJWT 0.12.6, Flyway 10.x, jOOQ, Hypersistence Utils hibernate-63:3.9.11, YAML mapper)
- 5 Flyway migrations V1-V5 create the complete Phase 1 schema: users/roles/refresh/password-reset tokens, club profiles, governing body affiliations, tracks, decoder loops, lap thresholds, racing classes, race format templates with JSONB config, event classes with JSONB snapshot+override
- Docker Compose defines postgres:16-alpine and mailpit services for local dev; application.yml uses `ddl-auto: validate` and `open-in-view: false`; dev profile wired to Docker Compose datasource + MailPit SMTP

## Task Commits

Each task was committed atomically:

1. **Task 1: Gradle multi-module scaffold with wrapper** - `401b4ee` (chore)
2. **Task 2: Docker Compose, Spring Boot config, and Flyway migrations** - `655780e` (feat)
3. **Deviation: .gitignore for build artifacts** - `12a9e53` (chore)

**Plan metadata:** (this SUMMARY commit)

## Files Created/Modified

- `settings.gradle.kts` - Multi-module root: include(":app", ":forwarder")
- `build.gradle.kts` - Root build: Spring Boot 3.4.7 + dependency-management plugins (apply false)
- `app/build.gradle.kts` - App module: all Phase 1 deps, Java 21 toolchain
- `forwarder/build.gradle.kts` - Stub module: Java 21 toolchain, deferred to Phase 5
- `gradle/wrapper/gradle-wrapper.properties` - Pinned to gradle-8.14.2-bin.zip
- `docker-compose.yml` - postgres:16-alpine (rctiming_dev) + mailpit services
- `app/src/main/java/dev/monkeypatch/rctiming/RcTimingApplication.java` - Spring Boot entry point
- `app/src/main/java/dev/monkeypatch/rctiming/config/JacksonConfig.java` - Named YAML ObjectMapper bean
- `app/src/main/resources/application.yml` - Shared config: ddl-auto=validate, open-in-view=false, Flyway, JWT
- `app/src/main/resources/application-dev.yml` - Dev profile: Docker Compose DB + MailPit SMTP
- `app/src/main/resources/db/migration/V1__create_users_and_roles.sql` - users, user_roles, refresh_tokens, password_reset_tokens
- `app/src/main/resources/db/migration/V2__create_club.sql` - club_profiles, governing_body_affiliations
- `app/src/main/resources/db/migration/V3__create_tracks.sql` - tracks, decoder_loops, track_lap_thresholds
- `app/src/main/resources/db/migration/V4__create_racing_classes.sql` - racing_classes + FK from thresholds
- `app/src/main/resources/db/migration/V5__create_race_formats.sql` - race_format_templates (JSONB config), event_classes (JSONB snapshot+override)
- `.gitignore` - Gradle build artifacts, IDE files, frontend dist

## Decisions Made

- Gradle wrapper requires an existing settings.gradle.kts and module directories before `gradle wrapper` command will execute — created minimal settings first, ran wrapper, then completed full build files
- Both `flyway-core` AND `flyway-database-postgresql` declared; Flyway 10.x splits database-specific drivers into separate artifacts
- `hypersistence-utils-hibernate-63` artifact (not `-hibernate-62`) — must match Hibernate 6.3.x bundled with Spring Boot 3.4.x
- JWT dev secret uses `${JWT_SECRET:base64-placeholder}` pattern — works out of the box for dev, production must set JWT_SECRET env var (T-01-01 mitigation)
- Added `.gitignore` (deviation Rule 2 — missing critical artifact) to prevent Gradle build directories from being tracked

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added .gitignore for Gradle build artifacts**
- **Found during:** Task 2 post-commit
- **Issue:** `git status` showed `.gradle/`, `app/build/`, `build/` untracked after compilation — Gradle build directories would pollute git history
- **Fix:** Created `.gitignore` covering Gradle outputs, IDE files, OS files, and frontend build artifacts
- **Files modified:** `.gitignore`
- **Verification:** `git status` no longer shows untracked build directories
- **Committed in:** `12a9e53`

---

**Total deviations:** 1 auto-fixed (Rule 2 — missing critical gitignore)
**Impact on plan:** Essential housekeeping. No scope creep.

## Issues Encountered

- `gradle wrapper --gradle-version 8.14.2` failed on first attempt because there was no existing `settings.gradle.kts` in the worktree. Fixed by creating minimal settings.gradle.kts first, then running the wrapper command, then writing the complete build files.
- Gradle 9.3.1 also required module directories to exist before the wrapper command (`:forwarder` directory must be present). Created both `app/` and `forwarder/` directories before running the wrapper.

## Known Stubs

None — this plan creates only infrastructure/schema files with no data-rendering paths.

## Next Phase Readiness

- Build compiles (`./gradlew :app:compileJava` BUILD SUCCESSFUL)
- All 5 Flyway migrations define the Phase 1 schema
- Docker Compose ready for `docker compose up -d` to start dev postgres and mailpit
- Plan 01-02 (auth entities, JWT security, Spring Security config) can proceed immediately — the build scaffold and V1 migration are in place
- Plan 01-03 (club/track/raceclass CRUD) depends on V2/V3/V4 migrations — all present
- Plan 01-04 (format config entities) depends on V5 migration — present

---
*Phase: 01-domain-foundation*
*Completed: 2026-04-16*

## Self-Check: PASSED

All 15 key files verified present. All 3 task commits (401b4ee, 655780e, 12a9e53) verified in git log.
