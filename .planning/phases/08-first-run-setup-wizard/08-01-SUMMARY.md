---
phase: 08-first-run-setup-wizard
plan: "01"
subsystem: test-scaffolding
tags:
  - setup-wizard
  - testing
  - wave-0
dependency_graph:
  requires: []
  provides:
    - Wave-0 test stub files for SetupController (SC-1 through SC-4)
    - Wave-0 test stub files for SetupService (bootstrap replay guard)
    - Wave-0 test stub file for V25 migration smoke test
    - Wave-0 frontend test stubs for SetupGuard, DecoderConfigStep, AdminPanelLayout
  affects:
    - Plans 02-06 (downstream plans enable stubs by removing @Disabled / describe.skip)
tech_stack:
  added: []
  patterns:
    - Class-level @Disabled with Assertions.fail() bodies for JUnit 5 stubs
    - describe.skip with expect.fail() bodies for Vitest stubs
key_files:
  created:
    - app/src/test/java/dev/monkeypatch/rctiming/api/setup/SetupControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/setup/SetupServiceTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/MigrationIntegrationTest.java
    - frontend/src/pages/setup/__tests__/SetupGuard.test.tsx
    - frontend/src/pages/setup/__tests__/DecoderConfigStep.test.tsx
    - frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx
  modified: []
decisions:
  - Wave-0 stubs use class-level @Disabled (backend) and describe.skip (frontend) so builds stay green while giving downstream plans concrete files to enable
metrics:
  duration: "~5 minutes"
  completed: "2026-05-05T14:50:43Z"
  tasks_completed: 2
  files_created: 6
  files_modified: 0
---

# Phase 8 Plan 01: Wave-0 Test Scaffolding Summary

**One-liner:** Six Wave-0 test stub files (3 backend, 3 frontend) using class-level @Disabled and describe.skip so downstream plans have concrete targets to enable without file creation overhead.

## Tasks Completed

| Task | Name | Commit | Key Files |
|------|------|--------|-----------|
| 1 | Backend test stub files | 89e4af9 | SetupControllerIT.java, SetupServiceTest.java, MigrationIntegrationTest.java |
| 2 | Frontend test stub files | 26781b0 | SetupGuard.test.tsx, DecoderConfigStep.test.tsx, AdminPanelLayout.test.tsx |

## What Was Built

### Backend Test Stubs (Task 1)

**SetupControllerIT.java** — extends `AbstractIntegrationTest`, class-level `@Disabled("Wave 0 stub — enabled in Plan 02 (Setup Backend)")`. Contains 7 `@Test` stubs covering all SC-1 through SC-4 behaviors plus T-08-03:

- `getStatus_returnsSetupComplete_false_whenNoClub()` (SC-1)
- `getStatus_returnsSetupComplete_true_afterClubSaved()` (SC-1)
- `bootstrap_createsAdminUserAndReturnsToken()` (SC-2)
- `bootstrap_returns409_whenUsersExist()` (SC-2 / T-08-01 replay guard)
- `getProgress_reflectsDataState()` (SC-3)
- `downloadForwarderConfig_returnsEnvAttachment()` (SC-4)
- `downloadForwarderConfig_includesTokenPlaceholder_notPlaintext()` (T-08-03)

**SetupServiceTest.java** — plain JUnit 5 unit test (no Spring), class-level `@Disabled`. Contains 4 `@Test` stubs for bootstrap guard and progress logic.

**MigrationIntegrationTest.java** — extends `AbstractIntegrationTest`, class-level `@Disabled`. Single stub for V25 migration smoke test.

### Frontend Test Stubs (Task 2)

**SetupGuard.test.tsx** — `describe.skip` with 4 `it` stubs covering redirect behavior and Pitfall 1 (infinite redirect prevention).

**DecoderConfigStep.test.tsx** — `describe.skip` with 4 `it` stubs covering polling behavior (D-17: 30s timeout), CONNECTED badge, and download button disabled state.

**AdminPanelLayout.test.tsx** — `describe.skip` with 1 `it` stub covering SC-5 nav entry.

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

All 6 files are intentional stubs. They are Wave-0 placeholders — the entire purpose of this plan is to create them. Downstream plans 02–06 will enable them by removing `@Disabled` / `describe.skip` and implementing the actual test bodies.

## Threat Flags

None — this plan creates test code only. No production attack surface was added.

## Self-Check: PASSED

Files exist at expected paths:
- app/src/test/java/dev/monkeypatch/rctiming/api/setup/SetupControllerIT.java — FOUND
- app/src/test/java/dev/monkeypatch/rctiming/api/setup/SetupServiceTest.java — FOUND
- app/src/test/java/dev/monkeypatch/rctiming/MigrationIntegrationTest.java — FOUND
- frontend/src/pages/setup/__tests__/SetupGuard.test.tsx — FOUND
- frontend/src/pages/setup/__tests__/DecoderConfigStep.test.tsx — FOUND
- frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx — FOUND

Commits exist:
- 89e4af9 — test(08-01): add Wave-0 backend test stubs for setup wizard
- 26781b0 — test(08-01): add Wave-0 frontend test stubs for setup wizard

Content verified:
- @Disabled count in SetupControllerIT.java: 1
- @Disabled count in SetupServiceTest.java: 1
- describe.skip count in SetupGuard.test.tsx: 1
- describe.skip count in DecoderConfigStep.test.tsx: 1
- describe.skip count in AdminPanelLayout.test.tsx: 1
- "Pitfall 1: infinite redirect" present in SetupGuard.test.tsx: yes
- "30s timeout per D-17" present in DecoderConfigStep.test.tsx: yes
