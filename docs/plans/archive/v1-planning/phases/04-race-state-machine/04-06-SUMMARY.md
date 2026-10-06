---
phase: 04-race-state-machine
plan: 06
subsystem: race-control-api
tags: [java, spring, websocket, stomp, live-timing, rest, race-control, state-machine, integration-test]

requires:
  - phase: 04-race-state-machine/plan-02
    provides: Race, Round, RaceStatus, RaceStateMachineService, MarshalAdjustment, UnknownTransponderLink and repos
  - phase: 04-race-state-machine/plan-03
    provides: RoundGeneratorService.applyPreviousRoundFinishingOrder

provides:
  - WebSocketConfig: STOMP broker on /ws/timing, no SockJS, JWT channel interceptor wired
  - WebSocketJwtChannelInterceptor: JWT validation on STOMP CONNECT frame (Pitfall 1 pattern)
  - LapPassingEvent + LiveRacePosition + LiveRaceState: in-memory per-race position model
  - LapTimingService: ConcurrentHashMap<Long,LiveRaceState>, @Async @EventListener for lap events
  - LiveTimingHub: SimpMessagingTemplate broadcasts on /timing, /state, /marshal, /unknown-transponder
  - SyntheticTimingService: @Profile("dev") synthetic lap event generator
  - RunOrderQuery + RunOrderItemDto: jOOQ run-order list for cockpit left panel (D-04)
  - RaceControlController: REST endpoints CTRL-01 (lifecycle), CTRL-03 (marshal), CTRL-06 (unknown transponder), CTRL-08 (abandon), CTRL-09 (skip-to)
  - DevSyntheticTimingController: @Profile("dev") POST /api/v1/dev/race/{id}/synthetic-passing
  - RaceStateMachineService: now broadcasts on every transition + wires applyPreviousRoundFinishingOrder on RUNNING→FINISHED

affects: [04-race-state-machine/plan-07, cockpit-frontend, live-timing-frontend]

tech-stack:
  added:
    - spring-boot-starter-websocket (already added prior to this plan)
    - @EnableAsync on RcTimingApplication (new)
  patterns:
    - "STOMP in-memory broker: /topic prefix, /app destination prefix, /ws/timing endpoint (no SockJS)"
    - "JWT at STOMP CONNECT not HTTP upgrade (Pitfall 1): /ws/timing permitAll in SecurityConfig, JWT validated in WebSocketJwtChannelInterceptor"
    - "LiveRaceState synchronized on this (Pitfall 3): all mutations under intrinsic lock"
    - "ConcurrentHashMap<Long,LiveRaceState> in LapTimingService: computeIfAbsent creates state per race"
    - "Process-local skip-to override: ConcurrentHashMap<Long,Long> activeRaceByEvent on RaceControlController"
    - "Finishing-order propagation: RUNNING→FINISHED hook looks up next round/heat/class race and calls applyPreviousRoundFinishingOrder"

key-files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/config/websocket/WebSocketConfig.java
    - app/src/main/java/dev/monkeypatch/rctiming/security/WebSocketJwtChannelInterceptor.java
    - app/src/main/java/dev/monkeypatch/rctiming/timing/LapPassingEvent.java
    - app/src/main/java/dev/monkeypatch/rctiming/timing/LiveRacePosition.java
    - app/src/main/java/dev/monkeypatch/rctiming/timing/LiveRaceState.java
    - app/src/main/java/dev/monkeypatch/rctiming/timing/LapTimingService.java
    - app/src/main/java/dev/monkeypatch/rctiming/timing/LiveTimingHub.java
    - app/src/main/java/dev/monkeypatch/rctiming/timing/dto/LiveTimingRowDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/timing/dto/RaceStateChangeDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/timing/dto/MarshalAdjustmentDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/SyntheticTimingService.java
    - app/src/main/java/dev/monkeypatch/rctiming/query/racecontrol/RunOrderQuery.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/RaceControlController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/DevSyntheticTimingController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/dto/RunOrderItemDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/dto/MarshalAdjustmentRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/dto/UnknownTransponderLinkRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/dto/SkipToRaceRequest.java
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceStateMachineService.java
    - app/src/main/java/dev/monkeypatch/rctiming/RcTimingApplication.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/racecontrol/RaceControlControllerIT.java

key-decisions:
  - "LapTimingService injects EntryRepository (not TransponderRepository) to resolve transponder→entry via Entry.transponderNumberSnapshot — avoids N+1 on Transponder table since Entry already has snapshot"
  - "RaceStateMachineService zero-arg constructor delegates to full constructor with null guards — preserves plan-02 unit tests without any test changes"
  - "CTRL-09 skip-to is process-local ConcurrentHashMap on RaceControlController — cross-session persistence deferred beyond Phase 4 per spec"
  - "Test helper seedRoundWithClass() returns (Round, eventClassId) record to avoid eventClassRepository.findAll() which would deserialize all EventClass rows including those from other test classes with potentially incompatible configSnapshot JSON"
  - "Gap calculation in LiveRaceState uses leaderLastPassingMs - myLastPassingMs (same logical-clock basis) — documented simplification for Phase 4; multi-lap gap deferred to plan 07"

duration: 75min
completed: 2026-04-24
---

# Phase 4 Plan 06: WebSocket Live Timing Infrastructure + Race Control API Summary

**STOMP WebSocket infrastructure, in-memory LiveRaceState, and all CTRL endpoint implementations — 7 integration tests passing, state machine wired for finishing-order propagation**

## Performance

- **Duration:** ~75 min
- **Completed:** 2026-04-24
- **Tasks:** 3/3 complete
- **Files created:** 18 new, 3 modified

## Accomplishments

### Task 1: WebSocket Infrastructure
- `WebSocketConfig`: STOMP broker at `/ws/timing`, no SockJS, JWT channel interceptor wired in `configureClientInboundChannel`
- `WebSocketJwtChannelInterceptor`: validates JWT on `StompCommand.CONNECT`, returns null to reject unauthenticated frames
- `LiveRaceState`: in-memory model with `synchronized` mutating methods — `applyLapPassing`, `applyLapDelta`, `calculatePositions` (sorted by laps DESC, time ASC with gap calculation)
- `LapTimingService`: `ConcurrentHashMap<Long, LiveRaceState>`, `@Async @EventListener(LapPassingEvent.class)`, transponder→entry resolution via `EntryRepository`
- `LiveTimingHub`: 4 `SimpMessagingTemplate.convertAndSend` calls on `/timing`, `/state`, `/marshal`, `/unknown-transponder` topics
- `SyntheticTimingService`: `@Profile("dev")` fires random `LapPassingEvent` for any race entry
- `@EnableAsync` added to `RcTimingApplication`

### Task 2: Run-Order Query
- `RunOrderQuery`: jOOQ join of `ROUNDS→RACES→EVENT_CLASSES→RACING_CLASSES` ordered by `sequenceInEvent ASC, heatNumber ASC`
- `RunOrderItemDto`: 9-field record including roundType, className, finalLetter, status
- `RaceControlController`: skeleton with `GET /event/{eventId}/run-order`

### Task 3: POST Endpoints + State Machine Hook
- `RaceControlController`: full constructor with 8 collaborators; all POST endpoints (call-grid, start, stop, abandon, marshal-adjustment, unknown-transponder-link, skip-to)
- `DevSyntheticTimingController`: `@Profile("dev")` POST `/api/v1/dev/race/{id}/synthetic-passing` → 202
- `RaceStateMachineService`: extended with `LiveTimingHub`, `RoundGeneratorService`, `LapTimingService`, `RoundRepository`; broadcasts on every transition; wires `applyPreviousRoundFinishingOrder` on RUNNING→FINISHED for PRACTICE/QUALIFIER rounds
- `RaceControlControllerIT`: 7 passing tests + 1 intentionally `@Disabled` (getPrintResults, plan 07)
- `RaceStateMachineServiceTest`: all 4 plan-02 unit tests still pass

## Task Commits

1. **Task 1: WebSocket infrastructure** — `e7f4a60`
2. **Task 2: RunOrderQuery + GET endpoint** — `0f5fc32`
3. **Task 3: POST endpoints + state machine** — `e83b81f`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing] @EnableAsync added to RcTimingApplication**
- **Found during:** Task 1
- **Issue:** `@Async` on `LapTimingService.onLapPassing` requires `@EnableAsync` to be present; was missing from main class
- **Fix:** Added `@EnableAsync` to `RcTimingApplication`
- **Files modified:** `RcTimingApplication.java`
- **Commit:** e7f4a60

**2. [Rule 1 - Bug] Test helper refactored to avoid findAll() deserialization failure**
- **Found during:** Task 3 test run
- **Issue:** `eventClassRepository.findAll()` in test helper deserialized all EventClass rows from other test classes, some with malformed `config_snapshot` JSON from prior test suites — caused `InvalidTypeIdException: Could not resolve type id 'jsonb'`
- **Fix:** Introduced `RoundWithClass` record in test; `seedRoundWithClass()` returns both Round and eventClassId directly, avoiding cross-test `findAll()` query
- **Files modified:** `RaceControlControllerIT.java`
- **Commit:** e83b81f

**3. [Rule 1 - Bug] LapTimingService uses EntryRepository not TransponderRepository**
- **Found during:** Task 1
- **Issue:** Plan specified `TransponderRepository` for transponder→entry resolution, but the correct lookup path is Race→RaceEntry→Entry.transponderNumberSnapshot; TransponderRepository resolves user ownership not race entry
- **Fix:** Injected `EntryRepository` instead; resolves via `Entry.transponderNumberSnapshot` field
- **Files modified:** `LapTimingService.java`

## Known Stubs

- `LapTimingService.resolveEntryId` iterates all RaceEntries and loads each Entry individually (N+1 query). This is acceptable for Phase 4 at venue scale (≤40 entries per race). Phase 5 forwarder integration may cache this mapping.

## Threat Flags

None — all threats in plan's threat model are mitigated as designed.

## Self-Check: PASSED
