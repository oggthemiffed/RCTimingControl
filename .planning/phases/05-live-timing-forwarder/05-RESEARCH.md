# Phase 5: Live Timing & Forwarder — Research

**Researched:** 2026-04-26
**Domain:** Netty TCP client, gRPC bidirectional streaming, Spring WebSocket/STOMP, forwarder Gradle submodule, in-race transponder linking
**Confidence:** HIGH — primary claims verified against codebase and library versions; protocol claims verified against in-repo docs

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** RC-4 text protocol only in Phase 5. P3 binary deferred to backlog.
- **D-02:** `TimingSource` interface defined cleanly (per TIMING-05); RC-4 implementation only. P3 slots in later with no changes to race control or timing logic.
- **D-03:** `AmbRc4TimingSource` parser is a pure function at its core — parsing a line into a domain event — with no Spring dependencies.
- **D-04:** Simulator fidelity: playback + generative mode. Playback replays files from `RCTimingForwarder/samples/` over TCP. Generative mode emits synthetic PASSING records for a configured set of transponder IDs at configurable intervals.
- **D-05:** Simulator location: separate runnable in the forwarder module (`forwarder/src/main/java/.../simulator/`). Launched via Gradle task (`./gradlew :forwarder:runSimulator`). Real forwarder connects to it identically to a real decoder.
- **D-06:** RC-4 `timeSinceStart_s` relative accuracy is sufficient for RC club timing — lap times are differences between consecutive passings.
- **D-07:** Conversion strategy: record `Instant.now()` as `decoderEpoch` when first PASSING record arrives. `rtcTimeMicros = (decoderEpoch.toEpochMilli() + (timeSinceStart_s - firstTimeSinceStart_s) * 1000) * 1000L`.
- **D-08:** Forwarder authenticates with cloud service using a static API token stored in `forwarder.properties`. Format: `forwarder.api-token=<value>`.
- **D-09:** Token renewal: admin generates/regenerates via admin portal. Token displayed once for copy-paste. No automated rotation in v1.
- **D-10:** All RC-4 PASSING records treated as start/finish passings regardless of loop ID. Loop-aware filtering deferred.
- **D-11:** Unknown transponder cockpit alert already exists (Phase 4). Phase 5 adds "link in-race" action from the cockpit alert.
- **D-12:** Retroactive credit: when unknown transponder linked, all passings from that transponder since race start are retroactively credited to the linked entry. In-memory `LiveRaceState` updated; positions rebroadcast.
- **D-13:** Slim status bar at top of race control cockpit (below main nav). Two pills: `● DECODER connected/disconnected` and `● FORWARDER connected/disconnected`.
- **D-14:** Status updates via new STOMP topic `/topic/system/forwarder-status`. gRPC receiver publishes status changes; frontend subscribes and updates status bar.
- **D-15:** Pill colours: green dot = connected, red dot = disconnected/error, amber dot = reconnecting (using `--flag-green/red/yellow` CSS tokens per UI-SPEC).

### Claude's Discretion

- gRPC protobuf schema design (field names, message types for streaming passing events and status updates)
- Netty channel configuration for the RC-4 TCP client (reconnect backoff, keep-alive)
- WATCHDOG absence threshold for declaring decoder disconnected (suggest 10 seconds — STATUS records arrive every ~5s)
- Exact Gradle task names and simulator CLI flags
- gRPC server port (suggest 9090, configurable)
- REST URL for admin token generation (`POST /api/v1/admin/forwarder/token` or similar)
- How the in-race transponder link endpoint is secured (RACE_DIRECTOR or ADMIN role)

### Deferred Ideas (OUT OF SCOPE)

- P3 binary protocol implementation
- Multi-loop aware filtering (admin config of scoring loop IDs)
- Automated token expiry/rotation
- Multi-decoder operation (TRACK-04 post-v1)
- Audio announcements (Phase 6)
- Results & championship (Phase 7)
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FORWARDER-01 | Separate forwarder app runs on club LAN, connects to AMB decoder, forwards timing data to cloud | Covered by Netty RC-4 TCP client + gRPC streaming design |
| FORWARDER-02 | Forwarder connects to decoder via TCP using AMB P3 binary protocol — **NOTE: CONTEXT.md overrides this to RC-4 text (D-01)** | RC-4 text on port 5100 is the actual implementation target |
| FORWARDER-03 | Forwarder streams decoded timing events to cloud via gRPC bidirectional streaming; cloud can send RESEND requests back | Covered by gRPC bidirectional streaming design — RESEND not applicable to RC-4 (no resend support in text protocol) |
| FORWARDER-04 | Forwarder is a Java Gradle submodule within the same repo, sharing domain model classes with main app | `forwarder/build.gradle.kts` placeholder exists; shared domain via project dependency |
| FORWARDER-05 | Forwarder authenticates with cloud service using pre-configured API token before streaming begins | Static API token in `forwarder.properties`; server validates on gRPC stream connect |
| TIMING-01 | Cloud service receives timing data from forwarder via gRPC; forwarder owns AMB TCP connection and all protocol parsing | `ForwarderGrpcService` → `ApplicationEventPublisher.publishEvent(LapPassingEvent)` path |
| TIMING-02 | Auto-reconnect on connection loss; WATCHDOG absence = lost connection; both connection statuses visible in race control UI | Netty `IdleStateHandler` + `SmartLifecycle` reconnect loop; `/topic/system/forwarder-status` STOMP topic |
| TIMING-03 | Live lap times, positions, gaps in browser during race via WebSocket | Already works via Phase 4 infrastructure; this phase wires the real forwarder as producer |
| TIMING-04 | Lap times use decoder's RTC_TIME, not server receipt time | D-07 conversion strategy anchors decoder epoch at first PASSING record |
| TIMING-05 | `TimingSource` interface; switching protocol requires only new implementation class | `TimingSource` interface definition + `AmbRc4TimingSource` implementation |
| TIMING-06 | FIRST_CONTACT handshake — **NOTE: RC-4 text has NO handshake (docs confirmed); no handshake needed** | Per `docs/AMB_DECODER_PROTOCOL.md`: RC-4 decoder streams immediately on TCP connect |
| TIMING-07 | Monitor PASSING_NUMBER for gaps; trigger RESEND on gaps — **NOTE: RC-4 has no RESEND** | Log seq_num gaps; no RESEND mechanism exists in text protocol; gap logging only |
| TIMING-08 | In-memory transponder→entry map at race start; unknown passings logged and surfaced in race control UI | Already exists in Phase 4; Phase 5 adds in-race linking action with retroactive credit |
</phase_requirements>

---

## Summary

Phase 5 delivers the physical timing integration end-to-end: from the club's AMB decoder hardware, through a local forwarder application, over gRPC to the cloud service, and into the existing `LapPassingEvent` → `LapTimingService` → `LiveTimingHub` → STOMP chain already built in Phase 4. The Phase 4 infrastructure is complete and correct — Phase 5 adds only the upstream producer.

The work is cleanly divided across three domains: (1) the forwarder Gradle submodule with Netty RC-4 TCP client, RC-4 parser, and simulator; (2) the gRPC transport layer binding forwarder to cloud (with authentication and status reporting); (3) cloud-side additions — `ForwarderGrpcService`, the `/topic/system/forwarder-status` STOMP broadcast, the admin token management endpoints and frontend page, and in-race transponder linking with retroactive lap credit.

Two requirements note mismatches with REQUIREMENTS.md: TIMING-06 (FIRST_CONTACT handshake) does not apply to RC-4 text protocol — the decoder streams immediately on TCP connect with no application-level handshake. TIMING-07 (RESEND on gaps) does not apply to RC-4 — there is no RESEND mechanism in the text protocol; detected seq_num gaps are logged only. The FORWARDER-02 requirement says "P3 binary protocol" but CONTEXT.md D-01 locks to RC-4 only — the requirements document reflects the original spec before the decision was made.

**Primary recommendation:** Build the forwarder as a standalone Java 21 Gradle submodule (`:forwarder`) using Netty 4.1.x for RC-4 TCP, gRPC 1.73.0 + grpc-netty-shaded for the cloud stream, and protobuf 3.25.x for wire format. The shared domain model is accessed via a project dependency on the domain classes extracted from `:app`. The simulator runs as a separate Gradle exec task and enables full end-to-end testing without hardware.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| RC-4 TCP decode | Forwarder (on-site JVM) | — | Protocol parsing must run on the club LAN; cloud cannot reach decoder directly (FORWARDER-01) |
| gRPC stream client | Forwarder | — | Forwarder initiates and owns the outbound gRPC stream |
| gRPC stream server | Cloud App (Spring Boot) | — | `ForwarderGrpcService` receives stream and publishes domain events |
| API token authentication | Cloud App (Spring Security filter) | Forwarder (sends token in metadata) | Token issued by admin; forwarder presents it on connect |
| `LapPassingEvent` production | Cloud App (`ForwarderGrpcService`) | — | Publishes to existing `ApplicationEventPublisher` — unchanged downstream |
| In-memory position calculation | Cloud App (`LapTimingService` / `LiveRaceState`) | — | Already built in Phase 4; receives `LapPassingEvent` |
| STOMP broadcast | Cloud App (`LiveTimingHub`) | — | Already built in Phase 4; Phase 5 adds `forwarder-status` topic only |
| Status reporting | Cloud App (`LiveTimingHub` → STOMP) | Forwarder (pushes status over gRPC stream) | Forwarder reports decoder connectivity; cloud app reports forwarder connectivity |
| Admin token CRUD | Cloud App (Spring REST controller) | — | Token lifecycle management in admin panel |
| In-race transponder link | Cloud App (REST endpoint + `LiveRaceState`) | — | Retroactive credit requires access to in-memory `LiveRaceState` |
| Frontend status bar | Browser (React) | — | Subscribes to `/topic/system/forwarder-status` via existing `useStomp` hook |
| Frontend link dialog | Browser (React) | — | Modal action from existing unknown transponder alert |
| Frontend token management | Browser (React admin page) | — | CRUD for admin token lifecycle |

---

## Standard Stack

### Core — Forwarder Module

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `io.netty:netty-all` | `4.1.121.Final` | RC-4 TCP client with `LineBasedFrameDecoder` | Project-mandated (CLAUDE.md); proven NIO event loop; `LineBasedFrameDecoder` perfectly matches RC-4 CRLF-terminated lines [VERIFIED: Maven Central] |
| `io.grpc:grpc-stub` | `1.73.0` | Generated gRPC stub (async, bidirectional streaming) | Latest stable; Java 21 compatible [VERIFIED: Maven Central] |
| `io.grpc:grpc-netty-shaded` | `1.73.0` | gRPC transport (shaded Netty, no version conflict with forwarder's own Netty) | Shaded jar prevents conflict between gRPC's internal Netty and forwarder's Netty 4.1 [VERIFIED: Maven Central] |
| `com.google.protobuf:protobuf-java` | `3.25.8` | Protobuf runtime (message serialization) | Latest stable 3.x; matches protobuf-gradle-plugin codegen [VERIFIED: Maven Central] |
| `com.google.protobuf:protobuf-gradle-plugin` | `0.10.0` | Protoc invocation in Gradle build | Standard Gradle protobuf plugin; generates Java stubs from `.proto` files [VERIFIED: Maven Central] |

### Core — Cloud App (additions to existing `:app`)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `io.grpc:grpc-stub` | `1.73.0` | Generated gRPC service interface (server side) | Same version as forwarder [VERIFIED: Maven Central] |
| `io.grpc:grpc-netty-shaded` | `1.73.0` | gRPC transport on the server side | Shaded jar avoids Spring Boot's Tomcat/Netty class path [VERIFIED: Maven Central] |
| Spring Boot gRPC support | No extra library needed | Spring Boot 3.4.x ships without built-in gRPC server starter | Use `GrpcServerFactory` or `ServerBuilder` programmatically inside a `SmartLifecycle` bean — consistent with how the project runs background TCP via `SmartLifecycle` [ASSUMED] |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `io.netty:netty-handler` | `4.1.121.Final` | `IdleStateHandler` for heartbeat timeout detection | Use in the RC-4 pipeline to detect STATUS record absence > 30s |
| `io.grpc:grpc-protobuf` | `1.73.0` | Protobuf message codec for gRPC | Required alongside `grpc-stub` |
| `io.grpc:grpc-services` | `1.73.0` | Optional health check and reflection services | Use health check endpoint on gRPC server so forwarder can probe cloud availability |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `grpc-netty-shaded` | `grpc-netty` (unshaded) | Unshaded requires careful Netty version alignment between gRPC and the app's Netty; shaded is zero-conflict |
| Custom reconnect loop | `io.grpc:grpc-core` retry policy | gRPC built-in retry policy works for unary calls; for bidirectional streaming a custom reconnect loop (with `SmartLifecycle`) is more controllable |
| `LineBasedFrameDecoder` | Manual buffer accumulation | `LineBasedFrameDecoder` is purpose-built for CRLF line protocols — exactly what RC-4 is |

### Installation (forwarder module)

```bash
# These go in forwarder/build.gradle.kts
plugins {
    id("com.google.protobuf") version "0.10.0"
    java
}

dependencies {
    implementation("io.netty:netty-all:4.1.121.Final")
    implementation("io.grpc:grpc-stub:1.73.0")
    implementation("io.grpc:grpc-netty-shaded:1.73.0")
    implementation("io.grpc:grpc-protobuf:1.73.0")
    implementation("com.google.protobuf:protobuf-java:3.25.8")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")  // required by protoc-generated stubs
    project(":app")  // or extracted shared domain jar
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.25.8" }
    plugins {
        create("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.73.0" }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins { create("grpc") }
        }
    }
}
```

**Version verification (as of 2026-04-26):**
- `io.netty:netty-all` 4.1.121.Final — confirmed [VERIFIED: Maven Central]
- `io.grpc:grpc-stub` 1.73.0 — confirmed [VERIFIED: Maven Central]
- `com.google.protobuf:protobuf-java` 3.25.8 — confirmed latest stable 3.x [VERIFIED: Maven Central]
- `com.google.protobuf:protobuf-gradle-plugin` 0.10.0 — confirmed [VERIFIED: Maven Central]

---

## Architecture Patterns

### System Architecture Diagram

```
Club LAN                         Cloud (Spring Boot :app)
──────────────────               ──────────────────────────────────────
AMB Decoder                      ForwarderGrpcService
  TCP:5100                          @SmartLifecycle gRPC server
     │                              │  validates API token (metadata)
     ▼                              │  receives LapPassing stream
Forwarder (:forwarder submodule)    │  publishes LapPassingEvent
  AmbRc4TimingSource               │                │
    Netty LineBasedFrameDecoder     │                ▼
    Rc4TextParser (pure fn)         │  LapTimingService (@EventListener @Async)
    epoch anchoring (D-07)         │    resolves transponder → entry
    seq gap detection              │    updates LiveRaceState (ConcurrentHashMap)
         │                         │    calculatePositions()
         │ LapPassing proto        │                │
         │ stream (gRPC bidi)      │                ▼
         └──────────────────────►  LiveTimingHub
                                     broadcastTimingUpdate → /topic/race/{id}/timing
                                     broadcastUnknownTransponder → /topic/race/{id}/unknown-transponder
                                     broadcastForwarderStatus → /topic/system/forwarder-status
                                              │
                                              ▼ STOMP over WebSocket
                                   Browser (React)
                                     LiveTimingPanel (already built)
                                     ForwarderStatusBar (new)
                                     UnknownTransponderLinkDialog (new)

Admin portal: POST /api/v1/admin/forwarder/token → ForwarderTokenService → DB (V21 migration)
```

### Recommended Project Structure

```
forwarder/
├── src/main/java/dev/monkeypatch/rctiming/forwarder/
│   ├── ForwarderApplication.java         # main class, standalone JAR
│   ├── config/
│   │   └── ForwarderConfig.java          # @ConfigurationProperties binding forwarder.properties
│   ├── timing/
│   │   ├── TimingSource.java             # interface: start(), stop()
│   │   ├── AmbRc4TimingSource.java       # Netty LineBasedFrameDecoder + reconnect
│   │   └── Rc4TextParser.java            # pure fn: String → Optional<LapPassingEvent>
│   ├── grpc/
│   │   └── ForwarderGrpcClient.java      # manages gRPC stream, sends LapPassing messages
│   └── simulator/
│       ├── FakeDecoderServer.java        # listens on configurable port, emits RC-4 lines
│       ├── PlaybackMode.java             # reads .dump files, replays at configurable speed
│       └── GenerativeMode.java           # synthetic passings for configured transponder list

app/src/main/java/dev/monkeypatch/rctiming/
├── forwarder/
│   ├── ForwarderGrpcService.java         # gRPC server-side service: receives stream → ApplicationEventPublisher
│   ├── ForwarderGrpcServer.java          # SmartLifecycle: starts/stops gRPC server on configurable port
│   ├── ForwarderStatusPublisher.java     # tracks gRPC connection state, publishes to /topic/system/forwarder-status
│   ├── ForwarderToken.java               # Hibernate entity: token hash, status (ACTIVE/REVOKED), created_at
│   ├── ForwarderTokenRepository.java
│   └── ForwarderTokenService.java        # generate (SecureRandom), revoke, validate
├── api/admin/
│   └── ForwarderTokenController.java     # GET/POST /api/v1/admin/forwarder/token
├── api/racecontrol/
│   └── (existing RaceControlController extended or new endpoint)
│       # PATCH /api/v1/race-control/race/{raceId}/unknown-transponder/{transponderId}/link
└── timing/
    └── LiveRaceState.java                # ADD: retroactiveLinkTransponder(transponderNumber, entryId, List<LapPassingEvent>)

app/src/main/resources/
├── db/migration/
│   └── V21__phase5_forwarder_token.sql   # forwarder_token table

frontend/src/
├── components/race-control/
│   └── ForwarderStatusBar.tsx            # new component per UI-SPEC
├── pages/race-control/dialogs/
│   └── UnknownTransponderLinkDialog.tsx  # new dialog per UI-SPEC
└── pages/admin/forwarder/
    └── ForwarderTokenPage.tsx            # new admin page per UI-SPEC

forwarder/src/main/resources/
└── forwarder.properties                  # forwarder.api-token=, forwarder.decoder.host=, forwarder.decoder.port=5100, forwarder.grpc.host=, forwarder.grpc.port=9090

forwarder/src/main/proto/
└── timing.proto                          # LapPassing, ForwarderStatus, TimingService definition
```

### Pattern 1: Netty RC-4 TCP Client with Reconnect

**What:** `AmbRc4TimingSource` implements `TimingSource`. Uses a Netty `Bootstrap` to connect to the decoder. Maintains a `SmartLifecycle` lifecycle. On `READER_IDLE` (30s no data) or channel close, schedules reconnect with exponential backoff.

**When to use:** The single implementation of `TimingSource` for RC-4 text protocol (port 5100, firmware < 4.5).

```java
// Source: Netty 4.1 docs, AMB_DECODER_PROTOCOL.md confirms LineBasedFrameDecoder suitability
public class AmbRc4TimingSource implements TimingSource, SmartLifecycle {

    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private volatile Channel channel;

    @Override
    public void start() {
        connect();
    }

    private void connect() {
        Bootstrap b = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(
                        new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS),  // heartbeat watch
                        new LineBasedFrameDecoder(1024),                   // CRLF frame splitter
                        new StringDecoder(StandardCharsets.US_ASCII),
                        new Rc4InboundHandler(parser, eventPublisher, reconnectCallback)
                    );
                }
            });
        b.connect(host, port).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) { channel = f.channel(); }
            else { scheduleReconnect(); }  // exponential backoff 1s/2s/4s/.../30s cap
        });
    }
}
```

### Pattern 2: Rc4TextParser — Pure Function

**What:** Stateless parser. Takes a `String` line (SOH already stripped by `LineBasedFrameDecoder` handler), returns `Optional<ParsedPassing>`. No Spring dependencies. The epoch anchoring lives in `AmbRc4TimingSource` (stateful), not in the parser.

**When to use:** Only call from `Rc4InboundHandler`. Test in isolation with JUnit 5 — no Spring context needed.

```java
// Source: AMB_DECODER_PROTOCOL.md field reference + existing codebase LapPassingEvent.java
public class Rc4TextParser {
    // Strip SOH (0x01) before calling this method
    public Optional<ParsedPassing> parse(String line) {
        String[] f = line.split("\t");
        if (f.length < 1) return Optional.empty();
        return switch (f[0]) {
            case "@" -> parsePassing(f);
            case "#" -> Optional.empty();   // STATUS — used for heartbeat only
            default  -> Optional.empty();
        };
    }

    private Optional<ParsedPassing> parsePassing(String[] f) {
        if (f.length < 9) return Optional.empty();
        // f[0]='@' f[1]=decoderId f[2]=seqNum f[3]=transponderId
        // f[4]=timeSinceStart_s f[5]=hits f[6]=strength f[7]=passingStatus f[8]=crc
        return Optional.of(new ParsedPassing(
            f[3],                              // transponderNumber (string — matches LapPassingEvent)
            Double.parseDouble(f[4]),          // timeSinceStart_s
            Integer.parseInt(f[2]),            // seqNum (for gap detection)
            Integer.parseInt(f[1])             // decoderId
        ));
    }
}
```

**Key detail:** `LapPassingEvent.transponderNumber` is a `String` in the existing code. RC-4 `transponder_code` is `uint ASCII decimal` — pass directly as string without conversion.

### Pattern 3: Epoch Anchoring (D-07)

**What:** Records `Instant.now()` and the first `timeSinceStart_s` when the first PASSING arrives. Subsequent passings compute `rtcTimeMicros` from the offset.

```java
// Source: D-07 from CONTEXT.md; AMB_DECODER_PROTOCOL.md §Timestamp Handling Option A
private volatile Instant decoderEpoch;
private volatile double firstTimeSinceStart;

long toRtcTimeMicros(double timeSinceStart) {
    if (decoderEpoch == null) {
        decoderEpoch = Instant.now();
        firstTimeSinceStart = timeSinceStart;
    }
    double offsetSeconds = timeSinceStart - firstTimeSinceStart;
    return (decoderEpoch.toEpochMilli() + (long)(offsetSeconds * 1000)) * 1000L;
}
```

**Note:** `LapPassingEvent.raceId` must be supplied by the gRPC receiver on the cloud side, not the forwarder. The forwarder does not know which race is running — it only knows transponder IDs and timestamps. The `ForwarderGrpcService` receives the transponder+timestamp and publishes `LapPassingEvent(raceId, transponderNumber, rtcTimeMicros)` where `raceId` comes from the currently-running race (query from `RaceRepository.findFirstByStatus(RUNNING)`).

### Pattern 4: gRPC Bidirectional Streaming Transport

**What:** Forwarder initiates one long-lived bidirectional gRPC stream to the cloud app on startup. Forwarder sends `LapPassing` messages (transponder + rtcTimeMicros + decoderEpoch context). Cloud sends `ForwarderCommand` messages (currently unused in Phase 5 — no RESEND for RC-4, but the stream exists for future P3 RESEND support).

**Proto schema (Claude's Discretion — recommended design):**

```protobuf
// Source: docs/AMB_DECODER_PROTOCOL.md field reference + CONTEXT.md D-08/D-14
syntax = "proto3";
package dev.monkeypatch.timing;
option java_package = "dev.monkeypatch.rctiming.forwarder.proto";

message LapPassing {
    string transponder_number  = 1;  // matches LapPassingEvent.transponderNumber (string)
    int64  rtc_time_micros     = 2;  // epoch-anchored micros per D-07
    int32  decoder_id          = 3;  // for logging
    int32  seq_num             = 4;  // for gap logging
    int32  signal_strength     = 5;  // optional diagnostic
    int32  hit_count           = 6;  // optional diagnostic
}

message ForwarderCommand {
    oneof command {
        AckConnect ack_connect = 1;  // server acknowledges authenticated connection
    }
}

message AckConnect { bool success = 1; }

message ForwarderStatus {
    enum ConnectionState { UNKNOWN = 0; CONNECTED = 1; RECONNECTING = 2; DISCONNECTED = 3; }
    string component    = 1;  // "DECODER" or "FORWARDER"
    ConnectionState state = 2;
}

service TimingService {
    rpc StreamPassings(stream LapPassing) returns (stream ForwarderCommand);
}
```

**Authentication via gRPC metadata (not in proto body):**
```java
// Forwarder: attach token as metadata on each stream
Metadata headers = new Metadata();
headers.put(Metadata.Key.of("x-forwarder-token", ASCII_STRING_MARSHALLER), token);
ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
    .usePlaintext()  // TLS optional for venue LAN — configurable
    .build();
TimingServiceStub stub = TimingServiceGrpc.newStub(channel)
    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
```

### Pattern 5: ForwarderGrpcServer (SmartLifecycle)

**What:** A `SmartLifecycle` bean in `:app` that starts a gRPC `Server` on a configurable port (default 9090). The server exposes `ForwarderGrpcService`. Validates the API token from `x-forwarder-token` metadata before accepting stream.

```java
// Source: grpc-java ServerBuilder docs + existing SmartLifecycle pattern in CLAUDE.md
@Component
public class ForwarderGrpcServer implements SmartLifecycle {
    private Server server;

    @Override
    public void start() {
        server = ServerBuilder.forPort(grpcPort)
            .addService(forwarderGrpcService)
            .intercept(new ForwarderTokenAuthInterceptor(tokenService))
            .build()
            .start();
    }
    @Override public void stop() { server.shutdown(); }
    @Override public boolean isRunning() { return server != null && !server.isShutdown(); }
}
```

### Pattern 6: In-Race Retroactive Transponder Link (D-12)

**What:** `LiveRaceState.retroactiveLinkTransponder(transponderNumber, entryId)` is called after persisting an `UnknownTransponderLink` record. It scans `lapHistory`, replays all cached `LapPassingEvent`s for that transponder as if they were for the given entry, then rebroadcasts positions.

**Key implementation detail:** `lapHistory` in `LiveRaceState` already stores every `LapPassingEvent` (verified from source). The method needs to iterate `lapHistory`, call `applyLapPassing` for each matching event with the resolved `entryId`, then call `calculatePositions()`.

```java
// Called from new REST endpoint after persisting UnknownTransponderLink
public synchronized List<LiveTimingRowDto> retroactiveLinkTransponder(
        String transponderNumber, long entryId) {
    for (LapPassingEvent event : lapHistory) {
        if (transponderNumber.equals(event.transponderNumber())) {
            applyLapPassing(event, entryId);  // already synchronized
        }
    }
    seenUnknownTransponders.remove(transponderNumber);
    return calculatePositions();
}
```

**The count for the success toast** ("N laps credited") = number of lapHistory entries matching transponderNumber.

### Pattern 7: ForwarderStatusBar React Component

**What:** A new `ForwarderStatusBar.tsx` component inserted in `RaceControlLayout.tsx` between the existing `<Separator />` and `<div className="flex-1 overflow-hidden">`. Uses the existing `useStomp<ForwarderStatusDto>` hook pattern to subscribe to `/topic/system/forwarder-status`.

**Integration point (verified from RaceControlLayout.tsx source):**
```tsx
// Insert between these two existing lines in RaceControlLayout.tsx:
<Separator />
{/* INSERT ForwarderStatusBar here */}
<ForwarderStatusBar eventId={eventId} />
<div className="flex-1 overflow-hidden">
```

The `ForwarderStatusDto` type is separate from the per-race topics — it is not scoped to a raceId; it is system-level.

### Anti-Patterns to Avoid

- **Storing raceId in the forwarder:** The forwarder does not know which race is running. `raceId` is resolved on the cloud side in `ForwarderGrpcService` by querying for the currently RUNNING race. If no race is running, passings are buffered briefly (or dropped with a warning log) — this is acceptable for RC club use.
- **Hibernate in forwarder:** The forwarder module does not load Spring Boot, does not use Hibernate, does not have a DB connection. It is a plain Java process. Any shared domain classes needed are domain value objects, not JPA entities.
- **Blocking gRPC call in Netty I/O thread:** `Rc4InboundHandler.channelRead` must hand off to gRPC asynchronously — never block the Netty event loop thread with a gRPC send. Use `channel.eventLoop().execute(...)` or a separate executor.
- **Resetting decoderEpoch on reconnect:** When the forwarder reconnects to the decoder, the decoder clock counter continues from where it was (it does not reset on client disconnect). Do not reset `firstTimeSinceStart`. Only reset if the decoder itself sends a STATUS record with seq_num=0 (indicating decoder restart).
- **Validating CRC in RC-4 parser:** CRC algorithm is unconfirmed in all open sources. Log CRC values but do not reject records on mismatch. (Per AMB_DECODER_PROTOCOL.md §CRC Format.)

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| CRLF-terminated line framing | Custom byte buffer accumulation | Netty `LineBasedFrameDecoder` | Handles partial frames, backpressure, max line length out of the box |
| NIO socket management and reconnect | `java.nio.SocketChannel` + selector loop | Netty `Bootstrap` + `ChannelFuture` listeners | Thread management, SO_KEEPALIVE, buffer tuning all handled |
| Protobuf wire serialization | Custom binary format | `protobuf-java` + generated code | Versioned, backward-compatible, binary-efficient |
| gRPC stream lifecycle (connect/disconnect/retry) | Custom TCP + custom framing | `grpc-netty-shaded` bidirectional streaming | Flow control, header compression, multiplexing over single TCP connection |
| Heartbeat timeout detection | `ScheduledExecutorService` polling last-seen time | Netty `IdleStateHandler(30, 0, 0, SECONDS)` | `READER_IDLE` event fires reliably; no polling needed |
| Secure token generation | `UUID.randomUUID()` or weak random | `SecureRandom` → Base64URL (or `java.security.SecureRandom`) | Cryptographically random; 32 bytes = 256-bit token |
| Token storage | Plaintext in DB column | BCrypt hash of token in DB | Token behaves like a password — only the hash is stored; plaintext shown once |

**Key insight:** The combination of Netty's decoder pipeline (SOH strip → `LineBasedFrameDecoder` → `StringDecoder` → application handler) eliminates all manual frame parsing state machines. The decoder side is 3 pipeline stages; the application handler only sees complete ASCII lines.

---

## Common Pitfalls

### Pitfall 1: raceId Resolution in ForwarderGrpcService

**What goes wrong:** Passings arrive over gRPC but `LapPassingEvent` requires a `raceId`. If there is no running race, `raceRepository.findFirstByStatus(RUNNING)` returns empty. Passings are silently dropped.

**Why it happens:** The forwarder knows nothing about race state — it just forwards all passings from the decoder, including practice laps and warm-up passings before a race starts.

**How to avoid:** `ForwarderGrpcService.onLapPassing()` should: (1) query for the currently RUNNING race; (2) if found, publish `LapPassingEvent`; (3) if not found, log at DEBUG level (not WARN — this is normal before race start) and discard.

**Warning signs:** Log messages like "Discarding passing — no running race" firing during an actual running race would indicate a `RaceStatus` transition problem from Phase 4.

### Pitfall 2: Decoder Epoch Reset vs Client Reconnect

**What goes wrong:** Forwarder loses TCP connection to decoder and reconnects. `timeSinceStart_s` in the new session may be higher (decoder kept running) or reset to near-zero (decoder was restarted). If code always sets `firstTimeSinceStart` from first record after reconnect, subsequent lap times will be wrong.

**Why it happens:** The decoder `timeSinceStart_s` is time since decoder power-on, not since last client connect. If the decoder was not restarted, the counter continues monotonically.

**How to avoid:** On reconnect, if the first new `timeSinceStart_s` is lower than the last seen value by more than a threshold (e.g., > 10s regression), treat it as a decoder restart and reset the epoch. Otherwise, continue with the existing epoch. Per D-07, the epoch is anchored from the very first PASSING ever received; if the decoder simply continued, the existing epoch remains valid.

**Warning signs:** Lap times suddenly showing as very short (e.g., < 1s) after a reconnect.

### Pitfall 3: Netty Thread Blocked by gRPC Send

**What goes wrong:** The `Rc4InboundHandler.channelRead()` executes on Netty's I/O thread. If the gRPC `stub.onNext()` call blocks (e.g., flow control back-pressure), the Netty I/O thread stalls and no further records are read from the TCP socket.

**Why it happens:** gRPC's async stub `onNext()` is non-blocking in normal operation, but if the gRPC channel is unhealthy or the server is slow, flow control can apply back-pressure.

**How to avoid:** Dispatch to a dedicated executor in `channelRead()`:
```java
grpcExecutor.execute(() -> grpcClient.sendPassing(passing));
```
A single-threaded `Executors.newSingleThreadExecutor()` is sufficient — the order of passings must be preserved.

### Pitfall 4: Multiple Flyway Migrations Competing

**What goes wrong:** The `:forwarder` module is a standalone process with its own main class. It does not run Flyway. The `forwarder_token` table is created by a migration in `:app`. If someone attempts to run forwarder migrations separately, conflicts arise.

**Why it happens:** Phase 5 introduces a new DB migration (V21) in `:app` for `forwarder_token`. The forwarder itself has no DB.

**How to avoid:** The forwarder has no `spring.datasource` config, no Flyway dependency, no Hibernate. Only `:app` touches the database. The V21 migration runs in `:app` on startup.

### Pitfall 5: grpc-netty-shaded vs app's Spring Netty Dependency

**What goes wrong:** If `:app` accidentally gets both `grpc-netty-shaded` and a different version of Netty on the classpath, class loading issues arise.

**Why it happens:** Spring Boot uses Netty for reactive stack components. However, this project uses the servlet stack (Tomcat), so there is no Netty on the `:app` classpath by default. `grpc-netty-shaded` packages its own Netty under a different package namespace — this is why "shaded" is specified.

**How to avoid:** Use `grpc-netty-shaded` exclusively in `:app`. Do not add `io.netty:*` to `:app`. If `netty-codec-http2` appears as a transitive dependency, exclude it — `grpc-netty-shaded` provides this shaded.

### Pitfall 6: SOH Byte Leaks into Parser

**What goes wrong:** Each RC-4 line begins with `0x01` (SOH). If `Rc4TextParser.parse()` receives the raw line including SOH, `f[0]` will be `"\x01#"` or `"\x01@"`, not `"#"` or `"@"`.

**Why it happens:** `LineBasedFrameDecoder` + `StringDecoder` pass the full line including the leading SOH byte.

**How to avoid:** The `Rc4InboundHandler` (not the parser) strips SOH before forwarding to the parser:
```java
String line = msg.startsWith("") ? msg.substring(1) : msg;
Optional<ParsedPassing> result = parser.parse(line);
```
The parser then always receives lines without SOH.

### Pitfall 7: Admin Token Security — Token Visible in Transit

**What goes wrong:** Admin regenerates a token. The token plaintext is returned in the HTTP response body and shown in the UI. If this HTTP traffic is intercepted (no HTTPS on venue LAN), the token is exposed.

**Why it happens:** One-time reveal pattern requires sending plaintext once. HTTPS is not enforced in development config.

**How to avoid:** This is acceptable for v1 on a venue LAN (per design decisions). Add a comment in `ForwarderTokenController` noting that HTTPS should be enabled in production deployment. Do not add more complex mitigation in Phase 5.

---

## Code Examples

### RC-4 Simulator — Generative Mode

```java
// Source: D-04/D-05 from CONTEXT.md; AMB_DECODER_PROTOCOL.md §STATUS and PASSING format
// FakeDecoderServer binds on configurable port, emits lines at configured intervals
public class FakeDecoderServer {
    private final int port;
    private final List<String> transponderIds;  // from config
    private final int intervalMs;               // ms between passings per transponder

    void emitStatus(PrintWriter out, int seqNum) {
        // SOH + # + TAB fields + CRC (CRC can be fake — not validated)
        out.print("#\t20\t" + seqNum + "\t72\t0\txDEAD\r\n");
        out.flush();
    }

    void emitPassing(PrintWriter out, int seqNum, String transponderId, double timeSinceStart) {
        out.print("@\t20\t" + seqNum + "\t" + transponderId
                  + "\t" + String.format("%.3f", timeSinceStart)
                  + "\t300\t130\t2\txDEAD\r\n");
        out.flush();
    }
}
```

### ForwarderStatus STOMP Broadcast

```java
// Source: existing LiveTimingHub pattern (verified from source)
// Add to LiveTimingHub:
public void broadcastForwarderStatus(String component, String state) {
    messagingTemplate.convertAndSend("/topic/system/forwarder-status",
        Map.of("component", component, "state", state));
}
```

### Frontend ForwarderStatusDto Type

```typescript
// New type in lib/raceControlApi.ts or types/forwarder.ts
export interface ForwarderStatusDto {
  component: 'DECODER' | 'FORWARDER';
  state: 'CONNECTED' | 'RECONNECTING' | 'DISCONNECTED';
}
```

### useStomp Usage in ForwarderStatusBar

```tsx
// Source: existing useStomp.ts pattern (verified from source)
// useStomp is parameterised — works identically for new topics
const { data: decoderStatus } = useStomp<ForwarderStatusDto>('/topic/system/forwarder-status');
```

**Note:** The existing `useStomp` hook returns a single `T | null`. Since `ForwarderStatusDto` carries `component` + `state`, the backend should publish two separate messages (one for DECODER state change, one for FORWARDER state change) or publish a composite object with both states. Recommendation: publish a composite:

```java
// ForwarderStatusPublisher: single STOMP message with both states
record ForwarderStatusDto(String decoderState, String forwarderState) {}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| SockJS WebSocket fallback | Native WebSocket only | 2022+ (modern venues) | CLAUDE.md explicitly forbids SockJS — already correct |
| gRPC with unshaded Netty (manual version alignment) | `grpc-netty-shaded` | gRPC 1.0+ | Shaded jar eliminates version conflicts; use always |
| Spring Boot gRPC starter (third-party) | Programmatic `ServerBuilder` in `SmartLifecycle` | N/A | No official Spring Boot gRPC starter; manual lifecycle is the correct approach |
| protobuf 4.x (RC) | protobuf 3.25.x (stable) | proto4 is still RC | Use 3.25.x for stability; 3.x is fully compatible with gRPC 1.73 |

**Deprecated / outdated:**
- `grpc-all` artifact: monolithic artifact including all transports. Use specific artifacts (`grpc-stub`, `grpc-netty-shaded`, `grpc-protobuf`) instead.
- `io.netty:netty-all` in `forwarder`: acceptable for simplicity (single artifact), but `netty-transport` + `netty-codec` + `netty-handler` is leaner. Either works.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Spring Boot 3.4.x has no built-in gRPC server starter — use programmatic `ServerBuilder` in a `SmartLifecycle` bean | Standard Stack | Low: if a third-party spring-boot-grpc-starter exists and is preferred, it wraps the same `ServerBuilder` pattern anyway |
| A2 | gRPC 1.73.0 is compatible with Java 21 (LTS) | Standard Stack | Low: gRPC has supported Java 11+ since 1.40; Java 21 is a superset |
| A3 | `raceId` resolution from `RaceRepository.findFirstByStatus(RUNNING)` is sufficient — only one race runs at a time at a single-club deployment | Architecture Patterns | Medium: if club runs two classes simultaneously on separate tracks, two races could be RUNNING concurrently. Phase 5 scope note: multi-decoder is post-v1. For now, query could be scoped by decoder ID if needed, but single-track assumption is reasonable for v1 |
| A4 | The STOMP `ForwarderStatusDto` should be a composite object (both DECODER and FORWARDER states in one message) rather than two separate per-component messages | Code Examples | Low: either approach works; composite simplifies frontend state management |
| A5 | `plaintext()` (no TLS) gRPC is acceptable for forwarder→cloud on a venue LAN | Architecture Patterns | Low: acceptable for v1; HTTPS and TLS should be added before public cloud deployment |

---

## Open Questions

1. **Shared domain classes: project dependency vs extracted JAR**
   - What we know: `:forwarder` needs `LapPassingEvent` and `ParsedPassing` (new). `LapPassingEvent` is currently in `app/src/.../timing/`.
   - What's unclear: Can `forwarder` take a `project(":app")` dependency without pulling in Spring Boot, Hibernate, and the full app classpath? Likely not — `:app` declares `spring-boot-starter-*` dependencies.
   - Recommendation: Extract shared types (`LapPassingEvent`, `ParsedPassing`) into a new `:shared` or `:domain` submodule, OR (simpler for Phase 5) copy/inline the 3 fields of `LapPassingEvent` into a separate `ForwarderLapEvent` record in the forwarder module and map at the gRPC boundary. The gRPC proto message serves as the wire contract; no shared Java class is strictly needed between forwarder and app.

2. **gRPC port conflict with app's 8080**
   - What we know: gRPC server will run on port 9090 (Claude's Discretion). Spring Boot runs on 8080.
   - What's unclear: Whether the `docker-compose.yml` needs updating to expose port 9090.
   - Recommendation: Add `9090:9090` mapping to `docker-compose.yml` in the app service, or run gRPC server on a configurable port via `app.grpc.port=9090`.

3. **Sample capture files location**
   - What we know: `docs/AMB_DECODER_PROTOCOL.md` references `oggthemiffed/RCTimingForwarder/samples/dump_amb_ip.dump` as the source of real protocol captures.
   - What's unclear: These files are not present in the current repository (`find` returned nothing). They exist in a separate Go prototype repository.
   - Recommendation: As part of Wave 0 (simulator setup), either (a) copy the sample `.dump` files into `forwarder/src/main/resources/samples/` or (b) create synthetic `.dump` files that match the known format from the inline examples in `AMB_DECODER_PROTOCOL.md`. The annotated samples in that file provide enough data for simulator playback.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 JDK | Compilation, runtime | Yes | 21.0.10 Temurin LTS | — |
| Gradle 8.14.2 | Build | Yes | 8.14.2 | — |
| Docker | jOOQ codegen (existing), Testcontainers tests | Yes | 29.4.1 | — |
| gRPC protoc compiler | Proto compilation (downloaded by plugin) | Via Gradle plugin `com.google.protobuf:protoc:3.25.8` | Downloaded on build | — |
| AMB decoder hardware | Real-world RC-4 TCP data | Not available (club hardware) | — | TCP simulator (D-04/D-05) covers all dev/test scenarios |
| Sample `.dump` files | Simulator playback mode | Not in repo | — | Synthetic `.dump` from AMB_DECODER_PROTOCOL.md annotated examples |

**Missing dependencies with no fallback:** None — all blocking dependencies are available.

**Missing dependencies with fallback:**
- AMB decoder hardware: Simulator covers all test scenarios including full end-to-end gRPC path.
- Sample `.dump` files: Inline annotated examples in `AMB_DECODER_PROTOCOL.md` provide enough real-world data to create synthetic playback files.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito + Testcontainers (backend); Vitest + React Testing Library (frontend) |
| Config file | `app/src/test/` — AbstractIntegrationTest base class; `frontend/vitest.config.ts` |
| Quick run command (backend) | `./gradlew :app:test --tests "*.timing.*" --tests "*.forwarder.*"` |
| Full suite command | `./gradlew :app:test :forwarder:test && cd frontend && npm run test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FORWARDER-01 | Forwarder connects to decoder simulator and parses RC-4 lines | Integration | `./gradlew :forwarder:test --tests "*.AmbRc4TimingSourceIT"` | Wave 0 |
| FORWARDER-03 | gRPC stream delivers LapPassing to cloud service | Integration | `./gradlew :app:test --tests "*.ForwarderGrpcServiceIT"` | Wave 0 |
| FORWARDER-05 | Token validation rejects missing/invalid tokens | Unit | `./gradlew :app:test --tests "*.ForwarderTokenServiceTest"` | Wave 0 |
| TIMING-02 | STATUS absence triggers reconnect | Unit | `./gradlew :forwarder:test --tests "*.ReconnectBehaviourTest"` | Wave 0 |
| TIMING-04 | RC-4 timeSinceStart → rtcTimeMicros epoch conversion | Unit | `./gradlew :forwarder:test --tests "*.EpochAnchorTest"` | Wave 0 |
| TIMING-05 | TimingSource interface contract | Unit | `./gradlew :forwarder:test --tests "*.TimingSourceTest"` | Wave 0 |
| TIMING-06 | RC-4 connection (no handshake required — verified) | N/A | No test needed — decoder streams immediately on connect per protocol docs | — |
| TIMING-07 | Seq_num gap detection logged | Unit | `./gradlew :forwarder:test --tests "*.GapDetectionTest"` | Wave 0 |
| TIMING-08 | Retroactive transponder link credits passings | Unit | `./gradlew :app:test --tests "*.LiveRaceStateRetroactiveTest"` | Wave 0 |
| FORWARDER-02 | RC-4 parser handles all documented line formats | Unit | `./gradlew :forwarder:test --tests "*.Rc4TextParserTest"` | Wave 0 |
| RC-4 parser | Parse STATUS, PASSING, unknown, malformed | Unit | `./gradlew :forwarder:test --tests "*.Rc4TextParserTest"` | Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew :forwarder:test --tests "*Parser*" :app:test --tests "*.timing.*"`
- **Per wave merge:** `./gradlew :app:test :forwarder:test && cd frontend && npm run test`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps

- [ ] `forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/Rc4TextParserTest.java` — covers FORWARDER-02, TIMING-07
- [ ] `forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/EpochAnchorTest.java` — covers TIMING-04
- [ ] `forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/GapDetectionTest.java` — covers TIMING-07
- [ ] `forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/AmbRc4TimingSourceIT.java` — requires simulator running on loopback (no Spring context)
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderGrpcServiceIT.java` — covers FORWARDER-03, TIMING-01
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenServiceTest.java` — covers FORWARDER-05
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/timing/LiveRaceStateRetroactiveTest.java` — covers TIMING-08/D-12

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | Yes | API token via HTTP metadata header; BCrypt hash stored in DB |
| V3 Session Management | No | Stateless gRPC stream; token validated per-stream |
| V4 Access Control | Yes | `ForwarderTokenController` requires ADMIN role; in-race link requires RACE_DIRECTOR or ADMIN |
| V5 Input Validation | Yes | Proto schema validation for gRPC; standard Hibernate validation for REST endpoints |
| V6 Cryptography | Yes | Token generation via `SecureRandom` (256-bit); token hash via BCrypt — do NOT use SHA-256 plain |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Forwarder impersonation (invalid API token) | Spoofing | Token validation in `ForwarderTokenAuthInterceptor` before stream accepted; gRPC returns `UNAUTHENTICATED` |
| Token replay after revocation | Spoofing | `ForwarderTokenService.validate()` checks DB status — revoked tokens rejected immediately |
| Man-in-the-middle on venue LAN | Tampering | Acceptable risk for v1 LAN deployment; note TLS upgrade path in deployment docs |
| Race director links wrong transponder retroactively | Tampering | `UnknownTransponderLink` audit record persisted with actor userId; action is reversible by admin |
| gRPC server DoS (many unauth connections) | Denial of Service | gRPC's built-in flow control; auth interceptor rejects unauthenticated streams before any business logic runs |

---

## Sources

### Primary (HIGH confidence)
- `docs/AMB_DECODER_PROTOCOL.md` [VERIFIED: in-repo] — RC-4 text protocol: field reference, line format, SOH prefix, STATUS/PASSING layout, CRC non-validation, no FIRST_CONTACT in RC-4, no RESEND in RC-4, port 5100, heartbeat every 5s
- `app/src/main/java/dev/monkeypatch/rctiming/timing/LapPassingEvent.java` [VERIFIED: codebase] — exact field names: `raceId`, `transponderNumber` (String), `rtcTimeMicros` (long)
- `app/src/main/java/dev/monkeypatch/rctiming/timing/LiveRaceState.java` [VERIFIED: codebase] — `lapHistory` list exists; `applyLapPassing` signature; `seenUnknownTransponders` set
- `app/src/main/java/dev/monkeypatch/rctiming/timing/LiveTimingHub.java` [VERIFIED: codebase] — existing broadcast methods and STOMP topics; `broadcastUnknownTransponder` exists
- `app/src/main/java/dev/monkeypatch/rctiming/timing/LapTimingService.java` [VERIFIED: codebase] — `ApplicationEventPublisher` injection pattern; `@EventListener @Async` pattern
- `frontend/src/hooks/race-control/useStomp.ts` [VERIFIED: codebase] — hook signature, `@stomp/stompjs` usage, Auth header pattern
- `frontend/src/pages/race-control/RaceControlLayout.tsx` [VERIFIED: codebase] — exact insertion point for `ForwarderStatusBar`
- `forwarder/build.gradle.kts` [VERIFIED: codebase] — currently `plugins { java }` placeholder
- Maven Central: `io.netty:netty-all` 4.1.121.Final [VERIFIED: Maven Central 2026-04-26]
- Maven Central: `io.grpc:grpc-stub` 1.73.0 [VERIFIED: Maven Central 2026-04-26]
- Maven Central: `io.grpc:grpc-netty-shaded` 1.73.0 [VERIFIED: Maven Central 2026-04-26]
- Maven Central: `com.google.protobuf:protobuf-java` 3.25.8 [VERIFIED: Maven Central 2026-04-26]
- Maven Central: `com.google.protobuf:protobuf-gradle-plugin` 0.10.0 [VERIFIED: Maven Central 2026-04-26]

### Secondary (MEDIUM confidence)
- `05-CONTEXT.md` decisions D-01 through D-15 — user-locked design decisions [CITED: .planning/phases/05-live-timing-forwarder/05-CONTEXT.md]
- `05-UI-SPEC.md` — component inventory confirms all required shadcn components are pre-installed [CITED: .planning/phases/05-live-timing-forwarder/05-UI-SPEC.md]
- `app/build.gradle.kts` — confirms existing Spring Boot 3.4.7 / jOOQ 3.19.24 / testcontainers 1.21.3 versions [VERIFIED: codebase]

### Tertiary (LOW confidence)
- Spring Boot 3.4.x gRPC integration via programmatic `ServerBuilder` in `SmartLifecycle` — standard pattern but not verified against Spring Boot 3.4 release notes [ASSUMED A1]

---

## Metadata

**Confidence breakdown:**
- Standard stack (library versions): HIGH — all versions verified against Maven Central on 2026-04-26
- Architecture patterns: HIGH — directly derived from existing Phase 4 codebase (sources read and confirmed)
- Protocol handling: HIGH — based on in-repo `AMB_DECODER_PROTOCOL.md` which documents confirmed captures
- Pitfalls: HIGH — derived from direct code inspection of existing Phase 4 implementations
- gRPC integration pattern: MEDIUM — standard industry pattern; Spring Boot gRPC starter assumption is LOW

**Research date:** 2026-04-26
**Valid until:** 2026-05-26 (library versions stable; protocol docs do not change)
