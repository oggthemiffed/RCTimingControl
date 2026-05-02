---
phase: 07-results-championship
plan: "07"
subsystem: results
tags: [results, lap-times, snapshot, gap-closure, RESULT-05]
dependency_graph:
  requires: [07-01, 07-02, 07-03, 07-04, 07-05, 07-06]
  provides: [per-lap-duration-display, RESULT-05]
  affects: [LiveRacePosition, LiveRaceState, ResultSnapshotDto, ResultSnapshotService, raceControlApi, PublicResultsPage]
tech_stack:
  added: []
  patterns: [nullable-Long-for-JSONB-backward-compat, per-entry-lap-index-counter]
key_files:
  created: []
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/timing/LiveRacePosition.java
    - app/src/main/java/dev/monkeypatch/rctiming/timing/LiveRaceState.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/dto/ResultSnapshotDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java
    - frontend/src/lib/raceControlApi.ts
    - frontend/src/pages/results/PublicResultsPage.tsx
decisions:
  - "Used Long (boxed nullable) not long (primitive) for lapTimeMs in PositionAtLap so legacy JSONB rows without the field deserialize as null rather than throwing"
  - "Lap index counter (lapIndex.merge) tracks per-entry position in lapTimes list during buildLapHistory iteration, handles multi-car scenarios correctly"
  - "getLapTimes() returns live list reference (no defensive copy) because mutations only occur under LiveRaceState synchronized methods, and reads happen after race FINISHED"
  - "Added pos.getLapTimes().add(lapMs) at both accumulateLap call sites: main applyLapPassing path and transponder-link replay path (applyPositionUpdate)"
metrics:
  duration: ~15 minutes
  completed: "2026-05-02"
  tasks: 6
  files_changed: 6
---

# Phase 07 Plan 07: Per-Lap Duration Display (RESULT-05 Gap Closure) Summary

Per-lap duration display for the public results page using ordered lap-time tracking in the live race position model, serialized into result snapshots and rendered as a Lap | Time | Pos table.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Add ordered lap-time list to LiveRacePosition | 7ba770d | LiveRacePosition.java |
| 2 | Record each lap time in LiveRaceState + getPositionSnapshot | b1b6ed4 | LiveRaceState.java |
| 3 | Add lapTimeMs to PositionAtLap record | fc03d44 | ResultSnapshotDto.java |
| 4 | Populate lapTimeMs in ResultSnapshotService.buildLapHistory | 64c69ad | ResultSnapshotService.java |
| 5 | Add lapTimeMs to frontend PositionAtLap type | 1fca00c | raceControlApi.ts |
| 6 | Render lap durations in LapTimesPanel | 667e312, da8f084 | PublicResultsPage.tsx |

## What Was Built

### Backend changes

**LiveRacePosition** gained a `private final List<Long> lapTimes = new ArrayList<>()` field with a `getLapTimes()` getter that returns the live list reference. No setter was added.

**LiveRaceState** now appends `pos.getLapTimes().add(lapMs)` after every `pos.accumulateLap(lapMs)` call — both in the primary `applyLapPassing` path and the transponder-link replay path `applyPositionUpdate`. A new `public synchronized LiveRacePosition getPositionSnapshot(long entryId)` method exposes position data for snapshot building without mutation.

**ResultSnapshotDto.PositionAtLap** was extended from 3-arg `(int lapNumber, long entryId, int position)` to 4-arg `(int lapNumber, long entryId, int position, Long lapTimeMs)`. `Long` (boxed) ensures existing JSONB rows missing this field deserialize as `null` via Jackson record deserialization.

**ResultSnapshotService.buildLapHistory** was updated to accept a nullable `LiveRaceState state` parameter. It uses a per-entry `lapIndex` counter (`HashMap<Long, Integer>`) to track which index into `pos.getLapTimes()` corresponds to each lap iteration. The 4-arg `PositionAtLap` constructor is called with the resolved `lapTimeMs` or `null` when state/position is unavailable.

### Frontend changes

**raceControlApi.ts** — `PositionAtLap` type extended with `lapTimeMs?: number | null` (optional and nullable to handle legacy snapshots vs new null-valued entries).

**PublicResultsPage.tsx** — `LapTimesPanel` now renders a 3-column table with `<thead>` (Lap | Time | Pos) and per-row format `Lap N | 23.456s | P2`. A `formatLapTime` helper converts milliseconds to `X.XXXs` or returns `'—'` (em dash) for null/undefined lapTimeMs. The `data-ms` attribute on the time cell exposes the raw millisecond value.

## Backward Compatibility

Legacy result snapshots stored before this change (JSONB without `lapTimeMs` field) continue to load correctly:
- Jackson deserializes missing fields as `null` for boxed `Long` record components
- The frontend `lapTimeMs?: number | null` type accepts absent, null, or numeric values
- `formatLapTime` renders `'—'` for null/undefined — no runtime errors

## RESULT-05 Gap Closure Confirmation

The gap identified in `07-VERIFICATION.md` is now closed:
- **Gap:** `ResultSnapshotDto.PositionAtLap` only carried `(lapNumber, entryId, position)` and `LapTimesPanel` showed position labels `P{position}` instead of durations
- **Resolution:** `PositionAtLap` now carries `lapTimeMs`, populated from `LiveRaceState.getPositionSnapshot(entryId).getLapTimes().get(idx)`, and `LapTimesPanel` renders `23.456s` formatted durations with graceful `—` fallback

## Deviations from Plan

### Auto-fixed Issues

None — plan executed exactly as specified.

### Notes

- Backend tests: pre-existing test compilation failures in `audio/PiperTtsClientTest.java` and `audio/TtsClipServiceTest.java` (`TtsProperties` 3-arg vs 4-arg constructor mismatch) blocked `./gradlew :app:test` from compiling. These failures predate this plan (confirmed by checking the base commit) and are out of scope. Backend main sources compile cleanly. Frontend TypeScript type-checks and builds successfully.
- jOOQ generated sources were not present in the worktree (Docker unavailable). They were copied from the main repo's build directory to enable compilation.
- `node_modules` were absent from the worktree frontend directory. A symlink to the main repo's `frontend/node_modules` was created for TypeScript and build verification.

## Self-Check

### Created files exist:
- `.planning/phases/07-results-championship/07-07-SUMMARY.md` — this file

### Modified files verified:
- `app/src/main/java/dev/monkeypatch/rctiming/timing/LiveRacePosition.java` — contains `List<Long> lapTimes` and `getLapTimes()`
- `app/src/main/java/dev/monkeypatch/rctiming/timing/LiveRaceState.java` — 2x `getLapTimes().add(lapMs)`, `getPositionSnapshot`
- `app/src/main/java/dev/monkeypatch/rctiming/api/racecontrol/dto/ResultSnapshotDto.java` — `Long lapTimeMs`
- `app/src/main/java/dev/monkeypatch/rctiming/service/ResultSnapshotService.java` — 4-arg `PositionAtLap` constructor
- `frontend/src/lib/raceControlApi.ts` — `lapTimeMs?: number | null`
- `frontend/src/pages/results/PublicResultsPage.tsx` — `formatLapTime`, `<thead>`, em-dash fallback

### Commits exist:
- 7ba770d feat(07-07): add ordered lap-time list to LiveRacePosition
- b1b6ed4 feat(07-07): record per-lap durations in LiveRaceState and add getPositionSnapshot
- fc03d44 feat(07-07): add nullable Long lapTimeMs to PositionAtLap record
- 64c69ad feat(07-07): populate lapTimeMs in buildLapHistory from LiveRaceState
- 1fca00c feat(07-07): add optional lapTimeMs to frontend PositionAtLap type
- 667e312 feat(07-07): render per-lap durations in LapTimesPanel (RESULT-05)
- da8f084 fix(07-07): split lapTimeMs attribute across lines for grep clarity

## Self-Check: PASSED
