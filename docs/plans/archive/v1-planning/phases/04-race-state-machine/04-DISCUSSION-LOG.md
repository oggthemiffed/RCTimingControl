# Phase 4: Race Control - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-22
**Phase:** 04-race-state-machine
**Areas discussed:** Race control client layout, Phase 4 timing scope, Grid management UX, Round generator workflow, Marshal laps UX, Referee tools placement, PDF results export, Pre-race readiness display

---

## Race Control Client Layout

| Option | Description | Selected |
|--------|-------------|----------|
| Single-page cockpit | All key info on one non-scrolling page: run order panel, current race status + controls, marshal list, grid call. No tabs, no navigation — director never leaves this screen. | ✓ |
| Tabbed interface | Tabs across the top: Run Order / Current Race / Grid / Results. Race director switches tabs during the meeting. | |
| Separate pages via sidebar | Race control gets its own sidebar with separate pages for run order, races, grid management, etc. | |

**User's choice:** Single-page cockpit

---

| Option | Description | Selected |
|--------|-------------|----------|
| Left panel: run order, right panel: current race | Two-column layout. Left ~30%: scrollable run order list. Right ~70%: current race controls, marshal list, grid call, status. | ✓ |
| Top: run order strip, bottom: current race controls | Horizontal run order strip across the top, main area below for controls. | |
| You decide the exact panel layout | Use whatever arrangement works best. | |

**User's choice:** Left panel run order (~30%), right panel current race (~70%)

---

| Option | Description | Selected |
|--------|-------------|----------|
| Desktop-first, mobile acceptable but not priority | Optimise for laptop screen. Mobile works but no dedicated mobile layout. | |
| Full mobile parity (same as racer portal) | Apply the same bottom-nav mobile treatment as the racer portal. | ✓ |

**User's choice:** Full mobile parity — race director may need to start a race from a phone at trackside.

---

| Option | Description | Selected |
|--------|-------------|----------|
| Current race auto-selected from run order | System advances automatically. Race director taps Start in the right panel. | ✓ |
| Director explicitly selects from run order list | Director clicks a race in the left panel to make it active. | |

**User's choice:** Auto-selected from run order

---

| Option | Description | Selected |
|--------|-------------|----------|
| Director clicks any race in run order list to jump | Clicking a race in the left panel overrides auto-advance. Confirmation dialog for skipping forward. | ✓ |
| Separate 'Skip to race' button in the control panel | Dedicated button opens a picker. Run order list stays click-disabled during a race. | |

**User's choice:** Click run order list to jump, with confirmation dialog ("This will skip N races — continue?")

---

## Phase 4 Timing Scope

| Option | Description | Selected |
|--------|-------------|----------|
| Full WebSocket infrastructure + synthetic timing generator | Build STOMP WebSocket in Phase 4 with synthetic event generator. Race control fully testable without hardware. Phase 5 swaps generator for real decoder passings. | ✓ |
| State machine + HTTP only; timing display deferred to Phase 5 | Phase 4 delivers state machine commands via REST only. Timing display is a placeholder until Phase 5. | |

**User's choice:** Full WebSocket infrastructure + synthetic timing generator

---

| Option | Description | Selected |
|--------|-------------|----------|
| Admin UI button in the race control cockpit (dev/test only) | A 'Simulate Lap' button visible only in dev profile. Each click fires a synthetic passing event. | ✓ |
| REST endpoint only — no UI | POST /api/v1/dev/simulate-passing fires a synthetic event. No UI button. | |

**User's choice:** UI button in cockpit, dev profile only

---

| Option | Description | Selected |
|--------|-------------|----------|
| Implement OFFICIAL-01/02 in Phase 4 using synthetic data | Build proximity alerts and backmarker detection in Phase 4. Logic is the same regardless of data source. | ✓ |
| Defer OFFICIAL-01/02 to Phase 5 | Build these views in Phase 5 alongside the real timing stream. | |

**User's choice:** Implement in Phase 4 with synthetic data

---

## Grid Management UX

| Option | Description | Selected |
|--------|-------------|----------|
| Numbered input per driver row | Admin types grid position number per row. Table auto-reorders. Fast, works on any device. | ✓ |
| Drag-and-drop reordering | Drag drivers up/down to reorder. Complex to implement, awkward on touchscreens. | |
| You decide | Pick the approach that fits existing patterns best. | |

**User's choice:** Numbered input per driver row

---

| Option | Description | Selected |
|--------|-------------|----------|
| In the right panel when race is in PENDING state | Grid editor shown in cockpit right panel. Panel content is state-dependent. | ✓ |
| Separate grid management page under admin panel | Admin edits grids from admin panel; cockpit shows read-only grid. | |

**User's choice:** Cockpit right panel when race is PENDING

---

## Round Generator Workflow

| Option | Description | Selected |
|--------|-------------|----------|
| Multi-step wizard | Step 1: rounds + heat size. Step 2: per-class finals config. Step 3: run order preview. Confirm to generate. | ✓ |
| Single-form with preview | All inputs on one page with live preview panel. | |

**User's choice:** Multi-step wizard

---

| Option | Description | Selected |
|--------|-------------|----------|
| Type, round number, class, heat number, driver count | Summary row per race. Enough to validate structure. | |
| Full driver list per race | Each race row shows which drivers are in it. | ✓ |
| You decide the preview detail level | Whatever gives enough to validate structure. | |

**User's choice:** Full driver list per race in the preview

---

| Option | Description | Selected |
|--------|-------------|----------|
| On the event detail page in the admin panel | 'Generate Run Order' button on event detail page once entries close. | |
| In the race control cockpit, accessible to race directors | Round generator triggered from cockpit, not admin panel. | ✓ |

**User's choice:** In the race control cockpit. Race directors run this on race day.

---

| Option | Description | Selected |
|--------|-------------|----------|
| Shown in the cockpit left panel when no run order exists | "Event setup incomplete — generate run order" prompt. Once generated, shows normal run order list. | ✓ |
| Always-accessible from a settings/setup area in the cockpit | Dedicated setup section for round generation, re-accessible to regenerate. | |

**User's choice:** Cockpit left panel prompt when no run order exists yet

---

## Marshal Laps UX

| Option | Description | Selected |
|--------|-------------|----------|
| +/− buttons per driver row in the live timing panel | Each driver row has +1 and −1 buttons. Confirmation dialog per action. Positions update immediately. | ✓ |
| Dedicated marshal panel as a slide-over | 'Marshal Laps' button opens side panel with larger touch targets. | |

**User's choice:** +/− buttons per driver row

**Notes:** User was explicit about the audit trail requirements. Minimum required fields: driver, transponder number, adjustment timestamp, acting user (userId + display name). "This is the minimum and we can store any other relevant data in the audit."

---

| Option | Description | Selected |
|--------|-------------|----------|
| Scrollable audit log section in the right panel | Collapsible 'Marshal Adjustments' section below live timing table. Visible during and after the race. | ✓ |
| Accessible via a history button per driver | Info icon per driver row opens a popover with that driver's adjustment history. | |
| You decide placement | Wherever it fits best. | |

**User's choice:** Scrollable audit log section in right panel

---

## Referee Tools Placement

| Option | Description | Selected |
|--------|-------------|----------|
| Integrated into the race control cockpit, role-gated | Same cockpit for all officials. REFEREE role sees additional 'Incidents & Penalties' section. | |
| Separate /referee page | Referees have their own dedicated page. Can open on a second device. | ✓ |

**User's choice:** Separate /referee page

---

| Option | Description | Selected |
|--------|-------------|----------|
| Live timing + race steward views on the referee page | Full situational awareness: timing panel, proximity alerts, backmarker detection, incident/penalty tools. | ✓ |
| Penalty tools only — no timing feed | Referee page is just incident reporting and penalty application. | |

**User's choice:** Full race steward view with live timing

**Notes:** "Not only do referees add penalties — they are also responsible for informing drivers of faster approaching drivers, incidents on the track, and what actions to take to mitigate it. They need a good view of race positions, car details, and who is gaining on whom."

---

## PDF Results Export

| Option | Description | Selected |
|--------|-------------|----------|
| Available after each race finishes — button in cockpit right panel | 'Print Results' button once race is FINISHED. Opens print-ready page in new tab. Per-race throughout the meeting. | ✓ |
| Available at event level only — full event results after all races complete | PDF generated from admin panel once event is COMPLETED. | |

**User's choice:** Available after each race finishes, from cockpit right panel

---

| Option | Description | Selected |
|--------|-------------|----------|
| Standard: position, car number, driver name, laps, total time, best lap, gap to leader | Clean timing sheet. Club logo at top if configured. | ✓ + position chart |
| Minimal: position, driver name, laps/time only | Very simple sheet. | |
| Detailed: all laps listed per driver | Full lap-by-lap breakdown. | |

**User's choice:** Standard fields + position chart (driver positions by lap number)

**Notes:** "We should look to always include a position graph showing the drivers' position in the race against what lap it is — this is useful."

---

## Pre-Race Readiness Display

| Option | Description | Selected |
|--------|-------------|----------|
| Two-column: marshals needed (left) + next race grid call (right) | Left: drivers from finished race who must marshal. Right: drivers called to grid in order. | ✓ |
| Single list combining both | Sequential single column — marshal duties first, then grid call. | |

**User's choice:** Two-column layout

---

| Option | Description | Selected |
|--------|-------------|----------|
| Race director applies marshal penalties from the pre-race screen | Checkbox/button per marshal to mark absent. Optional penalty application. | ✓ |
| Referee applies marshal failure penalties via /referee page | Race director just starts the race. Referee handles all penalties. | |

**User's choice:** Race director marks absent (optional penalty) from pre-race screen

**Notes:** "It might be good to mark an absent marshal and 'optionally' allow a penalty — this is important in club racing where we might genuinely not want to always give penalties. It would also be good to record and show how many missed marshal duties a driver has had beside the marshal's name on the pre-race screen (e.g., David Anderson (missed 2 times this event)) — this lets us see and allows a better decision on penalties to be made."

---

## Claude's Discretion

- Exact STOMP topic paths for race control
- In-memory position calculation approach
- REST URL structure for race control endpoints
- Race control page route
- Position chart library for PDF results
- Confirmation dialog wording for state transitions
- Data model for optional marshal penalty (extension of MarshalAdjustment or separate record)

## Deferred Ideas

- Real AMB decoder integration — Phase 5
- Audio announcements — Phase 6
- Championship standings — Phase 7
- Full-event results PDF export — Phase 7 extension
