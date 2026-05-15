---
status: partial
phase: 09-user-manual-documentation
source: [09-VERIFICATION.md]
started: 2026-05-15T00:00:00.000Z
updated: 2026-05-15T00:00:00.000Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. Print guide visual rendering
expected: All three print guides render cleanly in browser at /print/meeting-guide, /print/racer-guide, /print/admin-guide without login. Ctrl+P produces clean output with no sidebar, no nav, no extraneous UI elements.
result: [pending]

### 2. Race control help drawer
expected: Logged in as race director, clicking '?' in the race control header opens a Sheet drawer with contextual race control help content. A "Open Race Meeting Guide (printable)" link appears at the bottom of the article.
result: [pending]

### 3. Racer portal help drawer (desktop)
expected: Logged in as a racer, clicking '?' in the desktop nav opens a Sheet drawer with racer-relevant help. A "Open Racer Quick-Start Guide (printable)" link appears at the bottom.
result: [pending]

### 4. Help content clears on navigation
expected: Navigating away from any page that registered help content (e.g., CockpitPage → RefereePage) clears the previous help and loads the new page's help content. No stale content persists in the drawer.
result: [pending]

## Summary

total: 4
passed: 0
issues: 0
pending: 4
skipped: 0
blocked: 0

## Gaps
