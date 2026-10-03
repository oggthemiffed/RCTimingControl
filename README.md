# RCTimingControl

> **Early pre-release — v0.1**

Web-based RC club management and race timing system. Replaces RCResults with a modern browser-based race control client and self-service racer portal.

## Try it out

If you want to evaluate the system without setting up a development environment, use the Docker trial:

```bash
# 1. Copy the config template
cp .env.example .env

# 2. Start everything (downloads images on first run — a few minutes)
docker compose -f docker-compose.ghcr.yml up
```

Open **http://localhost** — demo data and a live fake decoder are included. See [docs/trial-quickstart.md](docs/trial-quickstart.md) for the full walkthrough including demo credentials.

---

## For developers

| Component | Description |
|-----------|-------------|
| `app/` | Spring Boot 3.4 cloud backend — REST API, JWT auth, WebSocket timing hub, gRPC timing server, event/championship organization |
| `frontend/` | React 18 + Vite + Tailwind + shadcn/ui — cloud racer portal, admin panel, race control |
| `forwarder/` | Separate module — connects to AMB/MyLaps decoder hardware, streams laps to the cloud app via gRPC |
| `decoder-protocol/` | Shared AMB/MyLaps decoder protocol parsing (RC-4 text + P3 binary) — used by both `forwarder/` and `localday/` |
| `localday/` | Independent Spring Boot backend — the **Local Race Day Program**: runs a full event day (check-in, race control, timing, results, public boards) with zero cloud dependency. See [Local Race Day Program](#local-race-day-program) below. |
| `frontend-local/` | React 18 + Vite frontend for `localday/` — officials' race-control UI and anonymous spectator boards |
| `docker-compose.yml` | PostgreSQL 16 + Mailpit (dev email) + MinIO (object storage) — cloud app only; `localday/` uses its own embedded PostgreSQL |

### Quick start (dev)

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

### Live timing (forwarder)

To use live lap timing you need to run the forwarder alongside the app. In development, a built-in fake decoder simulator replaces physical AMB hardware.

```bash
make simulator   # Terminal 2 — fake decoder on :5100
make forwarder   # Terminal 3 — streams laps to app gRPC on :9090
```

See the [Forwarder setup guide](docs/forwarder.md) for the full walkthrough including token generation and hardware setup.

> **Never run `forwarder/` and `localday/` against the same decoder at the same time** — see [Local Race Day Program](#local-race-day-program) below.

### Running tests

```bash
make test       # full integration suite — app + forwarder (requires Docker)
make test-fast  # skip jOOQ codegen for faster reruns
```

See [docs/testing.md](docs/testing.md) for the full test matrix, including `decoder-protocol/`, `localday/`, and `frontend-local/`.

---

## Local Race Day Program

`localday/` + `frontend-local/` is a separate, independent web application — its own Spring Boot backend (with an embedded PostgreSQL, no external database needed) and its own React frontend — that becomes the **sole authority for running an event day** once an official opens it. It runs check-in, race control, live timing, results, and multi-monitor now/next spectator boards with **zero cloud dependency**, and periodically pushes result snapshots up to the cloud while connected. See [docs/architecture.md](docs/architecture.md#local-race-day-program-split-architecture) for the full design rationale and the superseded prior plan it replaces.

```bash
# Terminal 1 — Local Race Day Program backend (embedded Postgres, no Docker needed)
./gradlew :localday:bootRun

# Terminal 2 — its frontend
cd frontend-local && npm run dev
```

Open **http://localhost:5173** — day setup walks you through pre-caching an event from the cloud (or opening offline if already cached). See [docs/development.md](docs/development.md#local-race-day-program-localday--frontend-local) for the full dev workflow, including the decoder simulator and the `e2e` Spring profile used by the Playwright suite.

**Never run `forwarder/` and `localday/` against the same decoder at the same venue** — only one of them should be connected to a given AMB decoder at a time (see [docs/forwarder.md](docs/forwarder.md#local-race-day-program-exclusivity)).

---

## What's implemented

All ten planned phases are complete:

| Phase | Feature area |
|-------|-------------|
| 1 | Domain foundation — entities, Flyway schema, JWT auth, club/track/format config APIs |
| 2 | Racer portal — profile, cars, transponders, online event entry |
| 3 | Admin panel — event/championship CRUD, entry management, event state machine |
| 4 | Race control — browser cockpit, race state machine, marshal laps, referee tools, round generator |
| 5 | Live timing & forwarder — AMB RC-4 TCP parser, gRPC streaming, WebSocket live display |
| 6 | Audio & practice — voice announcements (Piper TTS + Web Speech API), open practice sessions |
| 7 | Results & championship — result snapshots, best-X-from-Y standings, public results pages |
| 8 | First-run setup wizard — guided onboarding for new club installations |
| 9 | User manual & documentation — in-app help system, printable race meeting guide |
| 10 | Docker trial environment — single-command demo stack with fake decoder and seed data |

**Local Race Day Program** (`localday/` + `frontend-local/`, independent of the ten phases above): cloud day-lifecycle lock/unlock, offline check-in and transponder reassignment, independent race-control state machine and grid progression, independent decoder ingestion and live timing, local officials' authentication, multi-monitor spectator boards, periodic snapshot sync to the cloud with device-loss recovery.

---

## Docs

- [Trial quickstart](docs/trial-quickstart.md) — run the demo environment, no developer setup needed
- [Forwarder setup guide](docs/forwarder.md) — simulator, hardware, token setup, startup order
- [API reference](docs/api.md) — all endpoints with example requests
- [Development guide](docs/development.md) — environment setup, config, env vars
- [Architecture](docs/architecture.md) — module structure, design decisions
- [Testing guide](docs/testing.md) — running tests, manual UAT checklists
- [AMB decoder protocol](docs/AMB_DECODER_PROTOCOL.md) — RC-4 text and P3 binary wire formats
