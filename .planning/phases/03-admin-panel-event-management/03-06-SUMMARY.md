---
phase: 03-admin-panel-event-management
plan: "06"
status: checkpoint
completed: 2026-04-22
tasks_total: 4
tasks_completed: 3
one-liner: "Complete Phase 3 admin UI — championships (6 tabs), club profile with logo, tracks CRUD, formats CRUD with animated type-switcher, car-tag-categories with archive/unarchive"
subsystem: frontend
tags: [react, admin-ui, championships, club-profile, tracks, formats, car-tag-categories]
dependency_graph:
  requires:
    - "03-03 (ChampionshipController, ClubProfileController, TrackController, RaceFormatController endpoints)"
    - "03-04 (MinIO logo upload endpoint)"
    - "03-05 (AdminPanelLayout, adminQueryKeys, useAdminEvents hook pattern)"
  provides:
    - "adminApi.ts: championships (13 fns), club (3), tracks (5), formats (5), carTagCategories (5)"
    - "5 new hook files with useQuery/useMutation + cache invalidation"
    - "ChampionshipListPage + ChampionshipDetailPage (6 tabs)"
    - "ChampionshipConfigForm, PointsScaleEditor (ROAR/BRCA presets), ChampionshipStandingsTable"
    - "ClubProfilePage (logo upload + profile form)"
    - "TracksPage (CRUD)"
    - "FormatsPage + FormatConfigFields (animated type-switcher)"
    - "CarTagCategoriesPage (archive/unarchive + show-archived toggle)"
    - "App.tsx: 6 routes under /admin replacing ComingSoon placeholders"
tech_stack:
  patterns:
    - "Discriminated union RaceFormatConfig (TIMED | BUMP_UP | POINTS_FINALS) — replaced generic {type: string} in adminApi.ts"
    - "FormatConfigFields: patch(key: string, val: unknown) spread pattern to avoid TypeScript union key narrowing issue"
    - "ChampionshipDetailPage: exclusions loaded via separate useChampionshipExclusions hook (not embedded in detail)"
key_files:
  created:
    - frontend/src/hooks/admin/useAdminChampionships.ts
    - frontend/src/hooks/admin/useAdminClub.ts
    - frontend/src/hooks/admin/useAdminTracks.ts
    - frontend/src/hooks/admin/useAdminFormats.ts
    - frontend/src/hooks/admin/useAdminCarTagCategories.ts
    - frontend/src/pages/admin/championships/ChampionshipListPage.tsx
    - frontend/src/pages/admin/championships/ChampionshipDetailPage.tsx
    - frontend/src/pages/admin/championships/ChampionshipConfigForm.tsx
    - frontend/src/pages/admin/championships/PointsScaleEditor.tsx
    - frontend/src/pages/admin/championships/ChampionshipStandingsTable.tsx
    - frontend/src/pages/admin/club/ClubProfilePage.tsx
    - frontend/src/pages/admin/tracks/TracksPage.tsx
    - frontend/src/pages/admin/formats/FormatsPage.tsx
    - frontend/src/pages/admin/formats/FormatConfigFields.tsx
    - frontend/src/pages/admin/categories/CarTagCategoriesPage.tsx
  modified:
    - frontend/src/lib/adminApi.ts
    - frontend/src/pages/admin/events/EventClassSection.tsx
    - frontend/src/App.tsx
decisions:
  - "Route path 'categories' (not 'car-tag-categories') — matched existing AdminPanelLayout nav links and App.tsx placeholder"
  - "RaceFormatConfig upgraded from generic {type: string; [key]: unknown} to proper discriminated union — required fixing EventClassSection.tsx ConfigSummary to use type narrowing"
  - "patch() in FormatConfigFields uses (key: string, val: unknown) — TypeScript's keyof on a discriminated union resolves to intersection of member keys only; untyped spread avoids the false narrowing"
  - "ClubProfile API uses /api/v1/admin/club/profile (not /api/v1/admin/club) — matched actual ClubProfileController route"
metrics:
  duration: "~90 minutes"
  completed: "2026-04-22"
  tasks: 3
  files_created: 15
  files_modified: 3
---

# Phase 03 Plan 06: Complete Admin UI Summary

**One-liner:** All remaining Phase 3 admin pages shipped — championships (6-tab detail), club profile with logo upload, tracks CRUD, race format templates with animated type-switcher, and car-tag-categories with archive/unarchive.

## Tasks Completed

### Task 1: adminApi.ts extensions + 5 hook files

**adminApi.ts** extended with:
- `championships` sub-object: 13 functions (list, get, create, update, addClass, removeClass, linkEvent, unlinkEvent, replacePointsScale, listExclusions, createExclusion, deleteExclusion, getStandings)
- `club` sub-object: 3 functions (getProfile → `/api/v1/admin/club/profile`, updateProfile, uploadLogo)
- `tracks` sub-object: 5 functions (list, get, create, update, delete) — returns full `TrackDto` with venueNotes/trackLength
- `formats` sub-object: 5 functions (list, get, create, update, delete)
- `carTagCategories` sub-object: 5 functions (list with `includeArchived`, create, update, archive, unarchive)

`RaceFormatConfig` upgraded from `{ type: string; [key]: unknown }` to a proper discriminated union (`TimedRaceConfig | BumpUpConfig | PointsFinalsConfig`) with `StartType` and `QualifyingType` enums matching the backend.

5 hook files created, each following the `useAdminEvents.ts` pattern (useQuery + useMutation with `invalidateQueries` on success).

### Task 2: Championship pages (5 files)

- **ChampionshipListPage** — table with Create dialog using ChampionshipConfigForm
- **ChampionshipConfigForm** — reusable RHF/Zod form: name, scoringSource RadioGroup (QUALIFYING/FINALS/BOTH), bestX/Y number pair, tqBonusPoints, afinalWinnerBonusPoints
- **PointsScaleEditor** — ROAR preset (20,17,15,13,12,11,10,9,8,7) and BRCA preset (100,95,91,88,85,83,81,79,77,75); editable table with Add/Remove row; Save/Revert buttons; one PUT per save
- **ChampionshipStandingsTable** — reads Phase 3 scaffold (empty list); columns: position, driver, total, one column per round; dropped rounds greyed/line-through; EXC badge; DNS for position=0; gracefully renders both empty and populated
- **ChampionshipDetailPage** — 6 tabs: Config / Classes / Events / Points Scale / Standings / Exclusions; 409 toast on duplicate round link; audit trail in Exclusions tab (createdBy + createdAt)

### Task 3: Club, Tracks, Formats, Categories + App.tsx

- **ClubProfilePage** — logo card (preview or placeholder), file input restricted to `image/png,image/jpeg,image/webp,image/svg+xml`, auto-upload on select, error toast on 400; profile form with name/email/phone/websiteUrl/lat/lng/timezone
- **TracksPage** — CRUD table with edit dialog; name/venueNotes/trackLength fields
- **FormatConfigFields** — Select for type (TIMED/BUMP_UP/POINTS_FINALS); type change resets to defaults; variant-specific fields in `animate-in fade-in` div; `patch(key, val)` spread pattern
- **FormatsPage** — CRUD using FormatConfigFields; scrollable dialog for create/edit
- **CarTagCategoriesPage** — Switch toggle for show-archived (default off); Archive button on active rows; ArchiveRestore button on archived rows; colour swatch preview; Create/Edit dialogs
- **App.tsx** — 6 placeholder `AdminComingSoonPage` routes replaced with real page imports

## Deviations from Plan

**1. Route path `categories` not `car-tag-categories`**
- AdminPanelLayout nav links point to `/admin/categories` and App.tsx placeholder already used `path: 'categories'`. Used `categories` to match, not `car-tag-categories` as written in the plan spec.

**2. RaceFormatConfig union upgrade broke EventClassSection.tsx**
- Narrowing `RaceFormatConfig` from a generic loose type to a proper discriminated union caused TypeScript errors in the pre-existing `ConfigSummary` component which accessed `config.durationMinutes` etc. without narrowing.
- Fixed: replaced the loose property checks with proper `if (config.type === 'TIMED')` narrowing.

**3. `patch()` generic type issue in FormatConfigFields**
- `keyof Omit<RaceFormatConfig, 'type'>` on a discriminated union resolves to only the common keys (racePaddingMinutes, staggerIntervalSeconds, qualifyingType). Variant-specific keys like `durationMinutes` were rejected.
- Fixed: `patch(key: string, val: unknown)` with `as RaceFormatConfig` cast — correct at runtime, avoids false TypeScript narrowing.

## Known Stubs

- `ChampionshipStandingsTable` renders against Phase 3 backend scaffold (empty list) — Phase 7 implements race_results join; zero frontend changes required when it does.
- Exclusions tab shows "Driver {id}" and "admin {id}" for driverId/createdBy — Phase 7 can resolve these to names once the user profile query is available.

## Phase 7 TODO

`ChampionshipStandingsTable` already handles both empty-list (Phase 3) and populated (Phase 7) responses. When Phase 7 implements the race_results aggregation, zero frontend changes are required.

## Self-Check

- [x] `npm run build` exits 0
- [x] `npm run lint` exits 0 (3 pre-existing warnings, 0 errors)
- [x] All 5 hook files created
- [x] All 10 page/component files created
- [x] adminApi.ts has championships/club/tracks/formats/carTagCategories sub-objects
- [x] PointsScaleEditor has ROAR_PRESET and BRCA_PRESET constants
- [x] FormatConfigFields has animate-in on variant sections
- [x] CarTagCategoriesPage has includeArchived toggle + archive/unarchive actions
- [x] App.tsx has 6 new routes replacing AdminComingSoonPage placeholders
- [ ] Task 4: Human verification checkpoint — pending

## Self-Check: PENDING (human verification required)
