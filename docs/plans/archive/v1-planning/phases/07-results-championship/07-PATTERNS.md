# Phase 7: Results & Championship — Pattern Map

**Mapped:** 2026-05-01
**Files analyzed:** 17 new/modified files
**Analogs found:** 16 / 17

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `app/.../db/migration/V24__phase7_results_championship.sql` | migration | batch | `V23__phase6_practice_sessions.sql` | exact |
| `app/.../query/championship/ChampionshipStandingsQuery.java` | service (jOOQ query) | CRUD + transform | `ResultSnapshotQuery.java` | exact (same JSONB deserialization pattern) |
| `app/.../query/results/RacerResultHistoryQuery.java` | service (jOOQ query) | CRUD | `EntryQueryService.java` | exact (same user-scoped join pattern) |
| `app/.../api/public/PublicResultsController.java` | controller | request-response | `EventScheduleController.java` | exact (public, no auth) |
| `app/.../api/public/PublicChampionshipController.java` | controller | request-response | `EventScheduleController.java` | exact (public, no auth) |
| `app/.../api/racer/RacerResultsController.java` | controller | request-response | `RacerProfileController.java` | exact (RACER-role, auth.getName() IDOR pattern) |
| `app/.../security/SecurityConfig.java` (modify) | config | — | self | self |
| `app/.../query/event/EventScheduleQuery.java` (modify) | service (jOOQ query) | CRUD | self | self |
| `app/.../query/event/EventScheduleDto.java` (modify) | DTO | — | `StandingsRowDto.java` | role-match |
| `frontend/src/App.tsx` (modify) | config/routing | — | self | self |
| `frontend/src/pages/racer/RacerPortalLayout.tsx` (modify) | layout | — | self | self |
| `frontend/src/pages/results/PublicResultsPage.tsx` | component | request-response | `PrintResultsPage.tsx` | exact |
| `frontend/src/pages/championships/PublicChampionshipPage.tsx` | component | request-response | `ChampionshipDetailPage.tsx` (public subset) | role-match |
| `frontend/src/pages/racer/RacerResultsPage.tsx` | component | request-response | `CarsPage.tsx` | exact (racer portal tab pattern) |
| `frontend/src/components/results/LapTimesCollapsible.tsx` | component | request-response | `ChampionshipStandingsTable.tsx` (table structure) | partial-match |
| `frontend/src/hooks/race-control/usePublicResultSnapshot.ts` | hook | request-response | `useResultSnapshot.ts` | exact |
| `frontend/src/pages/events/EventSchedulePage.tsx` (implement) | component | request-response | `CarsPage.tsx` (list page with loading states) | role-match |
| `frontend/src/pages/admin/club/ClubProfilePage.tsx` (modify) | component | CRUD | self | self |

---

## Pattern Assignments

### `app/.../db/migration/V24__phase7_results_championship.sql` (migration, batch)

**Analog:** `app/src/main/resources/db/migration/V23__phase6_practice_sessions.sql`

**Migration header pattern** (lines 1–4 of V23):
```sql
-- V23: Phase 6 Audio & Practice schema additions
```

**ALTER TABLE pattern** (lines 38–49 of V23):
```sql
ALTER TABLE club_profiles
    ADD COLUMN audio_settings JSONB NOT NULL DEFAULT '{...}'::jsonb,
    ADD COLUMN default_voice_id VARCHAR(100) NOT NULL DEFAULT 'en_GB-alan-medium';
```

**V24 must add:**
```sql
-- V24: Phase 7 — car_number on race_entries, show_car_tags_in_results on club_profiles

ALTER TABLE race_entries
    ADD COLUMN car_number int;

COMMENT ON COLUMN race_entries.car_number IS
    'Assigned by RoundGeneratorService on qualifying creation; re-numbered by BumpUpSeedingService on finals seeding.';

ALTER TABLE club_profiles
    ADD COLUMN show_car_tags_in_results boolean NOT NULL DEFAULT false;
```

Note: `car_number` is nullable (no DEFAULT, no NOT NULL) — consistent with pattern established by V23 nullable columns like `event_id BIGINT REFERENCES events(id)`.

---

### `app/.../query/championship/ChampionshipStandingsQuery.java` (service, CRUD + transform)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/query/racecontrol/ResultSnapshotQuery.java`

**Imports pattern** (lines 1–21 of ResultSnapshotQuery.java):
```java
package dev.monkeypatch.rctiming.query.championship;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.ChampionshipEventLinks.CHAMPIONSHIP_EVENT_LINKS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ResultSnapshots.RESULT_SNAPSHOTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Races.RACES;
// etc.
```

**Constructor injection pattern** (lines 28–34 of ResultSnapshotQuery.java):
```java
private final DSLContext dsl;
private final ObjectMapper objectMapper;

public ResultSnapshotQuery(DSLContext dsl, ObjectMapper objectMapper) {
    this.dsl = dsl;
    this.objectMapper = objectMapper;
}
```

**Core jOOQ join + JSONB deserialization pattern** (lines 34–84 of ResultSnapshotQuery.java):
```java
var row = dsl
    .select(
        RESULT_SNAPSHOTS.RACE_ID,
        RESULT_SNAPSHOTS.FINISHED_AT,
        RESULT_SNAPSHOTS.POSITIONS_JSON,
        RESULT_SNAPSHOTS.LAP_HISTORY_JSON,
        ROUNDS.ROUND_NUMBER,
        ROUNDS.TYPE,
        RACES.HEAT_NUMBER,
        RACES.FINAL_LETTER,
        RACING_CLASSES.NAME.as("className"))
    .from(RESULT_SNAPSHOTS)
    .join(RACES).on(RACES.ID.eq(RESULT_SNAPSHOTS.RACE_ID))
    .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
    .join(EVENT_CLASSES).on(EVENT_CLASSES.ID.eq(RACES.EVENT_CLASS_ID))
    .join(RACING_CLASSES).on(RACING_CLASSES.ID.eq(EVENT_CLASSES.RACING_CLASS_ID))
    .where(RESULT_SNAPSHOTS.RACE_ID.eq(raceId))
    .fetchOne();

if (row == null) {
    throw new EntityNotFoundException("No result snapshot for race " + raceId);
}

JSONB positionsJsonb = row.get(RESULT_SNAPSHOTS.POSITIONS_JSON);
try {
    List<ResultSnapshotDto.ResultRow> positions = objectMapper.readValue(
        positionsJsonb.data(),
        new TypeReference<List<ResultSnapshotDto.ResultRow>>() {});
} catch (Exception e) {
    throw new RuntimeException("Failed to deserialize result snapshot for race " + raceId, e);
}
```

**Class/transaction annotation pattern** (lines 23–24 of ResultSnapshotQuery.java):
```java
@Component
@Transactional(readOnly = true)
public class ResultSnapshotQuery {
```

**Car tags join pattern** (lines 43–55 of CarQueryService.java — use for joining car tags when `show_car_tags_in_results` is true):
```java
Map<Long, Map<String, String>> tagsByCarId = dsl
    .select(CTV.CAR_ID, CTC.NAME, CTV.VALUE)
    .from(CTV)
    .join(CTC).on(CTC.ID.eq(CTV.CATEGORY_ID))
    .where(CTV.CAR_ID.in(carIds))
    .fetchGroups(CTV.CAR_ID).entrySet().stream()
    .collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> e.getValue().stream().collect(Collectors.toMap(
            r -> r.get(CTC.NAME),
            r -> r.get(CTV.VALUE),
            (a, b) -> a, LinkedHashMap::new))
    ));
```

**Two-pass join for car tags** (from CarQueryService.java lines 30–68): For each result row, we have an `entryId` that maps to `entries.car_id`. Use the same two-pass pattern: load entries first, collect car IDs, then load tags in a second query with `WHERE car_id IN (...)`. This avoids Cartesian explosion.

---

### `app/.../query/results/RacerResultHistoryQuery.java` (service, CRUD)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/query/entry/EntryQueryService.java`

**Full class pattern** (lines 1–49 of EntryQueryService.java):
```java
package dev.monkeypatch.rctiming.query.results;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Events.EVENTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RaceEntries.RACE_ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ResultSnapshots.RESULT_SNAPSHOTS;
// etc.

@Service
@Transactional(readOnly = true)
public class RacerResultHistoryQuery {

    private final DSLContext dsl;

    public RacerResultHistoryQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<RacerResultHistoryDto> findForUser(Long userId) {
        return dsl.select(...)
                 .from(ENTRIES)
                 .join(EVENTS).on(EVENTS.ID.eq(ENTRIES.EVENT_ID))
                 // join race_entries and result_snapshots per event
                 .where(ENTRIES.USER_ID.eq(userId))
                 .orderBy(EVENTS.EVENT_DATE.desc())
                 .fetch(r -> new RacerResultHistoryDto(...));
    }
}
```

**User-scoped query pattern** (line 22 of EntryQueryService.java — the key IDOR safety point):
```java
.where(ENTRIES.USER_ID.eq(userId))
```

The `userId` passed in comes exclusively from `Authentication.getName()` in the controller — never from a request parameter.

---

### `app/.../api/public/PublicResultsController.java` (controller, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/racer/EventScheduleController.java`

**Full controller pattern** (lines 1–26 of EventScheduleController.java):
```java
package dev.monkeypatch.rctiming.api.public;

import dev.monkeypatch.rctiming.api.racecontrol.dto.ResultSnapshotDto;
import dev.monkeypatch.rctiming.query.racecontrol.ResultSnapshotQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/results")
// NO @PreAuthorize — public endpoint, permitted in SecurityConfig
public class PublicResultsController {

    private final ResultSnapshotQuery query;

    public PublicResultsController(ResultSnapshotQuery query) {
        this.query = query;
    }

    @GetMapping("/{raceId}")
    public ResultSnapshotDto get(@PathVariable Long raceId) {
        return query.load(raceId);
        // EntityNotFoundException from query → 404 via GlobalExceptionHandler
    }
}
```

No `@PreAuthorize` annotation — the SecurityConfig `permitAll()` rule covers this path. This is the exact pattern of `EventScheduleController` (no annotation at all on the class or method).

---

### `app/.../api/public/PublicChampionshipController.java` (controller, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/racer/EventScheduleController.java`

**Same pattern as PublicResultsController** — no `@PreAuthorize`, single `@GetMapping("/{id}")`, delegates to `ChampionshipStandingsQuery.computeStandings(id)`.

---

### `app/.../api/racer/RacerResultsController.java` (controller, request-response)

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/racer/RacerProfileController.java`

**Auth + IDOR-safe pattern** (lines 29–49 of RacerProfileController.java):
```java
@RestController
@RequestMapping("/api/v1/racer")
@PreAuthorize("hasRole('RACER')")
public class RacerResultsController {

    private final RacerResultHistoryQuery query;

    public RacerResultsController(RacerResultHistoryQuery query) {
        this.query = query;
    }

    @GetMapping("/results")
    public List<RacerResultHistoryDto> getMyResults(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());  // IDOR safety: user ID from JWT, never from request param
        return query.findForUser(userId);
    }
}
```

Critical: `Long userId = Long.parseLong(auth.getName())` — extracted from the JWT principal, not from any query/path parameter. This is the identical pattern used in `RacerProfileController.getProfile()` at line 48.

---

### `app/.../security/SecurityConfig.java` (modify, config)

**Source:** `app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java` (lines 29–36)

**Existing public route block to extend:**
```java
.requestMatchers("/api/v1/auth/**").permitAll()
.requestMatchers("/actuator/health").permitAll()
.requestMatchers("/error").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/**").permitAll()
// Phase 7: add these two lines in the same block, before .anyRequest().authenticated()
.requestMatchers(HttpMethod.GET, "/api/v1/results/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/championships/**").permitAll()
```

The new rules must appear before `.requestMatchers("/api/v1/admin/**").hasAnyRole(...)` and before `.anyRequest().authenticated()`. Order matters in Spring Security's filter chain.

---

### `app/.../query/event/EventScheduleQuery.java` + `EventScheduleDto.java` (modify)

**Source:** `app/src/main/java/dev/monkeypatch/rctiming/query/event/EventScheduleQuery.java`

**Existing fetch pattern** (lines 23–56) to extend with additional joins:

The current `EventScheduleDto` constructor (line 50–53):
```java
return new EventScheduleDto(
    r.get(EVENTS.ID),
    r.get(EVENTS.NAME),
    r.get(EVENTS.EVENT_DATE),
    avail);
```

Phase 7 adds two nullable fields to `EventScheduleDto`:
- `List<Long> finishedRaceIds` — from `races` where `status = 'FINISHED'` for this event
- `Long championshipId` — from `championship_event_links` for this event (null if unlinked)

These require either a subquery or a left join. Use the existing `fetchGroups` pattern from `CarQueryService` lines 43–55: do the main events query first, then a second query for finished race IDs and championship links, merge in Java. This avoids a Cartesian join.

---

## Frontend Pattern Assignments

### `frontend/src/pages/results/PublicResultsPage.tsx` (component, request-response)

**Analog:** `frontend/src/pages/race-control/PrintResultsPage.tsx`

**Full page structure** (lines 1–104 of PrintResultsPage.tsx):
```tsx
import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useResultSnapshot } from '@/hooks/race-control/useResultSnapshot';

// Phase 7: use usePublicResultSnapshot instead — points to /api/v1/results/{raceId}

export default function PublicResultsPage() {
  const { raceId: raceIdStr } = useParams<{ raceId: string }>();
  const raceId = Number(raceIdStr);

  const { data, isLoading, error } = usePublicResultSnapshot(raceId || null);

  useEffect(() => {
    if (data) document.title = `Results — ${data.raceLabel}`;
  }, [data]);

  if (isLoading) return <div className="p-8 text-sm text-muted-foreground">Loading results…</div>;
  if (error || !data) return <div className="p-8 text-sm text-destructive">...</div>;

  return (
    <div className="p-8 max-w-3xl mx-auto print:p-4">
      {/* Header — identical to PrintResultsPage lines 44–61 */}
      {/* Results table — identical structure; rows become CollapsibleTrigger */}
      {/* Print button — identical to PrintResultsPage lines 94–101 */}
    </div>
  );
}
```

The difference from `PrintResultsPage`: (1) uses `usePublicResultSnapshot`, (2) each result row has a collapsible lap times section, (3) mounted on an unprotected route. The outer page shell is identical — copy lines 42–104 directly.

**Expandable row pattern** — wrap each `<tr>` in conditional rendering, NOT `Collapsible` wrapping `<tr>` (Pitfall 5 in RESEARCH.md). Use plain `useState`:
```tsx
const [expandedEntryId, setExpandedEntryId] = useState<number | null>(null);

// In tbody:
{data.positions.map((row) => (
  <>
    <tr key={row.entryId}
        className="border-b border-border/50 cursor-pointer select-none min-h-[44px]"
        onClick={() => setExpandedEntryId(expandedEntryId === row.entryId ? null : row.entryId)}>
      {/* existing cells */}
      <td><i className={`ri-arrow-down-s-line ${expandedEntryId === row.entryId ? 'rotate-180' : ''}`} /></td>
    </tr>
    {expandedEntryId === row.entryId && (
      <tr key={`${row.entryId}-laps`}>
        <td colSpan={8}><LapTimesCollapsible entryId={row.entryId} lapHistory={data.lapHistory} /></td>
      </tr>
    )}
  </>
))}
```

---

### `frontend/src/pages/championships/PublicChampionshipPage.tsx` (component, request-response)

**Analog:** `frontend/src/pages/admin/championships/ChampionshipStandingsTable.tsx`

**Reuse pattern** — wrap `ChampionshipStandingsTable` in a minimal public page shell:
```tsx
import { useParams } from 'react-router-dom';
import { ChampionshipStandingsTable } from '@/pages/admin/championships/ChampionshipStandingsTable';
// + fetch championship name/description for the page header

export default function PublicChampionshipPage() {
  const { id } = useParams<{ id: string }>();
  const championshipId = Number(id);
  // ...
  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold mb-2">{detail.name}</h1>
      <p className="text-sm text-muted-foreground mb-6">{detail.description}</p>
      <ChampionshipStandingsTable championshipId={championshipId} />
    </div>
  );
}
```

The `ChampionshipStandingsTable` already handles loading/error/empty states and renders the drop-score cells (line 35: `result.dropped ? 'text-slate-300 line-through' : ''`). The token fix from the UI-SPEC: change `text-slate-300` to `text-muted-foreground` in `ChampionshipStandingsTable.tsx` line 35.

**Drop score styling** (line 35 of ChampionshipStandingsTable.tsx — the one line to fix):
```tsx
// Current (broken token):
className={`text-center text-sm ${result.dropped ? 'text-slate-300 line-through' : ''}`}
// Fixed:
className={`text-center text-sm ${result.dropped ? 'text-muted-foreground line-through' : ''}`}
```

---

### `frontend/src/pages/racer/RacerResultsPage.tsx` (component, request-response)

**Analog:** `frontend/src/pages/racer/CarsPage.tsx`

**Loading/error/empty pattern** (lines 17–34 of CarsPage.tsx):
```tsx
if (isPending) {
  return (
    <div aria-live="polite" className="max-w-5xl mx-auto">
      <div className="animate-pulse bg-muted rounded h-8 w-24" />
      {/* skeleton rows */}
    </div>
  );
}
if (error) {
  return <div role="alert" className="text-destructive">Unable to load results.</div>;
}
```

**Page header pattern** (lines 36–41 of CarsPage.tsx):
```tsx
<div className="max-w-5xl mx-auto">
  <div className="flex items-center justify-between mb-4">
    <h1 className="text-2xl font-semibold">Results</h1>
  </div>
  {/* expandable event list */}
</div>
```

Each event row is expandable (similar to the results page collapsible pattern above). Each race entry within an event links to `/results/:raceId`.

---

### `frontend/src/components/results/LapTimesCollapsible.tsx` (component, transform)

**Analog:** `frontend/src/pages/admin/championships/ChampionshipStandingsTable.tsx` (inner cell component pattern)

**Inner component pattern** (lines 18–39 of ChampionshipStandingsTable.tsx — `RoundCell` component):
```tsx
function LapTimesCollapsible({ entryId, lapHistory }: { entryId: number; lapHistory: PositionAtLap[] }) {
  const laps = lapHistory.filter(l => l.entryId === entryId);

  if (laps.length === 0) {
    return <p className="text-xs text-muted-foreground p-3">No lap data recorded.</p>;
  }

  return (
    <div className="bg-muted/30 px-4 py-2">
      <table className="w-full text-xs font-mono">
        <tbody>
          {laps.map((l) => (
            <tr key={l.lapNumber}>
              <td className="pr-4 text-muted-foreground">Lap {l.lapNumber}</td>
              <td className="text-right">{fmtMs(l.lapTimeMs)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

Note: `lapHistory` in `ResultSnapshotDto` is `PositionAtLap[]` (position-at-each-lap), not per-lap times. Individual lap times are in `ResultRow` — check the actual DTO fields before implementing. The backend may need to add a `lapTimes: LapTimeDto[]` field per driver to `positions_json` for RESULT-05 to be useful.

---

### `frontend/src/hooks/race-control/usePublicResultSnapshot.ts` (hook, request-response)

**Analog:** `frontend/src/hooks/race-control/useResultSnapshot.ts`

**Full hook pattern** (lines 1–13 of useResultSnapshot.ts):
```ts
import { useQuery } from '@tanstack/react-query';
import { getPublicResultSnapshot, type ResultSnapshotDto } from '@/lib/raceControlApi';

export function usePublicResultSnapshot(raceId: number | null) {
  return useQuery<ResultSnapshotDto>({
    queryKey: ['public', 'results', raceId ?? -1],
    queryFn: () => getPublicResultSnapshot(raceId!),
    enabled: raceId !== null,
    staleTime: 60_000,   // public results are stable; longer stale time than authenticated hook
  });
}
```

The corresponding `getPublicResultSnapshot` API function goes in `raceControlApi.ts` or a new `publicApi.ts`:
```ts
export async function getPublicResultSnapshot(raceId: number): Promise<ResultSnapshotDto> {
  const { data } = await api.get<ResultSnapshotDto>(`/api/v1/results/${raceId}`);
  return data;
}
```

No auth token is sent (the endpoint is public). The existing `api` axios instance adds a Bearer token header if one exists in localStorage — this is harmless for a public endpoint.

---

### `frontend/src/App.tsx` (modify, routing)

**Source:** `frontend/src/App.tsx` (lines 49–132)

**Existing public route pattern** (line 127):
```tsx
{ path: '/events', element: <EventSchedulePage /> },
```

**Phase 7 additions** — insert alongside the existing `/events` public route:
```tsx
{ path: '/results/:raceId', element: <PublicResultsPage /> },
{ path: '/championships/:id', element: <PublicChampionshipPage /> },
```

**Racer portal children** (lines 88–95) — add results route:
```tsx
children: [
  { index: true, element: <Navigate to="/racer/profile" replace /> },
  { path: 'profile', element: <ProfilePage /> },
  { path: 'cars', element: <CarsPage /> },
  { path: 'transponders', element: <TranspondersPage /> },
  { path: 'entries', element: <EntriesPage /> },
  { path: 'results', element: <RacerResultsPage /> },  // Phase 7 addition
],
```

---

### `frontend/src/pages/racer/RacerPortalLayout.tsx` (modify, layout)

**Source:** `frontend/src/pages/racer/RacerPortalLayout.tsx` (lines 1–82)

**navItems array** (lines 5–10):
```tsx
const navItems = [
  { to: '/racer/profile', label: 'Profile', Icon: User },
  { to: '/racer/cars', label: 'Cars', Icon: Car },
  { to: '/racer/transponders', label: 'Transponders', Icon: Radio },
  { to: '/racer/entries', label: 'Entries', Icon: FileText },
] as const;
```

Phase 7 adds one entry. The existing pattern uses lucide icons; the UI-SPEC mandates remixicon for new Phase 7 elements. Use a wrapper component for the remixicon:

```tsx
// Addition — use remixicon inline rather than as a lucide Icon component
{ to: '/racer/results', label: 'Results', icon: 'ri-trophy-line' },
```

However, because the existing `navItems` type is narrowly typed with `Icon: ComponentType`, the cleanest approach is to add an optional `riIcon?: string` field or create a separate entry that renders differently. Alternatively, wrap remixicon in a thin component matching the `Icon` interface. The planner should decide the exact approach — both work.

---

### `frontend/src/pages/events/EventSchedulePage.tsx` (implement, request-response)

**Analog:** `frontend/src/pages/racer/CarsPage.tsx` (list page with loading/error/empty)

**Loading skeleton pattern** (lines 17–30 of CarsPage.tsx):
```tsx
if (isPending) {
  return (
    <div aria-live="polite" className="max-w-5xl mx-auto">
      {[1, 2, 3].map(i => (
        <div key={i} className="animate-pulse bg-muted rounded h-32 mb-3" />
      ))}
    </div>
  );
}
```

The page is a public page (no auth), so it uses the public event schedule query directly (no `@tanstack/react-query` auth headers needed). Link to `/results/:raceId` for finished races and `/championships/:id` when `championshipId` is present on the event.

---

### `frontend/src/pages/admin/club/ClubProfilePage.tsx` (modify, CRUD)

**Source:** `frontend/src/pages/admin/club/ClubProfilePage.tsx`

**Toggle pattern** — the `show_car_tags_in_results` boolean maps to a Switch component. The existing page uses `Label` + `Input` pairs (lines 157–195). Add a `Switch` section using the existing `@/components/ui/switch` component already in the codebase:

```tsx
import { Switch } from '@/components/ui/switch';

// In the form:
<div className="flex items-center gap-3">
  <Switch
    id="show-car-tags"
    checked={values.showCarTagsInResults}
    onCheckedChange={(checked) => setValue('showCarTagsInResults', checked)}
  />
  <Label htmlFor="show-car-tags">Show car tags in printed results</Label>
</div>
```

Add `showCarTagsInResults: z.boolean()` to the existing Zod schema at line 16.

---

## Shared Patterns

### Authentication — IDOR Safety
**Source:** `app/src/main/java/dev/monkeypatch/rctiming/api/racer/RacerProfileController.java` lines 47–48
**Apply to:** `RacerResultsController.java`
```java
@GetMapping("/results")
public List<RacerResultHistoryDto> getMyResults(Authentication auth) {
    Long userId = Long.parseLong(auth.getName());  // JWT principal, NOT a request param
    return query.findForUser(userId);
}
```

### Public Endpoint Security Pattern
**Source:** `app/src/main/java/dev/monkeypatch/rctiming/api/racer/EventScheduleController.java` (no annotation = public) + `SecurityConfig.java` lines 33
**Apply to:** `PublicResultsController.java`, `PublicChampionshipController.java`
- No `@PreAuthorize` on controller
- Add `requestMatchers(HttpMethod.GET, "/api/v1/results/**").permitAll()` to SecurityConfig
- Add `requestMatchers(HttpMethod.GET, "/api/v1/championships/**").permitAll()` to SecurityConfig

### jOOQ @Transactional(readOnly = true) Pattern
**Source:** All query classes — `ResultSnapshotQuery.java` line 23, `EntryQueryService.java` line 15, `EventScheduleQuery.java` line 14
**Apply to:** `ChampionshipStandingsQuery.java` (already has it), `RacerResultHistoryQuery.java`
```java
@Service
@Transactional(readOnly = true)
public class NewQueryClass {
    private final DSLContext dsl;
    public NewQueryClass(DSLContext dsl) { this.dsl = dsl; }
}
```

### EntityNotFoundException → 404 Pattern
**Source:** `app/src/main/java/dev/monkeypatch/rctiming/query/racecontrol/ResultSnapshotQuery.java` lines 54–56
**Apply to:** `PublicResultsController` and `PublicChampionshipController` — no try/catch needed in the controller; the `GlobalExceptionHandler` already maps `EntityNotFoundException` to HTTP 404.
```java
if (row == null) {
    throw new EntityNotFoundException("No result snapshot for race " + raceId);
}
```

### TanStack Query Hook Pattern
**Source:** `frontend/src/hooks/race-control/useResultSnapshot.ts` (lines 1–13)
**Apply to:** `usePublicResultSnapshot.ts`, any new racer results hook
```ts
export function useXxx(id: number | null) {
  return useQuery<XxxDto>({
    queryKey: [..., id ?? -1],
    queryFn: () => apiCall(id!),
    enabled: id !== null,
    staleTime: 30_000,
  });
}
```

### Racer Portal Page Loading/Error States
**Source:** `frontend/src/pages/racer/CarsPage.tsx` lines 17–34
**Apply to:** `RacerResultsPage.tsx`, `EventSchedulePage.tsx`
- `isPending` → skeleton `animate-pulse bg-muted rounded` divs
- `error` → `<div role="alert" className="text-destructive">Unable to load ...</div>`
- Empty → centered `text-muted-foreground py-12` with explanatory message

### Print-Page Structure
**Source:** `frontend/src/pages/race-control/PrintResultsPage.tsx` lines 42–103, `PrintPracticeResultsPage.tsx` lines 58–113
**Apply to:** `PublicResultsPage.tsx`
- Outer container: `className="p-8 max-w-3xl mx-auto print:p-4"`
- Print button: `className="mt-6 print:hidden"` wrapping `onClick={() => window.print()}`
- Tables: `className="w-full text-sm border-collapse"`
- Money columns: `className="text-right font-mono"`

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| (none) | — | — | All files have usable analogs in the codebase |

The one file closest to "no analog" is `LapTimesCollapsible.tsx` — there is no existing collapsible sub-row component. The closest analog for its inner structure is `RoundCell` in `ChampionshipStandingsTable.tsx`. The collapsible state management uses plain `useState` (Pitfall 5 — Radix Collapsible cannot be a `<tr>` child without `asChild` workarounds).

---

## Metadata

**Analog search scope:** `app/src/main/java/dev/monkeypatch/rctiming/` (all Java source), `frontend/src/` (all TypeScript source)
**Files scanned:** ~35 (Java) + ~85 (TypeScript) = ~120 total
**Pattern extraction date:** 2026-05-01

**Critical implementation notes captured:**
1. `usePublicResultSnapshot` must point to `/api/v1/results/{raceId}` — NOT the existing `/api/v1/race-control/race/{raceId}/result-snapshot` (Pitfall 7)
2. Expandable rows use `useState` + conditional `<tr>`, NOT `Collapsible` wrapping `<tr>` (Pitfall 5)
3. `RacerResultsController` must use `Authentication.getName()` — never a request param (IDOR critical)
4. `ChampionshipStandingsTable.tsx` line 35: change `text-slate-300` to `text-muted-foreground` (token fix, UI-SPEC)
5. `ChampionshipStandingsQuery` must remove the `ChampionshipRepository` dependency — use DSLContext only (architecture rule: no Hibernate in query module)
6. V24 `car_number` is nullable — no backfill of historical snapshots (Pitfall 2 accepted)
