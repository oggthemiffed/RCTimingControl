---
phase: 6
slug: audio-practice
type: research
status: complete
researched: 2026-04-29
---

# Phase 6 Research: Audio & Practice

> Answers the question: **"What do I need to know to PLAN this phase well?"**

---

## 1. Piper TTS: Protocol Correction ⚠️

**CONTEXT.md assumption**: "Spring calls it via HTTP to generate .wav clips via `RestTemplate`/`WebClient`"

**Reality**: `rhasspy/wyoming-piper` uses the **Wyoming TCP protocol** (port 10200, JSONL + binary PCM frames), NOT HTTP. The HTTP server at port 5000 exists as an optional Python extra (`wyoming[http]`) but **is NOT included in the standard Docker image** — the Dockerfile only installs `.[zeroconf,zh]`.

### Wyoming Protocol Exchange (what Spring must implement)

A `PiperTtsClient` opens a TCP socket and follows this exchange:

```
→  {"type": "synthesize", "data": {"text": "Alan Smith", "voice": {"name": "en_GB-alan-medium"}}}\n
←  {"type": "audio-start", "data": {"rate": 22050, "width": 2, "channels": 1}}\n
←  {"type": "audio-chunk", "data": {...}, "payload_length": N}\n  [N bytes raw PCM]
←  {"type": "audio-chunk", ...}\n  [repeated until end]
←  {"type": "audio-stop"}\n
```

**Implementation approach in Java**:
1. Open `Socket` to `piper-host:10200` (synchronous call — total time for a name clip ~100–500ms)
2. Write the `synthesize` JSON line + `\n`
3. Read header lines (each is UTF-8 JSON, terminated by `\n`); after each header if `payload_length > 0`, read exactly that many bytes and append to PCM buffer
4. Stop at `audio-stop` event; close socket
5. Build WAV file: 44-byte WAV header (RIFF, fmt chunk with rate/width/channels from `audio-start`, data chunk) + PCM bytes
6. Return `byte[]` for storage in MinIO

**No new dependencies required** — `java.net.Socket` is sufficient. Netty is already on the classpath (grpc-netty-shaded from Phase 5) but plain blocking sockets are simpler for this synchronous case.

**Voice names in the `synthesize` event** must match the Piper model directory names exactly (e.g. `en_GB-alan-medium`).

### Docker-Compose Integration

Add to `docker-compose.yml`:
```yaml
  piper:
    image: rhasspy/wyoming-piper
    container_name: rctiming-piper
    volumes:
      - piper_data:/data
    command: --voice en_GB-alan-medium
    # Wyoming TCP only — port 10200 is not exposed to host, only accessible on Docker network
    # (Spring connects to piper:10200 on the compose network)
```
Add `piper_data:` to the `volumes:` block.

Spring config (add to `application.yml`):
```yaml
tts:
  endpoint: ${TTS_ENDPOINT:piper:10200}
  defaultVoice: ${TTS_DEFAULT_VOICE:en_GB-alan-medium}
  enabled: ${TTS_ENABLED:true}
```

No external port exposure is needed — the Spring Boot app and Piper are on the same Docker network.

---

## 2. en_GB Voice Models Available

11 models available. Sizes are ~63–115 MB per model (downloaded into the volume on first startup).

| Voice ID | Quality | Notes |
|----------|---------|-------|
| `en_GB-alan-medium` | medium | ✅ **Recommended default** — male, clear, natural |
| `en_GB-alan-low` | low | 16kHz, smaller file |
| `en_GB-cori-high` | high | Female, 114MB — best quality but slowest |
| `en_GB-cori-medium` | medium | Female |
| `en_GB-northern_english_male-medium` | medium | Regional accent |
| `en_GB-jenny_dioco-medium` | medium | Female |
| `en_GB-aru-medium` | medium | |
| `en_GB-vctk-medium` | medium | Multi-speaker |
| `en_GB-alba-medium` | medium | |
| `en_GB-semaine-medium` | medium | |
| `en_GB-southern_english_female-low` | low | |

**Multiple voices in Docker**: to support voice selection (AUDIO-13), each voice model must be downloaded into the data volume. Piper downloads models on first request when `--update-voices` or the voice is not present. The container downloads models from HuggingFace on first use; subsequent calls are served from the volume.

**Piper describe/list voices**: Spring can call the Wyoming `describe` event (`{"type": "describe"}\n`) on startup to enumerate installed voices and populate the voice selector UI.

---

## 3. Web Speech API Browser Support Matrix

- **Chrome 33+**: Full support. **Linux caveat**: `speechSynthesis.speak()` may be silently ignored until after a user gesture (first click). Add a `document.addEventListener('click', ..., {once: true})` primer on page load. Chrome on Linux also depends on `speech-dispatcher` / `espeak-ng` being installed — if missing, the voices list is empty. The CONTEXT.md note about a "Test audio" button in race control settings is the correct mitigation.
- **Firefox 49+**: Full support, uses OS TTS engine (festival, espeak on Linux)
- **Safari 7+ / iOS**: Full support
- **Edge 14+**: Full support (uses Windows TTS)

**API surface needed** (fallback hook):
```typescript
function speakFallback(text: string): void {
  if (!window.speechSynthesis) return;
  const u = new SpeechSynthesisUtterance(text);
  window.speechSynthesis.speak(u);
}
```

**Key gotcha**: calling `speechSynthesis.getVoices()` immediately on page load returns empty on Chrome (voices load async). Must listen to `voiceschanged` event. For the "preview" path this is less important since Piper clips are used instead.

---

## 4. Flyway Migration Numbers

Latest migration: `V22__create_unknown_transponder_link.sql`

Phase 6 migrations start at **V23**. Expected sequence:
- `V23__phase6_practice_sessions.sql` — `practice_sessions` table
- `V24__phase6_club_audio_settings.sql` — audio config columns on `club_profiles` (or separate table)
- `V25__phase6_profanity_blocklist.sql` — `profanity_blocklist` table (club-extensible)

---

## 5. MinIO / ObjectStorageService Pattern

Current `ObjectStorageService` interface has one method:
```java
String upload(String key, byte[] content, String contentType);
// Returns public URL: publicBaseUrl + "/" + key
```

Used in `LogoUploadService` as the exact pattern to follow. No `download` method exists — the plan should **not** add one to the interface; serving clips to the browser is done via MinIO public URLs stored in the DB or returned by the API.

**Clip key conventions** (from CONTEXT.md, confirmed as the right approach):
```
audio/racer/{racerId}/name-{voiceId}.wav     ← name clip per racer, per voice
audio/race/{raceId}/countdown-{seconds}-{voiceId}.wav
audio/race/{raceId}/car-{carNumber}-{voiceId}.wav
audio/race/{raceId}/finish-{racerId}-{voiceId}.wav
```

**`contentType`**: `audio/wav`

**Clip URL returned from `upload()`** is a public MinIO URL (configured via `storage.publicBaseUrl`). This URL is what the browser fetches to play the clip (`<audio src="…">` or `new Audio(url).play()`).

**Clip invalidation on voice change**: delete old key (need to add `delete(key)` to `ObjectStorageService`) and re-upload with new voice. OR: the voice-scoped key convention means old clip for old voice stays in MinIO (orphaned) and the new voice generates a new key. **Decision required from planner**: either add `delete()` or accept orphaned clips. For v1, orphaned clips approach is simpler.

**S3 `delete` method**: `s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())` — add to interface only if chosen.

---

## 6. PracticeSession Entity and DB Design

### Schema (V23)

```sql
CREATE TABLE practice_sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    event_id BIGINT REFERENCES events(id),          -- nullable: standalone or event-linked
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE',      -- IDLE, RUNNING, STOPPED
    best_lap_n INT NOT NULL DEFAULT 3,               -- configurable consecutive-lap count
    created_by_user_id BIGINT REFERENCES users(id),
    started_at TIMESTAMPTZ,
    stopped_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_practice_sessions_event ON practice_sessions(event_id)
    WHERE event_id IS NOT NULL;
```

### Java Entity

`PracticeSession` entity in `dev.monkeypatch.rctiming.domain.practice` package:
- `PracticeStatus` enum: `IDLE, RUNNING, STOPPED`
- State machine: `IDLE → RUNNING → STOPPED`. Restart (STOPPED → RUNNING) is optional for v1.

### Relationship to Race/Round/Event

- `PracticeSession` is **independent** — not a `Round` or `Race` entity
- Does **not** use `race_entries` table
- Transponder → User resolution: look up `transponders` table (existing from Phase 2) by `transponderNumber` to find `userId`
- If transponder unknown: surface in UI via same `UnknownTransponderLinkDialog` pattern

### Practice Participant Tracking

Unlike races (which use `Entry` entities), practice tracks participants as `userId` resolved from transponder. If transponder has no known owner, the participant is shown as "Unknown #{transponderNumber}" until linked.

No new DB table for "practice participants" — all participation data lives in `PracticeTimingService` in-memory state, like races. Final print snapshot can be serialised to a JSON blob on `STOPPED`.

---

## 7. Best Consecutive N Laps Algorithm

**Data structure per participant**: ordered list of lap times (each element is `lapMs: Long`).

**Algorithm**: sliding window minimum sum over N consecutive elements.

```java
Long bestConsecutiveN(List<Long> lapTimes, int n) {
    if (lapTimes.size() < n) return null;
    long windowSum = 0;
    for (int i = 0; i < n; i++) windowSum += lapTimes.get(i);
    long best = windowSum;
    for (int i = n; i < lapTimes.size(); i++) {
        windowSum += lapTimes.get(i) - lapTimes.get(i - n);
        if (windowSum < best) best = windowSum;
    }
    return best;
}
```

- O(L) per call where L = number of laps. At club scale (≤100 laps, ≤40 participants), trivial.
- Called on every new lap passing during `calculatePositions()` in `LivePracticeState`.
- Store result as `bestConsecutiveNLapMs: Long` in the `PracticeTimingRowDto`.

---

## 8. Profanity Checking in Java

**Decision from CONTEXT.md**: "Java word-list + regex matcher. Plain word-list is sufficient."

**Implementation**: `ProfanityFilter` class in `dev.monkeypatch.rctiming.infrastructure.profanity`:
- Ships with a bundled base word list (resource file `profanity/base-words.txt`)
- Admin-extensible: `profanity_blocklist` table with `(id, word, addedByUserId, addedAt)` rows
- On startup, loads from resource + DB into `Set<String> blockedWords`
- `@RefreshScope` or admin endpoint triggers re-load after DB changes
- Check: for each word in blocklist, `Pattern.compile("\\b" + Pattern.quote(word) + "\\b", CASE_INSENSITIVE)` match against input

**Base word list source**: include a curated ~300-word list embedded in the JAR (not a dependency). There are public domain lists e.g. from Google's profanity filter.

**No new Java library dependency needed** — the regex approach is sufficient for a club tool.

**Schema** (V25):
```sql
CREATE TABLE profanity_blocklist (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    word VARCHAR(200) NOT NULL UNIQUE,
    added_by_user_id BIGINT REFERENCES users(id),
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 9. Club Config Additions

Phase 6 needs several new settings on `ClubProfile` (or a new `ClubAudioSettings` entity). **Recommendation**: add JSONB `audio_settings` column to `club_profiles` to avoid a new table.

```sql
-- V24
ALTER TABLE club_profiles
    ADD COLUMN audio_settings JSONB NOT NULL DEFAULT '{}',
    ADD COLUMN default_voice_id VARCHAR(100) NOT NULL DEFAULT 'en_GB-alan-medium';
```

`audio_settings` JSONB structure:
```json
{
  "announceCountdown": true,
  "announceStagger": true,
  "announceLapBeep": true,
  "announceFinish": true,
  "announceRunningOrder": true,
  "runningOrderDepth": 3
}
```

Accessed via `ClubAudioSettings` record (deserialised from JSONB in `ClubProfile`). Admin can toggle each flag from the Race Control settings panel without a server restart.

---

## 10. Existing Infrastructure to Reuse / Extend

| Phase 5 pattern | Phase 6 reuse |
|-----------------|---------------|
| `LapTimingService` — state keyed by `raceId` | `PracticeTimingService` — state keyed by `practiceSessionId`; same `@EventListener(LapPassingEvent.class)` hook |
| `LiveRaceState` — per-race in-memory position model | `LivePracticeState` — adds `bestConsecutiveNLapMs` per participant |
| `LiveTimingHub.broadcastTimingUpdate(raceId, rows)` | `PracticeTimingHub` or extend existing hub with `broadcastPracticeUpdate(sessionId, rows)` |
| STOMP topic `/topic/race/{raceId}/timing` | New topic `/topic/practice/{sessionId}/timing` |
| `useLiveTiming(raceId)` hook | `usePracticeTiming(sessionId)` hook — same `useStomp` + REST snapshot pattern |
| `UnknownTransponderLinkDialog` | Reuse for practice transponder linking |
| `ForwarderStatusBar` | No changes — forwarder already running from Phase 5 |

### Key Difference: `LapPassingEvent` routing

`LapPassingEvent` carries a `raceId`. For practice, we need to know which active practice session should receive a passing. Two options:

1. **Option A**: Add `practiceSessionId` to `LapPassingEvent` — if non-null, route to `PracticeTimingService`; if null, route to `LapTimingService`. Requires the forwarder to know about active practice sessions.
2. **Option B**: `PracticeTimingService` subscribes to the same `LapPassingEvent` and checks whether a practice session is `RUNNING`. If yes, it processes the event alongside `LapTimingService`. Both services can process the same event concurrently.

**Recommendation: Option B** — simpler, no coupling change to the forwarder. During a practice session, a race may not be running simultaneously. If both run concurrently, both services update independently.

---

## 11. Spring Async: TTS Clip Generation

**Profile save (AUDIO-08)**:
- `@EventListener` on `UserProfileUpdatedEvent` (new domain event), processed `@Async`
- Generates name clip for each configured voice model (just the default voice for now, or per racer preference)
- If async fails (Piper down), log warning — do not fail the profile save

**Race GRID transition (AUDIO-09)**:
- `RaceStateMachineService.transition()` publishes `RaceStateChangedEvent`
- A new `AudioPreGenerationService` listens `@Async` and fires off clip generation for:
  - Car number calls for each entry (stagger start): `"Car {number}"` for each grid position
  - Countdown intervals: `"Race {name}, 5 minutes"`, `"Race {name}, 2 minutes"`, `"Race {name}, 1 minute"`, `"Race {name}, 30 seconds"`
  - Finish phrases: `"Checkered flag"`, `"Car {number} has finished"`
- Clips generated synchronously inside the `@Async` method; stored in MinIO
- Client polls `GET /api/race/{raceId}/audio-clips` to get the pre-generated clip map

**Preview endpoint (AUDIO-13)**:
- `GET /api/racer/me/name-clip?voice={voiceId}` — **synchronous** (user is waiting)
- Check MinIO for existing clip; if not present, generate synchronously via `PiperTtsClient`
- Return 302 redirect to MinIO URL, or byte array directly with `Content-Type: audio/wav`
- Piper name clip generation time: ~100–500ms — acceptable for interactive preview

---

## 12. User Entity: `phoneticName` Already Exists

✅ `User.phoneticName` field already exists (added in Phase 2 or 3) at `@Column(name = "phonetic_name")`.  
✅ `RacerProfileService.updateProfile()` already handles `phoneticName` in the update request.

**What's missing**:
- Profanity check is NOT applied yet — add to `updateProfile()` service call
- Voice preference field (`preferredVoiceId`) is NOT on `User` yet — needs a new column in an `ALTER TABLE` migration
- Admin view/override endpoint for phonetic name (AUDIO-15)

**V23 or V24**: add `preferred_voice_id VARCHAR(100)` to `users` table.

---

## 13. Frontend Architecture for Audio

### Audio Hook: `useAnnouncements`

New hook `frontend/src/hooks/race-control/useAnnouncements.ts`:

```typescript
// Responsibilities:
// 1. On GRID state: call REST to get clip URLs for current race
// 2. Cache Audio objects per URL: Map<string, HTMLAudioElement>
// 3. Expose: play(clipKey), fallbackSpeak(text)
// 4. play() attempts cached Audio; on error → fallbackSpeak()
// 5. Settings-aware: check club announcement toggles before playing
```

### Countdown Timer (AUDIO-02/06)

Client-side timer runs in `CockpitPage` once race is `RUNNING`:
- `useEffect` starts interval tracking elapsed time
- At 2min/5min marks: trigger running order announcement (`playRunningOrder(positions, depth)`)
- At countdown intervals before race start: trigger countdown announcement

**Note**: race start time is available from the STOMP `state` topic. No server-side timer needed.

### Beep sounds (AUDIO-04)

Use programmatic `AudioContext` tones (no WAV clip):
```typescript
function playBeep(improving: boolean): void {
  const ctx = new AudioContext();
  const o = ctx.createOscillator();
  o.frequency.value = improving ? 880 : 440; // high or low pitch
  o.connect(ctx.destination);
  o.start(); o.stop(ctx.currentTime + 0.2);
}
```

Simpler than storing beep WAV clips in MinIO; no TTS needed.

### Practice Page: `PracticeSessionPage`

New route: `/race-control/practice/:sessionId`  
New hook: `usePracticeTiming(sessionId)` — subscribes to `/topic/practice/{sessionId}/timing`  
Reuses `LiveTimingPanel` but with an additional "Best N Laps" column (new `PracticeTimingRowDto` field).

---

## 14. New API Endpoints Summary

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/api/racer/me/name-clip?voice={voiceId}` | Preview name clip (synchronous) |
| `GET` | `/api/race/{raceId}/audio-clips` | Get map of clip key → URL for a race |
| `GET` | `/api/admin/audio/voices` | List available Piper voice models |
| `POST` | `/api/admin/audio/settings` | Save club audio settings |
| `GET/PUT` | `/api/admin/racer/{userId}/phonetic` | Admin read/override phonetic spelling |
| `DELETE` | `/api/admin/racer/{userId}/name-clip` | Force clip regeneration |
| `GET` | `/api/admin/audio/blocklist` | List profanity blocklist |
| `POST/DELETE` | `/api/admin/audio/blocklist/{word}` | Add/remove club-specific terms |
| `POST` | `/api/race-control/practice` | Create practice session |
| `POST` | `/api/race-control/practice/{id}/start` | Start practice (IDLE → RUNNING) |
| `POST` | `/api/race-control/practice/{id}/stop` | Stop practice (RUNNING → STOPPED) |
| `GET` | `/api/race-control/practice/{id}/snapshot` | Live timing snapshot (REST seed) |
| `GET` | `/api/race-control/practice/{id}/results` | Final results (for print) |

---

## 15. Suggested Plan Decomposition (6 plans)

| Plan | Description | Key deliverables |
|------|-------------|------------------|
| 06-01 | Wave 0 stubs | @Disabled test skeletons for all Phase 6 test targets |
| 06-02 | Schema + domain foundations | V23–V25 migrations, `PracticeSession` entity, `ClubProfile` audio settings columns, voice preference on User, profanity blocklist table |
| 06-03 | Piper TTS backend | `PiperTtsClient` (Wyoming TCP), `TtsClipService` (generate + cache to MinIO), `AudioPreGenerationService` (@Async GRID hook), profanity filter, name clip endpoint |
| 06-04 | Practice timing backend | `PracticeTimingService`, `LivePracticeState` (best-N algo), `PracticeController`, practice STOMP hub, transponder routing (Option B) |
| 06-05 | Audio frontend | `useAnnouncements` hook, beep synthesis, countdown timer, audio settings admin page, racer profile phonetic/voice UI (preview button), AUDIO-01 through AUDIO-15 |
| 06-06 | Practice frontend | `PracticeSessionPage`, `usePracticeTiming` hook, best-N column, practice session CRUD in race control UI, print results page reuse |

---

## 16. Risk Register

| Risk | Severity | Mitigation |
|------|----------|-----------|
| Piper model download on first start (63MB/model) | Medium | Pre-warm with `--voice` arg; Docker volume persists after restart. Dev: add startup healthcheck. |
| Wyoming TCP protocol details diverging from spec | Low | Protocol is JSONL which is human-readable; easy to debug with a TCP logging proxy. Implement minimal client and test against real Piper container. |
| Chrome Linux speechSynthesis silent | Medium | "Test audio" button in race control primes the synthesis engine. Documented known issue. |
| Multiple Piper voices require multiple model downloads | Medium | Default only `en_GB-alan-medium` for v1. Admin must manually add other models to volume. |
| Concurrent practice + race using same `LapPassingEvent` | Low | Option B (both services handle independently) is safe — no shared mutable state between them. |
| Practice session results persistence | Low | In-memory only during session; persisted as JSON blob on STOP. Phase 7 will normalise if needed. |

---

## RESEARCH COMPLETE
