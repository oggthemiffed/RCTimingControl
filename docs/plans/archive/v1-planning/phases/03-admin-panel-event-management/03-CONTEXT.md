# Phase 3: Admin Panel & Event Management - Context

**Gathered:** 2026-04-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 3 delivers the full admin panel: event creation and management through the complete state machine, racing class assignment and format configuration, entry management, championship setup and configuration, and the Phase 1 deferred admin config UI (club profile, tracks, race format templates, car tag categories). The admin panel replaces the `AdminPlaceholderPage.tsx` stub with a fully functional, mobile-polished admin interface.

Phase 4 (Race Control) builds on the event/race infrastructure created here. Phase 7 publishes the championship standings publicly (CHAMP-05 is Phase 7). Admin standings view in Phase 3 is internal only.

</domain>

<decisions>
## Implementation Decisions

### Admin Panel Layout
- **D-01:** Left sidebar navigation — the admin panel has 6+ sections which would crowd a top nav bar. Left sidebar (classic admin panel pattern) with a fixed sidebar on desktop.
- **D-02:** Same mobile polish as the racer portal — sidebar collapses on mobile with a hamburger toggle; a bottom nav bar is used on small screens (same pattern as the racer portal: top nav on `md+`, bottom nav on mobile). Admin may be used from a phone for managing entries.
- **D-03:** Sidebar items grouped into sections with dividers:
  - **Events & Competitions:** Events, Championships
  - **Configuration:** Tracks, Race Formats, Club Profile, Car Tag Categories

### Event State Machine UX
- **D-04:** Status badge + valid-action buttons on the event detail page. The current state is shown as a coloured badge in the event header; only the valid next-state transitions are shown as labelled buttons (e.g., "Publish Event", "Open Entries"). Invalid transitions are never shown in the UI — HTTP 409 is a last-resort server guard, not the primary UX.
- **D-05:** Confirm dialog before destructive state changes: `OPEN → ENTRIES_CLOSED` and `IN_PROGRESS → COMPLETED` require a confirmation modal ("This will prevent new entries — are you sure?"). Simple forward transitions (DRAFT → PUBLISHED, PUBLISHED → OPEN) proceed without a dialog.
- **D-06:** Event list page: table with name, date, and colour-coded status badge per row (grey=DRAFT, blue=PUBLISHED, green=OPEN, amber=ENTRIES_CLOSED, red=IN_PROGRESS, black=COMPLETED).

### Event Class & Entry Management
- **D-07:** Inline class management on the event detail page. A "Classes" section shows class rows with format assignment; admin clicks "+ Add Class" to pick a racing class and format template. Format overrides (FORMAT-07) are accessible via "Edit" on each class row.
- **D-08:** EVENT-06 class combining: checkbox multi-select on the class list + "Combine into Shared Race" button. Confirmation dialog states: "These classes will race together but score separately." No drag-and-drop.
- **D-09:** ENTRY-02 entries are nested under the event detail page — no separate top-level "Entries" nav item. Clicking a class row expands (or navigates to) the class entry list. Admin can view entries and withdraw on behalf of a racer (with confirmation). No bulk actions in v1.

### Championship Configuration
- **D-10:** One championship entity covers multiple racing classes (not a separate championship per class). Each class within a championship gets separate standings.
- **D-11:** Championship classes inherit championship-level defaults (best-X-from-Y, points scale, scoring source) with optional per-class overrides. This handles the common case (all classes use the same scoring) while supporting edge cases (e.g., beginner class scores differently).
- **D-12:** Championship is linked to events explicitly — admin adds events to a championship one by one from a picker. The system automatically matches event classes to championship classes by RacingClass entity (same DB record, no manual mapping).
- **D-13:** Points scale (CHAMP-04): editable points table with pre-populated ROAR-style defaults (1st=100, 2nd=80, 3rd=65...). Admin edits inline per position. Preset scale options available (ROAR, BRCA, custom). No JSON/YAML import for the points scale.
- **D-14:** Scoring source (CHAMP-06): radio buttons on the championship config form — "Score from: [Qualifying] [Finals] [Both]". If "Both" selected, qualifying + finals points are summed per round.
- **D-15:** TQ bonus (CHAMP-07) and A-final winner bonus (CHAMP-08): number fields on the championship creation/edit form ("TQ bonus: [1] pts", "A-final winner bonus: [1] pts"). Zero = no bonus. Same form as best-X-from-Y.
- **D-16:** CHAMP-09 driver exclusions are managed on the championship standings page — "Exclude" action per driver per round, admin picks round and reason, audit-logged. Standings recalculate immediately.
- **D-17:** CHAMP-10 standings display in Phase 3 (admin view): full standings table with all rounds shown, drop scores explicitly greyed out, best-X rounds highlighted. DNF/DNS/DQ shown as labelled result types (not just "0") in the standings table — prominent labels, not tooltips.

### Phase 1 Deferred Admin Config Forms
- **D-18:** All Phase 1 deferred config forms (club profile, tracks, race format templates, car tag categories) are built to the same quality as the racer portal — React Hook Form + Zod validation, inline error messages, loading states. These are foundational system config.
- **D-19:** Race format template form: type-specific dynamic fields. Admin picks format type (TIMED / BUMP_UP / POINTS_FINALS) from a dropdown; form fields switch to match that type. Validates against the Java sealed interface server-side.
- **D-20:** Track form: all fields on one form with clearly labelled sections — basic info (name, surface type, optional length) and a "Decoder / Loop Config" section (lap time thresholds TRACK-02/03, loop name, transponder frequency TRACK-04).
- **D-21:** Car tag categories: admin can add new categories, rename existing ones, and archive unused ones (hidden from new car entries, preserved for historical data). No hard delete.
- **D-22:** Club profile: includes name, website URL, governing body affiliations, **and a logo image upload**. Logo stored via MinIO (open source, Docker-backed, S3-compatible object storage). MinIO added to `docker-compose.yml` alongside Postgres. Spring app uses AWS S3 SDK pointing at MinIO endpoint in dev; same SDK works against real S3 in production.

### Claude's Discretion
- Exact sidebar width and collapse behaviour (hamburger animation, overlay vs push)
- Admin panel route structure under `/admin/` (e.g., `/admin/events`, `/admin/events/:id`, `/admin/championships/:id`)
- Format override edit UI (inline field overrides or a diff-style view)
- Points scale preset list (ROAR, BRCA — exact default values)
- MinIO bucket naming and upload path conventions

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Stack & Architecture
- `CLAUDE.md` — Authoritative stack spec (Spring Boot 3.4.x, Java 21, Gradle Kotlin DSL, PostgreSQL 16, Flyway, Hibernate 6 write / jOOQ 3.19 read, React 18 + Vite + Tailwind CSS + shadcn/ui, TanStack Query v5, React Hook Form v7, Zod). Component boundaries, Hibernate/jOOQ seam. **Read in full before planning.**

### Requirements
- `.planning/REQUIREMENTS.md` — Full v1 requirement list. Phase 3 requirements:
  - EVENT-01, EVENT-02, EVENT-05, EVENT-06, EVENT-07
  - ENTRY-02
  - CHAMP-01, CHAMP-02, CHAMP-03, CHAMP-04, CHAMP-06, CHAMP-07, CHAMP-08, CHAMP-09, CHAMP-10
  *(CHAMP-05 is Phase 7 — public standings. Phase 3 delivers admin standings view only.)*

### Roadmap
- `.planning/ROADMAP.md` §"Phase 3: Admin Panel & Event Management" — Goal, success criteria, and full requirements list.

### Prior Phase Context
- `.planning/phases/01-domain-foundation/01-CONTEXT.md` — Phase 1 decisions: format config sealed interface + Jackson polymorphism (D-10, D-11, D-12, D-13), EventClass snapshot + override pattern, Gradle multi-module layout, package structure.
- `.planning/phases/02-racer-portal/02-CONTEXT.md` — Phase 2 decisions: RacerPortalLayout pattern (top nav desktop + bottom nav mobile — replicate for admin), seeded car tag categories (Chassis, ESC, Motor, Servo, Battery, Body, Tyres), admin API stubs for RACER-08, RACER-10, RACER-14.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/pages/racer/RacerPortalLayout.tsx` — The exact navigation pattern to replicate for admin: top nav on `md+`, fixed bottom nav on mobile, `<Outlet />` for content. Copy and adapt for left sidebar variant.
- `frontend/src/components/ui/` — Full shadcn/ui set already installed (card, form, input, button, label, sheet, dialog, etc.). Use dialog for confirm modals.
- `frontend/src/hooks/useAuth.ts` — Auth state; admin pages need role guard (ADMIN role check).
- `frontend/src/components/ProtectedRoute.tsx` — Route guard already exists; extend or wrap for admin role check.
- `frontend/src/lib/api.ts` (or `racerApi.ts`) — Axios instance with JWT Bearer. All admin API calls go through this.
- `app/src/main/java/dev/monkeypatch/rctiming/api/admin/` — Admin API controllers already started in Phase 1. Check what's already implemented before adding new endpoints.
- `app/src/main/java/dev/monkeypatch/rctiming/domain/` — Entities for event, entry, format, raceclass, track, club, car, transponder, user already exist.

### Established Patterns
- TanStack Query v5 for all server state — mutations via `useMutation`, queries via `useQuery`/`useQueryClient`
- React Hook Form v7 + Zod for all forms — same pattern as racer portal profile and car forms
- shadcn/ui components + Tailwind CSS for all UI — no custom CSS
- Hibernate write side for entity mutations, jOOQ read side for projections — maintain the seam
- Spring Boot integration tests with Testcontainers — extend existing test infrastructure

### Integration Points
- `/admin/*` route in `App.tsx` already exists (renders `AdminPlaceholderPage`) — replace with real admin layout and subroutes
- `EventClass` entity: `config_snapshot` + `config_override` JSONB columns (Phase 1 D-13) — format override editing builds on this
- `User` entity with stackable roles (ADMIN, RACE_DIRECTOR, REFEREE) — admin panel requires ADMIN role
- `RaceFormatConfig` sealed interface (Phase 1 D-10) — format template form must produce valid sealed subtypes

</code_context>

<specifics>
## Specific Ideas

- MinIO for object storage: open source, Docker-backed, S3-compatible. Add to `docker-compose.yml` alongside Postgres. Use AWS S3 SDK pointing at MinIO in dev; same SDK works against real S3 if ever needed in prod.
- Championship standings: DNF/DNS/DQ shown as prominent labels (not just 0 pts) in the standings table — user explicitly wants result types visible, not hidden in tooltips.
- Admin panel navigation: sidebar on desktop with grouped sections (Events & Competitions | Configuration). Mobile: same bottom-nav-bar approach as the racer portal for consistency.
- Race format type-switching form: when admin changes the format type dropdown, the fields below should animate/transition to the new set of fields (not a full page reload).

</specifics>

<deferred>
## Deferred Ideas

- Public championship standings (CHAMP-05) — Phase 7
- Payment-gated entry confirmation — v2 (mentioned in Phase 2 deferred)
- Admin bulk entry actions — post-v1 (Phase 3 only needs view + individual withdraw)
- Real S3 migration from MinIO — operational concern, not a v1 code change
- Multi-decoder support (TRACK-04 notes multi-decoder as post-v1 per REQUIREMENTS.md)

</deferred>

---

*Phase: 03-admin-panel-event-management*
*Context gathered: 2026-04-20*
