# Testing Guide

## Automated tests

### Backend integration tests

Requires Docker (Testcontainers spins up a real PostgreSQL container automatically).

```bash
# Run all tests
./gradlew :app:test

# Run a specific test class
./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.racer.CarControllerIT"

# Run tests scoped to a phase
./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.admin.*"        # Phase 3 — admin panel
./gradlew :app:test --tests "*racer*"                                      # Phase 2 — racer portal
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

## Local Race Day Program (`localday/` + `frontend-local/` + `decoder-protocol/`)

Independent of the suites above — no Docker required for any of these (no Testcontainers; `localday/` uses an embedded PostgreSQL).

```bash
# Shared protocol parser — used by both forwarder/ and localday/
./gradlew :decoder-protocol:test

# Local Race Day Program backend — unit + integration
./gradlew :localday:test

# Its frontend — Vitest
cd frontend-local && npm test
npm run build   # Type-check + bundle
npm run lint    # ESLint
```

### End-to-end (Playwright)

`frontend-local/e2e/` drives a real `:localday` instance through the full offline workflow — login, grid call, start, marshal adjustments, stop, advance-round, and the spectator boards — plus the auth-guard checks that an unauthenticated device cannot perform a race-control write. It never talks to the cloud: fixture data is seeded directly into `:localday`'s database via a test-only endpoint that only exists under the `e2e` Spring profile (see [development.md](development.md#the-e2e-spring-profile)).

```bash
# Terminal 1
./gradlew :localday:bootRun --args="--spring.profiles.active=e2e"

# Terminal 2
cd frontend-local
npx playwright install chromium   # once
npm run dev -- --port 5173

# Terminal 3
cd frontend-local && npm run test:e2e
```

Runs with a single worker (`fullyParallel: false`, `workers: 1` in `playwright.config.ts`) — the backend is one shared, stateful instance (one open day, one race schedule), so specs run sequentially rather than racing each other's state. This mirrors `test-e2e-localday` in `.github/workflows/ci.yml`, which runs the same three steps against a CI-started `:localday` and `frontend-local` dev server.

### Manual checks not yet automated

Two checks from the Local Race Day Program's plan remain manual (the plan's own Verification Contract calls both "automated where possible, manual otherwise" or explicitly manual):

- **Embedded-Postgres durability under a hard kill.** `RestartPersistenceIT` (in `:localday:test`) proves data survives a graceful restart against the same data directory. Proving survival after `pg_ctl stop -m immediate` (or killing the JVM) mid-write or mid-migration is not yet scripted — do this by hand against a throwaway `LOCALDAY_PG_DATA_DIR` before a high-stakes event if you've changed anything in the persistence path.
- **Decoder ingestion against the simulator.** `./gradlew :forwarder:runSimulator --args="--mode=generative --port=5100 ..."` (same simulator the forwarder uses — see [docs/forwarder.md](forwarder.md)) against a running `:localday` confirms RC-4 lines parse and lap data lands without exceptions. Not wired into CI; run it manually after touching `decoder-protocol/` or `localday/.../timing/`.

---

## Starting the full dev environment

Always use the `dev` profile — it loads the datasource config from `application-dev.yml`. Running without it causes an immediate "Failed to configure a DataSource" error.

### First run — jOOQ codegen required

If you have just pulled new Flyway migrations (or are running for the first time), you must generate jOOQ sources before starting:

```bash
make up                   # Start PostgreSQL first
./gradlew :app:generateJooq   # Run codegen against the live schema
make dev-start            # Then start everything
```

Codegen only needs to re-run when the schema changes (new `V*__.sql` migration files). Subsequent `make dev-start` calls skip it automatically via the `-x generateJooq` flag.

```bash
# Recommended — starts docker, backend, and frontend in one command:
make dev-start

# Stop everything (docker, backend, frontend):
make stop

# Or run each service manually in separate terminals:
make up                                                             # Terminal 1: PostgreSQL + Mailpit + MinIO
./gradlew :app:bootRun --args='--spring.profiles.active=dev'       # Terminal 2: backend
cd frontend && npm run dev                                          # Terminal 3: frontend
```

MinIO console: http://localhost:9001 (user: `minioadmin`, pass: `minioadmin`)

**Dev seed accounts** (created automatically when `dev` profile is active):

| Email | Password | Role |
|-------|----------|------|
| `director@example.com` | `Racer1Pass!` | RACE_DIRECTOR + REFEREE |
| `admin1@example.com` | `Admin1Pass!` | ADMIN |
| `racer1@example.com` | `Racer1Pass!` | RACER |
| `racer2@example.com` | `Racer2Pass!` | RACER |
| `racer3@example.com` | `Racer1Pass!` | RACER |
| `racer4@example.com` | `Racer1Pass!` | RACER |
| `racer5@example.com` | `Racer1Pass!` | RACER |
| `racer6@example.com` | `Racer1Pass!` | RACER |

**Seed event** (V1003): "Club Championship Round 1" (IN_PROGRESS) with 6 Mod Buggy drivers, run order: P1 → P2 → Q1 → Q2 → Q3 → Final A. Navigate to Race Control via **Admin → Race Control** in the sidebar, or directly: `/race-control/event/1`.

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
