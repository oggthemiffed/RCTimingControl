---
phase: 07-results-championship
plan: "06"
subsystem: frontend-racer-portal
tags: [wave-3, racer-results, collapsible, club-settings]
dependency_graph:
  requires:
    - 07-04 (GET /api/v1/racer/results backend endpoint)
    - 07-05 (/results/:raceId public route for race result links)
  provides:
    - RacerResultsPage at /racer/results (authenticated RACER)
    - RacerEventHistoryCard with Collapsible event accordion
    - Results nav tab (ri-trophy-line) in RacerPortalLayout
    - showCarTagsInResults Switch toggle in ClubProfilePage
  affects:
    - frontend/src/App.tsx (racer/results child route added)
    - frontend/src/lib/racerApi.ts (RaceResult, RacerResultHistoryDto types + getMyResults)
    - frontend/src/lib/adminApi.ts (showCarTagsInResults field on DTOs)
    - frontend/src/pages/racer/RacerPortalLayout.tsx (Results nav item)
    - frontend/src/pages/admin/club/ClubProfilePage.tsx (Switch toggle)
tech_stack:
  added: []
  patterns:
    - TanStack Query useQuery with ['racer', 'results'] query key namespace
    - TrophyIcon thin wrapper (const TrophyIcon = () => <i ...>) to reuse lucide Icon slot without type changes
    - shadcn Collapsible with div-based layout (safe — not inside table)
    - z.boolean() (not .default()) avoids Zod infer type mismatch with React Hook Form Resolver
    - setValue(..., { shouldDirty: true }) to mark Switch toggle changes as dirty
key_files:
  created:
    - frontend/src/pages/racer/RacerResultsPage.tsx
    - frontend/src/components/racer/RacerEventHistoryCard.tsx
    - frontend/src/hooks/racer/useRacerResults.ts
  modified:
    - frontend/src/lib/racerApi.ts
    - frontend/src/lib/adminApi.ts
    - frontend/src/pages/racer/RacerPortalLayout.tsx
    - frontend/src/pages/admin/club/ClubProfilePage.tsx
    - frontend/src/App.tsx
decisions:
  - "TrophyIcon wrapper component (const TrophyIcon = () => <i className='ri-trophy-line' />) used instead of riIcon field to avoid type changes to navItems as const array"
  - "z.boolean() used in Zod schema (not z.boolean().default(false)) — .default() makes the input type boolean|undefined which conflicts with React Hook Form Resolver generic"
  - "setValue(..., { shouldDirty: true }) added to Switch onCheckedChange so toggling marks form dirty and enables the Save button"
  - "showCarTagsInResults added to both ClubProfileDto and UpdateClubProfileRequest in adminApi.ts so the PUT payload includes the field"
metrics:
  duration: "~8 minutes"
  completed: "2026-05-02"
  tasks_completed: 2
  files_created: 3
  files_modified: 5
---

# Phase 07 Plan 06: Racer Portal Results Tab + Admin Car Tags Toggle Summary

Racer portal Results tab with expandable event history cards linking to public race result pages, plus admin club settings Switch toggle for showing car details in printed results.

## What Was Built

### Task 1: RacerResultsPage, RacerEventHistoryCard, nav tab, racerApi, App.tsx route

**racerApi.ts additions:**
- `RaceResult` interface: `raceId`, `raceLabel`, `position`, `lapsCompleted`, `bestLapMs`
- `RacerResultHistoryDto` interface: `eventId`, `eventName`, `eventDate`, `races: RaceResult[]`
- `getMyResults()` — `GET /api/v1/racer/results`, returns `RacerResultHistoryDto[]`

**useRacerResults hook** at `frontend/src/hooks/racer/useRacerResults.ts`:
- Query key: `['racer', 'results']`
- `staleTime: 30_000`

**RacerEventHistoryCard** at `frontend/src/components/racer/RacerEventHistoryCard.tsx`:
- shadcn `Collapsible` with open/close state (`useState<boolean>`)
- Header row: event name, formatted date (en-GB locale), animated chevron
- Expanded content: table of race rows (label, position P1/P2/—, laps, View link)
- Each View link: `<Link to="/results/{raceId}">` with `ri-external-link-line` icon
- Empty races fallback: "No race results for this event."

**RacerResultsPage** at `frontend/src/pages/racer/RacerResultsPage.tsx`:
- `useRacerResults()` hook; loading: 3x `animate-pulse` skeletons; error: `role="alert"` div
- Empty state: heading "No results yet" + sub-line "Results appear here after your first race finishes."
- Data state: maps `RacerEventHistoryCard` per event

**RacerPortalLayout.tsx** — Results nav item:
- `const TrophyIcon = () => <i className="ri-trophy-line h-5 w-5" aria-hidden="true" />` wrapper
- Added `{ to: '/racer/results', label: 'Results', Icon: TrophyIcon }` to `navItems`
- Renders in both desktop text nav and mobile icon+text bottom nav

**App.tsx** — `{ path: 'results', element: <RacerResultsPage /> }` added to `/racer` children block

### Task 2: Admin car tags toggle in ClubProfilePage

**adminApi.ts** — both DTOs updated:
- `ClubProfileDto`: added `showCarTagsInResults: boolean`
- `UpdateClubProfileRequest`: added `showCarTagsInResults: boolean`

**ClubProfilePage.tsx** changes:
- Zod schema: `showCarTagsInResults: z.boolean()` (not `.default()` — avoids Resolver type mismatch)
- `useForm` destructuring: added `watch`, `setValue`
- `values` initialiser: `showCarTagsInResults: data.showCarTagsInResults ?? false`
- `toRequest()`: `showCarTagsInResults: values.showCarTagsInResults` included in PUT payload
- JSX: `<Switch id="show-car-tags" checked={watch('showCarTagsInResults')} onCheckedChange={...}>` with `shouldDirty: true`
- `<Label htmlFor="show-car-tags">Show car details in printed results</Label>`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] z.boolean().default(false) causes Resolver type mismatch**
- **Found during:** Task 2 — first build attempt
- **Issue:** `z.boolean().default(false)` produces an input type of `boolean | undefined` in Zod's inferred type, which conflicts with the React Hook Form `Resolver<FormValues>` generic expecting `boolean`
- **Fix:** Changed to `z.boolean()` (no default) — the `values` initialiser in `useForm` always provides `data.showCarTagsInResults ?? false` so no runtime issue
- **Files modified:** `frontend/src/pages/admin/club/ClubProfilePage.tsx`
- **Commit:** f368aea

**2. [Rule 2 - Missing functionality] shouldDirty flag on Switch setValue**
- **Found during:** Task 2 — code review
- **Issue:** Without `{ shouldDirty: true }`, toggling the Switch does not mark the form dirty, so the Save button remains disabled despite a change being made
- **Fix:** Added `{ shouldDirty: true }` to `setValue('showCarTagsInResults', checked, { shouldDirty: true })`
- **Files modified:** `frontend/src/pages/admin/club/ClubProfilePage.tsx`
- **Commit:** f368aea

**3. [Rule 2 - Missing functionality] showCarTagsInResults missing from adminApi.ts DTOs**
- **Found during:** Task 2 — reading ClubProfileDto
- **Issue:** `ClubProfileDto` and `UpdateClubProfileRequest` in `adminApi.ts` lacked `showCarTagsInResults` field; without it the form couldn't read the current value from the backend or send it on PUT
- **Fix:** Added `showCarTagsInResults: boolean` to both interfaces
- **Files modified:** `frontend/src/lib/adminApi.ts`
- **Commit:** f368aea

## Known Stubs

None — all components render live data from the API. RacerResultsPage fetches from the real backend endpoint (added in Plan 07-04). The Switch toggle sends the boolean to the existing PUT endpoint. No hardcoded empty values or placeholder text in data paths.

## Threat Surface Scan

No new threat surface beyond what the plan's threat model covers. RacerResultsPage sends no userId in requests — backend extracts identity from JWT (Plan 07-04 enforcement). The Switch toggle is a non-security-critical admin boolean setting.

## Self-Check: PASSED

**Files verified:**
- FOUND: `frontend/src/pages/racer/RacerResultsPage.tsx`
- FOUND: `frontend/src/components/racer/RacerEventHistoryCard.tsx`
- FOUND: `frontend/src/hooks/racer/useRacerResults.ts`
- FOUND: `frontend/src/lib/racerApi.ts` (modified)
- FOUND: `frontend/src/lib/adminApi.ts` (modified)
- FOUND: `frontend/src/pages/racer/RacerPortalLayout.tsx` (modified)
- FOUND: `frontend/src/pages/admin/club/ClubProfilePage.tsx` (modified)
- FOUND: `frontend/src/App.tsx` (modified)

**Commits verified:**
- FOUND: daa3043 (Task 1 — RacerResultsPage, RacerEventHistoryCard, hook, nav tab, App.tsx route)
- FOUND: f368aea (Task 2 — showCarTagsInResults Switch toggle, adminApi.ts DTO updates)

**Build:** `npm run build` — BUILD SUCCESSFUL (both tasks)
