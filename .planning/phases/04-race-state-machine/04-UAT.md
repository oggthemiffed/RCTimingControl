---
status: complete
phase: 04-race-state-machine
source: 04-01-SUMMARY.md, 04-02-SUMMARY.md, 04-03-SUMMARY.md, 04-04-SUMMARY.md, 04-05-SUMMARY.md, 04-06-SUMMARY.md, 04-07-SUMMARY.md
started: 2026-04-25T00:00:00Z
updated: 2026-04-25T00:00:00Z
---

## Current Test

complete: true

## Tests

### 1. Cold Start Smoke Test
expected: Kill any running server/service. Clear ephemeral state (temp DBs, caches, lock files). Start the application from scratch (./gradlew :app:bootRun or equivalent). Server boots without errors, Flyway migrations V17/V18/V19 apply cleanly (check logs for "Successfully applied 3 migration(s)" or similar), and a basic API call (e.g. GET /api/v1/events or health check) returns a live response.
result: pass

### 2. Race Control cockpit loads
expected: Navigate to /race-control/event/{id} (with an event that has at least one round generated). Page renders with a run order sidebar on the left (w-56) and a main panel on the right. If a race is in PENDING state, the main panel shows a "Call Grid" button. The page is protected — unauthenticated access should redirect to login.
result: pass

### 3. Run order sidebar shows race sequence
expected: The run order sidebar lists all rounds/heats in sequenceInEvent + heatNumber order. Each item shows the round type (PRACTICE/QUALIFIER/FINAL), class name, and a status badge (LIVE/GRID/STOPPED/DONE/PENDING). Clicking a row in the sidebar selects that race and updates the main panel.
result: pass

### 4. Call Grid — PENDING → GRID transition
expected: With a race in PENDING state selected, click "Call Grid". The race transitions to GRID state. The main panel switches from showing the Call Grid button to showing the GridEditorPanel (which wraps PreRaceReadinessPanel). The pre-race readiness panel shows two columns: "Marshal Duty" (list of drivers with their marshal obligation count) and "Grid Call" (position-ordered list of cars to call to the grid). A "Start Race" button is visible.
result: pass

### 5. Marshal duty warning highlight
expected: In the PreRaceReadinessPanel / GridEditorPanel, a driver who has missed 2 or more marshal duties in this event should appear with a destructive/red highlight (e.g. red text or background). A driver with 0 or 1 missed duties should have no highlight (missed count hidden when 0).
result: skipped — no seed data with missed marshal duties to trigger the highlight

### 6. Start race — GRID → RUNNING transition
expected: Click "Start Race" in the GridEditorPanel. The race transitions to RUNNING state. The main panel switches to LiveTimingPanel showing a position table (with position, driver name, laps, last lap time, best lap, gap columns). "Stop Race" and "Abandon" buttons appear. The WebSocket status indicator dot is visible (shows connection state).
result: pass — state badge shows "LIVE" instead of "RUNNING" on the page; confirmed intentional, no fix needed

### 7. Live timing updates via WebSocket (dev synthetic)
expected: With a race RUNNING, POST to /api/v1/dev/race/{raceId}/synthetic-passing (dev profile only). The LiveTimingPanel in the browser updates — lap counts increment, lap times appear, position ordering changes — without a page refresh. The WebSocket status dot should show green/connected.
result: pass

### 8. Stop and resume race
expected: Click "Stop Race" while race is RUNNING. Race transitions to STOPPED. LiveTimingPanel remains visible (showing last known positions) and buttons change to "Resume" and "Abandon". Click "Resume" — race transitions back to RUNNING and the LiveTimingPanel becomes live again.
result: pass

### 9. Abandon race → FINISHED + result snapshot
expected: With a race in RUNNING or STOPPED state, click "Abandon". Race transitions to FINISHED. The main panel switches to FinishedPanel, which shows a result snapshot table (position, driver, laps, best lap, penalties applied). A "Print" link is visible that navigates to the print results page.
result: pass

### 10. Print results page
expected: Click the Print link from FinishedPanel (or navigate directly to /race-control/event/{id}/results/{raceId}). Page loads a print-friendly layout with: club branding header, a position table with final standings, and a browser "Print" button. Table rows show correct final positions accounting for any TIME penalties applied.
result: pass

### 11. Referee page loads with timing table and alerts
expected: Navigate to the Referee tab (/race-control/event/{id}/referee) with a race RUNNING. Page shows the same run order sidebar on the left and a RefereeTimingTable on the right fed by STOMP. A driver whose gap to the car ahead has closed by ≥500ms AND is within 3000ms should have a highlighted row (bg-chart-3/20). A driver who has completed fewer laps than the leader should show a "LAPPED" badge. "Raise Incident" and "Apply Penalty" buttons are visible.
result: pass

### 12. Apply LAP penalty — live re-broadcast
expected: Click "Apply Penalty" on the Referee page. Fill in the PenaltyDialog: select a driver (entryId), choose LAP type, enter a value (e.g. -1), enter a reason. Submit the form. The live timing table immediately updates — the penalised driver's lap count changes and positions re-sort. No page refresh needed.
result: pass

### 13. Raise incident report
expected: Click "Raise Incident" on the Referee page. Fill in the IncidentDialog: select a driver, choose an incident type, enter a description. Submit. The form closes without error. The incident is recorded (can verify via backend DB or API if needed, but at minimum no error toast/message appears).
result: pass

### 14. HTTP 409 on duplicate race state command
expected: With a race already in RUNNING state, attempt to POST /api/v1/race-control/race/{id}/start again (via Postman, curl, or dev tools). The API returns HTTP 409 Conflict (not 200 or 500). The race remains in RUNNING state — the duplicate command is rejected cleanly.
result: pass

## Summary

total: 14
passed: 13
issues: 0
pending: 0
skipped: 1
blocked: 0

## Gaps

- **Transponder must come from racer's registered pool (RACER domain gap):** Entry submission currently accepts any free-text transponder number. A car assigned to a racer should be required to have a transponder from that user's registered transponder pool, and entry should only allow selecting from those. Without this, real AMB hardware passings won't resolve correctly (`passing → transponder → entry → racer` lookup breaks if the snapshot doesn't match a registered number). Needs a requirement in the RACER domain — likely a new phase or backlog item.
