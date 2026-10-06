# Phase 7: Results & Championship — Research

**Researched:** 2026-05-01
**Domain:** jOOQ read-side aggregations, public REST endpoints, React route/component extension
**Confidence:** HIGH

---

## Summary

Phase 7 is an extension phase, not a greenfield one. All infrastructure already exists: result snapshots are stored (V19), championship schema is in place (V16), jOOQ query classes are scaffolded, the frontend component library is wired. The work is (1) implementing the `ChampionshipStandingsQuery` TODO, (2) opening two public REST endpoints, (3) wiring three new frontend pages, and (4) adding one Flyway migration for two new columns (`show_car_tags_in_results` on `club_profiles`, `car_number` on `race_entries`).

The key carry-forward debt item — `car_number` column on `race_entries` — must be resolved in this phase because it appears in the existing `ResultSnapshotDto.ResultRow.carNumber` field (which is currently always `null`, set in `ResultSnapshotService.resolveEntryInfo()`). The `PrintResultsPage` already renders `row.carNumber ?? '—'`, confirming the frontend expects it. The migration and generator update must happen before the public results page is useful.

Championship standings calculation is the most algorithmically complex task. The jOOQ join must read `result_snapshots.positions_json` (JSONB array), unnest or parse it per-driver, apply the points scale, apply exclusions, identify TQ and A-final winner for bonus points, then apply best-X-from-Y drop logic. This is all done in-process in Java, not purely in SQL — the JSONB payload is deserialized and aggregated in the service layer. The existing `StandingsRowDto` and `RoundResultDto` record types already model the output shape.

**Primary recommendation:** Implement in five sequential plans: (1) V24 schema migration for car_number + show_car_tags_in_results; (2) championship standings computation in `ChampionshipStandingsQuery`; (3) public REST endpoints + Security config updates; (4) frontend public pages (results + standings); (5) racer results portal tab + event schedule links.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** New public route `/results/:raceId` — no login required. Reuses the existing `PrintResultsPage` component but mounted on an unprotected route.
- **D-02:** Individual lap times (RESULT-05) displayed as an expandable row per racer — click/tap to expand and see all lap times.
- **D-03:** Once a race is FINISHED, a results link appears on the public event schedule page (`/events`), linking to `/results/:raceId`.
- **D-04:** New Results page in the racer portal (alongside profile, cars, transponders, entries).
- **D-05:** Top-level display: list of events the racer entered, each expandable to show race results for that event.
- **D-06:** Each race entry links through to `/results/:raceId`.
- **D-07:** All car tags for the racer's entered car are displayed beneath their name in printed results (no category filtering).
- **D-08:** Global admin toggle in Admin → Club settings to enable/disable car tag display in results.
- **D-09:** New public route `/championships/:id` — no login required. Separate from the admin `ChampionshipDetailPage`.
- **D-10:** Public page shows standings table with drop scores visible — driver, total points, per-round scores, with dropped rounds visually distinguished.
- **D-11:** If an event is associated with a championship, a Standings link appears on the public event schedule page linking to `/championships/:id`.

### Claude's Discretion

- Exact visual styling of the drop-score indicator (greyed out vs struck through vs badge).
- Pagination or infinite scroll on racer history.
- Whether the public championship page reuses `ChampionshipStandingsTable` or builds its own.

### Deferred Ideas (OUT OF SCOPE)

- None from discussion.

</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| RESULT-01 | Final race results published after each race | `ResultSnapshotService` already runs on FINISH; needs public REST endpoint + unprotected route |
| RESULT-02 | Results reflect marshal lap adjustments and penalties | Already embedded in `positions_json` at snapshot time — no new logic needed |
| RESULT-03 | Per-racer result history viewable on racer portal | New `RacerResultsPage` + new jOOQ query joining result_snapshots → race_entries → entries by user |
| RESULT-04 | Printed results optionally display car tag values; controlled by admin setting | New `show_car_tags_in_results` column on `club_profiles`; read in `ResultSnapshotQuery.load()` |
| RESULT-05 | Result records include full individual lap time data | `lap_history_json` already stored; frontend needs expandable row to display it |
| CHAMP-05 | Public championship standings table live on the web | Implement `ChampionshipStandingsQuery.computeStandings()` + new public controller + new frontend page |

</phase_requirements>

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Championship standings computation | API/Backend (jOOQ query module) | — | Aggregates multiple DB tables; no browser-side calculation |
| Public results REST endpoint | API/Backend | — | Security config must permit anonymous GET |
| Public standings REST endpoint | API/Backend | — | Security config must permit anonymous GET |
| Car tag toggle admin setting | API/Backend (domain module) | Frontend (admin) | Stored on ClubProfile entity; frontend provides toggle UI |
| Car tag display in results | API/Backend (read query) | Frontend (print results) | `ResultSnapshotQuery` enriches DTO; frontend renders conditionally |
| Car number column | API/Backend (domain + Flyway) | — | Schema gap — V24 migration adds column |
| Public results page | Frontend/Browser | — | Mounts existing `PrintResultsPage` on unprotected route |
| Public standings page | Frontend/Browser | — | New page reusing `ChampionshipStandingsTable` component |
| Racer result history page | Frontend/Browser | API/Backend | New page + new query endpoint |
| Event schedule links | Frontend/Browser | API/Backend | `EventScheduleDto` must include `raceFinishedIds` and `championshipId` |

---

## Standard Stack

### Core (all verified in codebase — no new dependencies)

| Library | Version | Purpose | Source |
|---------|---------|---------|--------|
| jOOQ | 3.19.x | Championship standings aggregation, racer results query | [VERIFIED: existing `pom.xml`/build files] |
| Spring Boot | 3.4.x | REST controllers, security config updates | [VERIFIED: existing controllers] |
| React 18 + Vite | 18.x | New frontend pages | [VERIFIED: existing frontend] |
| TanStack Query v5 | 5.x | Data fetching hooks for new pages | [VERIFIED: existing hooks pattern] |
| shadcn/ui | current | UI components including `Collapsible` (new) | [VERIFIED: `07-UI-SPEC.md`] |
| Flyway | current | V24 migration | [VERIFIED: existing migrations V1–V23] |
| Remixicon | current | `ri-trophy-line`, `ri-file-list-line` icons | [VERIFIED: `07-UI-SPEC.md`] |

### New Frontend Component Required

| Component | shadcn command | Status |
|-----------|---------------|--------|
| `Collapsible` | `npx shadcn@latest add collapsible` | Not yet installed — must be Wave 0 addition |

[VERIFIED: `07-UI-SPEC.md` Component Inventory; not present in existing component imports]

---

## Architecture Patterns

### System Architecture Diagram

```
Public browser (no login)
  GET /results/:raceId
    → PublicResultsController → ResultSnapshotQuery → result_snapshots (JSONB)
    → enriched with show_car_tags_in_results from club_profiles
    → ResultSnapshotDto (with car tags if enabled) → PublicResultsPage

  GET /championships/:id
    → PublicChampionshipController → ChampionshipStandingsQuery
    → championship_event_links → result_snapshots (JSONB, unnested in Java)
    → championship_points_scale, championship_exclusions
    → StandingsRowDto[] (with drop flags) → PublicChampionshipPage

Racer portal (authenticated)
  GET /racer/results
    → RacerResultsController → RacerResultHistoryQuery
    → entries → race_entries → result_snapshots (per-user join)
    → RacerResultHistoryDto[] → RacerResultsPage

Admin toggle (authenticated ADMIN)
  PUT /api/v1/admin/club/profile
    → ClubProfileController (existing) → ClubProfile.showCarTagsInResults
    → V24 migration column

Event schedule (public, existing endpoint)
  EventScheduleQuery.getPublicSchedule()
    → must additionally JOIN championship_event_links
    → and query finished races (races.status = FINISHED)
    → EventScheduleDto enriched with resultRaceIds[], championshipId
```

### Recommended Project Structure (additions only)

```
app/src/main/java/dev/monkeypatch/rctiming/
├── api/
│   ├── public/
│   │   ├── PublicResultsController.java      ← GET /api/v1/results/{raceId}
│   │   └── PublicChampionshipController.java ← GET /api/v1/championships/{id}
│   └── racer/
│       └── RacerResultsController.java       ← GET /api/v1/racer/results (authenticated)
├── query/
│   ├── championship/
│   │   └── ChampionshipStandingsQuery.java   ← IMPLEMENT existing TODO
│   └── results/
│       └── RacerResultHistoryQuery.java      ← NEW

app/src/main/resources/db/migration/
└── V24__phase7_results_championship.sql      ← car_number + show_car_tags_in_results

frontend/src/
├── pages/
│   ├── results/
│   │   └── PublicResultsPage.tsx             ← mounts PrintResultsPage on public route
│   ├── championships/
│   │   └── PublicChampionshipPage.tsx        ← reuses ChampionshipStandingsTable
│   └── racer/
│       └── RacerResultsPage.tsx              ← new racer portal tab
└── components/
    └── results/
        └── LapTimesCollapsible.tsx           ← shadcn Collapsible + lap time list
```

### Pattern 1: jOOQ Standings Aggregation (implement in ChampionshipStandingsQuery)

The standings computation cannot be done purely in SQL because `positions_json` is a JSONB array of `ResultRow` objects that must be deserialized into Java, matched against the points scale, and scored. The algorithm is:

```java
// Source: [ASSUMED — based on domain model and existing pattern in ResultSnapshotQuery.java]
// For each championship class:
//   1. Load all ChampionshipEventLinks for the championship (in roundNumber order)
//   2. For each event link, find finished races matching the class + scoring_source (QUALIFYING/FINALS/BOTH)
//   3. Load result_snapshots for those races, deserialize positions_json
//   4. For each driver: look up their position → look up points in championship_points_scale
//   5. Check championship_exclusions — if excluded, points = 0, excluded = true
//   6. Award TQ bonus (fastest qualifier) and A-final winner bonus
//   7. Collect per-driver list of RoundResultDto (one per event link, even if DNS)
//   8. Apply best-X-from-Y: sort rounds by points desc, mark worst (Y-X) rounds as dropped = true
//   9. totalPoints = sum of non-dropped rounds
//  10. Sort drivers by totalPoints desc
```

**DNS handling:** A driver who entered the event but did not appear in `positions_json` scores 0 points for that round. A driver who did not enter the event at all does not appear in standings for that round (no `RoundResultDto` entry for them). The CHAMP-02 requirement says DNS counts toward Y rounds attended — this means: if a driver was entered but did not start, they get 0 points but the round still counts as one of their Y rounds. [ASSUMED — club confirmation flagged in STATE.md concerns]

**Key join path:**
```
championship_event_links (championship_id, event_id, round_number)
  → events (id)
  → event_classes (event_id, racing_class_id) where racing_class matches championship_class
  → rounds (event_id, type) — filter by scoring_source
  → races (round_id, event_class_id, status = FINISHED)
  → result_snapshots (race_id) — deserialize positions_json
```

### Pattern 2: Public REST Endpoint (SecurityConfig update)

The existing `SecurityConfig` already has the pattern for public GET routes:

```java
// Source: [VERIFIED: SecurityConfig.java]
.requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/**").permitAll()
// Add:
.requestMatchers(HttpMethod.GET, "/api/v1/results/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/championships/**").permitAll()
```

The existing `ResultSnapshotController` uses `@PreAuthorize("isAuthenticated()")`. Phase 7 creates a NEW controller at a new path (`/api/v1/results/{raceId}`) rather than changing the existing one. This preserves the existing race control results endpoint behaviour.

### Pattern 3: EventScheduleDto enrichment

`EventScheduleQuery.getPublicSchedule()` currently returns `EventScheduleDto(id, name, eventDate, entryAvailability)`. Phase 7 needs two additions:
- `List<Long> finishedRaceIds` — IDs of FINISHED races at this event (for "View Results" links)
- `Long championshipId` — if this event is linked to a championship (for "View Standings" link)

This requires extending `EventScheduleDto` and `EventScheduleQuery.getPublicSchedule()` with additional joins.

### Pattern 4: Car Number Column (V24 migration + domain update)

`race_entries` currently has no `car_number` column — confirmed by V17 migration and `RaceEntry.java` entity. The `ResultSnapshotDto.ResultRow.carNumber` field is always `null` (set in `resolveEntryInfo()` as `new String[]{"name", null}`).

V24 must add:
```sql
ALTER TABLE race_entries ADD COLUMN car_number int;
-- Nullable: car_number is null until round generator assigns it.

ALTER TABLE club_profiles ADD COLUMN show_car_tags_in_results boolean NOT NULL DEFAULT false;
```

`RoundGeneratorService` must be updated to set `car_number` (1–N in entry order for qualifying, re-numbered from qualifying results for finals). `BumpUpSeedingService` must also set car numbers when seeding finals grids. `ResultSnapshotService.resolveEntryInfo()` must read `car_number` from `race_entries` instead of hardcoding `null`.

### Pattern 5: Car Tags in Results DTO

`ResultSnapshotQuery.load()` currently returns `ResultSnapshotDto` with `carNumber` set per row but no car tags. To support RESULT-04:

1. Read `show_car_tags_in_results` from `club_profiles` in `ResultSnapshotQuery.fetchClubBranding()` (or a separate call)
2. If enabled: for each `entryId` in the result, join `entries → cars → car_tag_values → car_tag_categories` to get the tag snapshot at entry time
3. Add `List<CarTagDto>` to `ResultSnapshotDto.ResultRow` (null/empty when disabled)
4. Frontend renders inline as `Chassis: Xray T4 · Motor: 10.5T` in `text-xs text-muted-foreground`

Note: The CONTEXT.md says "all key/value pairs snapshotted at entry time." The entry record has `car_id` — but car tags live on the `Car` entity at read time, not as a snapshot on the entry. [ASSUMED: car tags are read from the current car state, not a snapshot] — this needs verification against the domain model.

### Pattern 6: Frontend — shadcn Collapsible

```tsx
// Source: [ASSUMED — based on shadcn Collapsible API]
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';

// Accordion: one open at a time — managed with local state index
const [openId, setOpenId] = useState<number | null>(null);

<Collapsible
  open={openId === row.entryId}
  onOpenChange={(open) => setOpenId(open ? row.entryId : null)}
>
  <CollapsibleTrigger asChild>
    <tr className="min-h-[44px] cursor-pointer select-none">
      {/* existing result row cells */}
      <td><ChevronDown className={openId === row.entryId ? 'rotate-180' : ''} /></td>
    </tr>
  </CollapsibleTrigger>
  <CollapsibleContent asChild>
    <tr><td colSpan={7}>{/* lap times list */}</td></tr>
  </CollapsibleContent>
</Collapsible>
```

The UI-SPEC mandates `ri-arrow-down-s-line` from remixicon, not ChevronDown from lucide. Use remixicon consistently.

### Anti-Patterns to Avoid

- **Cross-module boundary violation:** Do not add Hibernate entity navigation to the query module. All reads in `ChampionshipStandingsQuery` use DSLContext — no `@Autowired` repositories from the domain module.
- **Modifying the existing race-control results endpoint:** Do not change `ResultSnapshotController` or its `/api/v1/race-control/race/{raceId}/result-snapshot` URL. Create a new `/api/v1/results/{raceId}` endpoint for public access.
- **Incrementally updating championship points:** The architecture is explicit — calculate on demand from result snapshots. Do not store running totals.
- **Lazy-loading JSONB:** Deserialize `positions_json` and `lap_history_json` eagerly in the query service. Never pass `JSONB` objects out of the query layer.
- **Storing live race positions in DB:** Positions are snapshotted only on FINISH. The result snapshot is the source of truth for the public results page.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Accordion/expandable rows | Custom state machine with CSS visibility | shadcn `Collapsible` (Radix UI) | Keyboard accessibility, animation, focus management |
| Best-X-from-Y drop calculation | Clever SQL window function | Java `Stream` sort + mark dropped | JSONB positions_json already deserialized; SQL approach would require unnesting JSON in PostgreSQL — readable Java is simpler |
| Public route protection bypass | Custom `OncePerRequestFilter` | `SecurityConfig.requestMatchers().permitAll()` | Established pattern already in codebase |
| Car tag display format | Custom renderer | CSS inline format per UI-SPEC: `Key: Value · Key: Value` | Trivially implemented; no library needed |

---

## Runtime State Inventory

> This is not a rename/refactor phase. No runtime state inventory required.

---

## Common Pitfalls

### Pitfall 1: DNS vs "didn't enter" in standings (CHAMP-02)

**What goes wrong:** Treating "not in positions_json" and "entered but DNS" identically — both score 0 — but they have different semantics for best-X-from-Y. A DNS uses one of the driver's Y slots; a driver who didn't enter the event does not.

**Why it happens:** `positions_json` only contains drivers who were scored. A DNS driver may or may not appear depending on how `ResultSnapshotService` handled them (if they had no lap crossings, they may be absent).

**How to avoid:** The join must be through `race_entries` (who was entered in the race), not only through `positions_json` (who was scored). A driver present in `race_entries` but absent from `positions_json` = DNS = 0 points, round counts toward Y. A driver absent from `race_entries` for that event = did not enter = round does not count toward Y.

**Warning signs:** Standings showing fewer rounds than expected for drivers who always attend but sometimes DNS.

**Open question:** The STATE.md flags this explicitly: "Championship DNS/DQ scoring policy (does DNS count toward Y rounds?) needs club confirmation before Phase 7 scoring implementation." Mark as ASSUMED until confirmed.

### Pitfall 2: car_number is null in existing result snapshots

**What goes wrong:** After the V24 migration adds `car_number` to `race_entries`, all existing race entries have `car_number = NULL`. Any historical result snapshot loaded from `result_snapshots` will still have `carNumber: null` in `positions_json` (it was serialized at snapshot time without car numbers).

**Why it happens:** `positions_json` is a serialized JSON blob. The column addition does not retroactively fix the JSON inside it.

**How to avoid:** Accept that historical snapshots show `—` for car number. The `PrintResultsPage` already handles this: `row.carNumber ?? '—'`. Only new races (run after the migration and generator update) will have car numbers. Document this limitation in the plan.

### Pitfall 3: EventScheduleDto format change breaks existing callers

**What goes wrong:** Adding new fields to `EventScheduleDto` and the `EventScheduleQuery` join changes the API response shape. The frontend `EventSchedulePage` stub (`return <div>coming in Plan 06.</div>`) will be replaced in Phase 7, so there are no active callers to break — but the Spring Security `permitAll()` rule for `/api/v1/events/**` must be preserved.

**How to avoid:** Add new fields as nullable additions to `EventScheduleDto`. Keep the endpoint URL unchanged (`GET /api/v1/events`).

### Pitfall 4: ChampionshipStandingsTable uses lucide icons (not remixicon)

**What goes wrong:** `ChampionshipStandingsTable.tsx` currently imports `Loader2` and `AlertCircle` from `lucide-react`. The UI-SPEC mandates remixicon for this phase.

**How to avoid:** The UI-SPEC says to reuse `ChampionshipStandingsTable` with token correction (`text-muted-foreground` not `text-slate-300`). The public page wrapping it can use remixicon; the internal component doesn't need its lucide icons changed in this phase if they're loading states. Flag this to the planner — touch the drop-score token fix as specified, but don't needlessly replace loading spinners.

### Pitfall 5: Collapsible as table row children

**What goes wrong:** Radix UI `Collapsible` renders as a `<div>` by default. Nesting a `<div>` inside a `<tbody>` is invalid HTML and causes browser layout bugs.

**How to avoid:** Use `CollapsibleContent asChild` with `<tr>` as the underlying element, or implement the expandable state using plain React `useState` with conditional rendering of a `<tr>` row rather than the `Collapsible` component. The UI-SPEC recommends `Collapsible` for accessibility but the `asChild` pattern may be needed for table structure. Plan should explicitly address this.

### Pitfall 6: Missing shadcn Collapsible component

**What goes wrong:** `Collapsible` is not in the existing component set (confirmed by checking the import list — no `Collapsible` in any existing `.tsx`). If it's assumed to be present, the build will fail.

**How to avoid:** Wave 0 of Phase 7 must include `npx shadcn@latest add collapsible`.

### Pitfall 7: ResultSnapshotController authentication gate

**What goes wrong:** `ResultSnapshotController` at `/api/v1/race-control/race/{raceId}/result-snapshot` uses `@PreAuthorize("isAuthenticated()")`. If a public results page tries to use this endpoint without a token, it will get HTTP 401.

**How to avoid:** Create a NEW `PublicResultsController` at `/api/v1/results/{raceId}` with no `@PreAuthorize`, permitted in SecurityConfig. The `useResultSnapshot` hook in the frontend uses the old authenticated endpoint — the public page needs a new hook (`usePublicResultSnapshot`) pointing to the new URL.

### Pitfall 8: show_car_tags_in_results — where car tags come from

**What goes wrong:** `Entry` entity has a `carId` column (or reference to a car). Car tags are on `Car` → `CarTagValue` → `CarTagCategory`. These are the current tag values, not a snapshot from entry time.

**How to avoid:** RESULT-04 says "car tag values beneath their name." The CONTEXT.md D-07 says "all key/value pairs snapshotted at entry time." If entry does not snapshot car tags, the join reads current car tag values (which could have changed since the race). This may be acceptable for v1. Mark [ASSUMED] in the plan and confirm with user if needed. The safest v1 approach: join `entries.car_id → cars → car_tag_values` at results read time, accepting that current values are used. The `ResultSnapshotService` could alternatively serialize tags into the JSONB snapshot, but that would require re-snapshotting.

---

## Code Examples

### Existing jOOQ Pattern — ResultSnapshotQuery (reference for new queries)

```java
// Source: [VERIFIED: ResultSnapshotQuery.java]
// Pattern: DSLContext join with aliased columns, TypeReference deserialization for JSONB
var row = dsl
    .select(RESULT_SNAPSHOTS.RACE_ID, RESULT_SNAPSHOTS.POSITIONS_JSON, ...)
    .from(RESULT_SNAPSHOTS)
    .join(RACES).on(RACES.ID.eq(RESULT_SNAPSHOTS.RACE_ID))
    .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
    .where(RESULT_SNAPSHOTS.RACE_ID.eq(raceId))
    .fetchOne();

List<ResultSnapshotDto.ResultRow> positions = objectMapper.readValue(
    row.get(RESULT_SNAPSHOTS.POSITIONS_JSON).data(),
    new TypeReference<List<ResultSnapshotDto.ResultRow>>() {});
```

### Existing Security Pattern — Public Endpoints

```java
// Source: [VERIFIED: SecurityConfig.java]
.requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/**").permitAll()
// Phase 7 additions follow this exact pattern:
.requestMatchers(HttpMethod.GET, "/api/v1/results/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/championships/**").permitAll()
```

### Existing React Routing Pattern — Unprotected Route

```tsx
// Source: [VERIFIED: App.tsx — /events route]
{ path: '/events', element: <EventSchedulePage /> },
// Phase 7 additions:
{ path: '/results/:raceId', element: <PublicResultsPage /> },
{ path: '/championships/:id', element: <PublicChampionshipPage /> },
```

### Existing Racer Portal Pattern — New Tab

```tsx
// Source: [VERIFIED: RacerPortalLayout.tsx]
const navItems = [
  { to: '/racer/profile', label: 'Profile', Icon: User },
  { to: '/racer/cars', label: 'Cars', Icon: Car },
  { to: '/racer/transponders', label: 'Transponders', Icon: Radio },
  { to: '/racer/entries', label: 'Entries', Icon: FileText },
  // Phase 7 addition:
  { to: '/racer/results', label: 'Results', Icon: Trophy },  // ri-trophy-line from remixicon
] as const;
```

### V24 Migration — Car Number + Show Car Tags Flag

```sql
-- Source: [ASSUMED — follows V17 and V23 migration patterns]
-- V24: Phase 7 — car_number on race_entries, show_car_tags_in_results on club_profiles

ALTER TABLE race_entries
    ADD COLUMN car_number int;
-- Nullable: assigned by round generator (qualifying) and BumpUpSeedingService (finals)
-- Unique constraint: car_number unique per race_id per racing class (enforced in service layer)

ALTER TABLE club_profiles
    ADD COLUMN show_car_tags_in_results boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN race_entries.car_number IS
    'Assigned by RoundGeneratorService on qualifying creation; re-numbered by BumpUpSeedingService on finals seeding. Consistent within a phase, changes at qualifying-to-finals boundary.';
```

---

## State of the Art

| Old Approach | Current Approach | Notes |
|--------------|-----------------|-------|
| `ChampionshipStandingsQuery.computeStandings()` returns `List.of()` | Must implement full aggregation | Phase 3 stub — Phase 7 delivers it |
| `ResultSnapshotController` is authenticated-only | Phase 7 adds a parallel public endpoint | Keep existing endpoint; add new one |
| `EventSchedulePage` is a stub | Phase 7 implements it fully | Was placeholder since Phase 2 |
| `car_number` in `ResultSnapshotDto.ResultRow` is always `null` | Phase 7 wires it through from `race_entries.car_number` | V24 migration + generator update |
| `PrintResultsPage` mounted only in protected race-control layout | Phase 7 mounts `PublicResultsPage` on unprotected route | New page wraps or extends existing component |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | DNS (entered but not in positions_json) counts toward Y rounds in best-X-from-Y | Common Pitfalls 1 / CHAMP-02 | Standings totals wrong for DNS drivers; requires re-implementation |
| A2 | Car tags are read from current `car_tag_values` at results-load time, not from an entry-time snapshot | Pitfall 8 / RESULT-04 | Tag values shown could differ from what car actually ran; acceptable for v1 but not specification-accurate |
| A3 | The `Collapsible` shadcn component supports `asChild` on inner element to render as `<tr>` for table structure | Code Examples / Common Pitfalls 5 | May need plain `useState` + conditional `<tr>` instead; adds implementation complexity |
| A4 | Round generator assigns car numbers 1–N in grid_position order for qualifying; BumpUpSeedingService assigns car numbers 1–N from qualifying standing position for finals | Architecture Patterns / Pattern 4 | If the assignment rule differs, stagger announcements will be mis-ordered |
| A5 | All existing result snapshots in result_snapshots.positions_json have `carNumber: null` — no backfill needed, just accept historical gap | Common Pitfalls 2 | If backfill is expected, extra migration work required |

---

## Open Questions

1. **DNS counting toward Y rounds (CHAMP-02 policy)**
   - What we know: `STATE.md` flags this as requiring club confirmation before Phase 7
   - What's unclear: Does a driver who entered the event but didn't start use one of their Y allowed-drop rounds?
   - Recommendation: Default implementation treats DNS as 0 points, round counts toward Y (standard RC club policy). Planner should include a clear comment in the implementation and note in the plan that this is a policy assumption.

2. **Car tag snapshot vs current value (RESULT-04)**
   - What we know: Entry entity has `car_id`; car tags are on the Car entity and may change post-race
   - What's unclear: Does "snapshotted at entry time" (from D-07) mean the entry should store tag values at entry time, or is reading current values acceptable?
   - Recommendation: Read current values for v1 (simpler, no schema change beyond what's planned). This is consistent with the pattern — only transponder number is snapshotted on entry, not all car metadata.

3. **Race start sequence (carry-forward from Phase 6 UAT)**
   - What we know: UAT revealed a structured race-start flow (start-order recap, T-30, T-10, per-driver stagger) not fully in Phase 6
   - What's unclear: Does this belong in Phase 7 or a dedicated 7.x insertion?
   - Recommendation: This is an audio/race-control concern, not a results/championship concern. Defer to a Phase 7.1 insertion rather than bloating Phase 7 scope. Phase 7's RESULT/CHAMP-05 requirements are already well-scoped.

---

## Environment Availability

Step 2.6: SKIPPED — Phase 7 has no new external tool dependencies. All infrastructure (PostgreSQL, Spring Boot, Vite) was verified operational in Phases 1–6.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito + Testcontainers (backend); Vitest + React Testing Library (frontend) |
| Config file | `app/src/test/` hierarchy; `frontend/vitest.config.ts` |
| Quick run command | `./gradlew :app:test --tests "*Championship*" --tests "*Result*"` |
| Full suite command | `./gradlew :app:test && cd frontend && npm test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| RESULT-01 | Public endpoint returns result snapshot without auth | Integration | `./gradlew :app:test --tests "*PublicResultsControllerTest*"` | Wave 0 |
| RESULT-02 | Marshal laps reflected in snapshot (existing coverage) | Unit | Already covered in Phase 4 `ResultSnapshotService` tests | Check existing |
| RESULT-03 | Racer history query returns per-user race results | Integration | `./gradlew :app:test --tests "*RacerResultHistoryQueryTest*"` | Wave 0 |
| RESULT-04 | Car tags appear in DTO when setting enabled; absent when disabled | Unit | `./gradlew :app:test --tests "*ResultSnapshotQueryCarTagTest*"` | Wave 0 |
| RESULT-05 | Full lap time data returned with result snapshot | Integration | Covered by RESULT-01 test — verify `lapHistory` non-empty | Within RESULT-01 |
| CHAMP-05 | Public standings endpoint returns standings without auth | Integration | `./gradlew :app:test --tests "*PublicChampionshipControllerTest*"` | Wave 0 |
| CHAMP-05 | Best-X-from-Y drop logic marks correct rounds as dropped | Unit | `./gradlew :app:test --tests "*ChampionshipStandingsQueryTest*"` | Wave 0 |
| CHAMP-05 | TQ bonus and A-final winner bonus added correctly | Unit | Within `*ChampionshipStandingsQueryTest*` | Wave 0 |

### Wave 0 Gaps

- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/public/PublicResultsControllerTest.java` — REQ RESULT-01, RESULT-05
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/public/PublicChampionshipControllerTest.java` — REQ CHAMP-05
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQueryTest.java` — REQ CHAMP-05 (drop logic, bonuses)
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQueryTest.java` — REQ RESULT-03

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | Public endpoints — no auth required |
| V3 Session Management | No | Stateless JWT; not applicable to new public endpoints |
| V4 Access Control | Yes | `SecurityConfig.requestMatchers().permitAll()` for new public paths |
| V5 Input Validation | Yes | `raceId` and `championshipId` are path variables — Spring validates as `long`; no SQL injection (jOOQ parameterized) |
| V6 Cryptography | No | No new secrets |

### Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Enumeration of race IDs via public endpoint | Information Disclosure | `EntityNotFoundException → 404` already handled by `GlobalExceptionHandler`; no race data leaks on not-found |
| IDOR on racer history endpoint | Elevation of Privilege | `GET /api/v1/racer/results` must use `Authentication.getName()` (current user's ID) not a request parameter; no racer can see another's history |
| Standings computed with manipulated round numbers | Tampering | Input is `championshipId` (path variable); standings are computed from DB records only |

**IDOR on racer history is a critical security control.** The `RacerResultsController` must resolve the logged-in user's ID from `Authentication`, not from a request parameter. Existing pattern in `ProfilePage` / `racerApi.ts` follows this correctly — same approach applies here.

---

## Sources

### Primary (HIGH confidence — verified directly in codebase)

- `app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java` — snapshot structure, resolveEntryInfo pattern
- `app/src/main/java/dev/monkeypatch/rctiming/query/racecontrol/ResultSnapshotQuery.java` — jOOQ query pattern for JSONB deserialization
- `app/src/main/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQuery.java` — TODO stub, existing DSLContext wiring
- `app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java` — public route pattern
- `app/src/main/resources/db/migration/V16__create_championships.sql` — full championship schema
- `app/src/main/resources/db/migration/V17__phase4_race_schema.sql` — race_entries schema (confirmed no car_number)
- `app/src/main/resources/db/migration/V19__phase4_result_snapshots.sql` — result_snapshots schema
- `frontend/src/pages/race-control/PrintResultsPage.tsx` — component to reuse/extend
- `frontend/src/pages/admin/championships/ChampionshipStandingsTable.tsx` — component to reuse
- `frontend/src/App.tsx` — routing pattern for public and protected routes
- `frontend/src/pages/racer/RacerPortalLayout.tsx` — nav items pattern for new Results tab
- `.planning/phases/07-results-championship/07-UI-SPEC.md` — component inventory, design tokens, interaction contract
- `.planning/phases/07-results-championship/07-CONTEXT.md` — locked decisions D-01 through D-11
- `.planning/STATE.md` — DNS/DQ concern flagged as open for club confirmation

### Secondary (MEDIUM confidence)

- `.planning/REQUIREMENTS.md` — requirement text for RESULT-01..05, CHAMP-05
- `.planning/ROADMAP.md` — Phase 7 success criteria and carry-forward research items

---

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH — no new dependencies; all libraries verified in codebase
- Architecture patterns: HIGH — all patterns directly verifiable in existing code
- Championship standings algorithm: MEDIUM — logic is new; algorithm structure is ASSUMED from domain model analysis
- DNS counting policy: LOW — explicitly flagged as open in STATE.md; requires club confirmation
- Car tag snapshot semantics: LOW — "snapshotted at entry time" vs "current values" is ambiguous

**Research date:** 2026-05-01
**Valid until:** 2026-06-01 (stable stack; 30-day horizon)
