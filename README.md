# RCTimingControl

Web-based RC club management and race timing system. Replaces RCResults with a modern browser-based race control client and self-service racer portal.

## What's here

| Component | Description |
|-----------|-------------|
| `app/` | Spring Boot 3.4 backend — REST API, JWT auth, WebSocket timing hub |
| `frontend/` | React 18 + Vite + Tailwind + shadcn/ui |
| `forwarder/` | Separate module — connects to AMB/MyLaps decoder hardware |
| `docker-compose.yml` | PostgreSQL 16 + Mailpit (dev email) |

## Quick start

**Prerequisites:** Java 21, Docker, Node 20+

```bash
# 1. Start dev infrastructure
docker compose up -d

# 2. Run the backend (auto-runs Flyway migrations)
./gradlew :app:bootRun --args='--spring.profiles.active=dev'

# 3. Run the frontend (separate terminal)
cd frontend && npm install && npm run dev
```

Backend: `http://localhost:8080`  
Frontend: `http://localhost:5173`  
Mailpit (catch-all email): `http://localhost:8025`

## Running tests

```bash
# Unit tests only (no Docker needed)
./gradlew :app:test --tests "dev.monkeypatch.rctiming.domain.*"

# Full integration tests (requires Docker for Testcontainers)
./gradlew :app:test
```

## Current state — Phase 2 complete

### Phase 1 — Domain Foundation
- Flyway schema: users, auth tokens, club, tracks, racing classes, race formats (JSONB)
- JWT auth: register, login, token refresh (HttpOnly cookie), password reset
- Admin APIs: club profile, tracks + decoder loops, racing classes, race format templates (JSON/YAML export/import)
- Frontend: auth screens (login, register, forgot/reset password), protected routing, silent token refresh

### Phase 2 — Racer Portal
- **Schema**: Flyway migrations V6–V14 — user profile fields, governing body memberships, class ratings, cars + tag system, transponders, events, entries, entry audit log
- **jOOQ**: code-generation pipeline (Docker-based) producing type-safe DSL classes for all Phase 2 tables
- **Cars API**: full CRUD at `/api/v1/racer/cars` — create, update, archive (soft-delete), tag values; jOOQ read projection (no N+1); admin tag category management at `/api/v1/admin/car-tag-categories`
- **Profile + Transponders API**: profile display/edit at `/api/v1/racer/profile`; governing body memberships CRUD; transponders at `/api/v1/racer/transponders` (system-wide uniqueness enforced; 409 on duplicate)
- **Events + Entries API**: public event schedule at `/api/v1/events` (no auth); entry submit/withdraw at `/api/v1/racer/entries`; transponder snapshot at submission time; duplicate transponder warning; membership hard block (422) with admin override; full entry audit log; jOOQ entry history projection
- **Racer Portal UI**: React portal shell under `/racer/*` with responsive nav (desktop top nav / mobile bottom nav); `ProfilePage` (load, patch, membership CRUD); `CarsPage` (grid, `CarEditSheet` sheet, archive); TanStack Query v5 hooks

Phases 3–7 (Admin Panel → Results & Championship) are planned. See `.planning/ROADMAP.md`.

## Docs

- [API reference](docs/api.md) — all endpoints with example requests
- [Development guide](docs/development.md) — environment setup, config, env vars
- [Architecture](docs/architecture.md) — module structure, design decisions
- [Testing guide](docs/testing.md) — running tests, manual UAT checklist for Phase 2 UI
