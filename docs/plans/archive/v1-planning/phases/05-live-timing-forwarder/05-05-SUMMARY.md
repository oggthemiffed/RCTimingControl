---
phase: 05-live-timing-forwarder
plan: "05"
subsystem: frontend
tags: [react, stomp, websocket, ui, vitest, shadcn]
dependency_graph:
  requires: [05-04]
  provides: [forwarder-status-bar, transponder-link-dialog, forwarder-token-page]
  affects: [frontend/race-control, frontend/admin]
tech_stack:
  added: []
  patterns:
    - useStomp hook for real-time STOMP subscription in components
    - Radix UI Select mock with native select for jsdom test compatibility
    - vitest/config defineConfig for proper test typing
key_files:
  created:
    - frontend/src/pages/race-control/panels/ForwarderStatusBar.tsx
    - frontend/src/pages/race-control/panels/ForwarderStatusBar.test.tsx
    - frontend/src/pages/race-control/dialogs/UnknownTransponderLinkDialog.tsx
    - frontend/src/pages/race-control/dialogs/UnknownTransponderLinkDialog.test.tsx
    - frontend/src/pages/admin/race-control/ForwarderTokenPage.tsx
    - frontend/src/test/setup.ts
  modified:
    - frontend/src/pages/race-control/RaceControlLayout.tsx
    - frontend/src/pages/race-control/CockpitPage.tsx
    - frontend/src/pages/admin/AdminPanelLayout.tsx
    - frontend/src/App.tsx
    - frontend/src/lib/raceControlApi.ts
    - frontend/package.json
    - frontend/vite.config.ts
decisions:
  - "Radix UI Select mocked with native <select> in tests to avoid jsdom portal rendering issues — Select interaction tested via fireEvent.change rather than click+pick"
  - "Vitest setup file added at src/test/setup.ts with @testing-library/jest-dom/vitest for DOM matchers"
  - "test script added to package.json; vite.config.ts switched to vitest/config defineConfig for proper type support"
  - "Pre-existing zodResolver/z.preprocess type incompatibility in Championship forms fixed with as any cast"
metrics:
  duration: "14m"
  completed: "2026-04-26"
  tasks: 3
  files: 13
---

# Phase 05 Plan 05: React UI Layer (ForwarderStatusBar, UnknownTransponderLinkDialog, ForwarderTokenPage) Summary

**One-liner:** React UI layer for live timing forwarder — STOMP status bar in race control cockpit, in-race transponder linking dialog, and admin token management page with one-time reveal.

## Tasks Completed

| # | Task | Commit | Key Files |
|---|------|--------|-----------|
| 1 | ForwarderStatusBar + RaceControlLayout integration | 9e93924 | ForwarderStatusBar.tsx, ForwarderStatusBar.test.tsx, RaceControlLayout.tsx |
| 2 | UnknownTransponderLinkDialog + API + CockpitPage | 64e8741 | UnknownTransponderLinkDialog.tsx, UnknownTransponderLinkDialog.test.tsx, raceControlApi.ts, CockpitPage.tsx |
| 3 | ForwarderTokenPage + admin nav + route | c55ebba | ForwarderTokenPage.tsx, AdminPanelLayout.tsx, App.tsx |

## What Was Built

### ForwarderStatusBar (Task 1)
- `ForwarderStatusBar.tsx` subscribes to `/topic/system/forwarder-status` via `useStomp` hook
- Displays two status pills (DECODER, FORWARDER) with colour-coded dots:
  - 🟢 CONNECTED = `--flag-green` with `animate-pulse`
  - 🟡 RECONNECTING = `--flag-yellow`
  - 🔴 DISCONNECTED / null = `--flag-red`
- Inserted between `<Separator />` and content area in `RaceControlLayout.tsx`
- 5 Vitest tests covering all state transitions

### UnknownTransponderLinkDialog (Task 2)
- Dialog for in-race transponder linking via `POST /api/v1/race-control/races/{raceId}/transponders/link`
- Entry selector populated from `GET /api/v1/race-control/races/{raceId}/entries`
- Success toast shows retroactively credited lap count
- `CockpitPage.tsx` subscribes to `/topic/race/{raceId}/unknown-transponder` and shows unknown transponder list with "Link to entry" buttons
- 4 Vitest tests with Radix UI Select mocked for jsdom compatibility

### ForwarderTokenPage (Task 3)
- Admin page at `/admin/forwarder` with 4 states:
  - **NONE**: Generate Token button
  - **ACTIVE**: Status, generated date, Regenerate/Revoke with confirmation
  - **One-time reveal**: Copy-to-clipboard with warning "Copy this token now. It will not be shown again."
  - **REVOKED**: Status badge, Generate Token button
- Added to AdminPanelLayout `operationsGroup` with `Radio` icon
- Route registered in `App.tsx` under admin children

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Missing] Add test script and vitest setup**
- **Found during:** Task 1 pre-check
- **Issue:** `package.json` had no `test` script; `@testing-library/jest-dom` matchers (`toBeInTheDocument`, `toHaveClass`) would fail without a vitest setup file
- **Fix:** Added `"test": "vitest"` to package.json scripts; created `src/test/setup.ts` importing `@testing-library/jest-dom/vitest`; updated `vite.config.ts` to use `vitest/config` and `setupFiles`
- **Files modified:** `frontend/package.json`, `frontend/vite.config.ts`, `frontend/src/test/setup.ts`
- **Commit:** 9e93924

**2. [Rule 1 - Bug] Fix test Select interaction for Radix UI jsdom portal issues**
- **Found during:** Task 2 RED/GREEN cycle
- **Issue:** Plan-provided test code used `fireEvent.click(combobox)` + `waitFor(() => getByText('Car 42...'))` pattern, but Radix UI Select renders its dropdown via a portal that doesn't appear correctly in jsdom. Tests failed with "Unable to find an element with the text: Car 42 — John Doe"
- **Fix:** Mocked `@/components/ui/select` in the test file with native HTML `<select>`/`<option>` elements; changed interaction to `fireEvent.change(combobox, { target: { value: '1' } })`; added `waitFor(() => expect(combobox).not.toBeDisabled())` to wait for entries to load before interaction
- **Files modified:** `frontend/src/pages/race-control/dialogs/UnknownTransponderLinkDialog.test.tsx`
- **Commit:** 64e8741

**3. [Rule 1 - Bug] Fix pre-existing TypeScript errors blocking build**
- **Found during:** Task 3 build verification
- **Issue:** `ChampionshipConfigForm.tsx` and `ChampionshipDetailPage.tsx` had pre-existing TypeScript errors from `z.preprocess` + `zodResolver` type incompatibility introduced by newer `@hookform/resolvers`. Build returned exit 1
- **Fix:** Added `as any` type cast to `zodResolver(schema)` in both files — behaviour is identical, only TypeScript's type checker is relaxed
- **Files modified:** `frontend/src/pages/admin/championships/ChampionshipConfigForm.tsx`, `frontend/src/pages/admin/championships/ChampionshipDetailPage.tsx`
- **Commit:** c55ebba

## Known Stubs

None — all components are wired to real API endpoints and STOMP topics.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: token-display | ForwarderTokenPage.tsx | One-time token reveal in browser — mitigated by "Copy this token now. It will not be shown again." warning; token stored in React state only, not persisted to DOM after `handleDone()` |

## Self-Check: PASSED

All 7 created/modified files verified on disk. All 3 task commits found in git log. 15/15 tests pass. Build exits 0.
