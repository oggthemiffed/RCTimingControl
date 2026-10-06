---
status: partial
phase: 10-docker-trial-environment
source: [10-VERIFICATION.md]
started: 2026-05-16T20:43:43Z
updated: 2026-05-16T20:43:43Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. SC-3 generative mode deviation acceptance
expected: User formally accepts that `--mode=generative` (continuous synthetic timing) satisfies SC-3 ("replays a recorded RC-4 passing file") — confirmed acceptable in RESEARCH.md but no formal override record exists yet. Accept by confirming live timing is visible in the race control UI.
result: [pending]

### 2. Live timing visible in race control UI
expected: With the full trial stack running (`docker compose -f docker-compose.trial.yml up`), the race control client at `http://localhost` shows live lap passings from the fake-decoder via the forwarder → gRPC → WebSocket pipeline. Transponder numbers 101-108 should appear as cars pass.
result: [pending]

### 3. Setup wizard accessible and functional
expected: Navigating to `http://localhost/setup` in the running trial stack renders the Phase 8 first-run setup wizard. The wizard steps complete successfully (club profile, tracks, decoder config).
result: [pending]

## Summary

total: 3
passed: 0
issues: 0
pending: 3
skipped: 0
blocked: 0

## Gaps
