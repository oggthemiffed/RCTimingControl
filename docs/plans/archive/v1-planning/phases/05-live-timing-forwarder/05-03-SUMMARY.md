---
phase: 05-live-timing-forwarder
plan: "03"
subsystem: forwarder-token-lifecycle
tags: [forwarder, auth, bcrypt, flyway, rest, admin]
dependency_graph:
  requires: [05-01]
  provides: [ForwarderTokenService.validate(), POST/GET/DELETE /api/v1/admin/forwarder/token]
  affects: [05-04-grpc-auth-interceptor, 05-05-admin-ui]
tech_stack:
  added: []
  patterns: [SecureRandom 256-bit token, BCrypt hash storage, one-time-reveal HTTP response, Spring @PreAuthorize ADMIN role]
key_files:
  created:
    - app/src/main/resources/db/migration/V21__create_forwarder_token.sql
    - app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenStatus.java
    - app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderToken.java
    - app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenService.java
    - app/src/main/java/dev/monkeypatch/rctiming/forwarder/dto/ForwarderTokenStatusDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/forwarder/dto/ForwarderTokenGenerateResponseDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/admin/ForwarderTokenController.java
    - app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenControllerTest.java
  modified:
    - app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenServiceTest.java
decisions:
  - Wave-0 @Disabled stub in ForwarderTokenServiceTest replaced with 6 real integration tests (Testcontainers)
  - forwarderTokenRepository.deleteAll() in @BeforeEach isolates controller tests against shared Testcontainer DB state
  - ForwarderToken entity uses package-private setters (consistent with plan spec; public getters for service layer)
metrics:
  duration: "~12 minutes"
  completed: "2026-04-26"
  tasks_completed: 2
  files_changed: 10
---

# Phase 05 Plan 03: Forwarder Admin Token Lifecycle Summary

**One-liner:** Admin token lifecycle with SecureRandom 256-bit plaintext, BCrypt hash storage, one-time-reveal POST endpoint, and ADMIN-gated REST controller — FORWARDER-05 complete.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | V21 migration + entity + service + 6 tests | 783a660 | V21 SQL, ForwarderToken, Status, Repository, Service, ServiceTest |
| 2 | Controller + DTOs + 5 controller tests | c654ef9 | ForwarderTokenController, 2 DTOs, ControllerTest |

## What Was Built

### Task 1: Persistence + Service

- **V21__create_forwarder_token.sql** — `forwarder_token` table with `BIGSERIAL` id, `token_hash VARCHAR(255)`, `status VARCHAR(16) CHECK (status IN ('ACTIVE','REVOKED'))`, `generated_at TIMESTAMPTZ`, `revoked_at TIMESTAMPTZ`, plus `idx_forwarder_token_status` index.
- **ForwarderTokenStatus** — enum `ACTIVE | REVOKED`
- **ForwarderToken** — Hibernate `@Entity` with `TIMESTAMPTZ` `columnDefinition`, package-private setters.
- **ForwarderTokenRepository** — Spring Data JPA with `findAllByStatus`, `findFirstByStatusOrderByGeneratedAtDesc`, `findFirstByOrderByGeneratedAtDesc`.
- **ForwarderTokenService** — `@Service @Transactional`:
  - `generate()` — `SecureRandom` 32-byte → Base64URL no-padding (43 chars), `passwordEncoder.encode(plaintext)` stored as BCrypt hash, revokes any existing ACTIVE tokens first.
  - `revoke()` — marks ACTIVE token REVOKED, idempotent.
  - `validate(plaintext)` — iterates ACTIVE tokens, `passwordEncoder.matches()`; returns `Optional.empty()` for null/blank input or no match.
  - `getCurrentStatus()` — returns `CurrentStatus` record (ACTIVE/REVOKED/NONE + timestamps).
- **ForwarderTokenServiceTest** — `@Disabled` removed; 6 tests passing via Testcontainers.

### Task 2: REST Controller + DTOs

- **ForwarderTokenStatusDto** — record `(String status, Instant generatedAt, Instant revokedAt)` — **no hash field**.
- **ForwarderTokenGenerateResponseDto** — record `(String token, String status, Instant generatedAt)`.
- **ForwarderTokenController** — `@RestController @RequestMapping("/api/v1/admin/forwarder/token") @PreAuthorize("hasRole('ADMIN')")`:
  - `GET` → 200 `ForwarderTokenStatusDto` (status = ACTIVE/REVOKED/NONE, no token value)
  - `POST` → 201 `ForwarderTokenGenerateResponseDto` (plaintext shown once)
  - `DELETE` → 204 (revoke, idempotent)
  - Class-level Javadoc notes HTTPS recommendation for production (T-05-11 accepted risk).
- **ForwarderTokenControllerTest** — 5 integration tests: NONE state, generate once, revoke, 403 racer, 401 unauthenticated.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Shared Testcontainer DB caused getReturnsNoneWhenNoTokenGenerated() to fail**
- **Found during:** Task 2 initial test run
- **Issue:** `ForwarderTokenServiceTest` leaves active tokens in the shared Testcontainer database. `ForwarderTokenControllerTest.getReturnsNoneWhenNoTokenGenerated()` expected `NONE` but found `ACTIVE`.
- **Fix:** Added `forwarderTokenRepository.deleteAll()` in `ForwarderTokenControllerTest.@BeforeEach` to isolate each controller test.
- **Files modified:** `ForwarderTokenControllerTest.java`
- **Commit:** c654ef9

## Known Stubs

None — all service and controller functionality is fully wired.

## Threat Surface Scan

All security surfaces are within the plan's threat model (T-05-08 through T-05-13). No new surfaces introduced beyond what was planned.

## Deferred Issues

**Pre-existing failure (out of scope):** `RoundGeneratorServiceTest` fails with `UnnecessaryStubbingException` in Mockito — confirmed pre-existing before Plan 03 changes. Not introduced by this plan. Logged to `.planning/phases/05-live-timing-forwarder/deferred-items.md`.

## Self-Check: PASSED

- `app/src/main/resources/db/migration/V21__create_forwarder_token.sql` ✓ exists
- `app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderToken.java` ✓ exists
- `app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenService.java` ✓ exists
- `app/src/main/java/dev/monkeypatch/rctiming/api/admin/ForwarderTokenController.java` ✓ exists
- `app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenControllerTest.java` ✓ exists
- Commit 783a660 ✓ exists
- Commit c654ef9 ✓ exists
- 11 forwarder tests passing (6 service + 5 controller) ✓
