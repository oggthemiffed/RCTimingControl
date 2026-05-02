---
phase: 07-results-championship
plan: "01"
subsystem: test-scaffold, frontend-components
tags: [wave-0, test-stubs, shadcn, collapsible, nyquist]
dependency_graph:
  requires: []
  provides:
    - Wave 0 test scaffold — four @Disabled stub test classes for Phase 7 plans
    - RacerResultHistoryQuery stub class (query.results package)
    - shadcn Collapsible component for expandable UI rows
  affects:
    - app/src/test (Phase 7 plans 03, 04, 05 enable these stubs)
    - frontend/src/components/ui (Plan 06 uses Collapsible for RacerEventHistoryCard)
tech_stack:
  added:
    - RacerResultHistoryQuery (stub, query.results package)
    - frontend/src/components/ui/collapsible.tsx (Radix UI Collapsible wrapper)
  patterns:
    - Class-level @Disabled with Assertions.fail() bodies (Phase 5 Wave 0 convention)
    - Radix UI primitive wrappers via shadcn component pattern
key_files:
  created:
    - app/src/test/java/dev/monkeypatch/rctiming/api/pub/PublicResultsControllerTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/pub/PublicChampionshipControllerTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQueryTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQueryTest.java
    - app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQuery.java
    - frontend/src/components/ui/collapsible.tsx
  modified: []
decisions:
  - "api.pub package used instead of api.public — 'public' is a Java reserved keyword and cannot appear as a package identifier component; equivalent directory path preserved"
  - "RacerResultHistoryQuery stub class created in main sources to satisfy compilation dependency from RacerResultHistoryQueryTest; stub will be fully implemented in 07-05-PLAN.md"
  - "shadcn CLI installed collapsible.tsx to wrong @/ alias path; file manually placed at frontend/src/components/ui/collapsible.tsx to match existing component conventions"
metrics:
  duration: "~10 minutes"
  completed: "2026-05-02"
  tasks_completed: 2
  files_created: 6
---

# Phase 07 Plan 01: Wave 0 Test Scaffold and Collapsible Component Summary

Wave 0 test scaffold: four @Disabled stub test classes installed in the correct packages, plus the shadcn Collapsible UI component added to the frontend registry before Plan 06 requires it.

## What Was Built

### Task 1: Four @Disabled stub test classes

Four integration test stub files were created following the Phase 5 Wave 0 convention (class-level `@Disabled` with `Assertions.fail()` bodies):

| File | Package | Plan that enables it | Requirements covered |
|------|---------|----------------------|---------------------|
| `PublicResultsControllerTest.java` | `api.pub` | 07-04 | RESULT-01, RESULT-05 |
| `PublicChampionshipControllerTest.java` | `api.pub` | 07-04 | CHAMP-05 |
| `ChampionshipStandingsQueryTest.java` | `query.championship` | 07-03 | CHAMP-05 (drop logic, bonuses) |
| `RacerResultHistoryQueryTest.java` | `query.results` | 07-05 | RESULT-03 |

Each class extends `AbstractIntegrationTest` and has test methods with `// TODO (07-XX-PLAN.md):` comments pointing to the plan that implements them.

A companion stub `RacerResultHistoryQuery.java` was created in the main sources (query.results package) to satisfy the compilation requirement — without this, `RacerResultHistoryQueryTest` would fail to compile since the autowired type doesn't exist yet.

### Task 2: shadcn Collapsible component

`frontend/src/components/ui/collapsible.tsx` was installed. The component wraps Radix UI primitives and exports:
- `Collapsible` (root — wraps `CollapsiblePrimitive.Root`)
- `CollapsibleTrigger` (wraps `CollapsiblePrimitive.CollapsibleTrigger`)
- `CollapsibleContent` (wraps `CollapsiblePrimitive.CollapsibleContent`)

Per the RESEARCH.md Pitfall 5 note in the plan: Collapsible is used for `RacerEventHistoryCard` `<div>` contexts, NOT for `<tr>` table rows (where Radix portals cause DOM issues).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Java reserved keyword in package name**
- **Found during:** Task 1
- **Issue:** Plan specified `package dev.monkeypatch.rctiming.api.public;` but `public` is a Java reserved keyword and cannot be used as a package identifier component — would cause compile failure
- **Fix:** Used `api.pub` as the package name; files placed in `app/src/test/java/dev/monkeypatch/rctiming/api/pub/`
- **Files modified:** Both `PublicResultsControllerTest.java` and `PublicChampionshipControllerTest.java`
- **Commit:** b238a37

**2. [Rule 2 - Missing critical functionality] RacerResultHistoryQuery stub class**
- **Found during:** Task 1
- **Issue:** `RacerResultHistoryQueryTest` autowires `RacerResultHistoryQuery` which doesn't exist yet (implemented in 07-05). Without the class, the test file would fail to compile.
- **Fix:** Created minimal `RacerResultHistoryQuery.java` stub in `query.results` package with DSLContext injection and `@SuppressWarnings("unused")` annotation consistent with other Phase 7 stubs
- **Files modified:** `app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQuery.java` (new)
- **Commit:** b238a37

**3. [Rule 1 - Bug] shadcn CLI alias resolution**
- **Found during:** Task 2
- **Issue:** `npx shadcn@latest add collapsible --yes` installed the file to `frontend/@/components/ui/collapsible.tsx` (literal `@` directory) instead of resolving the `@/*` → `./src/*` tsconfig alias. The CLI failed to follow the `tsconfig.json` project references to `tsconfig.app.json` where the paths are defined.
- **Fix:** Removed the incorrectly placed file; manually wrote the correct content to `frontend/src/components/ui/collapsible.tsx`
- **Files modified:** `frontend/src/components/ui/collapsible.tsx` (created at correct path)
- **Commit:** 2a17b17

## Known Stubs

| Stub | File | Reason |
|------|------|--------|
| `RacerResultHistoryQuery` (empty body, no query methods) | `app/src/main/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQuery.java` | Compilation stub; full implementation in 07-05-PLAN.md |

## Deferred Items

Pre-existing compile error in audio test files (out of scope for this plan):
- `app/src/test/java/dev/monkeypatch/rctiming/audio/PiperTtsClientTest.java:43` — `TtsProperties` called with 3 args; record now requires 4 (added `locales` field)
- `app/src/test/java/dev/monkeypatch/rctiming/audio/TtsClipServiceTest.java:31` — same issue

These errors pre-date Phase 7 and were introduced during Phase 06. Docker was unavailable in this execution environment, preventing full test suite verification. The stub test files' compilation is confirmed correct based on syntax and import review.

## Verification Status

- Docker unavailable in execution environment — full `./gradlew :app:test` could not run
- `./gradlew :app:compileJava` passes (main sources including new `RacerResultHistoryQuery` stub)
- Test file syntax and imports verified manually; all four files follow the established Phase 5 Wave 0 pattern exactly
- `frontend/src/components/ui/collapsible.tsx` exists and exports all three Radix primitives

## Self-Check: PASSED

**Files verified:**
- FOUND: `app/src/test/java/.../api/pub/PublicResultsControllerTest.java`
- FOUND: `app/src/test/java/.../api/pub/PublicChampionshipControllerTest.java`
- FOUND: `app/src/test/java/.../query/championship/ChampionshipStandingsQueryTest.java`
- FOUND: `app/src/test/java/.../query/results/RacerResultHistoryQueryTest.java`
- FOUND: `app/src/main/java/.../query/results/RacerResultHistoryQuery.java`
- FOUND: `frontend/src/components/ui/collapsible.tsx`

**Commits verified:**
- FOUND: b238a37 (Task 1 — test stubs)
- FOUND: 2a17b17 (Task 2 — collapsible component)
