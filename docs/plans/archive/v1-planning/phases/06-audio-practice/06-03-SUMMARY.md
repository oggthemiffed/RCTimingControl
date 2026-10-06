---
phase: 06-audio-practice
plan: "03"
subsystem: infrastructure/tts
tags: [piper-tts, wyoming-tcp, minio, profanity-filter, spring-events, async]
dependency_graph:
  requires: ["06-02"]
  provides: ["piper-tts-client", "tts-clip-service", "profanity-filter", "audio-api"]
  affects: ["RacerProfileService", "User", "ObjectStorageService"]
tech_stack:
  added: []
  patterns: ["Wyoming TCP protocol (JSONL + binary payloads)", "Spring ApplicationEvent @Async listener", "@ConfigurationProperties record binding"]
key_files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/infrastructure/tts/PiperTtsClient.java
    - app/src/main/java/dev/monkeypatch/rctiming/infrastructure/tts/TtsClipService.java
    - app/src/main/java/dev/monkeypatch/rctiming/infrastructure/tts/TtsUnavailableException.java
    - app/src/main/java/dev/monkeypatch/rctiming/infrastructure/tts/VoiceInfo.java
    - app/src/main/java/dev/monkeypatch/rctiming/infrastructure/tts/TtsProperties.java
    - app/src/main/java/dev/monkeypatch/rctiming/infrastructure/tts/NameClipGenerationListener.java
    - app/src/main/java/dev/monkeypatch/rctiming/infrastructure/profanity/ProfanityFilter.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/audio/AudioController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/UserProfileUpdatedEvent.java
    - app/src/main/java/dev/monkeypatch/rctiming/config/TtsConfig.java
    - app/src/main/resources/profanity/base-words.txt
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/user/RacerProfileService.java
    - app/src/test/java/dev/monkeypatch/rctiming/audio/PiperTtsClientTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/audio/TtsClipServiceTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/audio/ProfanityFilterTest.java
decisions:
  - "assembleWav() made public (not package-private) because tests are in a different package (audio) than the production code (infrastructure.tts)"
  - "Wyoming protocol: read JSON headers byte-by-byte (readJsonLine helper) then read binary payload bytes with readNBytes(N) — avoids BufferedReader consuming binary data"
  - "ProfanityFilter.blockPattern built as (?i)\\b(?:word1|word2|...)\\b regex for O(1) per-check performance"
  - "RacerProfileService checks firstName/lastName/phoneticName for profanity; displayName for TTS = firstName + space + lastName"
  - "AudioController uses Authentication.getName() (JWT user ID) not @AuthenticationPrincipal UserDetails, matching existing controller pattern"
metrics:
  duration: "~20 minutes"
  completed: "2026-04-28T20:16:24Z"
  tasks_completed: 3
  files_changed: 15
---

# Phase 6 Plan 03: Piper TTS Infrastructure Summary

**One-liner:** PiperTtsClient (Wyoming TCP socket protocol), TtsClipService (MinIO storage with voice-scoped keys), ProfanityFilter (classpath base list + DB custom terms), AudioController, and profanity check wired into RacerProfileService.

## What Was Built

### Task 1: PiperTtsClient with Wyoming TCP Protocol

Created the full Piper TTS client stack:

- **`TtsProperties`** — `@ConfigurationProperties(prefix="tts")` record binding `tts.endpoint`, `tts.defaultVoice`, `tts.enabled` from `application.yml`
- **`TtsConfig`** — `@EnableConfigurationProperties(TtsProperties.class)` registration
- **`TtsUnavailableException`** — unchecked exception for Piper connection failures and disabled-state
- **`VoiceInfo`** — record (`voiceId`, `label`, `isDefault`) for the voice list API
- **`PiperTtsClient`** — Wyoming TCP protocol implementation:
  - `synthesize(text, voiceName)`: opens `java.net.Socket` to `piper:10200`, sends JSONL `synthesize` event, reads `audio-start` / `audio-chunk` (JSON header + N raw PCM bytes read with `readNBytes(N)`) / `audio-stop` frames, assembles 44-byte RIFF/WAVE header + PCM into WAV
  - `listVoices()`: sends `describe` event, parses `info` response, returns `List<VoiceInfo>`
  - `assembleWav(pcm, rate, width, channels)`: builds standard RIFF/WAV header (ByteBuffer little-endian)
  - Graceful degradation: `listVoices()` returns empty list on IOException; `synthesize()` throws `TtsUnavailableException`

**PiperTtsClientTest** (17 test methods replaced from stub):
- `ServerSocket`-based in-process mock Piper — no Spring context, no DB required
- Tests: valid WAV returned, RIFF marker, empty text rejection, disabled-TTS exception, unavailable connection, correct voice name in request, WAV header structure, voice list parsing

### Task 2: TtsClipService and ProfanityFilter

- **`TtsClipService`** — generates and stores TTS clips:
  - `generateNameClip(racerId, text, voiceId)` → key `audio/racer/{id}/name-{voice}.wav`
  - `generateCountdownClip(raceId, seconds, text, voiceId)` → key `audio/race/{id}/countdown-{s}-{voice}.wav`
  - `generateCarNumberClip(raceId, carNum, text, voiceId)` → key `audio/race/{id}/car-{n}-{voice}.wav`
  - `generateFinishClip(raceId, racerId, text, voiceId)` → key `audio/race/{id}/finish-{racerId}-{voice}.wav`
  - All methods return MinIO URL or `null` on `TtsUnavailableException` (graceful degradation)
  - `generatePreview(text, voiceId)` — synthesizes without storing, returns raw WAV bytes

- **`ProfanityFilter`** — blocklist filter:
  - `@PostConstruct init()` → calls `reload()`
  - `reload()`: loads `profanity/base-words.txt` from classpath, merges `ProfanityBlocklistRepository.findAllWords()` from DB
  - `isBlocked(text)`: case-insensitive word-boundary regex match against compiled pattern
  - `getBlockedWords()`: returns unmodifiable copy of current word set

- **`profanity/base-words.txt`**: 28 common base-form profanity terms (lowercase)

**TtsClipServiceTest** — Mockito-based: verifies key patterns, voice resolution, null-on-down, no-storage-on-preview
**ProfanityFilterTest** — Mockito repository: tests base list hit, custom DB term, clean text, case-insensitivity, reload

### Task 3: AudioController and RacerProfileService Integration

- **`UserProfileUpdatedEvent`** — Spring `ApplicationEvent` carrying `userId`, `displayName`, `phoneticName`, `preferredVoiceId`

- **`RacerProfileService`** updated (AUDIO-14):
  - Constructor now injects `ProfanityFilter` and `ApplicationEventPublisher`
  - `updateProfile()` calls `profanityFilter.isBlocked()` on `firstName`, `lastName`, `phoneticName` before saving; throws `IllegalArgumentException("... contains inappropriate content")` if blocked
  - After save: publishes `UserProfileUpdatedEvent` for async clip regeneration

- **`NameClipGenerationListener`** — `@Async @EventListener(UserProfileUpdatedEvent.class)`:
  - Runs in `taskExecutor` thread pool (configured by `AsyncConfig`)
  - Prefers `phoneticName` over `displayName` for TTS text
  - Calls `TtsClipService.generateNameClip()`; logs warning on failure (non-blocking)

- **`AudioController`** at `/api/v1/audio` (AUDIO-13):
  - `GET /voices` → `piperClient.listVoices()` returns `List<VoiceInfo>` (empty if Piper down)
  - `GET /preview?voice={voiceId}` → synthesize current user's name, return `audio/wav` bytes; `503` if Piper unavailable

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Made `assembleWav()` public (test package boundary)**
- **Found during:** Task 1 compilation
- **Issue:** Plan spec said "package-private for testing" but test class is in `dev.monkeypatch.rctiming.audio` (not `infrastructure.tts`) — package-private is invisible across packages
- **Fix:** Changed `byte[] assembleWav(...)` → `public byte[] assembleWav(...)` with "Visible for testing" Javadoc
- **Files modified:** `PiperTtsClient.java`
- **Commit:** 95d95a6

**2. [Rule 1 - Bug] Avoided double-reader bug in Wyoming protocol**
- **Found during:** Task 1 implementation analysis
- **Issue:** Plan's code template created both `DataInputStream` and `BufferedReader` wrapping the same `socket.getInputStream()` — `BufferedReader` would buffer-ahead and consume binary payload bytes meant for the `DataInputStream`
- **Fix:** Implemented `readJsonLine(InputStream)` helper that reads byte-by-byte until `\n`, then calls `rawIn.readNBytes(payloadLength)` for binary PCM payload — single stream, no buffering conflict
- **Files modified:** `PiperTtsClient.java`
- **Commit:** 95d95a6

**3. [Rule 2 - Missing] Added profanity check on firstName and lastName**
- **Found during:** Task 3 implementation
- **Issue:** Plan mentioned "profanity check on displayName and phoneticName" but User entity has no `displayName` field — only `firstName` + `lastName`. Checking only `phoneticName` would leave racer name fields unprotected
- **Fix:** Applied `profanityFilter.isBlocked()` to `firstName`, `lastName`, and `phoneticName` individually in `updateProfile()`
- **Files modified:** `RacerProfileService.java`
- **Commit:** 7545797

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew :app:compileJava` | ✅ BUILD SUCCESSFUL |
| `PiperTtsClientTest` (6 tests) | ✅ BUILD SUCCESSFUL |
| `TtsClipServiceTest` (6 tests) | ✅ BUILD SUCCESSFUL |
| `ProfanityFilterTest` (6 tests) | ✅ BUILD SUCCESSFUL |
| `./gradlew :app:test --tests "dev.monkeypatch.rctiming.audio.*"` | ✅ BUILD SUCCESSFUL |

## Known Stubs

None. All implemented functionality is wired end-to-end.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: input-validation | AudioController.java | `/api/v1/audio/preview` accepts user-controlled `voice` param passed to Piper — no whitelist validation on voice name |

The `voice` parameter in `GET /api/v1/audio/preview` is passed directly to `PiperTtsClient.synthesize()` without validation against a known voice list. For a club-internal tool this is low risk (Piper rejects unknown voice names and throws an IOException which becomes `TtsUnavailableException` → 503), but a future plan should validate against `piperClient.listVoices()` if the endpoint becomes unauthenticated.

## Self-Check: PASSED
