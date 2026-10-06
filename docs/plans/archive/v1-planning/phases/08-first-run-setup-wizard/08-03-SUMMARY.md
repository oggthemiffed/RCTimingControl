---
phase: 08-first-run-setup-wizard
plan: "03"
subsystem: setup-backend
tags:
  - setup-wizard
  - backend
  - decoder-config
  - forwarder
dependency_graph:
  requires:
    - Plan 08-02 (SetupController/SetupService skeleton, V25 migration, bootstrap endpoint)
  provides:
    - PATCH /api/v1/setup/decoder-config — ADMIN-only; updates decoderHost/Port/Protocol on ClubProfile; returns SetupProgressDto
    - POST /api/v1/setup/staff — ADMIN-only; creates staff user with ADMIN/RACE_DIRECTOR/REFEREE roles; 201 on success
    - GET /api/v1/setup/forwarder-config-download — ADMIN-only; streams fresh forwarder.env with placeholder token (T-08-03 mitigated)
    - ClubProfileService.updateDecoderConfig(host, port, protocol)
    - UserService.createStaff(email, password, firstName, lastName, Set<Role>)
    - DecoderConfigUpdateRequest DTO with @Pattern(regexp="RC4|P3") validation
    - SetupStaffRequest DTO with @NotEmpty Set<String> roles
  affects:
    - Plan 08-04 (frontend wizard steps consume these endpoints)
tech_stack:
  added: []
  patterns:
    - PATCH endpoint returning existing aggregate DTO (progress) for single-round-trip sidebar update
    - Text/plain attachment response via ResponseEntity<byte[]>
    - ServletUriComponentsBuilder.fromRequestUri() for base URL derivation
    - Forwarder env file generated fresh per request — never cached
key_files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/api/setup/dto/DecoderConfigUpdateRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/setup/dto/SetupStaffRequest.java
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/api/setup/SetupController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/setup/SetupService.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfileService.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserService.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/setup/SetupControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/setup/SetupServiceTest.java
decisions:
  - base-url-derivation: Used ServletUriComponentsBuilder.fromRequestUri().replacePath("") rather than fromCurrentContextPath() to correctly strip the path and produce just scheme+host+port
  - staff-endpoint-returns-204-void: POST /setup/staff returns 204 No Content (void) — a minimal response; frontend redirects immediately after staff creation
  - decoder-config-patch-returns-progress: PATCH /setup/decoder-config returns the full SetupProgressDto so the sidebar can update without a second round-trip
metrics:
  duration: ~25m
  completed: 2026-05-06
  tasks: 2
  files_changed: 8
---

# Phase 08 Plan 03: Setup Backend — Decoder Config + Staff + Forwarder Config Download Summary

## One-liner

Decoder config PATCH, staff POST, and forwarder.env GET endpoints with bcrypt-safe token placeholder (T-08-03 mitigated) and activated integration tests.

## What Was Delivered

**DecoderConfigUpdateRequest DTO** (`api/setup/dto/`): Java record with `@NotBlank @Size(max=255)` on `decoderHost`, `@NotNull @Min(1) @Max(65535)` on `decoderPort`, and `@NotNull @Pattern(regexp="RC4|P3")` on `decoderProtocol`. Mitigates T-08-09 by rejecting any protocol value outside RC4/P3 at the Bean Validation layer.

**SetupStaffRequest DTO** (`api/setup/dto/`): Java record mirroring `RegisterRequest` field constraints plus a `@NotEmpty Set<String> roles` field. Role names validated at parse time against `Role.valueOf()` in the controller.

**ClubProfileService.updateDecoderConfig(host, port, protocol)**: Loads the singleton `ClubProfile` via `findAll().stream().findFirst()`, throws `IllegalStateException` if none exists, updates the three decoder fields and `updatedAt`, saves.

**UserService.createStaff(email, password, firstName, lastName, Set<Role>)**: Rejects null/empty role sets (`IllegalArgumentException`) and any set containing `Role.RACER` (T-08-08 mitigation). Checks email uniqueness before persist. Follows the same structural pattern as `createAdmin`.

**SetupController** — three new endpoints added:
- `PATCH /api/v1/setup/decoder-config` — delegates to `ClubProfileService.updateDecoderConfig`, returns `SetupProgressDto` for sidebar update in one round trip. `@PreAuthorize("hasRole('ADMIN')")`.
- `POST /api/v1/setup/staff` — maps role strings to `Role` enum values, calls `UserService.createStaff`, returns 201 void. `@PreAuthorize("hasRole('ADMIN')")`.
- `GET /api/v1/setup/forwarder-config-download` — returns `ResponseEntity<byte[]>` with `Content-Disposition: attachment; filename="forwarder.env"` and `Content-Type: text/plain`. Delegates body generation to `SetupService.generateForwarderEnv(HttpServletRequest)`. `@PreAuthorize("hasRole('ADMIN')")`.

**SetupService.generateForwarderEnv(HttpServletRequest)**: Derives base URL via `ServletUriComponentsBuilder.fromRequestUri().replacePath("")`. Reads current `ForwarderTokenService.getCurrentStatus()` for the comment header (status, generatedAt timestamp). The `APP_FORWARDER_TOKEN` line always contains the literal string `<paste-your-token-here>` — the bcrypt hash is never included (T-08-03).

**SetupControllerIT** — two previously `@Disabled` tests activated:
- `downloadForwarderConfig_returnsEnvAttachment`: Asserts 200 OK, Content-Disposition contains `attachment` and `forwarder.env`, body contains `APP_SERVER_URL=` and `APP_DECODER_HOST=`.
- `downloadForwarderConfig_includesTokenPlaceholder_notPlaintext`: Asserts body contains verbatim `APP_FORWARDER_TOKEN=<paste-your-token-here>` (T-08-03).

**SetupServiceTest** — added `ForwarderTokenService` mock to satisfy the updated 7-arg `SetupService` constructor.

## Test Results

```
./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.setup.SetupControllerIT"  → BUILD SUCCESSFUL (7 tests, 0 failures)
./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.setup.SetupServiceTest"   → BUILD SUCCESSFUL (4 tests, 0 failures)
```

All 7 IT methods pass (5 from Plan 02 + 2 activated here). No regressions.

## Deviations from Plan

**SetupServiceTest constructor update (Rule 3 — blocking issue)**: The plan specified adding `ForwarderTokenService` to `SetupService`'s constructor. The existing `SetupServiceTest` used a 6-arg constructor call that no longer compiled. Updated the unit test to add a `@Mock ForwarderTokenService` and pass it as the 7th argument. This was a trivial compile fix, not a design change.

## Threat Flags

No new security-relevant surface beyond what is documented in the plan's threat model. All three threat mitigations implemented:

| Flag | File | Status |
|------|------|--------|
| T-08-03 mitigated | SetupService.java | APP_FORWARDER_TOKEN line contains literal placeholder; verified by IT test |
| T-08-08 mitigated | UserService.java | createStaff() rejects Role.RACER with IllegalArgumentException |
| T-08-09 mitigated | DecoderConfigUpdateRequest.java | @Pattern(regexp="RC4\|P3") rejects any other protocol string |

## Known Stubs

None — all fields in the env download are wired to real data (ClubProfile decoder fields, ForwarderTokenService status). The `APP_FORWARDER_TOKEN=<paste-your-token-here>` placeholder is intentional and correct by design (T-08-03).

## Self-Check: PASSED

- `app/.../dto/DecoderConfigUpdateRequest.java` exists: FOUND
- `app/.../dto/SetupStaffRequest.java` exists: FOUND
- Commits b8369fb and ca7f0e5 exist in git log: FOUND
- All SetupControllerIT tests pass: CONFIRMED (BUILD SUCCESSFUL)
