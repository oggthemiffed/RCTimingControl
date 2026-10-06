# Phase 8: First-Run Setup Wizard - Context

**Gathered:** 2026-05-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 8 delivers a guided multi-step setup wizard that takes a brand-new club installation from an empty database to ready-to-run-a-meeting without needing external documentation. It covers:

1. **Pre-wizard gate** — create the first admin account (unprotected, one-time screen before the wizard sidebar)
2. **Five wizard steps** — club profile, track, race format template, additional staff accounts, decoder hardware config
3. **First-run redirect** — the app detects no club record and redirects to `/setup`; once club profile is saved the redirect permanently stops
4. **Forwarder config download** — the decoder step generates a downloadable `forwarder.env` file (self-contained: token + app URL + decoder IP + protocol) for running the forwarder JAR or Docker container
5. **Setup completion summary** — final screen lists all configured items with edit links
6. **Re-entrant access** — wizard is accessible post-completion from the Admin sidebar as a permanent "Setup Wizard" entry

</domain>

<decisions>
## Implementation Decisions

### Wizard UI Layout
- **D-01:** Full-page `/setup` route with a **left sidebar listing all 5 steps** and their completion state (✓ done / ○ incomplete). Right panel shows the active step form. Consistent with `AdminPanelLayout` sidebar pattern.
- **D-02:** Each step creates the **first item only** (one track, one format template, one additional staff account). A "Manage more in Admin →" link appears below each step for power users wanting to add more before continuing.
- **D-03:** Final wizard screen is a **setup complete summary page**: lists all configured items (club name, track, format, staff, decoder) with edit links. Single "Go to Admin Panel →" button. Not a redirect — user sees what they set up.

### First-Run Detection & Redirect
- **D-04:** **Frontend route guard** approach. On app load, React calls `GET /api/v1/setup/status` → `{ setupComplete: boolean }`. A root-level guard in `App.tsx` redirects to `/setup` when `setupComplete = false`. No Spring Security filter changes needed.
- **D-05:** `setupComplete` is **derived from data**: `SELECT COUNT(*) FROM club_profiles > 0`. No separate flag or migration needed. The redirect clears as soon as the club profile step is saved.
- **D-06:** `/setup` route is **unprotected** (accessible without login). It is only open to create the first admin before any user exists. Once a club profile exists, `setupComplete = true` and the redirect no longer fires; accessing `/setup` directly then requires ADMIN role (protected).
- **D-07:** `GET /api/v1/setup/status` and `POST /api/v1/setup/bootstrap` (admin account creation) are **publicly accessible** endpoints (no auth required). All other setup endpoints require ADMIN role.
- **D-08:** The **pre-wizard gate** is a single "Create your admin account" screen (email + password + confirm password) shown before the wizard sidebar appears. On submit, the account is created and auto-logged in. The 5-step wizard then proceeds authenticated.

### Re-entrancy & Step Completion
- **D-09:** Step completion is **derived from data presence** — no `wizard_progress` table. Logic: club saved → step 1 done; at least one track exists → step 2 done; at least one format template exists → step 3 done; at least one non-RACER staff user exists → step 4 done; decoder config saved on ClubProfile → step 5 done. Re-entry lands on the first incomplete step.
- **D-10:** `GET /api/v1/setup/progress` returns per-step completion status: `{ club, track, format, staff, decoder }` booleans. The sidebar uses this to show ✓/○ indicators.
- **D-11:** **Club profile step has no Skip** — club name (minimum field) is required to create the `club_profiles` row and clear the first-run redirect. Steps 2–5 all have a "Skip for now" option.
- **D-12:** Wizard is accessible **post-completion** via a permanent "Setup Wizard" entry in the Admin panel sidebar (between Staff/Categories and Race Control/Forwarder sections).

### Decoder Config Step
- **D-13:** Decoder config fields (`decoderHost`, `decoderPort`, `decoderProtocol`) stored on `ClubProfile` (new columns, V25 migration). Port is auto-derived from protocol (RC-4 → 5100, P3 → 5403) but editable.
- **D-14:** The decoder step generates a **downloadable `forwarder.env` file** on demand from current DB state: contains `APP_SERVER_URL`, `APP_FORWARDER_TOKEN`, `APP_DECODER_HOST`, `APP_DECODER_PORT`, `APP_DECODER_PROTOCOL`. Always generated fresh (never cached) — re-download after token regeneration.
- **D-15:** `.env` format chosen for Docker compatibility. The forwarder JAR (Spring Boot) reads env vars natively via property binding. The same file works with `env_file: forwarder.env` in `docker-compose.yml`.
- **D-16:** Token generation in the wizard step reuses the existing `ForwarderTokenService` (same logic as `ForwarderTokenPage`). If a token already exists, the wizard shows it and offers "Regenerate" (same confirm flow as `ForwarderTokenPage`).
- **D-17:** **"Test connection"** button polls `GET /api/v1/race-control/forwarder/status` (existing `ForwarderStatusPublisher` endpoint). Shows a spinner while polling, success indicator when `forwarderState = CONNECTED`. Non-blocking — user can Skip the decoder step without a successful test.

### Claude's Discretion
- Exact polling interval and timeout for the "Test connection" button (e.g., poll every 2s for up to 30s before showing a "not yet connected" message).
- Whether the admin account creation pre-gate validates password strength via Zod (same rules as `RegisterPage`) or has its own schema — reuse existing auth validation if it fits.
- Visual styling of the step sidebar completion indicators (icon choice for ✓/○/current step).
- Whether `GET /api/v1/setup/progress` is a new endpoint or merged into `GET /api/v1/setup/status` as extra fields.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope
- `.planning/ROADMAP.md` — Phase 8 goal, success criteria SC-1 through SC-5
- `.planning/REQUIREMENTS.md` — No Phase 8 requirements formally listed yet; success criteria in ROADMAP.md are the authoritative spec

### Existing infrastructure to extend
- `app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfile.java` — entity to extend with `decoderHost`, `decoderPort`, `decoderProtocol` fields
- `app/src/main/java/dev/monkeypatch/rctiming/domain/club/ClubProfileService.java` — write-side service; extend for decoder config fields
- `app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenService.java` — reuse token generation/revocation logic for decoder step
- `app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderStatusPublisher.java` — existing connection state tracker; "Test connection" polls this
- `app/src/main/resources/db/migration/` — next migration is V25; add decoder config columns to `club_profiles`

### Frontend existing pages (reuse patterns)
- `frontend/src/pages/admin/AdminPanelLayout.tsx` — sidebar nav pattern; wizard sidebar follows this layout
- `frontend/src/pages/admin/club/ClubProfilePage.tsx` — club profile form fields (name, timezone, email, phone); wizard club step reuses same fields/schema
- `frontend/src/pages/admin/tracks/TracksPage.tsx` — track form fields; wizard track step uses same schema
- `frontend/src/pages/admin/formats/FormatsPage.tsx` — format template form; wizard format step reuses
- `frontend/src/pages/admin/race-control/ForwarderTokenPage.tsx` — token generate/revoke UX; wizard decoder step reuses this pattern
- `frontend/src/pages/race-control/panels/ForwarderStatusBar.tsx` — existing forwarder connection status display; "Test connection" adapts this
- `frontend/src/App.tsx` — add `/setup` as unprotected route; add root-level setup guard
- `frontend/src/pages/auth/RegisterPage.tsx` — admin account creation form reuses this pattern (or shares Zod schema)

### Auth (admin bootstrap)
- `app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java` — must permit `/api/v1/setup/**` without authentication
- `app/src/main/resources/db/seed/V1000__dev_seed_users.sql` — dev seed only (not production); confirm prod has no pre-seeded users

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ClubProfilePage.tsx` — form with React Hook Form + Zod already wired; wizard club step uses the same schema with a subset of fields
- `ForwarderTokenPage.tsx` — token generate/revoke/copy UX; wizard decoder step embeds this behavior inline
- `ForwarderStatusBar.tsx` — existing STOMP + REST polling for forwarder state; "Test connection" adapts the REST poll (`/api/v1/race-control/forwarder/status`)
- `ProtectedRoute.tsx` — existing auth guard; wizard uses a similar but inverted guard ("redirect to /setup if not setup complete")
- shadcn/ui `Card`, `Button`, `Input`, `Label`, `Badge`, `Switch`, `Select` — all available; no stepper component exists, wizard step sidebar is custom

### Established Patterns
- **React Hook Form + Zod** — all forms use this; wizard steps follow the same pattern
- **TanStack Query** — data fetching for step completion status and progress polling
- **Sidebar layout** — `AdminPanelLayout` has a left nav + right content split; wizard reuses this visual structure at `/setup` outside the admin shell
- **Public routes** — `/login`, `/register`, `/events`, `/results/:raceId`, `/championships/:id` are already unprotected; `/setup` follows the same pattern
- **Flyway migrations** — next is V25; decoder config columns on `club_profiles`

### Integration Points
- `ClubProfile` entity — add `decoderHost` (varchar), `decoderPort` (int), `decoderProtocol` (varchar enum RC4/P3); V25 migration
- `SecurityConfig` — permit `/api/v1/setup/**` for unauthenticated access
- `App.tsx` routing — add `/setup` route + root guard checking `setupComplete`; admin sidebar gets "Setup Wizard" entry
- `ForwarderTokenService` — wizard decoder step calls the same `generateToken()` method as `ForwarderTokenPage`
- `AdminPanelLayout.tsx` — add "Setup Wizard" nav entry in sidebar

</code_context>

<specifics>
## Specific Ideas

- The `forwarder.env` download should be generated fresh every time it's requested — never serve a cached version. This ensures it always contains the current token, even after regeneration.
- "Test connection" is non-blocking: user can download the config, skip the test, and proceed. The test is confirmatory, not a gate.
- The pre-wizard admin account creation screen should be clean and standalone — no wizard sidebar, just a centred card. Only appears when zero users exist in the DB.
- The wizard is intentionally designed to work for both laptop-local deployments (club laptop on LAN) and hosted deployments — the `APP_SERVER_URL` in the generated `forwarder.env` is the only difference between the two.
- Future: forwarder pulling its config from the app (Route 2) is a viable evolution once Phase 10 Docker work is done. The DB storage of decoder config in Phase 8 enables this without schema changes.

</specifics>

<deferred>
## Deferred Ideas

- **Forwarder config pull on startup** (Route 2) — forwarder calls `GET /api/v1/forwarder/decoder-config` after connecting with its token instead of reading from env file. Deferred to post-Phase 10 Docker work; DB schema in Phase 8 already enables this.
- **Environment variable bootstrap** (Option B) — `SETUP_ADMIN_EMAIL` / `SETUP_ADMIN_PASSWORD` env vars to seed first admin on startup. Deferred to Phase 10 Docker environment.

</deferred>

---

*Phase: 08-first-run-setup-wizard*
*Context gathered: 2026-05-03 via interactive discussion*
