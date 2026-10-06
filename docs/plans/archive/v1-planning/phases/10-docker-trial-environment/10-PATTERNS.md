# Phase 10: Docker Trial Environment - Pattern Map

**Mapped:** 2026-05-16
**Files analyzed:** 12 new/modified files
**Analogs found:** 9 / 12 (3 have no codebase analog — infrastructure-only)

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `docker-compose.trial.yml` | config | request-response | `docker-compose.yml` | role-match (same tool, different scope) |
| `docker-compose.ghcr.yml` | config | request-response | `docker-compose.yml` | role-match |
| `.env.example` | config | — | `forwarder/src/main/resources/forwarder.properties` | partial (env-var surface reference) |
| `docker/app/Dockerfile` | config | batch (build) | `forwarder/build.gradle.kts` + `Makefile` `build` target | partial (Gradle build pattern) |
| `docker/forwarder/Dockerfile` | config | batch (build) | `forwarder/build.gradle.kts` `application` plugin | partial |
| `docker/fake-decoder/Dockerfile` | config | batch (build) | `forwarder/build.gradle.kts` `runSimulator` task | partial |
| `docker/frontend/Dockerfile` | config | batch (build) | `frontend/package.json` + `Makefile` `ui-build` target | partial |
| `docker/nginx/nginx.conf` | config | request-response | none — no nginx in codebase | none |
| `docker/seed/Dockerfile` | config | batch | `docker-compose.yml` postgres service | partial |
| `docker/seed/seed.sql` | utility | CRUD | `app/src/main/resources/db/migration/V1__create_users_and_roles.sql` | role-match (SQL, same schema) |
| `docker/forwarder/entrypoint.sh` | utility | transform | `forwarder/src/main/java/.../ForwarderApplication.java` | partial (wraps same process) |
| `.github/workflows/publish-trial-images.yml` | config | batch | none — no CI workflows exist | none |

---

## Pattern Assignments

### `docker-compose.trial.yml` (config, standalone trial stack)

**Analog:** `docker-compose.yml` (lines 1–57)

**Dev compose — exact service definitions to reuse verbatim (copy these configs):**
```yaml
# docker-compose.yml — piper service (lines 43–52): copy verbatim into trial stack
piper:
  image: rhasspy/wyoming-piper:latest
  container_name: rctiming-piper
  volumes:
    - piper_data:/data
  command: >
    --voice en_GB-alan-medium
    --voice en_GB-cori-high
    --voice en_US-amy-medium
  ports:
    - "10200:10200"

# docker-compose.yml — minio healthcheck pattern (lines 37–41): apply same pattern to postgres
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**Critical differences from dev compose (do NOT copy these behaviours):**
- Dev compose exposes postgres port 5432 to host. Trial compose must NOT expose postgres (D-08).
- Dev compose has no app/frontend/forwarder/fake-decoder services — these are all new in trial.
- Dev compose has no `healthcheck` on postgres — trial compose must add one.
- Dev compose uses `rctiming_dev` as database name. Trial compose uses `rctiming`.

**Trial compose service dependency chain:**
```yaml
# Copy this dependency ordering exactly — order matters for seed idempotency
postgres (healthcheck: pg_isready)
  ↓ condition: service_healthy
demo-seed (restart: "no")
  ↓ condition: service_completed_successfully
app (healthcheck: curl /actuator/health)
  ↓ condition: service_healthy
frontend/nginx (ports: - "${HOST_PORT:-80}:80")

fake-decoder (no deps — starts independently)
  ↓ condition: service_started
forwarder (depends_on: app + fake-decoder)
```

**postgres healthcheck pattern — use `pg_isready` not sleep:**
```yaml
# RESEARCH.md Pattern 3 — confirmed working with postgres:16-alpine
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U rctiming -d rctiming"]
  interval: 5s
  timeout: 3s
  retries: 10
```

**app healthcheck — Spring Boot actuator (confirmed at `/actuator/health` from `app/build.gradle.kts` line with `spring-boot-starter-actuator`):**
```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -sf http://localhost:8080/actuator/health | grep -q UP || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 12
  start_period: 90s
```

**Note — `restart: "no"` must be quoted:** YAML 1.1 parses unquoted `no` as boolean false. Always: `restart: "no"`.

**Note — `minio` in trial stack:** Include MinIO (same config as dev) per Open Question 4 recommendation — clubs will want to upload a logo during setup wizard evaluation.

---

### `docker-compose.ghcr.yml` (config, GHCR pre-built variant)

**Analog:** `docker-compose.trial.yml` (just written above) — identical structure, different image sources.

**Only difference:** Replace every `build:` block with `image:` referencing GHCR:
```yaml
# docker-compose.trial.yml has:
app:
  build:
    context: .
    dockerfile: docker/app/Dockerfile

# docker-compose.ghcr.yml replaces with:
app:
  image: ghcr.io/oggthemiffed/rctimingcontrol/app:${RCTIMING_VERSION:?Set RCTIMING_VERSION in .env}
```

**All four GHCR image names (from CONTEXT.md specifics):**
```
ghcr.io/oggthemiffed/rctimingcontrol/app:v{tag}
ghcr.io/oggthemiffed/rctimingcontrol/frontend:v{tag}
ghcr.io/oggthemiffed/rctimingcontrol/forwarder:v{tag}
ghcr.io/oggthemiffed/rctimingcontrol/fake-decoder:v{tag}
```

**Use `${RCTIMING_VERSION:?...}` (not a default)** to fail fast if no version pinned — D-09 forbids `latest`.

---

### `.env.example` (config, env-var surface)

**Analog 1:** `app/src/main/resources/application.yml` (lines 1–33) — authoritative env-var list for the app.

**All env vars the app reads (extract from application.yml):**
```yaml
# application.yml lines 11-33 — these become .env.example keys
JWT_SECRET          # line 12: app.jwt.secret
STORAGE_ENDPOINT    # line 20: storage.endpoint
STORAGE_ACCESS_KEY  # line 21: storage.accessKey
STORAGE_SECRET_KEY  # line 22: storage.secretKey
STORAGE_REGION      # line 23: storage.region
STORAGE_BUCKET      # line 24: storage.bucket
STORAGE_PUBLIC_BASE_URL  # line 25: storage.publicBaseUrl
TTS_ENDPOINT        # line 28: tts.endpoint
TTS_DEFAULT_VOICE   # line 29: tts.defaultVoice
TTS_ENABLED         # line 30: tts.enabled
TTS_LOCALES         # line 33: tts.locales
```

**Analog 2:** `forwarder/src/main/resources/forwarder.properties` (lines 1–6) — forwarder config keys:
```properties
# forwarder/src/main/resources/forwarder.properties — all six keys
forwarder.api-token=KM6D4RoIUe4dI1_5y1Ka69fnozo1CmwLjsLQy36AOd8
forwarder.decoder.host=localhost
forwarder.decoder.port=5100
forwarder.grpc.host=localhost
forwarder.grpc.port=9090
forwarder.grpc.plaintext=true
```
These map to the `FORWARDER_*` env vars used by `docker/forwarder/entrypoint.sh`.

**Additional .env.example keys not in application.yml:**
```
POSTGRES_PASSWORD     # used by postgres service and Spring datasource
MINIO_ROOT_USER       # dev compose line 28: MINIO_ROOT_USER
MINIO_ROOT_PASSWORD   # dev compose line 29: MINIO_ROOT_PASSWORD
HOST_PORT             # nginx host port (default 80, D-08)
FORWARDER_API_TOKEN   # pre-shared token — must match seed SQL value in forwarder_token table
RCTIMING_VERSION      # ghcr.yml only — semver tag, e.g. v1.0.0
```

**Security note:** All secrets in `.env.example` must carry a comment: `# TRIAL ONLY — change before any real deployment`.

---

### `docker/app/Dockerfile` (config, multi-stage Java build)

**Analog:** `Makefile` `build` target (lines ~130–137) and `app/build.gradle.kts` jOOQ config.

**Critical constraint from `app/build.gradle.kts` line 196:** jOOQ generated sources output dir is `build/generated-sources/jooq`. This directory is gitignored (`.gitignore` line: `build/`). The Dockerfile MUST pass `-x generateJooq` — confirmed by `Makefile` `test-fast` target and `build` target which checks for `JOOQ_GENERATED` first.

**Gradle bootJar task name** (from `forwarder/build.gradle.kts` — app uses same Spring Boot plugin):
```bash
./gradlew :app:bootJar -x test -x generateJooq --no-daemon
```

**Output jar location** (from `app/build.gradle.kts` Spring Boot plugin pattern):
```
app/build/libs/app-*.jar   # Spring Boot names jar after project name + version
```
Version is `0.0.1-SNAPSHOT` from `build.gradle.kts` line 10: `version = "0.0.1-SNAPSHOT"`. Use wildcard `*.jar` in COPY.

**Multi-stage pattern:**
```dockerfile
# Stage 1: Builder — copy both subprojects (forwarder shares proto classes with app)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY buildSrc/ buildSrc/
COPY app/ app/
COPY forwarder/ forwarder/
COPY gradle/ gradle/
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && \
    ./gradlew :app:bootJar -x test -x generateJooq --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/app/build/libs/*.jar app.jar
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why `settings.gradle.kts` line 2 requires both subprojects:** `include(":app", ":forwarder")` — Gradle resolves the multi-project build from root. Proto files are shared. Both directories must be in the build context.

---

### `docker/forwarder/Dockerfile` (config, multi-stage Java build)

**Analog:** `forwarder/build.gradle.kts` — `application` plugin (line 39: `mainClass.set("dev.monkeypatch.rctiming.forwarder.ForwarderApplication")`).

**Key difference from app Dockerfile:** forwarder uses `application` plugin, not Spring Boot. Use `installDist` task, not `bootJar`:
```bash
./gradlew :forwarder:installDist -x test --no-daemon
# Output: forwarder/build/install/forwarder/
```

Alternatively build a fat jar manually — but `installDist` is cleaner with the `application` plugin.

**Entrypoint:** The container uses `docker/forwarder/entrypoint.sh` (see below) rather than a direct `java -jar`. The Dockerfile ENTRYPOINT delegates to the shell script.

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY buildSrc/ buildSrc/
COPY app/ app/
COPY forwarder/ forwarder/
COPY gradle/ gradle/
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && \
    ./gradlew :forwarder:installDist -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/forwarder/build/install/forwarder/ .
COPY docker/forwarder/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
```

---

### `docker/fake-decoder/Dockerfile` (config, multi-stage Java build)

**Analog:** `forwarder/build.gradle.kts` `runSimulator` task (line 43–49):
```kotlin
// forwarder/build.gradle.kts lines 43-49
tasks.register<JavaExec>("runSimulator") {
    group = "application"
    description = "Run the AMB RC-4 TCP decoder simulator"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.monkeypatch.rctiming.forwarder.simulator.SimulatorMain")
}
```

**Main class:** `dev.monkeypatch.rctiming.forwarder.simulator.SimulatorMain`

**CLI args pattern** (from `SimulatorMain.java` lines 37–68 — confirmed `--mode=generative` loops forever):
```
--mode=generative --port=5100 --transponders=101,102,103,104,105,106,107,108 --interval-ms=13000 --jitter-ms=2500
```

**Dockerfile:** Same multi-stage as forwarder but different ENTRYPOINT:
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
# ... same COPY + gradlew as forwarder Dockerfile ...
RUN ./gradlew :forwarder:installDist -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/forwarder/build/install/forwarder/ .
# Note: SimulatorMain is in the same installDist output — select via CMD
ENTRYPOINT ["bin/forwarder"]
# Override mainClass via compose `command:` block
```

**Alternative — simpler:** Build a separate shadow jar or use the same `installDist` and override the main class via a wrapper script. The cleanest approach is a small shell wrapper `simulate.sh`:
```bash
#!/bin/sh
exec java -cp /app/lib/'*' dev.monkeypatch.rctiming.forwarder.simulator.SimulatorMain "$@"
```

---

### `docker/frontend/Dockerfile` (config, multi-stage Node build)

**Analog:** `Makefile` `ui-build` target (line ~175: `cd frontend && npm run build`) and `frontend/package.json`.

**Build output confirmed:** `frontend/dist/` — confirmed gitignored in `.gitignore` line `frontend/dist/`.

**Vite build command** (from Makefile `ui-build`): `npm run build`

**Node modules cache:** `frontend/node_modules/` exists locally. In Docker, install from `package-lock.json`:
```dockerfile
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

**Note on build context:** Context is repo root (`.`), so paths are `frontend/package*.json` not `package*.json`.

---

### `docker/nginx/nginx.conf` (config, WebSocket reverse-proxy)

**No codebase analog.** No nginx configuration exists anywhere in this repository.

**Use RESEARCH.md Pattern 2 and Code Example (lines 522–578) as the source pattern.** Key non-negotiable elements:

1. `map $http_upgrade $connection_upgrade` directive — required for WebSocket
2. `try_files $uri $uri/ /index.html` — required for React Router deep links (SPA fallback)
3. `proxy_read_timeout 3600s` on `/ws` location — default 60s drops STOMP connections mid-race
4. `/assets/` location with `expires 1y; Cache-Control: public, immutable` — Vite fingerprints asset filenames, safe to cache aggressively
5. `/api/` proxy_pass to `http://app:8080` with standard proxy headers

The nginx container is the `frontend` service in compose — it serves both the built SPA (`/usr/share/nginx/html/`) and proxies backend traffic.

---

### `docker/seed/Dockerfile` (config, one-shot seed runner)

**Analog:** `docker-compose.yml` postgres service (lines 2–8) — same `postgres:16-alpine` base image.

**Pattern:** Use `postgres:16-alpine` as base — it includes `psql` and `pg_isready`. The seed Dockerfile just copies `seed.sql` and provides an entrypoint that runs `psql`:
```dockerfile
FROM postgres:16-alpine
COPY seed.sql /seed.sql
ENTRYPOINT ["sh", "-c", "psql -h $PGHOST -U $PGUSER -d $PGDATABASE -f /seed.sql"]
```

Env vars `PGHOST`, `PGUSER`, `PGDATABASE`, `PGPASSWORD` are standard libpq environment variables honoured by `psql` automatically — no extra flags needed.

---

### `docker/seed/seed.sql` (utility, idempotent seed data)

**Analog:** All 26 Flyway migration files — same PostgreSQL dialect, same schema, same table names.

**Table name reference** (read from migration files):
- `V1__create_users_and_roles.sql`: tables `users`, `user_roles`, `refresh_tokens`, `password_reset_tokens`
- `V2__create_club.sql`: tables `club_profiles`, `governing_body_affiliations`
- `V3__create_tracks.sql`: table `tracks` (inferred)
- `V4__create_racing_classes.sql`: table `racing_classes` (inferred)
- `V5__create_race_formats.sql`: table `race_formats` (inferred)
- `V21__create_forwarder_token.sql`: table `forwarder_token` with columns `id`, `token_hash`, `status`, `generated_at`, `revoked_at`
- `V26__forwarder_token_plaintext.sql`: adds column `token_value VARCHAR(255)` to `forwarder_token`

**FK insertion order** (derived from migration sequence — V1 first, then V2, V3... preserves FK deps):
```sql
-- 1. users (no FK deps)
-- 2. user_roles (FK → users.id)
-- 3. club_profiles
-- 4. governing_body_affiliations
-- 5. tracks (FK → club_profiles.id — verify from V3)
-- 6. racing_classes (FK → club_profiles.id — verify from V4)
-- 7. race_formats (FK → club_profiles.id — verify from V5)
-- 8. events (FK → tracks.id, club_profiles.id — verify from V12)
-- 9. event_classes (FK → events.id, race_formats.id, racing_classes.id)
-- 10. cars (FK → users.id — V9)
-- 11. transponders (FK → users.id — V11)
-- 12. entries (FK → event_classes.id, users.id, cars.id, transponders.id — V13)
-- 13. championships (FK → club_profiles.id — V16)
-- 14. championship rounds/standings (FK → championships.id)
-- 15. result_snapshots (FK → races.id — V19)
-- 16. forwarder_token (no FK — insert trial token last)
```

**Idempotency guard pattern** (standard PostgreSQL PL/pgSQL — no codebase analog, but confirmed from RESEARCH.md Code Example):
```sql
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM club_profiles WHERE name = 'Wyvern RC Club') THEN
    RAISE NOTICE 'Seed data already present — skipping';
    RETURN;
  END IF;
  -- all inserts go here
END $$;
```

**Password hashes:** Racer passwords must be BCrypt-hashed. Use a pre-computed BCrypt hash for a known trial password (e.g., `trial123`). Do NOT generate hashes at seed time — SQL has no BCrypt function. Pre-compute using Spring Security's `BCryptPasswordEncoder` and hardcode the result.

**Forwarder token seed:** Insert a plaintext token into `forwarder_token` matching the `FORWARDER_API_TOKEN` in `.env.example`:
```sql
INSERT INTO forwarder_token (token_hash, token_value, status, generated_at)
VALUES ('trial-token-hash-not-used', 'DEMO-TOKEN-CHANGE-BEFORE-PRODUCTION', 'ACTIVE', NOW());
```
Note: V26 added `token_value` for direct comparison. V21 has `token_hash` for BCrypt — but V26 migration adds the plaintext column and the app now validates against `token_value` directly. Confirm with `ForwarderTokenService` at implementation time.

---

### `docker/forwarder/entrypoint.sh` (utility, config-file generator)

**Analog:** `forwarder/src/main/java/.../ForwarderApplication.java` (lines 24–50) — wraps the same process.

**ForwarderConfig.java confirmed** (lines 52–74): `ForwarderConfig.load(Path)` is a public static method accepting a filesystem path. `ForwarderApplication.main()` (line 25) calls `ForwarderConfig.loadDefault()` only — it does NOT currently accept `--config-file`.

**Required code change:** `ForwarderApplication.java` line 25 must be extended:
```java
// ForwarderApplication.java — replace line 25 with:
ForwarderConfig cfg = (args.length > 0 && args[0].startsWith("--config-file="))
    ? ForwarderConfig.load(Path.of(args[0].substring("--config-file=".length())))
    : ForwarderConfig.loadDefault();
```
This is the only Java source change in Phase 10.

**Entrypoint script pattern** (shell, no codebase analog — pure Docker pattern):
```sh
#!/bin/sh
# docker/forwarder/entrypoint.sh
# Writes forwarder.properties from env vars, then launches the JAR
set -e
cat > /tmp/forwarder.properties <<PROPS
forwarder.api-token=${FORWARDER_API_TOKEN}
forwarder.decoder.host=${FORWARDER_DECODER_HOST:-fake-decoder}
forwarder.decoder.port=${FORWARDER_DECODER_PORT:-5100}
forwarder.grpc.host=${FORWARDER_GRPC_HOST:-app}
forwarder.grpc.port=${FORWARDER_GRPC_PORT:-9090}
forwarder.grpc.plaintext=${FORWARDER_GRPC_PLAINTEXT:-true}
PROPS
exec /app/bin/forwarder --config-file=/tmp/forwarder.properties
```

**Key detail — `exec`:** The shell must `exec` the Java process so it becomes PID 1 and receives Docker stop signals correctly. Without `exec`, `docker stop` sends SIGTERM to the shell wrapper, not the JVM.

---

### `.github/workflows/publish-trial-images.yml` (config, CI/CD)

**No codebase analog.** No existing GitHub Actions workflows in this repository.

**Use RESEARCH.md Pattern 6 (lines 357–394) as source.** Key structural requirements:

**Trigger:** `push: tags: ['v*']` — D-09 mandates version-tag-only triggering.

**Permissions block** (required for GHCR write):
```yaml
permissions:
  contents: read
  packages: write
```

**Matrix strategy for four images** (cleaner than four separate jobs):
```yaml
strategy:
  matrix:
    include:
      - image: app
        dockerfile: docker/app/Dockerfile
      - image: frontend
        dockerfile: docker/frontend/Dockerfile
      - image: forwarder
        dockerfile: docker/forwarder/Dockerfile
      - image: fake-decoder
        dockerfile: docker/fake-decoder/Dockerfile
```

**Standard actions** (from RESEARCH.md Pattern 6 — treat as LOW confidence on exact versions, verify at implementation time from marketplace):
```yaml
- uses: actions/checkout@v4
- uses: docker/login-action@v3         # GHCR auth
- uses: docker/metadata-action@v5      # Extracts semver tag from git ref
- uses: docker/build-push-action@v6   # Build + push with GHA cache
```

**Image name pattern per matrix entry:**
```yaml
images: ghcr.io/oggthemiffed/rctimingcontrol/${{ matrix.image }}
tags: type=semver,pattern={{version}}
```

**BuildKit GHA cache:**
```yaml
cache-from: type=gha
cache-to: type=gha,mode=max
```

---

## Shared Patterns

### Gradle Build Context (applies to: `docker/app/Dockerfile`, `docker/forwarder/Dockerfile`, `docker/fake-decoder/Dockerfile`)

**Source:** `settings.gradle.kts` (line 2) and `build.gradle.kts` (lines 1–12).

All Java Dockerfiles use repo root as build context because `settings.gradle.kts` defines a multi-project build. Every Dockerfile must COPY:
```dockerfile
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY buildSrc/ buildSrc/
COPY gradle/ gradle/
COPY app/ app/
COPY forwarder/ forwarder/
```

The `buildSrc/` directory contains the Kotlin DSL precompiled script plugins. Omitting it breaks the Gradle build silently.

### jOOQ Generated Sources Constraint (applies to: `docker/app/Dockerfile`)

**Source:** `.gitignore` line `build/` (confirms `build/generated-sources/jooq/` is gitignored) and `app/build.gradle.kts` line 196 (`directory = "build/generated-sources/jooq"`).

The `app` Dockerfile MUST pass `-x generateJooq`. Without pre-committed generated sources and without Docker-in-Docker, `generateJooq` will fail. Resolution options in priority order:
1. Pass `-x generateJooq` and commit generated sources to `app/src/generated/jooq/` (move from gitignored `build/`)
2. Use Makefile pattern: build jOOQ sources in a separate CI step before Docker build

The planner must add a task to commit jOOQ generated sources or adjust `.gitignore` accordingly.

### Postgres Environment Variables (applies to: `docker-compose.trial.yml`, `docker/seed/Dockerfile`)

**Source:** `docker-compose.yml` (lines 5–8) — dev compose pattern:
```yaml
environment:
  POSTGRES_DB: rctiming_dev      # trial uses: rctiming
  POSTGRES_USER: rctiming
  POSTGRES_PASSWORD: rctiming
```

`psql` in the seed container uses standard libpq env vars: `PGHOST`, `PGUSER`, `PGPASSWORD`, `PGDATABASE` — no connection string needed.

### Spring Boot Env-Var Override Pattern (applies to: `docker-compose.trial.yml`, `.env.example`)

**Source:** `app/src/main/resources/application.yml` (lines 1–33) — Spring Boot resolves `${ENV_VAR:default}` notation.

Spring Boot also maps `SPRING_DATASOURCE_URL` → `spring.datasource.url` automatically via relaxed binding. The trial compose does NOT need a Spring profile — setting the datasource env vars is sufficient:
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/rctiming
SPRING_DATASOURCE_USERNAME: rctiming
SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
```

---

## No Analog Found

Files with no close match in the codebase (planner should use RESEARCH.md patterns instead):

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `docker/nginx/nginx.conf` | config | request-response | No nginx in codebase — pure infrastructure pattern from nginx docs |
| `.github/workflows/publish-trial-images.yml` | config | batch | No existing CI workflows — use RESEARCH.md Pattern 6 |
| `docker/forwarder/entrypoint.sh` | utility | transform | Shell scripting with no codebase analog — Docker-specific concern |

---

## Metadata

**Analog search scope:** `/home/david/git/java/RCTimingControl/` — all subdirectories
**Files scanned:** 15 source files read, 26 migration files enumerated
**Key codebase files read:**
- `/home/david/git/java/RCTimingControl/docker-compose.yml`
- `/home/david/git/java/RCTimingControl/app/src/main/resources/application.yml`
- `/home/david/git/java/RCTimingControl/forwarder/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderApplication.java`
- `/home/david/git/java/RCTimingControl/forwarder/src/main/java/dev/monkeypatch/rctiming/forwarder/config/ForwarderConfig.java`
- `/home/david/git/java/RCTimingControl/forwarder/src/main/java/dev/monkeypatch/rctiming/forwarder/simulator/SimulatorMain.java`
- `/home/david/git/java/RCTimingControl/forwarder/src/main/resources/forwarder.properties`
- `/home/david/git/java/RCTimingControl/forwarder/build.gradle.kts`
- `/home/david/git/java/RCTimingControl/settings.gradle.kts`
- `/home/david/git/java/RCTimingControl/build.gradle.kts`
- `/home/david/git/java/RCTimingControl/.gitignore`
- `/home/david/git/java/RCTimingControl/Makefile`
- `/home/david/git/java/RCTimingControl/app/src/main/resources/db/migration/V1__create_users_and_roles.sql`
- `/home/david/git/java/RCTimingControl/app/src/main/resources/db/migration/V2__create_club.sql`
- `/home/david/git/java/RCTimingControl/app/src/main/resources/db/migration/V21__create_forwarder_token.sql`
- `/home/david/git/java/RCTimingControl/app/src/main/resources/db/migration/V26__forwarder_token_plaintext.sql`

**Pattern extraction date:** 2026-05-16
