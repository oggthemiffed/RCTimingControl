---
phase: 09-user-manual-documentation
reviewed: 2026-05-15T00:00:00Z
depth: standard
files_reviewed: 36
files_reviewed_list:
  - frontend/src/App.tsx
  - frontend/src/context/HelpContext.test.tsx
  - frontend/src/context/HelpContext.tsx
  - frontend/src/help/CarTransponderHelp.tsx
  - frontend/src/help/ChampionshipHelp.tsx
  - frontend/src/help/EntryManagementHelp.tsx
  - frontend/src/help/EventEntryHelp.tsx
  - frontend/src/help/EventManagementHelp.tsx
  - frontend/src/help/PracticeHelp.tsx
  - frontend/src/help/RaceControlHelp.tsx
  - frontend/src/help/RacerProfileHelp.tsx
  - frontend/src/help/RefereeHelp.tsx
  - frontend/src/help/ResultsHelp.tsx
  - frontend/src/help/SetupWizardHelp.tsx
  - frontend/src/pages/admin/AdminPanelLayout.tsx
  - frontend/src/pages/admin/championships/ChampionshipDetailPage.tsx
  - frontend/src/pages/admin/events/EventDetailPage.tsx
  - frontend/src/pages/admin/events/EventListPage.tsx
  - frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx
  - frontend/src/pages/print/AdminGuidePage.test.tsx
  - frontend/src/pages/print/AdminGuidePage.tsx
  - frontend/src/pages/print/MeetingGuidePage.test.tsx
  - frontend/src/pages/print/MeetingGuidePage.tsx
  - frontend/src/pages/print/RacerGuidePage.test.tsx
  - frontend/src/pages/print/RacerGuidePage.tsx
  - frontend/src/pages/race-control/CockpitPage.tsx
  - frontend/src/pages/race-control/PracticeLandingPage.tsx
  - frontend/src/pages/race-control/RaceControlLayout.tsx
  - frontend/src/pages/race-control/RefereePage.tsx
  - frontend/src/pages/racer/CarsPage.tsx
  - frontend/src/pages/racer/EntriesPage.tsx
  - frontend/src/pages/racer/ProfilePage.tsx
  - frontend/src/pages/racer/RacerPortalLayout.tsx
  - frontend/src/pages/racer/RacerResultsPage.tsx
  - frontend/src/pages/racer/TranspondersPage.tsx
  - frontend/src/pages/setup/SetupLayout.tsx
findings:
  critical: 1
  warning: 7
  info: 5
  total: 13
status: issues_found
---

# Phase 09: Code Review Report

**Reviewed:** 2026-05-15
**Depth:** standard
**Files Reviewed:** 36
**Status:** issues_found

## Summary

This phase added in-app contextual help panels (HelpContext + 10 help components), three printable guide pages (MeetingGuidePage, RacerGuidePage, AdminGuidePage), and wired help content into pages across all three layout trees (Racer Portal, Admin Panel, Race Control). The HelpContext implementation itself is clean and correctly guarded. The main issues are: one critical logic bug in the `CockpitPage` beep detection that produces false positives on every render; several incorrect or incomplete wiring decisions in `AdminPanelLayout` (help button invisible on desktop); a misleading help text mis-assignment on `TranspondersPage`; stub pages shipping help content describing features not yet implemented; and a missing help hook up in `SetupLayout` that causes the help sheet to never open even though content is registered.

---

## Critical Issues

### CR-01: Audio beep fires on every `liveRows` re-render, not only on new laps

**File:** `frontend/src/pages/race-control/CockpitPage.tsx:106-125`

**Issue:** The beep-detection effect compares `row.lastPassingTimeMs !== prevTime` where `prevTime` comes from `prevPassingRef.current`. After a lap is detected on a given `row.entryId`, `prev.set(row.entryId, row.lastPassingTimeMs)` stores the value. However, `prevPassingRef.current` is a `useRef` — it persists across renders — but the `liveRows` array returned by `useLiveTiming` is a new array reference on every WebSocket message even when no new laps occurred (the rows update in-place with the same timestamp values). The condition `row.lastPassingTimeMs !== prevTime` will be `false` in that case, so no double-beep occurs there. The real bug is the opposite case: the effect only runs when `liveRows` changes (React dependency array). When `liveRows` is structurally the same object but React re-renders for other reasons (e.g. `selectedRace?.status` changes), the effect does **not** re-fire — this is correct. **However**, the improving-lap condition is wrong:

```tsx
const improving =
  row.lastLapMs !== null &&
  row.bestLapMs !== null &&
  row.lastLapMs <= row.bestLapMs;   // BUG: equal-to triggers "improving" beep
```

`lastLapMs <= bestLapMs` is true on the very first lap (where `lastLapMs === bestLapMs` because the first lap IS the best lap) and on a lap that ties the best. The improving beep should only fire when the last lap strictly beats the previous best, i.e. the best lap was already set before this lap. The correct test is `row.lastLapMs < row.bestLapMs` — using strict less-than. With `<=`, every first-completed lap triggers an unnecessary "improving" sound, which is audibly misleading to the race director.

**Fix:**
```tsx
// Before
const improving =
  row.lastLapMs !== null &&
  row.bestLapMs !== null &&
  row.lastLapMs <= row.bestLapMs;

// After — strict less-than: a lap only improves if it beats the existing best
const improving =
  row.lastLapMs !== null &&
  row.bestLapMs !== null &&
  row.lastLapMs < row.bestLapMs;
```

---

## Warnings

### WR-01: Help button is only rendered on mobile in AdminPanelLayout — desktop users cannot open help

**File:** `frontend/src/pages/admin/AdminPanelLayout.tsx:175-186`

**Issue:** The help `HelpCircle` button inside `AdminPanelLayout` is placed inside the `<header className="md:hidden ...">` mobile header block (line 165). On desktop (`md+`) the layout renders only the fixed sidebar (`<aside className="hidden md:flex ...">`), and the sidebar `SidebarContent` component contains no help button at all. As a result, desktop admin users who have help content registered (e.g. when viewing the Events List or Championship Detail pages) have no visible affordance to open the help sheet. The help sheet is wired and works — it is just inaccessible on the screen size where the admin panel is primarily used.

Compare `RaceControlLayout.tsx:66-77` and `RacerPortalLayout.tsx:48-58` — both layouts correctly add the help button to the always-visible header/nav, not inside a mobile-only section.

**Fix:** Add a help button to the desktop sidebar footer (inside `SidebarContent`, adjacent to the logout button), or add it to a persistent desktop toolbar area. At minimum, move the help button outside the `md:hidden` header:

```tsx
// In SidebarContent, add the help button next to logout:
const { helpContent, setIsOpen } = useHelp();  // expose from parent or call useHelp() here

// ... in the user+logout footer section:
{helpContent && (
  <Button variant="ghost" size="icon-sm" onClick={() => setIsOpen(true)} aria-label="Open help">
    <HelpCircle className="h-4 w-4" />
  </Button>
)}
```

Note: `SidebarContent` currently does not call `useHelp()` — the parent `AdminPanelLayout` would need to pass `setIsOpen` down, or `SidebarContent` would need its own `useHelp()` call.

### WR-02: TranspondersPage registers CarTransponderHelp — the wrong help panel for transponders

**File:** `frontend/src/pages/racer/TranspondersPage.tsx:4-11`

**Issue:** `TranspondersPage` imports and registers `CarTransponderHelp` as its help content. The `CarTransponderHelp` component's prose is written for the **Cars** page (it describes the Cars page, mentions "Add car", "Edit a car", and specifically says "Transponder numbers are registered separately via the Transponders page"). When a user on the Transponders page opens help they see car-focused instructions telling them to go elsewhere — the exact page they are already on. `CarsPage` (line 5 of `CarsPage.tsx`) correctly uses `CarTransponderHelp`. The help component would need either a transponder-specific variant or conditional content, but registering the same panel for both pages creates a content accuracy bug.

**Fix:** Create a `TransponderHelp` component with transponder-specific copy (how to add a transponder, uniqueness constraint, using a club transponder), or split the existing `CarTransponderHelp` into two separate components. Register the correct one in `TranspondersPage`.

### WR-03: Stub pages ship help content describing unimplemented UI — help will mislead users

**File:** `frontend/src/pages/racer/EntriesPage.tsx:13` and `frontend/src/pages/racer/TranspondersPage.tsx:13`

**Issue:** Both `EntriesPage` and `TranspondersPage` are confirmed stubs:
- `EntriesPage` renders `"Entries — coming in Plan 06."` 
- `TranspondersPage` renders `"Transponders — coming in Plan 06."`

Both pages still call `setHelpContent(...)` with fully-written help panels that describe UI controls (buttons, forms, entry submission flows) that do not exist yet. If a user on a stub page opens the help sheet, they receive instructions for interacting with a blank page. This is a user-experience defect — help content should only be registered when the described UI is actually rendered.

**Fix:** Remove or guard the `setHelpContent` calls in stub pages until the full page implementation lands:

```tsx
// EntriesPage.tsx — remove or comment out the useEffect until Plan 06 ships:
// useEffect(() => {
//   setHelpContent(<EventEntryHelp />);
//   return () => setHelpContent(null);
// }, [setHelpContent]);
```

### WR-04: SetupLayout registers help content but there is no mechanism to open the help sheet

**File:** `frontend/src/pages/setup/SetupLayout.tsx:138-141`

**Issue:** `SetupLayout` correctly calls `setHelpContent(<SetupWizardHelp />)` (lines 138-141). However, the layout renders no help trigger button — neither the desktop sidebar (`SidebarContent`) nor the mobile top bar contain a `HelpCircle` button or any way for the user to open the help sheet. `SetupLayout` does not even import `isOpen`/`setIsOpen` from `useHelp()` — only `setHelpContent` is destructured (line 136). The registered content sits in context but is completely inaccessible. Compare with `RacerPortalLayout` (lines 49-58) and `RaceControlLayout` (lines 66-77) which both conditionally render a help button when `helpContent` is non-null.

**Fix:** Add help button rendering to `SetupLayout`. The mobile header already exists at line 218. Add to both the mobile header and (if one is ever added) the desktop sidebar:

```tsx
// In SetupLayout, update destructuring:
const { setHelpContent, helpContent, isOpen, setIsOpen } = useHelp();

// In the mobile header (after the brand span):
{helpContent && (
  <Button
    variant="ghost"
    size="icon-sm"
    aria-label="Open help"
    onClick={() => setIsOpen(true)}
    className="ml-auto"
  >
    <HelpCircle className="h-4 w-4" />
  </Button>
)}

// Add the Sheet component to the layout JSX (before or after the mobile Sheet):
<Sheet open={isOpen} onOpenChange={setIsOpen}>
  <SheetContent side="right" className="w-96" showCloseButton>
    <SheetHeader>
      <SheetTitle>Help</SheetTitle>
      <SheetDescription>Page guide</SheetDescription>
    </SheetHeader>
    <div className="overflow-y-auto flex-1 px-6 pb-6">{helpContent}</div>
  </SheetContent>
</Sheet>
```

### WR-05: Print guide pages are unprotected routes — any unauthenticated visitor can access them

**File:** `frontend/src/App.tsx:145-147`

**Issue:** The three print guide routes are defined as plain, unprotected routes inside `RootLayout` (which wraps with `AuthProvider` and `HelpProvider` but no `ProtectedRoute`):

```tsx
{ path: '/print/meeting-guide', element: <MeetingGuidePage /> },
{ path: '/print/racer-guide', element: <RacerGuidePage /> },
{ path: '/print/admin-guide', element: <AdminGuidePage /> },
```

The `AdminGuidePage` in particular contains internal navigation paths (e.g. `/admin/club`, `/admin/racers`, `/admin/forwarder`), role definitions, and operational procedure details that are intended for staff. Exposing this to anonymous users is an information disclosure. While the data described is operational rather than sensitive PII, it exposes the internal URL structure, role model, and workflow to unauthenticated visitors. The Racer and Meeting guides contain information about how to register, which is intentionally public, but the Admin guide should require at minimum an authenticated session.

**Fix:** Protect `AdminGuidePage` behind `ProtectedRoute` with `roles={['ADMIN', 'RACE_DIRECTOR', 'REFEREE']}`. The racer and meeting guides can remain public, or be wrapped in a loose `ProtectedRoute` without role restriction:

```tsx
{ path: '/print/meeting-guide', element: <MeetingGuidePage /> },
{ path: '/print/racer-guide', element: <RacerGuidePage /> },
{
  path: '/print/admin-guide',
  element: (
    <ProtectedRoute roles={['ADMIN', 'RACE_DIRECTOR', 'REFEREE']}>
      <AdminGuidePage />
    </ProtectedRoute>
  ),
},
```

### WR-06: ChampionshipDetailPage - NaN id passed to hooks when URL param is missing or non-numeric

**File:** `frontend/src/pages/admin/championships/ChampionshipDetailPage.tsx:417-418`

**Issue:**
```tsx
const { id: idParam } = useParams<{ id: string }>();
const id = Number(idParam);
```

`Number(undefined)` returns `NaN`. If the route is somehow reached without the `:id` segment (which can happen via direct navigation or a router misconfiguration), `id` is `NaN` and is passed directly to `useChampionshipDetail(NaN)`, `useUpdateChampionship(NaN)`, `useRemoveChampionshipClass(NaN)`, etc. `NaN` coerces to the string `"NaN"` in URL template literals, producing API calls to e.g. `/api/championships/NaN/...`. The same pattern exists in `EventDetailPage.tsx:128-129`. Neither page guards against `NaN`.

**Fix:** Add a guard after parsing the id:

```tsx
const { id: idParam } = useParams<{ id: string }>();
const id = Number(idParam);

if (!id || isNaN(id)) {
  // Render a not-found state or navigate away
  return <Navigate to="/admin/championships" replace />;
}
```

### WR-07: DriverCombobox initial search state is stale when users list loads asynchronously

**File:** `frontend/src/pages/admin/championships/ChampionshipDetailPage.tsx:69-71`

**Issue:**
```tsx
const { data: users = [] } = useAdminUsersList();
const [search, setSearch] = useState(() => {
  const u = users.find(u => u.id === Number(value));
  return u ? `${u.firstName} ${u.lastName}` : '';
});
```

The `useState` initialiser runs only once on mount. At mount time, `users` is `[]` (the default before the query resolves), so `users.find(...)` always returns `undefined` and `search` is always initialised to `''` even when `value` is a valid pre-populated driver id. This means that when the `CreateExclusionDialog` is opened with an existing `driverId` value (e.g. after form re-population), the combobox shows an empty search input rather than the driver's name. The search display is only used for new exclusion forms (where `value` starts as `''`), so this is low-impact today — but when the form is ever pre-populated the bug will surface.

**Fix:** Drive the displayed value from `users` via a derived value rather than `useState`:

```tsx
const displayValue = useMemo(() => {
  if (!value) return '';
  const u = users.find(u => u.id === Number(value));
  return u ? `${u.firstName} ${u.lastName}` : '';
}, [value, users]);
```

Then use `displayValue` as the controlled input value instead of separate `search` state, or sync `search` with a `useEffect` when `users` loads and `value` is set.

---

## Info

### IN-01: TODO comment in EventListPage shipping in production code

**File:** `frontend/src/pages/admin/events/EventListPage.tsx:281`

**Issue:** A TODO comment is left in the JSX:

```tsx
{/* TODO Plan 06: add track select when tracks API is available */}
```

TODO comments in shipped code are a maintenance hazard and indicate known incomplete behaviour.

**Fix:** Remove or convert to a tracked issue. The feature is already planned — the comment adds no value at commit time.

### IN-02: `window.print()` called from a plain `<button>` element — inconsistent with design system

**File:** `frontend/src/pages/print/AdminGuidePage.tsx:335-340`, `frontend/src/pages/print/MeetingGuidePage.tsx:311-316`, `frontend/src/pages/print/RacerGuidePage.tsx:227-232`

**Issue:** All three print guide pages use a raw `<button>` element with hand-rolled Tailwind classes rather than the project's `Button` shadcn/ui component:

```tsx
<button
  onClick={() => window.print()}
  className="px-4 py-2 bg-primary text-primary-foreground rounded text-sm"
>
  Print / Save as PDF
</button>
```

This is inconsistent with every other interactive element in the codebase which uses `<Button>`. The styling is also slightly inconsistent with `Button`'s default variant (missing `font-medium`, `h-10`, focus-visible ring, etc.).

**Fix:** Replace with the `Button` component:

```tsx
import { Button } from '@/components/ui/button';

<Button onClick={() => window.print()} className="print:hidden">
  Print / Save as PDF
</Button>
```

Note: Also replace `className="print:hidden"` with the `print:hidden` utility on the wrapping `<div>` (already done for the outer `<div className="mt-8 print:hidden">`) — the button itself does not need the class if the container already hides it.

### IN-03: HelpContext test does not test `setHelpContent` or `setIsOpen` state changes

**File:** `frontend/src/context/HelpContext.test.tsx`

**Issue:** The test suite verifies initial state only (null content, false open) and the error boundary case. It does not test that calling `setHelpContent(...)` updates the context value, nor that `setIsOpen(true)` toggles the `isOpen` flag. These state transitions are the primary contract of the context. Without testing them, regressions in the setter functions would not be caught.

**Fix:** Add state-transition tests using `act` and `renderHook`:

```tsx
import { act } from '@testing-library/react';

it('setHelpContent updates helpContent', () => {
  const { result } = renderHook(() => useHelp(), {
    wrapper: ({ children }) => <HelpProvider>{children}</HelpProvider>,
  });
  act(() => {
    result.current.setHelpContent(<span>help</span>);
  });
  expect(result.current.helpContent).not.toBeNull();
});

it('setIsOpen toggles isOpen', () => {
  const { result } = renderHook(() => useHelp(), {
    wrapper: ({ children }) => <HelpProvider>{children}</HelpProvider>,
  });
  act(() => {
    result.current.setIsOpen(true);
  });
  expect(result.current.isOpen).toBe(true);
});
```

### IN-04: AdminGuidePage and print pages wrap in `<MemoryRouter>` in tests but pages contain no router-dependent code

**File:** `frontend/src/pages/print/AdminGuidePage.test.tsx:8`, `frontend/src/pages/print/MeetingGuidePage.test.tsx:8`, `frontend/src/pages/print/RacerGuidePage.test.tsx:8`

**Issue:** The three print page tests wrap in `<MemoryRouter>` but none of the three guide pages use `Link`, `NavLink`, `useNavigate`, or any other router hook. The wrapping is harmless but misleading — it implies router context is needed when it is not.

**Fix:** Remove the `MemoryRouter` wrapper from all three guide page tests. This also removes the need to import `MemoryRouter`.

### IN-05: SetupWizardHelp text says "Skip wizard" link is "only visible once setup is complete" — this does not match `SetupLayout` render logic

**File:** `frontend/src/help/SetupWizardHelp.tsx:22-26` and `frontend/src/pages/setup/SetupLayout.tsx:118-126`

**Issue:** The help text reads:

> "If you need to come back later, use the 'Skip wizard' link at the bottom of the sidebar (only visible once setup is complete)."

The actual render condition in `SetupLayout.tsx` (line 118) is:

```tsx
{clickable && (
  <div className="px-4 py-4">
    <Button ... asChild><Link to="/admin">Skip wizard</Link></Button>
  </div>
)}
```

Where `clickable = statusData?.setupComplete === true`. The wording "once setup is complete" accurately describes the condition. However, "come back later" and "if you need to come back later" is confusing — if setup is complete the wizard has already been finished and the user is re-visiting it. The help text implies the skip link is useful during an incomplete first run, but it is intentionally hidden then. A user in the middle of first-run setup who reads the help text expecting a "come back later" link will not find one. This is a content accuracy issue that could be confusing at a critical onboarding moment.

**Fix:** Update the help text to clarify the skip link is only present on re-entry:

```
If you are revisiting the wizard after completing initial setup, a "Skip wizard" 
link appears at the bottom of the sidebar to return directly to the Admin panel.
During first-run setup the link is hidden — all five steps must be completed first.
```

---

_Reviewed: 2026-05-15_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
