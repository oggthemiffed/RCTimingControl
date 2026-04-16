# Phase 2: Racer Portal - Context

**Gathered:** 2026-04-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 2 delivers the racer self-service portal: profile management (name, contact details, governing body memberships), car and transponder management, ability rating display, online event entry submission, and a public event schedule visible without login.

Admin UI for event configuration is Phase 3. Race director tooling (transponder swap at race start) is Phase 4. Payment integration is v2. All admin-actor requirements in this phase (RACER-10, RACER-08, RACER-14) are backend API + integration test only — no admin UI.

</domain>

<decisions>
## Implementation Decisions

### Racer Entity Design
- **D-01:** Extend the existing `User` entity — do NOT create a separate `RacerProfile`. Every racer has a login; no separate profile entity is needed for v1. Staff-only users (no racer data) are fine with nullable racer fields.
- **D-02:** Contact fields added to `User`: `phoneNumber` (nullable), `emergencyContactName` (nullable), `emergencyContactPhone` (nullable). `firstName` + `lastName` already exist.
- **D-03:** Ability ratings stored in a separate `user_class_ratings` table (userId, classId, rating 0–100). Displayed on the racer's portal profile page (read-only). Admin-editable via API. Auto-update from championship points is Phase 7.
- **D-04:** Governing body memberships stored in `user_governing_body_memberships` table (userId, governingBodyCode, membershipNumber). Already modelled in Phase 1 design; Phase 2 wires it to the racer profile form.

### Portal Navigation
- **D-05:** Routed pages for the racer portal:
  - `/racer/profile` — profile & contact info
  - `/racer/cars` — car list + tag values
  - `/racer/transponders` — transponder list
  - `/racer/entries` — my entries + history
  - All routes are protected (require login with `RACER` role or any authenticated user).
- **D-06:** Responsive nav: **horizontal top nav on desktop**, **bottom nav bar on mobile** (thumb-friendly, standard mobile pattern). Mobile-first design — compatible with lower-res monitors and phones. Tailwind responsive breakpoints handle the switch.
- **D-07:** Cars section uses **list + inline edit** (card expansion or slide-over sheet using shadcn/ui Sheet component). No separate `/racer/cars/:id` route. Tag values are edited inline on the car card.

### Event Entry Scope
- **D-08:** Full-stack in Phase 2: backend APIs + complete racer-facing UI. Integration tests use Flyway-seeded test event data (one OPEN event with classes and a DRAFT event). This allows end-to-end verification without real admin tooling.
- **D-09:** Public event schedule (`/events`, no login required) shows: event name, date, entry status (OPEN / CLOSED / UPCOMING), and a link to enter. No class list, no entry counts. Simple and fast.
- **D-10:** Entry auto-confirms on submission. PENDING is used only as an internal transient state during the submission transaction — the racer sees "Confirmed" immediately. Admin entry management is Phase 3; payment-gated PENDING is v2.

### Admin Requirements in Phase 2 (Backend Only)
- **D-11:** `RACER-10` (car tag categories): Seed the 7 default categories via Flyway migration (`Chassis`, `ESC`, `Motor`, `Servo`, `Battery`, `Body`, `Tyres`). Expose admin REST API for CRUD. No admin UI — that's Phase 3.
- **D-12:** `RACER-08` (transponder reassignment by race director): Backend endpoint only. The UI for this lives in the Phase 4 race control client.
- **D-13:** `RACER-14` (admin override for membership-required entry): A boolean `membershipOverride` flag on the entry + an admin API endpoint to set it with audit logging. No admin UI in Phase 2.

### Claude's Discretion
- Transponders section layout (card/list pattern, consistent with cars)
- Entries page layout (table or card list — whichever suits the data better)
- Exact Flyway seeding structure for test events and classes
- REST URL structure (following the `/api/v1/` convention established in Phase 1)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Stack & Architecture
- `CLAUDE.md` — Authoritative stack spec (Spring Boot 3.4.x, Java 21, Gradle Kotlin DSL, PostgreSQL 16, Flyway, Hibernate 6 write / jOOQ 3.19 read, JJWT 0.12.x, React 18 + Vite + Tailwind CSS + shadcn/ui, TanStack Query v5, React Hook Form v7, Zod). Component boundaries, Hibernate/jOOQ seam. **Read in full before planning.**

### Requirements
- `.planning/REQUIREMENTS.md` — Full v1 requirement list. Phase 2 requirements:
  - RACER-01 through RACER-14
  - EVENT-03, EVENT-04
  - ENTRY-01

### Roadmap
- `.planning/ROADMAP.md` §"Phase 2: Racer Portal" — Goal, success criteria, and full requirements list for this phase.

### Prior Phase Context
- `.planning/phases/01-domain-foundation/01-CONTEXT.md` — Phase 1 decisions: package structure, Gradle multi-module layout, Hypersistence Utils for JSONB, EventClass entity (snapshot + override pattern), auth screen scaffold.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/components/ui/card.tsx` — shadcn/ui Card; suits the car and transponder list cards
- `frontend/src/components/ui/form.tsx` — shadcn/ui Form (React Hook Form integration); use for profile edit, car add/edit, transponder add, entry submission
- `frontend/src/components/ui/input.tsx`, `button.tsx`, `label.tsx` — Standard form primitives
- `frontend/src/lib/api.ts` — Axios instance with JWT Bearer + httpOnly refresh cookie. All racer API calls go through this.
- `frontend/src/hooks/useAuth.ts` — Auth state; use to get current user id for racer API calls
- `frontend/src/providers/QueryProvider.tsx` — TanStack Query v5 provider already wired in `main.tsx`
- `frontend/src/pages/racer/RacerPlaceholderPage.tsx` — Stub to replace with real portal layout

### Established Patterns
- TanStack Query v5 for all server state (consistent with Phase 1 auth queries)
- React Hook Form v7 + Zod for form validation (established in auth screens)
- shadcn/ui components + Tailwind CSS for all UI (established in Phase 1)
- Hibernate write side for entity mutations, jOOQ read side for projections — the seam must be maintained in Phase 2 racer queries
- Spring Boot integration tests with Testcontainers (established in Phase 1)

### Integration Points
- `User` entity (`domain/user/User.java`) — Phase 2 extends this with racer contact fields
- `UserRepository` — Phase 2 adds racer-profile queries
- `AuthController` + `AuthProvider` — Racer portal uses existing session/JWT flow; no auth changes needed
- `EventClass` entity (Phase 1) — Entry submission references event classes for class selection
- `/api/v1/` URL prefix convention — Follow this for all new Phase 2 endpoints

</code_context>

<specifics>
## Specific Ideas

- User explicitly wants bottom-nav-bar on mobile (not hamburger) for the racer portal — thumb-friendly navigation is a priority
- Car tag inline editing preferred (no separate car detail page) — keeps the cars section compact and mobile-friendly
- Payment integration (PayPal or similar) noted as a likely v2 addition: when payment is added, entry submission would remain PENDING until payment is confirmed. The current auto-confirm design should not hard-code CONFIRMED in a way that's hard to change — keep the state transition logic in a service method.
- Login-less racer import flagged as a future desire: when that feature arrives, `User.passwordHash` would need to become nullable and the auth layer would need guards. Phase 2 does not need to accommodate this — just keep the service boundary clean.

</specifics>

<deferred>
## Deferred Ideas

- Payment integration (PayPal/Stripe): Entry stays PENDING until payment confirmed — v2 addition. Design the entry state machine in Phase 2 to make this addition straightforward (service-layer state transitions, not hard-coded in controllers).
- Login-less racer imports: Future feature where admin imports racers who don't need a login. Requires nullable `passwordHash` on User and auth-layer guards. Deferred post-v1.
- Public entry list visibility: Currently out of scope (REQUIREMENTS.md Out of Scope) — confirmed as privacy decision.
- Admin car tag category UI: Phase 3 admin panel will surface the category CRUD forms.

</deferred>

---

*Phase: 02-racer-portal*
*Context gathered: 2026-04-16*
