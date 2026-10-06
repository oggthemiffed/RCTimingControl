---
phase: 09-user-manual-documentation
plan: 02
subsystem: frontend
tags: [react, typescript, context, help-system, print-pages, layouts]

requires:
  - phase: 09-user-manual-documentation
    plan: 01
    provides: Wave 0 test stubs for HelpContext and print pages

provides:
  - HelpProvider React context with useHelp hook wired into app root
  - '?' help button in RaceControlLayout, AdminPanelLayout (mobile), RacerPortalLayout (desktop)
  - Help Sheet (side="right") accessible from all three layout headers
  - MeetingGuidePage, RacerGuidePage, AdminGuidePage shell pages at /print/* routes
  - /print/meeting-guide, /print/racer-guide, /print/admin-guide unprotected flat routes

affects:
  - 09-03 (useHelp() is now available for article wiring)
  - 09-04 (print page shells ready for content)

tech-stack:
  added: []
  patterns:
    - "HelpContext: collocation pattern — context, provider, and hook in a single file"
    - "Help Sheet: conditionally rendered on helpContent presence (null check)"
    - "Print page: useEffect document.title, print:hidden button, max-w-3xl container"

key-files:
  created:
    - frontend/src/context/HelpContext.tsx
    - frontend/src/pages/print/MeetingGuidePage.tsx
    - frontend/src/pages/print/RacerGuidePage.tsx
    - frontend/src/pages/print/AdminGuidePage.tsx
  modified:
    - frontend/src/App.tsx
    - frontend/src/context/HelpContext.test.tsx
    - frontend/src/pages/race-control/RaceControlLayout.tsx
    - frontend/src/pages/admin/AdminPanelLayout.tsx
    - frontend/src/pages/racer/RacerPortalLayout.tsx
    - frontend/src/pages/print/MeetingGuidePage.test.tsx
    - frontend/src/pages/print/RacerGuidePage.test.tsx
    - frontend/src/pages/print/AdminGuidePage.test.tsx
    - frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx

key-decisions:
  - "HelpProvider nested inside AuthProvider but outside SetupGuard — allows useHelp() from any routed page"
  - "Toaster stays outside HelpProvider — no dependency on help state"
  - "AdminPanelLayout uses separate isOpen/setIsOpen from useHelp() — sheetOpen/setSheetOpen reserved for nav drawer only"
  - "Print routes are flat unprotected siblings to /events and /championships/:id — no auth wall"

requirements-completed:
  - SC-1
  - SC-2
  - SC-3
  - SC-4

duration: ~5min
completed: 2026-05-15
---

# Phase 09 Plan 02: HelpContext Infrastructure and Print Guide Shells Summary

**HelpProvider context wired into app root, '?' button added to all three layout headers, and three print guide shell pages created at /print/* routes with 10 new passing tests**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-05-15T18:32:03Z
- **Completed:** 2026-05-15T18:37:00Z
- **Tasks:** 2
- **Files modified:** 13

## Accomplishments

- Created `HelpContext.tsx` with `HelpProvider`, `useHelp()` hook, and `HelpContextValue` interface (React.ReactNode + boolean sheet state)
- Wired `HelpProvider` into `App.tsx` `RootLayout` — inside `AuthProvider`, wrapping `SetupGuard`
- Registered `/print/meeting-guide`, `/print/racer-guide`, `/print/admin-guide` as flat unprotected routes
- Added conditional `HelpCircle` '?' button and `Sheet` (side="right", w-96) to `RaceControlLayout` header
- Added conditional `HelpCircle` '?' button (ml-auto) to `AdminPanelLayout` mobile header only; help Sheet separate from nav drawer Sheet
- Added conditional `HelpCircle` '?' button and `Sheet` to `RacerPortalLayout` desktop nav
- Created three print guide shell pages with `document.title`, subtitle, placeholder content note, and Print/Save as PDF button
- Enabled all 4 HelpContext Wave 0 tests (now real tests, no describe.skip)
- Enabled all 6 print page Wave 0 tests (2 per page)
- Full test suite: 13 files, 50 tests, 0 failures

## Task Commits

Each task committed atomically:

1. **Task 1: HelpContext + App.tsx** - `1059b96` (feat)
2. **Task 2: Layouts + print pages** - `0a253a7` (feat)

**Plan metadata:** _(docs commit follows)_

## Files Created/Modified

- `frontend/src/context/HelpContext.tsx` — HelpProvider, useHelp hook, HelpContextValue interface
- `frontend/src/App.tsx` — HelpProvider in RootLayout, three /print/* routes
- `frontend/src/context/HelpContext.test.tsx` — 4 enabled tests (Wave 0 stubs replaced)
- `frontend/src/pages/race-control/RaceControlLayout.tsx` — HelpCircle button + help Sheet in header
- `frontend/src/pages/admin/AdminPanelLayout.tsx` — HelpCircle button in mobile header + help Sheet
- `frontend/src/pages/racer/RacerPortalLayout.tsx` — HelpCircle button + help Sheet in desktop nav
- `frontend/src/pages/print/MeetingGuidePage.tsx` — Shell: "Race Meeting Guide"
- `frontend/src/pages/print/RacerGuidePage.tsx` — Shell: "Racer Quick-Start Guide"
- `frontend/src/pages/print/AdminGuidePage.tsx` — Shell: "Admin Configuration Guide"
- `frontend/src/pages/print/MeetingGuidePage.test.tsx` — 2 enabled tests
- `frontend/src/pages/print/RacerGuidePage.test.tsx` — 2 enabled tests
- `frontend/src/pages/print/AdminGuidePage.test.tsx` — 2 enabled tests
- `frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx` — Added HelpProvider wrapper (Rule 1 fix)

## Decisions Made

- Collocation pattern: HelpContext.tsx holds context object, provider, and hook in a single file (unlike AuthProvider/useAuth which are separate files)
- `Toaster` stays outside `HelpProvider` in `RootLayout` — it has no dependency on help state
- Admin help Sheet uses `isOpen`/`setIsOpen` from `useHelp()`, never `sheetOpen`/`setSheetOpen` (which drives the nav drawer)
- Print routes are siblings to `/events` and `/championships/:id` — same unprotected flat pattern

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed AdminPanelLayout test missing HelpProvider wrapper**
- **Found during:** Task 2 verification (npm test -- --run)
- **Issue:** Existing `AdminPanelLayout.test.tsx` rendered the layout without `HelpProvider`. After adding `useHelp()` to the layout, the test threw "useHelp must be used within HelpProvider"
- **Fix:** Added `HelpProvider` import and wrapped `<AdminPanelLayout />` with `<HelpProvider>` in the test render
- **Files modified:** `frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx`
- **Commit:** `0a253a7` (included in Task 2 commit)

## Threat Surface Scan

| Flag | File | Description |
|------|------|-------------|
| threat_flag: unprotected-route | frontend/src/App.tsx | /print/meeting-guide, /print/racer-guide, /print/admin-guide registered without ProtectedRoute — intentional per plan (T-09-02-01 accepted) |

Per the plan's threat model: these routes contain only publicly visible documentation about system features. No personal data, credentials, or sensitive configuration. Consistent with /events and /championships/:id.

## Known Stubs

Three print guide pages contain intentional placeholder content:
- `frontend/src/pages/print/MeetingGuidePage.tsx` — "Content will be completed in Plan 09-04."
- `frontend/src/pages/print/RacerGuidePage.tsx` — "Content will be completed in Plan 09-04."
- `frontend/src/pages/print/AdminGuidePage.tsx` — "Content will be completed in Plan 09-04."

These stubs are intentional per plan design. Plan 09-04 will complete all three guides with real content sections.

## Next Phase Readiness

- Plan 09-03 can now wire help articles by calling `setHelpContent(<ArticleContent />)` from any page using `useHelp()`
- Plan 09-04 can now fill the three print page shells with real content sections
- The help Sheet infrastructure is complete and tested in all three layouts

## Self-Check: PASSED

Files verified:
- `frontend/src/context/HelpContext.tsx` — FOUND
- `frontend/src/pages/print/MeetingGuidePage.tsx` — FOUND
- `frontend/src/pages/print/RacerGuidePage.tsx` — FOUND
- `frontend/src/pages/print/AdminGuidePage.tsx` — FOUND

Commits verified:
- `1059b96` — Task 1 (HelpContext + App.tsx)
- `0a253a7` — Task 2 (layouts + print pages)

Test count: 50 passing, 0 failing, 0 skipped

---
*Phase: 09-user-manual-documentation*
*Completed: 2026-05-15*
