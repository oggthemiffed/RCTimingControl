# Phase 9: User Manual & Documentation - Research

**Researched:** 2026-05-15
**Domain:** React frontend — context/provider pattern, shadcn/ui Sheet, print CSS pages
**Confidence:** HIGH — all findings verified directly from the live codebase

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Help surfaces as a slide-out drawer (shadcn/ui Sheet component) from the right side of the screen.
- **D-02:** A '?' icon button is placed in the top-right of each page's header bar, consistent across all layouts.
- **D-03:** Help content covers key workflow pages only (~12 high-impact pages). Simple/utility pages (login, print pages, 404) do not get help drawers.
- **D-04:** Guides are print CSS React pages — dedicated routes (`/print/meeting-guide`, `/print/racer-guide`, `/print/admin-guide`). Same pattern as existing `PrintResultsPage` and `PrintPracticeResultsPage`. No new libraries.
- **D-05:** Three distinct guides: Race Meeting Guide (officials), Racer Quick-Start Guide (racers), Admin Configuration Guide (admins).
- **D-06:** Guides are accessible via links in the help drawer at the bottom of relevant help articles.
- **D-07:** Help content is plain JSX React components in `frontend/src/help/`. No markdown parsers, no Vite plugins, no extra dependencies.
- **D-08:** Pages declare their help content via a `useHelp()` hook + `HelpProvider` context pattern.
- **D-09:** In-app help articles: 2-3 sentences + bulleted list of 3-5 key actions + "Common mistakes" note.
- **D-10:** Printable guides are comprehensive. Race Meeting Guide covers full race-day workflow.
- **D-11:** Claude writes all documentation content; David reviews.

### Claude's Discretion
- Exact shadcn/ui Sheet trigger placement within each layout's header.
- Visual styling of the '?' button (ghost variant, icon size, aria-label text).
- Whether `HelpProvider` passes a React node or a component reference.
- Print guide page layout (margins, font sizes, section breaks, club name injection).
- Exact list of "key workflow pages" within the ~12 target.

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope.
</user_constraints>

---

## Summary

Phase 9 is a pure-frontend phase with zero backend changes and zero new npm dependencies. All infrastructure required already exists in the codebase: shadcn/ui Sheet (installed), Button (installed), Separator (installed), Badge (installed), `lucide-react` icons, React context/provider pattern (used by `AuthProvider`), and the print page pattern (two examples: `PrintResultsPage.tsx` and `PrintPracticeResultsPage.tsx`).

The work is structurally similar to Phase 8's `SetupGuard`/`SetupLayout` — a new context (`HelpContext`) wraps the app, layouts read from it, and individual pages push content into it via a hook. The implementation is straightforward because the codebase already demonstrates every pattern needed. The primary effort is **content authorship**: the executor must read each of the ~12 target pages and write accurate help text from what the pages actually do.

The one finding requiring attention is that the **Tooltip component is not installed as a shadcn/ui file** in `frontend/src/components/ui/` — there is no `tooltip.tsx`. However, Tooltip IS available via `radix-ui` (the unified package already installed: `"radix-ui": "^1.4.3"`). The planner must decide: either install `tooltip.tsx` via shadcn CLI, or fall back to a native `title` attribute on the `<Button>` as permitted by the UI-SPEC. The `icon-sm` button size is 32px (`size-8`), which is below the WCAG 2.5.5 44px minimum for touch targets; the '?' button should be wrapped in a `p-2` class or use `size="icon"` (36px) instead.

**Primary recommendation:** Follow the `AuthProvider`/`useAuth` pattern exactly. Wire `HelpProvider` inside `AuthProvider` and `QueryProvider` in `App.tsx`. Layouts own the Sheet; pages call `useHelp({ content: <XxxHelp /> })` in a `useEffect`. The print guides follow `PrintResultsPage.tsx` verbatim in structure.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| HelpProvider / HelpContext | Browser / Client | — | Pure React context, no server involvement |
| '?' button in layout headers | Browser / Client | — | UI mutation to existing layout components |
| Help drawer (Sheet) | Browser / Client | — | shadcn Sheet is a client-side overlay |
| Help article JSX components | Browser / Client | — | Static JSX, no API calls |
| Print guide pages | Browser / Client | — | Client-rendered pages with @media print CSS |
| Club name in print guides | Browser / Client | API / Backend | `useClubProfile()` hook fetches from existing admin API |
| Route registration for /print/* | Browser / Client | — | React Router in App.tsx, unprotected |

---

## Standard Stack

### Core (all already installed)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19.2.4 | UI framework | Project standard |
| `radix-ui` | 1.4.3 | Sheet, Tooltip primitives | Used by all shadcn components in this codebase |
| `lucide-react` | 1.8.0 | `HelpCircle` icon for '?' button | Confirmed codebase standard (despite components.json declaring remixicon, all existing pages use lucide-react) |
| `react-router-dom` | 7.14.1 | Route registration for `/print/*` | Project standard |
| Tailwind CSS | 4.2.2 | All styling | Project standard |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `@hookform/resolvers` / `zod` | existing | Not needed for this phase | — |
| `@tanstack/react-query` | 5.99.0 | `useClubProfile()` in print guides | Only if club name injection is implemented in print guides |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Plain JSX help articles | Markdown + remark/rehype | Markdown adds a dependency and a build-time plugin. Locked as out of scope by D-07. |
| Native `title` attribute for '?' tooltip | shadcn Tooltip component | Tooltip not installed as .tsx file. Native `title` is the D-07 permitted fallback. |

**Installation:** No new packages. Zero npm installs required.

**Version verification:** All versions confirmed from `frontend/package.json`. [VERIFIED: package.json]

---

## Architecture Patterns

### System Architecture Diagram

```
App.tsx
  └── QueryProvider
        └── RouterProvider
              └── RootLayout
                    ├── AuthProvider          (existing — manages JWT)
                    │     └── HelpProvider    (NEW — wraps children, owns Sheet state)
                    │           └── SetupGuard
                    │                 └── <Outlet /> → layout routes
                    │
                    ├── RaceControlLayout     (modified — reads helpContent from context, renders Sheet + '?' button)
                    │     └── CockpitPage     (modified — calls useHelp({ content: <RaceControlHelp /> }))
                    │     └── RefereePage     (modified — calls useHelp({ content: <RefereeHelp /> }))
                    │     └── PracticeLandingPage (modified)
                    │
                    ├── AdminPanelLayout      (modified — mobile header gets '?' button)
                    │     └── EventDetailPage (modified — calls useHelp)
                    │     └── EventListPage   (modified)
                    │     └── ChampionshipDetailPage (modified)
                    │     └── ... other admin pages
                    │
                    ├── RacerPortalLayout     (modified — desktop nav gets '?' button)
                    │     └── ProfilePage     (modified — calls useHelp)
                    │     └── CarsPage        (modified)
                    │     └── TranspondersPage (modified)
                    │     └── EntriesPage     (modified)
                    │
                    └── Unprotected routes
                          ├── /print/meeting-guide → MeetingGuidePage  (NEW)
                          ├── /print/racer-guide   → RacerGuidePage    (NEW)
                          └── /print/admin-guide   → AdminGuidePage    (NEW)
```

### Recommended Project Structure
```
frontend/src/
├── context/
│   └── HelpContext.tsx          # HelpProvider + useHelp hook (NEW)
├── help/
│   ├── RaceControlHelp.tsx      # Cockpit help article (NEW)
│   ├── RoundGeneratorHelp.tsx   # Round generator help (NEW)
│   ├── RefereeHelp.tsx          # Referee help (NEW)
│   ├── EventManagementHelp.tsx  # Event management help (NEW)
│   ├── EntryManagementHelp.tsx  # Entry management help (NEW)
│   ├── ChampionshipHelp.tsx     # Championship help (NEW)
│   ├── RacerProfileHelp.tsx     # Racer profile help (NEW)
│   ├── CarTransponderHelp.tsx   # Cars & transponders help (NEW)
│   ├── EventEntryHelp.tsx       # Event entry submission help (NEW)
│   ├── ResultsHelp.tsx          # Results view help (NEW)
│   ├── PracticeHelp.tsx         # Practice session help (NEW)
│   └── SetupWizardHelp.tsx      # Setup wizard help (NEW)
└── pages/
    └── print/
        ├── MeetingGuidePage.tsx  # Race Meeting Guide (NEW)
        ├── RacerGuidePage.tsx    # Racer Quick-Start Guide (NEW)
        └── AdminGuidePage.tsx    # Admin Configuration Guide (NEW)
```

### Pattern 1: HelpProvider / useHelp Context

Model: mirrors `AuthProvider`/`useAuth` exactly. [VERIFIED: frontend/src/providers/AuthProvider.tsx]

```typescript
// frontend/src/context/HelpContext.tsx
import React, { createContext, useContext, useState, useEffect } from 'react';

interface HelpContextValue {
  helpContent: React.ReactNode | null;
  setHelpContent: (content: React.ReactNode | null) => void;
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
}

const HelpContext = createContext<HelpContextValue | null>(null);

export function HelpProvider({ children }: { children: React.ReactNode }) {
  const [helpContent, setHelpContent] = useState<React.ReactNode | null>(null);
  const [isOpen, setIsOpen] = useState(false);

  return (
    <HelpContext.Provider value={{ helpContent, setHelpContent, isOpen, setIsOpen }}>
      {children}
    </HelpContext.Provider>
  );
}

export function useHelp() {
  const ctx = useContext(HelpContext);
  if (!ctx) throw new Error('useHelp must be used within HelpProvider');
  return ctx;
}
```

Pages call it like this (within a `useEffect` to mirror how `document.title` is set in print pages):

```typescript
// Inside CockpitPage.tsx (or any target page)
import { useHelp } from '@/context/HelpContext';
import { RaceControlHelp } from '@/help/RaceControlHelp';

export default function CockpitPage() {
  const { setHelpContent } = useHelp();

  useEffect(() => {
    setHelpContent(<RaceControlHelp />);
    return () => setHelpContent(null);   // cleanup on unmount
  }, [setHelpContent]);

  // ... rest of page
}
```

The cleanup on unmount (`return () => setHelpContent(null)`) is critical — without it, navigating from a page with help to one without leaves stale content in the drawer.

### Pattern 2: Layout Owns the '?' Button and Sheet

The layout reads `helpContent` from context. If `helpContent` is null, the button is hidden (not disabled — just not rendered).

```typescript
// Inside RaceControlLayout.tsx — add to the ml-auto flex group
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription, SheetFooter } from '@/components/ui/sheet';
import { HelpCircle } from 'lucide-react';
import { useHelp } from '@/context/HelpContext';

// In the layout component:
const { helpContent, isOpen, setIsOpen } = useHelp();

// In the header's ml-auto div (before the logout Button):
{helpContent && (
  <Button
    variant="ghost"
    size="icon-sm"
    aria-label="Open help"
    title="Open help"
    onClick={() => setIsOpen(true)}
  >
    <HelpCircle className="h-4 w-4" />
  </Button>
)}

<Sheet open={isOpen} onOpenChange={setIsOpen}>
  <SheetContent side="right" className="w-96">
    <SheetHeader>
      <SheetTitle>Help</SheetTitle>
      <SheetDescription>Page guide</SheetDescription>
    </SheetHeader>
    <div className="overflow-y-auto flex-1 p-6">
      {helpContent}
    </div>
    <SheetFooter>
      {/* Guide link — varies per layout */}
    </SheetFooter>
  </SheetContent>
</Sheet>
```

### Pattern 3: Print Guide Pages

Model: mirrors `PrintResultsPage.tsx` exactly. [VERIFIED: frontend/src/pages/race-control/PrintResultsPage.tsx]

```typescript
// frontend/src/pages/print/MeetingGuidePage.tsx
import { useEffect } from 'react';
// optionally: import { useClubProfile } from '@/hooks/admin/useAdminClub';

export default function MeetingGuidePage() {
  useEffect(() => {
    document.title = 'Race Meeting Guide';
  }, []);

  return (
    <div className="p-8 max-w-3xl mx-auto print:p-4">
      {/* Document header */}
      <div className="mb-6">
        <h1 className="text-2xl font-semibold">Race Meeting Guide</h1>
        <p className="text-sm text-muted-foreground mt-1">For race officials</p>
      </div>

      {/* Guide content sections */}
      {/* ... */}

      {/* Print action */}
      <div className="mt-8 print:hidden">
        <button
          onClick={() => window.print()}
          className="px-4 py-2 bg-primary text-primary-foreground rounded text-sm"
        >
          Print / Save as PDF
        </button>
      </div>
    </div>
  );
}
```

### Pattern 4: Help Article JSX Format

Each `frontend/src/help/XxxHelp.tsx` exports a single component:

```typescript
// frontend/src/help/RaceControlHelp.tsx
export function RaceControlHelp() {
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        The Cockpit is the central race control interface. It shows the current race
        run order, live timing during a race, and controls for starting, stopping,
        and managing races.
      </p>

      <ul className="mt-3 space-y-1.5 text-sm">
        <li><span className="font-semibold">Select a race:</span> Click any race in the run order panel.</li>
        <li><span className="font-semibold">Call the grid:</span> Use the Grid Call button before starting.</li>
        <li><span className="font-semibold">Start/stop:</span> Use the large Start / Stop button during a race.</li>
        <li><span className="font-semibold">Marshal laps:</span> Use the +1 / -1 buttons in the timing table.</li>
        <li><span className="font-semibold">Print results:</span> Click Print after a race finishes.</li>
      </ul>

      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>Starting a race before the grid has been called is blocked by the system.
        If the Start button is greyed out, check the Pre-Race Readiness panel for
        the reason.</p>
      </div>
    </div>
  );
}
```

### Anti-Patterns to Avoid

- **Stale help content:** Not returning a cleanup function from `useEffect` in page components. Without `return () => setHelpContent(null)`, navigating away from a page leaves its help content visible in another page's drawer.
- **Sheet conflict:** AdminPanelLayout already uses `Sheet` for the mobile navigation drawer (controlled by `sheetOpen` state). The help Sheet must use a separate `isOpen` state from `HelpContext` — it must not reuse `sheetOpen`. [VERIFIED: AdminPanelLayout.tsx lines 151-182]
- **Button in sidebar:** AdminPanelLayout has no persistent desktop header — the sidebar occupies the left side and has no '?' slot. The '?' button goes only in the mobile top bar header (`<header className="md:hidden ...">` at line 162). On desktop, admin pages have no layout header, so the '?' button is only available on mobile for admin pages. [VERIFIED: AdminPanelLayout.tsx]
- **Tooltip component not installed:** `frontend/src/components/ui/tooltip.tsx` does not exist. The Tooltip primitive is available in the `radix-ui` package but no shadcn wrapper `.tsx` file has been generated. Either install it via `npx shadcn add tooltip`, or use `title="Open help"` attribute as the UI-SPEC fallback. Do NOT attempt to import from a non-existent `@/components/ui/tooltip`.
- **Missing /print/* route in App.tsx:** The three new print routes must be added as flat unprotected routes at the same level as `/events`, `/results/:raceId`, and `/championships/:id`. They must NOT be nested under any `ProtectedRoute` or layout.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Slide-out drawer | Custom drawer | `shadcn Sheet` | Already installed, handles animation, focus trap, ESC, click-outside |
| Context pattern | Ad-hoc global state | `createContext` + `useState` | Standard React, matches `AuthProvider` already in codebase |
| Print CSS | PDF generation library | `@media print` CSS + `window.print()` | Pattern established in `PrintResultsPage.tsx` and `PrintPracticeResultsPage.tsx` |

---

## Runtime State Inventory

Step 2.5 SKIPPED — this is not a rename, refactor, or migration phase. Phase 9 adds new files only; nothing is renamed or migrated.

---

## Environment Availability Audit

Step 2.6 SKIPPED — Phase 9 is a pure-frontend, code/config-only change. No external services, CLIs, databases, or runtimes beyond the existing Node.js/npm toolchain are required. The frontend dev server (`npm run dev`) and test runner (`npm run test`) are already available from prior phases.

---

## Existing Pages Inventory

All routes enumerated from `frontend/src/App.tsx`. [VERIFIED: App.tsx]

### Pages That Get `useHelp()` Wiring (the ~12 target pages)

| Page Component | Route | Layout | Help Article File |
|----------------|-------|---------|-------------------|
| `CockpitPage` | `/race-control/event/:eventId` (index) | RaceControlLayout | `RaceControlHelp.tsx` |
| `RefereePage` | `/race-control/event/:eventId/referee` | RaceControlLayout | `RefereeHelp.tsx` |
| `PracticeLandingPage` | `/race-control/event/:eventId/practice` | RaceControlLayout | `PracticeHelp.tsx` |
| `EventDetailPage` | `/admin/events/:id` | AdminPanelLayout | `EventManagementHelp.tsx` |
| `EventListPage` | `/admin/events` | AdminPanelLayout | `EntryManagementHelp.tsx` (repurpose or separate) |
| `ChampionshipDetailPage` | `/admin/championships/:id` | AdminPanelLayout | `ChampionshipHelp.tsx` |
| `ProfilePage` | `/racer/profile` | RacerPortalLayout | `RacerProfileHelp.tsx` |
| `CarsPage` | `/racer/cars` | RacerPortalLayout | `CarTransponderHelp.tsx` |
| `TranspondersPage` | `/racer/transponders` | RacerPortalLayout | `CarTransponderHelp.tsx` (shared article) |
| `EntriesPage` | `/racer/entries` | RacerPortalLayout | `EventEntryHelp.tsx` |
| `PublicResultsPage` | `/results/:raceId` | none (standalone) | `ResultsHelp.tsx` |
| `SetupLayout` (wizard) | `/setup` | custom layout | `SetupWizardHelp.tsx` |

**Notes:**
- `RoundGeneratorWizard` is defined in `frontend/src/pages/race-control/RoundGeneratorWizard.tsx` but is **not imported or used anywhere** in the current codebase. It is a dialog component, not a routed page. Skip `RoundGeneratorHelp.tsx` or defer it. [VERIFIED: grep across all .tsx files]
- `PracticeSessionPage` at `/race-control/practice/:sessionId` uses no layout wrapper (protected directly) — adding help there requires a per-page Sheet implementation, which is out of scope. Skip it.
- `PublicResultsPage` has no layout header — help wiring here would require a per-page button. Lower priority; can be skipped.
- `SetupLayout` has its own custom layout with Sheet already used for mobile nav — a second Sheet for help is feasible but adds complexity. Include only if time permits.

**Recommended final 12 (matching D-03 exactly):**

1. `CockpitPage` — RaceControlHelp
2. `RefereePage` — RefereeHelp
3. `PracticeLandingPage` — PracticeHelp
4. `EventDetailPage` — EventManagementHelp
5. `EventListPage` — EntryManagementHelp
6. `ChampionshipDetailPage` — ChampionshipHelp
7. `ProfilePage` — RacerProfileHelp
8. `CarsPage` — CarTransponderHelp
9. `TranspondersPage` — CarTransponderHelp (same article, re-used)
10. `EntriesPage` — EventEntryHelp
11. `RacerResultsPage` — ResultsHelp
12. `SetupLayout` steps — SetupWizardHelp

### Pages That Do NOT Get Help (simple/utility pages per D-03)
- Auth pages: `LoginPage`, `RegisterPage`, `ForgotPasswordPage`, `ResetPasswordPage`
- Print pages: `PrintResultsPage`, `PrintPracticeResultsPage`, new `/print/*` guides
- Error pages: `NotFoundPage`, `UnauthorizedPage`
- List pages of low complexity: `ChampionshipListPage`, `AdminRacersListPage`, `AdminRacerDetailPage`
- `RaceControlSelectPage` (select event to run)
- `ForwarderTokenPage`, `AdminAudioSettingsPage`, `ClubProfilePage`, `TracksPage`, `FormatsPage`, `CarTagCategoriesPage`

---

## Layout Header Placement: Exact Insertion Points

### RaceControlLayout.tsx

**Existing header structure** (line 29-62): [VERIFIED: RaceControlLayout.tsx]
```
<header className="flex items-center h-12 px-4 border-b bg-card shrink-0 gap-4">
  <Link ...>RC Timing</Link>
  <nav ...>  [Cockpit, Practice, Referee links]  </nav>
  <div className="ml-auto flex items-center gap-3">
    <span>{user name}</span>
    <Button variant="ghost" size="icon-sm" onClick={logout}>   // logout button
      <LogOut className="h-4 w-4" />
    </Button>
  </div>
</header>
```

**Insertion:** Add the `?` Button **before** the logout Button inside the `ml-auto` div:
```tsx
<div className="ml-auto flex items-center gap-3">
  <span className="text-xs text-muted-foreground hidden sm:block">
    {user ? `${user.firstName} ${user.lastName}` : ''}
  </span>
  {helpContent && (                                    // NEW
    <Button variant="ghost" size="icon-sm" aria-label="Open help" title="Open help" onClick={() => setIsOpen(true)}>
      <HelpCircle className="h-4 w-4" />
    </Button>
  )}
  <Button variant="ghost" size="icon-sm" onClick={logout} aria-label="Log out">
    <LogOut className="h-4 w-4" />
  </Button>
</div>
```

Also add `<Sheet>` wrapping structure after the header (or as a sibling rendered at layout level).

### AdminPanelLayout.tsx

**Desktop layout:** No persistent header. A fixed left sidebar (`<aside className="hidden md:flex fixed inset-y-0 left-0 w-60 ...">`) renders `<SidebarContent />`. No '?' button slot exists on desktop — the sidebar has no header bar with an `ml-auto` group. [VERIFIED: AdminPanelLayout.tsx lines 157-159]

**Mobile layout:** A top bar at line 162:
```
<header className="md:hidden flex items-center border-b h-14 px-4 sticky top-0 bg-background z-10">
  <Button variant="ghost" size="icon-sm" ...><Menu /></Button>
  <span className="ml-3 font-semibold text-sm">RC Timing — Admin</span>
</header>
```

**Insertion:** Add `?` button after the brand `<span>`:
```tsx
<header className="md:hidden flex items-center border-b h-14 px-4 sticky top-0 bg-background z-10">
  <Button variant="ghost" size="icon-sm" onClick={() => setSheetOpen(true)} aria-label="Open navigation">
    <Menu className="h-5 w-5" />
  </Button>
  <span className="ml-3 font-semibold text-sm">RC Timing — Admin</span>
  {helpContent && (                                         // NEW
    <Button
      variant="ghost"
      size="icon-sm"
      aria-label="Open help"
      title="Open help"
      onClick={() => setIsOpen(true)}
      className="ml-auto"
    >
      <HelpCircle className="h-4 w-4" />
    </Button>
  )}
</header>
```

Note: `setSheetOpen` already exists in `AdminPanelLayout` (controls mobile nav Sheet). The help `isOpen`/`setIsOpen` come from `useHelp()` — they are separate state.

### RacerPortalLayout.tsx

**Desktop nav** (line 22-45): A `<nav className="hidden md:flex items-center border-b px-6 h-14 gap-6">` with nav links and a logout button at `ml-auto`. [VERIFIED: RacerPortalLayout.tsx]

**Insertion in desktop nav:**
```tsx
<button onClick={logout} aria-label="Log out" className="ml-auto flex items-center gap-1.5 ...">
```
Replace `ml-auto` with flex group:
```tsx
<div className="ml-auto flex items-center gap-3">
  {helpContent && (
    <Button variant="ghost" size="icon-sm" aria-label="Open help" title="Open help" onClick={() => setIsOpen(true)}>
      <HelpCircle className="h-4 w-4" />
    </Button>
  )}
  <button onClick={logout} ...>...</button>
</div>
```

Note: The racer portal mobile layout is a bottom nav bar — no persistent mobile header exists. The `?` button only appears on desktop for the racer portal.

---

## Existing shadcn/ui Components Inventory

Confirmed installed files in `frontend/src/components/ui/`: [VERIFIED: directory listing]

| Component | File Exists | Used in Phase 9 |
|-----------|-------------|-----------------|
| `Sheet` | `sheet.tsx` ✓ | Yes — help drawer |
| `Button` | `button.tsx` ✓ | Yes — '?' trigger |
| `Separator` | `separator.tsx` ✓ | Yes — article sections |
| `Badge` | `badge.tsx` ✓ | Optional — key term highlights |
| `Tooltip` | **NOT INSTALLED** | No — use `title` attribute instead |
| `Alert` | `alert.tsx` ✓ | Not needed |
| `Card` | `card.tsx` ✓ | Not needed |

**Tooltip gap:** `tooltip.tsx` is absent from `frontend/src/components/ui/`. The `radix-ui` package (version 1.4.3) bundles `@radix-ui/react-tooltip@1.2.8` [VERIFIED: node_modules/radix-ui/package.json], so Tooltip CAN be used by importing `{ Tooltip as TooltipPrimitive } from 'radix-ui'` or by running `npx shadcn add tooltip` to generate the wrapper file. The UI-SPEC explicitly permits the fallback: "fall back to a native `title="Open help"` attribute on the `<Button>` element." [CITED: 09-UI-SPEC.md] The simplest path is the native `title` attribute — zero new files, zero shadcn commands.

### Sheet Component API (as implemented in codebase)

From `frontend/src/components/ui/sheet.tsx`: [VERIFIED]

```typescript
// Available named exports:
import {
  Sheet,            // Root (Radix Dialog)
  SheetTrigger,     // Trigger
  SheetClose,       // Close button
  SheetContent,     // Content wrapper — accepts side="right"|"left"|"top"|"bottom", showCloseButton?: boolean
  SheetHeader,      // <div className="flex flex-col gap-1.5 p-6">
  SheetFooter,      // <div className="mt-auto flex flex-col gap-2 p-6"> — pinned to bottom
  SheetTitle,       // Radix Dialog.Title
  SheetDescription, // Radix Dialog.Description
} from '@/components/ui/sheet';
```

`SheetContent` defaults: `side="right"`, `showCloseButton={true}` (renders an X button at top-right using `RiCloseLine` from `@remixicon/react`). The Sheet uses `data-open`/`data-closed` animation classes.

`SheetContent` width on right side: Tailwind class `data-[side=right]:w-3/4 data-[side=right]:sm:max-w-sm`. The UI-SPEC requests `w-96` (384px) — this requires overriding with `className="w-96"` on `SheetContent`.

### Button sizes (relevant to '?' button)

From `frontend/src/components/ui/button.tsx`: [VERIFIED]

| Size | CSS class | Pixels |
|------|-----------|--------|
| `icon-xs` | `size-6` | 24px |
| `icon-sm` | `size-8` | 32px ← used for logout button in RaceControlLayout |
| `icon` | `size-9` | 36px |
| `icon-lg` | `size-10` | 40px |

WCAG 2.5.5 requires 44px minimum touch target. `icon-sm` (32px) is below threshold. The existing logout button uses `size="icon-sm"` — matching it keeps visual consistency at the cost of touch target compliance. The UI-SPEC notes this and suggests `p-2` padding wrapper. The planner should pick `size="icon-sm"` to match existing buttons in the same header row (since the logout button already violates this and consistency matters more here than strict compliance).

---

## Common Pitfalls

### Pitfall 1: Stale Help Content After Page Navigation
**What goes wrong:** Navigating from a page that called `useHelp({ content: <XxxHelp /> })` to a page that does not call `useHelp()` leaves the previous page's help content in the drawer. The '?' button appears on a page where it should be hidden.
**Why it happens:** The `useEffect` in the page component runs on mount to register content, but without a cleanup function the content persists after unmount.
**How to avoid:** Always return a cleanup: `useEffect(() => { setHelpContent(<XxxHelp />); return () => setHelpContent(null); }, [setHelpContent]);`
**Warning signs:** Clicking '?' on a page you didn't expect to have help opens a drawer with another page's content.

### Pitfall 2: Sheet State Conflict in AdminPanelLayout
**What goes wrong:** `AdminPanelLayout` already uses a `Sheet` component for the mobile navigation drawer, controlled by `sheetOpen` state. If the help Sheet re-uses `sheetOpen`, opening navigation on mobile also opens the help drawer.
**Why it happens:** Both Sheets need independent open/close state. `sheetOpen` is local to `AdminPanelLayout`; help `isOpen` comes from `useHelp()` context.
**How to avoid:** Never share `sheetOpen` with help Sheet open state. The `isOpen`/`setIsOpen` from `useHelp()` are completely independent. [VERIFIED: AdminPanelLayout.tsx uses `const [sheetOpen, setSheetOpen] = useState(false)` locally]

### Pitfall 3: HelpProvider Outside Router Context
**What goes wrong:** If `HelpProvider` is placed outside `RouterProvider` in `App.tsx`, components inside the help drawer that use `useNavigate` or `<Link>` will throw "useNavigate() may be used only in the context of a Router component".
**Why it happens:** React Router context is provided by `RouterProvider`. Everything that uses router hooks must be inside `RouterProvider`'s tree.
**How to avoid:** Place `HelpProvider` inside `RootLayout` (which is inside `RouterProvider`), after `AuthProvider`. The correct nesting in `App.tsx`:
```tsx
function RootLayout() {
  return (
    <AuthProvider>
      <HelpProvider>          {/* NEW — inside AuthProvider, inside RouterProvider tree */}
        <SetupGuard>
          <Outlet />
        </SetupGuard>
      </HelpProvider>
      <Toaster />
    </AuthProvider>
  );
}
```
`QueryProvider` wraps `RouterProvider` at the top level and is fine staying there.

### Pitfall 4: Print Route Added Inside a Protected Layout
**What goes wrong:** If the `/print/meeting-guide` route is nested under the `/admin` route with `<ProtectedRoute>`, printing without being logged in as an admin fails.
**Why it happens:** The existing `/print/race/:raceId` route is nested inside the race control layout's protected route. The guide print routes must follow the other unprotected pattern.
**How to avoid:** Add the three print routes as flat children of the root layout, alongside `/events` and `/championships/:id`:
```tsx
{ path: '/print/meeting-guide', element: <MeetingGuidePage /> },
{ path: '/print/racer-guide', element: <RacerGuidePage /> },
{ path: '/print/admin-guide', element: <AdminGuidePage /> },
```

### Pitfall 5: Using Non-Existent Tooltip Import
**What goes wrong:** Writing `import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'` causes a build error because `tooltip.tsx` does not exist in the codebase.
**Why it happens:** The UI-SPEC listed Tooltip as "available" based on the shadcn registry, but the file was never generated for this project.
**How to avoid:** Use `title="Open help"` attribute on the `<Button>` element as permitted by UI-SPEC. Or install first: `npx shadcn@latest add tooltip` (but this requires running shadcn CLI and is unnecessary overhead for a `title` attribute).

### Pitfall 6: RoundGeneratorWizard Is Unreachable
**What goes wrong:** Planning a `RoundGeneratorHelp.tsx` article and wiring `useHelp()` in a component that never renders.
**Why it happens:** `RoundGeneratorWizard` is defined in its file but not imported or rendered anywhere in the application. [VERIFIED: grep found zero import sites]
**How to avoid:** Skip `RoundGeneratorHelp.tsx` from the 12-article list. Document it as "component exists but is unrouted — help article deferred."

---

## App.tsx Current Route Structure

Complete enumeration for planner reference. [VERIFIED: App.tsx]

```
/ → redirect to /login
/login → LoginPage
/register → RegisterPage
/forgot-password → ForgotPasswordPage
/reset-password → ResetPasswordPage
/admin (ProtectedRoute ADMIN|RACE_DIRECTOR|REFEREE) → AdminPanelLayout
  /admin → redirect to /admin/events
  /admin/events → EventListPage
  /admin/events/:id → EventDetailPage
  /admin/championships → ChampionshipListPage
  /admin/championships/:id → ChampionshipDetailPage
  /admin/club → ClubProfilePage
  /admin/tracks → TracksPage
  /admin/formats → FormatsPage
  /admin/categories → CarTagCategoriesPage
  /admin/race-control → RaceControlSelectPage
  /admin/forwarder → ForwarderTokenPage
  /admin/audio → AdminAudioSettingsPage
  /admin/racers → AdminRacersListPage
  /admin/racers/:userId → AdminRacerDetailPage
/racer (ProtectedRoute any role) → RacerPortalLayout
  /racer → redirect to /racer/profile
  /racer/profile → ProfilePage
  /racer/cars → CarsPage
  /racer/transponders → TranspondersPage
  /racer/entries → EntriesPage
  /racer/results → RacerResultsPage
/race-control/event/:eventId (ProtectedRoute RACE_DIRECTOR|REFEREE|ADMIN) → RaceControlLayout
  index → CockpitPage
  /practice → PracticeLandingPage
  /referee → RefereePage
  /results/:raceId → PrintResultsPage          ← PRINT, inside protected layout
/race-control/practice/:sessionId (ProtectedRoute RACE_DIRECTOR|ADMIN) → PracticeSessionPage
/race-control/practice/:sessionId/print (ProtectedRoute) → PrintPracticeResultsPage  ← PRINT, protected
/setup → SetupLayout                            ← unprotected
/events → EventSchedulePage                     ← unprotected
/results/:raceId → PublicResultsPage            ← unprotected public
/championships/:id → PublicChampionshipPage     ← unprotected public
/unauthorized → UnauthorizedPage
* → NotFoundPage

NEW routes to add:
/print/meeting-guide → MeetingGuidePage          ← unprotected
/print/racer-guide → RacerGuidePage              ← unprotected
/print/admin-guide → AdminGuidePage              ← unprotected
```

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Vitest 4.1.5 + React Testing Library 16.3.2 |
| Config file | `frontend/vite.config.ts` (test section) |
| Quick run command | `cd frontend && npm test -- --run` |
| Full suite command | `cd frontend && npm test -- --run` |
| Test setup | `frontend/src/test/setup.ts` (jest-dom, ResizeObserver mock) |

### Phase Requirements → Test Map

| SC | Behavior | Test Type | Automated Command | Notes |
|----|----------|-----------|-------------------|-------|
| SC-1 | HelpProvider renders without crashing; useHelp hook throws outside provider | Unit | `npm test -- --run HelpContext` | Wave 0 gap |
| SC-1 | RaceControlLayout renders '?' button when help content registered | Unit | `npm test -- --run RaceControlLayout` | Extend existing test |
| SC-1 | '?' button hidden when no help content registered | Unit | same | |
| SC-1 | AdminPanelLayout '?' button visible in mobile header | Unit | `npm test -- --run AdminPanelLayout` | Extend existing test |
| SC-2/3/4 | Print guide pages render title and print button | Unit | `npm test -- --run MeetingGuidePage` | Wave 0 gap |
| SC-5 | No test applicable — documentation currency is human-review only | Manual | — | |

### Sampling Rate
- **Per task commit:** `cd frontend && npm test -- --run`
- **Per wave merge:** `cd frontend && npm test -- --run`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `frontend/src/context/HelpContext.test.tsx` — SC-1 provider + hook tests
- [ ] `frontend/src/pages/print/MeetingGuidePage.test.tsx` — SC-2 print page renders
- [ ] `frontend/src/pages/print/RacerGuidePage.test.tsx` — SC-3
- [ ] `frontend/src/pages/print/AdminGuidePage.test.tsx` — SC-4

Existing tests to extend:
- [ ] `frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx` — add '?' button assertion
- [ ] `frontend/src/pages/race-control/RaceControlLayout` (no test exists) — create stub

---

## Security Domain

This phase adds no authentication, no data writes, no user input fields, and no server-side logic. All new code is client-side read-only documentation UI.

| ASVS Category | Applies | Notes |
|---------------|---------|-------|
| V2 Authentication | No | No new auth flows |
| V3 Session Management | No | No new session logic |
| V4 Access Control | No | Print routes are intentionally unprotected (consistent with existing unprotected public pages) |
| V5 Input Validation | No | No user input in help content or print guides |
| V6 Cryptography | No | No crypto |

The only access-control decision: print routes (`/print/meeting-guide` etc.) are unprotected. This matches the UI-SPEC [CITED: 09-UI-SPEC.md constraint 4] and the existing pattern for `/events` and `/championships/:id`. Officials should be able to print guides from any browser without logging in.

---

## Open Questions

1. **Club name in print guides**
   - What we know: `useClubProfile()` hook exists in `frontend/src/hooks/admin/useAdminClub.ts` and fetches `GET /api/v1/admin/club/profile`. [VERIFIED: useAdminClub.ts]
   - What's unclear: Print guides are unprotected routes. The `/api/v1/admin/club/profile` endpoint may require auth. If it does, the `useClubProfile()` hook will fail for unauthenticated visitors.
   - Recommendation: Use a static placeholder `"RC Timing Club"` for v1 rather than injecting the club name dynamically. Avoids auth complexity. Note: `PrintResultsPage.tsx` DOES show club name via `data.clubBranding?.clubName` (from the result snapshot which is public), so a public club-branding endpoint may already exist — but this needs verification at implementation time.

2. **SetupLayout as a help-wiring target**
   - What we know: `SetupLayout` has its own custom layout with a Sheet already in use (line 7: `import { Sheet, ... } from '@/components/ui/sheet'`). [VERIFIED: SetupLayout.tsx line 7]
   - What's unclear: Wiring `useHelp()` inside `SetupLayout` while it already manages a Sheet creates two Sheets in one component. This is technically fine (independent state) but adds complexity.
   - Recommendation: Skip `SetupLayout` from the first plan wave. Include only if the other 11 pages are completed with time to spare.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `useClubProfile()` requires admin auth and will fail for unauthenticated visitors on print pages | Open Questions | Print guides show broken club name; mitigated by using static placeholder |
| A2 | The RoundGeneratorWizard is genuinely unreachable — no route or import in the current app | Pages Inventory | If wired in future, a help article would be needed; low risk since it's currently dead code |

---

## Sources

### Primary (HIGH confidence — verified from live codebase)
- `frontend/src/providers/AuthProvider.tsx` — provider/hook pattern to replicate
- `frontend/src/hooks/useAuth.ts` — hook pattern
- `frontend/src/pages/race-control/PrintResultsPage.tsx` — print page pattern
- `frontend/src/pages/race-control/PrintPracticeResultsPage.tsx` — second print page example
- `frontend/src/pages/race-control/RaceControlLayout.tsx` — exact header markup for '?' insertion
- `frontend/src/pages/admin/AdminPanelLayout.tsx` — sidebar layout, mobile header, existing Sheet usage
- `frontend/src/pages/racer/RacerPortalLayout.tsx` — racer portal nav structure
- `frontend/src/App.tsx` — full route map
- `frontend/src/components/ui/sheet.tsx` — Sheet API and exports
- `frontend/src/components/ui/button.tsx` — Button variants and sizes
- `frontend/package.json` — installed packages and versions
- `frontend/src/test/setup.ts` — test setup file
- `frontend/vite.config.ts` — test framework configuration
- `frontend/components.json` — shadcn configuration
- `frontend/src/hooks/admin/useAdminClub.ts` — useClubProfile hook
- Directory listing of `frontend/src/components/ui/` — Tooltip absence confirmed
- `node_modules/radix-ui/package.json` — Tooltip availability in radix-ui bundle

### Secondary (MEDIUM confidence)
- `.planning/phases/09-user-manual-documentation/09-CONTEXT.md` — locked decisions
- `.planning/phases/09-user-manual-documentation/09-UI-SPEC.md` — UI design contract (checker-approved)
- `.planning/ROADMAP.md` — success criteria SC-1 through SC-5

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries verified from package.json and node_modules
- Architecture patterns: HIGH — verified against live AuthProvider, Sheet, and Layout code
- '?' button placement: HIGH — exact JSX lines cited from each layout file
- Tooltip gap: HIGH — directory listing confirmed absence; radix-ui package confirmed availability
- RoundGeneratorWizard unreachable: HIGH — grep across all .tsx files found zero import sites
- Pitfalls: HIGH — all derived from direct code reading, not training knowledge

**Research date:** 2026-05-15
**Valid until:** 2026-06-15 (stable frontend stack, no planned changes)
