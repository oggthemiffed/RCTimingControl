---
phase: 07-results-championship
verified: 2026-05-03T00:00:00Z
status: human_needed
score: 3/3 must-haves verified
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 2/3
  gaps_closed:
    - "RESULT-05 individual lap times: PositionAtLap now carries Long lapTimeMs (nullable), populated from LiveRaceState.getPositionSnapshot(entryId).getLapTimes() in buildLapHistory, rendered as Lap|Time|Pos table in LapTimesPanel"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Navigate to /results/{a finished raceId} without logging in"
    expected: "Result table renders with driver positions, lap counts, and time totals. Clicking a row expands to show a Lap | Time | Pos table with formatted durations (e.g. 23.456s) or em-dash for legacy rows."
    why_human: "Requires a running server and a finished race in the database to confirm end-to-end including the new lapTimeMs column."
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

# Phase 7: Results & Championship Verification Report (Re-verification)

**Phase Goal:** Final race results are published after each race, championship standings update automatically, and per-racer history is visible on the portal
**Verified:** 2026-05-03
**Status:** human_needed
**Re-verification:** Yes — after gap closure plan 07-07 (RESULT-05 per-lap times)

## Goal Achievement

### Observable Truths (from ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 1 | Final race results published publicly after each race, correctly reflecting all marshal lap adjustments and penalties, **including every individual lap time** | VERIFIED | All three sub-requirements confirmed: (a) marshal adjustments flow via LiveRaceState.applyLapDelta → calculatePositions → ResultSnapshotService; (b) PositionAtLap now has `Long lapTimeMs` (4th field, nullable); (c) buildLapHistory uses per-entry lapIndex counter to look up pos.getLapTimes().get(idx); (d) LapTimesPanel renders Lap|Time|Pos with formatLapTime(ms) → "X.XXXs" or "—" |
| 2 | Championship standings table is live on the web with no login required, showing results in best-to-worst order per driver with drop scores visible | VERIFIED | Unchanged from initial verification: PublicChampionshipController at /api/v1/championships/{id} — permitAll in SecurityConfig; PublicChampionshipPage renders inline standings with dropped round strikethrough via text-muted-foreground line-through |
| 3 | Per-racer result history viewable on racer portal; printed results optionally display car tag values (admin setting) | VERIFIED | Unchanged from initial verification: RacerResultHistoryQuery.findForUser() with IDOR guard; RacerResultsPage; /racer/results route; showCarTagsInResults Switch in ClubProfilePage; ResultSnapshotQuery two-pass car tag enrichment |

**Score:** 3/3 truths verified

### Gap Closure: RESULT-05 Detail

**Previous gap:** `PositionAtLap(lapNumber, entryId, position)` had no `lapTimeMs`; `LapTimesPanel` showed `P{position}` labels only.

**Closure evidence (all confirmed by direct file reads):**

| Check | File | Finding | Status |
|-------|------|---------|--------|
| `List<Long> lapTimes` field | LiveRacePosition.java line 19 | `private final List<Long> lapTimes = new ArrayList<>()` | VERIFIED |
| `getLapTimes()` getter | LiveRacePosition.java line 50 | `public List<Long> getLapTimes() { return lapTimes; }` | VERIFIED |
| `getLapTimes().add(lapMs)` at primary applyLapPassing path | LiveRaceState.java line 87 | appends after `pos.accumulateLap(lapMs)` | VERIFIED |
| `getLapTimes().add(lapMs)` at replay path (applyPositionUpdate) | LiveRaceState.java line 231 | appends after `pos.accumulateLap(lapMs)` in transponder-link replay | VERIFIED |
| `getPositionSnapshot` method | LiveRaceState.java line 247 | `public synchronized LiveRacePosition getPositionSnapshot(long entryId)` — read-only, no mutation | VERIFIED |
| `Long lapTimeMs` in PositionAtLap record | ResultSnapshotDto.java line 26 | `public record PositionAtLap(int lapNumber, long entryId, int position, Long lapTimeMs)` — boxed Long, nullable | VERIFIED |
| `buildLapHistory(rows, state)` 2-arg signature | ResultSnapshotService.java line 162 | Accepts nullable `LiveRaceState state` | VERIFIED |
| Per-entry lapIndex counter | ResultSnapshotService.java lines 167–182 | `Map<Long, Integer> lapIndex = new HashMap<>()` + `lapIndex.merge(row.entryId(), 1, Integer::sum) - 1` | VERIFIED |
| `state.getPositionSnapshot(row.entryId())` wiring | ResultSnapshotService.java line 174 | Fetches pos, reads `pos.getLapTimes().get(idx)` | VERIFIED |
| 4-arg PositionAtLap constructor call | ResultSnapshotService.java line 179 | `new ResultSnapshotDto.PositionAtLap(lap, row.entryId(), row.position(), lapTimeMs)` | VERIFIED |
| No 3-arg PositionAtLap constructor anywhere | grep across app/src/main | Only 1 match; inspection confirms it IS the 4-arg call (regex false positive — 4 args present) | VERIFIED |
| `lapTimeMs?: number \| null` type | raceControlApi.ts line 62 | Optional + nullable — handles legacy absent field and new null-valued entries | VERIFIED |
| `lapTimeMs` references in LapTimesPanel | PublicResultsPage.tsx | 2 occurrences: `formatLapTime(l.lapTimeMs)` and `data-ms={l.lapTimeMs ?? ''}` | VERIFIED |
| `formatLapTime` helper | PublicResultsPage.tsx lines 21–24 | `(ms / 1000).toFixed(3) + 's'` with em-dash fallback for null/undefined | VERIFIED |
| 3-column table with `<thead>` | PublicResultsPage.tsx lines 26–33 | `<th>Lap</th>`, `<th>Time</th>`, `<th>Pos</th>` | VERIFIED |
| Backward compat: em-dash on null | PublicResultsPage.tsx line 22 | `if (ms === null \|\| ms === undefined) return '—'` | VERIFIED |

**Git commits confirmed:** All 7 plan-07-07 commits exist in HEAD history:
- 7ba770d feat(07-07): add ordered lap-time list to LiveRacePosition
- b1b6ed4 feat(07-07): record per-lap durations in LiveRaceState and add getPositionSnapshot
- fc03d44 feat(07-07): add nullable Long lapTimeMs to PositionAtLap record
- 64c69ad feat(07-07): populate lapTimeMs in buildLapHistory from LiveRaceState
- 1fca00c feat(07-07): add optional lapTimeMs to frontend PositionAtLap type
- 667e312 feat(07-07): render per-lap durations in LapTimesPanel (RESULT-05)
- da8f084 fix(07-07): split lapTimeMs attribute across lines for grep clarity

### Key Link Verification (Re-verified for gap-closure changes)

| From | To | Via | Status |
|------|----|-----|--------|
| LiveRaceState.applyLapPassing | LiveRacePosition.lapTimes list | `pos.getLapTimes().add(lapMs)` (line 87) | WIRED |
| LiveRaceState.applyPositionUpdate (replay) | LiveRacePosition.lapTimes list | `pos.getLapTimes().add(lapMs)` (line 231) | WIRED |
| ResultSnapshotService.buildLapHistory | LiveRaceState.getPositionSnapshot | `state.getPositionSnapshot(row.entryId())` (line 174) | WIRED |
| ResultSnapshotService.buildLapHistory | LiveRacePosition.getLapTimes | `pos.getLapTimes().get(idx)` (line 175) | WIRED |
| PublicResultsPage.LapTimesPanel | PositionAtLap.lapTimeMs | `formatLapTime(l.lapTimeMs)` in table cell | WIRED |

All key links from initial verification (Truth 2 and Truth 3) remain unchanged and were not modified by plan 07-07.

### Requirements Coverage

| Requirement | Plans | Description | Status | Evidence |
|-------------|-------|-------------|--------|---------|
| RESULT-01 | 02, 04, 05 | Final race results published publicly | SATISFIED | PublicResultsController, SecurityConfig permitAll, PublicResultsPage |
| RESULT-02 | 02, 04 | Results reflect marshal adjustments and penalties | SATISFIED | applyLapDelta → calculatePositions → snapshot. Marshal path confirmed. |
| RESULT-03 | 03, 06 | Per-racer result history on racer portal | SATISFIED | RacerResultHistoryQuery.findForUser() with IDOR guard; RacerResultsPage; /racer/results route |
| RESULT-04 | 02, 04, 06 | Car tags in printed results, admin-controlled | SATISFIED | showCarTagsInResults in V24 migration, ClubProfile, ResultSnapshotQuery enrichment, ClubProfilePage Switch |
| RESULT-05 | 04, 05, 07 | Full individual lap time data (every lap) | SATISFIED | PositionAtLap carries `Long lapTimeMs`; populated from getLapTimes() in buildLapHistory; LapTimesPanel renders Lap\|Time\|Pos with formatted durations |
| CHAMP-05 | 03, 04, 05 | Public championship standings live on web | SATISFIED | PublicChampionshipController, SecurityConfig permitAll, PublicChampionshipPage with drop scores |

### Behavioral Spot-Checks

Step 7b: SKIPPED — Docker unavailable; cannot run Spring Boot + Testcontainers. Backend main sources compile clean (jOOQ generated sources present from main repo build). TypeScript compiles clean (`npx tsc --noEmit`).

### Anti-Patterns Scan (Re-check of modified files)

No TODO/FIXME/placeholder comments found in any of the 6 files modified by plan 07-07. No hardcoded empty returns in the data path. The `if (state != null)` null guard in buildLapHistory is correct defensive coding for legacy snapshot loads, not a stub.

### Human Verification Required

All four human verification items from the initial verification remain — they cannot be automated without a running server. The lap times item now specifically requires confirming the new Lap|Time|Pos table renders correctly.

#### 1. Public results page with per-lap durations (RESULT-05 end-to-end)

**Test:** Without logging in, navigate to `/results/{raceId}` for a finished race that was recorded after this change.
**Expected:** Result table renders; clicking a row expands to show a 3-column Lap|Time|Pos table with lap durations formatted as "23.456s" per lap. Legacy snapshots (stored before this change) should show "—" in the Time column.
**Why human:** Requires running server + finished race in database with live state captured at snapshot time.

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

### Summary

The RESULT-05 BLOCKER gap from the initial verification is fully closed. All three must-have truths are now VERIFIED:

1. **Truth 1 (RESULT-05 was the gap):** `PositionAtLap` carries a nullable `Long lapTimeMs`. `LiveRaceState` records per-lap durations in `LiveRacePosition.lapTimes` at both call sites (primary path + transponder-link replay). `buildLapHistory` uses a per-entry index counter to look up the correct duration. `LapTimesPanel` renders a 3-column Lap|Time|Pos table with `formatLapTime` helper. Backward compatibility confirmed via boxed `Long` (null for missing field in legacy JSONB) and `'—'` em-dash fallback in the UI.

2. **Truth 2 (championship standings):** Unchanged and verified.

3. **Truth 3 (racer history + car tags):** Unchanged and verified.

Four human verification items remain — these require a running server with data and were already present in the initial verification. The automated evidence is complete; the phase is ready for human sign-off.

---

_Verified: 2026-05-03T00:00:00Z_
_Verifier: Claude (gsd-verifier)_
