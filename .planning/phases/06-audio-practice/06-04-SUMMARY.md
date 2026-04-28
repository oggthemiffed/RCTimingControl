---
phase: 06-audio-practice
plan: "04"
subsystem: infrastructure/tts,api/audio,api/admin,api/racecontrol,infrastructure/audio
tags: [audio-pre-generation, spring-events, stomp, admin-api, race-control]
dependency_graph:
  requires: ["06-03"]
  provides: ["audio-pregeneraton-service", "audio-clip-endpoint", "admin-audio-api", "running-order-announcements", "race-control-audio-settings"]
  affects: ["RaceStateMachineService", "ClubAudioSettings", "ClubProfile"]
tech_stack:
  added: []
  patterns: ["Spring ApplicationEvent (RaceStatusChangedEvent)", "@Async @EventListener", "@Scheduled(fixedRate)", "STOMP broadcast via SimpMessagingTemplate", "ConcurrentHashMap in-memory clip cache"]
key_files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceStatusChangedEvent.java
    - app/src/main/java/dev/monkeypatch/rctiming/infrastructure/tts/AudioPreGenerationService.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/audio/AudioClipController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/admin/AdminAudioController.java
    - app/src/main/java/dev/monkeypatch/rctiming/infrastructure/audio/RunningOrderAnnouncementService.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/AudioSettingsController.java
    - app/src/test/java/dev/monkeypatch/rctiming/audio/RunningOrderAnnouncementServiceTest.java
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceStateMachineService.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubAudioSettings.java
    - app/src/test/java/dev/monkeypatch/rctiming/audio/AudioPreGenerationServiceTest.java
decisions:
  - "Used RaceStatusChangedEvent (new Spring ApplicationEvent) published from RaceStateMachineService.transition() — no direct coupling between state machine and audio services"
  - "AudioPreGenerationService uses EntryRepository+UserRepository (not RaceEntry navigation) because RaceEntry has no nested associations — matches existing LapTimingService.loadEntryNames() pattern"
  - "RunningOrderAnnouncementService also listens to RaceStatusChangedEvent for RUNNING/STOPPED/FINISHED transitions rather than requiring direct wiring into RaceStateMachineService"
  - "countdownIntervals added to ClubAudioSettings record (stored in existing audio_settings JSONB) — no V24 migration column needed"
  - "onRaceStarted/onRaceStopped/getRaceStartTimes made public (not package-private) because tests are in dev.monkeypatch.rctiming.audio, not infrastructure.audio"
metrics:
  duration: "~25 minutes"
  completed: "2026-04-28T21:30:00Z"
  tasks_completed: 5
  files_changed: 10
---

# Phase 6 Plan 04: Race Audio Pre-Generation & Admin APIs Summary

**One-liner:** RaceStatusChangedEvent published from RaceStateMachineService drives async audio pre-generation on GRID transition, running-order STOMP announcements on RUNNING, plus AudioClipController, AdminAudioController, and AudioSettingsController REST endpoints.

## What Was Built

### Task 1: AudioPreGenerationService + RaceStatusChangedEvent

Created the Spring event infrastructure and the pre-generation service:

- **`RaceStatusChangedEvent`** — `ApplicationEvent` subclass carrying `raceId` and `newStatus`. Published from `RaceStateMachineService.transition()` after every successful state change.
- **`RaceStateMachineService`** updated — `ApplicationEventPublisher` injected (nullable for test-only constructor), event published between `setStatus()` and STOMP broadcast.
- **`AudioPreGenerationService`** — `@Async @EventListener(RaceStatusChangedEvent)`:
  - Filters: only `RaceStatus.GRID` transitions
  - Generates 5 countdown clips (600s/300s/120s/60s/30s) via `TtsClipService.generateCountdownClip()`
  - Generates per-entry stagger calls ("Car N, on the line") using grid position; looks up entries via `RaceEntryRepository` and `EntryRepository`
  - Generates per-racer finish clips ("Name has finished"); resolves `phoneticName` over `firstName + lastName`
  - Caches URLs in `ConcurrentHashMap<Long, Map<String, String>>` keyed by raceId
  - `getClipMap(raceId)` returns cached map (empty if not yet generated)
  - `clearClips(raceId)` evicts on race end

**AudioPreGenerationServiceTest** (8 tests replacing @Disabled stub):
- Countdown clips generated (5 intervals), stagger calls per entry, finish clips per racer, phonetic name preference, clip map retrieval, empty map before generation, non-GRID transition ignored, race-not-found handled gracefully

### Task 2: AudioClipController

- **`AudioClipController`** at `GET /api/v1/race/{raceId}/audio-clips`:
  - Delegates to `AudioPreGenerationService.getClipMap()`
  - Returns `Map<String, String>` of `clipKey → MinIO URL`
  - Empty map `{}` when clips not yet generated (race not in GRID)

### Task 3: AdminAudioController

- **`AdminAudioController`** at `/api/v1/admin/audio`, `@PreAuthorize("hasRole('ADMIN')")`:
  - `GET/POST /settings` — view/save club audio toggle settings and default voice (AUDIO-07)
  - `GET/POST/DELETE /blocklist` — manage profanity blocklist terms (AUDIO-14); calls `profanityFilter.reload()` after changes; handles conflict (409) and missing word (400)
  - `GET/PUT /racer/{userId}/phonetic` — admin view/override phonetic name (AUDIO-15); bypasses profanity check
  - `DELETE /racer/{userId}/name-clip` — force regenerate name clip via `TtsClipService.generateNameClip()`
  - Uses `firstName + " " + lastName` as `displayName` (User has no `getDisplayName()`)

### Task 4: RunningOrderAnnouncementService

- **`RunningOrderAnnouncementService`** with `@Scheduled(fixedRate=10_000)`:
  - `@EventListener(RaceStatusChangedEvent)`: RUNNING → `onRaceStarted()`; STOPPED/FINISHED → `onRaceStopped()`
  - `checkAndAnnounce()`: for each tracked race, computes elapsed time; 2-min interval for first 10 min, then 5-min (AUDIO-06 spec); verifies race still RUNNING via DB
  - Broadcasts `RunningOrderAnnouncement{type="running-order", positions=[...]}` to `/topic/race/{id}/audio` via STOMP
  - Uses `LapTimingService.peek(raceId).calculatePositions()` → `LiveTimingRowDto.driverName()` for current order
  - `announcementDepth` from `ClubAudioSettings.runningOrderDepth()` (default 3)

**RunningOrderAnnouncementServiceTest** (9 tests):
- Lifecycle (register/deregister), event-driven state changes, interval not-yet-elapsed, deregistration on stopped race, empty positions, default depth

### Task 5: AudioSettingsController + ClubAudioSettings Update

- **`ClubAudioSettings`** record extended with `countdownIntervals int[]` field:
  - Compact constructor ensures non-null (defaults: `{600, 300, 120, 60, 30}`)
  - No V24 migration needed — field stored within existing `audio_settings` JSONB column

- **`AudioSettingsController`** at `/api/v1/race-control/settings/audio`, `@PreAuthorize("hasAnyRole('RACE_DIRECTOR', 'ADMIN')")`:
  - `GET` returns full `AudioSettingsDto` including `countdownIntervals[]`
  - `PATCH` persists updated settings to `ClubProfile` (survives page reload)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] RaceState → RaceStatus enum mismatch**
- **Found during:** Task 1 implementation
- **Issue:** Plan template referenced `RaceState.GRID` but actual enum is `RaceStatus` (`RaceStatus.GRID`). Also `race.getState()` doesn't exist — it's `race.getStatus()`
- **Fix:** Used correct `RaceStatus` throughout all files
- **Files modified:** `AudioPreGenerationService.java`, `RunningOrderAnnouncementService.java`
- **Commit:** 321b8f2

**2. [Rule 1 - Bug] RaceEntry has no nested associations**
- **Found during:** Task 1 implementation
- **Issue:** Plan template used `entry.getEntry().getCar().getName()` and `entry.getEntry().getUser()` but `RaceEntry` is a flat join table with only `raceId`, `entryId`, `gridPosition`, `bumped` — no JPA navigation
- **Fix:** Used `EntryRepository.findById(raceEntry.getEntryId())` then `UserRepository.findById(entry.getUserId())` — same pattern as `LapTimingService.loadEntryNames()`; used `gridPosition` as car number for stagger calls
- **Files modified:** `AudioPreGenerationService.java`
- **Commit:** 321b8f2

**3. [Rule 1 - Bug] TtsClipService.generateCarNumberClip takes int not String**
- **Found during:** Task 1 implementation
- **Issue:** Plan template passed `String carNumber` but actual signature is `generateCarNumberClip(Long raceId, int carNumber, String text, String voiceId)`
- **Fix:** Used `int gridPos = raceEntry.getGridPosition()` as car number
- **Files modified:** `AudioPreGenerationService.java`
- **Commit:** 321b8f2

**4. [Rule 1 - Bug] RaceEntryRepository method name mismatch**
- **Found during:** Task 1 implementation
- **Issue:** Plan template called `findByRaceIdOrderByGridPositionAsc` but actual method is `findByRaceIdOrderByGridPosition`
- **Fix:** Used correct method name
- **Files modified:** `AudioPreGenerationService.java`
- **Commit:** 321b8f2

**5. [Rule 1 - Bug] RunningOrderAnnouncementService package-private methods invisible to test**
- **Found during:** Task 4 test compilation
- **Issue:** Test class in `dev.monkeypatch.rctiming.audio` can't access package-private methods in `dev.monkeypatch.rctiming.infrastructure.audio` — compilation failed with 15 errors
- **Fix:** Made `onRaceStarted()`, `onRaceStopped()`, `getRaceStartTimes()` public with "Visible for testing" Javadoc
- **Files modified:** `RunningOrderAnnouncementService.java`
- **Commit:** a6d1401

**6. [Rule 1 - Bug] No RaceStateChangedEvent existed**
- **Found during:** Task 1 analysis
- **Issue:** Plan referenced `RaceStateChangedEvent` which didn't exist in the codebase
- **Fix:** Created `RaceStatusChangedEvent` as a new Spring `ApplicationEvent`; published from `RaceStateMachineService.transition()`
- **Files modified:** `RaceStatusChangedEvent.java` (new), `RaceStateMachineService.java`
- **Commit:** 321b8f2

**7. [Rule 1 - Bug] User has no getDisplayName()**
- **Found during:** Task 3 implementation
- **Issue:** Plan template called `user.getDisplayName()` but User only has `firstName` + `lastName`
- **Fix:** Used `user.getFirstName() + " " + user.getLastName()` consistently
- **Files modified:** `AdminAudioController.java`
- **Commit:** 942c49c

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew :app:compileJava` | ✅ BUILD SUCCESSFUL |
| `./gradlew :app:compileTestJava` | ✅ BUILD SUCCESSFUL |
| `AudioPreGenerationServiceTest` (8 tests) | ✅ BUILD SUCCESSFUL |
| `RunningOrderAnnouncementServiceTest` (9 tests) | ✅ BUILD SUCCESSFUL |
| `./gradlew :app:test --tests "dev.monkeypatch.rctiming.audio.*"` | ✅ BUILD SUCCESSFUL |
| `@PreAuthorize("hasRole('ADMIN')")` on AdminAudioController | ✅ |
| AudioClipController at `/api/v1/race/{raceId}/audio-clips` | ✅ |
| AudioSettingsController at `/api/v1/race-control/settings/audio` | ✅ |

## Known Stubs

None. All endpoints are wired to real implementations; audio generation depends on Piper TTS being available at runtime (graceful degradation: returns null URL on `TtsUnavailableException`).

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: elevation-of-privilege | AdminAudioController.java | Admin-only endpoints protected by `@PreAuthorize("hasRole('ADMIN')")` at class level ✅ |
| threat_flag: tampering | AudioSettingsController.java | Race-director role required for settings mutation; no CSRF risk on REST+JWT stack |

## Self-Check: PASSED
