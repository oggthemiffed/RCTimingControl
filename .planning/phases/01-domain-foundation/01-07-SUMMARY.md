---
phase: 01
plan: 07
subsystem: integration-tests
tags: [testcontainers, integration-tests, auth, admin, spring-boot-test]
dependency_graph:
  requires: [01-04, 01-05]
  provides: [integration-test-suite, quality-gate]
  affects: []
tech_stack:
  added: [testcontainers:junit-jupiter, singleton-testcontainers-pattern]
  patterns: [AbstractIntegrationTest singleton container, @ServiceConnection, TestRestTemplate]
key_files:
  created:
    - app/src/test/java/dev/monkeypatch/rctiming/AbstractIntegrationTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/auth/AuthControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/security/SecurityIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/admin/ClubControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/admin/TrackControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/admin/RacingClassControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/admin/FormatControllerIT.java
    - app/src/test/resources/application.yml
  modified:
    - app/build.gradle.kts
    - app/src/main/java/dev/monkeypatch/rctiming/domain/auth/RefreshToken.java
    - app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfile.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/GlobalExceptionHandler.java
decisions:
  - "Singleton Testcontainers pattern (static final + static initializer) over @Testcontainers annotation to share one PostgreSQL container across all test classes"
  - "Test resources application.yml configures spring.mail.host=localhost,test-connection=false to satisfy JavaMailSender autoconfiguration without a real SMTP server"
  - "UUID-suffixed names for entities with unique constraints (RacingClass.name) to prevent cross-test collisions"
metrics:
  duration: ~30min
  completed: 2026-04-16
  tasks_completed: 2
  files_created: 8
  files_modified: 5
---

# Phase 1 Plan 7: Integration Test Suite Summary

**One-liner:** Complete integration test suite (57 tests) against real PostgreSQL via Testcontainers with singleton container pattern, covering all Phase 1 AUTH, CLUB, TRACK, RACECLASS, and FORMAT requirements.

## Tasks Completed

| Task | Description | Commit |
|------|-------------|--------|
| 1 | Auth and security integration tests | bd80461 |
| 2 | Admin API integration tests (Club, Track, RacingClass, Format) | 3a77831 |

## What Was Built

### Task 1: Auth and Security Integration Tests

**AbstractIntegrationTest** (`test/.../AbstractIntegrationTest.java`):
- Singleton `PostgreSQLContainer` via `static final` + `static {}` initializer block — one container for entire test JVM process
- `@ServiceConnection` auto-configures `spring.datasource.*` — no `@DynamicPropertySource` needed
- Uses `postgres:16-alpine` image

**AuthControllerIT** — 11 tests covering AUTH-01 through AUTH-03:
- `register_validRequest_returns201` — register happy path, roles contains RACER
- `register_duplicateEmail_returns409` — GlobalExceptionHandler maps DataIntegrityViolationException to 409
- `register_invalidEmail_returns400` — Jakarta validation on RegisterRequest
- `login_validCredentials_returns200WithTokenAndCookie` — Set-Cookie header with HttpOnly refresh_token
- `login_invalidPassword_returns401` — generic error, no credential enumeration
- `login_nonexistentEmail_returns401` — same error as wrong password
- `refresh_validCookie_returnsNewAccessToken` — extracts cookie from Set-Cookie header, sends as Cookie header
- `refresh_invalidCookie_returns401` — garbage token rejected
- `passwordResetRequest_existingEmail_returns200` — email enumeration prevention
- `passwordResetRequest_nonexistentEmail_returns200` — always 200 (AUTH-02 security)
- `passwordResetConfirm_expiredToken_returns400or410` — invalid token rejected

**SecurityIT** — 4 tests covering AUTH-04, AUTH-05:
- RACER role denied access to admin endpoints (403)
- ADMIN role allowed access to admin endpoints
- Unauthenticated requests denied (401/403)
- Stackable roles: ADMIN+RACE_DIRECTOR user can access admin endpoints

### Task 2: Admin API Integration Tests

**ClubControllerIT** — 7 tests (CLUB-01, CLUB-02):
- Profile upsert with GPS coordinates and Europe/London timezone
- Invalid timezone returns 400 (DateTimeException handler)
- Affiliation CRUD with duplicate code → 409

**TrackControllerIT** — 9 tests (TRACK-01 through TRACK-04):
- Track CRUD, decoder loops with loopType and isScoringLoop flag
- Track-wide and class-specific lap thresholds, maxLastLapMs field

**RacingClassControllerIT** — 4 tests (RACECLASS-01):
- Full CRUD with unique name constraint handling

**FormatControllerIT** — 10 tests (FORMAT-05, FORMAT-06, FORMAT-07, FORMAT-14):
- All 3 format types created (TIMED, BUMP_UP, POINTS_FINALS)
- Invalid type discriminator returns 400
- JSON export (Accept: application/json) and YAML export (Accept: application/yaml)
- JSON import and YAML import (Content-Type: application/yaml)
- Invalid schema import returns 400
- Snapshot immutability: template edit does not affect event class configSnapshot (FORMAT-06)

## Deviations from Plan

### Auto-fixed Issues (Rule 1 — Bug Fixes)

**1. [Rule 1 - Bug] Missing testcontainers:junit-jupiter dependency**
- **Found during:** Task 1 compilation
- **Issue:** `@Testcontainers` and `@Container` annotations unavailable; build.gradle.kts had `testcontainers:postgresql` but not `testcontainers:junit-jupiter`
- **Fix:** Added `testImplementation("org.testcontainers:junit-jupiter")` to build.gradle.kts
- **Files modified:** `app/build.gradle.kts`
- **Commit:** bd80461

**2. [Rule 1 - Bug] Testcontainers container lifecycle causing cross-class context reuse failures**
- **Found during:** Task 1 when running AuthControllerIT + SecurityIT together
- **Issue:** `@Testcontainers` on each subclass manages its own container lifecycle; Spring context cache reused HikariPool pointing to a stopped container
- **Fix:** Changed to singleton static initializer pattern (`static { POSTGRES.start(); }`) — one container per JVM process, shared across all test classes
- **Files modified:** `AbstractIntegrationTest.java`
- **Commit:** bd80461

**3. [Rule 1 - Bug] LazyInitializationException in AuthController.refresh()**
- **Found during:** Task 1, refresh_validCookie_returnsNewAccessToken
- **Issue:** `RefreshToken.user` was `FetchType.LAZY`; `oldToken.getUser()` called after JPA session closed in `AuthController.refresh()` (no `@Transactional` on controller)
- **Fix:** Changed `RefreshToken.user` to `FetchType.EAGER` (always needed when validating refresh token)
- **Files modified:** `domain/auth/RefreshToken.java`
- **Commit:** bd80461

**4. [Rule 1 - Bug] permitAll() endpoints returning 403 for error responses**
- **Found during:** Task 1, passwordResetConfirm_expiredToken_returns400or410
- **Issue:** When `ResponseStatusException(BAD_REQUEST)` is thrown, Spring dispatches forward to `/error`. `/error` was not in `permitAll()`, causing Spring Security to block the forward with 403
- **Fix:** Added `.requestMatchers("/error").permitAll()` to SecurityConfig
- **Files modified:** `security/SecurityConfig.java`
- **Commit:** bd80461

**5. [Rule 1 - Bug] ClubProfile @Lob annotation incompatible with PostgreSQL bytea column**
- **Found during:** Task 2, ClubControllerIT tests
- **Issue:** Hibernate 6 maps `@Lob byte[]` to PostgreSQL `oid` type, but the schema defines `logo` as `bytea`. PSQL error: "column logo is of type bytea but expression is of type oid"
- **Fix:** Removed `@Lob` annotation from `ClubProfile.logo`; plain `byte[]` maps correctly to `bytea` in Hibernate 6
- **Files modified:** `domain/club/ClubProfile.java`
- **Commit:** 3a77831

**6. [Rule 2 - Missing error handling] IllegalArgumentException from format import not handled**
- **Found during:** Task 2, FormatControllerIT.importInvalidSchema_returns400
- **Issue:** `RaceFormatController.importFormat()` catches all exceptions and re-throws as `IllegalArgumentException`. `GlobalExceptionHandler` had no handler for this, causing 500 instead of 400
- **Fix:** Added `@ExceptionHandler(IllegalArgumentException.class)` returning 400 ProblemDetail to `GlobalExceptionHandler`
- **Files modified:** `api/GlobalExceptionHandler.java`
- **Commit:** 3a77831

**7. [Rule 2 - Missing test isolation] RacingClass unique name constraint causes cross-test conflicts**
- **Found during:** Task 2, TrackControllerIT.setLapThreshold_classSpecific
- **Issue:** Multiple test classes create racing classes with the same names (e.g., "Electric Open"). The `racing_classes.name UNIQUE` constraint causes 409 errors in later test classes
- **Fix:** Added UUID suffixes to all racing class names created in tests
- **Files modified:** `TrackControllerIT.java`, `RacingClassControllerIT.java`
- **Commit:** 3a77831

### Test Infrastructure Addition (Rule 3 — Blocking Issue)

**8. [Rule 3 - Blocking] JavaMailSender bean not available in test context**
- **Found during:** Task 1, first test run
- **Issue:** `PasswordResetService` requires `JavaMailSender`. No mail config for tests → Spring context failed to start
- **Fix:** Added `app/src/test/resources/application.yml` with `spring.mail.host=localhost, port=25, test-connection=false`
- **Files created:** `app/src/test/resources/application.yml`
- **Commit:** bd80461

## Threat Model Coverage

| Threat ID | Status |
|-----------|--------|
| T-01-24: Test database isolation | Mitigated — Testcontainers creates ephemeral PostgreSQL per JVM process |
| T-01-25: Test admin user elevation | Accepted — test-only code path, not reachable in production |

## Known Stubs

None — all integration tests exercise real production code paths.

## Threat Flags

None — no new security surface introduced.

## Self-Check: PASSED

Files created:
- app/src/test/java/dev/monkeypatch/rctiming/AbstractIntegrationTest.java — present
- app/src/test/java/dev/monkeypatch/rctiming/api/auth/AuthControllerIT.java — present
- app/src/test/java/dev/monkeypatch/rctiming/security/SecurityIT.java — present
- app/src/test/java/dev/monkeypatch/rctiming/api/admin/ClubControllerIT.java — present
- app/src/test/java/dev/monkeypatch/rctiming/api/admin/TrackControllerIT.java — present
- app/src/test/java/dev/monkeypatch/rctiming/api/admin/RacingClassControllerIT.java — present
- app/src/test/java/dev/monkeypatch/rctiming/api/admin/FormatControllerIT.java — present
- app/src/test/resources/application.yml — present

Commits verified:
- bd80461: feat(01-07): auth and security integration tests (Task 1)
- 3a77831: feat(01-07): admin API integration tests (Task 2)

Test suite: ./gradlew :app:test — BUILD SUCCESSFUL, 57 tests, 0 failures
Test count: 57 (> required minimum of 25)
@ServiceConnection: present in AbstractIntegrationTest
@DynamicPropertySource: not used (only appears in a comment)
