---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: Completed 05-04-PLAN.md
last_updated: "2026-04-26T20:39:57.573Z"
last_activity: 2026-04-26
progress:
  total_phases: 7
  completed_phases: 4
  total_plans: 30
  completed_plans: 29
  percent: 97
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-15)

**Core value:** Racers can enter events online and manage their own car/transponder details, while officials run a full race meeting from any Windows or Linux machine — with live timing fed directly from AMB/MyLaps hardware via a local forwarder agent to a cloud-hosted service.
**Current focus:** Phase 05 — live-timing-forwarder

## Current Position

Phase: 05 (live-timing-forwarder) — EXECUTING
Plan: 5 of 5
Status: Ready to execute
Last activity: 2026-04-26

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**

- Total plans completed: 20
- Average duration: —
- Total execution time: —

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 7 | - | - |
| 03 | 6 | - | - |
| 04 | 7 | - | - |

**Recent Trend:**

- Last 5 plans: —
- Trend: —

*Updated after each plan completion*
| Phase 04 P06 | 75 | 3 tasks | 21 files |
| Phase 05 P01 | 3m | 3 tasks | 10 files |
| Phase 05 P02 | 14 | 2 tasks | 24 files |
| Phase 05 P03 | 12m | 2 tasks | 10 files |
| Phase 05 P04 | 27m | 3 tasks | 21 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Roadmap: 7 phases derived from 91 v1 requirements at standard granularity
- Architecture: Modular monolith — single Spring Boot JAR, single PostgreSQL database
- AMB Protocol: Highest-risk unknown — must register at mylaps.com/developers or capture Wireshark traces before Phase 5
- [Phase 04]: LapTimingService uses EntryRepository for transponder resolution via Entry.transponderNumberSnapshot snapshot field
- [Phase 04]: CTRL-09 skip-to is process-local ConcurrentHashMap on RaceControlController — cross-session persistence deferred
- [Phase 05]: Wave-0 stubs use class-level @Disabled with Assertions.fail() bodies; forwarder/build.gradle.kts adds only junit-jupiter:5.10.2 — no Netty/gRPC until Plan 02
- [Phase 05]: protobuf-gradle-plugin 0.9.4 used; AmbRc4TimingSource reuses EpochAnchor/Parser/GapDetector across reconnects to preserve epoch (Pitfall 2)
- [Phase 05]: FakeDecoderServer.stop() uses CopyOnWriteArrayList<Socket> activeClients; IT test adds 200ms sleep before server.stop() to eliminate accept-loop race condition
- [Phase 05]: ForwarderTokenControllerTest uses forwarderTokenRepository.deleteAll() in @BeforeEach to isolate against shared Testcontainer DB state
- [Phase 05]: UnknownTransponderLinkAudit in forwarder package (singular table) separate from domain.race.UnknownTransponderLink (plural table from V18); preserves CTRL-06 vs TIMING-08 separation
- [Phase 05]: AbstractIntegrationTest uses app.grpc.port=0 to prevent port conflicts when multiple Spring test contexts start ForwarderGrpcServer simultaneously

### Pending Todos

None yet.

### Blockers/Concerns

- [Phase 5 blocker]: AMB P3 exact binary frame format for club's decoder model is unknown — need SDK registration or Wireshark capture before forwarder implementation
- [Phase 5 blocker]: Decoder timestamp format (UTC vs session-relative offset) unknown — determines clock drift handling
- [Phase 7 concern]: Championship DNS/DQ scoring policy (does DNS count toward Y rounds?) needs club confirmation before Phase 7 scoring implementation

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| *(none)* | | | |

## Session Continuity

Last session: 2026-04-26T20:39:57.567Z
Stopped at: Completed 05-04-PLAN.md
Resume file: None
