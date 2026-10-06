# Phase 3: Admin Panel & Event Management - Pattern Map

**Mapped:** 2026-04-20
**Files analyzed:** 47 new/modified files
**Analogs found:** 43 / 47

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `api/admin/EventController.java` | controller | request-response | `api/admin/TrackController.java` | exact |
| `api/admin/EventClassController.java` | controller | request-response | `api/admin/RaceFormatController.java` | exact |
| `api/admin/ChampionshipController.java` | controller | request-response | `api/admin/TrackController.java` | exact |
| `api/admin/ClubProfileController.java` (extend) | controller | request-response | `api/admin/ClubProfileController.java` | exact |
| `api/admin/AdminEntryController.java` (extend) | controller | request-response | `api/admin/AdminEntryController.java` | exact |
| `api/admin/dto/CreateEventRequest.java` | model | request-response | `api/admin/dto/CreateTrackRequest.java` | exact |
| `api/admin/dto/EventDto.java` | model | request-response | `api/admin/dto/TrackDto.java` | exact |
| `api/admin/dto/TransitionEventRequest.java` | model | request-response | `api/admin/dto/MembershipOverrideRequest.java` | role-match |
| `api/admin/dto/ChampionshipDto.java` | model | request-response | `api/admin/dto/TrackDto.java` | exact |
| `api/admin/dto/AdminEntryDto.java` | model | request-response | `api/racer/dto/EntryDto.java` | role-match |
| `domain/event/EventService.java` | service | CRUD | `domain/track/TrackService.java` | exact |
| `domain/event/EventStateMachineService.java` | service | event-driven | `domain/entry/EntryService.java` (state logic) | role-match |
| `domain/format/EventClassService.java` | service | CRUD | `domain/format/RaceFormatService.java` | exact |
| `domain/championship/Championship.java` | model | CRUD | `domain/event/Event.java` | exact |
| `domain/championship/ChampionshipClass.java` | model | CRUD | `domain/raceclass/RacingClass.java` | exact |
| `domain/championship/ChampionshipExclusion.java` | model | CRUD | `domain/entry/EntryAuditLog.java` | role-match |
| `domain/championship/ChampionshipService.java` | service | CRUD | `domain/club/ClubProfileService.java` | exact |
| `domain/championship/ChampionshipRepository.java` | model | CRUD | `domain/event/EventRepository.java` | exact |
| `domain/car/CarTagCategoryService.java` (modify) | service | CRUD | `domain/car/CarTagCategoryService.java` | exact |
| `domain/club/ClubProfile.java` (modify) | model | CRUD | `domain/club/ClubProfile.java` | exact |
| `config/MinioConfig.java` | config | file-I/O | none in codebase | no analog |
| `query/event/AdminEventQueryService.java` | service | CRUD | `query/event/EventScheduleQuery.java` | exact |
| `query/entry/AdminEntryQueryService.java` | service | CRUD | `query/entry/EntryQueryService.java` | exact |
| `query/championship/ChampionshipStandingsQuery.java` | service | CRUD | `query/entry/EntryQueryService.java` | role-match |
| `db/migration/V15__add_track_to_events.sql` | migration | CRUD | `db/migration/V12__create_events.sql` | exact |
| `db/migration/V17__create_championships.sql` | migration | CRUD | `db/migration/V5__create_race_formats.sql` | role-match |
| `api/GlobalExceptionHandler.java` (extend) | middleware | request-response | `api/GlobalExceptionHandler.java` | exact |
| `frontend/src/pages/admin/AdminPanelLayout.tsx` | component | request-response | `frontend/src/pages/racer/RacerPortalLayout.tsx` | exact |
| `frontend/src/pages/admin/events/EventListPage.tsx` | component | CRUD | `frontend/src/pages/racer/CarsPage.tsx` | exact |
| `frontend/src/pages/admin/events/EventDetailPage.tsx` | component | CRUD | `frontend/src/pages/racer/ProfilePage.tsx` | role-match |
| `frontend/src/pages/admin/championships/ChampionshipListPage.tsx` | component | CRUD | `frontend/src/pages/racer/CarsPage.tsx` | exact |
| `frontend/src/pages/admin/championships/ChampionshipDetailPage.tsx` | component | CRUD | `frontend/src/pages/racer/ProfilePage.tsx` | role-match |
| `frontend/src/pages/admin/championships/ChampionshipStandingsTable.tsx` | component | CRUD | `frontend/src/pages/racer/EntriesPage.tsx` | role-match |
| `frontend/src/pages/admin/config/ClubProfilePage.tsx` | component | CRUD | `frontend/src/pages/racer/ProfilePage.tsx` | exact |
| `frontend/src/pages/admin/config/TracksPage.tsx` | component | CRUD | `frontend/src/pages/racer/CarsPage.tsx` | exact |
| `frontend/src/pages/admin/config/FormatsPage.tsx` | component | CRUD | `frontend/src/pages/racer/CarsPage.tsx` | role-match |
| `frontend/src/pages/admin/config/CategoriesPage.tsx` | component | CRUD | `frontend/src/pages/racer/CarsPage.tsx` | role-match |
| `frontend/src/hooks/admin/useAdminEvents.ts` | hook | CRUD | `frontend/src/hooks/racer/useCars.ts` | exact |
| `frontend/src/hooks/admin/useAdminChampionships.ts` | hook | CRUD | `frontend/src/hooks/racer/useCars.ts` | exact |
| `frontend/src/hooks/admin/useAdminEntries.ts` | hook | CRUD | `frontend/src/hooks/racer/useCars.ts` | exact |
| `frontend/src/hooks/admin/adminQueryKeys.ts` | utility | request-response | `frontend/src/hooks/racer/racerQueryKeys.ts` | exact |
| `frontend/src/lib/adminApi.ts` | utility | request-response | `frontend/src/lib/racerApi.ts` | exact |
| `frontend/src/App.tsx` (modify) | config | request-response | `frontend/src/App.tsx` | exact |

---

## Pattern Assignments

### `api/admin/EventController.java` (controller, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/TrackController.java`

**Imports pattern** (lines 1-15):
```java
package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.api.admin.dto.CreateEventRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EventDetailDto;
import dev.monkeypatch.rctiming.api.admin.dto.EventDto;
import dev.monkeypatch.rctiming.api.admin.dto.TransitionEventRequest;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateEventRequest;
import dev.monkeypatch.rctiming.domain.event.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
```

**Auth pattern** (TrackController.java lines 29-31):
```java
@RestController
@RequestMapping("/api/v1/admin/events")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
```

**Core CRUD pattern** (TrackController.java lines 35-61):
```java
@GetMapping
public List<EventDto> listEvents() {
    return eventService.findAll();
}

@GetMapping("/{id}")
public EventDetailDto getEvent(@PathVariable Long id) {
    return eventService.findById(id);
}

@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public EventDto createEvent(@RequestBody @Valid CreateEventRequest request) {
    return eventService.create(request);
}

@PutMapping("/{id}")
public EventDto updateEvent(@PathVariable Long id,
                             @RequestBody @Valid UpdateEventRequest request) {
    return eventService.update(id, request);
}
```

**State machine endpoint** (new pattern, no analog — copy intent from RaceFormatController POST pattern):
```java
// POST /api/v1/admin/events/{id}/transition
// Returns 409 if EventStateMachineService throws IllegalStateTransitionException
@PostMapping("/{id}/transition")
public EventDto transitionEvent(@PathVariable Long id,
                                 @RequestBody @Valid TransitionEventRequest request) {
    return eventService.transition(id, request.targetStatus());
}
```

---

### `api/admin/EventClassController.java` (controller, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/RaceFormatController.java`

**Auth pattern** (RaceFormatController.java lines 28-31):
```java
@RestController
@RequestMapping("/api/v1/admin/events/{eventId}/classes")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
```

**Core pattern** (modeled on RaceFormatController nested resource style):
```java
// POST /api/v1/admin/events/{eventId}/classes
// PUT  /api/v1/admin/events/{eventId}/classes/{classId}/overrides
// POST /api/v1/admin/events/{eventId}/classes/combine
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public EventClassDto addClassToEvent(@PathVariable Long eventId,
                                      @RequestBody @Valid AddEventClassRequest request) {
    return eventClassService.addClassToEvent(eventId, request);
}

@PutMapping("/{classId}/overrides")
public EventClassDto updateOverrides(@PathVariable Long eventId,
                                      @PathVariable Long classId,
                                      @RequestBody @Valid UpdateEventClassOverrideRequest request) {
    return eventClassService.updateOverrides(classId, request);
}
```

---

### `api/admin/ChampionshipController.java` (controller, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/TrackController.java`

**Auth pattern** (TrackController.java lines 27-31):
```java
@RestController
@RequestMapping("/api/v1/admin/championships")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
```

**Core CRUD pattern** — identical CRUD + two extra subresource endpoints:
```java
// POST /api/v1/admin/championships/{id}/events  — link event
// GET  /api/v1/admin/championships/{id}/standings — delegated to ChampionshipStandingsQuery
// POST /api/v1/admin/championships/{id}/exclusions — audit-logged via ChampionshipService
```

---

### `domain/event/EventService.java` (service, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/track/TrackService.java`

**Imports pattern** (TrackService.java lines 1-14):
```java
package dev.monkeypatch.rctiming.domain.event;

import dev.monkeypatch.rctiming.api.admin.dto.CreateEventRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EventDto;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateEventRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
```

**Constructor injection pattern** (TrackService.java lines 27-35):
```java
@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventStateMachineService stateMachineService;

    public EventService(EventRepository eventRepository,
                        EventStateMachineService stateMachineService) {
        this.eventRepository = eventRepository;
        this.stateMachineService = stateMachineService;
    }
```

**CRUD method pattern** (TrackService.java lines 49-73):
```java
public EventDto create(CreateEventRequest request) {
    Event event = new Event();
    event.setName(request.name());
    event.setEventDate(request.eventDate());
    event.setTrackId(request.trackId());        // nullable — EVENT-07
    Instant now = Instant.now();
    event.setCreatedAt(now);
    event.setUpdatedAt(now);
    return EventDto.from(eventRepository.save(event));
}

public EventDto update(Long id, UpdateEventRequest request) {
    Event event = getEventOrThrow(id);
    event.setName(request.name());
    event.setEventDate(request.eventDate());
    event.setTrackId(request.trackId());
    event.setUpdatedAt(Instant.now());
    return EventDto.from(eventRepository.save(event));
}

// Private helper — copy from TrackService.getTrackOrThrow (line 146-149)
private Event getEventOrThrow(Long id) {
    return eventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));
}
```

**Error handling pattern** (TrackService.java lines 69-74):
```java
public void delete(Long id) {
    if (!eventRepository.existsById(id)) {
        throw new EntityNotFoundException("Event not found: " + id);
    }
    eventRepository.deleteById(id);
}
```

---

### `domain/event/EventStateMachineService.java` (service, event-driven)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryService.java` (status validation pattern, lines 61-71)

**State validation pattern** (EntryService.java lines 65-71 as reference):
```java
// EntryService validates EventStatus on submitEntry — copy the pattern for EventStateMachine
if (event.getStatus() != EventStatus.OPEN && event.getStatus() != EventStatus.PUBLISHED) {
    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Event is not open for entries");
}
```

**State machine implementation** (new pattern, based on RESEARCH.md Pattern 1):
```java
@Service
public class EventStateMachineService {

    private static final Map<EventStatus, Set<EventStatus>> VALID_TRANSITIONS = Map.of(
        EventStatus.DRAFT,          Set.of(EventStatus.PUBLISHED),
        EventStatus.PUBLISHED,      Set.of(EventStatus.OPEN),
        EventStatus.OPEN,           Set.of(EventStatus.ENTRIES_CLOSED),
        EventStatus.ENTRIES_CLOSED, Set.of(EventStatus.IN_PROGRESS),
        EventStatus.IN_PROGRESS,    Set.of(EventStatus.COMPLETED)
    );

    public void transition(Event event, EventStatus targetStatus) {
        Set<EventStatus> valid = VALID_TRANSITIONS.getOrDefault(event.getStatus(), Set.of());
        if (!valid.contains(targetStatus)) {
            throw new IllegalStateTransitionException(
                "Cannot transition from " + event.getStatus() + " to " + targetStatus);
        }
        event.setStatus(targetStatus);
    }
}
```

Note: `IllegalStateTransitionException` is a new custom exception. Add handler to `GlobalExceptionHandler` mapping it to HTTP 409 (see Shared Patterns section below).

---

### `domain/format/EventClassService.java` (service, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/format/RaceFormatService.java`

**Snapshot copy pattern** (RaceFormatService.java lines 63-74):
```java
// Deep copy via ObjectMapper — exact pattern to replicate for config snapshot
public EventClass assignTemplateToEventClass(RaceFormatTemplate template) {
    RaceFormatConfig snapshot = objectMapper.convertValue(
            template.getConfig(),
            RaceFormatConfig.class
    );
    EventClass eventClass = new EventClass();
    eventClass.setConfigSnapshot(snapshot);
    eventClass.setTemplate(template);
    return eventClass;
}
```

**Override merge pattern** (RaceFormatService.java lines 35-56):
```java
// Merge snapshot + override — copy this pattern for getEffectiveConfig
Map<String, Object> snapshotMap = objectMapper.convertValue(
        eventClass.getConfigSnapshot(),
        new TypeReference<Map<String, Object>>() {}
);
snapshotMap.putAll(override);
return objectMapper.convertValue(snapshotMap, RaceFormatConfig.class);
```

**Phase 3 adds:** `addClassToEvent(Long eventId, Long racingClassId, Long templateId)` method that:
1. Looks up the `RaceFormatTemplate` by templateId
2. Calls `assignTemplateToEventClass(template)` to get snapshot
3. Sets `eventId` and `racingClassId` on the new `EventClass`
4. Saves and returns `EventClassDto`

---

### `domain/championship/Championship.java` (model, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/event/Event.java`

**Entity pattern** (Event.java lines 1-68):
```java
@Entity
@Table(name = "championships")
public class Championship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "best_x_from_y_x")
    private Integer bestXFromYX;   // "best X rounds"

    @Column(name = "best_x_from_y_y")
    private Integer bestXFromYY;   // "from Y rounds"

    @Column(name = "scoring_source", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ScoringSource scoringSource = ScoringSource.FINALS;

    @Column(name = "tq_bonus_points", nullable = false)
    private int tqBonusPoints = 0;

    @Column(name = "afinal_winner_bonus_points", nullable = false)
    private int afinalWinnerBonusPoints = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // getters and setters follow same pattern as Event.java
}
```

---

### `domain/championship/ChampionshipExclusion.java` (model, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryAuditLog.java`

**Audit entity pattern** (EntryAuditLog.java lines 1-64):
```java
@Entity
@Table(name = "championship_exclusions")
public class ChampionshipExclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "championship_id", nullable = false)
    private Long championshipId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(columnDefinition = "text", nullable = false)
    private String reason;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;   // admin userId — same as EntryAuditLog.adminUserId

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
```

---

### `domain/championship/ChampionshipService.java` (service, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfileService.java`

**Constructor injection and @Transactional pattern** (ClubProfileService.java lines 17-30):
```java
@Service
@Transactional
public class ChampionshipService {

    private final ChampionshipRepository championshipRepository;
    private final ChampionshipClassRepository championshipClassRepository;
    private final ChampionshipExclusionRepository exclusionRepository;

    public ChampionshipService(ChampionshipRepository championshipRepository,
                               ChampionshipClassRepository championshipClassRepository,
                               ChampionshipExclusionRepository exclusionRepository) {
        this.championshipRepository = championshipRepository;
        this.championshipClassRepository = championshipClassRepository;
        this.exclusionRepository = exclusionRepository;
    }
```

**Audit write pattern** (EntryService.java lines 192-203 for `writeAudit`):
```java
// ChampionshipService.addExclusion() writes an audit row — copy EntryService.writeAudit pattern
private void writeExclusion(Long championshipId, Long driverId, Long eventId,
                             Long adminId, String reason) {
    ChampionshipExclusion exclusion = new ChampionshipExclusion();
    exclusion.setChampionshipId(championshipId);
    exclusion.setDriverId(driverId);
    exclusion.setEventId(eventId);
    exclusion.setReason(reason);
    exclusion.setCreatedBy(adminId);
    exclusion.setCreatedAt(Instant.now());
    exclusionRepository.save(exclusion);
}
```

---

### `domain/car/CarTagCategoryService.java` (modify — soft delete)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategoryService.java` (lines 51-56)

**Current delete pattern to replace** (CarTagCategoryService.java lines 51-56):
```java
// BEFORE (hard delete — must be changed to soft delete per D-21):
public void delete(Long id) {
    if (!carTagCategoryRepository.existsById(id)) {
        throw new EntityNotFoundException("Car tag category not found: " + id);
    }
    carTagCategoryRepository.deleteById(id);
}
```

**New archive pattern** (modeled on EntryService.withdraw lines 136-147):
```java
// AFTER (soft delete — set archived = true):
public CarTagCategoryDto archive(Long id) {
    CarTagCategory category = getCategoryOrThrow(id);
    category.setArchived(true);
    return CarTagCategoryDto.from(carTagCategoryRepository.save(category));
}

// findAll() must filter archived by default:
@Transactional(readOnly = true)
public List<CarTagCategoryDto> findAll() {
    return carTagCategoryRepository.findAllByArchivedFalseOrderBySortOrderAsc().stream()
            .map(CarTagCategoryDto::from)
            .toList();
}
```

---

### `config/MinioConfig.java` (config, file-I/O)

**Analog:** None in codebase — no existing object storage or file I/O config.

**Use pattern from RESEARCH.md Pattern 6:**
```java
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .forcePathStyle(true)   // REQUIRED for MinIO
            .build();
    }
}
```

**Key:** `forcePathStyle(true)` is mandatory for MinIO. Add `@PostConstruct` to auto-create bucket on startup (see RESEARCH.md Open Question 3).

---

### `query/event/AdminEventQueryService.java` (service, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/query/event/EventScheduleQuery.java`

**Full file pattern** (EventScheduleQuery.java lines 1-57):
```java
package dev.monkeypatch.rctiming.query.event;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Events.EVENTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Tracks.TRACKS;

@Service
@Transactional(readOnly = true)
public class AdminEventQueryService {

    private final DSLContext dsl;

    public AdminEventQueryService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<AdminEventListDto> listEvents() {
        return dsl.select(
                    EVENTS.ID, EVENTS.NAME, EVENTS.EVENT_DATE, EVENTS.STATUS,
                    TRACKS.NAME.as("trackName"))
                .from(EVENTS)
                .leftJoin(TRACKS).on(TRACKS.ID.eq(EVENTS.TRACK_ID))
                .orderBy(EVENTS.EVENT_DATE.desc())
                .fetch(r -> new AdminEventListDto(
                        r.get(EVENTS.ID),
                        r.get(EVENTS.NAME),
                        r.get(EVENTS.EVENT_DATE),
                        EventStatus.valueOf(r.get(EVENTS.STATUS)),
                        r.get("trackName", String.class)
                ));
    }
}
```

**Seam rule:** This service uses jOOQ DSL — do NOT inject `EventRepository` (Hibernate). Matches seam in EventScheduleQuery.java.

---

### `query/entry/AdminEntryQueryService.java` (service, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/query/entry/EntryQueryService.java`

**Full file pattern** (EntryQueryService.java lines 1-49):
```java
@Service
@Transactional(readOnly = true)
public class AdminEntryQueryService {

    private final DSLContext dsl;

    public AdminEntryQueryService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<AdminEntryProjection> findEntriesForClass(Long eventId, Long eventClassId) {
        return dsl.select(
                    ENTRIES.ID,
                    ENTRIES.USER_ID,
                    ENTRIES.TRANSPONDER_NUMBER,   // snapshot column
                    ENTRIES.STATUS,
                    ENTRIES.SUBMITTED_AT,
                    ENTRIES.WITHDRAWN_AT)
                .from(ENTRIES)
                .join(USERS).on(USERS.ID.eq(ENTRIES.USER_ID))
                .where(ENTRIES.EVENT_ID.eq(eventId))
                  .and(ENTRIES.EVENT_CLASS_ID.eq(eventClassId))
                .orderBy(ENTRIES.SUBMITTED_AT.asc())
                .fetch(r -> new AdminEntryProjection(/* ... */));
    }
}
```

**Join pattern** (EntryQueryService.java lines 23-47 — join EVENTS, extend to join USERS for racer name):
```java
// Note: EntryQueryService joins ENTRIES → EVENTS
// AdminEntryQueryService joins ENTRIES → USERS (for racer first/last name display)
.join(USERS).on(USERS.ID.eq(ENTRIES.USER_ID))
```

---

### `query/championship/ChampionshipStandingsQuery.java` (service, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/query/entry/EntryQueryService.java`

**Constructor and @Transactional pattern** (EntryQueryService.java lines 14-21):
```java
@Service
@Transactional(readOnly = true)
public class ChampionshipStandingsQuery {

    private final DSLContext dsl;

    public ChampionshipStandingsQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    // Phase 3: returns empty list — Phase 7 provides result data
    public List<StandingsRowDto> queryStandings(Long championshipId, Long classId) {
        return List.of();
    }
}
```

**Note:** Scaffold the query structure now; Phase 7 fills in the join to `race_results`. Do not join race_results in Phase 3 (table does not exist yet).

---

### Flyway Migration Pattern

**Analog:** `app/src/main/resources/db/migration/V12__create_events.sql`

**Migration style** (V12 lines 1-17):
```sql
-- V15: Add track_id FK to events (EVENT-07) and racing_class_id to event_classes

ALTER TABLE events
    ADD COLUMN track_id BIGINT REFERENCES tracks(id) ON DELETE SET NULL;

ALTER TABLE event_classes
    ADD COLUMN racing_class_id BIGINT REFERENCES racing_classes(id) ON DELETE SET NULL;

CREATE INDEX idx_events_track_id ON events(track_id);
CREATE INDEX idx_event_classes_racing_class_id ON event_classes(racing_class_id);
```

**Championship migration style** (V5 tables pattern for new tables):
```sql
-- V17: Create championship tables (CHAMP-01 through CHAMP-09)

CREATE TABLE championships (
    id                         BIGSERIAL    PRIMARY KEY,
    name                       VARCHAR(255) NOT NULL,
    best_x_from_y_x            INT,
    best_x_from_y_y            INT,
    scoring_source             VARCHAR(20)  NOT NULL DEFAULT 'FINALS'
                               CHECK (scoring_source IN ('QUALIFYING','FINALS','BOTH')),
    tq_bonus_points            INT          NOT NULL DEFAULT 0,
    afinal_winner_bonus_points INT          NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- ... + championship_classes, championship_event_links,
--       championship_points_scale (position INT, points INT),
--       championship_exclusions
```

**Order constraint:** Run `./gradlew :app:generateJooq` after each migration before writing jOOQ query services that reference the new tables.

---

### `frontend/src/pages/admin/AdminPanelLayout.tsx` (component, request-response)

**Analog:** `frontend/src/pages/racer/RacerPortalLayout.tsx`

**Full layout pattern** (RacerPortalLayout.tsx lines 1-81):
```typescript
import { Outlet, NavLink } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
// ... lucide icons

export default function AdminPanelLayout() {
  const { logout } = useAuth();
  // navItems include grouped sections — use Separator from shadcn/ui between groups

  return (
    <div className="min-h-screen flex bg-background">

      {/* Desktop left sidebar — md+ (240px fixed) */}
      <aside className="hidden md:flex flex-col fixed inset-y-0 w-60 border-r bg-sidebar">
        <div className="p-4 font-semibold border-b">Admin Panel</div>
        <nav className="flex-1 p-2 space-y-1 overflow-y-auto">
          {/* Events & Competitions group */}
          <p className="px-2 py-1 text-xs text-muted-foreground font-medium uppercase tracking-wide">
            Events & Competitions
          </p>
          {eventsNavItems.map(({ to, label, Icon }) => (
            <NavLink key={to} to={to} className={/* active/inactive classes */}>
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
          <Separator className="my-2" />
          {/* Configuration group */}
          ...
        </nav>
        <button onClick={logout} ...>Log out</button>
      </aside>

      {/* Content — ml-60 on desktop to clear sidebar */}
      <main className="flex-1 md:ml-60 pb-16 md:pb-0 p-4 md:p-6">
        <Outlet />
      </main>

      {/* Mobile bottom nav — IDENTICAL to RacerPortalLayout (lines 50-78) */}
      <nav className="fixed bottom-0 inset-x-0 flex md:hidden border-t bg-background z-10 h-14">
        {/* Show top 4 most important admin items + logout */}
        ...
      </nav>
    </div>
  );
}
```

**Key difference from RacerPortalLayout:** Desktop uses left `<aside>` (not top `<nav>`); main content has `md:ml-60`. Mobile bottom nav is identical in structure to RacerPortalLayout (lines 50-78).

---

### `frontend/src/pages/admin/events/EventListPage.tsx` (component, CRUD)

**Analog:** `frontend/src/pages/racer/CarsPage.tsx`

**Page structure pattern** (CarsPage.tsx lines 1-64):
```typescript
import { useState } from 'react';
import { Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useAdminEvents } from '@/hooks/admin/useAdminEvents';

export default function EventListPage() {
  const { data: events, isPending, error } = useAdminEvents();

  // Loading skeleton pattern (CarsPage.tsx lines 17-30)
  if (isPending) {
    return (
      <div aria-live="polite" className="max-w-5xl mx-auto">
        <div className="animate-pulse bg-muted rounded h-8 w-24" />
        ...
      </div>
    );
  }
  if (error) {
    return <div role="alert" className="text-destructive">Unable to load events.</div>;
  }

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-semibold">Events</h1>
        <Button onClick={() => navigate('/admin/events/new')}>
          <Plus className="mr-2 h-4 w-4" />Create Event
        </Button>
      </div>

      {/* Table using shadcn table component (must install: npx shadcn@latest add table) */}
      {/* Each row: name, date, status Badge with D-06 colour mapping */}
      {/* Badge variant mapping: DRAFT=secondary, PUBLISHED=default, OPEN=success,
          ENTRIES_CLOSED=warning, IN_PROGRESS=destructive, COMPLETED=outline */}
    </div>
  );
}
```

---

### `frontend/src/pages/admin/events/EventDetailPage.tsx` (component, CRUD)

**Analog:** `frontend/src/pages/racer/ProfilePage.tsx`

**Form + mutation pattern** (ProfilePage.tsx lines 39-90):
```typescript
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import { useAdminEvent, useUpdateEvent, useTransitionEvent } from '@/hooks/admin/useAdminEvents';
import { useQueryClient } from '@tanstack/react-query';
import { adminQueryKeys } from '@/hooks/admin/adminQueryKeys';

// Status badge + action buttons pattern (D-04):
// Only render buttons for valid next states — no invalid transitions shown
function EventActionButtons({ event }: { event: EventDto }) {
  const transition = useTransitionEvent();
  const qc = useQueryClient();

  const handleTransition = async (targetStatus: string, needsConfirm: boolean) => {
    if (needsConfirm) {
      // Use shadcn Dialog for confirmation (D-05)
    }
    try {
      await transition.mutateAsync({ eventId: event.id, targetStatus });
      toast.success('Event status updated');
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 409) {
        qc.invalidateQueries({ queryKey: adminQueryKeys.event(event.id) });
        toast.error('Transition rejected — page has been refreshed');
      }
    }
  };
  // Return buttons only for valid next states per current event.status
}
```

**Error pattern** (ProfilePage.tsx lines 79-89):
```typescript
} catch (err) {
  if (isAxiosError(err) && err.response?.status === 400) {
    const errors = err.response.data?.errors as Record<string, string> | undefined;
    if (errors) {
      Object.entries(errors).forEach(([field, msg]) =>
        form.setError(field as keyof EventForm, { message: msg }));
      return;
    }
  }
  toast.error('Failed to save. Please try again.', { duration: 8000 });
}
```

---

### `frontend/src/pages/admin/config/ClubProfilePage.tsx` (component, CRUD)

**Analog:** `frontend/src/pages/racer/ProfilePage.tsx`

**Form with sections pattern** (ProfilePage.tsx lines 133-210 — Card + CardContent + Form + FormField):
```typescript
// Copy Card > CardHeader > CardTitle + CardContent > Form > FormField pattern exactly
// Add logo upload section using <input type="file" accept="image/png,image/svg+xml">
// Logo upload calls PUT /api/v1/admin/club/logo (multipart/form-data)
// On success: invalidate adminQueryKeys.clubProfile
// Display current logo via <img src={profile.logoUrl} /> if logoUrl is set
```

**Loading skeleton pattern** (ProfilePage.tsx lines 120-128):
```typescript
if (isPending) {
  return (
    <div aria-live="polite" className="max-w-2xl mx-auto space-y-8">
      <div className="animate-pulse bg-muted rounded h-48" />
      <div className="animate-pulse bg-muted rounded h-32" />
    </div>
  );
}
```

---

### `frontend/src/pages/admin/config/FormatsPage.tsx` (component, CRUD)

**Analog:** `frontend/src/pages/racer/CarsPage.tsx`

**Type-switching form pattern** (D-19 — new pattern, no direct analog):
```typescript
// Use Zod discriminated union for format type validation (from RESEARCH.md Code Examples):
const formatSchema = z.discriminatedUnion('type', [timedSchema, bumpUpSchema, pointsFinalsSchema]);

// Type selector triggers field switch with tw-animate-css animate-in fade-in slide-in-from-top-2:
{formatType === 'TIMED' && (
  <div className="animate-in fade-in slide-in-from-top-2">
    {/* TIMED-specific fields */}
  </div>
)}
```

---

### `frontend/src/hooks/admin/useAdminEvents.ts` (hook, CRUD)

**Analog:** `frontend/src/hooks/racer/useCars.ts`

**Full hook pattern** (useCars.ts lines 1-37):
```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchAdminEvents, createEvent, updateEvent, transitionEvent,
         type AdminEventListDto, type CreateEventRequest } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useAdminEvents() {
  return useQuery<AdminEventListDto[]>({
    queryKey: adminQueryKeys.events,
    queryFn: fetchAdminEvents,
  });
}

export function useCreateEvent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: CreateEventRequest) => createEvent(req),
    onSettled: () => qc.invalidateQueries({ queryKey: adminQueryKeys.events }),
  });
}

export function useTransitionEvent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ eventId, targetStatus }: { eventId: number; targetStatus: string }) =>
      transitionEvent(eventId, targetStatus),
    onSuccess: (_, { eventId }) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.event(eventId) });
    },
    // 409 handled at call site (EventDetailPage) — not here
  });
}
```

---

### `frontend/src/hooks/admin/adminQueryKeys.ts` (utility, request-response)

**Analog:** `frontend/src/hooks/racer/racerQueryKeys.ts`

**Key factory pattern** (racerQueryKeys.ts lines 1-9):
```typescript
export const adminQueryKeys = {
  events:            ['admin', 'events'] as const,
  event:             (id: number) => ['admin', 'events', id] as const,
  championships:     ['admin', 'championships'] as const,
  championship:      (id: number) => ['admin', 'championships', id] as const,
  championshipStandings: (id: number) => ['admin', 'championships', id, 'standings'] as const,
  entries:           (eventId: number, classId: number) =>
                       ['admin', 'entries', eventId, classId] as const,
  clubProfile:       ['admin', 'club'] as const,
  tracks:            ['admin', 'tracks'] as const,
  formats:           ['admin', 'formats'] as const,
  categories:        ['admin', 'categories'] as const,
};
```

Note: racerQueryKeys uses simple arrays. adminQueryKeys needs factory functions for entity-specific keys (e.g., `event(id)`) because detail pages need targeted invalidation.

---

### `frontend/src/lib/adminApi.ts` (utility, request-response)

**Analog:** `frontend/src/lib/racerApi.ts`

**API module pattern** (racerApi.ts lines 1-93):
```typescript
import api from './api';   // reuse the same axios instance with JWT interceptor

// DTOs — copy interface pattern from racerApi.ts
export interface AdminEventListDto {
  id: number;
  name: string;
  eventDate: string;
  status: EventStatus;
  trackName: string | null;
}

// API call functions — copy arrow function pattern
export const fetchAdminEvents = () =>
  api.get<AdminEventListDto[]>('/api/v1/admin/events').then(r => r.data);

export const createEvent = (req: CreateEventRequest) =>
  api.post<AdminEventListDto>('/api/v1/admin/events', req).then(r => r.data);

export const transitionEvent = (eventId: number, targetStatus: string) =>
  api.post(`/api/v1/admin/events/${eventId}/transition`, { targetStatus })
     .then(r => r.data);

// Logo upload — multipart/form-data (different from JSON calls)
export const uploadClubLogo = (file: File) => {
  const form = new FormData();
  form.append('file', file);
  return api.put('/api/v1/admin/club/logo', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};
```

---

### `frontend/src/App.tsx` (modify — rewire admin routes)

**Analog:** `frontend/src/App.tsx` (lines 38-44)

**Current admin route** (App.tsx lines 38-44):
```typescript
// BEFORE:
{
  path: '/admin/*',
  element: (
    <ProtectedRoute roles={['ADMIN', 'RACE_DIRECTOR', 'REFEREE']}>
      <AdminPlaceholderPage />
    </ProtectedRoute>
  ),
},
```

**New admin route** (modeled on racer portal nested route pattern, lines 45-60):
```typescript
// AFTER — same ProtectedRoute wrapper, AdminPanelLayout replaces AdminPlaceholderPage:
{
  path: '/admin',
  element: (
    <ProtectedRoute roles={['ADMIN', 'RACE_DIRECTOR', 'REFEREE']}>
      <AdminPanelLayout />
    </ProtectedRoute>
  ),
  children: [
    { index: true, element: <Navigate to="/admin/events" replace /> },
    { path: 'events', element: <EventListPage /> },
    { path: 'events/:id', element: <EventDetailPage /> },
    { path: 'championships', element: <ChampionshipListPage /> },
    { path: 'championships/:id', element: <ChampionshipDetailPage /> },
    { path: 'config/tracks', element: <TracksPage /> },
    { path: 'config/formats', element: <FormatsPage /> },
    { path: 'config/club', element: <ClubProfilePage /> },
    { path: 'config/categories', element: <CategoriesPage /> },
  ],
},
```

---

### Integration Tests (controller IT files)

**Analog:** `app/src/test/java/dev/monkeypatch/rctiming/api/admin/TrackControllerIT.java`

**Full test class pattern** (TrackControllerIT.java lines 1-253):

**Imports block** (lines 1-32):
```java
package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
```

**@BeforeEach setUp pattern** (lines 47-56):
```java
@BeforeEach
void setUp() {
    String email = "admin-event-" + UUID.randomUUID() + "@test.com";
    createAdminUser(email, "adminPass123");
    ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
            "/api/v1/auth/login",
            new LoginRequest(email, "adminPass123"),
            AuthResponse.class);
    assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    adminToken = loginResp.getBody().accessToken();
}
```

**Test method pattern** (lines 59-68):
```java
@Test
void createEvent_asAdmin_returns201() {
    ResponseEntity<EventDto> response = restTemplate.exchange(
            "/api/v1/admin/events", HttpMethod.POST,
            new HttpEntity<>(Map.of("name", "Spring Meeting 2026", "eventDate", "2026-05-10"),
                             adminHeaders()),
            EventDto.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
}
```

**State machine 409 test** (new test pattern — use AdminEntryControllerIT role-check tests as structural guide):
```java
@Test
void transitionEvent_invalidTransition_returns409() {
    // Create event (DRAFT), attempt to transition directly to IN_PROGRESS
    EventDto created = createEvent();
    var resp = restTemplate.exchange(
            "/api/v1/admin/events/" + created.id() + "/transition",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("targetStatus", "IN_PROGRESS"), adminHeaders()),
            String.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
}
```

**Helpers pattern** (TrackControllerIT.java lines 233-252):
```java
private HttpHeaders adminHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
}

private void createAdminUser(String email, String password) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setFirstName("Admin");
    user.setLastName("Test");
    user.setRoles(Set.of(Role.ADMIN));
    Instant now = Instant.now();
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    userRepository.save(user);
}
```

---

## Shared Patterns

### Authentication — All Admin Controllers
**Source:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/TrackController.java` lines 27-31
**Apply to:** Every new `api/admin/*Controller.java`
```java
@RestController
@RequestMapping("/api/v1/admin/...")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
```

### Error Handling — HTTP 409 for State Machine
**Source:** `app/src/main/java/dev/monkeypatch/rctiming/api/GlobalExceptionHandler.java`
**Apply to:** `GlobalExceptionHandler.java` (extend) + `EventStateMachineService.java`
```java
// Add new exception class (no analog — new):
// domain/event/IllegalStateTransitionException.java
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String message) { super(message); }
}

// Add handler to GlobalExceptionHandler.java after line 63:
@ExceptionHandler(IllegalStateTransitionException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public ProblemDetail handleStateTransition(IllegalStateTransitionException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
}
```

### Entity Not Found — All Services
**Source:** `app/src/main/java/dev/monkeypatch/rctiming/domain/track/TrackService.java` lines 146-149
**Apply to:** All new `domain/*/` services
```java
private Championship getChampionshipOrThrow(Long id) {
    return championshipRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Championship not found: " + id));
}
```

### Transactional Read-Only — jOOQ Services
**Source:** `app/src/main/java/dev/monkeypatch/rctiming/query/event/EventScheduleQuery.java` lines 13-14
**Apply to:** All `query/**/*QueryService.java` and `query/**/*Query.java`
```java
@Service
@Transactional(readOnly = true)
```

### TanStack Query v5 Mutation — Frontend
**Source:** `frontend/src/hooks/racer/useCars.ts` lines 15-21
**Apply to:** All `frontend/src/hooks/admin/use*.ts`
```typescript
const qc = useQueryClient();
return useMutation({
  mutationFn: ...,
  onSettled: () => qc.invalidateQueries({ queryKey: adminQueryKeys.xxx }),
});
```

### Sonner Toast — Frontend
**Source:** `frontend/src/pages/racer/ProfilePage.tsx` lines 78-89 (import + usage)
**Apply to:** All admin page components that call mutations
```typescript
import { toast } from 'sonner';
// success:
toast.success('Event published successfully');
// error with 400 field errors:
toast.error('Failed to save. Please try again.', { duration: 8000 });
// 409 specific (state machine):
toast.error('Transition rejected — page has been refreshed');
```

### Confirm Dialog Pattern — Destructive Transitions (D-05)
**Source:** `frontend/src/components/ui/dialog` (shadcn, already installed)
**Apply to:** `EventDetailPage.tsx` for OPEN→ENTRIES_CLOSED and IN_PROGRESS→COMPLETED transitions
```typescript
// Use shadcn AlertDialog (not Dialog) for confirmation modals:
import { AlertDialog, AlertDialogAction, AlertDialogCancel,
         AlertDialogContent, AlertDialogDescription, AlertDialogFooter,
         AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from '@/components/ui/alert-dialog';
```

### Loading Skeleton — All List Pages
**Source:** `frontend/src/pages/racer/CarsPage.tsx` lines 17-31
**Apply to:** All `*ListPage.tsx` and `*DetailPage.tsx`
```typescript
if (isPending) {
  return (
    <div aria-live="polite" className="max-w-5xl mx-auto">
      <div className="animate-pulse bg-muted rounded h-8 w-24" />
    </div>
  );
}
if (error) {
  return <div role="alert" className="text-destructive">Unable to load [resource].</div>;
}
```

### JSONB Snapshot — EventClass Config
**Source:** `app/src/main/java/dev/monkeypatch/rctiming/domain/format/EventClass.java` lines 23-36
**Apply to:** Any new entity with JSONB columns (no new entities need JSONB in Phase 3 beyond existing)
```java
@Type(JsonType.class)
@Column(name = "config_snapshot", columnDefinition = "jsonb", nullable = false)
private RaceFormatConfig configSnapshot;
```

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `config/MinioConfig.java` | config | file-I/O | No object storage or external service config exists in codebase |
| `domain/event/IllegalStateTransitionException.java` | utility | request-response | No custom exception classes exist yet; all errors use standard exceptions |
| `domain/championship/ScoringSource.java` | model | CRUD | No enums with semantic scoring logic exist yet (closest: `EventStatus` — same enum shape) |

---

## Metadata

**Analog search scope:**
- `app/src/main/java/dev/monkeypatch/rctiming/api/admin/` (all 6 controllers + 15 DTOs)
- `app/src/main/java/dev/monkeypatch/rctiming/domain/` (all 20 service/entity/repository files)
- `app/src/main/java/dev/monkeypatch/rctiming/query/` (all 3 query services)
- `app/src/main/java/dev/monkeypatch/rctiming/api/GlobalExceptionHandler.java`
- `app/src/main/resources/db/migration/` (all 14 migration files)
- `app/src/test/java/dev/monkeypatch/rctiming/` (all 13 IT test files)
- `frontend/src/pages/racer/` (all 5 page components + layout)
- `frontend/src/hooks/racer/` (3 hook files + key factory)
- `frontend/src/lib/` (api.ts + racerApi.ts)
- `frontend/src/App.tsx`

**Files scanned:** 62
**Pattern extraction date:** 2026-04-20
