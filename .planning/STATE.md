---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: completed
stopped_at: Completed 09-04-PLAN.md
last_updated: "2026-05-15T22:30:00.000Z"
last_activity: 2026-05-15 -- Phase 09 Plan 04 complete (3 comprehensive print guides authored and human-reviewed)
progress:
  total_phases: 11
  completed_phases: 8
  total_plans: 54
  completed_plans: 53
  percent: 98
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-15)

**Core value:** Racers can enter events online and manage their own car/transponder details, while officials run a full race meeting from any Windows or Linux machine — with live timing fed directly from AMB/MyLaps hardware via a local forwarder agent to a cloud-hosted service.
**Current focus:** Phase 09 — user-manual-documentation

## Current Position

Phase: 09 (user-manual-documentation) — COMPLETE
Status: 09-04 complete (Wave 4/4)
Last activity: 2026-05-15 -- Phase 09 Plan 04 complete (3 comprehensive print guides authored and human-reviewed)

Progress: [█████████░] 93%

## Performance Metrics

**Velocity:**

- Total plans completed: 21
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
| Phase 06 P01 | ~5m | 1 task | 3 files |
| Phase 06 P02 | ~4m | 3 tasks | 13 files |
| Phase 06 P03 | ~20m | 3 tasks | 15 files |
| Phase 06 P04 | ~25m | 5 tasks | 10 files |
| Phase 06 P05 | ~6m | 3 tasks | 10 files |
| Phase 09 P01 | 2 | 2 tasks | 4 files |
| Phase 09 P02 | ~5m | 2 tasks | 13 files |
| Phase 09 P03 | ~7m | 2 tasks | 23 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Roadmap: 10 phases (7 original + Phase 8 first-run setup wizard + Phase 9 user manual/documentation + Phase 10 Docker trial environment added 2026-04-30)
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
- [Phase 05]: Radix UI Select mocked with native <select> in tests to avoid jsdom portal issues; vitest setup file added; zodResolver type cast applied to pre-existing championship form TypeScript errors
- [Phase 06-03]: assembleWav() made public (not package-private) because audio test package differs from infrastructure.tts production package
- [Phase 06-03]: Wyoming protocol uses byte-by-byte readJsonLine() + readNBytes(N) to avoid BufferedReader consuming binary PCM payloads after JSONL headers
- [Phase 06-03]: Profanity filter checks firstName/lastName/phoneticName individually in RacerProfileService.updateProfile() (no displayName field on User entity)
- [Phase 06-04]: RaceStatusChangedEvent (new Spring ApplicationEvent) published from RaceStateMachineService.transition() — decouples audio services from state machine via event bus
- [Phase 06-04]: AudioPreGenerationService uses EntryRepository+UserRepository (not RaceEntry navigation) because RaceEntry is a flat join table without JPA associations
- [Phase 06-04]: countdownIntervals stored within existing audio_settings JSONB column; no V24 migration column needed
- [Phase 06-05]: LapPassingEvent is a Java record (raceId, transponderNumber, rtcTimeMicros) — lap time derived from rtcMicros delta / 1000; no getLapTimeMs() method
- [Phase 06-05]: PracticeTimingService omits @Async to keep @Transactional effective for PracticeLap persistence
- [Phase 08-04]: SetupLayout handles pre-gate vs wizard mode internally (single /setup route) rather than splitting into two routes — keeps URL stable
- [Phase 08-04]: AuthProvider extended with setAuthFromToken(token, authUser) for bootstrap auto-login without a second network call
- [Phase 08-04]: SetupGuard wraps AuthProvider children (not outside) so useAuth() resolves in AdminBootstrapGate after bootstrap
- [Phase ?]: [Phase 09-04]: Content derived by reading implemented components before writing

### Pending Todos

None yet.

### Blockers/Concerns

- [Phase 5 resolved]: Club runs MRT Pro transponders — locked to firmware < 4.5, port 5100 RC-4 text protocol. P3 binary (port 5403) not needed for this deployment; deferred indefinitely.
- [Phase 5 resolved]: RC-4 text protocol uses session-relative timestamps (timeSinceStart offset from decoder power-on). Handled via EpochAnchor server-side anchoring — implemented in Phase 5.
- [Phase 7 resolved]: Championship DNS/DQ scoring policy — DNS counts toward Y rounds, scores 0 points. Documented with ASSUMED comment in ChampionshipStandingsQuery pending formal club confirmation.

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| *(none)* | | | |

## Session Continuity

Last session: 2026-05-15T22:20:01.666Z
Stopped at: Completed 09-03-PLAN.md
Resume file: None
