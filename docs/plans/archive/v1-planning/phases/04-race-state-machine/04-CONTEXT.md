# Phase 4: Race Control - Context

**Gathered:** 2026-04-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 4 delivers the race control client and the server-side race state machine. It covers:
- New entities: Round, Race, RaceEntry (plus EventClass finals config fields) — per the heat structure spec
- Round generator: admin/director wizard to create all Round and Race records after entries close
- Race state machine: PENDING → GRID → RUNNING → STOPPED/FINISHED (with abandon and skip)
- Race control browser client: single-page cockpit for race directors
- STOMP WebSocket infrastructure with synthetic timing generator (feeds Phase 5's decoder passings)
- Marshal laps with mandatory audit trail
- Pre-race readiness display: marshal duty list + grid call
- /referee page: race steward view with live positions, proximity alerts, backmarker detection, incident reports, and lap/time penalties
- Per-race PDF/printable results with position chart
- OFFICIAL-01/02/03/04 and CTRL-01 through CTRL-09 requirements

Phase 5 swaps the synthetic timing generator for real AMB decoder passings via gRPC. The WebSocket topology built in Phase 4 does not change.

</domain>

<decisions>
## Implementation Decisions

### Race Control Client Layout
- **D-01:** Single-page cockpit — no tabs, no navigation. Race director stays on one non-scrolling screen all day. All key information is visible simultaneously.
- **D-02:** Two-column layout: left panel (~30%) shows the scrollable run order list; right panel (~70%) shows the active content for the current race state (pre-race readiness / live timing / post-race results). Current race is highlighted in the run order.
- **D-03:** Full mobile parity — same approach as the racer portal (top nav on `md+` breakpoint, bottom nav bar on mobile). Race director may need to start a race from a phone at trackside.
- **D-04:** Current race is always auto-selected from the run order — the system advances through races automatically. Race director taps Start in the right panel; no manual selection required for the normal flow.
- **D-05:** CTRL-09 skip/re-run: clicking any race in the left run order list overrides auto-advance and makes it the active race. A confirmation dialog fires for skipping forward: "This will skip N races — continue?" No separate skip button needed.

### Phase 4 Timing Scope
- **D-06:** Build the full STOMP WebSocket infrastructure in Phase 4, fed by a synthetic timing generator. Race control is fully testable without hardware. Phase 5 plugs in real decoder passings without changing the WebSocket topology.
- **D-07:** Synthetic timing generator: a UI button visible only in the `dev` Spring profile, in the cockpit right panel. Each click fires a synthetic `LapPassingEvent` for a random entry in the current race. Keeps the prod cockpit clean; dev/UAT can simulate laps on demand.
- **D-08:** OFFICIAL-01 (proximity alerts) and OFFICIAL-02 (backmarker detection) are implemented in Phase 4 using synthetic timing data. The alert logic is the same regardless of data source.

### Grid Management UX
- **D-09:** Numbered input per driver row — admin types the new grid position; the table auto-reorders after each change. Fast, works well on any device. No drag-and-drop.
- **D-10:** Grid management lives in the cockpit right panel when the current race is in `PENDING` state. The right panel content is state-dependent: PENDING = grid editor; GRID/RUNNING = live timing; STOPPED/FINISHED = results + audit.

### Round Generator Workflow
- **D-11:** Multi-step wizard in the race control cockpit (not the admin panel). Accessible to RACE_DIRECTOR role. On race day, the director runs this step.
- **D-12:** The wizard appears in the cockpit left panel when no run order has been generated yet: an "Event setup incomplete — generate run order" prompt with a launch button. Once generated, the normal run order list replaces it.
- **D-13:** Wizard steps: (1) Practice rounds count + qualifying rounds count + max cars per heat. (2) Per-class finals config — `finalsCount`, `carsPerFinal`, `bumpCount`, pre-populated from EventClass settings. (3) Full preview: each race row shows type, round number, class name, heat number, and the full driver list for that race. Admin confirms to generate all Round and Race records.

### Marshal Laps UX
- **D-14:** +1/−1 buttons per driver row in the live timing panel. Confirmation dialog on each action: "Add 1 lap to [Driver Name]?" Positions recalculate immediately on confirmation.
- **D-15:** **Mandatory audit trail fields** (minimum required): driver, transponder number, adjustment timestamp, acting user (userId + display name). Store race ID, race state at time of adjustment, and adjusted lap delta as additional fields.
- **D-16:** Marshal adjustments audit: a collapsible "Marshal Adjustments" section below the live timing table in the right panel, showing all adjustments in chronological order. Visible during and after the race.

### Pre-Race Readiness Display
- **D-20:** Two-column pre-race readiness view shown in the right panel between races (after a race finishes, before the next one starts): left column = marshal duty list (drivers from the just-finished race), right column = grid call (drivers due on track for the next race, in grid order).
- **D-21:** Each marshal row shows driver name + **cumulative missed marshal count for this event** — e.g., "David Anderson – missed 2 this event". This informs the penalty decision.
- **D-22:** Race director can mark a marshal as absent without automatically applying a penalty. Absence is recorded (tracked toward D-21 count). Applying a penalty is a separate optional action. This supports club racing where organisers may choose not to penalise every absence.

### Referee Tools
- **D-17:** Separate `/race-control/referee` page accessible to REFEREE role users. A referee may operate from a dedicated second device while the race director runs the main cockpit.
- **D-18:** The referee page is the full race steward view: live timing panel (positions, gaps, car details), proximity alerts (OFFICIAL-01 — cars closing on others), backmarker detection (OFFICIAL-02 — lapped cars approaching leaders), incident report form (OFFICIAL-03), and lap/time penalty application (OFFICIAL-04). Referees need full situational awareness to inform drivers of approaching cars and track incidents.

### PDF Results Export
- **D-19:** "Print Results" button appears in the cockpit right panel once a race reaches FINISHED state. Opens a print-ready page in a new browser tab. Available per-race throughout the meeting (race directors don't wait until the event is over).
- **D-23:** Results sheet content: position, car number, driver name, laps completed, total time, best lap, gap to leader + a **position chart** (driver positions plotted by lap number — shows how positions changed during the race). Club logo at top if club profile has a logo configured.

### Claude's Discretion
- Exact STOMP topic paths for race control (extend the pattern from CLAUDE.md: `/topic/race/{raceId}/...`)
- In-memory position calculation approach (sort by laps desc, then by last passing timestamp asc)
- REST URL structure for race control endpoints (follow `/api/v1/` convention)
- Race control page route (`/race-control` or similar)
- Position chart library for the PDF (Chart.js or equivalent server-side rendering)
- Exact confirmation dialog wording for state transitions
- How to model the optional marshal penalty (whether it extends `MarshalAdjustment` or is a separate record)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Heat Structure (Primary Design Spec for Phase 4)
- `.planning/phases/04-race-state-machine/04-HEAT-STRUCTURE-SPEC.md` — **Read in full.** Entity definitions (Round, Race, RaceEntry, EventClass finals config fields), bump-up seeding algorithm, start order rules (stagger round 1 = random/entry order; subsequent rounds = best finisher goes first), round generator inputs, race control implications.

### Stack & Architecture
- `CLAUDE.md` — Authoritative stack spec: Spring Boot 3.4.x, Java 21, Gradle Kotlin DSL, PostgreSQL 16, Flyway, Hibernate 6 write / jOOQ 3.19 read, React 18 + Vite + Tailwind CSS + shadcn/ui, TanStack Query v5, React Hook Form v7, Zod, `@stomp/stompjs` (native WebSocket). STOMP topics: `/topic/race/{raceId}/timing`, `/topic/race/{raceId}/state`, `/topic/race/{raceId}/marshal`. Race state machine transitions. **Read in full before planning.**

### Requirements
- `.planning/REQUIREMENTS.md` — Full v1 requirement list. Phase 4 requirements:
  - CTRL-01, CTRL-02, CTRL-03, CTRL-04, CTRL-05, CTRL-06, CTRL-07, CTRL-08, CTRL-09
  - OFFICIAL-01, OFFICIAL-02, OFFICIAL-03, OFFICIAL-04

### Roadmap
- `.planning/ROADMAP.md` §"Phase 4: Race Control" — Goal, success criteria, full requirements list, and the pre-requisite schema work block (EventClass finals config fields, Round/Race/RaceEntry entities, round generator spec).

### Prior Phase Context
- `.planning/phases/01-domain-foundation/01-CONTEXT.md` — Package structure (`dev.monkeypatch.rctiming`), Gradle multi-module, Hibernate/jOOQ seam (must be maintained), `IllegalStateTransitionException` pattern, format config sealed interface.
- `.planning/phases/02-racer-portal/02-CONTEXT.md` — `RacerPortalLayout` navigation pattern: top nav on `md+`, bottom nav on mobile. Phase 4 cockpit replicates this for mobile parity (D-03).
- `.planning/phases/03-admin-panel-event-management/03-CONTEXT.md` — `EventStateMachineService` pattern (EnumMap of valid transitions, `IllegalStateTransitionException`) to follow for the race state machine. TanStack Query + React Hook Form + shadcn/ui patterns. Admin panel layout.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/dev/monkeypatch/rctiming/domain/event/EventStateMachineService.java` — The exact state machine pattern to replicate for race state (EnumMap of valid transitions, `IllegalStateTransitionException` on invalid move).
- `app/src/main/java/dev/monkeypatch/rctiming/api/GlobalExceptionHandler.java` — Already handles `IllegalStateTransitionException` → HTTP 409. Race state machine errors will be caught by this handler automatically.
- `frontend/src/pages/racer/RacerPortalLayout.tsx` (via Phase 2 context) — Navigation pattern to replicate: top nav desktop + bottom nav mobile. Use as the base for the race control cockpit layout.
- `frontend/src/pages/admin/AdminPanelLayout.tsx` — Admin layout reference; race control uses a different (cockpit) layout but can borrow the mobile nav pattern.
- `frontend/src/components/ui/` — Full shadcn/ui set installed: dialog (for confirmation modals), sheet, table, button, badge, card. Use dialog for all confirmation prompts.
- `frontend/src/lib/api.ts` — Axios instance with JWT Bearer; all race control API calls go through this.
- `app/src/main/resources/db/migration/` — Latest migration is V16. Phase 4 starts at V17.

### Established Patterns
- Hibernate write side for entity mutations; jOOQ read side for projections and aggregations — the seam must be maintained. Race positions are calculated in memory (not persisted during a race) per CLAUDE.md.
- TanStack Query v5 for all server state (queries + mutations)
- React Hook Form v7 + Zod for all forms (round generator wizard inputs, grid position editor)
- Spring Boot integration tests with Testcontainers — extend existing test infrastructure
- EnumMap state machine pattern (`EventStateMachineService`) for all state transitions

### Integration Points
- `EventClass` entity — Phase 4 adds `finalsCount`, `carsPerFinal`, `bumpCount` columns (per heat structure spec)
- `Entry` entity — `RaceEntry` links Race to Entry; entry snapshot fields (transponder) used for timing
- Spring WebSocket STOMP — configure in `config/` package; use `@SendTo` for broadcasts
- JWT auth — Race control and referee pages require `RACE_DIRECTOR` and `REFEREE` roles respectively; `SecurityConfig` needs new role-gated paths

</code_context>

<specifics>
## Specific Ideas

- **Position chart on results PDF:** User explicitly requested a position-by-lap chart on the printed results sheet. "This is useful" — it shows how positions changed during the race. Plan must include this deliverable.
- **Marshal absence tracking:** The "missed marshal count this event" displayed on the pre-race readiness screen is a specific data point the user called out by name (e.g., "David Anderson – missed 2 times this event"). This needs to be stored and queried efficiently.
- **Synthetic timing button:** Dev-profile-only button in the cockpit right panel. User's exact intent: fire a synthetic lap passing to test marshal laps and standings. Not a REST endpoint — a UI affordance in the cockpit.
- **Optional marshal penalty:** User was explicit that not every absence needs a penalty ("club racing where we might genuinely not want to always give penalties"). The absence record and the penalty are separate actions.
- **Referee view purpose:** Referees are physically present and need situational awareness to inform drivers of approaching cars and track incidents — not just to apply penalties. The `/referee` page must provide the full race picture.

</specifics>

<deferred>
## Deferred Ideas

- Real AMB decoder integration — Phase 5 (FORWARDER-*, TIMING-*)
- Audio announcements (AUDIO-01 through AUDIO-15) — Phase 6
- Championship standings calculation — Phase 7
- Event-level results PDF (full meeting export) — can extend Phase 4's per-race export in Phase 7
- Multi-decoder operation — post-v1 (TRACK-04 notes)

</deferred>

---

*Phase: 04-race-state-machine*
*Context gathered: 2026-04-22*
