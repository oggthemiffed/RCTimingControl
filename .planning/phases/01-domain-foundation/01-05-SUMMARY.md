---
plan: 01-05
phase: 01-domain-foundation
status: complete
wave: 3
completed: 2026-04-16
commits:
  - d44f52d
  - 40bdbf1
---

# Plan 01-05 Summary: Admin REST Controllers

## What Was Built

All admin management APIs for club, track, racing class, and race format, completing the write-side HTTP surface for Phase 1.

### Task 1: Club, Track, and RacingClass APIs
- **ClubProfileController** (`/api/v1/admin/club`) — singleton profile upsert with IANA timezone validation, governing body affiliation CRUD
- **TrackController** (`/api/v1/admin/tracks`) — track CRUD with nested decoder loop and lap threshold management
- **RacingClassController** (`/api/v1/admin/classes`) — standard CRUD
- **Services**: `ClubProfileService`, `TrackService`, `RacingClassService` — all `@Service @Transactional`, constructor injection, `EntityNotFoundException` on missing resources
- **DTOs**: All Java records with Jakarta Validation (`@NotBlank`, `@Email`, `@Size`, `@NotNull`, `@Min`)

### Task 2: Race Format Controller with Export/Import
- **RaceFormatController** (`/api/v1/admin/formats`) — full CRUD for format templates
- **Export endpoint** — `GET /{id}/export` content-negotiates `application/json` vs `application/yaml` via Accept header
- **Import endpoint** — `POST /import` accepts both content types; uses `@Qualifier("yamlObjectMapper")` for YAML parsing (not the `@Primary` JSON mapper)
- **RaceFormatService** extended — added `findAll`, `findById`, `create`, `update`, `delete`, `exportConfig`, `importConfig`
- **GlobalExceptionHandler** — merged both agents' contributions: `AccessDeniedException` (from 01-04) + `DateTimeException` for timezone validation errors (from 01-05)

## Key Files Created

- `api/admin/ClubProfileController.java`
- `api/admin/TrackController.java`
- `api/admin/RacingClassController.java`
- `api/admin/RaceFormatController.java`
- `api/admin/dto/` — 15 DTO records
- `domain/club/ClubProfileService.java`
- `domain/track/TrackService.java`
- `domain/raceclass/RacingClassService.java`

## Deviations

- **GlobalExceptionHandler overlap**: Both 01-04 and 01-05 generated `GlobalExceptionHandler.java`. Resolved by merging: kept `AccessDeniedException` from 01-04 and `DateTimeException` from 01-05. All five exception types are present.
- **JwtTokenService fix**: Field `signingKey` typed as `java.security.Key` caused JJWT 0.12.x `signWith` generic inference failure. Changed to `javax.crypto.SecretKey` (which `Keys.hmacShaKeyFor` always returns). Fix committed as `d188ed4`.
- **Bash restriction on 01-05 agent**: Agent completed file creation but could not commit. Commits applied manually by orchestrator.

## Self-Check

- [x] `./gradlew :app:compileJava` — BUILD SUCCESSFUL
- [x] All controllers have `@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")`
- [x] Export endpoint produces both `application/json` and `application/yaml`
- [x] Import endpoint consumes both content types
- [x] YAML ObjectMapper injected via `@Qualifier` (not `@Primary`)
- [x] No `@Autowired` field injection
- [x] `ClubProfileService` validates timezone via `ZoneId.of()` — invalid zones throw `DateTimeException` → 400

## Self-Check: PASSED
