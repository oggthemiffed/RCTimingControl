# Research Summary — RCTimingControl

**Synthesized:** 2026-04-15
**Sources:** STACK.md, FEATURES.md, ARCHITECTURE.md, PITFALLS.md, PROJECT.md

---

## Recommended Stack

| Concern | Choice | Notes |
|---------|--------|-------|
| Backend | Spring Boot 3.4.x on Java 21 | Java 21 LTS; virtual threads simplify high-concurrency I/O |
| AMB decoder client | Netty 4.1.x (custom ByteToMessageDecoder) | No Java AMB library exists; Netty pipeline model is the right abstraction |
| Real-time push | STOMP over WebSocket (spring-boot-starter-websocket) | Bidirectional; topic-based pub/sub for race events |
| Frontend | React 18 + Vite 5 | Vite replaces unmaintained CRA; TanStack Table suits live timing grids |
| State / data fetching | Zustand + TanStack Query v5 | No Redux; WS events to Zustand; REST state to TanStack Query |
| UI components | shadcn/ui + Tailwind CSS 3.x | Check Tailwind v4 migration status before starting |
| Database | PostgreSQL 16 | Window functions for standings; JSONB for scoring config; microsecond timestamps |
| ORM / migrations | Spring Data JPA + Flyway 10.x | Flyway plain-SQL over Liquibase; ddl-auto=validate in production only |
| Auth | Spring Security + JWT (JJWT 0.12.x) | Stateless; JWT in STOMP CONNECT headers for WebSocket auth |
| Print/export | JasperReports | PDF race results at venue |
| Testing | JUnit 5 + Mockito + Testcontainers; Vitest + RTL (frontend) | Testcontainers for PostgreSQL integration |

**Verify before use:** Spring Boot GA version, Java 25 LTS status (projected Sept 2025), React 18 vs 19, Vite 5 vs 6, JJWT 0.12.x, shadcn/ui Tailwind v4 compatibility.

**Do not use:** Spring Boot 2.x (EOL), Spring WebFlux (unnecessary complexity at this scale), HTMX (incompatible with WS push), Next.js (SSR not needed), Redux, SockJS (venue LAN), Kafka/RabbitMQ (overkill for single club), Liquibase, ddl-auto=create in production.

---

## Table Stakes Features

Must ship in v1 or the system cannot replace RCResults:

**Racer / Member Management**
- Racer self-registration and profile management (stated gap vs RCResults)
- Car registration with class assignment
- Transponder assignment to car — racer self-service (stated gap)
- Admin user management (list, search, edit, approve membership)
- Password reset via email

**Event Management**
- Event creation with multiple classes and race format per class
- Online event entry / registration by racers (stated gap)
- Event schedule visible publicly without login
- Heat seeding (by qualifying time or draw)

**Race Control — browser-based, primary differentiator**
- Start and stop race
- Grid call (show which cars are next on track)
- Marshal lap adjustments with audit trail
- Result review (provisional to final) before publishing
- Decoder connection status indicator visible at all times

**Timing and Lap Counting**
- AMB/MyLaps TCP decoder integration (without this there is no timing)
- Transponder read to lap count mapping with duplicate-hit deduplication
- Lap time recording using decoder-native timestamps, not server clock
- Fastest lap tracking
- Total race time calculation

**Results and Championships**
- Race results published publicly — no login required
- Finishing order with laps, time, and gap to leader
- Print or export race results at venue (PDF)
- Championship series with configurable best-X-from-Y scoring (default 4 from 6)
- Configurable points scale per championship
- Championship standings visible publicly

**Defer to v2:** Membership renewal reminders, transponder loan pool, waitlist auto-promotion, lap-by-lap breakdown in results, multiple decoder support / split timing, results analytics.

---

## Architecture Highlights

**Pattern: Modular monolith.** Single Spring Boot JAR, single PostgreSQL database, single deployment. No microservices — the modular boundary between TcpReceiver + LapTimingService and the rest of the application is the right future split point if multi-club operation is ever needed.

**Data flow (TCP to browser):**

    AMB Decoder (TCP)
      -> TcpReceiverComponent (Netty, dedicated background thread)
      -> LapTimingService (persists LapTime to DB, calculates position)
      -> LiveTimingHub (SimpMessagingTemplate)
      -> /topic/race/{raceId}/timing
      -> browser clients (race control, public timing display)

**Key STOMP topics:**
- `/topic/race/{raceId}/timing` — live lap passings and positions
- `/topic/race/{raceId}/state` — race lifecycle state changes
- `/topic/race/{raceId}/marshal` — marshal lap adjustments
- `/topic/event/{eventId}/schedule` — schedule updates

**Race state machine:** Server owns state exclusively. RaceStateMachine service enforces explicit transitions: PENDING -> GRID -> RUNNING -> STOPPED -> RUNNING -> FINISHED. Every transition is persisted before broadcasting. The browser client is a read-only view; newly connected clients receive full current state immediately on WebSocket connect.

**TCP receiver design:** Dedicated background thread isolated from the Tomcat thread pool, started via Spring SmartLifecycle. Exponential-backoff reconnect (1s -> 2s -> 4s -> 30s cap). Protocol parsing in a pure MyLapsProtocolParser class with no Spring dependencies (byte[] -> LapPassingEvent). Ingestion thread posts to a bounded LinkedBlockingQueue; a separate broadcast thread drains the queue and sends to WebSocket sessions — decoder read loop must never block on WebSocket I/O.

**Critical data model decisions:**
- Snapshot transponder-to-car assignments at race start; resolve all laps against the snapshot, not the live assignment table
- Persist every accepted LapTime record to the database on receipt; in-memory structures are read caches
- Store lap timestamps as UTC microseconds from the decoder's own clock, not System.currentTimeMillis()
- Championship points calculated on-demand from race result snapshots; standings invalidated and recomputed on any result amendment
- Marshal laps carry a synthetic=true flag; excluded from fastest-lap calculation

**Suggested build order:**
1. Domain foundation — entities, Flyway migrations, basic CRUD APIs
2. Racer portal — auth, car/transponder self-service, event entry, public schedule
3. Admin panel — event / race / championship CRUD
4. Race state machine + race control API — HTTP commands only, no WebSocket yet
5. WebSocket live timing infrastructure — STOMP broker, state broadcasts, stub lap data
6. AMB/MyLaps TCP receiver — real decoder integration (highest-risk phase)
7. Results and championship standings — post-race snapshots, points calculation, PDF export

---

## Top Pitfalls

**C1 — TCP stream framing [CRITICAL]**
InputStream.read() does not return one record at a time. Naive code drops and corrupts laps silently under real race load. Prevention: accumulate bytes into a ByteBuffer; parse only when a complete frame is confirmed by the protocol's length field. Unit-test the parser with every possible byte-boundary split before trusting any timing data.

**C2 — No reconnect loop [CRITICAL]**
A single TCP disconnect without automatic reconnection stops timing for the rest of the event with no indication to officials. Prevention: exponential-backoff reconnect is a first-class architectural component, not an afterthought; surface CONNECTED/RECONNECTING state visibly in race control UI from day one.

**C3 — Server clock used instead of decoder timestamp [CRITICAL]**
Processing latency (50-200ms under real load) causes incorrect lap times. The fastest-lap winner can be wrong. Prevention: parse and use the decoder's embedded timestamp exclusively; fail tests that compute lap time from Instant.now().

**C4 — Duplicate transponder hits not filtered [CRITICAL]**
A transponder in range for 100ms may generate multiple hits. Without a per-transponder deduplication window, racers accumulate phantom laps. Prevention: configurable minimum-lap-time window (1-3 s, track-dependent); discard hits within the window; test both sides of the boundary.

**C5 — Race state machine has no authoritative owner [CRITICAL]**
Two open race control browser windows can issue conflicting commands if the server does not enforce state. Prevention: all transitions happen server-side with guard conditions; client receives full state on connect; browser never sets race start time.

**C6 — WebSocket broadcast blocks the decoder ingestion thread [CRITICAL]**
Slow browser connections stall the decoder read loop causing TCP buffer overflow and missed laps. Prevention: producer/consumer queue between decoder thread and broadcast thread; decoder thread never blocks on WebSocket I/O.

**M1 — Transponder-to-car assignment changed mid-race**
Prevention: snapshot assignments at race start; use the snapshot for all lap attribution during the race.

**M3 — Best-X-from-Y scoring breaks on DNF/DNS/DQ**
Prevention: model all result statuses explicitly; document the club's scoring policy in code; exhaustive tests with partial result sets including zero-result racers.

**m3 — Lap counted before race officially starts**
Prevention: discard all transponder hits received before the race enters RUNNING state; log discarded hits for official review.

**m4 — Marshal lap picked as fastest lap**
Prevention: synthetic=true flag on all marshal laps; exclude synthetic laps from every fastest-lap calculation.

---

## Open Questions

These must be answered before or during implementation — not deferred to discovery mid-phase.

**Protocol — blocks Phase 6 entirely**

1. What is the exact binary frame format for the club's specific decoder model? Action: register at mylaps.com/developers for the SDK, or capture a Wireshark trace of the club's existing RCResults installation talking to the real decoder hardware.

2. Does the decoder provide UTC timestamps or session-relative offsets? The answer determines how clock drift is handled and how race-start base time is captured.

3. What initialisation or handshake messages does the decoder expect from the client before it begins streaming passings?

4. Does the decoder send a session-end notification, or does the client infer end-of-session from a race stop command?

**Championship scoring policy — blocks Phase 7**

5. For best-4-from-6 scoring: does a DNS count as a round toward the 6 (scoring zero, potentially displacing a better result), or is it excluded from the pool entirely? What about DQ vs DNF?

6. What tie-break rule applies when two racers have equal points after drops? (Most wins? Best result in most recent round? Head-to-head?)

7. What points scale does the club use — BRCA standard, another federation table, or a custom club scale?

**Heat structure — affects Phase 1 data model**

8. Does each class run qualifying heats then an A-final / B-final, or is qualifying a separate session type before a single final? Confirming the exact format before Phase 1 prevents data model rework.

**Operational**

9. Is the race control laptop always wired to the same LAN as the AMB decoder, or does it connect over WiFi? Affects how aggressively the reconnect logic needs to be designed.

10. Does the club want historical RCResults data importable, or is a clean start acceptable for v1?
