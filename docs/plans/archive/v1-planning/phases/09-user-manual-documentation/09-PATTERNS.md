# Phase 9: User Manual & Documentation - Pattern Map

**Mapped:** 2026-05-15
**Files analyzed:** 24 (5 new infrastructure files, 12 new help articles, 3 new print guides, 4 modified files)
**Analogs found:** 24 / 24

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/context/HelpContext.tsx` | provider + hook | request-response | `frontend/src/providers/AuthProvider.tsx` + `frontend/src/hooks/useAuth.ts` | exact |
| `frontend/src/help/RaceControlHelp.tsx` | component | — (static JSX) | `frontend/src/pages/race-control/panels/ForwarderStatusBar.tsx` (Tailwind/shadcn prose) | role-match |
| `frontend/src/help/RefereeHelp.tsx` | component | — (static JSX) | same as above | role-match |
| `frontend/src/help/PracticeHelp.tsx` | component | — (static JSX) | same as above | role-match |
| `frontend/src/help/EventManagementHelp.tsx` | component | — (static JSX) | same as above | role-match |
| `frontend/src/help/EntryManagementHelp.tsx` | component | — (static JSX) | same as above | role-match |
| `frontend/src/help/ChampionshipHelp.tsx` | component | — (static JSX) | same as above | role-match |
| `frontend/src/help/RacerProfileHelp.tsx` | component | — (static JSX) | same as above | role-match |
| `frontend/src/help/CarTransponderHelp.tsx` | component | — (static JSX) | same as above | role-match |
| `frontend/src/help/EventEntryHelp.tsx` | component | — (static JSX) | same as above | role-match |
| `frontend/src/help/ResultsHelp.tsx` | component | — (static JSX) | same as above | role-match |
| `frontend/src/help/SetupWizardHelp.tsx` | component | — (static JSX) | same as above | role-match |
| `frontend/src/pages/print/MeetingGuidePage.tsx` | page | request-response | `frontend/src/pages/race-control/PrintResultsPage.tsx` | exact |
| `frontend/src/pages/print/RacerGuidePage.tsx` | page | request-response | `frontend/src/pages/race-control/PrintResultsPage.tsx` | exact |
| `frontend/src/pages/print/AdminGuidePage.tsx` | page | request-response | `frontend/src/pages/race-control/PrintResultsPage.tsx` | exact |
| `frontend/src/App.tsx` (modify) | config/router | — | itself (additive change) | exact |
| `frontend/src/pages/race-control/RaceControlLayout.tsx` (modify) | layout | request-response | itself (additive change) | exact |
| `frontend/src/pages/admin/AdminPanelLayout.tsx` (modify) | layout | request-response | itself (additive change) | exact |
| `frontend/src/pages/racer/RacerPortalLayout.tsx` (modify) | layout | request-response | itself (additive change) | exact |
| 12 target pages (modify — add `useHelp` call) | page | request-response | `frontend/src/pages/race-control/CockpitPage.tsx` (any page calling a context hook) | exact |

---

## Pattern Assignments

### `frontend/src/context/HelpContext.tsx` (provider + hook)

**Analog:** `frontend/src/providers/AuthProvider.tsx` (lines 1–109) + `frontend/src/hooks/useAuth.ts` (lines 1–8)

**Imports pattern** (from AuthProvider.tsx lines 1–6, simplified for HelpContext):
```typescript
import React, { createContext, useContext, useState } from 'react';
```
Note: No `useNavigate`, no `axios`, no `api` — HelpContext has no async I/O.

**Context + export pattern** (AuthProvider.tsx lines 16–40, adapted):
```typescript
// AuthProvider exports the context directly for the hook to consume
// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | null>(null);
```
Apply same `eslint-disable-next-line react-refresh/only-export-components` comment above the context export — the linter flags non-component exports from component files.

**Provider component pattern** (AuthProvider.tsx lines 42–109):
```typescript
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  // ...
  return (
    <AuthContext.Provider value={{ user, accessToken: accessTokenState, login, logout, isLoading, setAuthFromToken }}>
      {children}
    </AuthContext.Provider>
  );
}
```
For HelpContext: two `useState` calls (`helpContent: React.ReactNode | null`, `isOpen: boolean`), no `useEffect`, no async logic. Provider body is minimal.

**Hook pattern** (useAuth.ts lines 1–8 — copy exactly):
```typescript
import { useContext } from 'react';
import { AuthContext } from '@/providers/AuthProvider';

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
```
For `useHelp`: change import source to `@/context/HelpContext`, change function name to `useHelp`, change error message to `'useHelp must be used within HelpProvider'`. The hook lives in the same file as the provider (not a separate file), because HelpContext is simpler than Auth and the RESEARCH.md pattern collocates them.

**File placement:** `frontend/src/context/HelpContext.tsx` (new `context/` directory — analogous to `providers/` but scoped to UI state, not auth).

---

### `frontend/src/pages/print/MeetingGuidePage.tsx` (page, print)
### `frontend/src/pages/print/RacerGuidePage.tsx` (page, print)
### `frontend/src/pages/print/AdminGuidePage.tsx` (page, print)

**Analog:** `frontend/src/pages/race-control/PrintResultsPage.tsx` (lines 1–104) and `frontend/src/pages/race-control/PrintPracticeResultsPage.tsx` (lines 1–113)

**Imports pattern** (PrintResultsPage.tsx lines 1–3):
```typescript
import { useEffect } from 'react';
// No useParams — print guides have no dynamic ID
// No data hooks — print guides are static content
```
For static print guides: only `import { useEffect } from 'react';`. No query hooks unless club name injection is implemented (see Open Questions in RESEARCH.md — default to static placeholder).

**Document title pattern** (PrintResultsPage.tsx lines 21–25):
```typescript
useEffect(() => {
  if (data) {
    document.title = `Results — ${data.raceLabel}`;
  }
}, [data]);
```
For static guides (no `data` dependency):
```typescript
useEffect(() => {
  document.title = 'Race Meeting Guide';   // or 'Racer Quick-Start Guide' / 'Admin Configuration Guide'
}, []);
```
Empty dependency array — runs once on mount.

**Page wrapper pattern** (PrintResultsPage.tsx line 42):
```typescript
<div className="p-8 max-w-3xl mx-auto print:p-4">
```
Copy this class string exactly. `max-w-3xl` constrains line length for readability. `print:p-4` reduces margin in print output.

**Header section pattern** (PrintResultsPage.tsx lines 43–61):
```typescript
<div className="mb-6">
  <div className="flex items-start justify-between">
    <div>
      <h1 className="text-2xl font-bold">{data.raceLabel}</h1>
      <p className="text-sm text-muted-foreground mt-1">
        {data.clubBranding?.clubName} &bull; Finished{' '}
        {new Date(data.finishedAt).toLocaleString()}
      </p>
    </div>
  </div>
</div>
```
For static guides: replace with static title and subtitle. No `data.clubBranding` — use placeholder `"RC Timing Club"` until open question on auth is resolved.

**Print button pattern** (PrintResultsPage.tsx lines 93–101):
```typescript
<div className="mt-6 print:hidden">
  <button
    onClick={() => window.print()}
    className="px-4 py-2 bg-primary text-primary-foreground rounded text-sm"
  >
    Print
  </button>
</div>
```
Copy exactly. `print:hidden` hides the button in the printed output. Label can be "Print / Save as PDF" for clarity.

**Route registration pattern** (App.tsx lines 136–138 — existing unprotected flat routes):
```typescript
{ path: '/events', element: <EventSchedulePage /> },
{ path: '/results/:raceId', element: <PublicResultsPage /> },
{ path: '/championships/:id', element: <PublicChampionshipPage /> },
```
New print routes follow the same pattern — flat children of the root router array, NOT nested under any `ProtectedRoute` or layout element:
```typescript
{ path: '/print/meeting-guide', element: <MeetingGuidePage /> },
{ path: '/print/racer-guide', element: <RacerGuidePage /> },
{ path: '/print/admin-guide', element: <AdminGuidePage /> },
```

---

### `frontend/src/help/RaceControlHelp.tsx` and all other `frontend/src/help/*.tsx` files (static JSX components)

**No direct analog exists** — the codebase has no static prose content components. The pattern is pure Tailwind/shadcn styling applied to HTML structure.

**Styling conventions** (extracted from shadcn component prose patterns and Tailwind usage across all pages):

```typescript
// Named export (not default) — matches the RESEARCH.md pattern
export function RaceControlHelp() {
  return (
    <div className="space-y-4">
      {/* 2–3 sentence description */}
      <p className="text-sm text-muted-foreground">
        ...
      </p>

      {/* Bulleted key actions list */}
      <ul className="mt-3 space-y-1.5 text-sm">
        <li>
          <span className="font-semibold">Action name:</span> Description.
        </li>
        {/* 3–5 items */}
      </ul>

      {/* Common mistakes block */}
      <div className="mt-4 rounded-md bg-muted p-3 text-sm">
        <p className="font-semibold mb-1">Common mistakes</p>
        <p>...</p>
      </div>

      {/* Print guide link — at bottom, only in relevant articles */}
      <div className="mt-4 pt-4 border-t">
        <a
          href="/print/meeting-guide"
          target="_blank"
          rel="noopener noreferrer"
          className="text-sm text-primary hover:underline"
        >
          Open Race Meeting Guide (printable)
        </a>
      </div>
    </div>
  );
}
```

**Link targets per article:**
- `RaceControlHelp`, `RefereeHelp`, `PracticeHelp` → `/print/meeting-guide`
- `RacerProfileHelp`, `CarTransponderHelp`, `EventEntryHelp`, `ResultsHelp` → `/print/racer-guide`
- `EventManagementHelp`, `EntryManagementHelp`, `ChampionshipHelp`, `SetupWizardHelp` → `/print/admin-guide`

---

### `frontend/src/pages/race-control/RaceControlLayout.tsx` (modify — add '?' button + Sheet)

**Analog:** itself. Exact insertion point verified from the live file.

**Current header structure** (RaceControlLayout.tsx lines 29–62):
```typescript
<header className="flex items-center h-12 px-4 border-b bg-card shrink-0 gap-4">
  <Link to="/admin/race-control" ...>
    <ChevronLeft className="h-4 w-4" />
    <span className="text-sm font-semibold">RC Timing</span>
  </Link>

  <nav className="flex items-center gap-1 ml-4">
    {/* Cockpit / Practice / Referee links */}
  </nav>

  <div className="ml-auto flex items-center gap-3">
    <span className="text-xs text-muted-foreground hidden sm:block">
      {user ? `${user.firstName} ${user.lastName}` : ''}
    </span>
    <Button variant="ghost" size="icon-sm" onClick={logout} aria-label="Log out">
      <LogOut className="h-4 w-4" />
    </Button>
  </div>
</header>
```

**Imports to add** (lines 1–8, insert after existing imports):
```typescript
import { HelpCircle } from 'lucide-react';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet';
import { useHelp } from '@/context/HelpContext';
```
`Sheet` is already imported in `AdminPanelLayout.tsx` — use the same import path `@/components/ui/sheet`.

**Hook call to add** (inside component function body, after existing `const { user, logout } = useAuth();`):
```typescript
const { helpContent, isOpen, setIsOpen } = useHelp();
```

**'?' button insertion** (inside `<div className="ml-auto flex items-center gap-3">`, BEFORE the logout Button):
```typescript
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
<Button variant="ghost" size="icon-sm" onClick={logout} aria-label="Log out">
  <LogOut className="h-4 w-4" />
</Button>
```
`size="icon-sm"` matches the existing logout button size (line 58). `title="Open help"` provides the tooltip via native attribute — no Tooltip component needed.

**Sheet JSX to add** (after the closing `</header>` tag, before `<Separator />`):
```typescript
<Sheet open={isOpen} onOpenChange={setIsOpen}>
  <SheetContent side="right" className="w-96" showCloseButton>
    <SheetHeader>
      <SheetTitle>Help</SheetTitle>
      <SheetDescription>Page guide</SheetDescription>
    </SheetHeader>
    <div className="overflow-y-auto flex-1 px-6 pb-6">
      {helpContent}
    </div>
  </SheetContent>
</Sheet>
```
`className="w-96"` overrides the default `sm:max-w-sm` from sheet.tsx line 63. `showCloseButton` (default `true`) renders the X button from sheet.tsx lines 69–81.

---

### `frontend/src/pages/admin/AdminPanelLayout.tsx` (modify — add '?' button in mobile header + Sheet)

**Analog:** itself. Key constraint: `sheetOpen` already exists (line 152) for the nav drawer — do NOT reuse it.

**Current mobile header** (AdminPanelLayout.tsx lines 162–172):
```typescript
<header className="md:hidden flex items-center border-b h-14 px-4 sticky top-0 bg-background z-10">
  <Button
    variant="ghost"
    size="icon-sm"
    onClick={() => setSheetOpen(true)}
    aria-label="Open navigation"
  >
    <Menu className="h-5 w-5" />
  </Button>
  <span className="ml-3 font-semibold text-sm">RC Timing — Admin</span>
</header>
```

**Existing Sheet usage** (AdminPanelLayout.tsx lines 175–182 — nav Sheet, must remain unchanged):
```typescript
<Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
  <SheetContent side="left" showCloseButton className="w-72 p-0">
    <SheetHeader className="sr-only">
      <SheetTitle>Navigation</SheetTitle>
    </SheetHeader>
    <SidebarContent onNavClick={() => setSheetOpen(false)} />
  </SheetContent>
</Sheet>
```

**Imports to add** (lines 21 — Sheet already imported; add new named imports):
```typescript
// Sheet, SheetContent, SheetHeader, SheetTitle already imported at line 21
// Add to the lucide-react import at lines 2–17:
import { ..., HelpCircle } from 'lucide-react';
// Add after existing imports:
import { SheetDescription } from '@/components/ui/sheet';
import { useHelp } from '@/context/HelpContext';
```

**Hook call to add** (inside `AdminPanelLayout` function body, after `const [sheetOpen, setSheetOpen] = useState(false);`):
```typescript
const { helpContent, isOpen, setIsOpen } = useHelp();
```

**'?' button insertion in mobile header** (after `<span className="ml-3 font-semibold text-sm">RC Timing — Admin</span>`):
```typescript
{helpContent && (
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
```
`className="ml-auto"` pushes the button to the far right of the mobile header, away from the hamburger menu and brand name.

**Help Sheet to add** (after the nav `<Sheet>` block at line 182, before `<main>`):
```typescript
<Sheet open={isOpen} onOpenChange={setIsOpen}>
  <SheetContent side="right" className="w-96" showCloseButton>
    <SheetHeader>
      <SheetTitle>Help</SheetTitle>
      <SheetDescription>Page guide</SheetDescription>
    </SheetHeader>
    <div className="overflow-y-auto flex-1 px-6 pb-6">
      {helpContent}
    </div>
  </SheetContent>
</Sheet>
```

**Desktop note:** No '?' button on desktop admin layout — the fixed sidebar (`<aside className="hidden md:flex ...">` at line 157) has no header bar with an `ml-auto` slot. Mobile header only.

---

### `frontend/src/pages/racer/RacerPortalLayout.tsx` (modify — add '?' button in desktop nav + Sheet)

**Analog:** itself.

**Current desktop nav** (RacerPortalLayout.tsx lines 22–46):
```typescript
<nav className="hidden md:flex items-center border-b px-6 h-14 gap-6">
  <span className="font-semibold mr-4">RC Timing</span>
  {navItems.map(...)}
  <button
    onClick={logout}
    aria-label="Log out"
    className="ml-auto flex items-center gap-1.5 text-muted-foreground hover:text-foreground text-sm"
  >
    <LogOut className="h-4 w-4" aria-hidden="true" />
    Log out
  </button>
</nav>
```

**Imports to add**:
```typescript
import { HelpCircle } from 'lucide-react';   // add to existing lucide import
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet';
import { useHelp } from '@/context/HelpContext';
```

**Hook call to add** (inside `RacerPortalLayout` function body, after `const { logout } = useAuth();`):
```typescript
const { helpContent, isOpen, setIsOpen } = useHelp();
```

**Desktop nav modification** — wrap logout button in a flex group to insert '?' before it:
```typescript
<div className="ml-auto flex items-center gap-3">
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
  <button
    onClick={logout}
    aria-label="Log out"
    className="flex items-center gap-1.5 text-muted-foreground hover:text-foreground text-sm"
  >
    <LogOut className="h-4 w-4" aria-hidden="true" />
    Log out
  </button>
</div>
```
Remove `ml-auto` from the `<button>` and move it to the new wrapper `<div>`.

**Help Sheet to add** (after the desktop `<nav>`, before `<main>`):
```typescript
<Sheet open={isOpen} onOpenChange={setIsOpen}>
  <SheetContent side="right" className="w-96" showCloseButton>
    <SheetHeader>
      <SheetTitle>Help</SheetTitle>
      <SheetDescription>Page guide</SheetDescription>
    </SheetHeader>
    <div className="overflow-y-auto flex-1 px-6 pb-6">
      {helpContent}
    </div>
  </SheetContent>
</Sheet>
```

**Mobile note:** RacerPortalLayout has no mobile top bar header — only a bottom nav (lines 54–83). No '?' button on mobile for the racer portal.

---

### Each of the 12 target pages (modify — add `useHelp` call)

**Pattern:** Add `useEffect` hook that registers and cleans up help content.

**Imports to add** (at the top of each target page file):
```typescript
import { useEffect } from 'react';              // if not already imported
import { useHelp } from '@/context/HelpContext';
import { RaceControlHelp } from '@/help/RaceControlHelp';  // substitute appropriate article
```

**Hook call pattern** (inside the page's component function, after existing hook calls):
```typescript
const { setHelpContent } = useHelp();

useEffect(() => {
  setHelpContent(<RaceControlHelp />);           // substitute appropriate Help component
  return () => setHelpContent(null);             // CRITICAL: cleanup on unmount
}, [setHelpContent]);
```
The `return () => setHelpContent(null)` cleanup is mandatory. Without it, navigating away leaves stale content in the drawer.

**Target pages and their articles:**

| Page file | Article import |
|-----------|---------------|
| `CockpitPage.tsx` | `RaceControlHelp` from `@/help/RaceControlHelp` |
| `RefereePage.tsx` | `RefereeHelp` from `@/help/RefereeHelp` |
| `PracticeLandingPage.tsx` | `PracticeHelp` from `@/help/PracticeHelp` |
| `EventDetailPage.tsx` | `EventManagementHelp` from `@/help/EventManagementHelp` |
| `EventListPage.tsx` | `EntryManagementHelp` from `@/help/EntryManagementHelp` |
| `ChampionshipDetailPage.tsx` | `ChampionshipHelp` from `@/help/ChampionshipHelp` |
| `ProfilePage.tsx` | `RacerProfileHelp` from `@/help/RacerProfileHelp` |
| `CarsPage.tsx` | `CarTransponderHelp` from `@/help/CarTransponderHelp` |
| `TranspondersPage.tsx` | `CarTransponderHelp` from `@/help/CarTransponderHelp` (shared article) |
| `EntriesPage.tsx` | `EventEntryHelp` from `@/help/EventEntryHelp` |
| `RacerResultsPage.tsx` | `ResultsHelp` from `@/help/ResultsHelp` |
| `SetupLayout.tsx` | `SetupWizardHelp` from `@/help/SetupWizardHelp` (complexity risk — defer to last) |

---

### `frontend/src/App.tsx` (modify — HelpProvider + 3 new routes)

**HelpProvider insertion** (App.tsx lines 45–54 — current RootLayout):
```typescript
function RootLayout() {
  return (
    <AuthProvider>
      <SetupGuard>
        <Outlet />
      </SetupGuard>
      <Toaster />
    </AuthProvider>
  );
}
```

**Modified RootLayout** — insert `HelpProvider` inside `AuthProvider`, wrapping `SetupGuard`:
```typescript
function RootLayout() {
  return (
    <AuthProvider>
      <HelpProvider>
        <SetupGuard>
          <Outlet />
        </SetupGuard>
      </HelpProvider>
      <Toaster />
    </AuthProvider>
  );
}
```
`HelpProvider` must be INSIDE `AuthProvider` (so help articles can use `useAuth` if needed) and INSIDE `RouterProvider` tree (so `<Link>` in help articles works). `Toaster` stays outside `HelpProvider` — it has no dependency on help state.

**New route registration** (App.tsx lines 136–140 — after existing flat unprotected routes):
```typescript
// Existing:
{ path: '/events', element: <EventSchedulePage /> },
{ path: '/results/:raceId', element: <PublicResultsPage /> },
{ path: '/championships/:id', element: <PublicChampionshipPage /> },
// Add after:
{ path: '/print/meeting-guide', element: <MeetingGuidePage /> },
{ path: '/print/racer-guide', element: <RacerGuidePage /> },
{ path: '/print/admin-guide', element: <AdminGuidePage /> },
```

**Import additions** (lines 1–43 — add alongside existing page imports):
```typescript
import { HelpProvider } from '@/context/HelpContext';
import MeetingGuidePage from '@/pages/print/MeetingGuidePage';
import RacerGuidePage from '@/pages/print/RacerGuidePage';
import AdminGuidePage from '@/pages/print/AdminGuidePage';
```

---

## Shared Patterns

### Context + Hook Colocation
**Source:** Pattern derived from `frontend/src/providers/AuthProvider.tsx` (provider) + `frontend/src/hooks/useAuth.ts` (hook)
**Apply to:** `HelpContext.tsx` — colocate both the `HelpProvider` and `useHelp` hook in a single file (`frontend/src/context/HelpContext.tsx`), unlike Auth which separates them. Acceptable because HelpContext has no async logic and is simpler.
```typescript
// eslint-disable-next-line react-refresh/only-export-components
export const HelpContext = createContext<HelpContextValue | null>(null);

export function HelpProvider({ children }: { children: React.ReactNode }) { ... }

export function useHelp() {
  const ctx = useContext(HelpContext);
  if (!ctx) throw new Error('useHelp must be used within HelpProvider');
  return ctx;
}
```

### Sheet Component Usage
**Source:** `frontend/src/pages/admin/AdminPanelLayout.tsx` lines 21, 175–182
**Apply to:** All three layout files (RaceControlLayout, AdminPanelLayout, RacerPortalLayout) for the help Sheet
```typescript
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';

<Sheet open={isOpen} onOpenChange={setIsOpen}>
  <SheetContent side="right" className="w-96" showCloseButton>
    <SheetHeader>
      <SheetTitle>Help</SheetTitle>
      <SheetDescription>Page guide</SheetDescription>
    </SheetHeader>
    <div className="overflow-y-auto flex-1 px-6 pb-6">
      {helpContent}
    </div>
  </SheetContent>
</Sheet>
```

### Button Size Consistency
**Source:** `frontend/src/pages/race-control/RaceControlLayout.tsx` line 58
**Apply to:** All '?' buttons across all layouts
```typescript
<Button variant="ghost" size="icon-sm" aria-label="Open help" title="Open help" onClick={() => setIsOpen(true)}>
  <HelpCircle className="h-4 w-4" />
</Button>
```
`size="icon-sm"` (32px, `size-8`) matches the existing logout button in RaceControlLayout. `title="Open help"` is the native tooltip fallback — no Tooltip component needed.

### Print Page Structure
**Source:** `frontend/src/pages/race-control/PrintResultsPage.tsx` lines 41–103
**Apply to:** All three print guide pages
```typescript
// Wrapper
<div className="p-8 max-w-3xl mx-auto print:p-4">
  {/* Header */}
  <div className="mb-6"> ... </div>
  {/* Sections */}
  {/* Print button */}
  <div className="mt-6 print:hidden">
    <button onClick={() => window.print()} className="px-4 py-2 bg-primary text-primary-foreground rounded text-sm">
      Print / Save as PDF
    </button>
  </div>
</div>
```

### `useEffect` for `document.title`
**Source:** `frontend/src/pages/race-control/PrintResultsPage.tsx` lines 21–25, `PrintPracticeResultsPage.tsx` lines 37–41
**Apply to:** All three print guide pages
```typescript
useEffect(() => {
  document.title = 'Race Meeting Guide';
}, []);
```
Empty deps array for static guides (no data to wait for).

### Test File Structure
**Source:** `frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx` (lines 1–36)
**Apply to:** New test files for HelpContext and print pages
```typescript
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: { ... },
    logout: vi.fn(),
    // ...
  }),
}));

describe('ComponentName (Wave 0 stub)', () => {
  it('renders without crashing', () => {
    render(<MemoryRouter><ComponentName /></MemoryRouter>);
    expect(screen.getByText('...')).toBeTruthy();
  });
});
```
For `HelpContext.test.tsx`: wrap in a `HelpProvider` directly (no MemoryRouter needed for isolated hook tests). For print page tests: wrap in `MemoryRouter` (pages use no router hooks, but convention is consistent).

---

## No Analog Found

All files have analogs. The `frontend/src/help/*.tsx` files are the weakest match — static prose components have no direct equivalent in the current codebase — but the Tailwind class conventions and component styling principles are consistent throughout the app.

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `frontend/src/help/*.tsx` (12 files) | component | static | No static prose content components exist in codebase. Pattern derived from Tailwind + shadcn conventions used across all other components. |

---

## Metadata

**Analog search scope:** `frontend/src/providers/`, `frontend/src/hooks/`, `frontend/src/pages/race-control/`, `frontend/src/pages/admin/`, `frontend/src/pages/racer/`, `frontend/src/components/ui/`, `frontend/src/App.tsx`
**Files read:** 12 source files (AuthProvider.tsx, useAuth.ts, QueryProvider.tsx, PrintResultsPage.tsx, PrintPracticeResultsPage.tsx, RaceControlLayout.tsx, AdminPanelLayout.tsx, RacerPortalLayout.tsx, App.tsx, sheet.tsx, AdminPanelLayout.test.tsx, SetupGuard.test.tsx)
**Pattern extraction date:** 2026-05-15
