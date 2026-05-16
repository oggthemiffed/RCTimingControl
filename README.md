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
| `app/` | Spring Boot 3.4 backend — REST API, JWT auth, WebSocket timing hub, gRPC timing server |
| `frontend/` | React 18 + Vite + Tailwind + shadcn/ui |
| `forwarder/` | Separate module — connects to AMB/MyLaps decoder hardware, streams laps to app via gRPC |
| `docker-compose.yml` | PostgreSQL 16 + Mailpit (dev email) + MinIO (object storage) |

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

### Running tests

```bash
make test       # full integration suite — app + forwarder (requires Docker)
make test-fast  # skip jOOQ codegen for faster reruns
```

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

---

## Docs

- [Trial quickstart](docs/trial-quickstart.md) — run the demo environment, no developer setup needed
- [Forwarder setup guide](docs/forwarder.md) — simulator, hardware, token setup, startup order
- [API reference](docs/api.md) — all endpoints with example requests
- [Development guide](docs/development.md) — environment setup, config, env vars
- [Architecture](docs/architecture.md) — module structure, design decisions
- [Testing guide](docs/testing.md) — running tests, manual UAT checklists
- [AMB decoder protocol](docs/AMB_DECODER_PROTOCOL.md) — RC-4 text and P3 binary wire formats
