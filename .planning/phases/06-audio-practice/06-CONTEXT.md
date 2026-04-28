# Phase 6: Audio & Practice — Context

**Gathered:** 2026-04-28
**Status:** Ready for planning
**Source:** Interactive discussion

---

<domain>
## Phase Boundary

Phase 6 delivers two capabilities on top of the running live-timing system:

1. **Audio announcements** — the race control browser produces voice announcements throughout the meeting using the Web Speech API. No server-side TTS provider. All synthesis is browser-side.
2. **Open practice sessions** — officials can run a live-timed open practice session (standalone or event-linked) with automatic transponder detection and a best-consecutive-laps display.

</domain>

<decisions>
## Implementation Decisions

### TTS Architecture — PIPER TTS (LOCAL, FREE) + WEB SPEECH API FALLBACK (LOCKED)
- **Piper TTS** as a Docker sidecar (added to `docker-compose.yml`). Free, fully offline, zero running cost.
- Image: `rhasspy/wyoming-piper` — Spring calls it via HTTP to generate `.wav` clips.
- Generated clips stored in MinIO (already in stack). Served via HTTP to the race control browser (AUDIO-10).
- **Fallback**: Web Speech API (`window.speechSynthesis`) used if a clip is unavailable at playback time (AUDIO-11). Non-blocking — never prevents a race from running.
- AUDIO-08: When a racer profile is saved, Spring calls Piper to generate a name clip and stores it in MinIO. Regenerated on display name or phonetic spelling change.
- AUDIO-09: When race transitions to `GRID`, server pre-generates all predictable clips (countdown intervals, stagger car-number calls, finish announcements) and caches in MinIO.
- AUDIO-10: Race control client fetches and locally caches all clips for the current race during grid preparation.
- AUDIO-13: Voice selection from Piper's available voice models. Admin configures system default voice (e.g. `en_GB-alan-medium`). Racer can select a preferred voice from available models. Voice preference stored per racer server-side.
- Voice model: English voices from Rhasspy HuggingFace repository (~50MB model file, bundled with Docker image config). `en_GB-alan-medium` as the default for a UK club.

### Phonetic Spelling Field
- Optional field on racer profile (AUDIO-12). Stored server-side.
- If set, browser uses this text as the utterance string for the racer's name.
- If not set, falls back to display name.
- Screened against profanity blocklist before saving (AUDIO-14).
- Admin can view, override, or clear phonetic spelling per racer (AUDIO-15).

### Profanity Blocklist (AUDIO-14)
- **Both**: a base library (e.g. `bad-words` npm package on the backend) provides sensible defaults out of the box.
- Admin can extend the list with club-specific terms.
- Blocklist applied to both display name AND phonetic spelling fields on save.
- Club-specific extensions stored in DB (new table or JSON column on club config).
- If blocked: save rejected, validation error shown to user.

### Running Order Announcement Depth (AUDIO-06)
- **Configurable per club** as an admin setting (e.g. "announce top N positions").
- Default value: top 3 (sensible club default).
- Stored on club config entity.
- Announced at 2-minute intervals for first 10 minutes, then 5-minute intervals.

### Practice Session Architecture (PRACTICE-01)
- **Both modes supported**:
  - **Standalone**: race director creates a practice session with just a name (no event link). Lives independently in DB.
  - **Event-linked**: practice session optionally links to an event (useful for practice rounds before a race day).
- Single `PracticeSession` entity covers both. `eventId` is nullable.
- Practice session has its own state machine: `IDLE → RUNNING → STOPPED`.
- Live lap data flows through existing `LapTimingService` / STOMP topic infrastructure (reuse Phase 5).
- Results are printable after the session (PDF, reuse existing print infrastructure).

### Racer Detection During Practice (PRACTICE-01/02)
- **Hybrid**: known transponders auto-added to live display as soon as they pass the decoder.
- Unknown transponders: surfaced in UI (reuse Phase 5's `UnknownTransponderLinkDialog` or similar). Race director can link them on the fly.
- No pre-registration required — just start the session and run.

### Best Consecutive Laps Display (PRACTICE-02)
- Shows each racer's best run of N consecutive laps (configurable N, default 3).
- Calculated in memory during session, same as live race position calculations.
- Displayed alongside single best lap in the practice display.
- N configurable per session (set when creating/starting the session, defaulting to club default).

### Announcement Toggle Granularity (AUDIO-07)
- Each announcement TYPE is individually on/off in admin settings:
  - Countdown intervals (AUDIO-02)
  - Stagger car-number calls (AUDIO-03)
  - Per-lap beeps improving/not-improving (AUDIO-04)
  - Finish announcements (AUDIO-05)
  - Running order announcements (AUDIO-06)
- Stored in club config or race control session settings (admin decides at race-day, not just admin panel).

### Stagger Interval Configuration (AUDIO-03)
- Stagger interval (seconds between car calls) is part of the race format config (already in Phase 4 as a FORMAT field).
- No new config needed — use existing stagger interval.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope
- `.planning/ROADMAP.md` — Phase 6 goal, requirements list (AUDIO-01..15, PRACTICE-01..02), success criteria
- `.planning/REQUIREMENTS.md` — Full requirement text for all 17 requirements

### Existing infrastructure to extend
- `app/src/main/java/dev/monkeypatch/rctiming/timing/` — `LapTimingService`, `LiveRaceState`, `LiveTimingHub` — practice reuses this
- `app/src/main/java/dev/monkeypatch/rctiming/config/` — `AsyncConfig`, club config entities — add announcement prefs and blocklist
- `frontend/src/pages/race-control/` — existing race control layout, cockpit/referee pages — audio hooks attach here
- `frontend/src/hooks/race-control/useLiveTiming.ts` — STOMP subscription pattern to follow for practice
- `app/src/main/resources/db/migration/` — Flyway migrations — Phase 6 will need V23+

### Format config
- `.planning/phases/04-race-state-machine/04-HEAT-STRUCTURE-SPEC.md` — stagger interval lives in race format config

### Phase 5 patterns (transponder linking, forwarder status)
- `frontend/src/pages/race-control/dialogs/` — UnknownTransponderLinkDialog pattern to reuse for practice

</canonical_refs>

<specifics>
## Specific Implementation Notes

- **Piper Docker image**: Use `rhasspy/wyoming-piper`. Mount voice model files as a volume. Expose HTTP port. Spring `RestTemplate`/`WebClient` calls it synchronously for clip generation.
- **Clip naming in MinIO**: Use a deterministic key scheme, e.g. `audio/racer/{racerId}/name.wav` and `audio/race/{raceId}/countdown-{seconds}.wav` so clips can be checked for existence before regenerating.
- **Web Speech API fallback**: Add a user-visible "Test audio" button in race control settings to verify browser synthesis works (Chrome on Linux can be silent until first user interaction).
- **Practice vs Race in STOMP**: Practice session will need its own STOMP topic pattern, e.g. `/topic/practice/{sessionId}/timing`. Do NOT reuse the race topic — different entity lifecycle.
- **Profanity library**: backend profanity check — use a simple Java word-list + regex matcher. A plain word-list is sufficient for a club tool. No heavy dependency needed.

</specifics>

<deferred>
## Deferred Ideas

- Paid cloud TTS providers (Google Cloud TTS, AWS Polly, etc.) — Piper covers quality needs at zero cost. Could be added as an alternative `TtsProvider` implementation in a future phase.
- Audio playback logging / analytics — out of scope.
- Practice session history/archive beyond the current session — out of scope for Phase 6.

</deferred>

---

*Phase: 06-audio-practice*
*Context gathered: 2026-04-28 via interactive discussion*
