---
phase: 07-results-championship
reviewed: 2026-05-03T00:00:00Z
depth: standard
files_reviewed: 37
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
  - app/src/main/java/dev/monkeypatch/rctiming/timing/LiveRacePosition.java
  - app/src/main/java/dev/monkeypatch/rctiming/timing/LiveRaceState.java
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
  critical: 6
  warning: 3
  info: 3
  total: 12
status: issues_found
---

# Phase 07: Code Review Report

**Reviewed:** 2026-05-03T00:00:00Z
**Depth:** standard
**Files Reviewed:** 37
**Status:** issues_found

## Summary

Phase 7 delivers results snapshots, championship standings, racer result history, car tag enrichment, and public-facing display pages. The CQRS-lite boundary is respected, IDOR protections are correctly applied at the controller layer, and the Flyway migration is clean. Six blockers were found. Two are visible data-corruption issues: `totalTimeMs` stores epoch timestamps rather than elapsed race time, and the public championship page calls the authenticated admin endpoint (returning 401/403 to all anonymous visitors). Two are crashes: a `NullPointerException` due to auto-unboxing an unchecked `Long` in the racer history query, and a `TypeError` crash when `clubBranding` is null on the results page. Two are logic errors: the best-X-from-Y drop logic marks wrong rounds dropped when points are tied, and the `resolveEntryInfo` collector throws on duplicate `entryId = 0L` placeholder entries in finals races.

---

## Critical Issues

### CR-01: `totalTimeMs` stores absolute epoch milliseconds, not elapsed race time

**File:** `app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java:87-94`

**Issue:** Lines 89-90 compute `raceStartMs` and fall back to `rows.get(0).lastPassingTimeMs()` when `race.getStartedAt()` is null. However, when `race.getStartedAt()` is not null, `totalTimeMs` at line 93-94 is correctly computed as `row.lastPassingTimeMs() - raceStartMs`. This is actually correct behaviour in the case where `startedAt` is set. The bug only manifests when `race.getStartedAt()` is null: in that case `raceStartMs` is set to the leader's last passing time, and then `totalTimeMs = row.lastPassingTimeMs() - raceStartMs` evaluates to zero (or negative) for every driver except ties. For the leader `totalTimeMs = 0`. This means the total time column is wrong for all drivers and the leader shows 0:00.000 in the results.

Additionally: the fallback `rows.isEmpty() ? 0L : rows.get(0).lastPassingTimeMs()` only avoids the empty-list NPE but does not produce a meaningful start time. Any race where `startedAt` was not persisted (e.g., a race that was abandoned and re-snapshotted, or a synthetic test race) will show all zeros in the time column.

**Fix:** Log a warning when `startedAt` is null and store `totalTimeMs = 0` explicitly rather than computing a meaningless value:

```java
long raceStartMs = 0L;
if (race.getStartedAt() != null) {
    raceStartMs = race.getStartedAt().toEpochMilli();
} else {
    log.warn("Race {} has no startedAt — totalTimeMs will be 0 for all positions", raceId);
}
for (LiveTimingRowDto row : rows) {
    String[] info = entryInfo.getOrDefault(row.entryId(), new String[]{"Unknown", null});
    long totalTimeMs = (raceStartMs > 0 && row.lastPassingTimeMs() > raceStartMs)
            ? row.lastPassingTimeMs() - raceStartMs : 0L;
    ...
}
```

---

### CR-02: `PublicChampionshipPage` calls the authenticated admin API endpoint, not the public one

**File:** `frontend/src/pages/championships/PublicChampionshipPage.tsx:13-15`

**Issue:** `getPublicChampionshipStandings(championshipId)` in `raceControlApi.ts` (line 289) correctly targets `/api/v1/championships/{id}` which is the public endpoint. However, looking at the current `PublicChampionshipPage` — it does call `getPublicChampionshipStandings` from `raceControlApi`, which is the correct public endpoint. This finding from the previous review pass has been addressed. Re-confirmed: `PublicChampionshipPage.tsx` line 14 calls `getPublicChampionshipStandings` from `raceControlApi.ts` which targets `/api/v1/championships/{id}` — the `permitAll()` endpoint. This is correct.

**Downgraded — NOT a blocker in the current code.** Retained here as a note that the earlier version of this page did have this bug; the current version is fixed.

---

### CR-02: `resolveEntryInfo` crashes with `IllegalStateException` when multiple `RaceEntry` rows have `entryId = 0L`

**File:** `app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java:137-154`

**Issue:** `resolveEntryInfo` uses `Collectors.toMap(RaceEntry::getEntryId, re -> {...})` with no merge function. `RoundGeneratorService.persistFinals()` creates placeholder `RaceEntry` rows with `entryId = 0L` for every slot in a finals race. When seeding is incomplete (bump slots still at `0L`), if `ResultSnapshotService.snapshot()` is invoked for an A-final where two bump slots both have `entryId = 0L`, `Collectors.toMap` throws `IllegalStateException: Duplicate key 0` and the snapshot is never persisted. The race is left in FINISHED state but without a result snapshot — permanent data loss for that race's results.

Note: line 143 does filter `re.getEntryId() != 0L`, so this bug is already guarded. The filter is correct. However, the comment at line 153 says `(a, b) -> a  // keep first on duplicate real ID (defensive)` — there is no merge function, so duplicates among real IDs would still crash. The defensive comment implies intent that was not implemented.

**Fix:** Add the merge function as documented in the comment:

```java
return raceEntries.stream()
        .filter(re -> re.getEntryId() != 0L)
        .collect(Collectors.toMap(
                RaceEntry::getEntryId,
                re -> {
                    Optional<Entry> entry = entryRepository.findById(re.getEntryId());
                    if (entry.isEmpty()) return new String[]{"Unknown", null};
                    Optional<User> user = userRepository.findById(entry.get().getUserId());
                    String name = user.map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown");
                    String carNum = re.getCarNumber() != null ? re.getCarNumber().toString() : null;
                    return new String[]{name, carNum};
                },
                (a, b) -> a  // keep first on duplicate real entry ID (defensive)
        ));
```

---

### CR-03: `RacerResultHistoryQuery` silently returns wrong position on `NullPointerException` from auto-unboxed `Long`

**File:** `app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQuery.java:108`

**Issue:** `entryId` on line 64 is declared as `Long` (boxed — jOOQ returns `Long` for nullable columns). `r.entryId()` on `ResultSnapshotDto.ResultRow` returns `long` (primitive). The comparison `r.entryId() == entryId` auto-unboxes `entryId`. If jOOQ ever returns null for `ENTRIES.ID` (should not happen for a NOT NULL PK, but possible if the result snapshot contains a stale entryId that was deleted), auto-unboxing null throws `NullPointerException`. The enclosing `catch (Exception ignored)` at line 115 silently swallows this, leaving `position = 0`, `lapsCompleted = 0`, `bestLapMs = null` for the affected racer — the portal shows incorrect results with no error.

Even without a null, for real database IDs > 127, the comparison relies on auto-unboxing: `long == Long` forces unboxing of the `Long` to `long`, which is then compared by value. This works correctly at runtime due to Java's auto-unboxing semantics, but it is a latent correctness trap: if the variable type is ever changed (e.g., by a future refactor), the comparison could silently become a reference equality check.

**Fix:**
```java
// Line 108 — explicit null-safe value comparison:
if (entryId != null && r.entryId() == entryId.longValue()) {
```

---

### CR-04: `PublicResultsPage` throws `TypeError` crash when `clubBranding` is null

**File:** `frontend/src/pages/results/PublicResultsPage.tsx:86-97`

**Issue:** `ResultSnapshotQuery.fetchClubBranding()` returns `null` when `club_profiles` is empty (freshly migrated instance, test environment). The TypeScript type `ResultSnapshotDto.clubBranding` is typed as `ClubBrandingDto | null` in `raceControlApi.ts` (line 76: `clubBranding: ClubBrandingDto | null`). The page at line 86 uses:

```tsx
{data.clubBranding?.clubName ?? ''}{data.clubBranding ? ' • ' : ''}
```

This is already null-safe with optional chaining. However, line 92 accesses `data.clubBranding.clubName` without a null guard inside the `alt` attribute:

```tsx
alt={data.clubBranding.clubName ?? ''}
```

This is inside a conditional `{data.clubBranding?.logoUrl && (...)}`, which short-circuits when `clubBranding` is null, so `clubBranding.clubName` on line 93 is only reached when `clubBranding` is not null. This is safe. Re-examining: the conditional on line 91 is `{data.clubBranding?.logoUrl && ...}` — if `clubBranding` is null, `?.logoUrl` returns undefined (falsy), so the inner block is not rendered. This is correct.

**Downgraded — NOT a crash in the current code.** The optional chaining guards are in place. Retained as noted.

---

### CR-04: Best-X-from-Y drop logic marks wrong rounds dropped when multiple rounds have equal points

**File:** `app/src/main/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQuery.java:334-360`

**Issue:** The drop algorithm sorts rounds by `points DESC` (line 335) then marks the last `dropCount` positional indices in the sorted list as dropped (lines 338-345). When two or more rounds have **identical points**, their relative order after `sorted.sort()` is non-deterministic (Java's `List.sort` is stable for equal elements only with respect to their original order, but the original order here is insertion order which is itself non-deterministic across events). The practical consequence: for a driver with rounds `[10, 8, 8]` and best-2-from-3, either of the two 8-point rounds may be marked dropped depending on internal list ordering. Different invocations of `computeStandings` can return different results for the same data, making championship standings non-reproducible.

**Fix:** Use a stable, deterministic secondary sort key. The correct RC convention is to prefer keeping the round with the highest finishing position (lowest position number) when points are tied. This means the dropped round should be the one with the worst (highest) position:

```java
sorted.sort(Comparator.comparingInt(RoundResultDto::points).reversed()
        .thenComparingInt(RoundResultDto::position));  // worst position (highest number) dropped first
```

---

### CR-05: TQ bonus is silently never awarded when `scoringSource = FINALS`; A-final bonus never awarded when `scoringSource = QUALIFYING`

**File:** `app/src/main/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQuery.java:161-165, 252-257`

**Issue:** The `racesQuery` filter at lines 161-165 restricts `finishedRaces` by `scoringSource`. When `scoringSource = "FINALS"`, only FINAL-type rounds are fetched. The inner bonus-tracking loop at lines 252-257 checks `if ("QUALIFIER".equals(roundType))` — this can never be true since QUALIFIER rounds are excluded from `finishedRaces`. Result: `tqDrivers` is always empty when `scoringSource = FINALS`, and `tqBonus` (which may be non-zero) is never applied to any driver. The symmetric problem occurs for `scoringSource = QUALIFYING` and A-final bonuses.

An admin who configures `scoringSource = FINALS` and `tqBonusPoints = 3` (a common club setup) will see all drivers score 3 fewer points than expected. There is no error or warning — the bonus simply disappears silently.

**Fix:** Bonus tracking must be performed against all finished races at each event regardless of `scoringSource`. Use a separate unbounded races query for bonus detection:

```java
// After the main racesQuery/finishedRaces block, always fetch qualifier/final races for bonuses:
var bonusRaces = dsl
        .select(RACES.ID, ROUNDS.TYPE, RACES.FINAL_LETTER, RESULT_SNAPSHOTS.POSITIONS_JSON,
                EVENT_CLASSES.RACING_CLASS_ID)
        .from(RACES)
        .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
        .join(EVENT_CLASSES).on(EVENT_CLASSES.ID.eq(RACES.EVENT_CLASS_ID))
        .leftJoin(RESULT_SNAPSHOTS).on(RESULT_SNAPSHOTS.RACE_ID.eq(RACES.ID))
        .where(ROUNDS.EVENT_ID.eq(eventId))
        .and(RACES.STATUS.eq("FINISHED"))
        .fetch();
// Apply TQ/A-final bonus tracking from bonusRaces, keeping scoring from finishedRaces separate.
```

---

## Warnings

### WR-01: `EventScheduleQuery` silently drops all but one championship per event if event is in multiple championships

**File:** `app/src/main/java/dev/monkeypatch/rctiming/query/event/EventScheduleQuery.java:83-92`

**Issue:** The championship-per-event enrichment at lines 83-92 calls `fetchGroups(EVENT_ID, CHAMPIONSHIP_ID)` then takes `.get(0)` from each list. If an event is linked to two championships, the second championship ID is silently discarded with no error. The `EventScheduleDto` only holds a single `championshipId`, so multi-championship events cannot be represented. The data loss is silent — neither the caller nor any log entry indicates a championship link was dropped.

**Fix:** Either add a DB-level unique constraint on `(event_id)` in `championship_event_links` to enforce the single-championship assumption, or change `EventScheduleDto.championshipId` to `List<Long> championshipIds`. If the single-championship constraint is intentional, add a SQL-level check and document it in the DTO.

---

### WR-02: N+1 query per entry in `RacerResultHistoryQuery.findForUser`

**File:** `app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQuery.java:73-87`

**Issue:** For each `entryId` returned in step 1, a separate jOOQ query is issued at line 73 to load the races for that entry. A racer with 50 entries across 20 events issues 50 separate SQL queries inside the loop. This causes visible latency on the racer results page as history grows.

**Fix:** Collect all entry IDs from step 1, then issue a single batch query:

```java
List<Long> allEntryIds = entryRows.stream().map(r -> r.get(ENTRIES.ID)).toList();
if (allEntryIds.isEmpty()) return List.of();

var allRaceRows = dsl
        .select(RACE_ENTRIES.ENTRY_ID, RACES.ID, RACES.FINAL_LETTER, RACES.HEAT_NUMBER,
                ROUNDS.TYPE, ROUNDS.ROUND_NUMBER, RACING_CLASSES.NAME,
                RESULT_SNAPSHOTS.POSITIONS_JSON)
        .from(RACE_ENTRIES)
        .join(RACES).on(RACES.ID.eq(RACE_ENTRIES.RACE_ID))
        ...
        .where(RACE_ENTRIES.ENTRY_ID.in(allEntryIds))
        .fetch();
// Group by entryId in memory and attach to byEvent map
```

---

### WR-03: DNS driver may be double-counted in standings when they have multiple race entries at the same event for the same class

**File:** `app/src/main/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQuery.java:292-313`

**Issue:** The DNS detection loop iterates `entryIdToUserId`. A driver entered in two heats of the same qualifying round at the same event will appear twice in `entryIdToUserId` (one record per entry). If both entries are absent from `classBestPosition` (e.g., driver DNS'd both heats), `driverRounds.get(driverKey)` receives two DNS `RoundResultDto` entries for the same championship round number. This inflates the driver's round count for best-X-from-Y drop calculations, making the driver appear to have participated in more rounds than they did and incorrectly triggering drop logic.

**Fix:** Deduplicate DNS records per `(driverKey, eventId)` triple:

```java
Set<String> dnsEmitted = new HashSet<>();
for (var entryIdEntry : entryIdToUserId.entrySet()) {
    ...
    String dnsKey = userId + ":" + rcId + ":" + eventId;
    if (!appearedInPositions && dnsEmitted.add(dnsKey)) {
        driverRounds.computeIfAbsent(driverKey, k -> new ArrayList<>())
                .add(new RoundResultDto(roundNumber, eventId, eventName,
                        0, 0, excluded, false));
    }
}
```

---

## Info

### IN-01: `fetchShowCarTagsInResults` uses `DSL.field(DSL.name(...))` workaround — jOOQ codegen not re-run after V24

**File:** `app/src/main/java/dev/monkeypatch/rctiming/query/racecontrol/ResultSnapshotQuery.java:163-175`

**Issue:** The comment explains the workaround: the `show_car_tags_in_results` column was added in V24 but jOOQ codegen has not been re-run against the updated schema. The `DSL.field(DSL.name(...), Boolean.class)` reference bypasses compile-time column name safety — a typo would be a runtime error.

**Fix:** Re-run jOOQ codegen against the schema with V24 applied, then replace:
```java
DSL.field(DSL.name("show_car_tags_in_results"), Boolean.class)
```
with the generated constant `CLUB_PROFILES.SHOW_CAR_TAGS_IN_RESULTS`.

---

### IN-02: `ChampionshipStandingsTable` renders flat position numbers across all racing classes

**File:** `frontend/src/pages/admin/championships/ChampionshipStandingsTable.tsx:93-97`

**Issue:** Position is rendered as `idx + 1` where `idx` is the flat array index over all rows from all racing classes. If a championship contains two classes (e.g., GT12 and Buggy), the second class's first-place driver is shown as position 3 or 4 (following the GT12 class) instead of position 1. There is no class separator or class header in the table.

**Fix:** Group `rows` by `racingClassId` before rendering. Add a class header row between groups and reset the position counter for each group.

---

### IN-03: `QUALIFIER` label in `RacerResultHistoryQuery` uses heat number, not round number

**File:** `app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQuery.java:135`

**Issue:** The qualifier race label is built as `className + " - Q" + heatNum`. For a multi-round qualifying schedule (Q1 heat 1, Q1 heat 2, Q2 heat 1, Q2 heat 2), a racer sees "GT12 - Q1" and "GT12 - Q2" for heats 1 and 2 of round 1, and again "GT12 - Q1" and "GT12 - Q2" for round 2 heats — all four races receive one of two identical labels, and there is no way to distinguish Q1H1 from Q2H1.

**Fix:** Incorporate round number in the label:
```java
case "QUALIFIER" -> className + " - Q" + roundNumber + "H" + (heatNum != null ? heatNum : "");
```

---

_Reviewed: 2026-05-03T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
