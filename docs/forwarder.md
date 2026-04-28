# Forwarder Setup Guide

The forwarder is a separate Java process that connects to the AMB/MyLaps decoder hardware over TCP and streams timing data to the cloud app via gRPC. In development you use the built-in **fake decoder simulator** instead of real hardware.

---

## How it fits together

```
AMB Decoder (TCP :5100)          Fake Decoder Simulator
        │                                │
        └──────────┬─────────────────────┘
                   ▼
          Forwarder process
          (connects out to decoder,
           connects out to app gRPC)
                   │  gRPC :9090
                   ▼
          Spring Boot app
                   │  STOMP WebSocket
                   ▼
             Browser clients
```

The forwarder is a passive client in both directions — it dials out to the decoder and dials out to the app. Nothing connects in to the forwarder.

---

## Prerequisites

Same as the main app: **Java 21**, **Docker**, **`make`**.

---

## Step 1 — Start the app

The Spring Boot app must be running before the forwarder tries to connect (it needs the gRPC server on port 9090).

```bash
make up     # PostgreSQL
make dev    # Spring Boot — REST on :8080, gRPC on :9090
```

---

## Step 2 — Generate an API token

The forwarder authenticates to the app with a pre-shared token. Generate one through the admin UI:

1. Log in as an **ADMIN** user (dev seed: `admin@example.com` / `password` — see [testing.md](testing.md))
2. Navigate to **Admin Panel → Forwarder Token** (`/admin/forwarder`)
3. Click **Generate Token**
4. Copy the token from the one-time reveal panel (it is shown **once only**)
5. Click **Done**

---

## Step 3 — Configure the forwarder

Open `forwarder/src/main/resources/forwarder.properties` and paste the token:

```properties
# Token generated in the admin UI (Step 2)
forwarder.api-token=<paste token here>

# Decoder connection — use localhost when running the simulator
forwarder.decoder.host=localhost
forwarder.decoder.port=5100

# App gRPC server
forwarder.grpc.host=localhost
forwarder.grpc.port=9090
forwarder.grpc.plaintext=true
```

> **Note:** `forwarder.properties` is gitignored. Your token is never committed.

---

## Step 4 — Start the decoder (simulator or real hardware)

### Using the simulator (development)

The simulator emulates an AMB decoder. Start it **before** the forwarder — it must be listening on `:5100` first.

```bash
# Generative mode — emits synthetic PASSING records continuously
# Default: 6 transponders (matching dev seed), ~10–15 s laps with ±2.5 s jitter
make simulator

# Playback mode — replays a captured .dump file
make simulator-playback

# Playback with a custom dump file
make simulator-playback DUMP_FILE=path/to/capture.dump
```

Generative mode options (pass via `--args` directly if needed):
- `--transponders=101,102,...` — comma-separated transponder IDs (default matches dev seed: 101–106)
- `--interval-ms=12500` — base lap time in milliseconds
- `--jitter-ms=2500` — each lap is `intervalMs ± rand(0, jitterMs)` giving realistic variation

The simulator prints each emitted PASSING record to stdout so you can see what the forwarder will receive.

### Using real hardware

Point `forwarder.decoder.host` at the IP address of the AMB decoder on your venue LAN:

```properties
forwarder.decoder.host=192.168.1.100   # your decoder's IP
forwarder.decoder.port=5100            # RC-4 text protocol (firmware < 4.5)
```

Confirm the port in use with your decoder firmware. Port 5100 is standard for RC-4 text protocol (firmware ≤ 4.4). See [AMB_DECODER_PROTOCOL.md](AMB_DECODER_PROTOCOL.md) for protocol details.

---

## Step 5 — Start the forwarder

```bash
make forwarder
```

On startup you should see:

```
Connected to decoder at localhost:5100
Connected to app gRPC at localhost:9090
Streaming passings...
```

The **Forwarder Status Bar** in the race control cockpit will show green pills for both DECODER and FORWARDER once both connections are established.

---

## Full startup order (summary)

| Order | Terminal | Command | Waits for |
|-------|----------|---------|-----------|
| 1 | Any | `make up` | — |
| 2 | Terminal 1 | `make dev` | Docker up |
| 3 | Terminal 2 | `make simulator` | — |
| 4 | Terminal 3 | `make forwarder` | App gRPC ready + simulator listening |
| 5 | Terminal 4 | `make ui` | — (can start anytime) |

The forwarder will retry the decoder connection automatically (exponential backoff, 1 s → 30 s) if it starts before the simulator, so the order between steps 3 and 4 is forgiving in practice.

---

## Troubleshooting

### Forwarder exits immediately with "UNAUTHENTICATED"

The token in `forwarder.properties` is missing, wrong, or has been revoked. Regenerate a token in the admin UI and update the file.

### Status bar shows FORWARDER green but DECODER red

The forwarder connected to the app but cannot reach the decoder (or simulator). Check that `make simulator` is running and that `forwarder.decoder.host/port` are correct.

### Status bar shows DECODER green but FORWARDER red (or disconnected)

The app's gRPC server is not reachable. Confirm `make dev` is running and that `app.grpc.port=9090` is in `app/src/main/resources/application.properties`.

### Unknown transponder alerts appear in the cockpit

A PASSING record arrived for a transponder number not assigned to any entry in the current race. Use the **Link to entry** button in the cockpit to retroactively assign it — laps already counted will be credited to the entry automatically.

### Re-running jOOQ codegen after schema changes

Phase 5 adds two new migrations (V21, V22). If you pulled these and see `package ... does not exist` compile errors:

```bash
make up
./gradlew :app:generateJooq
```

---

## Forwarder Makefile targets

| Target | Description |
|--------|-------------|
| `make forwarder` | Run the forwarder (connects to real or simulated decoder) |
| `make simulator` | Run FakeDecoderServer in generative mode on :5100 |
| `make simulator-playback` | Replay `sample-passings.dump` through FakeDecoderServer |
| `make simulator-playback DUMP_FILE=…` | Replay a custom dump file |
| `make forwarder-build` | Compile the forwarder module without running it |
| `make forwarder-test` | Run forwarder unit + integration tests |
