---
phase: 09-user-manual-documentation
plan: 03
subsystem: frontend
tags: [react, typescript, help-articles, contextual-help, useHelp]

requires:
  - phase: 09-user-manual-documentation
    plan: 02
    provides: HelpContext, useHelp hook, layout '?' buttons, print guide shell pages

provides:
  - 11 help article components in frontend/src/help/
  - useHelp() wiring with cleanup in all 12 target page components
  - SC-1 delivered in full — every key workflow page responds to '?' with contextual help

affects:
  - 09-04 (print guides are the only remaining content gap — articles already link to print pages)

tech-stack:
  added: []
  patterns:
    - "Help article: named export function, no imports, pure JSX with Tailwind classes"
    - "Page wiring: useEffect + setHelpContent(JSX) + cleanup return () => setHelpContent(null)"
    - "useHelp cleanup: mandatory on every page to prevent stale content on navigation"

key-files:
  created:
    - frontend/src/help/RaceControlHelp.tsx
    - frontend/src/help/RefereeHelp.tsx
    - frontend/src/help/PracticeHelp.tsx
    - frontend/src/help/EventManagementHelp.tsx
    - frontend/src/help/EntryManagementHelp.tsx
    - frontend/src/help/ChampionshipHelp.tsx
    - frontend/src/help/RacerProfileHelp.tsx
    - frontend/src/help/CarTransponderHelp.tsx
    - frontend/src/help/EventEntryHelp.tsx
    - frontend/src/help/ResultsHelp.tsx
    - frontend/src/help/SetupWizardHelp.tsx
  modified:
    - frontend/src/pages/race-control/CockpitPage.tsx
    - frontend/src/pages/race-control/RefereePage.tsx
    - frontend/src/pages/race-control/PracticeLandingPage.tsx
    - frontend/src/pages/admin/events/EventDetailPage.tsx
    - frontend/src/pages/admin/events/EventListPage.tsx
    - frontend/src/pages/admin/championships/ChampionshipDetailPage.tsx
    - frontend/src/pages/racer/ProfilePage.tsx
    - frontend/src/pages/racer/CarsPage.tsx
    - frontend/src/pages/racer/TranspondersPage.tsx
    - frontend/src/pages/racer/EntriesPage.tsx
    - frontend/src/pages/racer/RacerResultsPage.tsx
    - frontend/src/pages/setup/SetupLayout.tsx

key-decisions:
  - "All 12 target pages wired (including SetupLayout which uses its own Sheet for nav but is still a React component accepting useHelp)"
  - "CarTransponderHelp shared by CarsPage and TranspondersPage — single article covers both pages"
  - "Content derived from reading each page implementation: button labels, tab names, dialog names used in articles"
  - "EntriesPage and TranspondersPage are stubs but wired with useHelp — help content is accurate for the planned UI"

requirements-completed:
  - SC-1

duration: ~7min
completed: 2026-05-15
---

# Phase 09 Plan 03: Help Article Components and Page Wiring Summary

**11 help article components created and wired into all 12 target pages via useHelp() — every key workflow page now responds to the '?' button with contextual, page-specific help derived from reading each page's implementation**

## Performance

- **Duration:** ~7 min
- **Started:** 2026-05-15T18:38:00Z
- **Completed:** 2026-05-15T19:47:00Z
- **Tasks:** 2
- **Files modified:** 23

## Accomplishments

- Created `frontend/src/help/` directory with 11 help article components — no RoundGeneratorHelp.tsx as instructed
- Each article follows the brief format: 2-3 sentence description, 3-5 bulleted key actions with bold lead words, Common mistakes muted box, print guide link
- Print guide links assigned correctly: RaceControlHelp/RefereeHelp/PracticeHelp → /print/meeting-guide; RacerProfileHelp/CarTransponderHelp/EventEntryHelp/ResultsHelp → /print/racer-guide; EventManagementHelp/EntryManagementHelp/ChampionshipHelp/SetupWizardHelp → /print/admin-guide
- Content derived from reading each target page: button labels (Call Grid, Resume Race, Raise Incident, Apply Penalty), tab names (Config, Classes, Events, Points Scale, Standings, Exclusions), dialog names (PracticeCreateDialog, UnknownTransponderLinkDialog), and state machine transitions
- Wired useHelp() into all 12 target pages with mandatory `return () => setHelpContent(null)` cleanup in every useEffect
- SetupLayout wired successfully — it is a full React component and accepts useHelp() without conflict with its own Sheet (which drives the step nav drawer, not the help drawer)
- Full Vitest suite: 50 tests, 0 failures — no regressions from wiring changes
- TypeScript compiles clean throughout

## Task Commits

Each task committed atomically:

1. **Task 1: 11 help article files** - `82a4324` (feat)
2. **Task 2: useHelp() wiring in 12 pages** - `77d1ef7` (feat)

**Plan metadata:** _(docs commit follows)_

## Files Created/Modified

**Created:**
- `frontend/src/help/RaceControlHelp.tsx` — Cockpit help (Run Order, Call Grid, Start/Stop, Link transponder, Jump to race)
- `frontend/src/help/RefereeHelp.tsx` — Referee View help (Raise Incident, Apply Penalty, proximity alerts)
- `frontend/src/help/PracticeHelp.tsx` — Practice Sessions help (New Session, session cards, status icons, best laps)
- `frontend/src/help/EventManagementHelp.tsx` — Event Detail help (Publish, Open Entries, Close Entries, Start/Complete Event, tabs)
- `frontend/src/help/EntryManagementHelp.tsx` — Events List help (Create Event, sort by date, Race Control shortcut)
- `frontend/src/help/ChampionshipHelp.tsx` — Championship Detail help (6 tabs: Config, Classes, Events, Points Scale, Standings, Exclusions)
- `frontend/src/help/RacerProfileHelp.tsx` — Racer Profile help (Save changes, phonetic name, memberships, voice preview)
- `frontend/src/help/CarTransponderHelp.tsx` — Cars + Transponders help (Add car, edit car card, transponder linkage)
- `frontend/src/help/EventEntryHelp.tsx` — Event Entry help (browse events, select class/car, view submissions)
- `frontend/src/help/ResultsHelp.tsx` — Results help (collapsible event cards, position/laps, View link)
- `frontend/src/help/SetupWizardHelp.tsx` — Setup Wizard help (5 steps, sidebar navigation, Skip wizard link)

**Modified (useHelp wiring):**
- `frontend/src/pages/race-control/CockpitPage.tsx` — RaceControlHelp
- `frontend/src/pages/race-control/RefereePage.tsx` — RefereeHelp
- `frontend/src/pages/race-control/PracticeLandingPage.tsx` — PracticeHelp (added useEffect import)
- `frontend/src/pages/admin/events/EventDetailPage.tsx` — EventManagementHelp (added useEffect import)
- `frontend/src/pages/admin/events/EventListPage.tsx` — EntryManagementHelp (added useEffect import)
- `frontend/src/pages/admin/championships/ChampionshipDetailPage.tsx` — ChampionshipHelp (added useEffect import)
- `frontend/src/pages/racer/ProfilePage.tsx` — RacerProfileHelp
- `frontend/src/pages/racer/CarsPage.tsx` — CarTransponderHelp (added useEffect import)
- `frontend/src/pages/racer/TranspondersPage.tsx` — CarTransponderHelp (stub page rewritten to include wiring)
- `frontend/src/pages/racer/EntriesPage.tsx` — EventEntryHelp (stub page rewritten to include wiring)
- `frontend/src/pages/racer/RacerResultsPage.tsx` — ResultsHelp (added useEffect import)
- `frontend/src/pages/setup/SetupLayout.tsx` — SetupWizardHelp

## Decisions Made

- SetupLayout wired: it has its own `Sheet` for step navigation (side="left"), which is separate from the help Sheet (side="right" in AdminPanelLayout). No conflict — both can coexist. The admin panel layout '?' button will show the setup wizard help when the admin navigates to /setup post-bootstrap.
- CarTransponderHelp shared between CarsPage and TranspondersPage: both pages cover the same domain (cars and their transponders) and users navigate between them. A single shared article is clearer than two near-identical articles.
- EntriesPage and TranspondersPage stub pages were rewritten (from single-line stubs) to include the useHelp() imports and wiring — the stub content was preserved.

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

- `frontend/src/pages/racer/TranspondersPage.tsx` — placeholder content "Transponders — coming in Plan 06." The help article (CarTransponderHelp) accurately describes the planned UI including how to link transponders to cars.
- `frontend/src/pages/racer/EntriesPage.tsx` — placeholder content "Entries — coming in Plan 06." The help article (EventEntryHelp) describes the planned entry submission flow.

These stubs are pre-existing from Phase 06 scope deferral. The help articles describe the intended UI. The stubs do not prevent SC-1 (contextual help) from being delivered — the '?' button works on these pages and shows relevant content.

## Threat Surface Scan

No new network endpoints, auth paths, file access patterns, or schema changes. All 11 help article files are static JSX with no server calls. Threat T-09-03-02 (stale content via missing cleanup) is mitigated — every wired page has `return () => setHelpContent(null)` in its useEffect.

## Self-Check: PASSED

Files verified (spot check):
- `frontend/src/help/RaceControlHelp.tsx` — FOUND
- `frontend/src/help/SetupWizardHelp.tsx` — FOUND
- `frontend/src/pages/race-control/CockpitPage.tsx` — FOUND (contains setHelpContent)
- `frontend/src/pages/setup/SetupLayout.tsx` — FOUND (contains setHelpContent)

Commits verified:
- `82a4324` — Task 1 (11 help article files)
- `77d1ef7` — Task 2 (useHelp wiring in 12 pages)

Test count: 50 passing, 0 failing, 0 skipped
TypeScript: no errors

---
*Phase: 09-user-manual-documentation*
*Completed: 2026-05-15*
