---
phase: 07-results-championship
plan: "05"
subsystem: frontend-public-pages
tags: [wave-3, public-results, championship-standings, event-schedule, expandable-rows]
dependency_graph:
  requires:
    - 07-04 (PublicResultsController, PublicChampionshipController, EventScheduleDto enrichment)
    - 07-03 (ChampionshipStandingsTable, useChampionshipStandings hook)
  provides:
    - PublicResultsPage at /results/:raceId (public, no auth)
    - PublicChampionshipPage at /championships/:id (public, no auth)
    - EventSchedulePage fully implemented with View Results and View Standings links
    - usePublicResultSnapshot hook (60s staleTime)
    - CarTagDto and EventScheduleDto types in raceControlApi.ts
    - getPublicResultSnapshot() and getEventSchedule() API functions
  affects:
    - frontend/src/App.tsx (two new public routes)
    - frontend/src/lib/raceControlApi.ts (types + public API functions)
    - frontend/src/pages/admin/championships/ChampionshipStandingsTable.tsx (token fix)
tech_stack:
  added: []
  patterns:
    - useState + conditional <tr> for expandable table rows (avoids broken Collapsible-in-table structure)
    - React.Fragment key pattern wrapping row pairs in tbody
    - Button asChild + Link for internal navigation in shadcn/ui
    - TanStack Query useQuery with public query key namespace ['public', ...]
key_files:
  created:
    - frontend/src/hooks/race-control/usePublicResultSnapshot.ts
    - frontend/src/pages/results/PublicResultsPage.tsx
    - frontend/src/pages/championships/PublicChampionshipPage.tsx
  modified:
    - frontend/src/lib/raceControlApi.ts
    - frontend/src/App.tsx
    - frontend/src/pages/events/EventSchedulePage.tsx
    - frontend/src/pages/admin/championships/ChampionshipStandingsTable.tsx
decisions:
  - "Expandable lap rows use useState + React.Fragment wrapping row pairs — Collapsible wrapping tr breaks HTML table structure"
  - "LapTimesPanel shows position-at-each-lap from lapHistory (P1/P2 per lap) since lapHistory PositionAtLap[] has no raw lap times; per-lap time display is a future DTO enhancement"
  - "getPublicResultSnapshot() reuses the existing authenticated api client — it adds JWT only if a token is present; backend permitAll block accepts requests with or without token"
  - "Drop score token changed from text-slate-300 to text-muted-foreground — design-system semantic token, correct for both light and dark themes"
  - "Button asChild + Link pattern used for View Results/Standings — avoids nested anchor elements and keeps React Router internal navigation"
metrics:
  duration: "~18 minutes"
  completed: "2026-05-02"
  tasks_completed: 2
  files_created: 3
  files_modified: 4
---

# Phase 07 Plan 05: Frontend Public Pages Summary

Public results page with expandable lap time rows, public championship standings page, and fully implemented event schedule with results and standings navigation links.

## What Was Built

### Task 1: usePublicResultSnapshot hook, PublicResultsPage, App.tsx routing

**raceControlApi.ts additions:**
- `CarTagDto` type added to `ResultRow` as `carTags: CarTagDto[] | null`
- `EventScheduleDto` type with `finishedRaceIds: number[]` and `championshipId: number | null`
- `getPublicResultSnapshot(raceId)` — hits `GET /api/v1/results/{raceId}` (no auth required)
- `getEventSchedule()` — hits `GET /api/v1/events` (no auth required)

**usePublicResultSnapshot hook** created at `frontend/src/hooks/race-control/usePublicResultSnapshot.ts`:
- Query key: `['public', 'results', raceId]` (separate namespace from authenticated hooks)
- `staleTime: 60_000` (60s vs 30s for authenticated hook — public results are stable)

**PublicResultsPage** at `frontend/src/pages/results/PublicResultsPage.tsx`:
- `useParams<{ raceId: string }>()` to extract raceId
- Loading, error, and data states with appropriate UI copy
- Header block: club name, race label, finished timestamp, optional logo
- Expandable rows using `useState<number | null>` + `React.Fragment` wrapper per row pair
- Click handler toggles `expandedEntryId` — accordion-style (one row expanded at a time)
- `LapTimesPanel` inline component renders position-at-each-lap from `lapHistory`
- Car tags rendered as `key: value` sub-line below driver name when present
- Print button (hidden in print media via `print:hidden`)
- No `Collapsible` component used — correct approach for HTML table structure

**App.tsx** — two new public routes added after `/events`:
```
{ path: '/results/:raceId', element: <PublicResultsPage /> },
{ path: '/championships/:id', element: <PublicChampionshipPage /> },
```
Both are top-level routes outside all `ProtectedRoute` wrappers.

### Task 2: PublicChampionshipPage, EventSchedulePage, ChampionshipStandingsTable token fix

**ChampionshipStandingsTable.tsx** — one-line fix:
- `text-slate-300` replaced with `text-muted-foreground` for dropped round cells
- Added `title="This round was dropped from the total"` tooltip to dropped cells

**PublicChampionshipPage** at `frontend/src/pages/championships/PublicChampionshipPage.tsx`:
- `useParams<{ id: string }>()` extracts championshipId
- Wraps `ChampionshipStandingsTable` which handles its own loading/error/empty states
- No admin controls — pure read-only view
- Container: `p-8 max-w-4xl mx-auto`

**EventSchedulePage** fully implemented replacing the stub:
- `useQuery` with `getEventSchedule()` and `['public', 'events']` query key
- Loading: 3x `animate-pulse bg-muted rounded h-32 mb-3` skeletons
- Error: `role="alert"` div with `text-destructive` copy
- Empty: centered `text-muted-foreground` message
- Event cards: `border rounded-lg p-4 mb-3` with name, date, entry availability badge
- Entry availability: `ENTRY_OPEN` → default badge "Open for Entry"; `ENTRY_CLOSED` → secondary badge "Entries Closed"; `ENTRY_NOT_YET_OPEN` → outline badge "Coming Soon"
- `finishedRaceIds.map(raceId => <Button variant="outline" size="sm" asChild><Link to="/results/{raceId}">View Results</Link></Button>)`
- `championshipId !== null` → `<Button variant="outline" size="sm" asChild><Link to="/championships/{championshipId}">View Standings</Link></Button>`

## Deviations from Plan

None — plan executed exactly as written. The `LapTimesPanel` component per the plan's own note correctly shows position-at-each-lap (not raw lap times) since `lapHistory: PositionAtLap[]` contains `lapNumber + entryId + position` but not raw timing values — this is documented in the plan itself as acceptable.

## Known Stubs

None — all components render live data from the API. The lap time expansion panel shows the data that the API provides (position progression per lap). Raw per-lap times would require a DTO change (future enhancement, as noted in plan).

## Threat Surface Scan

No new threat surface beyond what the plan's threat model already covers. Both new public routes render read-only data from the existing public API endpoints (already `permitAll` in SecurityConfig from Plan 07-04). No new network endpoints, auth paths, or schema changes introduced on the frontend.

## Self-Check: PASSED

**Files verified:**
- FOUND: `frontend/src/hooks/race-control/usePublicResultSnapshot.ts`
- FOUND: `frontend/src/pages/results/PublicResultsPage.tsx`
- FOUND: `frontend/src/pages/championships/PublicChampionshipPage.tsx`
- FOUND: `frontend/src/pages/events/EventSchedulePage.tsx` (modified)
- FOUND: `frontend/src/pages/admin/championships/ChampionshipStandingsTable.tsx` (modified)
- FOUND: `frontend/src/App.tsx` (modified)
- FOUND: `frontend/src/lib/raceControlApi.ts` (modified)

**Commits verified:**
- FOUND: 1a00e22 (Task 1 — hook + PublicResultsPage + PublicChampionshipPage + App.tsx routing)
- FOUND: 01335bc (Task 2 — token fix + EventSchedulePage)

**Build:** TypeScript compile via `npm run build` — BUILD SUCCESSFUL (both tasks)
