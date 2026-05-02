---
phase: 07-results-championship
reviewed: 2026-05-02T00:00:00Z
depth: standard
files_reviewed: 36
files_reviewed_list:
  - app/src/main/java/dev/monkeypatch/rctiming/api/pub/PublicChampionshipController.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/pub/PublicResultsController.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/dto/ResultSnapshotDto.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/RacerResultsController.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfile.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/race/RaceEntry.java
  - app/src/main/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQuery.java
  - app/src/main/java/dev/monkeypatch/rctiming/query/event/EventScheduleDto.java
  - app/src/main/java/dev/monkeypatch/rctiming/query/event/EventScheduleQuery.java
  - app/src/main/java/dev/monkeypatch/rctiming/query/racecontrol/ResultSnapshotQuery.java
  - app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryDto.java
  - app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQuery.java
  - app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java
  - app/src/main/java/dev/monkeypatch/rctiming/service/BumpUpSeedingService.java
  - app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java
  - app/src/main/java/dev/monkeypatch/rctiming/service/RoundGeneratorService.java
  - app/src/main/resources/db/migration/V24__phase7_results_championship.sql
  - app/src/test/java/dev/monkeypatch/rctiming/api/pub/PublicChampionshipControllerTest.java
  - app/src/test/java/dev/monkeypatch/rctiming/api/pub/PublicResultsControllerTest.java
  - app/src/test/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQueryTest.java
  - app/src/test/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQueryTest.java
  - frontend/src/App.tsx
  - frontend/src/components/racer/RacerEventHistoryCard.tsx
  - frontend/src/components/ui/collapsible.tsx
  - frontend/src/hooks/race-control/usePublicResultSnapshot.ts
  - frontend/src/hooks/racer/useRacerResults.ts
  - frontend/src/lib/adminApi.ts
  - frontend/src/lib/raceControlApi.ts
  - frontend/src/lib/racerApi.ts
  - frontend/src/pages/admin/championships/ChampionshipStandingsTable.tsx
  - frontend/src/pages/admin/club/ClubProfilePage.tsx
  - frontend/src/pages/championships/PublicChampionshipPage.tsx
  - frontend/src/pages/events/EventSchedulePage.tsx
  - frontend/src/pages/racer/RacerPortalLayout.tsx
  - frontend/src/pages/racer/RacerResultsPage.tsx
  - frontend/src/pages/results/PublicResultsPage.tsx
findings:
  critical: 4
  warning: 1
  info: 1
  total: 6
status: issues_found
---

# Phase 7: Code Review Report

**Reviewed:** 2026-05-02
**Depth:** standard
**Files Reviewed:** 36
**Status:** issues_found

## Summary

Phase 7 delivers results snapshots, championship standings, racer result history, and bump-up finals seeding. The backend service and query layers are generally well-structured with correct IDOR protections and appropriate CQRS boundaries. However, four blockers were found, two of which would cause visible data corruption or crashes in production: the `totalTimeMs` stored in result snapshots is an epoch timestamp rather than elapsed race time, and the public championship page silently calls the authenticated admin API endpoint rather than the public one (returning 403 for all anonymous visitors). Two additional blockers relate to null-safety crashes. One warning covers a silent data-loss edge case in the event schedule enrichment query.

---

## Critical Issues

### CR-01: `totalTimeMs` in result snapshot stores absolute epoch milliseconds, not elapsed race time

**File:** `app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java:87-90`

**Issue:** `leaderLastPassingMs` is set to `rows.get(0).lastPassingTimeMs()`, which is derived from `LapPassingEvent.rtcTimeMicros() / 1000` — a UTC epoch timestamp in milliseconds (confirmed: `SyntheticTimingService` uses `System.currentTimeMillis() * 1000L`; AMB P3 protocol RTC_TIME is "microseconds since Unix epoch"). The line `long totalTimeMs = leaderLastPassingMs > 0 ? row.lastPassingTimeMs() : 0L` stores each driver's absolute epoch timestamp as their total race time. When `fmtMs()` is called on this value in `PublicResultsPage`, it would render something like `472222:13:20.000` (hours since epoch) rather than an elapsed race duration.

**Fix:** Subtract the race start time from each driver's last passing time. The start time is available from `Race.getStartedAt()`. Example:

```java
long raceStartMs = race.getStartedAt() != null
    ? race.getStartedAt().toEpochMilli()
    : (rows.isEmpty() ? 0L : rows.get(0).lastPassingTimeMs());

for (LiveTimingRowDto row : rows) {
    String[] info = entryInfo.getOrDefault(row.entryId(), new String[]{"Unknown", null});
    long totalTimeMs = (raceStartMs > 0 && row.lastPassingTimeMs() > raceStartMs)
        ? row.lastPassingTimeMs() - raceStartMs
        : 0L;
    positions.add(new ResultSnapshotDto.ResultRow(
        row.position(), row.entryId(), info[0], info[1],
        row.lapsCompleted(), totalTimeMs,
        row.bestLapMs(), row.gapToLeaderMs(), null
    ));
}
```

Note: `gapToLeaderMs` (calculated in `LiveRaceState` as the difference between two epoch timestamps) is correct because it is already a delta. Only `totalTimeMs` needs the subtraction.

---

### CR-02: `PublicChampionshipPage` calls the authenticated admin API, not the public endpoint

**File:** `frontend/src/pages/championships/PublicChampionshipPage.tsx:1-13`

**Issue:** The page reuses `ChampionshipStandingsTable`, which calls `adminApi.championships.getStandings(id)`. That function targets `/api/v1/admin/championships/{id}/standings`, which is guarded by `requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "RACE_DIRECTOR", "REFEREE")` in `SecurityConfig`. The correct public endpoint for anonymous standings is `/api/v1/championships/{id}` (served by `PublicChampionshipController`, covered by `.requestMatchers(HttpMethod.GET, "/api/v1/championships/**").permitAll()`). Any unauthenticated visitor navigating to `/championships/:id` receives a 401/403 response and sees only the "Failed to load standings" error state.

**Fix:** Add a public standings fetcher in `raceControlApi.ts` and a dedicated hook:

```ts
// In frontend/src/lib/raceControlApi.ts
export async function getPublicStandings(id: number): Promise<StandingsRowDto[]> {
  const { data } = await api.get<StandingsRowDto[]>(`/api/v1/championships/${id}`);
  return data;
}
```

Then update `PublicChampionshipPage` to use its own query rather than `ChampionshipStandingsTable`:

```tsx
export default function PublicChampionshipPage() {
  const { id } = useParams<{ id: string }>();
  const championshipId = Number(id);
  const { data: rows, isLoading, isError } = useQuery({
    queryKey: ['public', 'championships', championshipId],
    queryFn: () => getPublicStandings(championshipId),
    enabled: Number.isFinite(championshipId) && championshipId > 0,
  });
  // render table from rows...
}
```

---

### CR-03: `resolveEntryInfo` crashes with `IllegalStateException` when multiple `RaceEntry` rows have `entryId = 0L`

**File:** `app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java:137`

**Issue:** `resolveEntryInfo` uses `Collectors.toMap(RaceEntry::getEntryId, ...)` with no merge function. `RoundGeneratorService.persistFinals()` creates placeholder `RaceEntry` rows with `entryId = 0L` for every slot in a finals race before seeding. `BumpUpSeedingService.seedFinals()` later fills regular slots with real entry IDs and leaves bump slots as `0L` until `applyBumpUpResults` is called. If `ResultSnapshotService.snapshot()` is invoked when any two race entries still share `entryId = 0L` (e.g., an A-final run before all bump slots are resolved), `Collectors.toMap` throws `IllegalStateException: Duplicate key 0` and the snapshot is not persisted.

**Fix:** Filter out placeholder entries before building the map, and use a merge function as a safety net:

```java
return raceEntries.stream()
    .filter(re -> re.getEntryId() != 0L)
    .collect(Collectors.toMap(
        RaceEntry::getEntryId,
        re -> { /* existing lambda */ },
        (a, b) -> a  // keep first on duplicate (shouldn't occur for real IDs)
    ));
```

---

### CR-04: `PublicResultsPage` throws `TypeError` when `clubBranding` is null

**File:** `frontend/src/pages/results/PublicResultsPage.tsx:72`

**Issue:** `ResultSnapshotQuery.fetchClubBranding()` returns `null` when the `club_profiles` table is empty (e.g., on a freshly-migrated instance or in a test environment). The `ResultSnapshotDto` is then constructed with `clubBranding = null`. The TypeScript type declares `clubBranding: ClubBrandingDto` (non-nullable), and `PublicResultsPage` line 72 unconditionally accesses `data.clubBranding.clubName`. When `clubBranding` is actually null at runtime, this throws a `TypeError: Cannot read properties of null (reading 'clubName')` and the page white-screens. The `PublicResultsControllerTest` does not create a club profile, so this path is untested.

**Fix (frontend):** Guard every access with optional chaining:

```tsx
<p className="text-sm text-muted-foreground mt-1">
  {data.clubBranding?.clubName ?? ''} &bull; Finished{' '}
  {new Date(data.finishedAt).toLocaleString()}
</p>
...
{data.clubBranding?.logoUrl && (
  <img src={data.clubBranding.logoUrl} alt={data.clubBranding.clubName ?? ''} ... />
)}
```

**Fix (backend):** Return a fallback branding object instead of null:

```java
private ResultSnapshotDto.ClubBrandingDto fetchClubBranding() {
    var club = dsl.select(CLUB_PROFILES.NAME, CLUB_PROFILES.LOGO_URL)
            .from(CLUB_PROFILES)
            .limit(1)
            .fetchOne();
    if (club == null) return new ResultSnapshotDto.ClubBrandingDto("", null);
    // ...existing logic
}
```

Update the TS type to make `clubBranding` nullable (`clubBranding: ClubBrandingDto | null`) if null is to be preserved at the contract level.

---

## Warnings

### WR-01: `EventScheduleQuery.fetchMap` silently drops all but one championship per event

**File:** `app/src/main/java/dev/monkeypatch/rctiming/query/event/EventScheduleQuery.java:85-86`

**Issue:** `fetchMap(CHAMPIONSHIP_EVENT_LINKS.EVENT_ID, CHAMPIONSHIP_EVENT_LINKS.CHAMPIONSHIP_ID)` builds a `Map<Long, Long>` keyed by `event_id`. If an event is linked to two championships (which the schema allows — `championship_event_links` has no unique constraint on `event_id`), jOOQ's `fetchMap` throws a `DataAccessException` on a duplicate key or silently overwrites the earlier entry depending on jOOQ version. The `EventScheduleDto` has a single `championshipId` field, so the design only supports one championship per event on the schedule page, but the silent data loss is not documented and the exception path is uncaught.

**Fix:** Either enforce at the DB level that an event may appear in at most one championship at a time (add a unique constraint on `championship_event_links.event_id`), or change the DTO to `List<Long> championshipIds` and use `fetchGroups`. If the single-championship constraint is intentional, add a comment explaining it and consider using `fetchOne` with an explicit ordering to make the selection deterministic.

---

## Info

### IN-01: `RacerResultHistoryQuery` compares `long` record accessor against `Long` variable using `==`

**File:** `app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQuery.java:108`

**Issue:** `r.entryId() == entryId` compares `long` (the `entryId` component of `ResultSnapshotDto.ResultRow`) against `Long entryId` (the jOOQ row value from `ENTRIES.ID`). Java auto-unboxes the `Long` to `long` for the comparison, so this is functionally correct as long as `ENTRIES.ID` is never null (which is guaranteed by the NOT NULL constraint). However, it is misleading — a reader might assume this is a reference equality check on `Long` objects.

**Fix:** Make the intent explicit:

```java
if (r.entryId() == entryId.longValue()) {
```

Or, since `ENTRIES.ID` is always a real PK, simply note a comment that unboxing is intentional. No behaviour change is needed.

---

_Reviewed: 2026-05-02_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
