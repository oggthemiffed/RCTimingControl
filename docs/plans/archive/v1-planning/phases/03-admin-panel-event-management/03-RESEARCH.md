# Phase 03: Admin Panel & Event Management - Research

**Researched:** 2026-04-20
**Domain:** Spring Boot admin REST APIs + React admin panel (event management, championship config, Phase 1 deferred config UI, MinIO logo upload)
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Admin Panel Layout**
- D-01: Left sidebar navigation on desktop, 240px fixed width
- D-02: Same mobile polish as racer portal — sidebar collapses on mobile via hamburger (Sheet), bottom nav bar on small screens (same pattern as RacerPortalLayout.tsx)
- D-03: Sidebar grouped: "Events & Competitions" (Events, Championships) | "Configuration" (Tracks, Race Formats, Club Profile, Car Tag Categories)

**Event State Machine UX**
- D-04: Status badge + valid-action buttons on event detail page only; invalid transitions never shown in UI; HTTP 409 is last-resort guard
- D-05: Confirm dialog before OPEN → ENTRIES_CLOSED and IN_PROGRESS → COMPLETED; simple forward transitions proceed without dialog
- D-06: Event list: table with name, date, colour-coded status badge (grey=DRAFT, blue=PUBLISHED, green=OPEN, amber=ENTRIES_CLOSED, red=IN_PROGRESS, black=COMPLETED)

**Event Class & Entry Management**
- D-07: Inline class management on event detail page; "+ Add Class" picks racing class + format template; format overrides via "Edit" on each class row
- D-08: CLASS combining via checkbox multi-select + "Combine into Shared Race" button; confirmation dialog; no drag-and-drop
- D-09: ENTRY-02 entries nested under event detail (no top-level Entries nav); class row expands/navigates to class entry list; admin can withdraw on behalf of racer; no bulk actions v1

**Championship Configuration**
- D-10: One championship covers multiple racing classes; separate standings per class
- D-11: Championship classes inherit defaults with optional per-class overrides
- D-12: Events linked to championship explicitly via picker; event classes auto-matched to championship classes by RacingClass entity
- D-13: Points scale: editable table, ROAR/BRCA preset buttons (exact values in UI-SPEC), no JSON import
- D-14: Scoring source: radio buttons (Qualifying / Finals / Both)
- D-15: TQ bonus (CHAMP-07) and A-final winner bonus (CHAMP-08): number fields on championship form
- D-16: CHAMP-09 driver exclusions managed on championship standings page; audit-logged; standings recalculate immediately
- D-17: Standings table: drop scores greyed (opacity-40), best-X highlighted (font-semibold), DNF/DNS/DQ as Badge labels not "0"

**Phase 1 Deferred Admin Config Forms**
- D-18: All deferred config forms (club, tracks, format templates, car tag categories) built to racer-portal quality — React Hook Form + Zod, inline errors, loading states
- D-19: Race format template form: type-specific dynamic fields (dropdown selects TIMED/BUMP_UP/POINTS_FINALS; form fields switch with animate-in/fade-in/slide-in-from-top-2)
- D-20: Track form: basic info + Decoder/Loop Config section (lap time thresholds, loop name, transponder frequency)
- D-21: Car tag categories: add, rename, archive (soft delete — no hard delete); archived hidden from new entries, preserved historically
- D-22: Club profile includes logo upload via MinIO (Docker, S3-compatible); AWS S3 SDK in app pointing at MinIO endpoint in dev; `docker-compose.yml` gets MinIO service

### Claude's Discretion
- Exact sidebar width and collapse behaviour (hamburger animation, overlay vs push)
- Admin panel route structure under `/admin/` (e.g., `/admin/events`, `/admin/events/:id`, `/admin/championships/:id`)
- Format override edit UI (inline field overrides or a diff-style view)
- Points scale preset list (ROAR, BRCA — exact default values)
- MinIO bucket naming and upload path conventions

### Deferred Ideas (OUT OF SCOPE)
- Public championship standings (CHAMP-05) — Phase 7
- Payment-gated entry confirmation — v2
- Admin bulk entry actions — post-v1
- Real S3 migration from MinIO — operational, not a v1 code change
- Multi-decoder support (TRACK-04 multi-decoder is post-v1)

</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| EVENT-01 | Admin can create an event with name, date, venue | New `EventService` (write-side), `EventController`, Flyway migration to add `track_id` FK |
| EVENT-02 | Admin can add racing classes to an event and assign a race format to each class | `EventClassService`, existing `EventClass` entity already has `config_snapshot` + `config_override` columns |
| EVENT-05 | Events follow a state machine DRAFT → PUBLISHED → OPEN → ENTRIES_CLOSED → IN_PROGRESS → COMPLETED; invalid transitions rejected | New `EventStateMachineService`, HTTP 409 on invalid transition via existing `GlobalExceptionHandler` |
| EVENT-06 | Admin can combine two or more classes into a single race (run together, score separately) | `combined_race_id` FK column on `event_classes` via Flyway migration |
| EVENT-07 | Admin associates event with a configured track | `track_id` FK on `events` via Flyway migration; track lap thresholds apply automatically |
| ENTRY-02 | Admin can view and manage all entries per event and per class | New `GET /api/v1/admin/events/{id}/classes/{classId}/entries`, `POST /{entryId}/withdraw` endpoints using existing `EntryService` |
| CHAMP-01 | Admin can configure a championship with "best X from Y rounds" scoring | New `Championship` entity + `ChampionshipService`, Flyway migration, `ChampionshipController` |
| CHAMP-02 | Championship scoring handles DNF, DNS, DQ correctly | `ChampionshipResultType` enum in championship domain; scoring logic in `ChampionshipScoringService` (jOOQ read side) |
| CHAMP-03 | Separate standings per racing class | `ChampionshipClass` entity (championship_id, racing_class_id, per-class config overrides); jOOQ query groups by class |
| CHAMP-04 | Admin can configure points scale per championship | `championship_points_scale` table (position → points JSONB or rows); editable inline in frontend |
| CHAMP-06 | Championship can score from qualifying, finals, or both | `scoring_source` enum column on `championships` (QUALIFYING / FINALS / BOTH) |
| CHAMP-07 | Championship can award TQ bonus per class per round | `tq_bonus_points` int column on `championships` (or per-class override) |
| CHAMP-08 | Championship can award A-final winner bonus per class per round | `afinal_winner_bonus_points` int column on `championships` |
| CHAMP-09 | Individual driver exclusions per round, audit-logged | `championship_exclusions` table (championship_id, driver_id, event_id, reason, created_by, created_at) |
| CHAMP-10 | Standings: best-X rounds highlighted, drop scores greyed, DNF/DNS/DQ as labels | jOOQ scoring projection; frontend standings table with cell styling |

</phase_requirements>

---

## Summary

Phase 3 is the largest frontend phase in the project: it delivers the full admin panel (replacing `AdminPlaceholderPage.tsx`), all event and championship management, the Phase 1 deferred config forms, and MinIO logo upload. The backend has substantial scaffolding already: all Phase 1 entities exist (`Event`, `EventClass`, `EventStatus`, `RaceFormatTemplate`, `RaceFormatConfig` sealed interface, `ClubProfile`, `Track`, `CarTagCategory`, `Entry`), all admin controllers from Phase 1 are implemented, and the security layer correctly gates `/api/v1/admin/**` to ADMIN/RACE_DIRECTOR/REFEREE roles.

What Phase 3 adds on the backend: `EventService` (create/update/state-machine transitions), `EventController`, `EventClassService` (add class to event + format override), `Championship` entity hierarchy (Championship + ChampionshipClass + ChampionshipExclusion + PointsScale), `ChampionshipController`, admin-scoped entry management endpoints, a `championship_exclusions` audit table, and Flyway migrations for the new columns (`events.track_id`, `event_classes.combined_race_id`, new championship tables). MinIO is added to `docker-compose.yml` with Spring AWS S3 SDK wired to it for logo upload (`PUT /api/v1/admin/club/logo`).

On the frontend: `AdminPanelLayout.tsx` (adapted from `RacerPortalLayout.tsx` — left sidebar on desktop, Sheet + bottom nav on mobile), all admin page components under `frontend/src/pages/admin/`, hooks under `frontend/src/hooks/admin/`, admin API client functions in `frontend/src/lib/adminApi.ts`. The shadcn components required (`table`, `tabs`, `checkbox`, `radio-group`, `switch`) are not yet installed and must be added with `npx shadcn add`.

**Primary recommendation:** Build backend-first by domain (Events → Championships → deferred config forms), then build frontend top-down (layout → list pages → detail pages → forms). The Hibernate/jOOQ seam must be strictly maintained: write operations go through JPA services, all admin list/detail read projections go through new jOOQ query services.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Event CRUD + state machine | API / Backend (Spring) | — | State transitions are business logic; HTTP 409 enforced server-side |
| Event class assignment + format override | API / Backend (Spring) | — | Snapshot copy at assignment time is a domain rule (FORMAT-06) |
| Entry view/withdraw by admin | API / Backend (Spring) | — | Withdraw is an audited state mutation |
| Championship entity management | API / Backend (Spring) | — | Championship config is structured domain data |
| Championship standings calculation | API / Backend — jOOQ read side | — | Aggregation across multiple events/rounds; never done client-side |
| Admin panel navigation + routing | Frontend (React) | — | SPA routes under /admin/* |
| Event status badge + action buttons | Frontend (React) | — | UI reflects server state; buttons trigger mutation, no client-side state machine |
| Points scale editable table | Frontend (React) | — | Inline editing of a simple array; submits full array to API on save |
| Logo upload | API / Backend (Spring → MinIO) | Frontend multipart form | Spring handles multipart upload, delegates to MinIO via S3 SDK |
| Club profile form | Frontend (React) → API | — | Form posts to existing ClubProfileController; logo upload is separate endpoint |
| Format template type-switching form | Frontend (React) | API validation | Client switches field sets; server validates sealed type on POST |

---

## Standard Stack

### Core (already installed — verified in build.gradle.kts and package.json)

| Library | Version | Purpose | Status |
|---------|---------|---------|--------|
| Spring Boot | 3.4.x | Backend framework | [VERIFIED: build.gradle.kts] |
| Java | 21 | Runtime | [VERIFIED: build.gradle.kts toolchain] |
| Spring Data JPA + Hibernate 6 | via Spring BOM | Write-side entity mutations | [VERIFIED: build.gradle.kts] |
| jOOQ | 3.19.24 | Read-side projections (standings, entry lists) | [VERIFIED: build.gradle.kts] |
| Flyway | via Spring BOM | Schema migrations | [VERIFIED: build.gradle.kts] |
| Hypersistence Utils | 3.9.11 | JSONB mapping (config_snapshot, config_override) | [VERIFIED: build.gradle.kts] |
| JJWT | 0.12.6 | JWT auth | [VERIFIED: build.gradle.kts] |
| React 19 | 19.2.4 | Frontend framework | [VERIFIED: package.json] |
| TanStack Query v5 | 5.99.0 | Server state management | [VERIFIED: package.json] |
| React Hook Form | 7.72.1 | Form management | [VERIFIED: package.json] |
| Zod | 3.25.76 | Schema validation | [VERIFIED: package.json] |
| shadcn/ui | 4.2.0 | Component library | [VERIFIED: package.json] |
| Tailwind CSS v4 | 4.2.2 | Styling | [VERIFIED: package.json] |
| tw-animate-css | 1.4.0 | Animate-in classes for format type-switching form | [VERIFIED: package.json] |
| lucide-react | 1.8.0 | Icons | [VERIFIED: package.json] |
| axios | 1.15.0 | HTTP client | [VERIFIED: package.json] |
| sonner | 2.0.7 | Toast notifications | [VERIFIED: package.json] |

### New Dependencies for Phase 3

| Library | Purpose | Install Command |
|---------|---------|-----------------|
| `software.amazon.awssdk:s3` | MinIO logo upload via S3 SDK | Gradle: `implementation("software.amazon.awssdk:s3:2.27.x")` |
| `software.amazon.awssdk:bom` | AWS SDK BOM for version alignment | [ASSUMED — verify latest 2.x BOM version] |

**shadcn components to install (not yet in frontend/src/components/ui/):**
```bash
npx shadcn@latest add table tabs checkbox radio-group switch
```
[VERIFIED: package.json shows these are absent; UI-SPEC specifies they are required]

**Docker Compose addition (MinIO):**
```yaml
minio:
  image: minio/minio:latest
  container_name: rctiming-minio
  command: server /data --console-address ":9001"
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin
  ports:
    - "9000:9000"   # S3 API
    - "9001:9001"   # Web console
  volumes:
    - miniodata:/data
```
[ASSUMED — standard MinIO Docker config; verify `minio/minio` image tag for stability]

---

## Architecture Patterns

### System Architecture Diagram

```
Browser (Admin SPA)
  │  /admin/* routes — AdminPanelLayout.tsx
  │  TanStack Query mutations + queries → adminApi.ts (axios + JWT Bearer)
  │
  ▼
Spring Boot (API Layer)
  │
  ├── EventController            POST /api/v1/admin/events
  │                              GET  /api/v1/admin/events
  │                              GET  /api/v1/admin/events/:id
  │                              PUT  /api/v1/admin/events/:id
  │                              POST /api/v1/admin/events/:id/transition
  │
  ├── EventClassController       POST /api/v1/admin/events/:id/classes
  │                              PUT  /api/v1/admin/events/:id/classes/:classId/overrides
  │                              POST /api/v1/admin/events/:id/classes/combine
  │
  ├── AdminEntryController       GET  /api/v1/admin/events/:id/classes/:classId/entries
  │  (extend existing)           POST /api/v1/admin/entries/:entryId/withdraw
  │
  ├── ChampionshipController     POST /api/v1/admin/championships
  │                              GET  /api/v1/admin/championships
  │                              GET  /api/v1/admin/championships/:id
  │                              PUT  /api/v1/admin/championships/:id
  │                              POST /api/v1/admin/championships/:id/events
  │                              GET  /api/v1/admin/championships/:id/standings
  │                              POST /api/v1/admin/championships/:id/exclusions
  │
  ├── ClubProfileController      PUT  /api/v1/admin/club/logo  (new multipart endpoint)
  │  (extend existing)
  │
  ├── RaceFormatController       (already complete)
  ├── TrackController            (already complete)
  ├── CarTagCategoryController   (already complete)
  │
  ├── Domain Write Side (Hibernate JPA)
  │   ├── EventService           — state machine transitions, entity mutations
  │   ├── EventClassService      — add class, snapshot config, apply override
  │   ├── ChampionshipService    — CRUD, class associations, exclusions
  │   └── LogoUploadService      — multipart → MinIO via S3 SDK
  │
  └── Query Read Side (jOOQ)
      ├── AdminEventQueryService     — event list projection, event detail projection
      ├── AdminEntryQueryService     — entries per event/class with racer name join
      └── ChampionshipStandingsQuery — standings calculation with drop-score logic
                │
                ▼
          PostgreSQL 16
          ├── events (+ track_id FK — Phase 3 migration)
          ├── event_classes (+ combined_race_id — Phase 3 migration)
          ├── entries (existing)
          ├── championships (new)
          ├── championship_classes (new)
          ├── championship_event_links (new)
          ├── championship_points_scale (new)
          └── championship_exclusions (new)

MinIO (S3-compatible)
  ← Spring app via AWS S3 SDK (endpoint: http://localhost:9000 in dev)
  ← Logo stored as club-logos/{clubId}/logo.{ext}
```

### Recommended Project Structure (new files only)

```
app/src/main/java/dev/monkeypatch/rctiming/
├── api/admin/
│   ├── EventController.java                       (new)
│   ├── EventClassController.java                  (new)
│   ├── ChampionshipController.java                (new)
│   └── dto/
│       ├── CreateEventRequest.java                (new)
│       ├── UpdateEventRequest.java                (new)
│       ├── EventDto.java                          (new)
│       ├── EventDetailDto.java                    (new)
│       ├── EventClassDto.java                     (new)
│       ├── AddEventClassRequest.java              (new)
│       ├── UpdateEventClassOverrideRequest.java   (new)
│       ├── CombineClassesRequest.java             (new)
│       ├── TransitionEventRequest.java            (new)
│       ├── AdminWithdrawEntryRequest.java         (new)
│       ├── AdminEntryDto.java                     (new)
│       ├── CreateChampionshipRequest.java         (new)
│       ├── ChampionshipDto.java                   (new)
│       ├── ChampionshipClassDto.java              (new)
│       ├── AddChampionshipEventRequest.java       (new)
│       ├── ChampionshipStandingsDto.java          (new)
│       ├── CreateExclusionRequest.java            (new)
│       └── PointsScaleEntryDto.java               (new)
├── domain/
│   ├── event/
│   │   ├── EventService.java                      (new)
│   │   └── EventStateMachineService.java          (new — or merged into EventService)
│   ├── format/
│   │   ├── EventClassService.java                 (new)
│   │   └── PointsFinalsConfig.java                (exists)
│   └── championship/
│       ├── Championship.java                      (new)
│       ├── ChampionshipClass.java                 (new)
│       ├── ChampionshipEventLink.java             (new)
│       ├── ChampionshipPointsScaleEntry.java      (new — or store as JSONB array)
│       ├── ChampionshipExclusion.java             (new)
│       ├── ScoringSource.java                     (new enum: QUALIFYING, FINALS, BOTH)
│       ├── ChampionshipRepository.java            (new)
│       ├── ChampionshipClassRepository.java       (new)
│       ├── ChampionshipEventLinkRepository.java   (new)
│       ├── ChampionshipExclusionRepository.java   (new)
│       └── ChampionshipService.java               (new)
├── query/
│   ├── event/
│   │   ├── AdminEventListDto.java                 (new)
│   │   ├── AdminEventDetailDto.java               (new)
│   │   └── AdminEventQueryService.java            (new)
│   ├── entry/
│   │   ├── AdminEntryProjection.java              (new)
│   │   └── AdminEntryQueryService.java            (new)
│   └── championship/
│       ├── StandingsRowDto.java                   (new)
│       ├── RoundResultDto.java                    (new)
│       └── ChampionshipStandingsQuery.java        (new)
└── config/
    └── MinioConfig.java                           (new — S3 client bean)

app/src/main/resources/db/migration/
├── V15__add_track_to_events.sql                  (new)
├── V16__add_combined_race_to_event_classes.sql   (new)
├── V17__create_championships.sql                 (new)
├── V18__add_entry_confirmed_withdrawn_at.sql     (new, if columns missing)
└── V19__add_minio_logo_url_to_club.sql           (new — stores MinIO object key/URL)

frontend/src/
├── pages/admin/
│   ├── AdminPanelLayout.tsx                      (new — adapt RacerPortalLayout.tsx)
│   ├── events/
│   │   ├── EventListPage.tsx                     (new)
│   │   ├── EventDetailPage.tsx                   (new)
│   │   └── EventClassSection.tsx                 (new — inline class rows + override form)
│   ├── championships/
│   │   ├── ChampionshipListPage.tsx              (new)
│   │   ├── ChampionshipDetailPage.tsx            (new)
│   │   └── ChampionshipStandingsTable.tsx        (new)
│   ├── config/
│   │   ├── TracksPage.tsx                        (new)
│   │   ├── FormatsPage.tsx                       (new)
│   │   ├── ClubProfilePage.tsx                   (new)
│   │   └── CategoriesPage.tsx                    (new)
│   └── AdminPlaceholderPage.tsx                  (delete — replaced by AdminPanelLayout)
├── hooks/admin/
│   ├── useAdminEvents.ts                         (new)
│   ├── useAdminChampionships.ts                  (new)
│   ├── useAdminEntries.ts                        (new)
│   ├── adminQueryKeys.ts                         (new)
│   └── useAdminTracks.ts / useAdminFormats.ts etc (new — one per config domain)
└── lib/
    └── adminApi.ts                               (new — admin API call functions)
```

### Pattern 1: Event State Machine (HTTP 409 on invalid transition)

**What:** `EventStateMachineService` encapsulates the valid transition graph. Controller calls it; service throws `IllegalStateTransitionException` on invalid transition. `GlobalExceptionHandler` maps it to HTTP 409.

**Example (service):**
```java
// Source: [ASSUMED — matches existing GlobalExceptionHandler pattern + EventStatus enum]
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

**GlobalExceptionHandler already handles `IllegalArgumentException` with 400.** Add a new `IllegalStateTransitionException` mapped to 409. [VERIFIED: GlobalExceptionHandler.java exists]

### Pattern 2: EventClass Format Snapshot + Override

**What:** When admin assigns a format template to an event class, take a snapshot of the current template config and store in `config_snapshot`. Override fields stored separately in `config_override` as `Map<String, Object>`. Effective config = merge at read time.

**Already established in Phase 1 (D-13)** — `EventClass` entity already has both columns. Phase 3 adds the `EventClassService` that performs the snapshot copy and the merge.

```java
// Source: [VERIFIED: EventClass.java — columns exist]
@Transactional
public EventClass addClassToEvent(Long eventId, Long racingClassId, Long templateId) {
    RaceFormatTemplate template = formatTemplateRepository.findById(templateId)
        .orElseThrow(() -> new EntityNotFoundException("Template not found"));
    EventClass ec = new EventClass();
    ec.setEventId(eventId);
    ec.setRacingClassId(racingClassId);
    ec.setTemplate(template);
    ec.setConfigSnapshot(template.getConfig()); // snapshot at assignment time — FORMAT-06
    ec.setConfigOverride(null);
    return eventClassRepository.save(ec);
}
```

**Gap found:** `EventClass` entity currently has no `racingClassId` field — it only has `eventId`, `template`, `configSnapshot`, `configOverride`. The `EventClass.java` file must be extended with a `racing_class_id` FK column and corresponding Flyway migration.

### Pattern 3: Championship Scoring (jOOQ read side)

**What:** Championship standings are a read-only projection calculated on demand from result snapshots (Phase 7 records final results). In Phase 3, standings UI exists but shows empty/placeholder until Phase 7 provides result data. The scoring query structure (jOOQ) should be scaffolded now.

```java
// Source: [ASSUMED — jOOQ pattern matching existing EntryQueryService + EventScheduleQuery]
@Service
@Transactional(readOnly = true)
public class ChampionshipStandingsQuery {

    private final DSLContext dsl;

    // Returns per-driver standings with per-round scores, applying best-X-from-Y logic
    public List<StandingsRowDto> queryStandings(Long championshipId, Long classId) {
        // Phase 3: returns empty list (no race results yet)
        // Phase 7: joins race_results table
        return List.of();
    }
}
```

### Pattern 4: AdminPanelLayout (adapt from RacerPortalLayout.tsx)

**What:** Left sidebar on desktop (fixed 240px, grouped nav items with Separator), top bar with hamburger + Sheet on mobile, fixed bottom nav bar on mobile. Route structure under `/admin/*`.

```typescript
// Source: [VERIFIED: RacerPortalLayout.tsx — exact pattern to adapt]
// Key difference: RacerPortalLayout uses horizontal top nav on md+
// AdminPanelLayout uses vertical left sidebar on md+
// Mobile pattern is IDENTICAL: bottom nav + hamburger Sheet

// Desktop:
// <div className="md:flex">
//   <aside className="hidden md:flex md:w-60 flex-col fixed inset-y-0 border-r bg-sidebar">
//     <nav>...</nav>
//   </aside>
//   <main className="md:pl-60 flex-1">
//     <Outlet />
//   </main>
// </div>
```

### Pattern 5: TanStack Query v5 Mutation for State Transitions

```typescript
// Source: [VERIFIED: package.json shows TanStack Query 5.99.0]
// Pattern established in Phase 2 racer portal mutations

const transitionEvent = useMutation({
  mutationFn: ({ eventId, targetStatus }: TransitionParams) =>
    adminApi.transitionEvent(eventId, targetStatus),
  onSuccess: (_, { eventId }) => {
    queryClient.invalidateQueries({ queryKey: adminQueryKeys.event(eventId) });
    toast.success('Event status updated');
  },
  onError: (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 409) {
      queryClient.invalidateQueries({ queryKey: adminQueryKeys.event(eventId) });
      toast.error('Transition rejected — page has been refreshed');
    }
  },
});
```

### Pattern 6: MinIO Logo Upload

**Backend — Spring AWS S3 SDK to MinIO:**
```java
// Source: [ASSUMED — standard AWS SDK v2 S3Client configuration for MinIO]
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
            .region(Region.US_EAST_1) // MinIO ignores region
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .forcePathStyle(true) // REQUIRED for MinIO
            .build();
    }
}
```

**Key: `forcePathStyle(true)` is required for MinIO** — without it, the SDK uses virtual-hosted-style URLs which MinIO does not support in default config. [ASSUMED — standard MinIO + AWS SDK pattern; not verified against current MinIO docs in this session]

### Anti-Patterns to Avoid

- **Calling Hibernate in the query module:** Championship standings and admin entry list projections go through `DSLContext` (jOOQ), not `EntryRepository` or `EventRepository`. The seam must be maintained.
- **Performing state machine transitions in the controller:** Transition logic belongs in `EventStateMachineService`, not in `EventController`. The controller calls the service and maps exceptions to HTTP codes.
- **Lazy loading across the Hibernate/jOOQ boundary:** `EventClass.template` is `FetchType.LAZY`. Code in the query module must never trigger Hibernate lazy loads. Always fetch what you need in the jOOQ query.
- **Hard-deleting car tag categories:** D-21 specifies soft delete (archived flag). The existing `CarTagCategoryService.delete()` uses hard delete — Phase 3 must change this to set an `archived` flag and add a Flyway migration adding the column.
- **Storing club logo as `bytea` in PostgreSQL:** The existing `ClubProfile.logo` field is a `byte[]` (bytea). Phase 3 decision D-22 replaces this with MinIO object storage. The migration must add a `logo_url` column (VARCHAR) for the MinIO object key and the controller must use the new approach. The old `bytea` column can remain nullable for backward compatibility.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| S3-compatible object storage | Custom file system or DB blob storage | MinIO + AWS S3 SDK v2 | MinIO is production-grade, Docker-native; S3 SDK handles multipart, retries, ACLs |
| Form field validation | Custom validation logic | React Hook Form + Zod discriminated unions | The `RaceFormatConfig` sealed interface maps directly to a Zod discriminated union |
| State machine invalid transition HTTP status | Custom error mapping | `GlobalExceptionHandler` + new `IllegalStateTransitionException` → 409 | Already proven pattern in project |
| jOOQ query context | Raw JDBC | `DSLContext` (already wired via `EntryService`) | jOOQ generated tables already cover `events`, `event_classes`, `entries` |
| Toast notifications | Custom notification component | `<Sonner>` + `toast()` (already in project) | `sonner.tsx` already installed; pattern established in Phase 2 |
| Championship standings pagination | Custom pagination | Return full standings (typical championship has < 50 drivers; no pagination needed v1) | Over-engineering for this domain size |

**Key insight:** The project already has the hardest parts — jOOQ codegen pipeline (running against Flyway-migrated schema), Hypersistence Utils JSONB mapping, JWT security filter, Testcontainers integration test base. Phase 3 builds on all of these without needing new infrastructure primitives.

---

## Common Pitfalls

### Pitfall 1: jOOQ generated tables don't include Phase 3 migrations

**What goes wrong:** Implementing `ChampionshipStandingsQuery` or `AdminEventQueryService` that reference `championships`, `championship_classes`, etc. before running jOOQ codegen against the new Flyway migrations.

**Why it happens:** jOOQ generates table classes from the schema at build time. New Flyway migrations added in Phase 3 won't exist in the generated code until `./gradlew :app:generateJooq` is run.

**How to avoid:** In each Wave 0 task, add the Flyway migration first, then run jOOQ codegen, then write the query service. The build pipeline (`generateJooq` → `flywayMigrateForCodegen`) handles this automatically if run in order.

**Warning signs:** Compilation errors referencing `CHAMPIONSHIPS` or `CHAMPIONSHIP_CLASSES` tables in generated jOOQ code.

### Pitfall 2: event_classes missing racing_class_id

**What goes wrong:** `EventClass` entity has no `racing_class_id` FK. The Phase 2 entry submission already uses `event_class_id` from the entry, but does NOT join back to the racing class. Phase 3 needs to display which racing class each event class represents.

**Why it happens:** Phase 1 created `event_classes` for format config storage; the racing class association was left implicit (only via the template).

**How to avoid:** Add `racing_class_id BIGINT REFERENCES racing_classes(id)` to `event_classes` in Phase 3 Flyway migration (V15 or similar). Update `EventClass` entity with `racingClassId` field. Make it nullable initially to avoid breaking Phase 2 test seed data; add NOT NULL constraint after backfilling.

**Warning signs:** Admin "Add Class" form has no way to specify which racing class the event class represents.

### Pitfall 3: Event.trackId column missing

**What goes wrong:** `events` table currently has no `track_id` column (V12 migration). EVENT-07 requires track association.

**Why it happens:** Phase 2 did not need track association (only needed event list for racer portal).

**How to avoid:** V15 migration adds `track_id BIGINT REFERENCES tracks(id)`. Make nullable (not all events may have a track assigned initially). `Event` entity gets `trackId` field. `CreateEventRequest` includes `trackId` (nullable).

**Warning signs:** EVENT-07 requirement unmet; event detail page has no track selector.

### Pitfall 4: entry_classes combined_race_id not present

**What goes wrong:** EVENT-06 class combining requires `event_classes` to reference a shared "combined race" grouping. Without this, the backend cannot model two classes racing together.

**Why it happens:** Not needed until Phase 3.

**How to avoid:** Add `combined_race_id BIGINT` to `event_classes` — a nullable self-FK or a separate `combined_races` table. Simplest approach: nullable `combined_race_id` FK column where event classes sharing the same non-null value are combined. A `combined_races` table with an id (auto) + event_id is cleaner but adds a migration.

**Recommendation:** Simple nullable UUID/bigint `combined_race_group` column on `event_classes` (same group = same race). No separate table needed for v1.

### Pitfall 5: ClubProfile.logo is bytea (existing entity stores logo in DB)

**What goes wrong:** The existing `ClubProfile` entity stores `logo` as `byte[]` (PostgreSQL `bytea`). D-22 says logo goes to MinIO. If Phase 3 adds the MinIO upload without addressing this, the old bytea column is orphaned and the code may still try to load large blobs during `GET /api/v1/admin/club/profile`.

**How to avoid:** Add `logo_url VARCHAR(500)` column via Flyway migration. Update `ClubProfileDto` to return `logoUrl` (the MinIO object key or presigned URL). Keep the old `logo` column nullable — do not remove it (data safety). Update `ClubProfileService.getProfile()` to return `logoUrl`, not the binary data. Add `PUT /api/v1/admin/club/logo` multipart endpoint.

### Pitfall 6: car tag category hard-delete breaks Phase 2 racer car forms

**What goes wrong:** `CarTagCategoryService.delete()` currently hard-deletes via JPA. Phase 3 D-21 says "archive" (soft delete). If Phase 3 replaces the delete endpoint behaviour, the test for `DELETE /api/v1/admin/car-tag-categories/{id}` in `CarTagCategoryIT.java` will need updating.

**How to avoid:** Add `archived BOOLEAN NOT NULL DEFAULT FALSE` column to `car_tag_categories` via Flyway. Change `CarTagCategoryService.delete()` to set `archived = true`. Update `findAll()` to filter out archived by default (or accept a flag). Update test to verify the category still exists but `archived = true`.

### Pitfall 7: Championship points scale as JSONB vs table rows

**What goes wrong:** Storing the points scale as a JSONB array on the `championships` table is tempting (one column, easy to edit), but jOOQ queries that need to join position → points for scoring must then unnest JSON, which is harder to write and debug.

**How to avoid:** Use a `championship_points_scale` table: `(championship_id BIGINT, position INT, points INT, PRIMARY KEY (championship_id, position))`. Cleaner joins, easier to query in jOOQ. [ASSUMED — either approach is valid; this recommendation based on query simplicity]

### Pitfall 8: React route structure breaks ProtectedRoute

**What goes wrong:** The current `App.tsx` wraps `/admin/*` in `<ProtectedRoute roles={['ADMIN', 'RACE_DIRECTOR', 'REFEREE']}>` but renders `<AdminPlaceholderPage />` as the child. When replacing with `<AdminPanelLayout>` and nested routes, the `ProtectedRoute` wrapper needs to work with `react-router-dom` nested routes (using `<Outlet />`).

**How to avoid:** The `ProtectedRoute` component already renders `{children}` — keep it as a wrapper around `<AdminPanelLayout />`, not around individual page components. Admin subroutes do not need individual `ProtectedRoute` wrapping once the parent route is protected.

---

## Code Examples

Verified patterns from the codebase:

### Integration Test Pattern (extend AbstractIntegrationTest)
```java
// Source: [VERIFIED: AbstractIntegrationTest.java — existing base class]
class EventControllerIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    @Test
    void createEvent_asAdmin_returns201() {
        String adminToken = loginAsAdmin(); // seed user from V101 test migration
        var body = Map.of("name", "Spring Meeting 2026", "eventDate", "2026-05-10");
        var resp = restTemplate.exchange("/api/v1/admin/events", HttpMethod.POST,
            new HttpEntity<>(body, headersWithBearer(adminToken)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

### jOOQ Query Service Pattern
```java
// Source: [VERIFIED: EntryQueryService.java / EventScheduleQuery.java — existing pattern]
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

### React Hook Form + Zod for Discriminated Union (Format Template)
```typescript
// Source: [ASSUMED — discriminated union pattern; matches RaceFormatConfig sealed interface]
const timedSchema = z.object({
  type: z.literal('TIMED'),
  durationMinutes: z.number().int().min(1),
  startType: z.enum(['STAGGER', 'GRID', 'ROLLING']),
  qualifyingType: z.enum(['FTQ', 'ROUND_BY_ROUND', 'FASTEST_LAP', 'CONSECUTIVE_LAPS']),
  racePaddingMinutes: z.number().int().min(0),
  staggerIntervalSeconds: z.number().int().min(1),
});
const bumpUpSchema = z.object({ type: z.literal('BUMP_UP'), /* ... */ });
const pointsFinalsSchema = z.object({ type: z.literal('POINTS_FINALS'), /* ... */ });

const formatSchema = z.discriminatedUnion('type', [timedSchema, bumpUpSchema, pointsFinalsSchema]);
```

### Sonner Toast Pattern
```typescript
// Source: [VERIFIED: sonner.tsx installed, sonner 2.0.7 in package.json]
import { toast } from 'sonner';

// Success
toast.success('Event published successfully');

// Error (409 specific)
toast.error('Transition rejected — page has been refreshed');
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `ClubProfile.logo` as `bytea` | MinIO S3 object storage (D-22) | Phase 3 | Logo upload endpoint added; entity stores `logo_url` string not binary |
| `CarTagCategoryService.delete()` hard-deletes | Soft-delete via `archived` flag (D-21) | Phase 3 | Existing `DELETE` endpoint changes behaviour; test updated |
| `AdminPlaceholderPage.tsx` stub | Full `AdminPanelLayout.tsx` with nested routes | Phase 3 | `App.tsx` admin route rewired to new layout |
| `event_classes` has no racing class association | `racing_class_id` FK added | Phase 3 | Enables "Add Class to Event" picking from available classes |
| `events` has no track association | `track_id` FK added | Phase 3 | EVENT-07 met |

**Deprecated/outdated:**
- `ClubProfile.logo` (bytea column): keep as nullable for backward compatibility but stop writing to it in Phase 3.
- `AdminPlaceholderPage.tsx`: delete in the frontend layout wave.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | MinIO `forcePathStyle(true)` required for AWS S3 SDK v2 | Code Examples | Logo upload fails with connection/URL error; simple config fix |
| A2 | `championship_points_scale` as a table (not JSONB) is recommended | Common Pitfalls | Increased migration complexity; easily changed if jOOQ JSONB operators preferred |
| A3 | AWS SDK BOM v2.27.x is the current version to use | Standard Stack | Wrong version pin; verify with `npm view` equivalent for Maven/Gradle |
| A4 | `event_classes` needs `racing_class_id` FK for "Add Class to Event" | Architecture Patterns | Without it, admin cannot specify which class each event class represents |
| A5 | MinIO Docker image `minio/minio:latest` is appropriate for dev | Standard Stack | Use a pinned version tag for reproducibility; `latest` could introduce breaking changes |
| A6 | `combined_race_group` nullable column on `event_classes` (vs separate table) for EVENT-06 | Architecture Patterns | Either approach works; separate table is cleaner if combined races need metadata |
| A7 | Championship standings return empty list in Phase 3 (Phase 7 provides result data) | Architecture Patterns | If admin expects to configure and preview standings with mock data, additional scaffold needed |

---

## Open Questions

1. **EventClass racing_class_id: nullable migration path**
   - What we know: `event_classes` currently has no `racing_class_id` column; Phase 2 test seed rows exist without it
   - What's unclear: Should the Flyway migration add it as nullable (safe) or NOT NULL with a default?
   - Recommendation: Add as nullable initially; add a planner note that production deployments need backfilling. NOT NULL can be enforced in Phase 4 once all event classes are created with the column populated.

2. **Championship class overrides: simple or complex?**
   - What we know: D-11 says "championship classes inherit defaults with optional per-class overrides"
   - What's unclear: Do per-class overrides cover TQ bonus, A-final bonus, and scoring source separately, or just "best-X-from-Y"?
   - Recommendation: For v1, store per-class override only for `best_x_from_y` (most common variation). Other fields default to championship-level values. This is the simplest implementation that satisfies the requirement.

3. **MinIO bucket auto-creation**
   - What we know: MinIO requires the bucket to exist before uploading; `docker-compose.yml` currently does not create buckets
   - What's unclear: Should the Spring app auto-create the bucket on startup, or should an init step in docker-compose handle it?
   - Recommendation: Add a `@PostConstruct` in `MinioConfig` that calls `s3Client.createBucket(...)` if the bucket does not exist (idempotent). This ensures dev setup works without manual steps.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker | MinIO in docker-compose, jOOQ codegen | [ASSUMED] ✓ | Unknown | MinIO can use filesystem mock; jOOQ codegen can use external DB |
| PostgreSQL 16 (via Docker Compose) | All backend tests | ✓ (Testcontainers auto-starts) | 16-alpine | Testcontainers pulls image automatically |
| MinIO | Club logo upload (D-22) | ✗ (not in docker-compose yet) | N/A | Add to docker-compose.yml in Wave 0; no fallback for logo upload feature |
| AWS S3 SDK v2 | MinIO S3 client | ✗ (not in build.gradle.kts) | N/A | Must add dependency in Wave 0 |
| shadcn `table`, `tabs`, `checkbox`, `radio-group`, `switch` | Admin UI | ✗ (not installed) | N/A | Must run `npx shadcn@latest add table tabs checkbox radio-group switch` in Wave 0 |

**Missing dependencies with no fallback:**
- MinIO Docker service — logo upload requires it; add to docker-compose.yml
- AWS S3 SDK v2 — required for MinIO integration; add to build.gradle.kts
- shadcn components — required for admin tables and forms; install before any UI work

**Missing dependencies with fallback:**
- None

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test + Testcontainers |
| Config file | `AbstractIntegrationTest.java` (shared base, Testcontainers Postgres singleton) |
| Quick run command | `./gradlew :app:test --tests "*EventControllerIT*" -x generateJooq` |
| Full suite command | `./gradlew :app:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| EVENT-01 | Create event returns 201 with name/date | Integration | `./gradlew :app:test --tests "*EventControllerIT*"` | ❌ Wave 0 |
| EVENT-02 | Add class to event with format snapshot | Integration | `./gradlew :app:test --tests "*EventClassControllerIT*"` | ❌ Wave 0 |
| EVENT-05 | Valid transition returns 200; invalid returns 409 | Integration | `./gradlew :app:test --tests "*EventControllerIT*"` | ❌ Wave 0 |
| EVENT-06 | Combine classes links them via combined_race_group | Integration | `./gradlew :app:test --tests "*EventClassControllerIT*"` | ❌ Wave 0 |
| EVENT-07 | Create event with trackId; track returns in detail | Integration | `./gradlew :app:test --tests "*EventControllerIT*"` | ❌ Wave 0 |
| ENTRY-02 | Admin GET entries for class returns racer data | Integration | `./gradlew :app:test --tests "*AdminEntryControllerIT*"` | ✅ (extend existing) |
| ENTRY-02 | Admin withdraw entry changes status to WITHDRAWN | Integration | `./gradlew :app:test --tests "*AdminEntryControllerIT*"` | ✅ (extend existing) |
| CHAMP-01 | Create championship with best_x/best_y; GET returns config | Integration | `./gradlew :app:test --tests "*ChampionshipControllerIT*"` | ❌ Wave 0 |
| CHAMP-04 | Points scale saved and returned per position | Integration | `./gradlew :app:test --tests "*ChampionshipControllerIT*"` | ❌ Wave 0 |
| CHAMP-09 | Driver exclusion saved with audit log entry | Integration | `./gradlew :app:test --tests "*ChampionshipControllerIT*"` | ❌ Wave 0 |
| D-22 | Logo upload returns 200; logo_url set on club profile | Integration | `./gradlew :app:test --tests "*ClubControllerIT*"` | ✅ (extend existing) |

### Sampling Rate
- **Per task commit:** `./gradlew :app:test --tests "*{TaskDomain}*IT*" -x generateJooq`
- **Per wave merge:** `./gradlew :app:test`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `EventControllerIT.java` — covers EVENT-01, EVENT-05, EVENT-07
- [ ] `EventClassControllerIT.java` — covers EVENT-02, EVENT-06
- [ ] `ChampionshipControllerIT.java` — covers CHAMP-01, CHAMP-04, CHAMP-09
- [ ] V15 through V19 Flyway migrations (required before jOOQ codegen)
- [ ] MinIO Docker service in docker-compose.yml (required for logo upload test)

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | Yes | Spring Security JWT filter (existing — all admin routes require Bearer token) |
| V3 Session Management | No | Stateless JWT; no session |
| V4 Access Control | Yes | `@PreAuthorize("hasAnyRole('ADMIN', ...)")` on all admin controllers; SecurityConfig gates `/api/v1/admin/**` |
| V5 Input Validation | Yes | `@Valid` + Jakarta Bean Validation on all request DTOs; Zod on frontend |
| V6 Cryptography | No | No new crypto in Phase 3 |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Insecure Direct Object Reference (admin accesses another club's data) | Elevation of Privilege | Single-club deployment; no multi-tenancy in v1. All admin endpoints are implicitly scoped to the single club. If multi-club is added later, add club_id FK checks. |
| Mass assignment on event creation | Tampering | Use dedicated `CreateEventRequest` record with only whitelisted fields; never bind `HttpServletRequest` params directly to entities |
| File upload abuse (logo) | DoS / Tampering | Validate file size (2 MB limit) and MIME type (PNG/SVG only) before forwarding to MinIO; reject other types at controller level |
| State transition replay (double-click publish) | Tampering | HTTP 409 on invalid state transition; event query invalidated after mutation so UI reflects current state |

---

## Sources

### Primary (HIGH confidence)
- `[VERIFIED: codebase]` — All existing entity files, controller files, migration SQL, build.gradle.kts, package.json read directly in this session
- `[VERIFIED: .planning/phases/03-admin-panel-event-management/03-CONTEXT.md]` — Locked decisions D-01 through D-22, deferred items
- `[VERIFIED: .planning/phases/03-admin-panel-event-management/03-UI-SPEC.md]` — Component inventory, route structure, badge color map, points scale defaults, interaction contracts

### Secondary (MEDIUM confidence)
- `[CITED: Spring Boot documentation]` — `@PreAuthorize`, `@ServiceConnection`, Testcontainers integration patterns (established patterns already in codebase)

### Tertiary (LOW confidence — see Assumptions Log)
- MinIO AWS S3 SDK `forcePathStyle(true)` requirement (A1)
- AWS SDK BOM version (A3)
- Championship points scale as table vs JSONB (A2)

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all verified against actual build files and package.json
- Architecture: HIGH — based on existing code patterns; new entities follow identical structure to existing ones
- Pitfalls: HIGH — found by direct inspection of existing code (bytea logo, missing racing_class_id, missing track_id, hard-delete categories)
- MinIO configuration: MEDIUM — standard well-known pattern but not verified against current MinIO docs in this session

**Research date:** 2026-04-20
**Valid until:** 2026-05-20 (30 days — stable stack)
