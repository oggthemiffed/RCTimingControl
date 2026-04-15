# RCTimingControl

## What This Is

A web-based RC club management and race timing system built to replace RCResults. It gives racers a self-service portal to manage their profiles, cars, and transponders, and lets them enter events online. Race officials run events and championships from a browser-based control client that connects to AMB/MyLaps decoders over TCP, with live results visible to anyone on the network.

## Core Value

Racers can enter events online and manage their own car/transponder details, while officials run a full race meeting from any Windows or Linux machine — with live timing fed directly from AMB/MyLaps hardware.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Racers can register and manage their profile, cars, and transponders via an online portal
- [ ] Racers can enter events and championships online
- [ ] Admins can create and configure events with multiple races and classes
- [ ] Admins can set up championships with configurable "best X from Y rounds" scoring (default 4 from 6)
- [ ] Race control client (browser-based) runs on Windows and Linux at the track
- [ ] Race control: start and stop races
- [ ] Race control: call the grid (show which cars are next on track)
- [ ] Race control: marshal laps (add/remove laps for on-track incidents)
- [ ] Race control: print or export race results at the venue
- [ ] Live lap timing received from AMB/MyLaps decoder via TCP
- [ ] Live timing display in the browser during a race
- [ ] Race results published and visible after each race
- [ ] Event schedule visible on the web
- [ ] Championship standings (points table) visible on the web

### Out of Scope

- Native mobile app — web is accessible on mobile devices; a dedicated app adds no value for v1
- Windows-only installer — must run cross-platform (Windows + Linux)
- Offline-only mode — system requires network connectivity at the venue

## Context

- **Replacing:** RCResults (rc-timing.com / rc-results.com) — Windows-only client, no racer self-service, no online entry
- **Timing hardware:** AMB/MyLaps transponder decoders, connected over TCP. The system must parse the MyLaps protocol from the decoder's TCP stream.
- **Club workflow:** Events consist of multiple races across classes. Championship series span multiple events, scored by configurable best-X-from-Y-rounds (e.g., best 4 of 6).
- **Users:** Two distinct roles — racers (self-service web portal) and officials/admins (event setup and race control client)
- **Deployment:** Java backend, browser-based frontend. Race control client must work on a venue laptop running Windows or Linux with no special client installation.

## Constraints

- **Tech Stack**: Java backend — club already has Java expertise and the directory convention is established
- **Frontend**: Browser-based for all interfaces (racer portal, admin, race control, results display) — no desktop app or native installer
- **Compatibility**: AMB/MyLaps TCP protocol — must implement or use existing protocol parsing for MyLaps decoder integration
- **Cross-platform**: Race control must work on both Windows and Linux without platform-specific setup

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Browser-based race control client | Avoids platform-specific desktop app; works on Windows and Linux from any browser | — Pending |
| AMB/MyLaps TCP integration | Club uses AMB/MyLaps hardware; must read their proprietary TCP protocol | — Pending |
| "Best X from Y" championship scoring | Club's primary format; configurable so other clubs can adapt | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-15 after initialization*
