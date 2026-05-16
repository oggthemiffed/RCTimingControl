# Phase 10: Docker Trial Environment - Research

**Researched:** 2026-05-16
**Domain:** Docker Compose, multi-stage Dockerfiles, nginx reverse-proxy, GitHub Actions GHCR publishing, demo data seeding
**Confidence:** HIGH

---

## Summary

Phase 10 delivers a single-command trial experience: `docker compose -f docker-compose.trial.yml up` brings up six containers (postgres, app, frontend/nginx, piper, forwarder, fake-decoder) pre-loaded with "Wyvern RC Club" demo data and a looping fake decoder emitting RC-4 passings. A companion `docker-compose.ghcr.yml` pulls pre-built images from GHCR so non-technical clubs skip the build step entirely. GitHub Actions publishes four images on `v*` tag pushes.

This is a pure infrastructure phase — no new application features. All required building blocks already exist in the codebase: `SimulatorMain` with generative and playback modes, `FakeDecoderServer`, Flyway migrations, Spring Boot actuator healthcheck at `/actuator/health`, and the Vite build already producing `frontend/dist/`. The forwarder reads config from a flat `forwarder.properties` file (not env vars), which means the Docker container must mount or bake a config file rather than use `environment:` keys.

**Primary recommendation:** Use generative mode (not playback) for the fake-decoder container because generative mode loops forever by design and the existing sample-passings.dump has only 27 lines covering 3 transponders — far too short for a demo. The 8-transponder seed scenario requires a fresh 3-minute replay file authored to match seeded transponder IDs, or generative mode with the 8 transponder IDs hardcoded in the compose env.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** nginx container serves the built Vite SPA and reverse-proxies `/api` and `/ws` to Spring Boot on port 8080 internally. Single exposed port: **80**. Clubs open `http://localhost` — no port number in the URL.
- **D-02:** Separate `docker-compose.trial.yml` — the existing `docker-compose.yml` (dev stack: postgres, mailpit, minio, piper) is left unchanged.
- **D-03:** Companion `docker-compose.ghcr.yml` for pre-built GHCR image variant.
- **D-04:** "Active club mid-season" scenario — "Wyvern RC Club", 2 tracks, 3 race formats, 8 racer accounts, 6-round championship (3 complete), upcoming event with entries open.
- **D-05:** Seed data runs as one-shot container (`restart: no`) on first boot only — idempotent (checks if data already exists).
- **D-06:** Bundled synthetic `.txt` RC-4 file with 8 transponder IDs matching seeded entries, replayed on a 3-minute loop.
- **D-07:** Fake decoder container is a standalone Java process reusing `FakeDecoderServer` from Phase 5, packaged as runnable jar or slim container.
- **D-08:** nginx on host port 80 (configurable via `HOST_PORT`). Internal bridge networking: Spring Boot on 8080, forwarder gRPC on 9090, Piper on 10200 — none exposed to host except nginx:80.
- **D-09:** GitHub Actions triggers on `v*` version tags. Publishes: `app`, `frontend`, `forwarder`, `fake-decoder` to GHCR. No `latest` tag.

### Claude's Discretion

- Gradle multi-project build configuration for producing the app jar (standard Spring Boot `bootJar` task)
- Dockerfile base images (e.g. `eclipse-temurin:21-jre-alpine` for Spring Boot, `node:20-alpine` for Vite build)
- nginx config specifics (gzip, WebSocket upgrade headers, cache headers for static assets)
- Health check intervals and startup order (`depends_on` conditions)
- Whether to use BuildKit cache mounts in Dockerfiles

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.

</user_constraints>

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Static asset serving (Vite SPA) | nginx container | — | Pre-built `dist/` baked into image at build time |
| API reverse proxy (`/api/**`) | nginx container | Spring Boot :8080 | nginx upstream proxy_pass; avoids CORS complexity |
| WebSocket reverse proxy (`/ws/**`) | nginx container | Spring Boot :8080 | Requires `Upgrade` / `Connection` headers in nginx config |
| Spring Boot app startup | app container | postgres (dep) | `depends_on: postgres: condition: service_healthy` |
| Database migrations | Spring Boot (Flyway on startup) | — | No separate migration step needed — confirmed from existing pattern |
| Demo seed data | demo-seed container (one-shot) | postgres (dep) | `restart: no`, runs after postgres healthy, before app starts |
| RC-4 passing emission | fake-decoder container | — | Generative or playback mode via `SimulatorMain` |
| Timing event forwarding | forwarder container | fake-decoder + app | Connects outbound to `fake-decoder:5100`, streams to `app:9090` |
| TTS audio generation | piper container | app | Reused verbatim from dev compose |
| GHCR image publishing | GitHub Actions | Docker BuildKit | On `v*` tag push only |

---

## Standard Stack

### Core

| Library / Tool | Version | Purpose | Why Standard |
|----------------|---------|---------|--------------|
| eclipse-temurin | 21-jre-alpine | JRE base for app and forwarder containers | Official Adoptium image, minimal Alpine footprint, confirmed tag exists [VERIFIED: Docker Hub API] |
| node | 20-alpine | Build stage for Vite SPA | Matches project Node version [VERIFIED: node --version on host = 20.19.2] |
| nginx | 1.27-alpine (or stable-alpine) | Frontend serving + reverse proxy | Industry standard, minimal image |
| postgres | 16-alpine | Database | Already in docker-compose.yml [VERIFIED: codebase] |
| rhasspy/wyoming-piper | latest | TTS | Already in docker-compose.yml [VERIFIED: codebase] |
| Gradle | wrapper (7/8 as in repo) | Multi-project build | Already in repo via `./gradlew` |

### Supporting

| Tool | Purpose | When to Use |
|------|---------|-------------|
| Docker BuildKit | Layer caching, multi-stage builds | Enabled by default in Docker 23+; use `--mount=type=cache` for Gradle and npm caches in CI |
| GHCR (ghcr.io) | Container registry | Pre-built image variant for non-technical clubs (D-03/D-09) |
| `docker/login-action` | GitHub Actions GHCR auth | Standard action for GHCR login |
| `docker/build-push-action` | GitHub Actions image build+push | Standard action for multi-platform builds |
| `docker/metadata-action` | Tag extraction from git ref | Extracts `v1.0.0` → image tags |

**Installation:** No new libraries. All container images pulled at `docker compose up` time.

---

## Architecture Patterns

### System Architecture Diagram

```
Club machine (host)
  port 80 exposed
       |
  [nginx container]
    /          \
  serve         proxy_pass
  /dist/SPA    /api/** and /ws/**
                   |
             [app:8080]
            Spring Boot
                |          \
         postgres:5432    piper:10200
                |
         [demo-seed]
          (one-shot,
          runs once)

  [forwarder container]
    ← connects → fake-decoder:5100  (RC-4 TCP)
    → streams  → app:9090           (gRPC)

  [fake-decoder container]
    SimulatorMain generative mode
    listens :5100
```

Data flow for live timing demo:
1. fake-decoder emits RC-4 PASSING records on port 5100 (looping, all 8 transponders)
2. forwarder TCP-connects to `fake-decoder:5100`, parses frames, EpochAnchor anchors timestamps
3. forwarder gRPC-streams `EpochCorrectedPassing` events to `app:9090`
4. app LapTimingService resolves transponder → entry → broadcasts over STOMP
5. browser receives `/topic/race/{id}/timing` updates in real time

### Recommended Project Structure

```
/                                   # repo root
├── docker-compose.trial.yml        # trial stack (build from source)
├── docker-compose.ghcr.yml         # trial stack (pre-built GHCR images)
├── .env.example                    # copy to .env before compose up
├── docker/
│   ├── app/
│   │   └── Dockerfile              # multi-stage: Gradle build + JRE runtime
│   ├── forwarder/
│   │   └── Dockerfile              # multi-stage: Gradle build + JRE runtime
│   ├── fake-decoder/
│   │   └── Dockerfile              # multi-stage: Gradle build + JRE runtime
│   ├── frontend/
│   │   └── Dockerfile              # multi-stage: node build + nginx serve
│   ├── nginx/
│   │   └── nginx.conf              # reverse proxy + WebSocket upgrade config
│   ├── seed/
│   │   ├── Dockerfile              # FROM postgres:16-alpine, runs seed.sql
│   │   └── seed.sql                # idempotent Wyvern RC Club seed data
│   └── replay/
│       └── wyvern-demo.dump        # RC-4 replay file, 8 transponders, ~3 min
└── .github/
    └── workflows/
        └── publish-trial-images.yml   # triggers on v* tags
```

### Pattern 1: Multi-Stage Dockerfile (App / Forwarder / Fake-Decoder)

**What:** Stage 1 builds the fat JAR using Gradle wrapper; Stage 2 copies the JAR into a slim JRE image.

**When to use:** All three Java containers use this pattern. Gradle cache mounts dramatically reduce CI build time.

**Key constraint — ForwarderConfig:** The forwarder reads `forwarder.properties` from the classpath by default. The container must either (a) bake a template into the image and override with a mounted volume, or (b) extend `ForwarderConfig` to accept env var overrides. **Recommended approach: mount `forwarder.properties` as a Docker volume/bind at `/app/forwarder.properties` and pass `--config-file=/app/forwarder.properties` as a container arg.** Check whether `SimulatorMain` / `ForwarderApplication` already accepts a `--config-file` argument. [VERIFIED: ForwarderConfig.load(Path) exists as public static method — the main() just calls `loadDefault()`, so an env-var-driven entrypoint script or a small CLI arg addition is needed]

**Example — app Dockerfile:**
```dockerfile
# Source: standard Spring Boot multi-stage pattern [ASSUMED - verified pattern]
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts buildSrc/ ./
COPY app/ app/
COPY forwarder/ forwarder/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :app:bootJar -x test -x generateJooq --no-daemon
    
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Note on jOOQ codegen:** The `bootJar` task depends on jOOQ code generation, which requires a live PostgreSQL database in the standard config. This will fail in a plain `docker build` without a running database. [VERIFIED: `generateJooq` task in app/build.gradle.kts uses Testcontainers to spin up postgres — confirmed from the jOOQ plugin config]. The workaround is to either (a) commit generated jOOQ sources to version control, or (b) use a multi-stage build that starts postgres in a service container (Docker buildx `--network` flag), or (c) use `./gradlew :app:bootJar -x generateJooq` with pre-committed jOOQ sources. **Option (c) is simplest and most reliable for CI.** Check whether jOOQ generated sources are currently gitignored. [ASSUMED: likely gitignored — needs verification]

**Example — frontend Dockerfile:**
```dockerfile
# Source: standard Vite multi-stage pattern [ASSUMED - verified pattern]
FROM node:20-alpine AS builder
WORKDIR /build
COPY frontend/package*.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci
COPY frontend/ .
RUN npm run build

FROM nginx:stable-alpine AS runtime
COPY --from=builder /build/dist/ /usr/share/nginx/html/
COPY docker/nginx/nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
```

### Pattern 2: nginx WebSocket Reverse Proxy

**What:** nginx must pass `Upgrade` and `Connection` headers for STOMP WebSocket connections. Without these, the WebSocket handshake fails silently.

**Critical config block:**
```nginx
# Source: official nginx WebSocket proxying docs [CITED: https://nginx.org/en/docs/http/websocket.html]
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

server {
    listen 80;
    
    # Static SPA assets
    location / {
        root   /usr/share/nginx/html;
        index  index.html;
        try_files $uri $uri/ /index.html;  # SPA fallback for React Router
    }
    
    # API proxy
    location /api/ {
        proxy_pass http://app:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    # WebSocket proxy — STOMP over SockJS-free native WebSocket
    location /ws {
        proxy_pass http://app:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_set_header Host $host;
        proxy_read_timeout 3600s;  # keep-alive for long-lived STOMP sessions
    }
}
```

**Why `proxy_read_timeout 3600s`:** Default nginx read timeout is 60 seconds. STOMP connections are long-lived; without this, nginx closes idle WebSocket connections mid-race.

### Pattern 3: Demo-Seed One-Shot Container

**What:** A `demo-seed` service runs after postgres is healthy, inserts demo data, then exits. `restart: no` prevents re-runs. Idempotency guard: check `SELECT count(*) FROM club WHERE name = 'Wyvern RC Club'` before inserting.

**Docker Compose dependency chain:**
```yaml
# docker-compose.trial.yml excerpt [ASSUMED - standard pattern]
services:
  postgres:
    image: postgres:16-alpine
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U rctiming -d rctiming"]
      interval: 5s
      timeout: 3s
      retries: 10
      
  demo-seed:
    build: docker/seed/
    restart: "no"
    environment:
      PGHOST: postgres
      PGUSER: rctiming
      PGPASSWORD: rctiming
      PGDATABASE: rctiming
    depends_on:
      postgres:
        condition: service_healthy
        
  app:
    build: docker/app/
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/rctiming
      # ... other env vars
    depends_on:
      postgres:
        condition: service_healthy
      demo-seed:
        condition: service_completed_successfully
```

**Why app depends on demo-seed completing:** If app starts Flyway migrations while seed is still inserting, race conditions can corrupt the seed (e.g., FK violations if seed inserts racers before app has finished creating tables). Depends_on `service_completed_successfully` guarantees seed finishes before app starts. Flyway migrations run as part of app startup — this is correct ordering.

### Pattern 4: Fake-Decoder Container — Generative Mode

**What:** `SimulatorMain` already supports `--mode=generative --transponders=<csv> --interval-ms=<n> --jitter-ms=<n> --port=5100`. For the trial environment, generative mode is preferred because it loops forever without a replay file and requires no file mount.

**Reasoning:** The existing `sample-passings.dump` has only 27 lines covering 3 transponder IDs (`8533156`, `3885036`, `1234567`). This does not match 8 seeded transponder IDs and loops too fast. Generative mode with 8 transponder IDs matching seed data is cleaner.

**Decision note (D-06 vs implementation):** D-06 specifies a `.txt` replay file. This is achievable via `PlaybackMode` but requires authoring a proper 3-minute loop dump file with 8 transponder IDs and loop-reset logic. `PlaybackMode.replay()` currently plays the file once and returns — it does not loop. A looping wrapper is needed if playback mode is used. **Generative mode already loops indefinitely and is simpler.** The planner should confirm whether D-06 strictly requires a replay file or whether generative mode with matching transponder IDs satisfies the intent. [ASSUMED: generative mode satisfies intent — flag for planner confirmation]

**Fake-decoder container command:**
```yaml
fake-decoder:
  build: docker/fake-decoder/
  command: >
    --mode=generative
    --port=5100
    --transponders=101,102,103,104,105,106,107,108
    --interval-ms=13000
    --jitter-ms=2500
  # No host port exposure — internal network only
```

### Pattern 5: Forwarder Config in Docker

**Critical discovery:** `ForwarderApplication.main()` calls `ForwarderConfig.loadDefault()` which reads `forwarder.properties` from the classpath. The container image bakes in the properties file at build time with `localhost` defaults — wrong for Docker networking.

**Three options:**

| Option | Mechanism | Effort | Risk |
|--------|-----------|--------|------|
| A — Volume mount | Mount `forwarder.properties` at runtime via Docker volume | Low | File must exist on host |
| B — Entrypoint script | Shell script writes properties from env vars, then exec's the JAR | Medium | Adds complexity |
| C — Env var support in ForwarderConfig | Extend `ForwarderConfig.load()` to accept `FORWARDER_*` env vars with properties fallback | Medium | Small code change |

**Recommendation: Option B (entrypoint script).** The forwarder is a plain Java process without Spring — adding env var support would touch `ForwarderConfig`. An entrypoint script is a self-contained Docker concern that generates a correct `forwarder.properties` from env vars before starting the JAR.

```bash
#!/bin/sh
# docker/forwarder/entrypoint.sh
cat > /app/forwarder.properties <<EOF
forwarder.api-token=${FORWARDER_API_TOKEN}
forwarder.decoder.host=${FORWARDER_DECODER_HOST:-fake-decoder}
forwarder.decoder.port=${FORWARDER_DECODER_PORT:-5100}
forwarder.grpc.host=${FORWARDER_GRPC_HOST:-app}
forwarder.grpc.port=${FORWARDER_GRPC_PORT:-9090}
forwarder.grpc.plaintext=${FORWARDER_GRPC_PLAINTEXT:-true}
EOF
exec java -cp /app/app.jar \
  dev.monkeypatch.rctiming.forwarder.ForwarderApplication \
  --config-file=/app/forwarder.properties
```

Wait — `ForwarderApplication.main()` does not accept a `--config-file` arg. It calls `ForwarderConfig.loadDefault()` only. **Option B requires either: adding `--config-file` arg support to `ForwarderApplication`, or having the entrypoint script inject the properties file into the classpath root via a temp dir trick, or changing to `ForwarderConfig.load(Path.of("/app/forwarder.properties"))`.**

**Revised recommendation for ForwarderApplication:** Add a minimal check in `main()`:
```java
// If first arg is --config-file=<path>, load from that path
ForwarderConfig cfg = (args.length > 0 && args[0].startsWith("--config-file="))
    ? ForwarderConfig.load(Path.of(args[0].substring("--config-file=".length())))
    : ForwarderConfig.loadDefault();
```
This is a one-line change, no Spring dependency, fully backward-compatible.

### Pattern 6: GitHub Actions GHCR Workflow

**Standard actions for GHCR publishing [ASSUMED: standard GitHub Actions patterns — not verified against live Actions marketplace in this session]:**

```yaml
# .github/workflows/publish-trial-images.yml
name: Publish Trial Images
on:
  push:
    tags: ['v*']

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
          
      - uses: docker/metadata-action@v5
        id: meta
        with:
          images: ghcr.io/oggthemiffed/rctimingcontrol/app
          tags: type=semver,pattern={{version}}
          
      - uses: docker/build-push-action@v6
        with:
          context: .
          file: docker/app/Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

**Four separate jobs** (one per image: `app`, `frontend`, `forwarder`, `fake-decoder`) or a matrix strategy. Matrix is cleaner for maintenance.

**GHCR image names (from CONTEXT.md specifics):**
- `ghcr.io/oggthemiffed/rctimingcontrol/app:v{tag}`
- `ghcr.io/oggthemiffed/rctimingcontrol/frontend:v{tag}`
- `ghcr.io/oggthemiffed/rctimingcontrol/forwarder:v{tag}`
- `ghcr.io/oggthemiffed/rctimingcontrol/fake-decoder:v{tag}`

### Anti-Patterns to Avoid

- **Using `latest` GHCR tag:** D-09 explicitly forbids it. Always pin to semantic version tag.
- **Exposing postgres, gRPC, or piper to host ports in trial compose:** Internal bridge only (D-08).
- **Using `docker-compose.yml` for trial:** D-02 mandates a separate file. Do not touch the dev compose.
- **`restart: always` on demo-seed:** Must be `restart: "no"` (D-05). Quoting is required in YAML — `no` unquoted is a boolean `false` in YAML.
- **`ddl-auto: create-drop` or `update`:** CLAUDE.md explicitly forbids these. Flyway manages schema; app uses `validate`.
- **`nginx: proxy_read_timeout` default (60s):** Will drop live timing WebSocket connections in mid-race.
- **Building forwarder without `--config-file` support:** The default classpath `forwarder.properties` has `localhost` — wrong in Docker network.
- **Depending on jOOQ codegen in Docker build without pre-committed sources:** Will fail unless Testcontainers PostgreSQL is available during image build, which it is not in standard `docker build`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| GHCR auth in CI | Custom curl/API calls | `docker/login-action@v3` | Standard, handles token refresh |
| Image tag extraction from git ref | Shell `git describe` parsing | `docker/metadata-action@v5` | Handles semver, branch, SHA, OCI labels automatically |
| BuildKit layer caching in CI | Manual cache volume management | `cache-from: type=gha` | GitHub Actions cache API, automatic keying |
| nginx WebSocket upgrade headers | Custom TCP proxy | nginx `$http_upgrade` map directive | Proven pattern; hand-rolled breaks on reconnect |
| Postgres healthcheck | Sleep timer | `pg_isready` in `healthcheck.test` | `pg_isready` reports TCP + auth readiness; sleep is a race condition |
| Seed idempotency | Truncate + re-insert | `INSERT ... WHERE NOT EXISTS` or `ON CONFLICT DO NOTHING` | Truncation destroys first-run state on restarts |

**Key insight:** The existing `SimulatorMain` already provides everything the fake-decoder container needs — no new Java code is required beyond the `--config-file` addition to `ForwarderApplication`.

---

## Common Pitfalls

### Pitfall 1: jOOQ Codegen Requires Live PostgreSQL During Docker Build

**What goes wrong:** `./gradlew :app:bootJar` triggers `generateJooq`, which launches a Testcontainers PostgreSQL. Testcontainers requires Docker-in-Docker or a Docker socket — neither is available in standard GitHub Actions `docker build`.

**Why it happens:** The jOOQ plugin config uses `JdbcDriverBasedCodegenConfigSource` with Testcontainers to spin up a throwaway database, apply Flyway migrations, then generate type-safe SQL classes.

**How to avoid:** Pass `-x generateJooq` to Gradle and commit the generated jOOQ sources. Check `.gitignore` for `build/generated-src/jooq/` — if gitignored, un-ignore just the generated sources (or move them to `src/generated/jooq/` which is convention for committed generated sources). This is the `build` argument used in the Makefile `test-fast` target.

**Warning signs:** `docker build` fails with `Cannot connect to Docker daemon` or `Failed to obtain JDBC Connection` during image build.

### Pitfall 2: YAML `no` Is a Boolean, Not a String

**What goes wrong:** `restart: no` in Docker Compose YAML is parsed as `restart: false` (boolean), not the string `"no"`. Docker Compose may reject or misinterpret this.

**Why it happens:** YAML 1.1 (used by older Docker Compose) treats `no`, `yes`, `true`, `false`, `on`, `off` as booleans.

**How to avoid:** Always quote: `restart: "no"`. Docker Compose v2 (Compose Spec) handles both but quoting is defensive.

### Pitfall 3: `depends_on` Does Not Wait for Application Readiness

**What goes wrong:** `depends_on: app: condition: service_healthy` requires the `app` service to have a `healthcheck` defined. Without it, `condition: service_healthy` is treated as `condition: service_started` (container running, not ready).

**Why it happens:** Docker Compose `service_healthy` condition only works when the service defines a `healthcheck`. The Spring Boot actuator is at `/actuator/health` — use it.

**How to avoid:**
```yaml
app:
  healthcheck:
    test: ["CMD-SHELL", "curl -sf http://localhost:8080/actuator/health | grep -q UP"]
    interval: 10s
    timeout: 5s
    retries: 10
    start_period: 60s  # Spring Boot + Flyway migrations take ~30s cold
```
`start_period` is critical — Spring Boot startup with Flyway and full schema takes longer than a default healthcheck allows.

### Pitfall 4: nginx `try_files` Missing for SPA Routing

**What goes wrong:** Deep-linked React Router URLs (e.g., `http://localhost/admin/events/123`) return 404 from nginx because the file doesn't exist on disk.

**Why it happens:** React Router is client-side — only `index.html` exists on disk. nginx must serve `index.html` for all non-file paths.

**How to avoid:** `try_files $uri $uri/ /index.html;` in the `location /` block.

### Pitfall 5: ForwarderConfig Classpath Properties Baked with Wrong Hosts

**What goes wrong:** Forwarder container starts but cannot connect to `localhost:5100` (fake decoder) or `localhost:9090` (app gRPC) because Docker networking uses service names.

**Why it happens:** `ForwarderConfig.loadDefault()` reads `forwarder.properties` from the classpath, which is baked into the JAR at build time with `localhost` defaults.

**How to avoid:** Either entrypoint script generates correct properties before JAR launch, or `ForwarderApplication.main()` is extended to accept `--config-file` argument. See Pattern 5.

### Pitfall 6: Seed Data FK Order

**What goes wrong:** Seed SQL inserts in wrong order — e.g., inserting `race_format` records before `club` exists, or inserting `championship_round` before `championship` — causing FK constraint violations.

**Why it happens:** FK constraints are enforced on every INSERT in PostgreSQL.

**How to avoid:** Order seed SQL statements to respect FK dependency chain:
1. `users` (racer accounts)
2. `club`
3. `governing_body_affiliation`
4. `tracks`
5. `racing_class`
6. `race_format`
7. `event` (references track, club)
8. `event_class` (references event, race_format, racing_class)
9. `car` (references user)
10. `transponder` (references user)
11. `entry` (references event_class, user, car, transponder)
12. `championship` (references club)
13. `championship_round` + `championship_standing` (references championship, event)
14. `result_snapshot` (references race — requires round/race records first)

The exact table names and columns must be confirmed against the V1–V26 migrations before writing seed SQL. [VERIFIED: migrations exist V1–V26; specific column names require migration file reading during planning]

### Pitfall 7: Piper `latest` Tag Instability

**What goes wrong:** `rhasspy/wyoming-piper:latest` pulls a different version between builds, causing voice model format changes.

**Why it happens:** `latest` is not pinned.

**How to avoid:** Pin to a specific digest or tag in the trial compose. However, the dev `docker-compose.yml` already uses `latest` — keep consistent with dev to avoid divergence, or accept the risk in a trial environment. [ASSUMED: `latest` acceptable for trial since it's already used in dev]

---

## Code Examples

### nginx.conf — Full WebSocket-Safe Config

```nginx
# Source: nginx WebSocket proxying docs [CITED: https://nginx.org/en/docs/http/websocket.html]
# Combined with standard SPA serving pattern [ASSUMED: standard pattern]

worker_processes auto;
events { worker_connections 1024; }

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    gzip          on;
    gzip_types    text/plain text/css application/json application/javascript;

    map $http_upgrade $connection_upgrade {
        default upgrade;
        ''      close;
    }

    server {
        listen 80;
        server_name _;

        # Static SPA
        root /usr/share/nginx/html;
        index index.html;

        location / {
            try_files $uri $uri/ /index.html;
        }

        # Long-cache static assets (Vite fingerprints filenames)
        location /assets/ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }

        # REST API proxy
        location /api/ {
            proxy_pass         http://app:8080;
            proxy_set_header   Host $host;
            proxy_set_header   X-Real-IP $remote_addr;
            proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        }

        # STOMP WebSocket proxy
        location /ws {
            proxy_pass             http://app:8080;
            proxy_http_version     1.1;
            proxy_set_header       Upgrade $http_upgrade;
            proxy_set_header       Connection $connection_upgrade;
            proxy_set_header       Host $host;
            proxy_read_timeout     3600s;
            proxy_send_timeout     3600s;
        }
    }
}
```

### docker-compose.trial.yml — Service Skeleton

```yaml
# Source: Docker Compose Spec [ASSUMED: standard pattern]
version: "3.9"

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: rctiming
      POSTGRES_USER: rctiming
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-rctiming_trial}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U rctiming -d rctiming"]
      interval: 5s
      timeout: 3s
      retries: 10

  demo-seed:
    build: docker/seed/
    restart: "no"
    environment:
      PGHOST: postgres
      PGUSER: rctiming
      PGPASSWORD: ${POSTGRES_PASSWORD:-rctiming_trial}
      PGDATABASE: rctiming
    depends_on:
      postgres:
        condition: service_healthy

  app:
    build:
      context: .
      dockerfile: docker/app/Dockerfile
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/rctiming
      SPRING_DATASOURCE_USERNAME: rctiming
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-rctiming_trial}
      SPRING_MAIL_HOST: mailpit
      SPRING_MAIL_PORT: 1025
      JWT_SECRET: ${JWT_SECRET:-trial-jwt-secret-change-in-production}
      STORAGE_ENDPOINT: http://minio:9000
      STORAGE_ACCESS_KEY: ${MINIO_ACCESS_KEY:-minioadmin}
      STORAGE_SECRET_KEY: ${MINIO_SECRET_KEY:-minioadmin}
      STORAGE_BUCKET: rctiming
      STORAGE_PUBLIC_BASE_URL: http://localhost/storage/rctiming
      TTS_ENDPOINT: piper:10200
      TTS_ENABLED: "true"
    depends_on:
      postgres:
        condition: service_healthy
      demo-seed:
        condition: service_completed_successfully
    healthcheck:
      test: ["CMD-SHELL", "curl -sf http://localhost:8080/actuator/health | grep -q UP || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 12
      start_period: 90s

  frontend:
    build:
      context: .
      dockerfile: docker/frontend/Dockerfile
    ports:
      - "${HOST_PORT:-80}:80"
    depends_on:
      app:
        condition: service_healthy

  piper:
    image: rhasspy/wyoming-piper:latest
    volumes:
      - piper_data:/data
    command: >
      --voice en_GB-alan-medium
      --voice en_GB-cori-high
      --voice en_US-amy-medium

  forwarder:
    build:
      context: .
      dockerfile: docker/forwarder/Dockerfile
    environment:
      FORWARDER_API_TOKEN: ${FORWARDER_API_TOKEN:-}
      FORWARDER_DECODER_HOST: fake-decoder
      FORWARDER_DECODER_PORT: 5100
      FORWARDER_GRPC_HOST: app
      FORWARDER_GRPC_PORT: 9090
    depends_on:
      app:
        condition: service_healthy
      fake-decoder:
        condition: service_started

  fake-decoder:
    build:
      context: .
      dockerfile: docker/fake-decoder/Dockerfile
    command: >
      --mode=generative
      --port=5100
      --transponders=101,102,103,104,105,106,107,108
      --interval-ms=13000
      --jitter-ms=2500

volumes:
  pgdata:
  piper_data:
```

### Seed SQL — Idempotency Pattern

```sql
-- Source: standard PostgreSQL ON CONFLICT DO NOTHING pattern [ASSUMED]
-- Guard: skip entire seed if Wyvern club already exists
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM club WHERE name = 'Wyvern RC Club') THEN
    RAISE NOTICE 'Seed data already present — skipping';
    RETURN;
  END IF;

  -- Insert club
  INSERT INTO club (name, ...) VALUES ('Wyvern RC Club', ...);
  
  -- Insert tracks, formats, users, etc. in FK order
  -- ...
END $$;
```

### Forwarder main() — Config File Arg Extension

```java
// Minimal backward-compatible change to ForwarderApplication.main()
// Source: ForwarderConfig.load(Path) already exists [VERIFIED: codebase]
public static void main(String[] args) throws Exception {
    ForwarderConfig cfg;
    if (args.length > 0 && args[0].startsWith("--config-file=")) {
        cfg = ForwarderConfig.load(Path.of(args[0].substring("--config-file=".length())));
    } else {
        cfg = ForwarderConfig.loadDefault();
    }
    // ... rest unchanged
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| docker-compose v1 (separate binary) | Docker Compose v2 plugin (`docker compose`) | Docker 20.10+ | No `docker-compose` binary needed; Compose Spec replaces v2/v3 versioning |
| `version: "3.9"` in compose files | Version field optional/deprecated in Compose Spec | 2023 | Still accepted; omitting `version:` is now valid but older tools require it |
| `condition: service_healthy` missing | Must define `healthcheck` on the depended-on service | Always required | Without `healthcheck`, condition silently falls back to `service_started` |
| GHCR `latest` images | Pinned semantic version tags | Best practice | `latest` causes non-reproducible deployments |

**Deprecated/outdated:**
- `docker-compose.yml` `version: "2"` or `"3"` schema: The Compose Spec absorbed all versions; specifying version is now optional in Docker Compose v2.
- `link:` directive in compose: Replaced by user-defined networks (bridge) which are the default.

---

## Runtime State Inventory

Step 2.5 SKIPPED — this is a greenfield infrastructure phase (new files only). No rename/refactor operations.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker Engine | All containers | ✓ | 26.1.5 | — |
| Docker Compose v2 | `docker compose up` | ✓ | v5.1.3 | — |
| Java 21 (JDK) | Gradle build in CI | ✓ (host) | OpenJDK 21.0.11 | eclipse-temurin:21-jdk-alpine in Docker builder stage |
| Node 20 | Vite build in Docker builder stage | ✓ (host) | 20.19.2 | node:20-alpine in Docker builder stage |
| GHCR write access | GitHub Actions publish | ✗ (local) | — | `GITHUB_TOKEN` with `packages: write` in Actions job |
| postgres:16-alpine | Database container | ✓ (pulled) | 16 | — |

**Missing dependencies with no fallback:**
- GHCR `packages: write` permission — requires GitHub Actions context; cannot be tested locally.

**Missing dependencies with fallback:**
- None blocking local trial development.

---

## Validation Architecture

Nyquist validation is enabled (`workflow.nyquist_validation` not set to false in config.json).

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 (backend), Vitest (frontend) |
| Config file | `app/build.gradle.kts` (useJUnitPlatform), `frontend/vite.config.ts` |
| Quick run command | `./gradlew :app:test -x generateJooq --tests "*Trial*"` |
| Full suite command | `./gradlew :app:test -x generateJooq && cd frontend && npm test` |

### Phase Requirements → Test Map

Phase 10 has no application-layer requirements (it is pure infrastructure). Tests focus on compose stack validation and smoke tests.

| Req ID | Behavior | Test Type | Automated Command | Notes |
|--------|----------|-----------|-------------------|-------|
| SC-1 | `docker compose up` brings all services healthy | smoke / manual | `docker compose -f docker-compose.trial.yml ps` | Manual verification; no automated test |
| SC-2 | Demo seed populates database on first boot | smoke / manual | `docker compose exec postgres psql -U rctiming -c "SELECT name FROM club"` | Manual |
| SC-3 | Fake decoder emits passings visible in race control | manual UAT | — | Requires browser + running race |
| SC-4 | Setup wizard accessible in trial environment | manual UAT | — | Browser navigation test |
| SC-5 | `docker-compose.ghcr.yml` pulls and runs from GHCR | manual | `docker compose -f docker-compose.ghcr.yml up` | Requires published images |

### Sampling Rate

- **Per task commit:** Verify `docker compose config --file docker-compose.trial.yml` parses without error
- **Per wave merge:** `docker compose -f docker-compose.trial.yml build` succeeds
- **Phase gate:** Full `docker compose -f docker-compose.trial.yml up` smoke test before `/gsd-verify-work`

### Wave 0 Gaps

- [ ] No automated test stubs needed — this phase has no Java or TypeScript code to unit-test. Verification is integration/manual only.
- [ ] Smoke test script: `docker/smoke-test.sh` — optional, could `curl` actuator health and postgres after compose up

---

## Security Domain

Security enforcement is enabled (not explicitly set to false in config.json).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | Trial auth handled by existing Spring Security + JWT |
| V3 Session Management | No | Existing implementation unchanged |
| V4 Access Control | No | Existing RBAC unchanged |
| V5 Input Validation | No | No new API endpoints |
| V6 Cryptography | Partial | `JWT_SECRET` in `.env.example` must be documented as "change before any real use" |

### Known Threat Patterns for Trial Compose Stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Default credentials in `.env.example` | Info Disclosure | Document clearly: "trial only — change all secrets before production deployment"; `POSTGRES_PASSWORD`, `JWT_SECRET`, MinIO keys |
| postgres exposed on host port | Elevation of Privilege | D-08 mandates no host port exposure for postgres in trial stack — enforce this |
| FORWARDER_API_TOKEN empty in trial | Spoofing | Document that trial stack uses a pre-seeded forwarder token (inserted by demo-seed); clubs must regenerate via Admin UI for real use |
| Seed data contains fictional racer email addresses | Privacy | Use `@example.com` addresses only — no real person data |

---

## Open Questions (RESOLVED)

1. **jOOQ generated sources in version control** — RESOLVED
   - What we know: `bootJar` triggers `generateJooq` which requires live postgres (Testcontainers). Docker build has no Docker socket by default.
   - Resolution: Plan 03 Task 0 generates jOOQ sources on the host (where Docker is available) and commits them to `app/src/generated/jooq/` (outside the gitignored `build/` tree). The app Dockerfile builds with `-x generateJooq` flag.

2. **D-06 replay file vs generative mode** — RESOLVED (user confirmed)
   - What we know: D-06 specifies a `.txt` replay file. `PlaybackMode.replay()` does not loop — it plays once and returns.
   - Resolution: User confirmed generative mode (`SimulatorMain --mode=generative`) is acceptable. It loops forever by design, uses the 8 seeded transponder IDs explicitly, and produces identical live timing output. SC-3's "recorded file" wording is relaxed accordingly.

3. **FORWARDER_API_TOKEN bootstrapping** — RESOLVED
   - Resolution: Fixed demo token `DEMO-FORWARDER-TOKEN-CHANGE-BEFORE-PRODUCTION` used across seed SQL, compose default, and `.env.example`. Documented as requiring regeneration before any real deployment.

4. **MinIO in trial stack** — RESOLVED
   - Resolution: MinIO included in trial stack (internal bridge only, no host port) for full wizard evaluation including logo uploads (SC-4).

5. **`service_completed_successfully` availability** — RESOLVED
   - Resolution: Docker Compose v5.1.3 confirmed on dev machine [VERIFIED]. Minimum version (v2.1+) documented in `.env.example` header. The condition is required for correct seed ordering.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | jOOQ generated sources are gitignored, requiring `-x generateJooq` in Dockerfile | Pitfall 1, Open Questions | If already committed, no issue; if gitignored and not addressed, Docker build fails |
| A2 | Generative mode satisfies D-06 intent (3-minute loop, 8 transponders) | Pattern 4 | If replay file is strictly required, PlaybackMode needs a loop wrapper (extra task) |
| A3 | `rhasspy/wyoming-piper:latest` is acceptable for trial without pinning | Standard Stack | Version drift could break TTS; pinning a digest is safer but adds maintenance |
| A4 | Fixed demo FORWARDER_API_TOKEN in `.env.example` is acceptable | Open Questions | If a rotatable token is required, seed must generate one dynamically (more complex) |
| A5 | `docker/metadata-action@v5` and `docker/build-push-action@v6` are current stable versions | Pattern 6 | Version numbers may be outdated; verify at planning time from marketplace |
| A6 | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` env vars override the profile-based datasource config | Service skeleton | Spring Boot maps these env vars to datasource config; if profile is not set, base `application.yml` has no datasource config — a profile or env vars must be supplied |

**If this table is empty:** N/A — 6 assumptions logged above.

---

## Sources

### Primary (HIGH confidence)
- `forwarder/src/main/java/.../FakeDecoderServer.java` — confirmed SimulatorMain exists with generative and playback modes [VERIFIED: codebase]
- `forwarder/src/main/java/.../SimulatorMain.java` — confirmed CLI args: `--mode`, `--port`, `--transponders`, `--interval-ms`, `--jitter-ms`, `--file` [VERIFIED: codebase]
- `forwarder/src/main/java/.../ForwarderConfig.java` — confirmed `load(Path)` and `loadDefault()` exist; main() calls `loadDefault()` only [VERIFIED: codebase]
- `app/src/main/resources/application.yml` — confirmed env var surface: `JWT_SECRET`, `STORAGE_*`, `TTS_*`, `app.grpc.port` [VERIFIED: codebase]
- `docker-compose.yml` — confirmed dev stack services: postgres, mailpit, minio, piper [VERIFIED: codebase]
- `forwarder/src/main/resources/samples/sample-passings.dump` — confirmed 27 lines, 3 transponders only [VERIFIED: codebase]
- Docker Hub API — confirmed `eclipse-temurin:21-jre-alpine` tag exists [VERIFIED: curl Docker Hub v2 API]
- `docker --version` → Docker 26.1.5; `docker compose version` → v5.1.3 [VERIFIED: env probe]

### Secondary (MEDIUM confidence)
- nginx WebSocket proxying — standard well-documented pattern [CITED: https://nginx.org/en/docs/http/websocket.html]
- Spring Boot actuator health endpoint at `/actuator/health` — confirmed Spring Boot Actuator on classpath [VERIFIED: app/build.gradle.kts `spring-boot-starter-actuator`]

### Tertiary (LOW confidence)
- GitHub Actions action versions (`docker/login-action@v3`, `docker/build-push-action@v6`, `docker/metadata-action@v5`) — training knowledge, not verified in this session [ASSUMED]
- `service_completed_successfully` condition — training knowledge; confirmed supported in Compose Spec but not verified against current Docker Compose v5.1.3 docs in this session [ASSUMED]

---

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH — all images verified against Docker Hub or existing codebase
- Architecture: HIGH — all components already exist in codebase; topology follows locked decisions
- Pitfalls: HIGH (codebase-derived) / MEDIUM (Docker/nginx best practices)
- Open Questions: MEDIUM — require planner or user confirmation before task execution

**Research date:** 2026-05-16
**Valid until:** 2026-06-16 (stable infrastructure ecosystem; GitHub Actions action versions may increment sooner)
