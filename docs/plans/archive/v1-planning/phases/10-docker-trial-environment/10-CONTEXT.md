# Phase 10: Docker Trial Environment - Context

**Gathered:** 2026-05-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver a `docker compose -f docker-compose.trial.yml up` experience that allows any club to evaluate the complete system locally — including live timing from a fake decoder — with no developer involvement beyond copying a `.env.example` file. A companion `docker-compose.ghcr.yml` pulls pre-built images from GHCR so non-technical clubs don't need to build from source. GHCR images are published automatically on version tags via GitHub Actions.

This phase is infrastructure and tooling only. No new application features are added.

</domain>

<decisions>
## Implementation Decisions

### Frontend Serving
- **D-01:** nginx container serves the built Vite SPA and reverse-proxies `/api` and `/ws` to Spring Boot on port 8080 internally. Single exposed port: **80**. Clubs open `http://localhost` — no port number in the URL.

### Trial Stack Structure
- **D-02:** Separate `docker-compose.trial.yml` — the existing `docker-compose.yml` (dev stack: postgres, mailpit, minio, piper) is left unchanged. The trial stack is a standalone file with everything self-contained. No risk of dev config leaking into trial builds.
- **D-03:** Companion `docker-compose.ghcr.yml` for pre-built GHCR image variant (targets non-technical clubs).

### Demo Seed Data
- **D-04:** "Active club mid-season" scenario — a sample club with 2 tracks, 3 race formats, 8 racer accounts, a 6-round championship in progress (3 rounds complete with results), and an upcoming event with entries open. Gives evaluators a realistic, populated system to explore immediately.
- **D-05:** Seed data runs as a one-shot container (`restart: no`) on first boot only — once the database is populated it won't re-run. Seeding is idempotent (checks if data already exists).

### Fake Decoder
- **D-06:** Bundled synthetic `.txt` RC-4 text file with 8 transponder IDs matching the seeded racer entries, replayed on a 3-minute loop. Fully self-contained — no club data needed, always works.
- **D-07:** The fake decoder container is a standalone Java or lightweight process (can reuse the `FakeDecoderServer` class from Phase 5 tests, packaged as a runnable jar or slim container). It connects to the forwarder using the same RC-4 text protocol on port 5100.

### Port / Networking
- **D-08:** nginx listens on host port **80** (configurable via `HOST_PORT` env var for clubs that already have something on 80). Internal networking is Docker bridge — Spring Boot on 8080, forwarder gRPC on 9090, Piper on 10200 — none exposed to host except nginx:80 and postgres (dev-only).

### GHCR Image Publishing
- **D-09:** GitHub Actions workflow triggers **on `v*` version tags only** (e.g. `v1.0.0`). Publishes images for: `app`, `frontend` (nginx-based), `forwarder`, `fake-decoder`. `docker-compose.ghcr.yml` pins to a specific tag. No `latest` image to avoid instability.

### Claude's Discretion
- Gradle multi-project build configuration for producing the app jar (standard Spring Boot `bootJar` task)
- Dockerfile base images (e.g. `eclipse-temurin:21-jre-alpine` for Spring Boot, `node:20-alpine` for Vite build)
- nginx config specifics (gzip, WebSocket upgrade headers, cache headers for static assets)
- Health check intervals and startup order (`depends_on` conditions)
- Whether to use BuildKit cache mounts in Dockerfiles

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Existing Docker / Dev Stack
- `docker-compose.yml` — Dev stack (postgres, mailpit, minio, piper). Trial stack must NOT modify this file.

### Application Config (env-var surface)
- `app/src/main/resources/application.yml` — All env-var-driven config (`JWT_SECRET`, `STORAGE_*`, `TTS_*`, `app.grpc.port`). Trial `.env.example` must cover these.

### Forwarder
- `forwarder/` — Separate Gradle submodule. Needs its own Dockerfile. Connects to fake decoder on port 5100 and streams to app gRPC on 9090.

### Fake Decoder (Phase 5 reference)
- `forwarder/src/test/java/.../FakeDecoderServer.java` — Existing RC-4 text emitter used in integration tests. Trial fake decoder container is derived from this.

### Protocol Reference
- `docs/AMB_DECODER_PROTOCOL.md` — RC-4 text format for the bundled synthetic `.txt` replay file.

### Phase 10 Roadmap Entry
- `.planning/ROADMAP.md` §Phase 10 — Success criteria (SC-1 through SC-5).

No external specs beyond the above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `FakeDecoderServer` (forwarder test class): Emits RC-4 text lines over TCP on a configurable port. Can be extracted into a standalone `fake-decoder` module with a `main()` entry point that reads a replay file path from an env var and loops it.
- `docker-compose.yml` piper service: `rhasspy/wyoming-piper:latest` with `en_GB-alan-medium` — trial stack can reuse this image definition verbatim.
- `app/src/main/resources/application.yml`: All connection strings are already env-var-driven. Trial compose just needs to set the right values.

### Established Patterns
- Gradle multi-project build (`settings.gradle.kts`): `app` and `forwarder` are separate subprojects. Each needs its own Dockerfile that runs `./gradlew :app:bootJar` or `./gradlew :forwarder:bootJar`.
- Flyway migrations run at app startup — no separate migration step needed in compose.
- Spring Boot actuator health endpoint available at `/actuator/health` — useful for compose `healthcheck`.

### Integration Points
- nginx → Spring Boot: proxy `/api/**` and `/ws/**` to `http://app:8080`; serve `/` from built frontend assets.
- forwarder → fake-decoder: forwarder connects outbound to `fake-decoder:5100` via RC-4 text protocol.
- forwarder → app: forwarder streams to `app:9090` via gRPC bidirectional streaming.
- demo-seed → postgres: one-shot container runs after postgres is healthy, before app starts.

</code_context>

<specifics>
## Specific Ideas

- Replay file: synthetic RC-4 `.txt` with 8 transponder IDs matching seeded entries, 3-minute loop cadence
- Seed scenario: "Wyvern RC Club" (fictional name), Rivermead Circuit + Parklands Arena tracks, 3 formats (Timed 5-min / Bump-up Finals / Points Finals), championship named "2026 Wyvern Winter Series"
- GHCR image names: `ghcr.io/oggthemiffed/rctimingcontrol/app:v{tag}`, `.../frontend:v{tag}`, `.../forwarder:v{tag}`, `.../fake-decoder:v{tag}`

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 10-docker-trial-environment*
*Context gathered: 2026-05-16*
