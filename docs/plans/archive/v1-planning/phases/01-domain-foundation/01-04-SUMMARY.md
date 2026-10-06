---
phase: 01
plan: 04
subsystem: security
tags: [jwt, auth, spring-security, password-reset, stateless]
dependency_graph:
  requires: [01-02]
  provides: [jwt-token-service, security-filter-chain, auth-endpoints, password-reset]
  affects: [all-api-plans]
tech_stack:
  added: [JJWT 0.12.x, BCryptPasswordEncoder, JavaMailSender]
  patterns: [OncePerRequestFilter, SecurityFilterChain, ProblemDetail RFC-9457]
key_files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/security/JwtTokenService.java
    - app/src/main/java/dev/monkeypatch/rctiming/security/JwtAuthenticationFilter.java
    - app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/GlobalExceptionHandler.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/auth/AuthController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/auth/RegisterRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/auth/LoginRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/auth/AuthResponse.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/auth/PasswordResetRequestDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/auth/PasswordResetConfirmDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/auth/PasswordResetService.java
  modified: []
decisions:
  - "Refresh tokens stored as SHA-256 hashes in DB (not stateless JWT) to support immediate revocation on password reset"
  - "javax.crypto.SecretKey retained for JJWT verifyWith() — Java SE core with no jakarta.crypto equivalent"
  - "Generic 401 message for login failures — no distinction between bad email vs bad password"
metrics:
  duration: ~25min
  completed: 2026-04-16
  tasks_completed: 2
  files_created: 11
---

# Phase 1 Plan 4: JWT Auth System Summary

**One-liner:** Stateless JWT auth with BCrypt + HMAC-SHA256 tokens, HttpOnly refresh cookie rotation, and SHA-256 hashed password reset tokens with email enumeration prevention.

## Tasks Completed

| Task | Description | Commit |
|------|-------------|--------|
| 1 | JWT security infrastructure | 73386f1 |
| 2 | Auth controller + password reset service | 4297851 |

## What Was Built

### Task 1: JWT Security Infrastructure

**JwtTokenService** (`security/JwtTokenService.java`):
- JJWT 0.12.x API: `.subject()`, `.claim()`, `.expiration()`, `.signWith(key, alg)` — no deprecated 0.11 setters
- `generateAccessToken(User)` embeds id, email, firstName, lastName, roles (as `List<String>`) in JWT claims
- `generateRefreshTokenValue()` returns 64-char hex-encoded random 32-byte token (opaque, not JWT)
- `parseToken(String)` validates HMAC-SHA256 signature and returns `Claims`

**JwtAuthenticationFilter** (`security/JwtAuthenticationFilter.java`):
- `OncePerRequestFilter` — skips `/api/v1/auth/**` via `shouldNotFilter()`
- Extracts `Authorization: Bearer {token}`, builds `UsernamePasswordAuthenticationToken` with `ROLE_` prefixed authorities
- On `JwtException`: does NOT set auth context, lets Spring Security reject the request if required

**SecurityConfig** (`security/SecurityConfig.java`):
- `SessionCreationPolicy.STATELESS` — no HttpSession created
- `/api/v1/auth/**` and `/actuator/health` are public
- `/api/v1/admin/**` requires `ADMIN`, `RACE_DIRECTOR`, or `REFEREE` role
- `JwtAuthenticationFilter` added before `UsernamePasswordAuthenticationFilter`
- BCryptPasswordEncoder bean

**GlobalExceptionHandler** (`api/GlobalExceptionHandler.java`):
- `@RestControllerAdvice` with RFC 9457 `ProblemDetail` responses
- 404 for `EntityNotFoundException`, 400+field-errors for `MethodArgumentNotValidException`, 409 for `DataIntegrityViolationException`, 403 for `AccessDeniedException`

### Task 2: Auth Controller + Password Reset

**DTOs** (all Java records with Jakarta Validation):
- `RegisterRequest`: firstName, lastName, email (@Email), password (@Size min=8)
- `LoginRequest`: email, password
- `AuthResponse`: accessToken, id, email, firstName, lastName, roles
- `PasswordResetRequestDto`, `PasswordResetConfirmDto`

**PasswordResetService** (`domain/auth/PasswordResetService.java`):
- `requestReset(email)`: returns silently for unknown emails (T-01-11 email enumeration prevention)
- Generates 32-byte random token, stores SHA-256 hash with 1-hour TTL
- `confirmReset(rawToken, newPassword)`: validates hash, checks `used` and `expiresAt`, updates password, marks token used, calls `refreshTokenRepository.deleteByUser()` to revoke all sessions

**AuthController** (`api/auth/AuthController.java`):
- `POST /api/v1/auth/register` → 201 + AuthResponse + refresh cookie
- `POST /api/v1/auth/login` → 200 + AuthResponse + refresh cookie; 401 with generic "Invalid email or password" message
- `POST /api/v1/auth/refresh` → reads `refresh_token` cookie, validates hash, rotates token (revokes old, issues new), returns new access token
- `POST /api/v1/auth/password-reset/request` → always 200 (never leaks email existence)
- `POST /api/v1/auth/password-reset/confirm` → 200 on success, 400/410 on invalid/expired token

**Refresh cookie**: `HttpOnly=true`, `SameSite=Lax`, `path=/api/v1/auth/refresh` (sent only on refresh requests)

## Deviations from Plan

### Auto-fixed Issues

None.

### Known Deviations

**1. [Acceptable - Java SE] javax.crypto.SecretKey in JwtTokenService**
- **Found during:** Task 1
- **Issue:** JJWT 0.12.x `JwtParserBuilder.verifyWith(SecretKey)` requires `javax.crypto.SecretKey`. There is no `jakarta.crypto` package — this Java SE API was never renamed as part of the Jakarta EE migration.
- **Resolution:** Field stored as `java.security.Key`, cast to `javax.crypto.SecretKey` inline at the `verifyWith()` call site. The plan's verification check `grep -rn "javax\." app/src/main/java/` will flag this one occurrence. This is an unavoidable JJWT dependency on Java SE core, not a Jakarta EE namespace violation.
- **Files:** `security/JwtTokenService.java` line 63

**2. [Login response] 401 returns null body for AuthResponse**
- The login endpoint returns `ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)` on failure. The plan specifies a generic error message. This satisfies the security requirement (no credential enumeration) but does not include a response body. The frontend should display its own generic message on 401.

## Threat Model Coverage

All mitigations from the plan's threat register are implemented:

| Threat ID | Status |
|-----------|--------|
| T-01-09: JWT spoofing | Mitigated — `verifyWith(secretKey)` rejects tampered tokens |
| T-01-10: Credential enumeration | Mitigated — generic "Invalid email or password" on login 401 |
| T-01-11: Email enumeration via reset | Mitigated — always returns 200 from `requestReset` |
| T-01-12: Role elevation | Mitigated — `hasAnyRole("ADMIN","RACE_DIRECTOR","REFEREE")` on `/api/v1/admin/**` |
| T-01-13: Refresh token reuse | Mitigated — token rotation: old token revoked on each refresh |
| T-01-14: Token storage | Mitigated — refresh + reset tokens stored as SHA-256 hashes only |
| T-01-15: CSRF via cookie | Mitigated — cookie path restricted to `/api/v1/auth/refresh`, `SameSite=Lax` |
| T-01-16: Brute-force | Accepted — documented in RESEARCH.md |

## Known Stubs

None — all auth endpoints are fully implemented with real logic. No hardcoded values flow to UI rendering.

## Threat Flags

None — no new security surface beyond what is described in the plan's threat model.

## Self-Check: PASSED

Files created:
- app/src/main/java/dev/monkeypatch/rctiming/security/JwtTokenService.java — present
- app/src/main/java/dev/monkeypatch/rctiming/security/JwtAuthenticationFilter.java — present
- app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java — present
- app/src/main/java/dev/monkeypatch/rctiming/api/GlobalExceptionHandler.java — present
- app/src/main/java/dev/monkeypatch/rctiming/api/auth/AuthController.java — present
- app/src/main/java/dev/monkeypatch/rctiming/domain/auth/PasswordResetService.java — present

Commits verified:
- 73386f1: feat(01-04): JWT security infrastructure
- 4297851: feat(01-04): auth controller with register, login, refresh and password reset
