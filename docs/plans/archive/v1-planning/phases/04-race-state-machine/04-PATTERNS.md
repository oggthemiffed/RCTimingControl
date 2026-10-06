# Phase 4: Race Control - Pattern Map

**Mapped:** 2026-04-23
**Files analyzed:** 38 new/modified files
**Analogs found:** 34 / 38

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `domain/race/Race.java` | model | CRUD | `domain/event/Event.java` | exact |
| `domain/race/Round.java` | model | CRUD | `domain/event/Event.java` | exact |
| `domain/race/RaceEntry.java` | model | CRUD | `domain/entry/Entry.java` | exact |
| `domain/race/RaceStatus.java` | model | — | `domain/event/EventStatus.java` | exact |
| `domain/race/RoundStatus.java` | model | — | `domain/event/EventStatus.java` | exact |
| `domain/race/RoundType.java` | model | — | `domain/event/EventStatus.java` | exact |
| `domain/race/RaceStateMachineService.java` | service | request-response | `domain/event/EventStateMachineService.java` | exact |
| `domain/race/MarshalAdjustment.java` | model | CRUD | `domain/entry/EntryAuditLog.java` | exact |
| `domain/race/MarshalAbsence.java` | model | CRUD | `domain/entry/EntryAuditLog.java` | role-match |
| `domain/race/IncidentReport.java` | model | CRUD | `domain/entry/EntryAuditLog.java` | role-match |
| `domain/race/Penalty.java` | model | CRUD | `domain/entry/EntryAuditLog.java` | role-match |
| `domain/race/UnknownTransponderLink.java` | model | CRUD | `domain/entry/EntryAuditLog.java` | role-match |
| `timing/LapPassingEvent.java` | model | event-driven | `domain/entry/EntryAuditLog.java` | partial |
| `timing/LiveRaceState.java` | utility | event-driven | none — no in-memory concurrent model exists | none |
| `timing/LiveRacePosition.java` | utility | event-driven | none | none |
| `timing/LapTimingService.java` | service | event-driven | `domain/entry/EntryService.java` | partial |
| `timing/LiveTimingHub.java` | service | event-driven | none — no STOMP broadcast exists yet | none |
| `service/RoundGeneratorService.java` | service | CRUD | `domain/event/EventService.java` | role-match |
| `service/SyntheticTimingService.java` | service | event-driven | none | none |
| `service/ResultSnapshotService.java` | service | CRUD | `domain/entry/EntryService.java` | role-match |
| `websocket/WebSocketConfig.java` | config | request-response | `security/SecurityConfig.java` | partial |
| `security/WebSocketJwtChannelInterceptor.java` | middleware | request-response | `security/JwtAuthenticationFilter.java` | role-match |
| `security/SecurityConfig.java` (modified) | config | request-response | `security/SecurityConfig.java` | exact |
| `api/racecontrol/RaceControlController.java` | controller | request-response | `api/admin/EventController.java` | exact |
| `api/racecontrol/RefereeController.java` | controller | request-response | `api/admin/EventController.java` | exact |
| `api/racecontrol/DevTimingController.java` | controller | request-response | `api/admin/EventController.java` | role-match |
| `api/racecontrol/dto/*.java` | model | request-response | `api/admin/dto/*.java` | exact |
| `query/race/RaceQueryService.java` | service | CRUD | `query/event/AdminEventQueryService.java` | exact |
| `frontend/src/pages/race-control/RaceControlLayout.tsx` | component | request-response | `pages/admin/AdminPanelLayout.tsx` | exact |
| `frontend/src/pages/race-control/CockpitPage.tsx` | component | request-response | `pages/admin/events/EventDetailPage.tsx` | role-match |
| `frontend/src/pages/race-control/RefereePage.tsx` | component | request-response | `pages/admin/events/EventDetailPage.tsx` | role-match |
| `frontend/src/pages/race-control/PrintResultsPage.tsx` | component | request-response | none — no print page exists | none |
| `frontend/src/pages/race-control/panels/*.tsx` | component | request-response | `pages/admin/events/EventDetailPage.tsx` | role-match |
| `frontend/src/hooks/race-control/raceControlQueryKeys.ts` | utility | request-response | `hooks/admin/adminQueryKeys.ts` | exact |
| `frontend/src/hooks/race-control/useRaceControl.ts` | hook | request-response | `hooks/admin/useAdminEvents.ts` | exact |
| `frontend/src/hooks/race-control/useRaceStateMutations.ts` | hook | request-response | `hooks/admin/useAdminEvents.ts` | exact |
| `frontend/src/hooks/race-control/useStomp.ts` | hook | event-driven | none — no STOMP hook exists | none |
| `frontend/src/lib/raceControlApi.ts` | utility | request-response | `lib/adminApi.ts` | exact |

---

## Pattern Assignments

### `domain/race/Race.java` (model, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/event/Event.java`

**Imports pattern** (lines 1–12):
```java
package dev.monkeypatch.rctiming.domain.race;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
```

**Core entity pattern** (lines 17–74):
```java
@Entity
@Table(name = "races")
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RaceStatus status = RaceStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // getters/setters follow the Event.java pattern: one getter + one setter per field
    public RaceStatus getStatus() { return status; }
    public void setStatus(RaceStatus status) { this.status = status; }
    // ...
}
```

Note: Use `FetchType.LAZY` on any `@ManyToOne` as in `EventClass.java` lines 34–36 — do not eager-load associations.

---

### `domain/race/RaceStateMachineService.java` (service, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/event/EventStateMachineService.java`

This is a direct copy-and-adapt of the existing pattern. Copy the entire file structure exactly.

**Full pattern** (lines 1–34):
```java
package dev.monkeypatch.rctiming.domain.race;

import org.springframework.stereotype.Service;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

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

**Key notes:**
- `IllegalStateTransitionException` is already in `domain/event/` — import it from there. Do not create a second copy.
- `GlobalExceptionHandler` at `api/GlobalExceptionHandler.java` lines 47–51 already maps this exception to HTTP 409. No changes needed to the handler.

---

### `domain/race/MarshalAdjustment.java` (model, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryAuditLog.java`

**Core entity pattern** (lines 1–64). Audit-trail entities follow this exact layout: `@Entity`, `@Table`, all `@Column` with explicit names, `Instant` timestamps, plain getters/setters. No `@PrePersist`, no Lombok.

```java
@Entity
@Table(name = "marshal_adjustments")
public class MarshalAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "race_id", nullable = false)
    private Long raceId;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "transponder_number", nullable = false, length = 20)
    private String transponderNumber;

    @Column(name = "lap_delta", nullable = false)
    private int lapDelta;  // +1 or -1

    @Column(name = "race_state_at_time", nullable = false, length = 20)
    private String raceStateAtTime;

    @Column(name = "acting_user_id", nullable = false)
    private Long actingUserId;

    @Column(name = "acting_user_name", nullable = false, length = 200)
    private String actingUserName;

    @Column(name = "adjusted_at", nullable = false)
    private Instant adjustedAt;

    // getters/setters following EntryAuditLog.java pattern
}
```

Apply the same pattern for `MarshalAbsence`, `IncidentReport`, `Penalty`, and `UnknownTransponderLink` — all audit-trail entities follow `EntryAuditLog.java`.

---

### `domain/race/RaceStatus.java`, `RoundStatus.java`, `RoundType.java` (model enums)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/event/EventStatus.java`

Simple enum, no annotations, one file per enum:
```java
package dev.monkeypatch.rctiming.domain.race;

public enum RaceStatus {
    PENDING, GRID, RUNNING, STOPPED, FINISHED;
}
```

---

### `service/RoundGeneratorService.java` (service, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/event/EventService.java`

**Imports and class scaffold** (lines 1–18):
```java
package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@Transactional
public class RoundGeneratorService {
    // Constructor injection — no @Autowired field injection (EventService.java lines 18–21 pattern)
}
```

**Error handling pattern** (lines 62–64 in EventService.java):
```java
private Round getRoundOrThrow(Long id) {
    return roundRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Round not found: " + id));
}
```

---

### `api/racecontrol/RaceControlController.java` (controller, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/EventController.java`

**Full imports and class scaffold** (lines 1–41):
```java
package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.api.racecontrol.dto.RaceDto;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/race-control")
@PreAuthorize("hasRole('RACE_DIRECTOR')")
public class RaceControlController {
    // Constructor injection of services
    // Methods: @GetMapping, @PostMapping, @ResponseStatus(HttpStatus.CREATED or NO_CONTENT)
}
```

**State transition endpoint pattern** (EventController.java lines 68–72):
```java
@PostMapping("/races/{id}/transition")
public RaceDto transitionRace(@PathVariable Long id,
                               @RequestBody @Valid TransitionRaceRequest request) {
    return raceControlService.transition(id, request.targetStatus());
}
```

**Auth guard pattern** (line 28): class-level `@PreAuthorize("hasRole('RACE_DIRECTOR')")`. For `RefereeController`, use `@PreAuthorize("hasRole('REFEREE')")`.

---

### `api/racecontrol/DevTimingController.java` (controller, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/EventController.java`

The `@Profile("dev")` annotation is the key additional element — the rest of the controller pattern is identical:
```java
@RestController
@RequestMapping("/api/v1/dev/race")
@Profile("dev")
@PreAuthorize("hasRole('RACE_DIRECTOR')")
public class DevTimingController {

    @PostMapping("/{raceId}/synthetic-passing")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void fireSyntheticPassing(@PathVariable Long raceId) {
        syntheticTimingService.fireSyntheticPassing(raceId);
    }
}
```

---

### `api/racecontrol/dto/*.java` (model, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/dto/*.java`

All DTOs use Java records. Example from `CreateEventRequest.java`:
```java
public record RaceDto(Long id, Long roundId, RaceStatus status, Integer heatNumber, Instant createdAt) {
    public static RaceDto from(Race race) {
        return new RaceDto(race.getId(), race.getRoundId(), race.getStatus(),
                           race.getHeatNumber(), race.getCreatedAt());
    }
}
```

Request DTOs use `jakarta.validation` constraints as in `CreateEventRequest`:
```java
public record TransitionRaceRequest(@NotNull RaceStatus targetStatus) {}
```

---

### `query/race/RaceQueryService.java` (service, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/query/event/AdminEventQueryService.java`

**Full pattern** (lines 1–38):
```java
package dev.monkeypatch.rctiming.query.race;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Races.RACES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Rounds.ROUNDS;

@Service
@Transactional(readOnly = true)
public class RaceQueryService {

    private final DSLContext dsl;

    public RaceQueryService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<RunOrderRowDto> getRunOrder(Long eventId) {
        return dsl.select(RACES.ID, RACES.STATUS, ROUNDS.TYPE, ROUNDS.ROUND_NUMBER, RACES.HEAT_NUMBER)
                .from(RACES)
                .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
                .where(ROUNDS.EVENT_ID.eq(eventId))
                .orderBy(RACES.SEQUENCE_IN_ROUND)
                .fetch(r -> new RunOrderRowDto(...));
    }
}
```

Note: jOOQ generated classes (`Tables.RACES`, `Tables.ROUNDS`) only exist after running `./gradlew :app:generateJooq` post-migration. The query module must never use Hibernate entities — use jOOQ projections only.

---

### `security/SecurityConfig.java` (modified — config, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java`

Add two lines to the existing `authorizeHttpRequests` block (lines 29–36):
```java
.requestMatchers("/ws/timing").permitAll()          // WebSocket upgrade — JWT validated at STOMP CONNECT
.requestMatchers("/api/v1/race-control/**").hasRole("RACE_DIRECTOR")
.requestMatchers("/api/v1/referee/**").hasRole("REFEREE")
.requestMatchers("/api/v1/dev/**").hasRole("RACE_DIRECTOR")
```

The `/ws/timing` must be `permitAll()` because the HTTP upgrade happens before STOMP-level JWT validation in the `ChannelInterceptor`.

---

### `security/WebSocketJwtChannelInterceptor.java` (middleware, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/security/JwtAuthenticationFilter.java`

The `JwtAuthenticationFilter` is the HTTP-layer analog. The STOMP-layer interceptor follows the same JWT extraction logic but operates on STOMP CONNECT frames:

**JWT extraction from JwtAuthenticationFilter** (lines 37–53):
```java
String header = request.getHeader("Authorization");
if (header != null && header.startsWith("Bearer ")) {
    String token = header.substring(7);
    try {
        Claims claims = jwtTokenService.parseToken(token);
        List<String> roles = claims.get("roles", List.class);
        List<GrantedAuthority> authorities = roles.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(
                claims.getSubject(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    } catch (JwtException e) {
        // Invalid token — do not set context
    }
}
```

Apply the same extraction logic in `WebSocketJwtChannelInterceptor.preSend()`, replacing `request.getHeader("Authorization")` with `accessor.getFirstNativeHeader("Authorization")` and replacing `SecurityContextHolder.getContext().setAuthentication(auth)` with `accessor.setUser(auth)`.

---

### `websocket/WebSocketConfig.java` (config, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java` (structural only)

No direct codebase analog exists for `@EnableWebSocketMessageBroker`. The config class structure (constructor injection, `@Configuration`, one `@Bean` method pattern) mirrors `SecurityConfig.java`. The STOMP-specific content comes from RESEARCH.md Pattern 2.

Key constraint from CLAUDE.md: **no `.withSockJS()`** on the endpoint registration.

---

### `frontend/src/pages/race-control/RaceControlLayout.tsx` (component, request-response)

**Analog:** `frontend/src/pages/admin/AdminPanelLayout.tsx`

The cockpit layout uses the same two-part mobile pattern as `AdminPanelLayout.tsx` but the cockpit is a fixed two-column split (not a scrollable page). Copy these structural elements:

**Mobile Sheet drawer pattern** (AdminPanelLayout.tsx lines 150–157):
```tsx
<Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
  <SheetContent side="left" showCloseButton className="w-72 p-0">
    <SheetHeader className="sr-only">
      <SheetTitle>Navigation</SheetTitle>
    </SheetHeader>
    <SidebarContent onNavClick={() => setSheetOpen(false)} />
  </SheetContent>
</Sheet>
```

**Mobile top bar with hamburger** (AdminPanelLayout.tsx lines 137–148): copy the `<header>` pattern with `Button variant="ghost" size="icon-sm"` triggering the Sheet.

**useAuth pattern** (AdminPanelLayout.tsx line 67): `const { user, logout } = useAuth();`

The cockpit layout differs from AdminPanelLayout in that the two-column split (`left 30% / right 70%`) is the main content structure, not a sidebar + scrollable content area. Use `flex h-screen overflow-hidden` on the outer container.

---

### `frontend/src/pages/race-control/CockpitPage.tsx` (component, request-response)

**Analog:** `frontend/src/pages/admin/events/EventDetailPage.tsx`

**Confirmation dialog pattern** (EventDetailPage.tsx lines 353–375):
```tsx
<Dialog open={confirmOpen} onOpenChange={setConfirmOpen}>
  <DialogContent>
    <DialogHeader>
      <DialogTitle>{confirmCopy.title}</DialogTitle>
      <DialogDescription>{confirmCopy.body}</DialogDescription>
    </DialogHeader>
    <DialogFooter>
      <Button variant="outline" onClick={() => setConfirmOpen(false)}>Cancel</Button>
      <Button
        variant={confirmCopy.destructive ? 'destructive' : 'default'}
        onClick={() => void confirmTransition()}
        disabled={transitionMutation.isPending}
      >
        {transitionMutation.isPending ? (
          <Loader2 className="h-4 w-4 mr-1 animate-spin" />
        ) : null}
        {confirmCopy.confirmLabel}
      </Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

**HTTP 409 handling pattern** (EventDetailPage.tsx lines 164–170):
```tsx
if (axios.isAxiosError(err) && err.response?.status === 409) {
  void refetch();
  toast.error('This transition is no longer valid. Refresh the page to see the current race status.');
} else {
  toast.error('Command failed. Check your connection and try again.');
}
```

**State-dependent rendering pattern** (EventDetailPage.tsx lines 216–218): compute `validNextStatuses` or `currentPanelContent` from `data.status` before the JSX block, then use a switch/map in the return.

---

### `frontend/src/hooks/race-control/raceControlQueryKeys.ts` (utility, request-response)

**Analog:** `frontend/src/hooks/admin/adminQueryKeys.ts`

**Full pattern** (lines 1–35). Copy the nested object structure exactly:
```typescript
export const raceControlQueryKeys = {
  runOrder: {
    all: (eventId: number) => ['race-control', 'run-order', eventId] as const,
  },
  race: {
    detail: (raceId: number) => ['race-control', 'races', raceId] as const,
    liveState: (raceId: number) => ['race-control', 'races', raceId, 'live-state'] as const,
    results: (raceId: number) => ['race-control', 'races', raceId, 'results'] as const,
    marshalAdjustments: (raceId: number) =>
      ['race-control', 'races', raceId, 'marshal-adjustments'] as const,
  },
  marshal: {
    absences: (eventId: number) => ['race-control', 'marshal', 'absences', eventId] as const,
  },
};
```

---

### `frontend/src/hooks/race-control/useRaceControl.ts` (hook, request-response)

**Analog:** `frontend/src/hooks/admin/useAdminEvents.ts`

**Query pattern** (useAdminEvents.ts lines 13–26):
```typescript
export function useRunOrder(eventId: number) {
  return useQuery({
    queryKey: raceControlQueryKeys.runOrder.all(eventId),
    queryFn: () => raceControlApi.getRunOrder(eventId),
    enabled: Number.isFinite(eventId) && eventId > 0,
  });
}
```

**Mutation pattern with cache invalidation** (useAdminEvents.ts lines 47–55):
```typescript
export function useTransitionRace(raceId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (targetStatus: RaceStatus) => raceControlApi.transitionRace(raceId, targetStatus),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: raceControlQueryKeys.race.detail(raceId) });
      qc.invalidateQueries({ queryKey: raceControlQueryKeys.runOrder.all(/* eventId */) });
    },
  });
}
```

---

### `frontend/src/lib/raceControlApi.ts` (utility, request-response)

**Analog:** `frontend/src/lib/adminApi.ts`

**File structure pattern** (adminApi.ts lines 1–420):
- Types section at top (all `export interface`/`export type`)
- `export const raceControlApi = { ... }` object at bottom
- All methods: `api.get<T>(url).then(r => r.data)` or `api.post<T>(url, body).then(r => r.data)`
- Import `api` from `'./api'` (the Axios instance with JWT interceptor)

```typescript
import api from './api';

export type RaceStatus = 'PENDING' | 'GRID' | 'RUNNING' | 'STOPPED' | 'FINISHED';

export interface RaceDto {
  id: number;
  roundId: number;
  status: RaceStatus;
  heatNumber: number;
  // ...
}

export const raceControlApi = {
  getRunOrder: (eventId: number) =>
    api.get<RunOrderRowDto[]>(`/api/v1/race-control/events/${eventId}/run-order`).then(r => r.data),

  transitionRace: (raceId: number, targetStatus: RaceStatus) =>
    api.post<RaceDto>(`/api/v1/race-control/races/${raceId}/transition`, { targetStatus })
       .then(r => r.data),

  applyMarshalAdjustment: (raceId: number, body: MarshalAdjustmentRequest) =>
    api.post<MarshalAdjustmentDto>(`/api/v1/race-control/races/${raceId}/marshal`, body)
       .then(r => r.data),
};
```

---

### `frontend/src/App.tsx` (modified — config, request-response)

**Analog:** `frontend/src/App.tsx` (existing file)

Add race control routes following the exact structure of the existing `/admin` block (lines 47–64):
```tsx
{
  path: '/race-control',
  element: (
    <ProtectedRoute roles={['RACE_DIRECTOR', 'REFEREE']}>
      <RaceControlLayout />
    </ProtectedRoute>
  ),
  children: [
    { index: true, element: <CockpitPage /> },
    { path: 'referee', element: <RefereePage /> },
    { path: 'results/:raceId/print', element: <PrintResultsPage /> },
  ],
},
```

The `PrintResultsPage` has no layout wrapper — it is a standalone full-page print view opened in a new tab via `window.open(...)`.

---

### Integration test files

**Analog:** `app/src/test/java/dev/monkeypatch/rctiming/api/admin/EventControllerIT.java`

**Full test structure** (lines 1–237). All integration tests follow this exact pattern:
- Extend `AbstractIntegrationTest` (no `@SpringBootTest` annotation on the test class — inherited)
- `@Autowired TestRestTemplate restTemplate`
- `@Autowired` of repositories needed for test data setup
- `@BeforeEach` creates a unique user via repository + logs in via `/api/v1/auth/login` → stores token
- Test methods use `restTemplate.exchange(url, method, new HttpEntity<>(body, headers), ResponseType.class)`
- Assertions use AssertJ: `assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)`
- Private `adminHeaders()` / `directorHeaders()` helper that builds `HttpHeaders` with `Bearer` token

**Unit test pattern:** `domain/format/RaceFormatServiceTest.java` for services with no Spring context. Use `@ExtendWith(MockitoExtension.class)`, `@Mock` for dependencies, `@InjectMocks` for the service under test.

---

## Shared Patterns

### Authentication guard
**Source:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/EventController.java` line 28
**Apply to:** All new controllers
```java
// Class-level for uniform enforcement:
@PreAuthorize("hasRole('RACE_DIRECTOR')")   // RaceControlController
@PreAuthorize("hasRole('REFEREE')")          // RefereeController
```
Individual method overrides only where a method needs a different role (e.g., read-only endpoint accessible to both roles: `@PreAuthorize("hasAnyRole('RACE_DIRECTOR','REFEREE')")`).

### HTTP 409 error handling (frontend)
**Source:** `frontend/src/pages/admin/events/EventDetailPage.tsx` lines 164–170
**Apply to:** All cockpit and referee mutations that invoke state transitions
```typescript
if (axios.isAxiosError(err) && err.response?.status === 409) {
  void refetch();
  toast.error('...');
}
```

### Toast notifications
**Source:** `frontend/src/pages/admin/events/EventDetailPage.tsx` lines 162–169
**Apply to:** All mutation success/error handlers in race control hooks
```typescript
import { toast } from 'sonner';
// success: toast.success('Race started');
// error:   toast.error('Command failed. Check your connection and try again.');
```

### EntityNotFoundException → 404
**Source:** `app/src/main/java/dev/monkeypatch/rctiming/api/GlobalExceptionHandler.java` lines 23–27
**Apply to:** All service methods — throw `EntityNotFoundException` for missing records; `GlobalExceptionHandler` handles it automatically.
```java
.orElseThrow(() -> new EntityNotFoundException("Race not found: " + id))
```

### @Transactional on services
**Source:** `app/src/main/java/dev/monkeypatch/rctiming/domain/event/EventService.java` lines 13–14
**Apply to:** All new domain services
```java
@Service
@Transactional          // write-side services
// or
@Transactional(readOnly = true)  // query-side services
```

### TanStack Query mutation pattern
**Source:** `frontend/src/hooks/admin/useAdminEvents.ts` lines 47–55
**Apply to:** All race control hooks
```typescript
return useMutation({
  mutationFn: (body: T) => raceControlApi.someMethod(body),
  onSuccess: () => {
    qc.invalidateQueries({ queryKey: raceControlQueryKeys.race.detail(raceId) });
  },
});
```

### Loading/error states (frontend)
**Source:** `frontend/src/pages/admin/events/EventDetailPage.tsx` lines 196–214
**Apply to:** All cockpit panels that depend on query data
```tsx
if (isLoading) {
  return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
    </div>
  );
}
if (isError || !data) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center gap-4">
      <p className="text-muted-foreground">Failed to load.</p>
      <Button variant="outline" onClick={() => void refetch()}>Retry</Button>
    </div>
  );
}
```

---

## No Analog Found

Files with no close match in the codebase (planner should reference RESEARCH.md patterns):

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `timing/LiveRaceState.java` | utility | event-driven | No in-memory concurrent race model exists. See RESEARCH.md Pattern 4 for the `ConcurrentHashMap<Long, LiveRaceState>` held in `LapTimingService`. |
| `timing/LiveRacePosition.java` | utility | event-driven | Value object with no analog. Simple POJO: fields for position int, laps int, lastPassingTime Instant, bestLapMs Long, gapToLeaderMs Long. |
| `timing/LiveTimingHub.java` | service | event-driven | No STOMP broadcast service exists. See RESEARCH.md Pattern 5 for `SimpMessagingTemplate.convertAndSend` pattern. |
| `service/SyntheticTimingService.java` | service | event-driven | No synthetic data generator exists. See RESEARCH.md `SyntheticTimingService` pattern and `@Profile("dev")` usage. |
| `websocket/WebSocketConfig.java` | config | request-response | No WebSocket config exists. See RESEARCH.md Pattern 2 for `@EnableWebSocketMessageBroker` full config (no SockJS). |
| `frontend/src/pages/race-control/PrintResultsPage.tsx` | component | request-response | No print page exists. Static React page with `@media print` CSS, `window.print()` on mount, Chart.js position chart rendered as `<img>` via `chart.toBase64Image()`. |
| `frontend/src/hooks/race-control/useStomp.ts` | hook | event-driven | No STOMP hook exists. See RESEARCH.md Pattern 6. Use `@stomp/stompjs` `Client` class; activate on mount, deactivate on cleanup. |

---

## Metadata

**Analog search scope:** `app/src/main/java/dev/monkeypatch/rctiming/`, `frontend/src/`
**Files scanned:** 94 Java source files, 68 TypeScript/TSX files
**Pattern extraction date:** 2026-04-23
