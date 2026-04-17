---
phase: "02"
plan: "03"
subsystem: racer-profile-transponders
tags: [profile, transponders, memberships, class-ratings, domain, api, integration-tests]
depends_on: ["02-01"]
provides:
  - User entity extended with 4 contact fields (phone_number, emergency_contact_name/phone, phonetic_name)
  - UserGoverningBodyMembership entity + repository + service (CRUD with duplicate 409)
  - UserClassRating entity with composite PK via @IdClass (read-only from racer perspective)
  - Transponder entity + repository + service (system-wide unique transponder_number enforced by DB UNIQUE + GlobalExceptionHandler)
  - RacerProfileService: getProfile, updateProfile (safe — no email/role mutation path), membership CRUD
  - TransponderService: findForUser (ownership-scoped), create (DIVE propagates on dup), delete (ownership filter)
  - RacerProfileController: GET/PATCH /api/v1/racer/profile + membership sub-resource CRUD
  - TransponderController: GET/POST/DELETE /api/v1/racer/transponders
  - RacerProfileControllerIT: 8 tests covering RACER-01, RACER-13
  - TransponderControllerIT: 6 tests covering RACER-03, RACER-05
affects:
  - app/src/main/java/dev/monkeypatch/rctiming/domain/user/User.java
  - app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategory.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategoryService.java
tech_stack:
  added: []
  patterns:
    - Cross-aggregate access via explicit repository lookups (no @OneToMany on User)
    - DataIntegrityViolationException propagation to GlobalExceptionHandler for 409 (no app-level dedup)
    - @IdClass composite PK pattern for user_class_ratings
    - Ownership filter in service layer (filter + orElseThrow) for transponder delete
    - HttpStatusEntryPoint(UNAUTHORIZED) for stateless API 401 on anonymous requests
key_files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserGoverningBodyMembership.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserGoverningBodyMembershipRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserClassRating.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserClassRatingRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/RacerProfileService.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/transponder/Transponder.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/transponder/TransponderRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/transponder/TransponderService.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/RacerProfileController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/TransponderController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/RacerProfileDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/UpdateRacerProfileRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/MembershipDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/UpsertMembershipRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/ClassRatingDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/TransponderDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/CreateTransponderRequest.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/racer/RacerProfileControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/racer/TransponderControllerIT.java
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/User.java
    - app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategory.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategoryService.java
decisions:
  - "RacerProfileService created as a separate service rather than extending UserService — UserService owns authentication-sensitive operations (password, email); profile edits are a separate bounded concern"
  - "DTOs created in same commit as services since services return DTOs — avoids compile failures in ordered commits"
  - "ClassRating write path deferred — racer portal is read-only for class ratings per RACER-12; admin write path is Phase 7"
  - "HttpStatusEntryPoint(UNAUTHORIZED) added to SecurityConfig — stateless JWT API should return 401 for anonymous requests; default Spring Security returns 403 via method security AccessDeniedException"
metrics:
  duration_minutes: 10
  completed_date: "2026-04-17"
  tasks_completed: 3
  tasks_total: 3
  files_created: 19
  files_modified: 4
---

# Phase 02 Plan 03: Racer Profile + Transponders Domain + API Summary

**One-liner:** Racer profile domain with User entity extension (4 contact fields), membership/class-rating/transponder CRUD, two ownership-scoped REST controllers, and 14 integration tests verifying RACER-01, RACER-03, RACER-05, RACER-12, RACER-13.

## What Was Built

### Task 1: Domain entities, repositories, DTOs, and services

**User entity extension** — Added 4 nullable columns mapped to V6 schema: `phoneNumber`, `emergencyContactName`, `emergencyContactPhone`, `phoneticName`. Getters/setters added in the existing explicit style (no Lombok).

**UserGoverningBodyMembership** — Entity mapped to `user_governing_body_memberships` (V7). No `@ManyToOne` to User — cross-aggregate access via `Long userId` per PATTERNS.md. DB UNIQUE on `(user_id, governing_body_code)` triggers `DataIntegrityViolationException` → GlobalExceptionHandler → 409.

**UserClassRating** — Entity with composite PK via `@IdClass(UserClassRatingId.class)`. Static inner class `UserClassRatingId` implements `Serializable` with `equals`/`hashCode`. Read-only from the racer portal (no write endpoint).

**Transponder** — Entity mapped to `transponders` (V11). `@Column(unique = true)` on `transponderNumber` mirrors the DB UNIQUE constraint. `TransponderService.delete` filters by `userId` before delete — cross-user access returns 404 (EntityNotFoundException).

**RacerProfileService** — `@Service @Transactional`. Critical safety: `updateProfile` never calls `setEmail`, `setPasswordHash`, or `setRoles`. Profile updates apply only non-null fields from `UpdateRacerProfileRequest` (which has no email/password/role fields). Membership operations are idempotent on delete (no-op if absent).

**All 7 DTOs** created alongside services (required for service return types to compile): `RacerProfileDto`, `UpdateRacerProfileRequest`, `MembershipDto`, `UpsertMembershipRequest`, `ClassRatingDto`, `TransponderDto`, `CreateTransponderRequest`.

### Task 2: REST controllers

**RacerProfileController** — `@RequestMapping("/api/v1/racer")`, `@PreAuthorize("hasRole('RACER')")`. Endpoints: `GET /profile`, `PATCH /profile`, `GET /memberships`, `POST /memberships` (201), `PUT /memberships/{code}` (200), `DELETE /memberships/{code}` (204). userId extracted from JWT subject via `Long.parseLong(auth.getName())`.

**TransponderController** — `@RequestMapping("/api/v1/racer/transponders")`. Endpoints: `GET` (list), `POST` (201), `DELETE /{id}` (204). No `catch (DataIntegrityViolationException)` — propagates to GlobalExceptionHandler.

### Task 3: Integration tests

**RacerProfileControllerIT** (8 tests):
- `getProfile_returnsIdentityAndContact` — verifies response shape including empty memberships/classRatings lists
- `patchProfile_updatesOnlySuppliedFields` — partial update preserves untouched fields
- `patchProfile_ignoresEmailField` — raw JSON with `"email"` field doesn't change stored email (Jackson drops unknown properties)
- `patchProfile_ignoresRoleField` — raw JSON with `"roles"` field doesn't change stored roles
- `addMembership_returns201` — POST creates membership, GET confirms presence
- `addMembership_duplicate_returns409` — second POST same governing body code → 409
- `updateMembership_modifiesNumber` — PUT changes membershipNumber
- `removeMembership_returns204` — DELETE removes membership, GET returns empty list

**TransponderControllerIT** (6 tests):
- `createTransponder_returns201` — POST creates transponder
- `listTransponders_returnsUsersOwnOnly` — racerA and racerB each see only their own transponders
- `createTransponder_duplicate_returns409` — same transponder number by different user → 409 from DB UNIQUE
- `deleteTransponder_returns204` — POST then DELETE, GET returns empty list
- `deleteAnotherUsersTransponder_returns404` — racerA's transponder, racerB DELETE → 404
- `anonymous_returns401` — no Authorization header → 401

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] CarTagCategory getter type mismatch caused Hibernate schema validation failure**
- **Found during:** Task 3 (first test run — Spring context failed to start)
- **Issue:** `CarTagCategory.getSortOrder()` returned `int` (Java widening from field `short`). Hibernate 6 uses the getter return type for schema validation — `int` maps to `INTEGER` but the DB column is `smallint` (`int2`). Validation message: "found [int2 (Types#SMALLINT)], but expecting [integer (Types#INTEGER)]"
- **Fix:** Changed `getSortOrder()` return type to `short` and `setSortOrder()` parameter to `short`. Updated `CarTagCategoryService` to call `.shortValue()` on the `Integer` request field.
- **Files modified:** `CarTagCategory.java`, `CarTagCategoryService.java`
- **Commit:** `553a5d7`

**2. [Rule 2 - Missing] Spring Security returned 403 instead of 401 for anonymous requests**
- **Found during:** Task 3 (`anonymous_returns401` test failed with HTTP 403)
- **Issue:** `SecurityConfig` had no `AuthenticationEntryPoint`. Method-level `@PreAuthorize` throws `AccessDeniedException` for anonymous users, which `GlobalExceptionHandler` maps to 403. Stateless JWT APIs should return 401 for unauthenticated requests.
- **Fix:** Added `.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))` to `SecurityConfig.filterChain`.
- **Files modified:** `SecurityConfig.java`
- **Commit:** `553a5d7`

## Known Stubs

None — all endpoints return real data from the database. ClassRating list is empty by default (no admin write path in this phase) but that is correct behavior, not a stub.

## Threat Surface Scan

All threat model mitigations applied:

| Threat ID | Mitigation Applied |
|-----------|-------------------|
| T-02-03-01 | `UpdateRacerProfileRequest` record has no email/roles/passwordHash fields; service never calls setEmail/setRoles |
| T-02-03-02 | DB UNIQUE (user_id, governing_body_code) + service-level userId scoping on all membership operations |
| T-02-03-03 | `TransponderService.findForUser` filters by userId — no cross-user list path |
| T-02-03-04 | GlobalExceptionHandler returns generic "Resource already exists" — no owning user identity leak |
| T-02-03-05 | `TransponderService.delete` uses `.filter(t -> t.getUserId().equals(userId))` before delete |
| T-02-03-06 | No racer-facing write path for class ratings |
| T-02-03-07 | Accepted — JWT subject is opaque integer |

No new threat surface introduced beyond what was modeled in the plan.

## Self-Check

Checking created files exist and commits are present...

- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserGoverningBodyMembership.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserClassRating.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/domain/user/RacerProfileService.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/domain/transponder/Transponder.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/domain/transponder/TransponderService.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/api/racer/RacerProfileController.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/api/racer/TransponderController.java` — present
- `/home/david/git/java/RCTimingControl/app/src/test/java/dev/monkeypatch/rctiming/api/racer/RacerProfileControllerIT.java` — present
- `/home/david/git/java/RCTimingControl/app/src/test/java/dev/monkeypatch/rctiming/api/racer/TransponderControllerIT.java` — present
- commit 6720542 — present (Task 1)
- commit 80c65e8 — present (Task 2)
- commit 553a5d7 — present (Task 3)

## Self-Check: PASSED
