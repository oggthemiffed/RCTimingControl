---
phase: 05
slug: live-timing-forwarder
created: 2026-04-26
status: pending
---

# Phase 5 UAT — Manual Verification Checklist

## Prerequisites

- App running locally: `./gradlew :app:bootRun`
- Simulator running: `./gradlew :forwarder:runSimulator`
- DB up: `docker compose up -d`
- Browser open at `http://localhost:8080`

---

## UAT-1 — Admin Token Generate & Revoke

**Requirement:** FORWARDER-05

1. Log in as a user with the `ADMIN` role
2. Navigate to **Admin Panel → Forwarder Token** (`/admin/forwarder`)
3. [ ] Page shows status: `NO TOKEN`
4. Click **Generate Token**
5. [ ] One-time reveal panel appears showing a long token string with a **Copy** button
6. Click **Copy** — paste into a text editor to confirm it copies
7. Click **Done**
8. [ ] Page now shows status: `ACTIVE` with a `generatedAt` timestamp
9. Click **Revoke**
10. [ ] Confirmation dialog appears
11. Confirm revoke
12. [ ] Status shows `REVOKED`
13. Click **Generate Token** again
14. [ ] New token issued, status returns to `ACTIVE`

**Result:** [/] Pass &nbsp; [ ] Fail &nbsp; [ ] N/A

---

## UAT-2 — Forwarder Status Bar Colours

**Requirement:** TIMING-02

1. Open the Race Control cockpit for any event
2. [ ] Status bar shows two pills: **DECODER** and **FORWARDER** — both red/grey (simulator not yet started)
3. Start the simulator: `./gradlew :forwarder:runSimulator` (with the API token from UAT-1 in `forwarder.properties`)
4. [ ] **FORWARDER** pill turns **green** once gRPC connection established
5. [ ] **DECODER** pill turns **green** once simulator begins emitting RC-4 PASSING records
6. Stop the simulator (`Ctrl+C`)
7. [ ] Within ~3 seconds (TCP close detected immediately; 8 s idle fallback), **DECODER** pill turns **red**
8. [ ] **FORWARDER** pill remains **green** (gRPC stream still open)
9. Restart simulator
10. [ ] Both pills return to **green** (auto-reconnect)

**Result:** [/] Pass &nbsp; [ ] Fail &nbsp; [ ] N/A — **needs re-test after fix**

**Fix applied 2026-04-26:** Decoder TCP state changes now propagated to server via new `ReportStatus` gRPC RPC. Idle timeout reduced 30 s → 8 s. TCP drop detected immediately; idle fallback within 8 s. ForwarderStatusPublisher now tracks decoder and forwarder states independently.

---

## UAT-3 — In-Race Unknown Transponder Linking

**Requirement:** TIMING-08

1. Create a race with at least 2 entries registered. Start the race (status: RUNNING)
2. Configure the simulator to emit a PASSING with a transponder number **not** assigned to any entry
3. [ ] An alert/banner appears in the cockpit: *"Unknown transponder: [number]"* with a **Link to entry** button
4. Click **Link to entry**
5. [ ] `UnknownTransponderLinkDialog` opens showing the transponder number (read-only)
6. Select an entry from the dropdown
7. Click **Link Entry**
8. [ ] Success toast appears: *"N laps credited retroactively"* (N ≥ 1)
9. [ ] Dialog closes
10. [ ] Positions panel updates to include the newly-linked entry
11. [ ] The alert for that transponder is no longer shown

**Result:** [ ] Pass &nbsp; [/] Fail &nbsp; [ ] N/A — **needs re-test after fix**

**Comments** WHile i have race running (see the seed data, with entries) when i new transponder is detected i dont have ANY entries to link it to.

Also if there is a live race running and i move to a race that has been run in the run order list then i loose all of the live results, moving back to the running race displays a message waiting for first transponder pick up. We should be showing all the laps that have been recorded since the starting of the race

**Fix applied 2026-04-27 (issue 1 — empty entries dropdown):** `GET /api/v1/race-control/races/{raceId}/entries` endpoint was missing. Added `RaceEntriesQuery` (jOOQ join of race_entries → entries → users → cars) and wired it into `TransponderLinkController`. Returns `[{ entryId, racerName, carNumber }]` for all entries in the race.

**Fix applied 2026-04-27 (issue 2 — live results lost on navigation):** `LiveTimingPanel` relied purely on STOMP push with no initial data fetch, so remounting after navigation started blank. Added `GET /api/v1/race-control/races/{raceId}/live-timing` which reads current in-memory `LiveRaceState` positions. Panel now fetches this snapshot on mount as seed data; STOMP updates replace it once the next passing arrives.


---

## UAT-4 — End-to-End: Simulator → gRPC → STOMP → Live Browser Updates

**Requirement:** TIMING-03 (full stack smoke test)

1. Ensure API token is generated (UAT-1) and placed in `forwarder/src/main/resources/forwarder.properties` as `forwarder.api-token=<token>`
2. Ensure `app.grpc.port=9090` in `application.properties` and `9090:9090` in `docker-compose.yml`
3. Start the app and simulator
4. Create an event with a race. Add entries with transponder numbers matching the simulator's generative output
5. Start the race in the Race Control cockpit
6. [ ] Laps begin appearing in the live timing panel within a few seconds of each simulated PASSING
7. [ ] Positions update correctly as more laps are counted (fastest laps first)
8. [ ] Gap to leader is displayed and updates dynamically
9. [ ] No duplicate passings appear for a single simulated record
10. Let the race run until auto-finish (if configured) or manually finish
11. [ ] Final results snapshot is persisted — navigate away and back; results still shown

**Result:** [ ] Pass &nbsp; [ ] Fail &nbsp; [ ] N/A

---

## Sign-Off

| Test | Result | Tester | Date |
|------|--------|--------|------|
| UAT-1 Admin Token | | | |
| UAT-2 Status Bar | | | |
| UAT-3 Transponder Linking | | | |
| UAT-4 End-to-End | | | |
