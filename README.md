# RCTimingControl

> **Early pre-release — v0.1**

RC club race timing and race control, replacing RCResults. It runs on a laptop at the track, reads the AMB/MyLaps decoder directly, and serves race control, check-in, the announcer and the spectator boards to browsers on the venue network. It is the timing side of the RaceHub suite: entries come from a RaceHub export or are added as walk-ins, and only officials sign in.

## Install on a venue laptop

Download the installer for the laptop from the [latest release](https://github.com/oggthemiffed/RCTimingControl/releases/latest): the `.msi` for Windows, the `.pkg` for macOS or the `.deb` for Ubuntu and other Debian-based Linux. It carries its own Java runtime and installs the app as a background service that starts with the laptop.

1. Run the installer.
2. Open **http://localhost:8080** on the laptop and follow the setup wizard to create the first official and the club.
3. Open the About page to see the addresses phones, tablets and boards on the venue network should use.

See [docs/installing.md](docs/installing.md) for each system, the data folder, backups and upgrades.

## Try it out

To explore the app with a demo club and a simulated decoder sending live laps, install it and follow [docs/trial-quickstart.md](docs/trial-quickstart.md).

---

## For developers

| Component | Description |
|-----------|-------------|
| `app/` | Spring Boot 3.4 backend — REST API, officials' JWT auth, WebSocket timing hub, direct AMB decoder listener, RaceHub import, events, championships, backups; serves the built frontend |
| `frontend/` | React 18 + Vite + Tailwind + shadcn/ui — admin panel, check-in, race control, spectator boards and public results |
| `decoder-simulator/` | Fake AMB decoder over TCP for development and trying the app out (generative and playback modes); the app runs it with `simulate` |
| `decoder-protocol/` | Shared AMB/MyLaps decoder protocol parsing (RC-4 text; P3 binary is deferred) — used by `app/` and `decoder-simulator/` |
| `docker-compose.yml` | Piper (TTS), optional, for announcer voices. The database is a SQLite file and club logos and TTS clips are stored on local disk, so nothing else needs Docker. |

### Quick start (dev)

**Prerequisites:** Java 21, Node 20+, `make`; Docker only for the optional Piper voices

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

Two officials (an admin and a race director) and a seed event with six competitors are created automatically in dev mode — see [docs/testing.md](docs/testing.md) for credentials.

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
make test       # full integration suite — app + decoder simulator (no Docker needed)
make test-fast  # skip jOOQ codegen for faster reruns
```

See [docs/testing.md](docs/testing.md) for the full test matrix, including `decoder-protocol/`.

---

## What's implemented

| Area | What it does |
|------|-------------|
| Club setup | First-run wizard, club profile, tracks and decoder loops, racing classes, race formats, officials and their roles |
| Entries | RaceHub Entry Export v1 import with class mapping, walk-ins added by hand, competitors with no login, a primary and secondary transponder per entry |
| Race day | Check-in desk with barcode input and transponder swaps, round generator, race control (grid, start/stop, marshal laps), referee tools, unknown-transponder linking, open practice |
| Live timing | Direct AMB RC-4 decoder listener, WebSocket live display, voice announcements (Piper TTS or the browser's voice), spectator boards |
| Results | Result snapshots, best-X-from-Y championship standings, public results pages, printable results |
| Running it | One installer per system that runs as a background service, scheduled backups and a restore command, in-app help and printable guides |

Still to come: results export to RaceHub, a live feed relay for remote viewers and a streaming overlay (#27 to #29).

---

## Docs

- [Installing](docs/installing.md) — install on a venue laptop, data folder, backups, upgrades
- [Trying it out](docs/trial-quickstart.md) — a demo club and a simulated decoder, no developer setup needed
- [Decoder setup guide](docs/forwarder.md) — connecting the AMB decoder, simulator, status
- [API reference](docs/api.md) — all endpoints with example requests
- [Development guide](docs/development.md) — environment setup, config, env vars
- [Architecture](docs/architecture.md) — module structure, design decisions
- [Testing guide](docs/testing.md) — running tests, manual UAT checklists
- [AMB decoder protocol](docs/AMB_DECODER_PROTOCOL.md) — RC-4 text and P3 binary wire formats
