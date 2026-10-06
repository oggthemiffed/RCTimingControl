---
phase: 06-audio-practice
plan: "05"
subsystem: domain/practice,api/racecontrol,infrastructure/timing
tags: [practice, live-timing, stomp, rest-api, best-consecutive-laps, sliding-window]
dependency_graph:
  requires: ["06-02"]
  provides: ["practice-timing-service", "practice-session-controller", "live-practice-state", "practice-timing-hub"]
  affects: ["PracticeSession", "PracticeLap", "LapPassingEvent"]
tech_stack:
  added: []
  patterns: ["Spring @EventListener(LapPassingEvent)", "ConcurrentHashMap in-memory state", "O(n) sliding window algorithm", "STOMP broadcast via SimpMessagingTemplate", "@PreAuthorize per-endpoint security", "Testcontainers integration test (AbstractIntegrationTest)"]
key_files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/practice/LivePracticeState.java
    - app/src/main/java/dev/monkeypatch/rctiming/practice/PracticeTimingHub.java
    - app/src/main/java/dev/monkeypatch/rctiming/practice/PracticeTimingService.java
    - app/src/main/java/dev/monkeypatch/rctiming/practice/PracticeSessionService.java
    - app/src/main/java/dev/monkeypatch/rctiming/practice/dto/PracticeTimingRowDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/practice/dto/PracticeSessionDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/PracticeSessionController.java
  modified:
    - app/src/test/java/dev/monkeypatch/rctiming/practice/BestConsecutiveLapsTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/practice/PracticeTimingServiceTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/practice/PracticeSessionControllerIT.java
decisions:
  - "LapPassingEvent is a Java record with raceId/transponderNumber/rtcTimeMicros — no getLapTimeMs() or getCrossingTime(). Lap time computed from rtcTimeMicros delta (/1000 → ms); crossingTime uses Instant.now()"
  - "Transponder entity has getUserId() not getUser() — resolved via UserRepository.findById()"
  - "User has getFirstName()+getLastName() not getDisplayName() — concatenated as racerName"
  - "PracticeTimingService@EventListener has no @Async to keep @Transactional compatible with lap persistence"
  - "AbstractIntegrationTest has no helper methods — createRaceDirectorUser() defined inline in IT class, matching TrackControllerIT pattern"
metrics:
  duration: "~6 minutes"
  completed: "2026-04-28T20:44:00Z"
  tasks_completed: 3
  files_changed: 10
---

# Phase 6 Plan 05: Practice Session Timing Backend Summary

**One-liner:** Practice session live timing backed by LapPassingEvent listener computing lap times from rtcTimeMicros deltas, with sliding-window best-N-consecutive-laps, STOMP broadcasts to /topic/practice/{id}/timing, and REST CRUD at /api/v1/practice-sessions.

## What Was Built

### Task 1: LivePracticeState + PracticeTimingRowDto + BestConsecutiveLapsTest

Created the core in-memory state and algorithm:

- **`LivePracticeState`** — Thread-safe (ConcurrentHashMap) per-session state holder:
  - `recordLap(transponderNumber, userId, racerName, lapTimeMs, crossingTime)` — adds crossing; only stores lapTimeMs when non-null and >0 (first crossing has no lap time)
  - `calculatePositions()` — returns sorted `List<PracticeTimingRowDto>` (laps desc, best lap asc)
  - `getBestConsecutiveN(n)` in `ParticipantState` — O(n) sliding window: initialise first window, slide right subtracting oldest + adding newest; returns null if fewer than N laps
  - `linkTransponder(transponderNumber, userId, racerName)` — retroactive user linking
  - `getUnknownTransponders()` — Set of unlinked transponder numbers

- **`PracticeTimingRowDto`** record — 9 fields: `position, transponderNumber, userId, racerName, laps, bestLapMs, bestConsecutiveNMs, lastLapMs, isUnknown`

- **`BestConsecutiveLapsTest`** — 4 pure unit tests (no Spring), replacing @Disabled stub:
  - `fewerThanNLaps_returnsNull` — 2 laps with N=3 → null
  - `exactlyNLaps_returnsSumOfAll` — 3 laps: 20+21+22=63000ms
  - `moreThanNLaps_returnsMinWindow` — 4 laps: best 3-window = 21+20+19=60000ms
  - `bestWindowAtEnd_findsIt` — 6 laps: best window at end 18+17+16=51000ms

### Task 2: PracticeTimingService + PracticeTimingHub + PracticeTimingServiceTest

Created the event-driven timing engine:

- **`PracticeTimingHub`** — STOMP broadcaster:
  - `broadcastTimingUpdate(sessionId, rows)` → `/topic/practice/{sessionId}/timing`
  - `broadcastUnknownTransponders(sessionId, transponders)` → `/topic/practice/{sessionId}/unknown-transponder`

- **`PracticeTimingService`** — core timing service:
  - `@EventListener(LapPassingEvent.class)` — fires on every transponder passing
  - Checks `sessionRepository.findRunningSession()` — no-op if no running practice session
  - Computes lap time: `(current_rtcMicros - prev_rtcMicros) / 1000L` ms; first crossing records null lapTimeMs (no lap credited)
  - Resolves transponder via `TransponderRepository.findByTransponderNumber()` → `UserRepository.findById(userId)`
  - Persists `PracticeLap` on each completed lap (lap number = existing laps + 1)
  - Broadcasts positions + unknown transponders after each passing
  - `startSession()` / `stopSession()` lifecycle — clears per-transponder RTC tracking on stop
  - `buildSnapshotFromDb()` — reconstructs `LivePracticeState` from persisted `PracticeLap` rows for stopped sessions

- **`PracticeTimingServiceTest`** — 5 Mockito unit tests (no Spring):
  - Running session records lap (2nd passing → lap saved)
  - No running session → ignored
  - Unknown transponder → lap saved with null user
  - `getSnapshot()` returns current positions
  - STOMP hub called once per passing

### Task 3: PracticeSessionService + PracticeSessionController + PracticeSessionControllerIT

Created CRUD and REST layer:

- **`PracticeSessionDto`** record — 8 fields: `id, name, eventId, eventName, status, bestLapN, startedAt, stoppedAt`

- **`PracticeSessionService`**:
  - `create(CreateRequest, createdByEmail)` — creates IDLE session; optional eventId and bestLapN
  - `start(id)` — IDLE→RUNNING guard, saves, delegates to `PracticeTimingService.startSession()`
  - `stop(id)` — RUNNING→STOPPED guard, saves, delegates to `PracticeTimingService.stopSession()`
  - `findById()`, `findRecent(limit)` — read queries

- **`PracticeSessionController`** at `/api/v1/practice-sessions`:
  - `POST /` (ADMIN|RACE_DIRECTOR) → 201 Created
  - `GET /{id}`, `GET /` (authenticated)
  - `POST /{id}/start`, `POST /{id}/stop` (ADMIN|RACE_DIRECTOR) → 200 OK / 409 Conflict on invalid state
  - `GET /{id}/snapshot`, `GET /{id}/results` (authenticated)
  - `POST /{id}/link-transponder` (ADMIN|RACE_DIRECTOR)
  - `@PreAuthorize` on each endpoint (STRIDE T-06-13 mitigation)

- **`PracticeSessionControllerIT`** — 6 integration tests via Testcontainers+Postgres:
  - `createSession_validRequest_returns201`
  - `createSession_withEventLink_associatesEvent` (verifies bestLapN customisation)
  - `startSession_idleSession_transitionsToRunning`
  - `stopSession_runningSession_transitionsToStopped`
  - `startSession_alreadyRunning_returns409`
  - `getResults_stoppedSession_returnsFinalSnapshot`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] LapPassingEvent is a Java record with different accessor API**
- **Found during:** Task 2 implementation
- **Issue:** Plan template called `event.getTransponderNumber()`, `event.getLapTimeMs()`, `event.getCrossingTime()` — but `LapPassingEvent` is a Java record with fields `raceId()`, `transponderNumber()`, `rtcTimeMicros()`. No `getLapTimeMs()` exists.
- **Fix:** Used record accessor `event.transponderNumber()`; computed lap time from `rtcTimeMicros` delta; used `Instant.now()` for crossing time
- **Files modified:** `PracticeTimingService.java`
- **Commit:** 0f19a8e

**2. [Rule 1 - Bug] Transponder has no getUser() navigation**
- **Found during:** Task 2 implementation
- **Issue:** Plan template called `transponder.getUser()` but `Transponder` only has `getUserId()` (flat entity, no JPA navigation)
- **Fix:** Used `userRepository.findById(transponder.getUserId())` — same pattern as `LapTimingService.loadEntryNames()`
- **Files modified:** `PracticeTimingService.java`
- **Commit:** 0f19a8e

**3. [Rule 1 - Bug] User has no getDisplayName()**
- **Found during:** Task 2 implementation
- **Issue:** Plan template called `user.getDisplayName()` — User only has `getFirstName()` + `getLastName()`
- **Fix:** Used `user.getFirstName() + " " + user.getLastName()` consistently
- **Files modified:** `PracticeTimingService.java`
- **Commit:** 0f19a8e

**4. [Rule 2 - Missing] PracticeTimingService no @Async to keep @Transactional**
- **Found during:** Task 2 design review
- **Issue:** LapTimingService uses `@Async @EventListener` but adding `@Async` would break `@Transactional` within `onLapPassing` (separate thread loses transaction context)
- **Fix:** Omitted `@Async` — practice session processing is lightweight and single per-event; transaction maintained for lap persistence
- **Files modified:** `PracticeTimingService.java`
- **Commit:** 0f19a8e

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew :app:compileJava` | ✅ BUILD SUCCESSFUL |
| `./gradlew :app:compileTestJava` | ✅ BUILD SUCCESSFUL |
| `BestConsecutiveLapsTest` (4 tests) | ✅ BUILD SUCCESSFUL |
| `PracticeTimingServiceTest` (5 tests) | ✅ BUILD SUCCESSFUL |
| `PracticeSessionControllerIT` (6 tests) | ✅ BUILD SUCCESSFUL |
| `./gradlew :app:test --tests "dev.monkeypatch.rctiming.practice.*"` | ✅ BUILD SUCCESSFUL |
| `@PreAuthorize` on all mutating endpoints | ✅ |
| STOMP topic `/topic/practice/{id}/timing` | ✅ |

## Known Stubs

None. All endpoints wired to real in-memory state and DB persistence. STOMP broadcasts fire on every lap passing. Practice results endpoint reads from in-memory state (active) or rebuilt from DB (stopped sessions).

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: elevation-of-privilege | PracticeSessionController.java | `@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")` on create/start/stop/link-transponder endpoints ✅ |
| threat_flag: tampering | PracticeSession.java | State machine guards in `start()`/`stop()` enforce valid transitions; controller returns 409 on violation ✅ |

## Self-Check: PASSED
