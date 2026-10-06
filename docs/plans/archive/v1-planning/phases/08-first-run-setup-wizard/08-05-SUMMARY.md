---
phase: 08-first-run-setup-wizard
plan: 05
subsystem: ui
tags: [react, typescript, vite, tanstack-query, zod, react-hook-form, shadcn, tailwind]

requires:
  - phase: 08-04
    provides: SetupLayout wizard shell, useSetupProgress hook, setupApi.ts

provides:
  - ClubProfileStep: Step 1 form (name, timezone, optional email/phone/websiteUrl)
  - TrackStep: Step 2 form with Skip/Back buttons
  - FormatStep: Step 3 form with Skip/Back buttons
  - StaffStep: Step 4 form with role checkboxes and 409 email collision handling
  - SetupLayout: updated with step state machine (steps 1-4 + sentinels 5/6), first-incomplete derivation via useEffect

affects:
  - 08-06 (DecoderConfigStep, SetupCompletePage — steps 5 and 6 are sentinels)

tech-stack:
  added: []
  patterns:
    - "Wizard step: Form/FormField/FormMessage with Zod + React Hook Form, onBlur validation"
    - "Step invalidation: always invalidate both setup-status and setup-progress on success"
    - "StaffStep role selector: Controller + Checkbox group (3 checkboxes) bound to array field"
    - "SetupLayout step machine: useEffect derives currentStep from progress; useState tracks live step"
    - "Re-entry mode: sidebar steps clickable when statusData.setupComplete === true"

key-files:
  created:
    - frontend/src/pages/setup/steps/ClubProfileStep.tsx
    - frontend/src/pages/setup/steps/TrackStep.tsx
    - frontend/src/pages/setup/steps/FormatStep.tsx
    - frontend/src/pages/setup/steps/StaffStep.tsx
  modified:
    - frontend/src/pages/setup/SetupLayout.tsx

key-decisions:
  - "FormatStep builds full RaceFormatConfig from type + durationMinutes using default values for other fields (simplest path for wizard)"
  - "StaffStep uses Controller + label/Checkbox pattern (not FormField) for the roles array to keep Checkbox change handlers clean"
  - "SidebarContent now accepts currentStep prop to correctly highlight active step (was previously deriving from progress alone)"
  - "StepItem converted from div to button element to support onClick for re-entry mode"

requirements-completed: [SC-2, SC-3]

duration: ~10min
completed: 2026-05-06
---

# Phase 8 Plan 05: Setup Wizard Steps 1-4 Summary

**Four wizard step forms (Club Profile, Track, Race Format, Staff Account) with React Hook Form + Zod + shadcn Form pattern; SetupLayout updated with first-incomplete step state machine**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-05-06T10:50:00Z
- **Completed:** 2026-05-06T11:00:00Z
- **Tasks:** 2 of 3 (Task 3 is checkpoint:human-verify — awaiting user verification)
- **Files modified:** 5

## Accomplishments

- **ClubProfileStep**: required name + timezone, optional email/phone/websiteUrl; shadcn Form/FormField/FormMessage; no Skip button (D-11); invalidates both query keys on success; "Manage more in Admin" link
- **TrackStep**: required name, optional lengthMeters + notes; Skip/Back buttons; same invalidation pattern; "Manage more in Admin" link
- **FormatStep**: required name + type (Select: Timed/Bump Up/Points Finals) + durationMinutes; Skip/Back buttons; builds full RaceFormatConfig with sensible defaults; "Manage more in Admin" link
- **StaffStep**: 5 user fields (firstName, lastName, email, password, confirmPassword) + role checkbox group (Admin/Race Director/Referee); `confirmPassword` refine; 409 → `form.setError('email', ...)` pattern; Skip/Back buttons
- **SetupLayout**: removed "Plans 05–06 will render" sentinel; added step state machine with `useState(currentStep)` + `useEffect` (derives from progress); `handleNext`/`handleBack`; step sentinels for steps 5 and 6 ("Decoder Config (Plan 06)" / "Setup Complete (Plan 06)"); sidebar StepItem clickable in re-entry mode

## Task Commits

1. **Task 1: ClubProfileStep + TrackStep** — `a1fcac2` (feat)
2. **Task 2: FormatStep + StaffStep + SetupLayout step orchestration** — `6cc78ba` (feat)
3. **Task 3: Visual verification** — CHECKPOINT (awaiting human-verify)

## Files Created/Modified

- `frontend/src/pages/setup/steps/ClubProfileStep.tsx` — Step 1 form (club name/timezone/contact fields)
- `frontend/src/pages/setup/steps/TrackStep.tsx` — Step 2 form with Skip (track name/length/notes)
- `frontend/src/pages/setup/steps/FormatStep.tsx` — Step 3 form with Skip (format name/type/duration)
- `frontend/src/pages/setup/steps/StaffStep.tsx` — Step 4 form with Skip (user fields + role checkboxes)
- `frontend/src/pages/setup/SetupLayout.tsx` — Step state machine, first-incomplete derivation, re-entry sidebar clicks

## Decisions Made

- FormatStep builds a full `RaceFormatConfig` from the selected type using default field values — avoids duplicating the full FormatConfigFields component for the wizard's simpler "create one format" use case
- StaffStep uses `Controller` from react-hook-form rather than `FormField` for the roles array, which keeps the checkbox `onCheckedChange` handlers clean and avoids array manipulation inside a render prop
- SidebarContent now receives `currentStep` as a prop from SetupLayout rather than deriving it purely from progress data — this allows the sidebar to show the correct "current" highlight even when user navigates via Back button (without invalidating progress)
- StepItem changed from `<div>` to `<button>` to correctly support keyboard navigation and onclick in re-entry mode

## Deviations from Plan

### Auto-fixed Issues

None. Plan executed as written. The format type enum values in the plan (`BUMP_UP_FINALS`) differed from the actual codebase values (`BUMP_UP`, `POINTS_FINALS`) — adjusted to match the codebase without counting as a deviation.

## Known Stubs

- `SetupLayout.tsx` step 5: `<div className="text-sm text-muted-foreground">Decoder Config (Plan 06)</div>` — intentional sentinel for Plan 06
- `SetupLayout.tsx` step 6: `<div className="text-sm text-muted-foreground">Setup Complete (Plan 06)</div>` — intentional sentinel for Plan 06

These stubs do not prevent the plan's goal (steps 1–4 fully functional). Plan 06 will replace them.

## Threat Flags

None — no new network endpoints or auth paths introduced. StaffStep mirrors T-08-15/T-08-16 mitigations from Plan 05 threat model:
- Frontend Zod schema constrains roles to `{ADMIN, RACE_DIRECTOR, REFEREE}` (T-08-15)
- 409 from email collision surfaced as `FormMessage` on the email field (T-08-16)

## Self-Check: PASSED

- frontend/src/pages/setup/steps/ClubProfileStep.tsx: FOUND
- frontend/src/pages/setup/steps/TrackStep.tsx: FOUND
- frontend/src/pages/setup/steps/FormatStep.tsx: FOUND
- frontend/src/pages/setup/steps/StaffStep.tsx: FOUND
- frontend/src/pages/setup/SetupLayout.tsx: FOUND (placeholder removed)
- Commit a1fcac2: FOUND (ClubProfileStep + TrackStep)
- Commit 6cc78ba: FOUND (FormatStep + StaffStep + SetupLayout)
- npm run build: PASSED (✓ built in 1.70s)
- npm test --run: PASSED (35 passed, 4 skipped)

---
*Phase: 08-first-run-setup-wizard*
*Completed: 2026-05-06*
