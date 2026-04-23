# Phase 4: Race Control — Research

**Researched:** 2026-04-23
**Domain:** Spring WebSocket/STOMP, Race State Machine, Live Timing UI, Round Generator
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Single-page cockpit — no tabs, no navigation. All key information visible simultaneously, non-scrolling outer container.
- **D-02:** Two-column layout: left panel (~30%) run order list; right panel (~70%) state-dependent content. Current race highlighted.
- **D-03:** Full mobile parity — cockpit uses Sheet drawer (not bottom nav) for mobile run order. Same breakpoint pattern as other layouts.
- **D-04:** Auto-advance through races — system selects next race automatically. Director taps Start; no manual selection in normal flow.
- **D-05:** Skip/re-run by clicking any race in run order list. Confirmation dialog for skipping forward: "This will skip N races — continue?"
- **D-06:** Full STOMP WebSocket infrastructure built in Phase 4, fed by synthetic timing generator. Phase 5 plugs in real passings without topology change.
- **D-07:** Synthetic timing button: `POST /api/v1/dev/race/{raceId}/synthetic-passing`, visible only when `VITE_DEV_PROFILE=dev`. Dev Spring profile only.
- **D-08:** OFFICIAL-01 (proximity alerts) and OFFICIAL-02 (backmarker detection) implemented in Phase 4 using synthetic timing data.
- **D-09:** Grid management: numbered input per driver row, table auto-reorders. No drag-and-drop.
- **D-10:** Right panel is state-dependent: PENDING=grid editor; GRID/RUNNING=live timing; STOPPED/FINISHED=results+audit.
- **D-11:** Round generator wizard in the cockpit right panel (not admin panel), accessible to RACE_DIRECTOR role.
- **D-12:** Wizard appears in left panel when no run order exists; normal run order replaces it once generated.
- **D-13:** Wizard steps: (1) Practice/qualifying rounds + max cars/heat, (2) per-class finals config, (3) full preview with driver lists. Confirm to generate.
- **D-14:** Marshal laps: +1/−1 buttons per driver in live timing. Confirmation dialog on each action. Positions recalculate immediately.
- **D-15:** Mandatory audit trail fields: driver, transponder number, adjustment timestamp, acting user (userId + display name), race ID, race state at time of adjustment, lap delta.
- **D-16:** Marshal adjustments audit: collapsible section below live timing table, visible during and after the race.
- **D-17:** Referee page at `/race-control/referee` — REFEREE role. Separate page for second device.
- **D-18:** Referee page: live timing, proximity alerts, backmarker detection, incident report form, lap/time penalty application.
- **D-19:** "Print Results" button in cockpit right panel when race reaches FINISHED. Opens new browser tab.
- **D-20:** Pre-race readiness: two-column view (marshal duty list | grid call) in right panel between races.
- **D-21:** Marshal row shows driver name + cumulative missed marshal count for this event.
- **D-22:** Race director marks marshal absent without auto-applying penalty. "Apply Penalty" is a separate optional action.
- **D-23:** Results sheet content: position, car number, driver name, laps, total time, best lap, gap + position chart. Club logo if configured.

### Claude's Discretion

- Exact STOMP topic paths (extend `/topic/race/{raceId}/...` pattern from CLAUDE.md)
- In-memory position calculation approach (sort by laps desc, then last passing timestamp asc)
- REST URL structure for race control endpoints (follow `/api/v1/` convention)
- Race control page route (`/race-control` or similar)
- Position chart library for PDF (Chart.js or equivalent)
- Exact confirmation dialog wording for state transitions
- How to model the optional marshal penalty (extension of MarshalAdjustment or separate record)

### Deferred Ideas (OUT OF SCOPE)

- Real AMB decoder integration — Phase 5
- Audio announcements (AUDIO-01–15) — Phase 6
- Championship standings calculation — Phase 7
- Event-level results PDF (full meeting export) — Phase 7
- Multi-decoder operation — post-v1
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CTRL-01 | Race director can start and stop a race from the browser race control client | Race state machine + REST API + STOMP broadcast |
| CTRL-02 | Race control displays the grid call — which cars are due on track next | GRID state panel in cockpit right panel, RaceEntry grid positions |
| CTRL-03 | Race director can add or remove marshal laps with full audit trail | MarshalAdjustment entity + D-14/D-15/D-16 |
| CTRL-04 | Race results exportable as printable/PDF sheet at the venue | Print page at `/race-control/results/{raceId}/print`, Chart.js position chart |
| CTRL-05 | Server enforces race state machine; conflicting commands rejected | RaceStateMachineService (EnumMap pattern), HTTP 409 |
| CTRL-06 | Unknown transponder passings retrospectively linked to entry | UnknownTransponderLink entity + banner UI + Dialog |
| CTRL-07 | Race control displays marshal list (drivers from previous race) | Pre-race readiness panel, GRID state |
| CTRL-08 | Race director can abandon a race in progress | RUNNING/STOPPED → FINISHED (abandon path) with result snapshot |
| CTRL-09 | Race director can skip to or re-run specific race | Left panel click with confirmation dialog (D-05) |
| OFFICIAL-01 | Race steward view shows live proximity alerts | Referee page: in-memory gap delta calculation per STOMP update |
| OFFICIAL-02 | Race steward view highlights backmarker situations | Referee page: lapped car detection (leader lap count > backmarker lap count) |
| OFFICIAL-03 | Race referee can raise incident report | IncidentReport entity + Dialog on referee page |
| OFFICIAL-04 | Race referee can apply lap or time penalty immediately updating standings | Penalty entity + in-memory position recalculation + STOMP broadcast |
</phase_requirements>

---

## Summary

Phase 4 is the largest phase in the project by feature surface. It introduces six interconnected subsystems: the schema foundation (Round, Race, RaceEntry entities plus EventClass finals fields), the round generator wizard, the server-side race state machine, the STOMP WebSocket infrastructure with synthetic timing, the race control cockpit UI, and the referee tools.

The single most important architectural decision in this phase is the **in-memory live timing model**. Race positions are never written to the database during a live race — they are calculated in memory from the sequence of `LapPassingEvent` objects and broadcast over STOMP. This means the server must maintain a `LiveRaceState` structure per active race in memory, which is the source of truth for all live timing, position calculation, marshal adjustments, and proximity/backmarker detection. Only when a race reaches `FINISHED` (or is abandoned) is a result snapshot persisted.

The WebSocket topology established here is permanent — Phase 5 only adds a real timing source without changing STOMP topics, message shapes, or the `LiveRaceState` model. This phase builds a `SyntheticTimingService` that generates `LapPassingEvent`s for testing, then plugs into exactly the same pipeline that real decoder passings will use in Phase 5.

**Primary recommendation:** Build the schema/state machine first (one plan), then the STOMP infrastructure with synthetic timing (one plan), then the cockpit UI in state-gated panels (one plan per major state: PENDING/grid editor, GRID/RUNNING timing, FINISHED/results), then the referee page (one plan). This order ensures each layer has testable seams.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Race state machine transitions | API / Backend | — | State must be enforced server-side (CTRL-05). HTTP 409 on conflict. |
| Live position calculation | API / Backend (in-memory) | — | CLAUDE.md: positions never persisted during race. Calculated from LapPassingEvent stream. |
| STOMP broadcast | API / Backend | — | SimpMessagingTemplate pushes updates; browser subscribes passively. |
| Marshal adjustment audit | API / Backend | Database | Write to DB immediately; in-memory positions update; STOMP rebroadcast. |
| Round generator algorithm | API / Backend | — | Seeding logic, heat splitting, bump-up slot filling. |
| Race control cockpit UI | Frontend | — | React single-page cockpit; state-dependent right panel; STOMP subscriber. |
| Referee alerts (proximity/backmarker) | Frontend | — | Computed client-side from STOMP timing messages (gap deltas, lap count comparison). |
| Print results page | Frontend | — | Static render from REST snapshot; Chart.js position chart; `window.print()` |
| JWT on STOMP CONNECT | API / Backend | Frontend | Server validates JWT in STOMP connect headers via ChannelInterceptor. |
| Penalty application | API / Backend | — | Penalty persisted + in-memory positions recalculated + STOMP rebroadcast. |

---

## Standard Stack

### Core (Backend)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| spring-boot-starter-websocket | 3.4.x (BOM) | Spring WebSocket + STOMP in-process broker | Locks in the CLAUDE.md stack; enables `@EnableWebSocketMessageBroker` |
| SimpMessagingTemplate | Spring Messaging (BOM) | Server-side STOMP broadcast via `convertAndSend` | Standard Spring idiom for pushing to `/topic/*` destinations |
| ApplicationEventPublisher | Spring Context (BOM) | Decouple LapPassingEvent emission from LapTimingService | Allows async `@EventListener` on LiveTimingHub |
| jOOQ 3.19.x | 3.19.x (BOM) | jOOQ read queries for run order, results projections, qualifying standings | Already established in project; query module seam maintained |
| Flyway | BOM | Schema migrations V17+ | Already established |

[VERIFIED: app/build.gradle.kts — spring-boot-starter-web, spring-boot-starter-data-jpa, jOOQ already present. spring-boot-starter-websocket NOT yet present — must be added.]

### Core (Frontend)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| @stomp/stompjs | 7.3.0 | STOMP-over-WebSocket client | Already installed in package.json; native WebSocket, no SockJS |
| chart.js | 4.5.1 | Position-by-lap chart on print results page | Specified in UI-SPEC (D-23); latest stable |
| react-chartjs-2 | 5.3.1 | React wrapper for Chart.js | Companion to chart.js; standard React integration |
| TanStack Query v5 | 5.99.0 | Server state for all REST queries + mutations | Already established; pattern is consistent across phases |
| React Hook Form v7 | 7.72.1 | Round generator wizard, grid position inputs | Already established |
| Zod | 3.25.x | Form validation schemas | Already established |
| sonner | 2.0.7 | Toast notifications (HTTP 409 race conflict, errors) | Already installed and used in other pages |

[VERIFIED: npm registry — chart.js@4.5.1, react-chartjs-2@5.3.1, @stomp/stompjs@7.3.0]
[VERIFIED: frontend/package.json — @stomp/stompjs, sonner, TanStack Query, RHF, Zod already installed]

### chart.js and react-chartjs-2 — NOT yet installed

Neither `chart.js` nor `react-chartjs-2` appears in `frontend/package.json`.

**Installation:**
```bash
npm install chart.js react-chartjs-2
```

**spring-boot-starter-websocket — NOT yet in build.gradle.kts**

```kotlin
implementation("org.springframework.boot:spring-boot-starter-websocket")
```

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| chart.js for position chart | recharts | recharts is more React-idiomatic but harder to use in a printable static page; chart.js works headlessly in the print page |
| SimpMessagingTemplate | Project Reactor FluxSink | WebFlux not permitted per CLAUDE.md |
| In-memory LiveRaceState | Redis | Single-club deployment; no external caching layer needed |
| ApplicationEventPublisher for lap events | Direct service call | Event publisher decouples TCP receiver from timing service; consistent with existing pattern |

---

## Architecture Patterns

### System Architecture Diagram

```
Browser (Race Director)              Browser (Referee)
  |  REST /api/v1/race-control/**      |  REST /api/v1/referee/**
  |  STOMP ws://.../ws/timing          |  STOMP (same)
  |                                    |
  +-------- Spring Boot (Tomcat) ------+
  |                                    |
  [RaceControlController]       [RefereeController]
       |                                  |
  [RaceStateMachineService] ←→ [LiveRaceState (in-memory, per race)]
       |                                  |
  [JPA Write Side]              [LiveTimingHub]
  Round/Race/RaceEntry                   |
  MarshalAdjustment             [SimpMessagingTemplate]
  IncidentReport                         |
  Penalty                        /topic/race/{id}/timing
       |                         /topic/race/{id}/state
  [Flyway / PostgreSQL]          /topic/race/{id}/marshal
       |                                  |
  [jOOQ Query Side]              Browser receives STOMP push
  Run order, standings, results          |
                                  Positions recalculated client-side
                                  OR pre-calculated server-side before push

  [SyntheticTimingService] ──→ LapPassingEvent ──→ [LapTimingService]
  (dev profile only;                                      |
   POST /api/v1/dev/race/{id}/synthetic-passing)  Updates LiveRaceState
                                                          |
                                                  [LiveTimingHub] broadcasts
```

### Recommended Project Structure

```
app/src/main/java/dev/monkeypatch/rctiming/
├── domain/
│   └── race/                          # New in Phase 4 (write side)
│       ├── Round.java                 # Entity
│       ├── RoundRepository.java
│       ├── Race.java                  # Entity
│       ├── RaceRepository.java
│       ├── RaceEntry.java             # Entity
│       ├── RaceEntryRepository.java
│       ├── RaceStatus.java            # Enum: PENDING/GRID/RUNNING/STOPPED/FINISHED
│       ├── RoundStatus.java           # Enum: PENDING/RUNNING/COMPLETED
│       ├── RoundType.java             # Enum: PRACTICE/QUALIFIER/FINAL
│       ├── RaceStateMachineService.java
│       ├── MarshalAdjustment.java     # Entity with audit fields (D-15)
│       ├── MarshalAdjustmentRepository.java
│       ├── MarshalAbsence.java        # Entity (separate from penalty per D-22)
│       ├── MarshalAbsenceRepository.java
│       ├── IncidentReport.java        # Entity (OFFICIAL-03)
│       ├── IncidentReportRepository.java
│       ├── Penalty.java               # Entity (OFFICIAL-04)
│       ├── PenaltyRepository.java
│       └── UnknownTransponderLink.java # Entity (CTRL-06)
├── timing/                            # New in Phase 4
│   ├── LiveRaceState.java             # In-memory model (not a Spring bean — one instance per race)
│   ├── LiveRacePosition.java          # Value object: position, laps, lastLap, bestLap, gap
│   ├── LapPassingEvent.java           # Domain event
│   ├── LapTimingService.java          # Receives events, updates LiveRaceState, triggers broadcast
│   └── LiveTimingHub.java             # Spring component wrapping SimpMessagingTemplate
├── service/
│   ├── RoundGeneratorService.java     # Creates Round+Race records (heat splitting, seeding)
│   ├── SyntheticTimingService.java    # Dev profile only: fires synthetic LapPassingEvents
│   └── ResultSnapshotService.java     # Persists final result on FINISHED/abandon
├── websocket/
│   └── WebSocketConfig.java           # @EnableWebSocketMessageBroker, JWT ChannelInterceptor
├── security/
│   └── WebSocketJwtChannelInterceptor.java  # Validates JWT in STOMP CONNECT frame
└── api/
    └── racecontrol/
        ├── RaceControlController.java  # RACE_DIRECTOR endpoints
        ├── RefereeController.java      # REFEREE endpoints
        ├── DevTimingController.java    # @Profile("dev") synthetic passing
        └── dto/
            ├── RaceDto.java
            ├── RaceEntryDto.java
            ├── LiveTimingRowDto.java
            ├── MarshalAdjustmentRequest.java
            ├── MarshalAdjustmentDto.java
            ├── GenerateRunOrderRequest.java
            ├── RoundPreviewDto.java
            ├── IncidentReportRequest.java
            ├── PenaltyRequest.java
            └── ResultSnapshotDto.java

frontend/src/
├── pages/
│   └── race-control/
│       ├── RaceControlLayout.tsx       # Shared top bar for cockpit + referee
│       ├── CockpitPage.tsx             # /race-control — race director
│       ├── RefereePage.tsx             # /race-control/referee
│       ├── PrintResultsPage.tsx        # /race-control/results/:raceId/print
│       ├── panels/
│       │   ├── RunOrderPanel.tsx       # Left panel
│       │   ├── GridEditorPanel.tsx     # Right panel PENDING
│       │   ├── LiveTimingPanel.tsx     # Right panel GRID/RUNNING
│       │   ├── StoppedPanel.tsx        # Right panel STOPPED
│       │   ├── FinishedPanel.tsx       # Right panel FINISHED
│       │   ├── PreRaceReadinessPanel.tsx # Right panel between races
│       │   └── RoundGeneratorWizard.tsx  # Multi-step wizard (D-11/13)
│       └── referee/
│           ├── RefereeTimingTable.tsx
│           ├── IncidentDialog.tsx
│           └── PenaltyDialog.tsx
├── hooks/
│   └── race-control/
│       ├── raceControlQueryKeys.ts
│       ├── useRaceControl.ts           # Queries for run order, race detail
│       ├── useRaceStateMutations.ts    # Mutations: callGrid, startRace, stopRace etc.
│       ├── useMarshalAdjustment.ts
│       ├── useStomp.ts                 # STOMP client lifecycle hook
│       └── useLiveTimingSubscription.ts # Subscribes to /topic/race/{id}/timing
└── lib/
    └── raceControlApi.ts               # Axios calls for race control endpoints
```

### Pattern 1: Race State Machine (EnumMap)

Exactly mirrors `EventStateMachineService`. One method per command. Invalid transitions throw `IllegalStateTransitionException` → caught by `GlobalExceptionHandler` → HTTP 409.

```java
// Source: existing EventStateMachineService.java (VERIFIED)
@Service
public class RaceStateMachineService {
    private static final Map<RaceStatus, Set<RaceStatus>> VALID_TRANSITIONS;

    static {
        Map<RaceStatus, Set<RaceStatus>> m = new EnumMap<>(RaceStatus.class);
        m.put(RaceStatus.PENDING,  EnumSet.of(RaceStatus.GRID));
        m.put(RaceStatus.GRID,     EnumSet.of(RaceStatus.RUNNING, RaceStatus.PENDING));
        m.put(RaceStatus.RUNNING,  EnumSet.of(RaceStatus.STOPPED, RaceStatus.FINISHED));
        m.put(RaceStatus.STOPPED,  EnumSet.of(RaceStatus.RUNNING, RaceStatus.FINISHED));
        m.put(RaceStatus.FINISHED, EnumSet.noneOf(RaceStatus.class));
        VALID_TRANSITIONS = Map.copyOf(m);
    }

    public void transition(Race race, RaceStatus target) {
        Set<RaceStatus> valid = VALID_TRANSITIONS.getOrDefault(race.getStatus(), Set.of());
        if (!valid.contains(target)) {
            throw new IllegalStateTransitionException(
                "Cannot transition " + race.getStatus() + " → " + target);
        }
        race.setStatus(target);
    }
}
```

[VERIFIED: existing codebase — GlobalExceptionHandler already catches IllegalStateTransitionException → HTTP 409]

### Pattern 2: WebSocket STOMP Configuration

```java
// Source: Spring official guide gs/messaging-stomp-websocket [CITED]
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // No SockJS — venue LAN 2026; CLAUDE.md forbids SockJS
        registry.addEndpoint("/ws/timing");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketJwtChannelInterceptor);
    }
}
```

### Pattern 3: JWT Validation on STOMP CONNECT

The existing `JwtAuthenticationFilter` runs on HTTP requests. WebSocket upgrades go through `/ws/timing` but STOMP messages arrive on a different channel. JWT must be validated in a `ChannelInterceptor`.

```java
// Source: Spring blog 2014/09/16 [CITED], adapted for project JWT
@Component
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Authentication auth = jwtService.validateAndBuildAuthentication(token);
                accessor.setUser(auth);
            }
        }
        return message;
    }
}
```

Client-side STOMP CONNECT with JWT:
```javascript
// Source: stompjs docs [VERIFIED: /stomp-js/stompjs]
const client = new Client({
  brokerURL: `ws://${window.location.host}/ws/timing`,
  connectHeaders: {
    Authorization: `Bearer ${jwtToken}`,
  },
  reconnectDelay: 5000,
  onConnect: () => {
    client.subscribe(`/topic/race/${raceId}/timing`, (msg) => {
      const update = JSON.parse(msg.body);
      // update live timing table
    });
  },
});
client.activate();
```

### Pattern 4: LiveRaceState In-Memory Model

```java
// Source: CLAUDE.md architecture notes [VERIFIED]
// NOT a Spring bean — one instance per active race, managed by LapTimingService
public class LiveRaceState {
    private final Long raceId;
    private final Map<Long, RacePosition> positions; // entryId → position
    private final List<LapRecord> lapHistory;         // for position chart
    private final List<MarshalAdjustmentRecord> adjustments;
    private Instant raceStartTime;
    // ...

    // Position sort: laps DESC, then last-passing-timestamp ASC (ahead = crossed earlier)
    public List<RacePosition> calculatePositions() {
        return positions.values().stream()
            .sorted(Comparator
                .comparingInt(RacePosition::getLaps).reversed()
                .thenComparing(RacePosition::getLastPassingTime))
            .toList();
    }
}
```

### Pattern 5: SimpMessagingTemplate Broadcast

```java
// Source: Spring blog 2013/07/24 [CITED], adapted for race timing
@Component
public class LiveTimingHub {
    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastTimingUpdate(Long raceId, List<LiveTimingRowDto> positions) {
        messagingTemplate.convertAndSend(
            "/topic/race/" + raceId + "/timing",
            positions);
    }

    public void broadcastStateChange(Long raceId, RaceStatus newStatus) {
        messagingTemplate.convertAndSend(
            "/topic/race/" + raceId + "/state",
            new RaceStateChangeDto(raceId, newStatus));
    }

    public void broadcastMarshalAdjustment(Long raceId, MarshalAdjustmentDto dto) {
        messagingTemplate.convertAndSend(
            "/topic/race/" + raceId + "/marshal",
            dto);
    }
}
```

### Pattern 6: STOMP React Hook

```typescript
// [ASSUMED] — pattern inferred from stompjs docs and project conventions
// Create as a custom hook, not a global singleton
function useLiveTimingSubscription(raceId: number, onUpdate: (rows: LiveTimingRow[]) => void) {
  const { token } = useAuth();

  useEffect(() => {
    const client = new Client({
      brokerURL: `ws://${window.location.host}/ws/timing`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/race/${raceId}/timing`, (msg) => {
          onUpdate(JSON.parse(msg.body));
        });
      },
    });
    client.activate();
    return () => { client.deactivate(); };
  }, [raceId, token]);
}
```

### Anti-Patterns to Avoid

- **`SpringWebFlux` or reactive types in LiveTimingHub:** CLAUDE.md forbids the reactive stack. Use `SimpMessagingTemplate` (sync, Tomcat thread pool is adequate).
- **`spring.jpa.hibernate.ddl-auto=update`:** CLAUDE.md explicitly forbids. All schema changes via Flyway V17+.
- **Storing live race positions in the database during a race:** CLAUDE.md explicitly forbids. In-memory only until `FINISHED`.
- **SockJS on the STOMP endpoint:** CLAUDE.md forbids. `registry.addEndpoint("/ws/timing")` without `.withSockJS()`.
- **Lazy-loading in the query module:** jOOQ projections only on the read side. Never pass a Hibernate-managed entity into the query module.
- **Sharing `LiveRaceState` instance across races:** One `LiveRaceState` per active race. Store in a `ConcurrentHashMap<Long, LiveRaceState>` in `LapTimingService`, keyed by `raceId`.
- **Proximity alert logic in the backend:** Proximity and backmarker detection happen client-side in the referee page from the STOMP timing stream. No separate API needed — the referee subscribes to the same `/topic/race/{id}/timing` as the cockpit.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| State machine transitions | Custom if/switch on status | EnumMap pattern (exact copy of EventStateMachineService) | Already proven in codebase; throws IllegalStateTransitionException automatically caught as 409 |
| WebSocket message encoding | Manual JSON framing | SimpMessagingTemplate.convertAndSend (Jackson auto-serialises DTOs) | Handles content-type, serialisation, routing |
| STOMP client reconnect | Manual reconnect loop | stompjs `reconnectDelay` configuration | Built-in exponential backoff, heartbeat |
| JWT validation on WS CONNECT | Session tracking | ChannelInterceptor preSend checking STOMP CONNECT command | Standard Spring pattern; integrates with existing JwtService |
| Position sort algorithm | Complex ranking logic | Laps DESC + lastPassingTime ASC (two-field sort) | Canonical RC race scoring; no library needed but pattern must be consistent everywhere |
| Form validation in wizard | Manual field checks | React Hook Form v7 + Zod | Already established in Phase 3 admin forms |
| Heat assignment algorithm | Custom scheduler | Round generator service (straightforward modulo split) | Not complex — drivers are split into heats by count, ordered by seed/entry number |
| Print PDF generation | Server-side PDF renderer (iText, Wkhtmltopdf) | Browser print page with `@media print` CSS | Simpler, no server dependency; browser handles rendering; Chart.js renders client-side |

**Key insight:** The hardest part of this phase is not any single library — it is maintaining the consistency of the in-memory `LiveRaceState` across concurrent operations (marshal adjustments while the race is running, multiple browsers connected). Use `synchronized` or `ReentrantLock` on state mutations in `LapTimingService` where needed.

---

## Common Pitfalls

### Pitfall 1: STOMP Endpoint Blocked by Spring Security

**What goes wrong:** The WebSocket upgrade request to `/ws/timing` is blocked by Spring Security with HTTP 403 before the STOMP handshake.
**Why it happens:** `SecurityConfig.authorizeHttpRequests` blocks the upgrade URL. The existing config has no rule for `/ws/timing`.
**How to avoid:** Add `.requestMatchers("/ws/timing").permitAll()` to `SecurityConfig`. JWT validation happens at the STOMP CONNECT frame level via `ChannelInterceptor`, not at HTTP level. The HTTP upgrade itself must be permitted.
**Warning signs:** Browser console shows `WebSocket connection failed: 403 Forbidden`.

[VERIFIED: codebase — SecurityConfig does not currently include `/ws/timing`; must be added]

### Pitfall 2: LiveRaceState Lost on Application Restart

**What goes wrong:** If the Spring Boot process restarts mid-race, all in-memory `LiveRaceState` objects are lost.
**Why it happens:** In-memory state is not durable.
**How to avoid:** Phase 4 scope — accept the limitation; document that restarting the server during a live race loses position history and requires an abandon+re-run. This is an acceptable trade-off at single-club scale. Do not add persistence complexity for Phase 4.
**Warning signs:** This is expected behaviour, not a bug.

### Pitfall 3: Concurrent Marshal Adjustment and Timing Update

**What goes wrong:** A marshal adjustment and a lap passing arrive at nearly the same time, causing a race condition in `LiveRaceState`.
**Why it happens:** `LapTimingService.@EventListener` runs on the async event thread; marshal adjustment REST handler runs on a Tomcat thread.
**How to avoid:** Synchronise mutations on the `LiveRaceState` instance. Use a `synchronized` block or `ReentrantLock` in `LapTimingService.updateState()` and `applyMarshalAdjustment()`. The lock window is small (in-memory sort) so this will not cause latency issues.
**Warning signs:** Occasional inconsistency between marshal audit trail count and displayed positions.

### Pitfall 4: STOMP Subscription Before Race Starts (Missing Initial State)

**What goes wrong:** The referee or a second cockpit browser connects mid-race and has no data until the next STOMP push.
**Why it happens:** STOMP topics are push-only; late subscribers don't receive history.
**How to avoid:** Provide a REST endpoint `GET /api/v1/race-control/race/{raceId}/live-state` that returns the current `LiveRaceState` as a snapshot. The frontend calls this once on mount, then subscribes to STOMP for incremental updates.
**Warning signs:** Blank live timing table until first crossing after page load.

### Pitfall 5: A-Final Grid Not Ready Until B-Final Completes

**What goes wrong:** The round generator creates bump-up slot rows in `RaceEntry` with `gridPosition = null` for bump-up positions in the A-Final.
**Why it happens:** The bump-up algorithm requires lower final results to be known before higher final grids are complete (per heat structure spec).
**How to avoid:** When a final finishes and `bumpCount > 0`, auto-fill the `gridPosition` values for the bump-up slots in the next-higher final. Alert the race director via a STOMP notification or UI banner: "B-Final complete — A-Final grid updated with bump-up drivers." Grid editor shows these as pre-filled but editable.
**Warning signs:** A-Final shows null grid positions for bump-up slots before B-Final completion.

### Pitfall 6: Unknown Transponder Duplicate Banners

**What goes wrong:** The same unknown transponder fires many times during a race, generating repeated UI banners.
**Why it happens:** Each `LapPassingEvent` with an unregistered transponder would trigger a new banner.
**How to avoid:** Deduplicate unknowns server-side: a `Set<String>` of already-reported transponder numbers in `LiveRaceState`. Only emit the STOMP unknown-transponder notification once per transponder per race. Subsequent passings are silently logged but not re-notified.
**Warning signs:** Multiple banners for the same transponder number during a race.

### Pitfall 7: Chart.js in Print Page Requires Canvas

**What goes wrong:** Chart.js renders to an HTML `<canvas>`. In a print page, the canvas may render as a blank box or be clipped.
**Why it happens:** `@media print` can suppress canvas elements in some browsers.
**How to avoid:** Set explicit pixel dimensions on the canvas element. Use `chart.resize()` on `window.onbeforeprint`. Alternatively, render the chart as a static PNG by calling `chart.toBase64Image()` on load and replacing the canvas with an `<img>`. The `<img>` approach is more reliable for printing.
**Warning signs:** Position chart appears blank on printed results sheet.

### Pitfall 8: jOOQ Code Generation After Schema Changes

**What goes wrong:** New Flyway migrations (V17–V2x) add Round, Race, RaceEntry tables. jOOQ generated code must be regenerated before the query module compiles.
**Why it happens:** The jOOQ DSL is generated from the live schema. If migrations run but codegen doesn't, the generated `Tables` class is stale.
**How to avoid:** Run `./gradlew :app:generateJooq` after adding each migration. Include this as a step in the first plan (schema plan). Tests using `AbstractIntegrationTest` will fail with `NoSuchColumnException` if the generated code is stale.
**Warning signs:** Compile errors in query module referencing `Tables.ROUND`, `Tables.RACE`, `Tables.RACE_ENTRY`.

---

## Code Examples

### Spring WebSocket Config (Full — no SockJS)

```java
// Source: Spring official guide, adapted per CLAUDE.md [CITED: spring.io/guides/gs/messaging-stomp-websocket]
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketJwtChannelInterceptor jwtInterceptor;

    public WebSocketConfig(WebSocketJwtChannelInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // No withSockJS() per CLAUDE.md
        registry.addEndpoint("/ws/timing");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtInterceptor);
    }
}
```

### Synthetic Timing Controller (Dev Profile)

```java
// [ASSUMED] pattern based on Spring @Profile and project conventions
@RestController
@RequestMapping("/api/v1/dev/race")
@Profile("dev")
public class DevTimingController {

    private final SyntheticTimingService syntheticTimingService;

    @PostMapping("/{raceId}/synthetic-passing")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('RACE_DIRECTOR')")
    public void fireSyntheticPassing(@PathVariable Long raceId) {
        syntheticTimingService.fireSyntheticPassing(raceId);
    }
}
```

### Flyway Migration V17 — Race Schema

```sql
-- V17: Phase 4 schema — Round, Race, RaceEntry; EventClass finals config
ALTER TABLE event_classes
    ADD COLUMN finals_count      int,
    ADD COLUMN cars_per_final    int,
    ADD COLUMN bump_count        int;

CREATE TABLE rounds (
    id                bigserial PRIMARY KEY,
    event_id          bigint NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    type              varchar(20) NOT NULL CHECK (type IN ('PRACTICE','QUALIFIER','FINAL')),
    round_number      int NOT NULL,
    sequence_in_event int NOT NULL,
    status            varchar(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','RUNNING','COMPLETED'))
);

CREATE TABLE races (
    id                  bigserial PRIMARY KEY,
    round_id            bigint NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    event_class_id      bigint NOT NULL REFERENCES event_classes(id),
    heat_number         int NOT NULL,
    sequence_in_round   int NOT NULL,
    final_letter        varchar(5),
    start_type          varchar(20) NOT NULL CHECK (start_type IN ('STAGGER','GRID')),
    format_id           bigint REFERENCES race_format_templates(id),
    format_overrides    jsonb,
    status              varchar(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','GRID','RUNNING','STOPPED','FINISHED'))
);

CREATE TABLE race_entries (
    id            bigserial PRIMARY KEY,
    race_id       bigint NOT NULL REFERENCES races(id) ON DELETE CASCADE,
    entry_id      bigint NOT NULL REFERENCES entries(id),
    grid_position int,
    bumped        boolean NOT NULL DEFAULT false,
    UNIQUE (race_id, entry_id)
);

CREATE INDEX idx_rounds_event_id ON rounds(event_id);
CREATE INDEX idx_races_round_id ON races(round_id);
CREATE INDEX idx_races_event_class_id ON races(event_class_id);
CREATE INDEX idx_race_entries_race_id ON race_entries(race_id);
CREATE INDEX idx_race_entries_entry_id ON race_entries(entry_id);
```

### Flyway Migration V18 — Marshal and Referee Schema

```sql
-- V18: Phase 4 marshal adjustments, absences, incidents, penalties
CREATE TABLE marshal_adjustments (
    id                   bigserial PRIMARY KEY,
    race_id              bigint NOT NULL REFERENCES races(id),
    entry_id             bigint NOT NULL REFERENCES entries(id),
    transponder_number   varchar(20) NOT NULL,
    lap_delta            int NOT NULL CHECK (lap_delta IN (-1, 1)),
    race_state_at_time   varchar(20) NOT NULL,
    acting_user_id       bigint NOT NULL,
    acting_user_name     varchar(200) NOT NULL,
    adjusted_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE marshal_absences (
    id              bigserial PRIMARY KEY,
    race_id         bigint NOT NULL REFERENCES races(id),
    entry_id        bigint NOT NULL REFERENCES entries(id),
    event_id        bigint NOT NULL REFERENCES events(id),
    recorded_at     timestamptz NOT NULL DEFAULT now(),
    recorded_by     bigint NOT NULL,
    UNIQUE (race_id, entry_id)
);

CREATE TABLE marshal_penalties (
    id              bigserial PRIMARY KEY,
    absence_id      bigint REFERENCES marshal_absences(id),
    entry_id        bigint NOT NULL REFERENCES entries(id),
    event_id        bigint NOT NULL REFERENCES events(id),
    applied_by      bigint NOT NULL,
    applied_at      timestamptz NOT NULL DEFAULT now(),
    notes           text
);

CREATE TABLE incident_reports (
    id              bigserial PRIMARY KEY,
    race_id         bigint NOT NULL REFERENCES races(id),
    entry_id        bigint NOT NULL REFERENCES entries(id),
    incident_type   varchar(50) NOT NULL,
    description     text,
    raised_by       bigint NOT NULL,
    raised_at       timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE penalties (
    id              bigserial PRIMARY KEY,
    race_id         bigint NOT NULL REFERENCES races(id),
    entry_id        bigint NOT NULL REFERENCES entries(id),
    penalty_type    varchar(20) NOT NULL CHECK (penalty_type IN ('LAP','TIME')),
    value           numeric NOT NULL,
    reason          text,
    applied_by      bigint NOT NULL,
    applied_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE unknown_transponder_links (
    id                  bigserial PRIMARY KEY,
    race_id             bigint NOT NULL REFERENCES races(id),
    transponder_number  varchar(20) NOT NULL,
    linked_entry_id     bigint REFERENCES entries(id),
    linked_by           bigint NOT NULL,
    linked_at           timestamptz NOT NULL DEFAULT now()
);
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| SockJS WebSocket fallback | Native WebSocket only (no SockJS) | Standard in 2024+ | Simpler client config; venue LAN always supports WS |
| `AbstractWebSocketMessageBrokerConfigurer` (deprecated) | `implements WebSocketMessageBrokerConfigurer` | Spring 5+ | Use interface, not abstract class |
| `ChannelInterceptorAdapter` (deprecated) | `implements ChannelInterceptor` | Spring 5+ | Use interface directly |
| `@SendTo` on controller methods | `SimpMessagingTemplate.convertAndSend` (programmatic) | Both valid | `@SendTo` fine for simple cases; `SimpMessagingTemplate` needed for conditional/dynamic sends like timing broadcast |

**Deprecated/outdated:**
- `AbstractWebSocketMessageBrokerConfigurer`: use `WebSocketMessageBrokerConfigurer` interface directly
- `ChannelInterceptorAdapter`: use `ChannelInterceptor` interface directly (single abstract method removed in Spring 5)
- SockJS: not needed for modern browsers on LAN; explicitly forbidden by CLAUDE.md

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 | Spring Boot backend | ✓ | 21.0.10 (Temurin LTS) | — |
| Node.js | Frontend build | ✓ | 20.19.2 | — |
| Docker | Testcontainers (PostgreSQL) | ✓ | 29.4.0 | — |
| spring-boot-starter-websocket | STOMP broker | ✗ (not in build.gradle.kts) | BOM-managed | Must add — no fallback |
| chart.js + react-chartjs-2 | Print results position chart | ✗ (not in package.json) | 4.5.1 / 5.3.1 | Must install — no fallback for D-23 |
| @stomp/stompjs | Frontend STOMP client | ✓ | 7.3.0 | Already installed |

**Missing dependencies with no fallback:**
- `spring-boot-starter-websocket` must be added to `app/build.gradle.kts` before the STOMP plan
- `chart.js` and `react-chartjs-2` must be installed via npm before the print results plan

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Testcontainers (backend) |
| Config file | `app/build.gradle.kts` (test section) |
| Quick run command | `./gradlew :app:test --tests "*.race.*" -x generateJooq` |
| Full suite command | `./gradlew :app:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CTRL-05 | Invalid state transition returns HTTP 409 | Unit (RaceStateMachineService) | `./gradlew :app:test --tests "*.RaceStateMachineServiceTest"` | ❌ Wave 0 |
| CTRL-05 | Valid transition updates race status | Unit | same | ❌ Wave 0 |
| CTRL-01 | POST callGrid returns 200 and transitions to GRID | Integration | `./gradlew :app:test --tests "*.RaceControlControllerIT"` | ❌ Wave 0 |
| CTRL-01 | POST startRace returns 200 and transitions to RUNNING | Integration | same | ❌ Wave 0 |
| CTRL-03 | Marshal adjustment persists audit fields | Integration | same | ❌ Wave 0 |
| CTRL-06 | Unknown transponder link creates record | Integration | same | ❌ Wave 0 |
| CTRL-08 | Abandon race saves result snapshot | Integration | `./gradlew :app:test --tests "*.RaceControlControllerIT.abandon*"` | ❌ Wave 0 |
| CTRL-04 | GET /results/{id}/print returns 200 with correct data | Integration | same | ❌ Wave 0 |
| OFFICIAL-03 | Incident report creates record linked to race | Integration | `./gradlew :app:test --tests "*.RefereeControllerIT"` | ❌ Wave 0 |
| OFFICIAL-04 | Penalty application recalculates positions | Integration | same | ❌ Wave 0 |
| Round generator | Heat splitting: 15 drivers, max 8/heat → 2 heats | Unit | `./gradlew :app:test --tests "*.RoundGeneratorServiceTest"` | ❌ Wave 0 |
| Round generator | Bump-up seeding: top N of B-Final appended to A-Final | Unit | same | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:test --tests "*.race.*" --tests "*.racecontrol.*" -x generateJooq`
- **Per wave merge:** `./gradlew :app:test`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `app/src/test/.../domain/race/RaceStateMachineServiceTest.java` — covers CTRL-05
- [ ] `app/src/test/.../api/racecontrol/RaceControlControllerIT.java` — covers CTRL-01, CTRL-03, CTRL-06, CTRL-08, CTRL-04
- [ ] `app/src/test/.../api/racecontrol/RefereeControllerIT.java` — covers OFFICIAL-03, OFFICIAL-04
- [ ] `app/src/test/.../service/RoundGeneratorServiceTest.java` — covers round generator algorithm

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | JWT via ChannelInterceptor on STOMP CONNECT; existing JwtService reused |
| V3 Session Management | partial | STOMP sessions are JWT-validated per-connection; no server session |
| V4 Access Control | yes | `@PreAuthorize("hasRole('RACE_DIRECTOR')")` on race control endpoints; `hasRole('REFEREE')` on referee endpoints |
| V5 Input Validation | yes | Zod on frontend wizard forms; `@Valid` on all REST DTOs; RaceStatus enum validation |
| V6 Cryptography | no | No new crypto operations; JWT signing handled by existing JjwtService |

### Known Threat Patterns for Race Control Stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Double-submit race command (two browsers racing to start) | Tampering | EnumMap state machine + DB transaction on state update → second command sees current state, throws 409 |
| Unauthorized marshal adjustment (race observer trying +1) | Elevation of Privilege | `@PreAuthorize("hasRole('RACE_DIRECTOR')")` on `/api/v1/race-control/**` |
| Unauthorized penalty application | Elevation of Privilege | `@PreAuthorize("hasRole('REFEREE')")` on `/api/v1/referee/**` |
| STOMP subscription to another race's timing | Information Disclosure | For Phase 4: `/topic/race/{raceId}/timing` is public (timing is not secret); STOMP subscribe access control can be added post-v1 if needed |
| Synthetic timing endpoint in production | Tampering | `@Profile("dev")` on `DevTimingController` — bean does not exist in prod profile |
| Forged JWT in STOMP CONNECT | Spoofing | ChannelInterceptor validates signature via JwtService; invalid JWT rejects CONNECT |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `VITE_DEV_PROFILE=dev` is the correct env var name for gating the Synthetic Lap button | Standard Stack / Code Examples | Button may appear in wrong environment; rename env var accordingly |
| A2 | The `LapTimingService` will hold the `ConcurrentHashMap<Long, LiveRaceState>` as the single authoritative in-memory store | Architecture Patterns | If split across beans, consistency is harder to maintain |
| A3 | Marshal penalty is modelled as a separate `marshal_penalties` table linked to `marshal_absences` | Code Examples (V18 migration) | If penalty extends MarshalAdjustment instead, schema differs — Claude's Discretion per D-22 context |
| A4 | Proximity alert threshold (OFFICIAL-01) is a computed gap in seconds defined as a constant or config value, not a DB setting | Domain | If configurable per-track, a config column is needed |
| A5 | `POST /api/v1/dev/race/{raceId}/synthetic-passing` is the correct URL form for the synthetic timing endpoint | Architecture Patterns | Only affects dev workflow; easy to change |
| A6 | The print results page is served as a React route, not a server-rendered template | Code Examples | No server-side PDF complexity introduced; print CSS handles layout |

---

## Open Questions

1. **Penalty and position sorting interaction**
   - What we know: Penalties are `LAP` or `TIME` type. Marshal adjustments are `±1 lap`.
   - What's unclear: Does a `TIME` penalty affect position sorting (which is lap-count-based during live race)? Or is it only applied to final result times?
   - Recommendation: For Phase 4 scope, apply `LAP` penalties to live position (deduct from lap count) and `TIME` penalties only to results display (add to total time on FINISHED). Document this in the implementation plan task that creates the penalty entity.

2. **Qualifying standings calculation trigger**
   - What we know: After each QUALIFIER race completes, the system must recalculate qualifying standings (per heat structure spec).
   - What's unclear: Does this happen automatically on `FINISHED` transition, or does the race director trigger it manually?
   - Recommendation: Auto-calculate qualifying standings on every QUALIFIER race `FINISHED` transition. Store the result in a `qualifying_standings` table (jOOQ read projection). This feeds finals grid seeding.

3. **`MarshalAbsence` count query efficiency**
   - What we know: D-21 requires displaying "missed N this event" per driver in the pre-race readiness view.
   - What's unclear: This requires a COUNT query over `marshal_absences` filtered by `event_id` — potentially slow if called per-row.
   - Recommendation: Use a single jOOQ query joining `marshal_absences` to the driver list with a GROUP BY, returning the count per entry. Not a per-row query.

---

## Sources

### Primary (HIGH confidence)
- Existing codebase (`app/src/main/java/...`) — EventStateMachineService, SecurityConfig, build.gradle.kts, Entry, EventClass, AbstractIntegrationTest, AdminPanelLayout
- Context7 `/stomp-js/stompjs` — STOMP client connection, subscription patterns, JWT connect headers, reconnect
- Context7 `/websites/spring_io` — WebSocketConfig pattern, SimpMessagingTemplate, ChannelInterceptor, @EnableWebSocketMessageBroker

### Secondary (MEDIUM confidence)
- `frontend/package.json` — confirmed installed dependencies, @stomp/stompjs@7.3.0
- npm registry — chart.js@4.5.1, react-chartjs-2@5.3.1, @stomp/stompjs@7.3.0 (VERIFIED via npm view)
- `.planning/phases/04-race-state-machine/04-CONTEXT.md` — locked decisions, canonical refs
- `.planning/phases/04-race-state-machine/04-HEAT-STRUCTURE-SPEC.md` — entity designs, seeding algorithm
- `.planning/phases/04-race-state-machine/04-UI-SPEC.md` — component inventory, layout contract, interaction patterns

### Tertiary (LOW confidence)
- A1–A6 in Assumptions Log — inferred from project patterns, not explicitly confirmed by official docs

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — dependencies verified against package.json and npm registry
- Architecture: HIGH — state machine pattern verified from codebase; WebSocket pattern from Spring official docs
- Pitfalls: HIGH (state machine, concurrent access, STOMP 403) / MEDIUM (Chart.js printing, jOOQ regeneration reminder)
- Schema: HIGH — follows exact patterns from V15/V16 migrations

**Research date:** 2026-04-23
**Valid until:** 2026-05-23 (stable Spring/Vite ecosystem — 30 days)
