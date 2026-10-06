# Phase 5: Live Timing & Forwarder - Context

**Gathered:** 2026-04-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 5 delivers the AMB decoder integration end-to-end: the forwarder submodule (Java Gradle, runs on-site at the club) connects to the AMB RC-4 decoder over TCP, streams decoded lap events to the cloud service via gRPC, and the cloud service feeds them into the existing `LapPassingEvent` → `LapTimingService` → `LiveTimingHub` → STOMP chain built in Phase 4.

Phase 5 also delivers:
- TCP decoder simulator (playback + generative) as a separate runnable in the forwarder module
- Admin UI: API token generation/regeneration for forwarder authentication
- Connection status display (decoder and forwarder) in the race control cockpit
- In-race unknown transponder linking with retroactive lap credit

The `LapTimingService`, `LiveTimingHub`, and STOMP WebSocket topology from Phase 4 are **not changed** — Phase 5 only adds the upstream producer (forwarder → gRPC → app → ApplicationEventPublisher).

Out of scope for Phase 5: P3 binary protocol implementation (deferred to backlog), multi-loop aware filtering (noted as a gap, deferred), audio announcements (Phase 6), results/championship (Phase 7).

</domain>

<decisions>
## Implementation Decisions

### Protocol Support
- **D-01:** Implement AMB RC-4 text protocol only in Phase 5. P3 binary deferred to backlog.
- **D-02:** `TimingSource` interface must be defined cleanly (per TIMING-05) with one RC-4 implementation. P3 will slot in as a second implementation without changes to race control or timing logic.
- **D-03:** The RC-4 parser (`AmbRc4TimingSource`) is a pure function at its core — parsing a line into a domain event — with no Spring dependencies. Protocol I/O is separate from parsing (per CLAUDE.md).

### Decoder TCP Simulator
- **D-04:** Simulator fidelity: **playback + generative mode**. Playback replays real capture files from `RCTimingForwarder/samples/` over TCP at configurable speed. Generative mode emits synthetic PASSING records for a configured set of transponder IDs at configurable intervals.
- **D-05:** Simulator location: **separate runnable in the forwarder module** (`forwarder/src/main/java/.../simulator/`). Launched via Gradle task (e.g., `./gradlew :forwarder:runSimulator`). The real forwarder connects to it identically to a real decoder — full code path exercised.

### RC-4 Timestamp Epoch Strategy
- **D-06:** RC-4 `timeSinceStart_s` is seconds-since-decoder-power-on (float, 3 decimal places = millisecond resolution). **Relative accuracy is sufficient** for RC club timing — lap times are differences between consecutive passings, not absolute wall-clock values.
- **D-07:** Conversion strategy: record `Instant.now()` as `decoderEpoch` when the first PASSING record arrives. For each subsequent record: `rtcTimeMicros = (decoderEpoch.toEpochMilli() + (timeSinceStart_s - firstTimeSinceStart_s) * 1000) * 1000L`. Error bounded by LAN RTT (< 1ms) — negligible for RC timing.

### Forwarder Authentication (FORWARDER-05)
- **D-08:** Forwarder authenticates with the cloud service using a static API token stored in `forwarder.properties` (or `application.properties` in the forwarder module). Format: `forwarder.api-token=<value>`.
- **D-09:** Token renewal: admin generates/regenerates the token via an admin portal page (in the main app). New token is displayed once for copy-paste into the forwarder config file. No automated rotation in v1. Token has no hardcoded expiry — renewal is admin-initiated (compromise response or periodic rotation).

### Multi-Loop Handling
- **D-10:** Phase 5 treats all RC-4 PASSING records as start/finish passings regardless of loop ID. Loop ID field is logged but ignored for scoring. **Noted as a future gap**: loop-aware filtering (admin config of scoring loop IDs) is deferred to a future phase.

### Unknown Transponder Handling (TIMING-08)
- **D-11:** When a PASSING arrives for an unregistered transponder, the cockpit alert (already built in Phase 4 — broadcasts on `/topic/race/{raceId}/unknown-transponder`) remains. Phase 5 adds the **link in-race** action: race director can assign the unknown transponder to an entry directly from the cockpit alert.
- **D-12:** Retroactive credit: when an unknown transponder is linked to an entry, **all passings from that transponder since race start are retroactively credited** to the linked entry. In-memory `LiveRaceState` is updated and positions are rebroadcast.

### Connection Status Display (TIMING-02)
- **D-13:** A slim persistent status bar appears at the top of the race control cockpit (below the main nav bar), always visible. Shows two status pills:
  - `● DECODER connected/disconnected` — reflects the forwarder's TCP connection to the physical decoder
  - `● FORWARDER connected/disconnected` — reflects the cloud app's gRPC connection to the forwarder
- **D-14:** Status updates are delivered via a new STOMP topic: `/topic/system/forwarder-status`. The gRPC receiver publishes status changes; the frontend subscribes and updates the status bar reactively.
- **D-15:** Pill colours: green dot = connected, red dot = disconnected/error, amber dot = reconnecting.

### Claude's Discretion
- gRPC protobuf schema design (field names, message types for streaming passing events and status updates)
- Netty channel configuration for the RC-4 TCP client (reconnect backoff, keep-alive)
- WATCHDOG absence threshold for declaring decoder disconnected (suggest 10 seconds — STATUS records arrive every ~5s)
- Exact Gradle task names and simulator CLI flags
- gRPC server port (suggest 9090, configurable)
- REST URL for admin token generation (`POST /api/v1/admin/forwarder/token` or similar)
- How the in-race transponder link endpoint is secured (RACE_DIRECTOR or ADMIN role)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Protocol Documentation
- `docs/AMB_DECODER_PROTOCOL.md` — **Read in full.** Definitive protocol reference covering both AMB RC-4 text protocol (Phase 5 target) and P3 binary. Includes: RC-4 line format, field reference, STATUS and PASSING record layouts, CRC format, timestamp handling, firmware version boundary, port numbers (RC-4 = 5100, P3 = 5403), and implementation notes. Sourced from real TCP captures and the Go prototype.

### Existing Phase 4 Timing Code (do not modify these — Phase 5 feeds into them)
- `app/src/main/java/dev/monkeypatch/rctiming/timing/LapPassingEvent.java` — The domain event record Phase 5 must publish. `rtcTimeMicros` is a `long` (microseconds).
- `app/src/main/java/dev/monkeypatch/rctiming/timing/LapTimingService.java` — `@EventListener(LapPassingEvent.class)` consumer. Already handles transponder resolution, position calculation, and STOMP broadcast. Phase 5 does not modify this.
- `app/src/main/java/dev/monkeypatch/rctiming/timing/LiveTimingHub.java` — STOMP broadcast hub. Already has `broadcastUnknownTransponder()`. Phase 5 adds the status topic but does not change existing methods.
- `app/src/main/java/dev/monkeypatch/rctiming/timing/LiveRaceState.java` — In-memory race state. Phase 5 adds retroactive transponder linking (D-12) — this file may need a new method.

### Synthetic Timing (keep, don't remove)
- `app/src/main/java/dev/monkeypatch/rctiming/service/SyntheticTimingService.java` — Dev-profile-only synthetic event generator. Retained in Phase 5 for dev testing even with the real forwarder available.
- `app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/DevSyntheticTimingController.java` — Dev-profile REST endpoint. Retained.

### Forwarder Module Shell
- `forwarder/build.gradle.kts` — Currently `plugins { java }` placeholder. Phase 5 fills this in with gRPC, Netty, and shared domain dependencies.

### Stack & Architecture
- `CLAUDE.md` — Full stack spec. Pay particular attention to: Netty 4.1.x for TCP, gRPC bidirectional streaming, `TimingSource` interface requirement (TIMING-05), STOMP topics, and the "dedicated background thread via SmartLifecycle" requirement for the TCP receiver.

### Requirements
- `.planning/REQUIREMENTS.md` — Phase 5 requirements: FORWARDER-01 through FORWARDER-05, TIMING-01 through TIMING-08.
- `.planning/ROADMAP.md` §"Phase 5: Live Timing & Forwarder" — Goal, success criteria, full requirements list.

### Prior Phase Context
- `.planning/phases/04-race-state-machine/04-CONTEXT.md` — D-06/D-07: STOMP WebSocket topology built in Phase 4, synthetic timing generator. D-14/D-15: marshal lap audit trail (in-memory state). D-17/D-18: referee page subscribes to same STOMP topics.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `LapPassingEvent` record — already defined with correct fields. Phase 5 publishes to this; does not redefine it.
- `LiveTimingHub.broadcastUnknownTransponder()` — already sends unknown transponder alerts to the cockpit frontend. Phase 5 connects the UI action (link in-race) to this existing signal.
- `SyntheticTimingService` — RC-4 forwarder replaces this as the live data source, but the synthetic service is retained for dev convenience. Both can coexist since the forwarder only connects when running.
- `WebSocketJwtChannelInterceptor` — existing JWT auth for STOMP. The new `/topic/system/forwarder-status` topic follows the same auth pattern.
- `app/src/main/resources/db/migration/` — latest migration is V18 (Phase 4). Phase 5 may need a migration for the forwarder API token table (to store the token server-side for validation).

### Established Patterns
- `ApplicationEventPublisher` → `@EventListener` → `@Async` — the event bus pattern the forwarder gRPC receiver must use to publish `LapPassingEvent`
- Hibernate write / jOOQ read seam — any new entities (forwarder token record) use Hibernate; any projections use jOOQ
- Spring `@Profile("dev")` gating for dev-only beans — simulator runnable is standalone, but any dev-only Spring beans in the app follow this pattern
- `SmartLifecycle` or `ApplicationRunner` for background threads (per CLAUDE.md) — TCP receiver and gRPC client lifecycle management

### Integration Points
- Forwarder → gRPC → `ForwarderGrpcService` (new Spring bean) → `ApplicationEventPublisher.publishEvent(LapPassingEvent)` → existing `LapTimingService`
- `LiveRaceState` — Phase 5 adds a method for retroactive transponder linking (D-12). The method replays all cached passings for the newly linked transponder and recalculates positions.
- Frontend race control cockpit — new status bar component subscribes to `/topic/system/forwarder-status` via STOMP and renders the two-pill display
- Admin panel — new token management page (`GET`/`POST /api/v1/admin/forwarder/token`) for generating and displaying the forwarder API token

</code_context>

<specifics>
## Specific Ideas

- **Status bar layout:** Slim bar below the main nav, always visible in the cockpit. Two pills: `● DECODER connected` and `● FORWARDER connected`. Green/amber/red dots. User explicitly selected this layout from a preview mockup.
- **In-race transponder link:** User explicitly wants the ability to link an unknown transponder to an entry directly from the cockpit during a race, with full retroactive credit for all passings since race start. This must be in Phase 5 scope.
- **Token renewal UX:** Admin sees current token status (last generated, never expires but can be revoked/regenerated). Generating a new token shows it once — admin copies it to the forwarder config. Existing token is invalidated when a new one is generated.
- **Real capture samples as simulator seed:** Use the files from `RCTimingForwarder/samples/` (real TCP dumps from the club decoder) as the playback source for the simulator. Ensures the forwarder is tested against real-world protocol data.
- **Multi-loop gap:** Loop-aware filtering (scoring loop config) is explicitly deferred. Add it to the backlog as a named gap so it's not forgotten.

</specifics>

<deferred>
## Deferred Ideas

- **P3 binary protocol implementation** — Deferred to backlog. `TimingSource` interface makes it a drop-in when needed. `docs/AMB_DECODER_PROTOCOL.md` has the full P3 spec ready.
- **Multi-loop aware filtering** — Deferred gap: admin config of which loop IDs count as scoring loops. All RC-4 passings treated as start/finish in Phase 5.
- **Automated token expiry/rotation** — No hardcoded expiry in v1. Token renewal is admin-initiated only.
- **Multi-decoder operation** — TRACK-04 noted as post-v1.
- **Audio announcements** — Phase 6.
- **Results & championship** — Phase 7.

</deferred>

---

*Phase: 05-live-timing-forwarder*
*Context gathered: 2026-04-25*
