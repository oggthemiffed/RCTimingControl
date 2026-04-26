# Phase 5: Live Timing & Forwarder - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-25
**Phase:** 05-live-timing-forwarder
**Areas discussed:** Protocol scope, Decoder simulator, RC-4 timestamp epoch, Connection status UI, gRPC auth token, Multi-loop handling, Unknown transponder linking

---

## Protocol Scope

| Option | Description | Selected |
|--------|-------------|----------|
| RC-4 only | Implement TimingSource interface with one RC-4 implementation. P3 deferred to backlog. | ✓ |
| Both RC-4 and P3 | Build both implementations in Phase 5 while protocol analysis is fresh. | |

**User's choice:** RC-4 only
**Notes:** P3 deferred to backlog. TimingSource interface keeps it a clean drop-in when needed.

---

## Decoder Simulator

| Option | Description | Selected |
|--------|-------------|----------|
| Playback mode only | Replays real capture files over TCP at configurable speed. | |
| Playback + generative | Playback mode plus synthetic random passings for configured transponders. | ✓ |
| Generative only | Only emits synthetic random passings. | |

**Simulator location:**

| Option | Description | Selected |
|--------|-------------|----------|
| Separate runnable in forwarder module | Main class launchable via Gradle task. | ✓ |
| Test utility only | Embedded in integration test infra, not standalone. | |

**User's choice:** Playback + generative, separate runnable in forwarder module
**Notes:** Real capture files from RCTimingForwarder/samples/ used as playback seed.

---

## RC-4 Timestamp Epoch

| Option | Description | Selected |
|--------|-------------|----------|
| Relative accuracy is fine | Anchor decoderEpoch to Instant.now() on first packet. Error < 1ms. | ✓ |
| Wall-clock accuracy needed | NTP-grade sync or reference timestamp from decoder. | |

**User's choice:** Relative accuracy is fine
**Notes:** Lap times are differences between consecutive passings — absolute wall-clock irrelevant for RC club timing.

---

## Connection Status UI

| Option | Description | Selected |
|--------|-------------|----------|
| Status bar at top of cockpit | Slim persistent bar below main nav with two pills: DECODER and FORWARDER. | ✓ |
| Corner badge in cockpit header | Small coloured dots in top-right corner. | |
| Dedicated section in left panel | Hardware section at bottom of run-order panel. | |

**User's choice:** Status bar at top of cockpit
**Notes:** User selected from preview mockup. Green/amber/red dot indicators.

---

## gRPC Auth Token

| Option | Description | Selected |
|--------|-------------|----------|
| API token in config file | Static token in forwarder.properties. | ✓ |
| Environment variable | FORWARDER_API_TOKEN env var. | |

**User's choice:** Config file token
**Notes:** User added: "as long as we have a way to renew the token." Admin portal token generation/regeneration page added to Phase 5 scope.

---

## Multi-Loop Handling

| Option | Description | Selected |
|--------|-------------|----------|
| All loops = start/finish | Every PASSING treated as lap completion. Loop ID logged but ignored. | ✓ (Phase 5) |
| Loop-aware handling | Filter passings by configured scoring loop IDs. | deferred |

**User's choice:** All loops = start/finish in Phase 5; loop-aware filtering noted as a named deferred gap
**Notes:** "1 now but lets do 2 as a gap to be completed later"

---

## Unknown Transponder Handling

| Option | Description | Selected |
|--------|-------------|----------|
| Alert only — log and display | Dismissable cockpit alert. No in-race action. | |
| Link in-race | Race director assigns unknown transponder to entry from cockpit alert. | ✓ |

**Retroactive credit:**

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — retroactive credit | All passings from that transponder since race start credited. | ✓ |
| Forward only | Only future passings credited after the link. | |

**User's choice:** Link in-race with full retroactive credit
**Notes:** Use case: racer swapped transponders at last minute and has been lapping without credit.

---

## Claude's Discretion

- gRPC protobuf schema design
- Netty channel configuration and reconnect backoff
- WATCHDOG absence threshold (suggested: 10 seconds)
- Gradle task names and simulator CLI flags
- gRPC server port (suggested: 9090, configurable)
- REST URL for admin token generation
- In-race transponder link endpoint role (RACE_DIRECTOR or ADMIN)

## Deferred Ideas

- P3 binary protocol implementation — TimingSource interface makes it a drop-in
- Multi-loop aware filtering — named gap, deferred
- Automated token expiry/rotation — admin-initiated only in v1
- Multi-decoder operation — post-v1
