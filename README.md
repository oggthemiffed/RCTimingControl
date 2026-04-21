# RCTimingControl

Web-based RC club management and race timing system. Replaces RCResults with a modern browser-based race control client and self-service racer portal.

## What's here

| Component | Description |
|-----------|-------------|
| `app/` | Spring Boot 3.4 backend — REST API, JWT auth, WebSocket timing hub |
| `frontend/` | React 18 + Vite + Tailwind + shadcn/ui |
| `forwarder/` | Separate module — connects to AMB/MyLaps decoder hardware |
| `docker-compose.yml` | PostgreSQL 16 + Mailpit (dev email) + MinIO (object storage) |

## Quick start

**Prerequisites:** Java 21, Docker, Node 20+, `make`

```bash
make dev-start
```

That's it. Starts PostgreSQL + Mailpit, the Spring Boot backend (dev profile), and the Vite frontend — all in the background.

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Mailpit (dev email) | http://localhost:8025 |
| MinIO S3 API | http://localhost:9000 |
| MinIO Console | http://localhost:9001 (user: `minioadmin`, pass: `minioadmin`) |

```bash
make stop       # shut everything down
make clean-db   # wipe the database and start fresh
```

Two racer accounts are seeded automatically in dev mode — see [docs/testing.md](docs/testing.md) for credentials.

For manual setup or individual service control see the [Development guide](docs/development.md).

## Running tests

```bash
make test       # full integration suite (requires Docker)
make test-fast  # skip jOOQ codegen for faster reruns
```

## Current state — Phase 3 in progress

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

### Phase 3 — Admin Panel (in progress)
- **Schema**: Flyway migrations V15–V16 — event state/track/class columns, championship tables (5 tables)
- **Event backend**: event CRUD + state machine (`DRAFT→PUBLISHED→OPEN→CLOSED→COMPLETED`, HTTP 409 on invalid transition); event-class assignment with config snapshot, overrides, and class combining; admin entry view/withdraw
- **Championship backend**: championship CRUD with best-X-from-Y scoring, TQ/A-final bonuses, scoring source selection; racing-class membership with per-class overrides; event linking with round numbers; points-scale editing; audited exclusions; standings scaffold for Phase 7
- **Storage**: MinIO/S3 object storage wired via `ObjectStorageService` abstraction; club logo upload at `PUT /api/v1/admin/club/logo`; `CarTagCategory` soft-delete via `archived` flag
- **Admin Panel UI** (partial): React shell under `/admin/*` — role-gated layout with sidebar/mobile drawer; Events list + detail + class management + entry withdraw (Plan 05 complete, awaiting UAT); Championships/Club/Tracks/Formats/Categories pages (Plan 06 pending)

Phases 4–7 (Race Control → Results & Championship) are planned. See `.planning/ROADMAP.md`.

## Docs

- [API reference](docs/api.md) — all endpoints with example requests
- [Development guide](docs/development.md) — environment setup, config, env vars
- [Architecture](docs/architecture.md) — module structure, design decisions
- [Testing guide](docs/testing.md) — running tests, manual UAT checklist for Phase 2 UI
