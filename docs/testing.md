# Testing Guide

## Automated tests

### Backend integration tests

No Docker needed: the tests run on a temporary SQLite database file, created fresh for each test run.

```bash
# Run all tests
./gradlew :app:test

# Run a specific test class
./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.auth.AuthControllerIT"

# Run tests scoped to a phase
./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.admin.*"        # Phase 3 — admin panel
./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.racecontrol.*"  # Phase 4 — race control

# Skip jOOQ codegen (faster when schema hasn't changed)
./gradlew :app:test -x generateJooq
```

### Frontend

Vitest unit tests cover pure logic (referee alerts, proximity detection):

```bash
cd frontend
npm test              # Run Vitest unit tests
npm run build         # Type-check + bundle (tsc -b && vite build)
npm run lint          # ESLint
```

---

## Decoder protocol parser (`decoder-protocol/`)

No Docker required.

```bash
# Shared protocol parser — used by app/ and decoder-simulator/
./gradlew :decoder-protocol:test
```

---

## Starting the full dev environment

Use the `dev` profile: it keeps the database in `app/data/db` and loads the dev seed data. `make clean-db` deletes that database so the next start begins fresh.

### First run — jOOQ codegen required

The generated jOOQ sources are committed. After changing a migration, regenerate them (no Docker needed; codegen migrates a throwaway SQLite file):

```bash
./gradlew :app:generateJooq
```

Codegen only needs to re-run when the schema changes (new `V*__.sql` migration files). Subsequent `make dev-start` calls skip it automatically via the `-x generateJooq` flag.

```bash
# Recommended — starts the backend, the frontend and (if Docker is available) Piper:
make dev-start

# Stop everything:
make stop

# Or run each service manually in separate terminals:
make up                                                             # Terminal 1: Piper (optional, announcer voices)
./gradlew :app:bootRun --args='--spring.profiles.active=dev'       # Terminal 2: backend
cd frontend && npm run dev                                          # Terminal 3: frontend
```

**Dev seed accounts** (created automatically when `dev` profile is active):

| Email | Password | Role |
|-------|----------|------|
| `director@example.com` | `Racer1Pass!` | RACE_DIRECTOR + REFEREE |
| `admin1@example.com` | `Admin1Pass!` | ADMIN |

Only officials have accounts. The six drivers in the seed event are competitors, not users.

**Seed event** (`db/seed/sqlite/V1002`): "Club Championship Round 1" (IN_PROGRESS) with 6 Mod Buggy competitors (transponders 101–106), run order: P1 → P2 → Q1 → Q2 → Q3 → Final A. Navigate to Race Control via **Admin → Race Control** in the sidebar, or directly: `/race-control/event/1`.

---

## Navigating to Race Control

Race control is accessible from two places in the UI:

1. **Admin sidebar → Race Control** — lists all IN_PROGRESS events; click to launch the cockpit
2. **Admin → Events list** — IN_PROGRESS rows have a "Race Control" button in the actions column

Log in as `director@example.com` or `admin1@example.com` to access these.

---

## Triggering synthetic lap passings (dev only)

With a race in RUNNING state, POST to the dev endpoint to generate a fake lap passing:

```bash
curl -X POST http://localhost:8080/api/v1/dev/race/{raceId}/synthetic-passing \
  -H "Authorization: Bearer <token>"
```

The live timing panel will update in real time via WebSocket without needing physical decoder hardware.

---

## Manual UAT checklists

Historical UAT records from v1 development are kept in `.planning/phases/` as a reference for
what was tested and how. They are not maintained going forward — use GitHub Issues to track
manual testing for new work.

| Phase | UAT document | Status |
|-------|-------------|--------|
| Phase 1 — Domain Foundation | [01-HUMAN-UAT.md](../.planning/phases/01-domain-foundation/01-HUMAN-UAT.md) | Complete (4/4) |
| Phase 2 — Racer Portal | [02-HUMAN-UAT.md](../.planning/phases/02-racer-portal/02-HUMAN-UAT.md) | Complete (8/8) |
| Phase 3 — Admin Panel | [03-HUMAN-UAT.md](../.planning/phases/03-admin-panel-event-management/03-HUMAN-UAT.md) | Complete (18/18) |
| Phase 4 — Race Control | [04-UAT.md](../.planning/phases/04-race-state-machine/04-UAT.md) | Complete |
