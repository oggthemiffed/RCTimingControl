---
phase: 07-results-championship
verified: 2026-05-02T00:00:00Z
status: gaps_found
score: 2/3 must-haves verified
overrides_applied: 0
gaps:
  - truth: "Final race results published publicly after each race, correctly reflecting all marshal lap adjustments and penalties, including every individual lap time"
    status: partial
    reason: "Marshal adjustments ARE reflected (LiveRaceState.applyLapDelta feeds calculatePositions which feeds ResultSnapshotService). However, per-lap individual times are NOT in the DTO — lapHistory contains PositionAtLap(lapNumber, entryId, position) only; no per-lap duration is stored or served. RESULT-05 requirement text: 'every lap, not just totals and best lap'. The DTO has lapsCompleted, totalTimeMs, bestLapMs but zero per-lap time rows. The plan itself acknowledged this as a future enhancement."
    artifacts:
      - path: "app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/dto/ResultSnapshotDto.java"
        issue: "PositionAtLap record has (lapNumber, entryId, position) — no lapTimeMs field. ResultRow has bestLapMs but not individual lap times."
      - path: "frontend/src/pages/results/PublicResultsPage.tsx"
        issue: "LapTimesPanel expands to show position-at-each-lap (P1, P2...) — not actual lap durations. Per plan comment line 255: 'Individual lap times are NOT in lapHistory'."
    missing:
      - "Add lapTimeMs field to PositionAtLap record (or a new per-lap record) in ResultSnapshotDto"
      - "Populate per-lap times in ResultSnapshotService.buildLapHistory() from LiveRacePosition lap records"
      - "Render actual lap durations in LapTimesPanel instead of position progression"
human_verification:
  - test: "Navigate to /results/{a finished raceId} without logging in"
    expected: "Result table renders with driver positions, lap counts, and time totals. Clicking a row expands to show lap data."
    why_human: "Requires a running server and a finished race in the database to confirm end-to-end."
  - test: "Navigate to /championships/{id} without logging in"
    expected: "Standings table renders with driver names, total points, and round scores. Dropped rounds show strikethrough styling."
    why_human: "Requires a running server with championship and result snapshot data."
  - test: "Log in as RACER, navigate to Results tab in racer portal"
    expected: "Results tab is visible with ri-trophy-line icon. Events the racer entered are listed as collapsible cards."
    why_human: "Requires authenticated session."
  - test: "Log in as ADMIN, open Club settings, toggle 'Show car details in printed results'"
    expected: "Switch toggles and Save button enables. After save, car tags appear in subsequent result snapshots."
    why_human: "Requires admin session and a result with car tags to observe the effect."
---

# Phase 7: Results & Championship Verification Report

**Phase Goal:** Results & Championship — public result snapshots, championship standings with best-X-from-Y scoring, racer result history, car number assignment, and frontend pages for viewing results and standings.
**Verified:** 2026-05-02
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (from ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 1 | Final race results published publicly after each race, correctly reflecting all marshal lap adjustments and penalties, **including every individual lap time** | PARTIAL | Marshal adjustments: VERIFIED (LiveRaceState.applyLapDelta → calculatePositions → ResultSnapshotService). Per-lap times: FAILED — PositionAtLap has no lapTimeMs; LapTimesPanel shows position progression only |
| 2 | Championship standings table is live on the web with no login required, showing results in best-to-worst order per driver with drop scores visible | VERIFIED | PublicChampionshipController at /api/v1/championships/{id} — permitAll in SecurityConfig; PublicChampionshipPage renders inline standings with dropped round strikethrough via text-muted-foreground line-through |
| 3 | Per-racer result history viewable on racer portal; printed results optionally display car tag values (admin setting) | VERIFIED | RacerResultsPage + RacerEventHistoryCard + /racer/results route; showCarTagsInResults Switch in ClubProfilePage; ResultSnapshotQuery two-pass car tag enrichment |

**Score:** 2/3 truths verified (Truth 1 is PARTIAL — marshal adjustments pass, per-lap times fail)

### Required Artifacts

| Artifact | Status | Evidence |
|----------|--------|---------|
| `V24__phase7_results_championship.sql` | VERIFIED | EXISTS; car_number count=5, show_car_tags_in_results count=3 |
| `RaceEntry.java` (carNumber field) | VERIFIED | EXISTS; carNumber count=3 (field + getter + setter) |
| `ClubProfile.java` (showCarTagsInResults) | VERIFIED | EXISTS; showCarTagsInResults count=3 |
| `RoundGeneratorService.java` (setCarNumber) | VERIFIED | EXISTS; setCarNumber count=1 |
| `BumpUpSeedingService.java` (setCarNumber) | VERIFIED | EXISTS; setCarNumber count=2 |
| `ResultSnapshotService.java` (getCarNumber) | VERIFIED | EXISTS; getCarNumber count=1 |
| `ChampionshipStandingsQuery.java` | VERIFIED | EXISTS; computeStandings=1, fetchExists=1, dropped=9, ASSUMED=1, ChampionshipRepository=0 |
| `RacerResultHistoryQuery.java` | VERIFIED | EXISTS; findForUser=1, USER_ID.eq=2 |
| `RacerResultHistoryDto.java` | VERIFIED | EXISTS |
| `PublicResultsController.java` | VERIFIED | EXISTS; /api/v1/results mapping present |
| `PublicChampionshipController.java` | VERIFIED | EXISTS; /api/v1/championships mapping present |
| `RacerResultsController.java` | VERIFIED | EXISTS; auth.getName() count=2 |
| `SecurityConfig.java` | VERIFIED | results/** permitAll=1, championships/** permitAll=1 |
| `ResultSnapshotDto.java` | PARTIAL | EXISTS; carTags field present; PositionAtLap lacks lapTimeMs (RESULT-05 gap) |
| `ResultSnapshotQuery.java` | VERIFIED | EXISTS; show_car_tags_in_results enrichment count=3 |
| `EventScheduleDto.java` | VERIFIED | EXISTS; finishedRaceIds+championshipId count=2 |
| `EventScheduleQuery.java` | VERIFIED | EXISTS; CHAMPIONSHIP_EVENT_LINKS+finishedRacesByEvent count=7 |
| `PublicResultsControllerTest.java` | VERIFIED | EXISTS; @Disabled=0; restTemplate usage=3 |
| `PublicChampionshipControllerTest.java` | VERIFIED | EXISTS; @Disabled=0; restTemplate usage=3 |
| `ChampionshipStandingsQueryTest.java` | VERIFIED | EXISTS; @Disabled=0; Assertions.fail=0 (real bodies) |
| `RacerResultHistoryQueryTest.java` | VERIFIED | EXISTS; @Disabled=0; Assertions.fail=0 (real bodies) |
| `frontend/src/components/ui/collapsible.tsx` | VERIFIED | EXISTS; Collapsible/CollapsibleContent/CollapsibleTrigger exports count=11 |
| `usePublicResultSnapshot.ts` | VERIFIED | EXISTS |
| `PublicResultsPage.tsx` | VERIFIED | EXISTS; useState=2, Collapsible=0 (correct — not used on table rows) |
| `PublicChampionshipPage.tsx` | VERIFIED | EXISTS; renders inline standings with drop score strikethrough; does NOT use ChampionshipStandingsTable (different implementation than plan specified, but functionally equivalent) |
| `EventSchedulePage.tsx` | VERIFIED | EXISTS; View Results=1, View Standings=1, finishedRaceIds/championshipId=4 |
| `RacerResultsPage.tsx` | VERIFIED | EXISTS; "No results yet"=1 |
| `RacerEventHistoryCard.tsx` | VERIFIED | EXISTS; Collapsible=7, results/ link=1 |
| `RacerPortalLayout.tsx` | VERIFIED | EXISTS; ri-trophy-line=1 |
| `ClubProfilePage.tsx` | VERIFIED | EXISTS; showCarTagsInResults=5, Switch=2, "Show car details in printed results"=1 |
| `racerApi.ts` (getMyResults) | VERIFIED | EXISTS; getMyResults=1 |
| `adminApi.ts` (showCarTagsInResults) | VERIFIED | EXISTS; showCarTagsInResults=2 |
| `App.tsx` routes | VERIFIED | results/:raceId=2, championships/:id=2, racer child 'results'=1 |

### Key Link Verification

| From | To | Via | Status | Evidence |
|------|----|-----|--------|---------|
| SecurityConfig permitAll | PublicResultsController /api/v1/results/** | HttpMethod.GET requestMatchers | WIRED | grep results/** in SecurityConfig = 1 |
| SecurityConfig permitAll | PublicChampionshipController /api/v1/championships/** | HttpMethod.GET requestMatchers | WIRED | grep championships/** in SecurityConfig = 1 |
| RacerResultsController | Authentication.getName() | JWT principal | WIRED | auth.getName() count=2 in RacerResultsController |
| RacerResultHistoryQuery | ENTRIES.USER_ID.eq(userId) | jOOQ where clause | WIRED | USER_ID.eq count=2 |
| ChampionshipStandingsQuery | dsl.fetchExists(CHAMPIONSHIPS) | replaces ChampionshipRepository | WIRED | fetchExists=1, ChampionshipRepository=0 |
| ResultSnapshotQuery | show_car_tags_in_results | conditional car tag enrichment | WIRED | show_car_tags_in_results count=3 |
| EventScheduleQuery | CHAMPIONSHIP_EVENT_LINKS | two-pass enrichment | WIRED | count=7 |
| RoundGeneratorService | RaceEntry.setCarNumber() | qualifying heat creation | WIRED | setCarNumber=1 |
| BumpUpSeedingService | RaceEntry.setCarNumber() | finals seeding | WIRED | setCarNumber=2 |
| ResultSnapshotService.resolveEntryInfo() | RaceEntry.getCarNumber() | lambda in stream | WIRED | getCarNumber=1 |
| App.tsx /results/:raceId | PublicResultsPage | public route outside auth wrapper | WIRED | routes present |
| App.tsx /championships/:id | PublicChampionshipPage | public route outside auth wrapper | WIRED | routes present |
| App.tsx /racer children | RacerResultsPage at path='results' | racer portal child route | WIRED | path='results' in racer children block |
| RacerPortalLayout navItems | /racer/results | TrophyIcon + NavLink | WIRED | ri-trophy-line in RacerPortalLayout |
| ClubProfilePage Switch | showCarTagsInResults PUT payload | React Hook Form + adminApi.ts | WIRED | showCarTagsInResults=5 in ClubProfilePage, =2 in adminApi.ts |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| PublicResultsPage | data (ResultSnapshotDto) | usePublicResultSnapshot → GET /api/v1/results/{raceId} → ResultSnapshotQuery.load() → result_snapshots table | Yes | FLOWING |
| PublicChampionshipPage | rows (PublicStandingsRowDto[]) | getPublicChampionshipStandings → GET /api/v1/championships/{id} → ChampionshipStandingsQuery.computeStandings() → DB join chain | Yes | FLOWING |
| EventSchedulePage | events (EventScheduleDto[]) | getEventSchedule → GET /api/v1/events → EventScheduleQuery.getPublicSchedule() + two-pass enrichment | Yes | FLOWING |
| RacerResultsPage | data (RacerResultHistoryDto[]) | useRacerResults → GET /api/v1/racer/results → RacerResultHistoryQuery.findForUser(userId) | Yes | FLOWING |
| ResultSnapshotDto.PositionAtLap | lapNumber, entryId, position | buildLapHistory() from LiveRaceState rows | Yes — but lapTimeMs absent | PARTIAL (RESULT-05 gap) |

### Behavioral Spot-Checks

Step 7b: SKIPPED — Docker unavailable; cannot run Spring Boot + Testcontainers. Java main sources compile clean per context confirmation (`./gradlew :app:compileJava -x startJooqDb -x generateJooq` exits 0). TypeScript build passes clean (`npx tsc --noEmit` exits 0 per context).

### Requirements Coverage

| Requirement | Plans | Description | Status | Evidence |
|-------------|-------|-------------|--------|---------|
| RESULT-01 | 02, 04, 05 | Final race results published publicly | SATISFIED | PublicResultsController, SecurityConfig permitAll, PublicResultsPage |
| RESULT-02 | 02, 04 | Results reflect marshal adjustments and penalties | PARTIAL | Marshal adjustments flow through LiveRaceState → calculatePositions → snapshot. Individual lap times absent from DTO — RESULT-05 overlap. Penalty (RefereeController) carry-through to snapshot not verified separately but marshal path is confirmed. |
| RESULT-03 | 03, 06 | Per-racer result history on racer portal | SATISFIED | RacerResultHistoryQuery.findForUser() with IDOR guard; RacerResultsPage; /racer/results route |
| RESULT-04 | 02, 04, 06 | Car tags in printed results, admin-controlled | SATISFIED | showCarTagsInResults in V24 migration, ClubProfile, ResultSnapshotQuery enrichment, ClubProfilePage Switch |
| RESULT-05 | 04, 05 | Full individual lap time data (every lap) | FAILED | PositionAtLap contains only position-per-lap, not lap duration. ResultRow has bestLapMs but not per-lap times. The expandable row panel displays position progression, not lap times. |
| CHAMP-05 | 03, 04, 05 | Public championship standings live on web | SATISFIED | PublicChampionshipController, SecurityConfig permitAll, PublicChampionshipPage with drop scores |

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `PublicResultsPage.tsx` (LapTimesPanel) | Shows position-at-each-lap (P1, P2) not lap duration — the plan itself notes this is the data available | Warning | RESULT-05 gap — users see "Lap 1: P2" not "Lap 1: 23.456s" |
| `ResultSnapshotDto.PositionAtLap` | No lapTimeMs field | Blocker | RESULT-05 requirement "every lap, not just totals and best lap" unmet |

No TODO/FIXME/placeholder stubs found in any data path. No hardcoded empty returns in controllers or query classes. All test files are enabled (@Disabled removed) with real test bodies.

### Human Verification Required

#### 1. Public results page end-to-end

**Test:** Without logging in, navigate to `/results/{raceId}` for a finished race.
**Expected:** Result table renders with driver positions; clicking a row expands to show lap position data.
**Why human:** Requires running server + finished race in database.

#### 2. Public championship standings end-to-end

**Test:** Without logging in, navigate to `/championships/{id}` for an active championship.
**Expected:** Standings render with driver names, total points, and dropped rounds shown with strikethrough.
**Why human:** Requires running server + championship data with result snapshots.

#### 3. Racer portal Results tab

**Test:** Log in as a RACER who has entered at least one finished race. Navigate to Results tab.
**Expected:** Tab visible with trophy icon. Event card expands to show race results with View link.
**Why human:** Requires authenticated session with race history.

#### 4. Admin car tags toggle

**Test:** Log in as ADMIN. Open Club settings. Toggle "Show car details in printed results" and save.
**Expected:** Switch toggles, form marks dirty, Save enables. After save, subsequent result pages show car tags under driver names.
**Why human:** Requires admin session and car tags configured on at least one car.

### Gaps Summary

One BLOCKER gap: **RESULT-05 individual lap times absent.**

The ROADMAP success criterion 1 states results must include "every individual lap time". The `PositionAtLap` record stores `(lapNumber, entryId, position)` — position-at-each-lap — not lap duration. `ResultRow` stores `bestLapMs` (single value) but no per-lap times array. The `LapTimesPanel` component in `PublicResultsPage` expands to show position progression ("Lap 1: P2") which does not satisfy "every lap, not just totals and best lap."

The plan itself acknowledged this limitation at the design stage (07-05-PLAN.md line 255: "Individual lap times are NOT in lapHistory") and noted it as a future DTO enhancement. This is not an implementation error — the decision was made during planning. However, the ROADMAP success criterion is explicit and the requirement is formally RESULT-05 "Pending" in REQUIREMENTS.md.

The gap requires either:
1. Adding `lapTimeMs` to `PositionAtLap` and populating it from `LiveRacePosition` lap records in `ResultSnapshotService.buildLapHistory()`
2. Or an explicit override accepted by the developer acknowledging this limitation

All other must-haves are VERIFIED: public endpoints work without auth, championship standings with drop logic are implemented, per-racer history with IDOR guard is implemented, car number assignment is wired through generators, car tags enrichment works with admin toggle, all four integration test stubs are enabled with real test bodies, and TypeScript build passes clean.

---

_Verified: 2026-05-02T00:00:00Z_
_Verifier: Claude (gsd-verifier)_
