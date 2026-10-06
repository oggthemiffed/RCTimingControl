---
status: partial
phase: 07-results-championship
source: [07-VERIFICATION.md]
started: 2026-05-03T00:00:00Z
updated: 2026-05-03T00:00:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. Public results page — lap time expansion
expected: Navigate to /results/{a finished raceId} without logging in. Result table renders with driver positions, lap counts, and time totals. Clicking a row expands to show a Lap | Time | Pos table with formatted durations (e.g. 23.456s) or em-dash for legacy rows.
result: [pending]

### 2. Public championship standings page
expected: Navigate to /championships/{id} without logging in. Standings table renders with driver names, total points, and round scores. Dropped rounds show strikethrough styling.
result: [pending]

### 3. Racer portal results tab
expected: Log in as RACER, navigate to Results tab in racer portal. Results tab is visible with ri-trophy-line icon. Events the racer entered are listed as collapsible cards.
result: [pending]

### 4. Admin car tags toggle
expected: Log in as ADMIN, open Club settings, toggle 'Show car details in printed results'. Switch toggles and Save button enables. After save, car tags appear in subsequent result snapshots.
result: [pending]

## Summary

total: 4
passed: 0
issues: 0
pending: 4
skipped: 0
blocked: 0

## Gaps
