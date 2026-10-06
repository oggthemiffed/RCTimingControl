# Phase 9: User Manual & Documentation - Context

**Gathered:** 2026-05-14
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 9 delivers two documentation layers:

1. **In-app help system** — a slide-out help drawer accessible via a '?' icon in the page header of every key workflow page. Content is brief, contextual, and page-specific.
2. **Printable guides** — three print-ready React pages (browser print / Ctrl+P) covering each user role: Race Meeting Guide (officials), Racer Quick-Start Guide, Admin Configuration Guide.

Both layers are authored by Claude from the implemented codebase and versioned in the frontend source tree. No external documentation tools, CMS, or PDF-generation libraries.

</domain>

<decisions>
## Implementation Decisions

### In-App Help Surface
- **D-01:** Help surfaces as a **slide-out drawer** (shadcn/ui Sheet component) from the right side of the screen. The user stays in context — no navigation away from the current page.
- **D-02:** A **'?' icon button** is placed in the **top-right of each page's header bar**, consistent across all layouts (race control cockpit, admin panel, racer portal). Low visual weight, always findable in the same spot.
- **D-03:** Help content covers **key workflow pages only** (~12 high-impact pages). Simple/utility pages (login, print pages, 404) do not get help drawers. Target pages include: Cockpit (race control), Round Generator Wizard, Referee page, Event management, Entry management, Championship setup, Racer profile, Car/transponder management, Event entry submission, Results view, Practice session, Setup Wizard steps.

### Printable Guides
- **D-04:** Guides are **print CSS React pages** — dedicated routes (`/print/meeting-guide`, `/print/racer-guide`, `/print/admin-guide`) rendered as React components with `@media print` CSS. Same pattern as existing `PrintResultsPage` and `PrintPracticeResultsPage`. No new libraries. User prints via Ctrl+P / browser print dialog.
- **D-05:** Three distinct guides matching the three audiences from the success criteria:
  - `Race Meeting Guide` — for race officials; covers full race-day workflow end-to-end
  - `Racer Quick-Start Guide` — registration, cars, transponders, event entry
  - `Admin Configuration Guide` — club setup, tracks, formats, championships, user/role management
- **D-06:** Guides are accessible via **links in the help drawer** (at the bottom of relevant help articles). The Race Meeting Guide link appears in race-control-related help; the Racer Guide in racer portal help; the Admin Guide in admin panel help.

### Content Architecture
- **D-07:** Help content is **plain JSX React components** in `frontend/src/help/`. No markdown parsers, no Vite plugins, no extra dependencies. Each help article is a `.tsx` file with headings, bulleted lists, and styled prose using existing Tailwind/shadcn classes.
  ```
  frontend/src/help/
    RaceControlHelp.tsx      // Cockpit page
    RoundGeneratorHelp.tsx   // Round generator wizard
    RefereeHelp.tsx
    EventManagementHelp.tsx
    EntryManagementHelp.tsx
    ChampionshipHelp.tsx
    RacerProfileHelp.tsx
    CarTransponderHelp.tsx
    EventEntryHelp.tsx
    ResultsHelp.tsx
    PracticeHelp.tsx
    SetupWizardHelp.tsx
    // Print guides:
    MeetingGuide.tsx
    RacerGuide.tsx
    AdminGuide.tsx
  ```
- **D-08:** Pages declare their help content via a **`useHelp()` hook + `HelpProvider` context** pattern. `HelpProvider` wraps the app. Each page calls `useHelp({ content: <RaceControlHelp /> })` (or similar) and the layout's '?' button reads from context to render the drawer. Layouts (AdminPanelLayout, RaceControlLayout, RacerLayout) own the '?' button and Sheet component — pages just register their content.

### Coverage Depth
- **D-09:** In-app help articles follow a **brief format**: 2–3 sentences explaining what the page does, a bulleted list of 3–5 key actions available on the page, and a "Common mistakes" note at the bottom. Readable in under a minute. Fits in the drawer without excessive scrolling.
- **D-10:** Printable guides are **comprehensive** — more detailed than the in-app help. The Race Meeting Guide in particular covers the full race-day workflow as a step-by-step narrative (pre-meeting setup → grid call → start → marshal laps → finish → results → next race). Racer and admin guides are mid-length structured documents.
- **D-11:** **Claude writes all documentation content** for both in-app help and printable guides, based on the implemented codebase. David reviews for accuracy and tone before the phase is marked complete.

### Claude's Discretion
- Exact shadcn/ui Sheet trigger placement within each layout's header — whether it's added directly to existing header markup or via a shared `PageHeader` wrapper component.
- Visual styling of the '?' button (ghost variant, icon size, aria-label text).
- Whether `HelpProvider` passes a React node or a component reference — implementation detail.
- Print guide page layout (margins, font sizes, section breaks, club name injection from ClubProfile or a static placeholder).
- Exact list of "key workflow pages" within the ~12 target — Claude identifies the highest-value candidates from the implemented page list.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope
- `.planning/ROADMAP.md` — Phase 9 goal, success criteria SC-1 through SC-5
- `.planning/REQUIREMENTS.md` — No Phase 9 requirements formally listed; success criteria in ROADMAP.md are the authoritative spec

### Existing print page patterns (reuse these)
- `frontend/src/pages/race-control/PrintResultsPage.tsx` — browser print CSS pattern; printable guides follow the same no-nav, @media print structure
- `frontend/src/pages/race-control/PrintPracticeResultsPage.tsx` — second example of the print page pattern

### Existing layouts to extend with '?' button
- `frontend/src/pages/race-control/RaceControlLayout.tsx` — add '?' button to race control header
- `frontend/src/pages/admin/AdminPanelLayout.tsx` — add '?' button to admin panel header
- `frontend/src/App.tsx` — add HelpProvider wrapping + /print/* routes

### shadcn/ui components available
- `frontend/src/components/ui/` — Sheet component for the slide-out drawer; Button for the '?' trigger

### Phase 8 context (prior phase patterns)
- `.planning/phases/08-first-run-setup-wizard/08-CONTEXT.md` — HelpProvider/useHelp pattern is analogous to SetupGuard; reuse the provider-wrapping approach

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `PrintResultsPage.tsx` / `PrintPracticeResultsPage.tsx`: Established print-CSS React page pattern. Printable guides reuse the same approach (no navbar, clean print styles, `useEffect` for document title).
- `frontend/src/components/ui/`: shadcn/ui Sheet component for the slide-out drawer, Button for the '?' trigger icon.
- `RaceControlLayout.tsx`, `AdminPanelLayout.tsx`: Existing layout shells where the '?' header button will be added. Pages don't need to change their structure.

### Established Patterns
- **Provider + hook pattern:** Already used by `AuthProvider`/`useAuth` and `QueryProvider`. `HelpProvider`/`useHelp` follows the same pattern — new context, exported hook, provider wraps the app in `App.tsx`.
- **Route structure:** React Router routes in `App.tsx`; `/print/*` routes are unprotected (same as the existing print routes).
- **Tailwind + shadcn styling:** All existing pages use this; help articles and print guides must use the same classes.

### Integration Points
- `App.tsx`: Add `HelpProvider` wrapping + three new `/print/*` routes (unprotected).
- `RaceControlLayout.tsx`: Add '?' icon button in header that opens the Sheet from HelpProvider context.
- `AdminPanelLayout.tsx`: Same '?' button addition.
- Each key workflow page (`CockpitPage.tsx`, `RoundGeneratorWizard.tsx`, etc.): Call `useHelp({ content: <XxxHelp /> })` to register page-specific help content.

</code_context>

<specifics>
## Specific Ideas

- The help drawer should link to the relevant printable guide at the bottom (e.g., race control help links to `/print/meeting-guide`). This is the primary discoverability path for the printable guides.
- The Race Meeting Guide is the most important deliverable — officials running a race day need a comprehensive, printed reference. It should cover the full workflow from pre-meeting setup through publishing results.
- Content is written by Claude from the implemented codebase; David reviews. This means the executor agent needs to read the actual page implementations to write accurate help content.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 9-user-manual-documentation*
*Context gathered: 2026-05-14*
