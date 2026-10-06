# Phase 8: First-Run Setup Wizard - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-03
**Phase:** 08-first-run-setup-wizard
**Areas discussed:** Wizard UI layout, First-run detection, Decoder config step, Step skippability & completion

---

## Wizard UI Layout

| Option | Description | Selected |
|--------|-------------|----------|
| Full page, sidebar steps | Dedicated /setup route, left panel lists all 5 steps with completion state, right panel shows active step form | ✓ |
| Full page, top progress bar | Horizontal progress steps (1→2→3→4→5), full-width content below | |

**User's choice:** Full-page sidebar layout

---

| Option | Description | Selected |
|--------|-------------|----------|
| First item inline, link for more | Wizard creates exactly one track/format; "Manage more in Admin →" link for extras | ✓ |
| Mini-list with add/remove | Inline list with "Add another" row per step | |

**User's choice:** First item only, with link to Admin for more

---

| Option | Description | Selected |
|--------|-------------|----------|
| Summary page with links | "Setup complete" screen listing all configured items with edit links, then "Go to Admin Panel" | ✓ |
| Redirect straight to Admin | Skip summary, go directly to Admin Panel on completion | |

**User's choice:** Summary page with edit links

---

## First-Run Detection

| Option | Description | Selected |
|--------|-------------|----------|
| Frontend route guard | On app load, call GET /api/v1/setup/status; redirect to /setup in App.tsx if incomplete | ✓ |
| Spring Security redirect | OncePerRequestFilter checks club_profiles count, issues 302 to /setup | |

**Notes:** User asked for clarification on Spring Security approach. Explained that for an SPA, the filter would need to distinguish API requests (return JSON error) from browser navigation (302 redirect) — adding complexity to both filter and frontend. Frontend guard is simpler and self-contained.

---

| Option | Description | Selected |
|--------|-------------|----------|
| Derive from data presence | setupComplete = club_profiles row exists | ✓ |
| Explicit completed flag | setup_completed boolean on ClubProfile or separate table | |

**User's choice:** Derive from data (no extra migration needed)

---

| Option | Description | Selected |
|--------|-------------|----------|
| Login first, then wizard | Wizard is protected; assumes seeded admin creds | |
| Wizard creates first admin | /setup unprotected on first install; step 0 creates admin account | — |
| Pre-wizard gate (Option A) | Single admin account creation screen before wizard sidebar; auto-login on submit | ✓ |

**Notes:** User noted that production installs have NO pre-seeded users (seed data only runs under dev profile via application-dev.yml). This confirmed that "login first" breaks for genuine first installs. User chose Option A (pre-wizard gate) as most self-contained.

| Option | Description | Selected |
|--------|-------------|----------|
| Pre-wizard gate, then 5 steps | Admin creation screen before sidebar; 5-step wizard proceeds authenticated | ✓ |
| Step 1 is 'Admin account' | Admin account creation is step 1 in the sidebar (6 steps total) | |

**User's choice:** Pre-wizard gate (standalone screen, not part of the 5-step sidebar)

---

## Decoder Config Step

**Context established by discussion:** Decoder config currently lives in the forwarder JAR's own config, not the main app DB. The forwarder authenticates to the app via a token. Discussion explored how to reconcile wizard-entered config with forwarder startup config.

| Option | Description | Selected |
|--------|-------------|----------|
| Store decoder config in DB | New fields on ClubProfile; forwarder pulls config after connecting with token | partial ✓ |
| Token only — forwarder self-configures | Wizard shows token for copying; decoder IP stays in forwarder config | |

**Notes:** User asked how the forwarder would get both decoder config AND the token if both lived in the DB. Three routes discussed:
- Route 1: Bootstrap endpoint (one-time token-free call to get everything)
- Route 2: Token in config file + forwarder pulls decoder config from app after connecting
- Route 3: Fully self-contained config file (token + decoder config all in one download)

User initially leaned toward Route 2 (pull config from app). Then raised Docker compose compatibility — would this require two compose files? Discussed that Route 3 (self-contained .env file) works for both bare JAR and Docker (env_file: forwarder.env). User agreed with KISS principle: "let's do the simplest thing, we can always change later." Chose Route 3.

User noted the future path: "we can run all parts locally on a club laptop OR if they're feeling fancy they have a hosted setup" — self-contained config file handles both equally.

| Option | Description | Selected |
|--------|-------------|----------|
| .env format | KEY=VALUE, works with Docker env_file natively | ✓ |
| You decide | Leave format to planner | |

**User's choice:** .env format for Docker compatibility

---

## Step Skippability & Completion

| Option | Description | Selected |
|--------|-------------|----------|
| Club name required, rest skippable | Club profile step has no Skip (creates the club_profiles row); steps 2–5 have "Skip for now" | ✓ |
| Any single save clears redirect | First save anywhere inserts a minimal club_profiles placeholder | |

**User's choice:** Club name is the only mandatory step; rest skippable

---

| Option | Description | Selected |
|--------|-------------|----------|
| Derive from data presence | No wizard_progress table; completion inferred per step from DB data | ✓ |
| Explicit wizard_progress row | Separate table tracking which steps are marked complete | |

**User's choice:** Derive from data (simpler, no extra migration)

---

| Option | Description | Selected |
|--------|-------------|----------|
| Admin sidebar — Setup entry | Permanent "Setup Wizard" entry in Admin sidebar, always accessible | ✓ |
| Link on Club Profile page | "Re-run setup wizard →" link at bottom of club profile admin page | |

**User's choice:** Admin sidebar entry (prominent, always accessible for reconfiguration)

---

## Claude's Discretion

- Polling interval and timeout for "Test connection" button
- Password strength validation on admin account creation screen (reuse RegisterPage Zod schema or custom)
- Visual styling of step sidebar completion indicators
- Whether `/api/v1/setup/progress` is a separate endpoint or merged into `/api/v1/setup/status`

## Deferred Ideas

- **Forwarder config pull on startup** — forwarder calls GET /api/v1/forwarder/decoder-config after connecting; deferred to post-Phase 10
- **Environment variable bootstrap** — SETUP_ADMIN_EMAIL / SETUP_ADMIN_PASSWORD env vars; deferred to Phase 10 Docker work
