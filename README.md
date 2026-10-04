# RCTimingControl

> **Early pre-release — v0.1**

Web-based RC club management and race timing system. Replaces RCResults with a modern browser-based race control client for club officials. Entries come from RaceHub or are added as walk-ins.

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
| `app/` | Spring Boot 3.4 backend — REST API, JWT auth, WebSocket timing hub, direct AMB decoder listener, event/championship organization |
| `frontend/` | React 18 + Vite + Tailwind + shadcn/ui — admin panel and race control for officials |
| `decoder-simulator/` | Fake AMB decoder over TCP for development and the trial stack (generative and playback modes) |
| `decoder-protocol/` | Shared AMB/MyLaps decoder protocol parsing (RC-4 text + P3 binary) — used by `app/` and `decoder-simulator/` |
| `docker-compose.yml` | Piper (TTS), optional, for announcer voices. The database is a SQLite file and club logos and TTS clips are stored on local disk, so nothing else needs Docker. |

### Quick start (dev)

**Prerequisites:** Java 21, Docker, Node 20+, `make`

```bash
make dev-start
```

Starts the Spring Boot backend (dev profile, SQLite database in `app/data/db`), the Vite frontend and, if Docker is available, Piper — all in the background.

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |

```bash
make stop       # shut everything down
make clean-db   # wipe the database and start fresh
```

Two racer accounts are seeded automatically in dev mode — see [docs/testing.md](docs/testing.md) for credentials.

For manual setup or individual service control see the [Development guide](docs/development.md).

### Live timing (decoder)

RCTC reads the AMB decoder directly over TCP. There is no separate process to run. Set the decoder's host and port in **Admin → Decoder**. In development, a built-in fake decoder replaces the physical hardware.

```bash
make simulator   # Terminal 2 — fake decoder on :5100
```

Then set the decoder host to `localhost` and click **Test Connection**. See the [decoder setup guide](docs/forwarder.md) for the full walkthrough and hardware setup.

### Spectator boards

Point a TV's browser at `/boards/now-next` (the race on track with live timing, then what's next) or `/boards/results` (the last finished race). No login is needed. Add `?event=ID` to pin a board to one event; otherwise it follows the event that is racing.

### Running tests

```bash
make test       # full integration suite — app + decoder simulator (requires Docker)
make test-fast  # skip jOOQ codegen for faster reruns
```

See [docs/testing.md](docs/testing.md) for the full test matrix, including `decoder-protocol/`.

---

## What's implemented

All ten planned phases are complete:

| Phase | Feature area |
|-------|-------------|
| 1 | Domain foundation — entities, Flyway schema, JWT auth, club/track/format config APIs |
| 2 | Racer portal — profile, cars, transponders, online event entry |
| 3 | Admin panel — event/championship CRUD, entry management, event state machine |
| 4 | Race control — browser cockpit, race state machine, marshal laps, referee tools, round generator |
| 5 | Live timing — AMB RC-4 TCP parser, WebSocket live display |
| 6 | Audio & practice — voice announcements (Piper TTS + Web Speech API), open practice sessions |
| 7 | Results & championship — result snapshots, best-X-from-Y standings, public results pages |
| 8 | First-run setup wizard — guided onboarding for new club installations |
| 9 | User manual & documentation — in-app help system, printable race meeting guide |
| 10 | Docker trial environment — single-command demo stack with fake decoder and seed data |

---

## Docs

- [Trial quickstart](docs/trial-quickstart.md) — run the demo environment, no developer setup needed
- [Decoder setup guide](docs/forwarder.md) — connecting the AMB decoder, simulator, status
- [API reference](docs/api.md) — all endpoints with example requests
- [Development guide](docs/development.md) — environment setup, config, env vars
- [Architecture](docs/architecture.md) — module structure, design decisions
- [Testing guide](docs/testing.md) — running tests, manual UAT checklists
- [AMB decoder protocol](docs/AMB_DECODER_PROTOCOL.md) — RC-4 text and P3 binary wire formats
