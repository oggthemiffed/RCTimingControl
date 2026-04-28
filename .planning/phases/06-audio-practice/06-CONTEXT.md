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

### TTS Architecture — BROWSER ONLY (LOCKED)
- **Web Speech API only** — zero server-side TTS provider. No Google Cloud, AWS Polly, Azure, or OpenAI TTS integration.
- All speech synthesis happens in the race control browser via `window.speechSynthesis`.
- AUDIO-08 interpretation: the server stores the phonetic spelling field; the browser performs synthesis using it. No audio clip files are generated or stored server-side.
- AUDIO-09 interpretation: "pre-generate" means the browser pre-warms synthesis for all race entries during GRID state (pre-issue synthesis calls with volume=0 or cache `SpeechSynthesisUtterance` objects). No server clip generation.
- AUDIO-10 interpretation: no HTTP clip serving needed. Client-side caching of utterance objects.
- AUDIO-13 interpretation: voice preference is a stored racer preference. Browser enumerates available voices via `speechSynthesis.getVoices()`; racer selects preferred voice name; preference is persisted server-side and sent to the client with the race grid data.
- MinIO (already in stack) is available if a future phase adds server-side TTS; no audio files stored in Phase 6.

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

- **Web Speech API reliability**: `speechSynthesis` has known issues in some browsers (Chrome on Linux can be silent until interacted with). The plan should include a user-visible "Test audio" button to verify synthesis works before a race starts.
- **Voice enumeration timing**: `speechSynthesis.getVoices()` is async on first call in Chrome. Use `voiceschanged` event. Factor this into the voice selection UI.
- **Practice vs Race in STOMP**: Practice session will need its own STOMP topic pattern, e.g. `/topic/practice/{sessionId}/timing`. Do NOT reuse the race topic — different entity lifecycle.
- **Profanity library**: backend profanity check via a Java library (e.g. `com.vdurmont:emoji-java` is not right — look for a dedicated Java profanity/content filter, or a simple word-list implementation). Keep it simple — a plain word-list + regex matcher is sufficient for a club tool.

</specifics>

<deferred>
## Deferred Ideas

- Full server-side TTS integration (Google Cloud TTS, AWS Polly, etc.) — the MinIO storage is ready if this is added in a future phase. Out of scope for Phase 6.
- Audio playback logging / analytics — out of scope.
- Practice session history/archive beyond the current session — out of scope for Phase 6.

</deferred>

---

*Phase: 06-audio-practice*
*Context gathered: 2026-04-28 via interactive discussion*
