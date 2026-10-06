# Phase 7: Results & Championship - Context

**Gathered:** 2026-05-01
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 7 delivers three capabilities:

1. **Public race results** — final race results are published at a public URL after each race, with individual lap times accessible per racer. Linked from the public event schedule.
2. **Racer result history** — a new page in the racer portal showing each racer's history of events and race results.
3. **Public championship standings** — championship standings table at a public URL (no login required), with drop scores visible, linked from the public event schedule.

Also completes the backend championship standings calculation (jOOQ query) and adds car tag display to printed results.

</domain>

<decisions>
## Implementation Decisions

### Public Results Access (RESULT-01, RESULT-05)
- **D-01:** New public route `/results/:raceId` — no login required. Reuses the existing `PrintResultsPage` component but mounted on an unprotected route.
- **D-02:** Individual lap times (RESULT-05) displayed as an **expandable row per racer** — click/tap to expand and see all lap times. Keeps the main results table clean.
- **D-03:** Once a race is FINISHED, a results link appears on the public event schedule page (`/events`), linking to `/results/:raceId`.

### Racer History Page (RESULT-03)
- **D-04:** New **Results** page in the racer portal (alongside profile, cars, transponders, entries).
- **D-05:** Top-level display: list of events the racer entered, each **expandable** to show race results for that event (position, laps, best lap per race).
- **D-06:** Each race entry links through to `/results/:raceId` (the full public results page).

### Car Tags in Printed Results (RESULT-04)
- **D-07:** **All car tags** for the racer's entered car are displayed beneath their name in printed results (no category filtering — show all key/value pairs snapshotted at entry time).
- **D-08:** Global admin toggle in **Admin → Club settings** (club profile page) to enable/disable car tag display in results. Single on/off for the whole club.

### Championship Standings Public Page (CHAMP-05)
- **D-09:** New public route `/championships/:id` — no login required. Separate from the admin `ChampionshipDetailPage` which keeps its management controls.
- **D-10:** Public page shows **standings table with drop scores visible** — driver, total points, per-round scores, with dropped rounds visually distinguished (greyed out / struck through). Best-to-worst order (CHAMP-10).
- **D-11:** If an event is associated with a championship, a **Standings link** appears on the public event schedule page (`/events`) linking to `/championships/:id`.

### Claude's Discretion
- Exact visual styling of the drop-score indicator (greyed out vs struck through vs badge) — either is fine.
- Pagination or infinite scroll on racer history (small clubs, so either is fine).
- Whether the public championship page reuses `ChampionshipStandingsTable` component from admin or builds its own — reuse if it fits, build new if not.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope
- `.planning/ROADMAP.md` — Phase 7 goal, requirements list (RESULT-01..05, CHAMP-05), success criteria
- `.planning/REQUIREMENTS.md` — Full requirement text for RESULT-01, RESULT-02, RESULT-03, RESULT-04, RESULT-05, CHAMP-05

### Existing infrastructure to extend
- `app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java` — captures result snapshots on race FINISH; Phase 7 exposes these publicly
- `app/src/main/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQuery.java` — jOOQ stub with Phase 7 TODO; implement full standings join here
- `frontend/src/pages/race-control/PrintResultsPage.tsx` — existing print results component; Phase 7 mounts it on a public route and adds expandable lap times
- `frontend/src/pages/admin/championships/ChampionshipStandingsTable.tsx` — existing standings table component; consider reusing for public page
- `frontend/src/pages/admin/championships/ChampionshipDetailPage.tsx` — admin championship page; public page is a separate route, not this one
- `frontend/src/App.tsx` — routing; add `/results/:raceId` and `/championships/:id` as public (unprotected) routes
- `frontend/src/pages/events/EventSchedulePage.tsx` — add results and standings links here
- `frontend/src/pages/racer/` — add new Results page/tab here

### Championship domain
- `app/src/main/java/dev/monkeypatch/rctiming/domain/championship/` — Championship, ChampionshipClass, ChampionshipPointsScale, ChampionshipEventLink entities
- `app/src/main/java/dev/monkeypatch/rctiming/domain/championship/ScoringSource.java` — scoring source enum (qualifying, finals, or both)

### Club config (for car tag toggle)
- `app/src/main/java/dev/monkeypatch/rctiming/domain/` — find ClubConfig entity; add `showCarTagsInResults` boolean field

### Phase 3 patterns (jOOQ query module)
- `app/src/main/java/dev/monkeypatch/rctiming/query/` — existing jOOQ query services to follow as pattern for standings implementation
- `app/src/main/resources/db/migration/` — Flyway migrations; Phase 7 will need V24+ migration for new club config column

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `PrintResultsPage.tsx` — already renders race results; needs public route + expandable lap time rows added
- `ChampionshipStandingsTable.tsx` — existing table component; evaluate for reuse on public standings page
- `PrintPracticeResultsPage.tsx` — pattern for print-optimised page (same approach for results)
- `useResultSnapshot` hook — already fetches result snapshot data; used by `PrintResultsPage`

### Established Patterns
- **jOOQ read queries** — `ChampionshipStandingsQuery.java`, `EventScheduleQuery.java`, `CarQueryService.java` show the pattern for read-side projections
- **Public routes** — `/events` and `/register` are already unprotected; follow same pattern for `/results/:raceId` and `/championships/:id`
- **Racer portal pages** — `EntriesPage.tsx`, `CarsPage.tsx` show the pattern for new racer portal tabs
- **Browser print** — Phase 6 practice print uses `window.print()` via a dedicated print page; results follow same approach

### Integration Points
- `ChampionshipStandingsQuery.computeStandings()` — implement the jOOQ join here; connects to `race_results` table (created by `ResultSnapshotService` on race FINISH)
- `ClubConfig` entity — add `showCarTagsInResults` boolean; read in `ResultSnapshotService` or result API endpoint when building print data
- `EventSchedulePage` — add results links (check race state = FINISHED) and championship standings links (check event-championship association)

</code_context>

<specifics>
## Specific Ideas

- The expandable lap times row on the public results page should work on mobile (touch tap to expand) — the public URL will be shared with spectators on phones.
- Drop scores on championship standings: visually distinguish dropped rounds (grey out or strikethrough the score cell) so it's immediately obvious which rounds were dropped without needing a legend.
- The `/championships/:id` public page should be clean and shareable — no admin controls visible, just the standings table and championship name/description.

</specifics>

<deferred>
## Deferred Ideas

- None — discussion stayed within phase scope.

</deferred>

---

*Phase: 07-results-championship*
*Context gathered: 2026-05-01 via interactive discussion*
