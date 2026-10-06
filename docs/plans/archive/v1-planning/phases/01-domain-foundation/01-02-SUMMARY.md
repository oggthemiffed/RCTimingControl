---
plan: 01-02
phase: 01-domain-foundation
status: complete
started: 2026-04-16
completed: 2026-04-16
tasks_total: 2
tasks_completed: 2
deviation_count: 0
self_check: PASSED
---

## Summary

All JPA entities, enum types, and Spring Data repositories for the Phase 1 domain.
21 files created across user, auth, club, track, and raceclass packages.
`./gradlew :app:compileJava` BUILD SUCCESSFUL. All entities map to V1–V4 Flyway schema.

## What Was Built

**User / Auth domain:**
- `User` — `@Entity` with `@ElementCollection` roles via `user_roles` join table, Instant timestamps
- `Role` — enum: RACER, ADMIN, RACE_DIRECTOR, REFEREE
- `UserRepository` — `findByEmail`, `existsByEmail`
- `UserService` — `createRacer()`, `findByEmail()` with constructor-injected dependencies
- `RefreshToken` — `tokenHash` unique, `revoked` field, `@ManyToOne User`
- `RefreshTokenRepository` — `findByTokenHash`, `deleteByUser`, `findByUserAndRevokedFalse`
- `PasswordResetToken` — `tokenHash` unique, `used` field, `expiresAt` Instant
- `PasswordResetTokenRepository` — `findByTokenHash`

**Club domain:**
- `ClubProfile` — singleton entity: GPS lat/lng (Double), timezone (String), logo `@Lob`, logoType
- `ClubProfileRepository` — standard CRUD
- `GoverningBodyAffiliation` — `code` unique, `membershipRequired` boolean
- `GoverningBodyAffiliationRepository` — `findByCode`, `existsByCode`

**Track domain:**
- `Track` — name, surfaceType, venueNotes, trackLength; `@OneToMany` DecoderLoop + TrackLapThreshold
- `TrackRepository`
- `LoopType` — enum: FINISH_LINE, CHICANE, OTHER
- `DecoderLoop` — loopNumber, `@Enumerated(EnumType.STRING)` loopType, `@ManyToOne Track`
- `DecoderLoopRepository` — `findByTrackId`
- `TrackLapThreshold` — minLapMs, maxLastLapMs, `@ManyToOne` track + optional racingClass; unique(track_id, racing_class_id)
- `TrackLapThresholdRepository` — `findByTrackId`, `findByTrackIdAndRacingClassId`, `findByTrackIdAndRacingClassIsNull`

**RacingClass domain:**
- `RacingClass` — name (unique), description
- `RacingClassRepository`

## Key Files Created

```
key-files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/User.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/Role.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/UserService.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/auth/RefreshToken.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/auth/RefreshTokenRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/auth/PasswordResetToken.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/auth/PasswordResetTokenRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfile.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/club/GoverningBodyAffiliation.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/track/Track.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/track/DecoderLoop.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/track/TrackLapThreshold.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/raceclass/RacingClass.java
```

## Verification

- `./gradlew :app:compileJava`: BUILD SUCCESSFUL
- All entities use `jakarta.persistence.*` (not javax) — Spring Boot 3.4.x compatible
- No `@Autowired` field injection — constructor injection throughout
- `ddl-auto: validate` will match Flyway V1–V4 schema at startup

## Deviations

None. All acceptance criteria met.

## Requirements Coverage

- AUTH-01, AUTH-02, AUTH-03, AUTH-05: User, Role, RefreshToken, PasswordResetToken entities
- CLUB-01, CLUB-02: ClubProfile + GoverningBodyAffiliation entities
- TRACK-01, TRACK-02, TRACK-03, TRACK-04: Track, DecoderLoop, TrackLapThreshold entities
- RACECLASS-01: RacingClass entity

## Self-Check: PASSED
