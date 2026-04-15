# Architecture Patterns

**Domain:** RC club timing and management system
**Researched:** 2026-04-15
**Confidence:** HIGH for structure and patterns; MEDIUM for AMB/MyLaps protocol specifics (proprietary)

---

## Recommended Architecture

**Modular monolith** — one Spring Boot process, multiple internal packages with enforced
boundaries. Single WAR/JAR, single database, single deployment. No inter-service network
calls, no distributed transaction complexity. Right-sized for single-club use.

Microservices add operational overhead (separate deployments, inter-service auth, distributed
tracing) that a single-club installation does not justify. A clean modular monolith can be
split later if scale demands it; starting as microservices cannot be unsplit.

---

## Component Boundaries

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Spring Boot Application                       │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────────┐  │
│  │ Racer Portal │  │ Admin Panel  │  │   Race Control Client   │  │
│  │  (REST API)  │  │  (REST API)  │  │      (REST + WS)        │  │
│  └──────┬───────┘  └──────┬───────┘  └────────────┬────────────┘  │
│         │                 │                         │               │
│         └─────────────────┴─────────────────────────┘              │
│                                 │                                   │
│                    ┌────────────▼────────────┐                     │
│                    │      Domain Core         │                     │
│                    │  (Racers, Cars,           │                     │
│                    │   Events, Races,          │                     │
│                    │   RaceState, Results,     │                     │
│                    │   Championship)           │                     │
│                    └────────────┬────────────┘                     │
│                                 │                                   │
│          ┌──────────────────────┼──────────────────────┐           │
│          │                      │                       │           │
│  ┌───────▼──────┐   ┌──────────▼─────────┐  ┌────────▼────────┐  │
│  │  Persistence  │   │  Live Timing Hub   │  │  TCP Receiver   │  │
│  │  (JPA/SQL)    │   │  (WebSocket/STOMP) │  │  (AMB/MyLaps)   │  │
│  └───────────────┘   └────────────────────┘  └─────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
         │                       │                       │
    PostgreSQL           Browser clients         AMB/MyLaps
    (or H2 for dev)     (racer portal,           decoder
                         admin, results,          (TCP)
                         live timing,
                         race control)
```

### Component Responsibilities

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| **Racer Portal API** | Profile, cars, transponders, event entry CRUD | Domain Core, Persistence |
| **Admin Panel API** | Event/championship creation and configuration | Domain Core, Persistence |
| **Race Control API** | Race lifecycle commands, marshal laps, grid calls | Domain Core, RaceStateMachine, LiveTimingHub |
| **Domain Core** | Business logic, aggregates, domain events | Persistence (via repositories) |
| **Race State Machine** | Enforces race lifecycle transitions | Domain Core, LiveTimingHub |
| **TCP Receiver** | Connects to decoder, parses MyLaps protocol, emits `LapPassingEvent`s | Domain Core (via application service), LiveTimingHub |
| **Live Timing Hub** | Broadcasts real-time updates to browser clients over WebSocket | Connected browsers |
| **Persistence** | JPA repositories, schema migrations (Flyway) | PostgreSQL |

---

## Data Flow: TCP Decoder to Live Display

```
AMB/MyLaps Decoder
       │
       │ TCP byte stream (proprietary binary or ASCII protocol)
       ▼
  TCP Receiver (background component — see below)
       │
       │ parsed LapPassingEvent { transponderId, passTime, loopId }
       ▼
  LapTimingService (domain application service)
       │ ├─ looks up transponder → car → racer
       │ ├─ calculates lap time (passTime - previous passTime for this transponder)
       │ ├─ assigns lap number
       │ ├─ persists LapTime record
       │ └─ updates race position order
       │
       │ LapRecordedEvent { racerId, carNumber, lapTime, lapNumber, position }
       ▼
  LiveTimingHub (WebSocket/STOMP broker)
       │
       │ STOMP message pushed to topic /topic/race/{raceId}/timing
       ▼
  Browser clients (race control, public live timing display)
```

This is a push-only flow from decoder to browser. The browser never writes back through
this path — race commands (start, stop, marshal lap) travel via separate REST calls that
may trigger state changes published through the same WebSocket topics.

---

## TCP Receiver: Structural Decision

**Recommendation: implement as a managed Spring component (not a separate process).**

Use a dedicated `@Component` that owns a single background thread (or a small fixed thread
pool via `ThreadPoolTaskExecutor`) that blocks on the TCP socket read loop. Spring manages
the lifecycle — the component connects on startup, reconnects on disconnect, and shuts down
cleanly on application stop.

**Why not a separate microservice:**
- Adds deployment complexity for no gain at single-club scale
- Requires inter-service messaging (Kafka, RabbitMQ) that becomes a new failure mode
- A single background thread inside the monolith is straightforward and observable

**Why not a raw `Thread`:**
- Spring's `ThreadPoolTaskExecutor` or `@Async` with a dedicated executor gives managed
  shutdown, exception handling hooks, and visibility in Actuator metrics
- Prefer `ApplicationRunner` or `SmartLifecycle` to start the TCP loop after the full
  Spring context is ready

**Reconnect strategy:**
The decoder is physical hardware that can reboot. The TCP receiver must implement
exponential-backoff reconnect (e.g., 1 s → 2 s → 4 s → max 30 s). Log each
connection attempt. Expose connection state via a health indicator (`HealthIndicator`)
so the race control UI can show "decoder connected / disconnected."

**AMB/MyLaps protocol:**
The MyLaps RC4 / AMB decoder family uses a binary TCP protocol. The exact frame format
is not public, but well-known in the community through reverse-engineering:
- Each passing is a fixed-width binary record: transponder ID (4 bytes), timestamp
  (milliseconds since epoch or epoch of the session), loop/antenna ID (1–2 bytes),
  and a checksum.
- The decoder sends passings in real time as they occur; the client does not need to poll.
- Session initialisation messages precede the passing stream; the receiver must handle
  these to synchronise the clock reference.
CONFIDENCE: MEDIUM — verify against actual decoder documentation or open-source
implementations (e.g., RMonitor protocol parsers, existing Java MyLaps libraries).

---

## Real-Time Architecture: WebSocket with STOMP

**Recommendation: Spring WebSocket with STOMP over SockJS.**

Spring Boot's `spring-boot-starter-websocket` provides first-class STOMP support.
STOMP gives publish/subscribe semantics on top of WebSocket: the server broadcasts to
topics, clients subscribe to topics. This maps cleanly onto race timing needs.

**Topic structure:**
```
/topic/race/{raceId}/timing       — lap passings, positions, gaps in real time
/topic/race/{raceId}/state        — race state changes (PENDING → GRID → RUNNING → FINISHED)
/topic/race/{raceId}/marshal      — marshal lap adjustments (so all displays stay in sync)
/topic/event/{eventId}/schedule   — schedule updates (race order changes)
```

**Why STOMP over raw WebSocket:**
- STOMP gives topic routing built-in; raw WebSocket requires implementing your own
  message routing protocol
- Spring Security integrates with STOMP; securing raw WebSocket is manual
- SockJS fallback (long-polling) handles environments where WebSocket is blocked by
  corporate proxies — relevant since the race control client may be on a venue network

**Why not SSE (Server-Sent Events):**
- SSE is unidirectional (server → client only); race control sends commands back to server,
  which requires a separate connection
- STOMP over WebSocket is bidirectional; the race control client can send grid confirmations
  or marshal requests over the same connection
- SSE is simpler, but the race control use case needs bidirectionality

**Why not polling:**
- With lap passings arriving every few seconds per transponder, polling at sufficient
  frequency creates unnecessary load and adds latency

---

## Race State Machine

**Recommendation: explicit state machine enforced in `RaceStateMachine` service.**

A race follows a strict lifecycle. Illegal transitions (e.g., starting a race that is
already finished) must be rejected, not silently ignored.

```
              ┌─────────┐
              │ PENDING  │  (race exists but not yet assembled)
              └────┬─────┘
                   │ CALL_GRID command
                   ▼
              ┌─────────┐
              │  GRID    │  (cars on starting grid, transponders active)
              └────┬─────┘
                   │ START command (triggers timing start, TCP receiver attaches to race)
                   ▼
              ┌─────────┐
              │ RUNNING  │  (lap times accumulating, live display active)
              └────┬─────┘
         ┌─────────┴──────────┐
         │ STOP command        │ FINISH (timer expires — future feature)
         ▼                     ▼
    ┌──────────┐          ┌──────────┐
    │ STOPPED  │          │ FINISHED │  (results finalised, points calculated)
    └────┬─────┘          └──────────┘
         │ RESUME (if race restarted)
         ▼
      RUNNING
```

**States:**

| State | Meaning | Allowed Commands |
|-------|---------|-----------------|
| `PENDING` | Race defined but not started | `CALL_GRID` |
| `GRID` | Grid called, racers assembling | `START`, `CANCEL` → PENDING |
| `RUNNING` | Race active, laps counting | `STOP`, `MARSHAL_LAP` |
| `STOPPED` | Race paused (incident, re-grid) | `RESUME`, `FINISH` |
| `FINISHED` | Results final | (none — terminal) |

**Implementation approach:**
- `RaceState` is an enum stored on the `Race` entity
- `RaceStateMachine` service has a method per command: `callGrid(raceId)`, `start(raceId)`,
  `stop(raceId)`, `finish(raceId)`, `marshalLap(...)`
- Each method loads the race, checks current state, throws `IllegalStateTransitionException`
  if invalid, applies the transition, saves, then publishes a domain event
- Published domain event is picked up by `LiveTimingHub` to push `/topic/race/{raceId}/state`
  to all subscribed browsers

**Marshal lap handling (within RUNNING state):**
- A marshal lap is not a state transition — it is a correction applied to a racer's lap count
- `marshalLap(raceId, racerId, adjustment)` where adjustment is +1 or -1
- Persisted as a `MarshalAdjustment` record (audit trail)
- Triggers recalculation of positions and re-broadcast of the timing topic

---

## Data Model

### Core Entities and Relationships

```
Racer
  ├─ id, name, email, club_member_number
  ├─── Car (many per racer)
  │      ├─ id, name, class, racer_id
  │      └─── Transponder (many per car; one "active" at a time)
  │             └─ id, transponder_number, car_id, active
  │
  └─── EventEntry (many per racer, per event)
         └─ id, racer_id, event_id, car_id, class

Event
  ├─ id, name, date, venue, description
  └─── Race (many per event)
         ├─ id, event_id, class, scheduled_start, state (enum)
         └─── Heat (many per race, if heats used)
                ├─ id, race_id, heat_number, state
                └─── LapTime (many per heat, per racer)
                       ├─ id, heat_id, racer_id, car_id, transponder_id
                       ├─ lap_number, lap_time_ms, crossing_time_epoch_ms
                       └─ is_marshal_adjusted (boolean)

MarshalAdjustment
  ├─ id, heat_id, racer_id, adjustment (+1/-1), reason, created_by, created_at

Championship
  ├─ id, name, season, class, best_of (e.g. 4), from_events (e.g. 6)
  └─── ChampionshipRound (many per championship)
         └─ id, championship_id, event_id, round_number

ChampionshipStanding (materialised/calculated)
  └─ championship_id, racer_id, total_points, rounds_scored, best_x_points[]
```

**Key relationships:**
- A `Transponder` is the hardware token that the AMB decoder sees. It belongs to a `Car`.
  The TCP receiver resolves a transponder number to a `Car` and therefore a `Racer`.
- A `Race` has a `state` field (the state machine target). It may contain `Heat`s (qualifying
  heats, A-final, B-final, etc.) or it may be a single timed event — the model supports both.
- `LapTime` records raw crossings. Position ordering is derived (not stored) except for the
  final published result snapshot.
- Championship points are calculated on demand from race results, not stored incrementally.
  Store the final result snapshot per racer per race; recalculate championship from snapshots.

---

## Suggested Build Order

Dependencies between subsystems dictate this order. Each phase delivers something
runnable that the next phase builds on.

### Phase 1 — Domain Foundation
**Goal:** Core data model, persistence, basic racer/car/transponder CRUD.

Build first because everything else depends on the domain entities.

- Spring Boot project scaffold with Flyway migrations
- `Racer`, `Car`, `Transponder` entities and repositories
- `Event`, `Race` entities (state field present but no state machine yet)
- Basic REST API for racer self-registration (unauthenticated or session-based)
- H2 in dev, PostgreSQL in production
- No UI yet — verify via curl or Postman

### Phase 2 — Racer Portal
**Goal:** Racers can register, manage cars/transponders, and enter events.

Builds on Phase 1 entities. Provides the self-service web interface.

- Spring Security (session-based auth, racer role)
- `EventEntry` entity and entry flow
- Racer portal frontend (Thymeleaf templates or single-page app — see STACK.md)
- Event schedule and entry list visible to public (read-only)

### Phase 3 — Admin Panel
**Goal:** Officials can create events, configure classes and championship structure.

Depends on Phase 1 entities. Independent of racer portal UI.

- Admin role in Spring Security
- Event / Race / Championship CRUD REST API
- Admin panel frontend

### Phase 4 — Race State Machine + Race Control API
**Goal:** Officials can drive a race through its lifecycle from a browser.

Depends on Phase 1 (entities), Phase 3 (events must exist before races can be controlled).

- `RaceStateMachine` service with full state transitions
- Race control REST API (`POST /races/{id}/start`, `/stop`, `/finish`, `/marshal-lap`)
- Race control browser client (thin page, no live timing yet — just command buttons)
- Illegal transition rejection with informative HTTP 409 responses

### Phase 5 — WebSocket Live Timing Infrastructure
**Goal:** State changes and race commands broadcast to browsers in real time.

Depends on Phase 4 (state machine events to broadcast). TCP receiver not needed yet —
test with synthetic lap events.

- `spring-boot-starter-websocket` + STOMP broker configuration
- `LiveTimingHub` component that holds `SimpMessagingTemplate`
- State machine publishes domain events; `LiveTimingHub` listens and broadcasts
- Race control client subscribes to `/topic/race/{id}/state` — command feedback is live

### Phase 6 — AMB/MyLaps TCP Receiver
**Goal:** Real lap timings from the physical decoder flow into the system.

Depends on Phase 5 (timing hub exists to receive the parsed events). Can be developed
in parallel with Phase 5 using a simulator.

- `TcpReceiverComponent` with managed background thread and reconnect logic
- `MyLapsProtocolParser` (binary frame parser — needs protocol specification research)
- `LapTimingService` resolves transponder → car → racer, calculates lap times, persists,
  orders positions, triggers `LiveTimingHub` broadcast
- Decoder connection health indicator in Spring Actuator
- **Flag: protocol parsing needs hands-on access to a real decoder or a protocol spec.
  Build a simulator (configurable test tool that sends fake TCP passings) to develop
  and test without physical hardware.**

### Phase 7 — Results and Championship Standings
**Goal:** Post-race results persisted and visible; championship points calculated.

Depends on Phase 6 (lap times must exist); Phase 3 (championship configuration).

- `RaceResultService`: when race transitions to `FINISHED`, snapshot positions and times
- Championship points calculation (configurable best-X-from-Y)
- Public results pages and championship standings table
- Print/export endpoint (PDF or structured HTML — for venue printing)

---

## Cross-Cutting Concerns

### Security Model
- **Racer role:** Can manage own profile, cars, transponders, event entries. Cannot see
  other racers' private details.
- **Admin role:** Can create/edit events, manage all entries, view all racers.
- **Race control role:** Can drive state machine, apply marshal laps. May be same as admin
  or a separate role for pit marshals who should not have admin access.
- **Anonymous:** Can view event schedule, live timing, results, championship standings.

Spring Security session-based auth is sufficient. OAuth2 / JWT adds complexity without
benefit for a club system that has no third-party API consumers.

### Persistence
- PostgreSQL in production. H2 in-memory for developer and CI environments.
- Flyway for schema migrations — never rely on Hibernate `ddl-auto: update` in production.
- Use `spring-boot-starter-data-jpa` with explicit SQL for complex queries (positions,
  championship calculations). Do not attempt to express championship scoring as JPQL.

### Configuration
- Decoder TCP host/port, reconnect behaviour, and session parameters in `application.yml`
  with environment variable overrides for deployment.
- Championship "best X from Y" is per-championship entity, not global config.

### Testing
- Unit test the `RaceStateMachine` exhaustively (all valid and invalid transitions).
- Unit test `MyLapsProtocolParser` against byte-level fixtures captured from a real decoder.
- Integration test the WebSocket hub using Spring's `StompSessionHandler` test support.
- Use a TCP simulator component (configurable fake decoder) for end-to-end tests that
  do not require physical hardware.

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Storing Derived Position State in the Database During a Race
**What:** Persisting "current position" per lap as it changes during a live race.
**Why bad:** Creates write contention under rapid lap crossings; positions are easily
recalculated from lap times; storing them mid-race is premature.
**Instead:** Calculate live positions in memory in `LapTimingService`, publish to
WebSocket, and only persist the final result snapshot when the race finishes.

### Anti-Pattern 2: Handling TCP Reads on the Spring MVC Thread Pool
**What:** Reading from the decoder socket in a request handler or scheduled task that
shares the web thread pool.
**Why bad:** A blocked TCP read starves the web container; if the decoder disconnects,
request handling degrades.
**Instead:** Dedicated thread for TCP, completely isolated from the Tomcat/Jetty pool.
The TCP thread posts parsed events to a small in-memory queue or calls the
`LapTimingService` via an `ApplicationEventPublisher` (async listener).

### Anti-Pattern 3: Coupling the Protocol Parser to the Application Service
**What:** Parsing MyLaps binary frames inside `LapTimingService` alongside business logic.
**Why bad:** Protocol parsing is an I/O concern, not a domain concern. Tight coupling makes
testing hard (must simulate TCP to test lap time logic).
**Instead:** `MyLapsProtocolParser` is a pure function: `byte[] → LapPassingEvent`. It has
no Spring dependencies. `LapTimingService` only receives `LapPassingEvent` objects.

### Anti-Pattern 4: Microservices for a Single-Club Deployment
**What:** Splitting TCP receiver, timing service, results service into separate deployable
units with inter-service messaging.
**Why bad:** Operational burden (multiple processes, health checks, messaging infrastructure)
far exceeds the needs of a single club running one event at a time.
**Instead:** Modular monolith. Enforce module boundaries through Java package conventions
and architecture tests (ArchUnit). Split only if the club federation model requires it.

### Anti-Pattern 5: Using Hibernate Schema Generation in Production
**What:** `spring.jpa.hibernate.ddl-auto=update` or `create-drop` in production config.
**Why bad:** Hibernate's DDL generation is not reliable for complex migrations; it cannot
drop columns or safely rename tables. Silent schema drift causes subtle data corruption.
**Instead:** Flyway from day one. Every schema change is a versioned migration script.

---

## Scalability Considerations

This system is designed for a single club. Scalability targets are modest.

| Concern | Single club (current) | Multi-club (future) |
|---------|----------------------|---------------------|
| Concurrent WebSocket clients | < 50 (one race, venue attendees) | 100–500 |
| TCP connections | 1 decoder, 1 connection | 1 per active track |
| Database writes during race | ~2–5 rows/second (lap passings) | Scale per active race |
| Horizontal scaling | Not needed; single JVM | Extract TCP receiver + timing service per club |

The modular monolith boundary between `TcpReceiver + LapTimingService` and the rest of
the application is the correct split point if multi-club operation is ever needed.

---

## Sources

- Spring Framework documentation (WebSocket/STOMP): HIGH confidence — well-established API
- Spring Security documentation: HIGH confidence — well-established API
- MyLaps/AMB TCP protocol: MEDIUM confidence — proprietary; community knowledge from
  open-source timing projects (e.g., LiveTiming.info integrations, RMonitor protocol).
  Verify with decoder manufacturer documentation or hardware access before Phase 6.
- Race state machine pattern: HIGH confidence — standard domain-driven design pattern
- Modular monolith recommendation: HIGH confidence — well-established architectural guidance
  for small-to-medium applications with a single deployment target
