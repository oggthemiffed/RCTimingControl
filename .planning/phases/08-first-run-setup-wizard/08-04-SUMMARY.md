---
phase: 08-first-run-setup-wizard
plan: 04
subsystem: ui
tags: [react, typescript, vite, tanstack-query, zod, react-hook-form, shadcn, tailwind, vitest]

requires:
  - phase: 08-02
    provides: SetupController, SetupService, bootstrap endpoint
  - phase: 08-03
    provides: decoder-config, staff, forwarder-config-download endpoints

provides:
  - setupApi.ts with getSetupStatus, getSetupProgress, bootstrap, updateDecoderConfig, createSetupStaff, downloadForwarderEnvUrl
  - useSetupStatus + useSetupProgress TanStack Query hooks
  - SetupGuard component with Pitfall 1 infinite-redirect protection
  - SetupLayout wizard shell with 5-step sidebar (AdminBootstrapGate pre-gate)
  - AdminBootstrapGate with bootstrap form using shadcn Form/FormField/FormMessage
  - AuthProvider.setAuthFromToken for programmatic JWT storage post-bootstrap
  - /setup route in App.tsx (unprotected, SetupLayout handles auth internally)
  - Setup Wizard nav entry in AdminPanelLayout linking to /setup (SC-5)

affects:
  - 08-05 (ClubProfileStep, TrackStep, FormatStep)
  - 08-06 (StaffStep, DecoderConfigStep)

tech-stack:
  added: []
  patterns:
    - "SetupGuard: guard component pattern from ProtectedRoute, inverted to redirect ON incomplete (not on unauthenticated)"
    - "AdminBootstrapGate: centred Card form with shadcn Form/FormField/FormMessage + Zod schema + axios error handling"
    - "SetupLayout: two-mode layout (pre-gate vs wizard sidebar) based on setupComplete + auth state"
    - "setAuthFromToken: new AuthProvider context method for storing JWT without navigating (bootstrap use case)"

key-files:
  created:
    - frontend/src/lib/setupApi.ts
    - frontend/src/hooks/setup/useSetupProgress.ts
    - frontend/src/pages/setup/SetupGuard.tsx
    - frontend/src/pages/setup/AdminBootstrapGate.tsx
    - frontend/src/pages/setup/SetupLayout.tsx
  modified:
    - frontend/src/App.tsx
    - frontend/src/pages/admin/AdminPanelLayout.tsx
    - frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx
    - frontend/src/pages/setup/__tests__/SetupGuard.test.tsx
    - frontend/src/providers/AuthProvider.tsx
    - frontend/src/pages/race-control/PrintResultsPage.tsx

key-decisions:
  - "SetupGuard wraps AuthProvider children in RootLayout so every navigation triggers the check (including after login/logout)"
  - "SetupLayout handles pre-gate vs wizard mode internally rather than two separate routes — keeps /setup as a single entry point"
  - "AuthProvider extended with setAuthFromToken to support programmatic JWT storage after bootstrap without page navigation"
  - "AdminBootstrapGate uses /setup route directly (not /login) — after bootstrap, URL stays at /setup and wizard sidebar appears"

requirements-completed: [SC-1, SC-2, SC-5]

duration: 15min
completed: 2026-05-06
---

# Phase 8 Plan 04: Setup Wizard Frontend Foundation Summary

**React setup wizard shell: SetupGuard redirect gate, AdminBootstrapGate centred card with Zod validation, SetupLayout wizard sidebar with 5-step state machine, and SC-5 re-entry nav entry in AdminPanelLayout**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-05-06T10:35:59Z
- **Completed:** 2026-05-06T10:50:00Z
- **Tasks:** 2 of 3 (Task 3 is checkpoint:human-verify)
- **Files modified:** 11

## Accomplishments

- SetupGuard with Pitfall 1 protection (`pathname.startsWith('/setup')` guard) — 4 tests pass
- AdminBootstrapGate: centred Card with "Set Up RC Timing" / "Create your admin account to get started." copy, 5 form fields (firstName, lastName, email, password, confirmPassword), shadcn Form/FormField/FormMessage, Zod validation
- SetupLayout: AdminBootstrapGate pre-gate when `setupComplete=false && !user`; wizard sidebar with 5 numbered steps (complete/current/incomplete states from useSetupProgress); Pitfall 5 redirect for non-admin post-setup; "Plans 05–06 will render the active step here." sentinel
- App.tsx: SetupGuard wraps Outlet inside AuthProvider; `/setup` added as unprotected top-level route
- AdminPanelLayout: Setup Wizard nav entry with Wand2 icon linking to /setup (SC-5)
- AdminPanelLayout test activated: 1 test passing
- AuthProvider extended with `setAuthFromToken` to store JWT programmatically after bootstrap

## Task Commits

1. **Task 1: setupApi + hooks + SetupGuard + activated SetupGuard tests** - `6c2d483` (feat)
2. **Task 2: SetupLayout + AdminBootstrapGate + App.tsx + AdminPanelLayout** - `11014bd` (feat)
3. **Task 3: Visual verification** - CHECKPOINT (awaiting human-verify)

## Files Created/Modified

- `frontend/src/lib/setupApi.ts` — Setup API: getSetupStatus, getSetupProgress, bootstrap, updateDecoderConfig, createSetupStaff, downloadForwarderEnvUrl
- `frontend/src/hooks/setup/useSetupProgress.ts` — useSetupStatus (staleTime 60s) + useSetupProgress (staleTime 0) hooks
- `frontend/src/pages/setup/SetupGuard.tsx` — Root redirect guard with Pitfall 1 protection
- `frontend/src/pages/setup/AdminBootstrapGate.tsx` — Pre-wizard admin account creation form
- `frontend/src/pages/setup/SetupLayout.tsx` — Wizard shell with sidebar and AdminBootstrapGate pre-gate
- `frontend/src/App.tsx` — Added SetupGuard wrapper and /setup route
- `frontend/src/pages/admin/AdminPanelLayout.tsx` — Added Setup Wizard nav entry (Wand2, /setup)
- `frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx` — Activated Wave-0 stub; 1 test passing
- `frontend/src/pages/setup/__tests__/SetupGuard.test.tsx` — Activated Wave-0 stub; 4 tests passing
- `frontend/src/providers/AuthProvider.tsx` — Added setAuthFromToken method + interface update
- `frontend/src/pages/race-control/PrintResultsPage.tsx` — Pre-existing null safety fix for data.clubBranding

## Decisions Made

- SetupGuard placed inside AuthProvider (not outside) so `useAuth()` resolves correctly in AdminBootstrapGate after bootstrap auto-login
- SetupLayout decides between pre-gate and wizard mode internally — single /setup route, no split routing
- `setAuthFromToken` added to AuthContext (not a standalone function) to keep auth state centralized in AuthProvider
- AdminBootstrapGate navigates to /setup after bootstrap (stays on wizard) rather than /admin — user completes wizard first

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added setAuthFromToken to AuthProvider**
- **Found during:** Task 2 (AdminBootstrapGate implementation)
- **Issue:** AuthProvider only exposed `login(email, password)` which makes a network call. Bootstrap response already contains the JWT — needed a way to store it without a second login call.
- **Fix:** Added `setAuthFromToken(token: string, authUser: AuthUser)` method to AuthContext, wires directly to internal setUser + setAccessToken state.
- **Files modified:** frontend/src/providers/AuthProvider.tsx
- **Committed in:** 11014bd (Task 2 commit)

**2. [Rule 1 - Bug] Fixed pre-existing null safety error in PrintResultsPage.tsx**
- **Found during:** Task 2 build verification
- **Issue:** `data.clubBranding` is typed as `ClubBrandingDto | null` but was accessed without null check on lines 49, 53, 55, 56 — TypeScript error prevented `npm run build` from passing (plan acceptance criterion)
- **Fix:** Changed `data.clubBranding.clubName` to `data.clubBranding?.clubName` (and similarly for logoUrl and alt text)
- **Files modified:** frontend/src/pages/race-control/PrintResultsPage.tsx
- **Committed in:** 11014bd (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (1 missing critical, 1 bug fix)
**Impact on plan:** Both fixes essential for correct operation. No scope creep.

## Issues Encountered

None beyond the deviations documented above.

## Known Stubs

- `SetupLayout.tsx`: `<div>Plans 05–06 will render the active step here.</div>` — intentional sentinel text. Plans 05–06 will add child routes and step components. The Outlet is already wired.

## Threat Flags

None — no new network endpoints, auth paths, file access patterns, or schema changes introduced. All security controls are client-side UI guards that mirror server-enforced auth.

## Next Phase Readiness

- Plans 05–06 can immediately add child routes under /setup and step components
- useSetupProgress hook provides all progress state needed for step indicators
- setupApi.ts exports all needed mutations (updateDecoderConfig, createSetupStaff)
- The wizard shell is fully functional; only step content is missing

## Self-Check: PASSED

- frontend/src/lib/setupApi.ts: FOUND
- frontend/src/hooks/setup/useSetupProgress.ts: FOUND
- frontend/src/pages/setup/SetupGuard.tsx: FOUND
- frontend/src/pages/setup/AdminBootstrapGate.tsx: FOUND
- frontend/src/pages/setup/SetupLayout.tsx: FOUND
- Commit 6c2d483: FOUND
- Commit 11014bd: FOUND
- 5 tests passing (4 SetupGuard + 1 AdminPanelLayout)
- npm run build: PASSED

---
*Phase: 08-first-run-setup-wizard*
*Completed: 2026-05-06*
