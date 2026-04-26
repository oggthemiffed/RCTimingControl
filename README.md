# RCTimingControl

Web-based RC club management and race timing system. Replaces RCResults with a modern browser-based race control client and self-service racer portal.

## What's here

| Component | Description |
|-----------|-------------|
| `app/` | Spring Boot 3.4 backend — REST API, JWT auth, WebSocket timing hub, gRPC timing server |
| `frontend/` | React 18 + Vite + Tailwind + shadcn/ui |
| `forwarder/` | Separate module — connects to AMB/MyLaps decoder hardware, streams laps to app via gRPC |
| `docker-compose.yml` | PostgreSQL 16 + Mailpit (dev email) + MinIO (object storage) |

## Quick start

**Prerequisites:** Java 21, Docker, Node 20+, `make`

```bash
make dev-start
```

Starts PostgreSQL + Mailpit, the Spring Boot backend (dev profile), and the Vite frontend — all in the background.

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

## Live timing (forwarder)

To use live lap timing you need to run the forwarder alongside the app. In development, a built-in fake decoder simulator replaces physical AMB hardware.

```bash
make simulator   # Terminal 2 — fake decoder on :5100
make forwarder   # Terminal 3 — streams laps to app gRPC on :9090
```

See the [Forwarder setup guide](docs/forwarder.md) for the full walkthrough including token generation and hardware setup.

## Running tests

```bash
make test       # full integration suite — app + forwarder (requires Docker)
make test-fast  # skip jOOQ codegen for faster reruns
```

## Current state — Phase 5 complete

### Phase 1 — Domain Foundation ✅
- Flyway schema: users, auth tokens, club, tracks, racing classes, race formats (JSONB)
- JWT auth: register, login, token refresh (HttpOnly cookie), password reset
- Admin APIs: club profile, tracks + decoder loops, racing classes, race format templates (JSON/YAML export/import)
- Frontend: auth screens (login, register, forgot/reset password), protected routing, silent token refresh

### Phase 2 — Racer Portal ✅
- **Schema**: Flyway migrations V6–V14 — user profile fields, governing body memberships, class ratings, cars + tag system, transponders, events, entries, entry audit log
- **jOOQ**: code-generation pipeline (Docker-based) producing type-safe DSL classes for all Phase 2 tables
- **Cars API**: full CRUD at `/api/v1/racer/cars` — create, update, archive (soft-delete), tag values; jOOQ read projection (no N+1); admin tag category management at `/api/v1/admin/car-tag-categories`
- **Profile + Transponders API**: profile display/edit at `/api/v1/racer/profile`; governing body memberships CRUD; transponders at `/api/v1/racer/transponders` (system-wide uniqueness enforced; 409 on duplicate)
- **Events + Entries API**: public event schedule at `/api/v1/events` (no auth); entry submit/withdraw at `/api/v1/racer/entries`; transponder snapshot at submission time; duplicate transponder warning; membership hard block (422) with admin override; full entry audit log; jOOQ entry history projection
- **Racer Portal UI**: React portal shell under `/racer/*` with responsive nav; `ProfilePage`, `CarsPage`, transponders, entries

### Phase 3 — Admin Panel & Event Management ✅
- **Schema**: Flyway migrations V15–V16 — event state/track/class columns, championship tables
- **Event backend**: event CRUD + state machine (`DRAFT→PUBLISHED→OPEN→CLOSED→COMPLETED`, HTTP 409 on invalid transition); event-class assignment with config snapshot, overrides, and class combining
- **Championship backend**: championship CRUD with best-X-from-Y scoring, TQ/A-final bonuses, scoring source selection; standings scaffold
- **Storage**: MinIO/S3 object storage; club logo upload
- **Admin Panel UI**: React shell under `/admin/*` — role-gated layout; Events, Championships, Club, Tracks, Formats, Categories pages

### Phase 4 — Race Control ✅
- **Schema**: Flyway migrations V17–V19 — rounds, races, race entries, marshal adjustments, penalties, incident reports, result snapshots
- **Race state machine**: `PENDING → GRID → RUNNING → STOPPED/FINISHED`; HTTP 409 on invalid transitions; concurrent command rejection
- **Round generator**: snake-draft heat assignment; stagger start ordering from previous round; finals seeding from qualifying standings; bump-up promotion chain
- **Race control API**: lifecycle commands, marshal lap add/remove (full audit), referee penalties, result snapshot on FINISHED
- **Race control UI**: browser cockpit — grid call, marshal list, live positions, marshal lap buttons, referee incident tools, print results

### Phase 5 — Live Timing & Forwarder ✅
- **Forwarder module**: RC-4 text protocol parser, EpochAnchor (server-clock anchoring), gap detector, Netty auto-reconnect TCP client, dual-mode FakeDecoderServer simulator
- **gRPC server**: SmartLifecycle gRPC server on port 9090; BCrypt forwarder API token lifecycle (admin generate/revoke); token auth interceptor
- **Live timing**: `LapPassingEvent` → `LapTimingService` → `LiveTimingHub` → STOMP broadcasts; forwarder connection status on `/topic/system/forwarder-status`
- **Retroactive transponder linking**: unknown passings surfaced in race control UI; race director can link transponder to entry mid-race; historical laps credited retroactively
- **Forwarder UI**: `ForwarderStatusBar` (DECODER/FORWARDER status pills), `UnknownTransponderLinkDialog`, `ForwarderTokenPage` (admin)

Phases 6–7 (Audio & Practice → Results & Championship) are planned. See `.planning/ROADMAP.md`.

## Docs

- [Forwarder setup guide](docs/forwarder.md) — simulator, hardware, token setup, startup order
- [API reference](docs/api.md) — all endpoints with example requests
- [Development guide](docs/development.md) — environment setup, config, env vars
- [Architecture](docs/architecture.md) — module structure, design decisions
- [Testing guide](docs/testing.md) — running tests, manual UAT checklists
- [AMB decoder protocol](docs/AMB_DECODER_PROTOCOL.md) — RC-4 text and P3 binary wire formats
