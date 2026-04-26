---
phase: 05-live-timing-forwarder
plan: "04"
subsystem: forwarder-grpc-infrastructure
tags: [grpc, protobuf, live-timing, forwarder, retroactive-link, spring-lifecycle]
dependency_graph:
  requires: [05-02, 05-03]
  provides: [grpc-server-cloud-side, retroactive-transponder-linking, forwarder-grpc-client]
  affects: [timing-pipeline, stomp-broadcasts, race-control-api]
tech_stack:
  added:
    - "io.grpc:grpc-stub:1.73.0"
    - "io.grpc:grpc-netty-shaded:1.73.0"
    - "io.grpc:grpc-protobuf:1.73.0"
    - "com.google.protobuf:protobuf-java:3.25.8"
    - "com.google.protobuf:protoc-gen-grpc-java:1.73.0 (protoc plugin)"
  patterns:
    - "SmartLifecycle gRPC server bean"
    - "gRPC ServerInterceptor for token auth"
    - "ApplicationEventPublisher from gRPC thread"
    - "synchronized retroactive position replay"
    - "gRPC ClientInterceptor for metadata injection"
key_files:
  created:
    - "app/src/main/proto/timing.proto"
    - "app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderGrpcServer.java"
    - "app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenAuthInterceptor.java"
    - "app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderGrpcService.java"
    - "app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderStatusPublisher.java"
    - "app/src/main/java/dev/monkeypatch/rctiming/forwarder/dto/ForwarderStatusDto.java"
    - "app/src/main/java/dev/monkeypatch/rctiming/forwarder/UnknownTransponderLinkAudit.java"
    - "app/src/main/java/dev/monkeypatch/rctiming/forwarder/UnknownTransponderLinkAuditRepository.java"
    - "app/src/main/java/dev/monkeypatch/rctiming/forwarder/dto/LinkTransponderRequestDto.java"
    - "app/src/main/resources/db/migration/V22__create_unknown_transponder_link.sql"
    - "app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/TransponderLinkController.java"
    - "forwarder/src/main/java/dev/monkeypatch/rctiming/forwarder/grpc/ForwarderGrpcClient.java"
  modified:
    - "app/build.gradle.kts (protobuf plugin + gRPC deps)"
    - "app/src/main/resources/application.yml (app.grpc.port: 9090)"
    - "app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceRepository.java (findFirstByStatus)"
    - "app/src/main/java/dev/monkeypatch/rctiming/timing/LiveRaceState.java (retroactiveLinkTransponder)"
    - "app/src/main/java/dev/monkeypatch/rctiming/timing/LapTimingService.java (linkTransponder)"
    - "app/src/main/java/dev/monkeypatch/rctiming/timing/LiveTimingHub.java (broadcastForwarderStatus)"
    - "app/src/test/java/dev/monkeypatch/rctiming/AbstractIntegrationTest.java (grpc.port=0)"
    - "forwarder/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderApplication.java (full wiring)"
    - "docker-compose.yml (9090:9090 port)"
decisions:
  - "UnknownTransponderLinkAudit entity created in forwarder package (table: unknown_transponder_link singular), separate from domain.race.UnknownTransponderLink (table: unknown_transponder_links plural from V18) to preserve CTRL-06 vs TIMING-08 separation"
  - "AbstractIntegrationTest uses app.grpc.port=0 to prevent port conflicts when multiple Spring test contexts start ForwarderGrpcServer simultaneously"
  - "retroactiveLinkTransponder uses applyPositionUpdate (not applyLapPassing) to avoid ConcurrentModificationException when iterating lapHistory"
metrics:
  duration: "~27m"
  completed_date: "2026-04-26"
  tasks_completed: 3
  files_created: 12
  files_modified: 9
---

# Phase 05 Plan 04: Cloud-side gRPC Infrastructure + Retroactive Transponder Linking Summary

**One-liner:** gRPC server (SmartLifecycle, port 9090) with token auth interceptor + LapPassingEvent publishing + retroactive transponder credit via synchronized lapHistory replay + ForwarderGrpcClient wiring forwarder end-to-end.

## What Was Built

### Task 1: gRPC Server Infrastructure

**app/build.gradle.kts** — Added protobuf-gradle-plugin 0.9.4 + grpc-stub, grpc-netty-shaded, grpc-protobuf 1.73.0 + protobuf-java 3.25.8 + javax.annotation-api compile-only. Added `protobuf {}` config block for gRPC stub generation.

**app/src/main/proto/timing.proto** — Identical to forwarder proto: `LapPassing`, `ForwarderCommand`, `AckConnect`, `ForwarderStatus`, and `TimingService.StreamPassings` RPC. Both modules compile stubs independently.

**app/src/main/resources/application.yml** — Added `app.grpc.port: 9090`.

**ForwarderTokenAuthInterceptor** — `ServerInterceptor` that validates `x-forwarder-token` metadata against `ForwarderTokenService.validate()`. Closes stream with `Status.UNAUTHENTICATED` before any business logic runs (T-05-14, T-05-15).

**ForwarderGrpcService** — Extends `TimingServiceGrpc.TimingServiceImplBase`. `streamPassings()`: calls `statusPublisher.onForwarderConnected()`, returns `StreamObserver<LapPassing>` that queries `raceRepository.findFirstByStatus(RUNNING)` and publishes `LapPassingEvent`, or logs at DEBUG when no running race. `onCompleted()`/`onError()` call `statusPublisher.onForwarderDisconnected()`.

**ForwarderStatusPublisher** — Broadcasts `ForwarderStatusDto` to LiveTimingHub. Methods: `onForwarderConnected()`, `onForwarderDisconnected()`, `onDecoderReconnecting()`.

**ForwarderGrpcServer** — `SmartLifecycle` bean. `start()` builds gRPC server with `ServerBuilder.forPort(grpcPort).addService(grpcService).intercept(authInterceptor)`. `stop()` calls `server.shutdown()`.

**LiveTimingHub.broadcastForwarderStatus()** — Sends `ForwarderStatusDto` to `/topic/system/forwarder-status`.

**ForwarderGrpcServiceIT** — 6 integration tests (all passing):
- `streamWithValidTokenAccepted()` — valid token, no UNAUTHENTICATED error within 1s
- `streamWithMissingTokenRejectedUnauthenticated()` — no token → UNAUTHENTICATED
- `streamWithRevokedTokenRejectedUnauthenticated()` — revoked token → UNAUTHENTICATED
- `lapPassingMessagePublishedAsApplicationEvent()` — no running race → no event
- `lapPassingDroppedWhenNoRunningRace()` — empty → no event
- `lapPassingResolvesRaceIdFromCurrentlyRunningRace()` — mock race, verify raceId in event

### Task 2: Retroactive Transponder Linking

**LiveRaceState.retroactiveLinkTransponder()** — Synchronized; iterates `lapHistory`, calls `applyPositionUpdate()` (not `applyLapPassing()` — avoids ConcurrentModificationException) for each matching transponder, removes from `seenUnknownTransponders`, returns `calculatePositions()`.

**LiveRaceState.countPassingsForTransponder()** — Returns count of lapHistory entries for a transponder. Used by controller to report `lapsCredited`.

**LiveRaceState.applyPositionUpdate()** — Private helper: updates position state without adding to lapHistory.

**LapTimingService.linkTransponder()** — Delegates to `LiveRaceState.retroactiveLinkTransponder()` and broadcasts via `liveTimingHub.broadcastTimingUpdate()`.

**V22__create_unknown_transponder_link.sql** — `unknown_transponder_link` table (singular; distinct from V18 `unknown_transponder_links`) with FK refs to races, entries, users. Audit table for retroactive link operations.

**TransponderLinkController** — POST `/api/v1/race-control/races/{raceId}/transponders/link`. `@PreAuthorize("hasAnyRole('RACE_DIRECTOR', 'ADMIN')")` (T-05-18). Counts passings, persists `UnknownTransponderLinkAudit` (T-05-16), calls `lapTimingService.linkTransponder()`, returns `{lapsCredited: N}`.

**LiveRaceStateRetroactiveTest** — 6 unit tests (all passing): credits, removes from unknown set, recalculates positions, noop on empty, preserves history order, count passings.

### Task 3: ForwarderGrpcClient + ForwarderApplication Wiring

**ForwarderGrpcClient** — Plain Java class. `connect()`: builds `ManagedChannel`, attaches `x-forwarder-token` via `ClientInterceptor`, calls `stub.streamPassings(responseObserver)`. `sendPassing()`: converts `EpochCorrectedPassing` → `LapPassing` proto and calls `requestObserver.onNext()`. `scheduleReconnect()`: daemon thread with 5s delay. `close()`: onCompleted + channel.shutdown().

**ForwarderApplication** — Full wiring: `ForwarderGrpcClient.fromConfig(cfg)` → `grpcClient.connect()` → `new AmbRc4TimingSource(host, port, grpcClient::sendPassing, stateLogger)` → `source.start()`. Shutdown hook closes both source and gRPC client.

**docker-compose.yml** — Added app service with ports 8080:8080 and 9090:9090.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] ConcurrentModificationException in retroactiveLinkTransponder**
- **Found during:** Task 2 test execution
- **Issue:** `retroactiveLinkTransponder` called `applyLapPassing()` while iterating `lapHistory`. `applyLapPassing()` adds to `lapHistory`, causing `ConcurrentModificationException`.
- **Fix:** Created private `applyPositionUpdate()` helper that updates position state WITHOUT adding to lapHistory. `retroactiveLinkTransponder` calls this instead.
- **Files modified:** `LiveRaceState.java`
- **Commit:** bbd1aec

**2. [Rule 1 - Bug] Port conflict: ForwarderGrpcServer binds port 9090 in ALL Spring test contexts**
- **Found during:** Task 1 full test suite run
- **Issue:** Multiple Spring test contexts (AbstractIntegrationTest shared context + ClubLogoUploadIT separate context due to @DynamicPropertySource for MinIO) each start `ForwarderGrpcServer` on port 9090 simultaneously. Second context fails to bind.
- **Fix:** Added `@TestPropertySource(properties = "app.grpc.port=0")` to `AbstractIntegrationTest`. Port 0 = OS assigns a random available port, no conflicts.
- **Files modified:** `AbstractIntegrationTest.java`
- **Commit:** 6b5390b

### Architectural Decisions (Deviations)

**3. [Deviation - Pre-existing entity reuse] UnknownTransponderLink naming**
- **Found during:** Task 2 implementation
- **Issue:** Plan specified `forwarder.UnknownTransponderLink` entity + V22 table. However, `domain.race.UnknownTransponderLink` (table: `unknown_transponder_links` plural) already exists from Phase 4 (V18 migration) for CTRL-06 in-race transponder registration.
- **Decision:** Created `forwarder.UnknownTransponderLinkAudit` (table: `unknown_transponder_link` singular) as the V22 entity, keeping it separate from the Phase 4 CTRL-06 entity. `TransponderLinkController` uses the new entity. Both tables coexist with different purposes.
- **Files modified:** `UnknownTransponderLinkAudit.java`, `V22__create_unknown_transponder_link.sql`, `TransponderLinkController.java`

## Pre-existing Issues (Out of Scope)

| Issue | File | Status |
|-------|------|--------|
| `RoundGeneratorServiceTest.heatSplit_fifteenDriversMaxEightPerHeat_createsTwoHeats` fails with `UnnecessaryStubbingException` | `RoundGeneratorServiceTest.java` | Pre-existing — unnecessary Mockito stub in Phase 04 test. Unrelated to Phase 05 changes. |

## Threat Model Review

All mitigations from the plan's threat register were applied:
- T-05-14: ForwarderTokenAuthInterceptor rejects invalid/absent token ✓
- T-05-15: tokenService.validate() only matches ACTIVE tokens ✓
- T-05-16: UnknownTransponderLinkAudit persists actor userId, raceId, entryId, linkedAt ✓
- T-05-17: Auth check in interceptor before StreamObserver allocates resources ✓
- T-05-18: @PreAuthorize on TransponderLinkController ✓
- T-05-19: Accepted (transponder IDs already broadcast in Phase 4) ✓
- T-05-20: Accepted (LAN deployment, Javadoc notes TLS upgrade path) ✓

## Threat Flags

No new security surface introduced beyond the plan's threat model.

## Self-Check

### Created files exist:
- [x] `app/src/main/proto/timing.proto`
- [x] `app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderGrpcServer.java`
- [x] `app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenAuthInterceptor.java`
- [x] `app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderGrpcService.java`
- [x] `app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderStatusPublisher.java`
- [x] `app/src/main/java/dev/monkeypatch/rctiming/forwarder/dto/ForwarderStatusDto.java`
- [x] `app/src/main/java/dev/monkeypatch/rctiming/forwarder/UnknownTransponderLinkAudit.java`
- [x] `app/src/main/resources/db/migration/V22__create_unknown_transponder_link.sql`
- [x] `app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/TransponderLinkController.java`
- [x] `forwarder/src/main/java/dev/monkeypatch/rctiming/forwarder/grpc/ForwarderGrpcClient.java`

### Commits exist:
- [x] 6b5390b — Task 1: gRPC server infrastructure
- [x] bbd1aec — Task 2: Retroactive transponder linking
- [x] d8a11e4 — Task 3: ForwarderGrpcClient + Application wiring

### Known stubs: None — all planned functionality fully implemented.
