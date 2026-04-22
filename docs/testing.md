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
./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.admin.*"   # Phase 3 — admin panel
./gradlew :app:test --tests "*racer*"                                 # Phase 2 — racer portal

# Skip jOOQ codegen (faster when schema hasn't changed)
./gradlew :app:test -x generateJooq
```

### Frontend

No unit test suite is set up yet. Use the type-checker and linter to catch issues:

```bash
cd frontend
npm run build     # Type-check + bundle (tsc -b && vite build)
npm run lint      # ESLint
```

---

## Starting the full dev environment

Always use the `dev` profile — it loads the datasource config from `application-dev.yml`. Running without it causes an immediate "Failed to configure a DataSource" error.

```bash
# Recommended — starts docker, backend, and frontend in one command:
make start

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
| `racer1@example.com` | `Racer1Pass!` | RACER |
| `racer2@example.com` | `Racer2Pass!` | RACER |
| `admin1@example.com` | `Admin1Pass!` | ADMIN |

---

## Manual UAT checklists

Each phase has its own UAT document with detailed steps and recorded pass/fail results.
These are the single source of truth for manual testing — do not duplicate steps here.

| Phase | UAT document | Status |
|-------|-------------|--------|
| Phase 1 — Domain Foundation | [01-HUMAN-UAT.md](../.planning/phases/01-domain-foundation/01-HUMAN-UAT.md) | Complete (4/4) |
| Phase 2 — Racer Portal | [02-HUMAN-UAT.md](../.planning/phases/02-racer-portal/02-HUMAN-UAT.md) | Complete (8/8) |
| Phase 3 — Admin Panel | [03-HUMAN-UAT.md](../.planning/phases/03-admin-panel-event-management/03-HUMAN-UAT.md) | Complete (18/18) |
| Phase 4 — Race Control | TBD | Pending |
