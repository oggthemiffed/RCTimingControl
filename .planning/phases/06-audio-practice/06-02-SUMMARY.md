---
phase: 06-audio-practice
plan: "02"
subsystem: domain/infrastructure
tags: [practice, audio, piper-tts, flyway, jpa, docker]
dependency_graph:
  requires: ["06-01"]
  provides: ["practice-domain", "audio-settings", "profanity-blocklist", "piper-tts-infra"]
  affects: ["ClubProfile", "User", "docker-compose"]
tech_stack:
  added: ["Piper TTS (rhasspy/wyoming-piper)"]
  patterns: ["JSONB JSONB mapping via @JdbcTypeCode(SqlTypes.JSON)", "Java Record for settings DTO", "JPA state machine (start/stop guards)"]
key_files:
  created:
    - app/src/main/resources/db/migration/V23__phase6_practice_sessions.sql
    - app/src/main/java/dev/monkeypatch/rctiming/domain/practice/PracticeStatus.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/practice/PracticeSession.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/practice/PracticeSessionRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/practice/PracticeLap.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/practice/PracticeLapRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubAudioSettings.java
    - app/src/main/java/dev/monkeypatch/rctiming/infrastructure/profanity/ProfanityBlocklistEntry.java
    - app/src/main/java/dev/monkeypatch/rctiming/infrastructure/profanity/ProfanityBlocklistRepository.java
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/User.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfile.java
    - docker-compose.yml
    - app/src/main/resources/application.yml
decisions:
  - "Used Java Record for ClubAudioSettings to map JSONB column via @JdbcTypeCode(SqlTypes.JSON)"
  - "PracticeSessionRepository uses findByEvent_Id (Spring Data traversal) instead of plain findByEventId since event is @ManyToOne"
  - "findRunningSession uses fully-qualified JPQL enum literal to avoid JPQL string-to-enum ambiguity"
  - "Piper TTS port 10200 exposed on docker-compose for local dev; Spring connects to piper:10200 inside docker network"
metrics:
  duration: "~4 minutes"
  completed: "2026-04-28T20:04:56Z"
  tasks_completed: 3
  files_changed: 13
---

# Phase 6 Plan 02: Phase 6 Schema Foundation & Infrastructure Summary

**One-liner:** V23 Flyway migration + practice domain entities + audio settings JSONB + profanity blocklist + Piper TTS Docker service added to stack.

## What Was Built

This plan established the complete data layer and infrastructure scaffolding for Phase 6 (Audio & Practice). Three tasks were executed atomically:

### Task 1: V23 Flyway Migration
Created `V23__phase6_practice_sessions.sql` covering all Phase 6 schema changes in a single migration:
- `practice_sessions` table with nullable `event_id` FK (standalone or event-linked), state (`IDLE/RUNNING/STOPPED`), `best_lap_n`, timestamps
- `practice_laps` table with `transponder_number`, nullable `user_id` (linked after transponder recognition), `lap_time_ms`, `crossing_time`
- `users.preferred_voice_id` column (AUDIO-13)
- `club_profiles.audio_settings` JSONB column with 5 toggle defaults + `runningOrderDepth: 3` (AUDIO-07)
- `club_profiles.default_voice_id` column
- `profanity_blocklist` table with case-insensitive unique index on `word` (AUDIO-14)

### Task 2: Practice Domain Entities and Repositories
Created the `domain.practice` package with 5 files:
- `PracticeStatus` enum (`IDLE`, `RUNNING`, `STOPPED`)
- `PracticeSession` JPA entity with state machine methods (`start()` / `stop()`) with guard checks, nullable `@ManyToOne` event FK
- `PracticeSessionRepository` with `findRunningSession()` (JPQL) and `findByEvent_Id()` (Spring Data traversal)
- `PracticeLap` JPA entity with nullable `user` (linked later) and all timing fields
- `PracticeLapRepository` with session/transponder-scoped query methods

### Task 3: Entity Extensions, Profanity Infrastructure, Docker & Config
- Added `preferredVoiceId` field to `User` (AUDIO-13)
- Created `ClubAudioSettings` Java record for JSONB column mapping with `defaults()` factory
- Extended `ClubProfile` with `audioSettings` (mapped via `@JdbcTypeCode(SqlTypes.JSON)`) and `defaultVoiceId`
- Created `ProfanityBlocklistEntry` entity and `ProfanityBlocklistRepository` with `findAllWords()` and `findByWordIgnoreCase()`
- Added `piper` service (`rhasspy/wyoming-piper:latest`) to `docker-compose.yml` with `piper_data` volume on port 10200
- Added `tts:` config block to `application.yml` (endpoint, defaultVoice, enabled)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed PracticeSessionRepository.findByEventId derived query**
- **Found during:** Task 2
- **Issue:** Plan used `findByEventId(Long eventId)` but `PracticeSession.event` is a `@ManyToOne` object — Spring Data can't derive `eventId` from a nested association without underscore notation
- **Fix:** Changed to `findByEvent_Id(Long eventId)` which correctly traverses the `event` association to its `id` field
- **Files modified:** `PracticeSessionRepository.java`
- **Commit:** b29542f

**2. [Rule 1 - Bug] Fixed JPQL enum comparison in findRunningSession**
- **Found during:** Task 2
- **Issue:** JPQL string literal `'RUNNING'` in enum comparison may fail with strict JPQL validators; using fully-qualified enum literal is type-safe
- **Fix:** Changed to fully-qualified JPQL enum: `dev.monkeypatch.rctiming.domain.practice.PracticeStatus.RUNNING`
- **Files modified:** `PracticeSessionRepository.java`
- **Commit:** b29542f

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew :app:compileJava` | ✅ BUILD SUCCESSFUL |
| V23 migration file exists | ✅ |
| `docker compose config --services` includes piper | ✅ |
| `preferred_voice_id` in User.java | ✅ (3 occurrences: field, getter, setter) |
| `piper_data` volume in docker-compose | ✅ |
| `tts:` config in application.yml | ✅ |

## Known Stubs

None. This plan establishes schema and entity foundations only — no UI or service layer yet.

## Threat Flags

No new security-relevant surfaces beyond what is modelled in the plan's threat register.

## Self-Check: PASSED

All created files verified to exist:
- `V23__phase6_practice_sessions.sql` ✅
- `PracticeSession.java` ✅
- `PracticeStatus.java` ✅
- `PracticeSessionRepository.java` ✅
- `PracticeLap.java` ✅
- `PracticeLapRepository.java` ✅
- `ClubAudioSettings.java` ✅
- `ProfanityBlocklistEntry.java` ✅
- `ProfanityBlocklistRepository.java` ✅

All commits verified in git log:
- `8f98188` feat(06-02): create V23 migration ✅
- `b29542f` feat(06-02): create practice domain entities ✅
- `8487cb6` feat(06-02): extend User/ClubProfile, add profanity entity, Piper ✅
