---
phase: 10-docker-trial-environment
reviewed: 2026-05-16T00:00:00Z
depth: standard
files_reviewed: 18
files_reviewed_list:
  - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderApplicationConfigArgTest.java
  - docker/forwarder/Dockerfile
  - docker/forwarder/entrypoint.sh
  - docker/fake-decoder/Dockerfile
  - docker/fake-decoder/simulate.sh
  - forwarder/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderApplication.java
  - docker/seed/seed.sql
  - docker/seed/Dockerfile
  - docker/app/Dockerfile
  - docker/frontend/Dockerfile
  - docker/nginx/nginx.conf
  - docker-compose.trial.yml
  - app/build.gradle.kts
  - .gitignore
  - frontend/src/context/HelpContext.test.tsx
  - .env.example
  - docker-compose.ghcr.yml
  - .github/workflows/publish-trial-images.yml
findings:
  critical: 3
  warning: 6
  info: 3
  total: 12
status: issues_found
---

# Phase 10: Code Review Report

**Reviewed:** 2026-05-16T00:00:00Z
**Depth:** standard
**Files Reviewed:** 18
**Status:** issues_found

## Summary

This phase delivers the Docker trial environment: multi-service compose stacks (build-from-source and GHCR pre-built variants), Dockerfiles for every component, a Piper TTS service, a fake AMB decoder, a demo seed script, nginx reverse proxy, and a GitHub Actions workflow to publish images on version tags.

The infrastructure is functionally coherent, but three blockers exist: the `demo-seed` service in `docker-compose.ghcr.yml` still builds from local source instead of pulling a published image (making the GHCR variant incomplete), the forwarder entrypoint silently writes a literal `${FORWARDER_API_TOKEN}` string into the config file when the env var is unset, and the nginx WebSocket proxy block is missing the `X-Forwarded-Proto` header which breaks STOMP over WSS when put behind a TLS terminator.

---

## Critical Issues

### CR-01: `demo-seed` in `docker-compose.ghcr.yml` builds from source — GHCR variant is broken for users without the repo

**File:** `docker-compose.ghcr.yml:44`
**Issue:** Every other service in the GHCR compose file pulls a pre-built image keyed to `${RCTIMING_VERSION}`. The `demo-seed` service alone retains `build: docker/seed/` from the trial compose file. A user who downloads only `docker-compose.ghcr.yml` and `.env` (the documented install path) has no `docker/seed/` directory, so `docker compose up` fails immediately with a build context error.

The CI workflow (`publish-trial-images.yml`) also does not publish a `seed` image, confirming the intent was to fix this but it was not completed.

**Fix:** Either publish the seed image in CI and reference it here, or embed the seed SQL inline as a bind-mounted config map, or use an `initContainers`-equivalent pattern with a postgres image and an init script volume.

Option A — publish seed image (matches pattern of other services):
```yaml
# In publish-trial-images.yml, add to matrix:
- image: seed
  dockerfile: docker/seed/Dockerfile

# In docker-compose.ghcr.yml:
  demo-seed:
    image: ghcr.io/oggthemiffed/rctimingcontrol/seed:${RCTIMING_VERSION:?Set RCTIMING_VERSION in .env}
    restart: "no"
    environment:
      PGHOST: postgres
      PGUSER: rctiming
      PGPASSWORD: ${POSTGRES_PASSWORD:-rctiming_trial}
      PGDATABASE: rctiming
    depends_on:
      app:
        condition: service_healthy
```

---

### CR-02: Forwarder entrypoint writes a literal `${FORWARDER_API_TOKEN}` string when env var is unset

**File:** `docker/forwarder/entrypoint.sh:4-11`
**Issue:** The heredoc uses single-quoted delimiter (`<<PROPS`, not `<<'PROPS'`), so variable expansion occurs inside it — which is the intended behaviour. However, `FORWARDER_API_TOKEN` has **no default value** (line 5: `${FORWARDER_API_TOKEN}` with no `:-` fallback), unlike every other variable in the same block. If the operator forgets to set `FORWARDER_API_TOKEN`, the shell substitutes an empty string and the forwarder authenticates with a blank token.

In the compose files this is papered over by `${FORWARDER_API_TOKEN:-DEMO-FORWARDER-TOKEN-CHANGE-BEFORE-PRODUCTION}`, but the entrypoint itself provides no defence. A future deployment that sets the env var at a different layer (e.g., Kubernetes secret) but omits the default in the compose override would silently pass an empty token.

**Fix:** Add an explicit guard or a non-silent default:
```sh
#!/bin/sh
set -e
: "${FORWARDER_API_TOKEN:?FORWARDER_API_TOKEN must be set}"
cat > /tmp/forwarder.properties <<PROPS
forwarder.api-token=${FORWARDER_API_TOKEN}
...
PROPS
```
The `:?` expansion aborts the container with a clear error rather than starting with an empty token.

---

### CR-03: nginx WebSocket proxy block missing `X-Forwarded-Proto` header — STOMP breaks under TLS termination

**File:** `docker/nginx/nginx.conf:43-52`
**Issue:** The `/ws` location proxies WebSocket upgrade correctly, but does not forward `X-Forwarded-Proto: https` to the backend. When the trial environment is placed behind any TLS terminator (Cloudflare Tunnel, another nginx, an AWS ALB), the Spring backend sees an `http` scheme and may reject WebSocket upgrade or issue redirect loops if `server.forward-headers-strategy` is set. The STOMP client running in the browser will also fail to connect over `wss://` because the backend constructs absolute URLs using the perceived scheme.

The `/api/` block has the same omission.

**Fix:**
```nginx
location /ws {
    set $upstream_app http://app:8080;
    proxy_pass             $upstream_app;
    proxy_http_version     1.1;
    proxy_set_header       Upgrade $http_upgrade;
    proxy_set_header       Connection $connection_upgrade;
    proxy_set_header       Host $host;
    proxy_set_header       X-Forwarded-Proto $scheme;
    proxy_set_header       X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_read_timeout     3600s;
    proxy_send_timeout     3600s;
}

location /api/ {
    set $upstream_app http://app:8080;
    proxy_pass         $upstream_app;
    proxy_set_header   Host $host;
    proxy_set_header   X-Real-IP $remote_addr;
    proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header   X-Forwarded-Proto $scheme;
}
```

---

## Warnings

### WR-01: `app` service does not depend on `minio-init` completing — bucket may not exist at startup

**File:** `docker-compose.trial.yml:73-77` (same issue in `docker-compose.ghcr.yml:73-75`)
**Issue:** The `app` service depends on `minio` (`service_healthy`) but not on `minio-init` (`service_completed_successfully`). If the Spring Boot application initialises its S3 client and attempts to access the `rctiming` bucket before `minio-init` has created it, the first upload request will fail with a `NoSuchBucketException`. On a fast host the race window is narrow but not zero.

**Fix:** Add `minio-init` as a dependency for `app`:
```yaml
  app:
    depends_on:
      postgres:
        condition: service_healthy
      minio:
        condition: service_healthy
      minio-init:
        condition: service_completed_successfully
```

---

### WR-02: `minio-init` has no `condition` check on its own — it has no healthcheck, so `service_completed_successfully` is required but the service has no restart policy guard

**File:** `docker-compose.trial.yml:30-41` (same in `docker-compose.ghcr.yml:30-41`)
**Issue:** `minio-init` uses `restart: "no"`, which is correct — a one-shot init container should not restart. However, if `mc` exits non-zero (e.g., MinIO API was briefly unavailable despite the healthcheck), there is no retry and the bucket is never created. The `--ignore-existing` flag on `mc mb` means success on re-run, but Docker Compose will not re-run a stopped `restart: "no"` container. The operator would have to manually `docker compose run minio-init` to recover.

**Fix:** Add a retry loop in the entrypoint, or use `restart: on-failure` with a `max-attempts` limit:
```yaml
  minio-init:
    image: minio/mc:latest
    restart: on-failure   # retries up to Compose default; add deploy.restart_policy for fine control
    depends_on:
      minio:
        condition: service_healthy
    entrypoint: >
      /bin/sh -c "
      mc alias set local http://minio:9000 ${MINIO_ROOT_USER:-minioadmin} ${MINIO_ROOT_PASSWORD:-minioadmin} &&
      mc mb --ignore-existing local/rctiming &&
      mc anonymous set download local/rctiming
      "
```

---

### WR-03: `fake-decoder` Dockerfile builds the entire Gradle project twice — duplicates forwarder build

**File:** `docker/fake-decoder/Dockerfile:1-19`
**Issue:** The fake-decoder Dockerfile is identical to the forwarder Dockerfile in its builder stage — it copies and builds the full Gradle project (`app/`, `forwarder/`, `buildSrc/`) just to get the `forwarder` distribution (which contains `SimulatorMain`). When both images are built in the same `docker compose build` or CI matrix job, the Gradle build runs twice in separate, independent build contexts. There is no shared BuildKit cache between the two builder stages because they are separate `docker build` invocations.

This is not a correctness bug but it is a reliability issue: if `SimulatorMain` is not in the `:forwarder:installDist` output (e.g., if it is only wired as a `mainClass` via a task, not as a distribution script), the entrypoint will fail at runtime with `ClassNotFoundException` and the error will not surface until someone actually tries to run the fake-decoder container.

**Fix:** Verify `SimulatorMain` is a declared application entry point in the forwarder `build.gradle.kts` (not just a class) and add an integration smoke-test. Longer-term, consider a shared builder image or a multi-stage build that shares the install directory.

---

### WR-04: `docker/app/Dockerfile` copies a glob `app-*.jar` — fails silently if build produces multiple jars

**File:** `docker/app/Dockerfile:16`
**Issue:** `COPY --from=builder /build/app/build/libs/app-*.jar app.jar` uses a glob. Spring Boot's `bootJar` task produces `app-<version>.jar` and, with default Gradle configuration, also a plain `app-<version>-plain.jar`. If both jars exist in `build/libs/`, Docker's COPY semantics require the destination to be a directory when the source is a multi-file glob — it will fail with `COPY failed: destination must be a directory when copying multiple files`. This typically does not trigger because `bootJar` is the only task run (`-x test`) and the plain jar task is disabled by the Spring Boot plugin by default, but it is fragile: any future addition of a classifier or the plain jar task will silently break the image build.

**Fix:** Reference the jar by its exact versioned name, or configure Gradle to produce a fixed-name artifact:
```kotlin
// In app/build.gradle.kts
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}
```
Then in the Dockerfile:
```dockerfile
COPY --from=builder /build/app/build/libs/app.jar app.jar
```

---

### WR-05: `simulate.sh` passes all `$@` args to `SimulatorMain` but `set -e` won't catch `java` exit codes if the classpath is wrong

**File:** `docker/fake-decoder/simulate.sh:1-4`
**Issue:** The script runs `exec java -cp "/app/lib/*" dev.monkeypatch.rctiming.forwarder.simulator.SimulatorMain "$@"`. If `SimulatorMain` does not exist in the classpath (wrong class name, not packaged), Java exits with code 1 and a `Could not find or load main class` error on stderr — but because `exec` replaces the shell process, Docker will capture this exit code and mark the container as failed. The issue is that there is no startup validation and no informative error. This is a reliability concern for demo users who see an immediate `Exited (1)` with no actionable message in the logs.

Additionally, the Dockerfile for fake-decoder uses the JRE image (`eclipse-temurin:21-jre-alpine`) which is correct, but the `simulate.sh` uses `-cp "/app/lib/*"` which relies on the `installDist` layout placing all jars under `lib/`. If the classpath glob expansion fails in the Alpine shell (which uses `ash`), the application class will not be found.

**Fix:** Add an existence check before `exec`:
```sh
#!/bin/sh
set -e
JAR_COUNT=$(ls /app/lib/*.jar 2>/dev/null | wc -l)
if [ "$JAR_COUNT" -eq 0 ]; then
    echo "ERROR: No jars found in /app/lib/ — build may have failed" >&2
    exit 1
fi
exec java -cp "/app/lib/*" \
  dev.monkeypatch.rctiming.forwarder.simulator.SimulatorMain "$@"
```

---

### WR-06: `piper` service has no `depends_on` from `app` — TTS calls will fail on fast startup

**File:** `docker-compose.trial.yml:70,95-102`
**Issue:** The `app` service references `TTS_ENDPOINT: piper:10200` but has no `depends_on: piper` constraint. Piper has no healthcheck defined in the compose file. On startup, if the Spring application initialises its TTS client eagerly and `piper` is still downloading its voice model data (stored in the `piper_data` volume), TTS calls fail silently or with an error. This is a startup race condition that is invisible to the operator.

This is the same in `docker-compose.ghcr.yml`.

**Fix:** Add a healthcheck to piper and a dependency from app, or accept that TTS is optional and ensure the Spring app handles TTS unavailability gracefully at startup (not just per-request). At minimum, document that TTS may not be available for the first 1-2 minutes.

---

## Info

### IN-01: Known-good BCrypt hash for `trial123` is hardcoded in seed data

**File:** `docker/seed/seed.sql:79`
**Issue:** The seed script hardcodes a BCrypt hash for the demo password `trial123`. This is intentional for a trial environment, but the hash and the cleartext password appear in different places (hash in `seed.sql`, cleartext only implied by comment). The `.env.example` does not document what the demo user credentials are. A user reading only `.env.example` and following the quickstart will not know how to log in.

**Fix:** Add a comment in `.env.example` and/or in `seed.sql` stating: `# Demo users: admin@example.com / dave.racer@example.com / ... — password for all: trial123`.

---

### IN-02: `forwarder_token` row stores the demo token value in plaintext in `token_value` column

**File:** `docker/seed/seed.sql:475-479`
**Issue:** The `forwarder_token` table row stores `'DEMO-FORWARDER-TOKEN-CHANGE-BEFORE-PRODUCTION'` in both `token_hash` and `token_value` columns. The `token_hash` value is `'demo-not-used-for-validation'` which suggests the application validates using `token_hash` in production but is intentionally bypassed here. Storing the plaintext token in `token_value` is acceptable for a demo environment but the column's existence in the schema (if it persists to production) is a latent credential exposure risk.

**Fix:** Add a comment in `seed.sql` noting this row must be deleted and regenerated via Admin UI before any non-trial deployment. Confirm the production code path never reads `token_value` for validation.

---

### IN-03: GitHub Actions workflow publishes on any `v*` tag without branch protection

**File:** `.github/workflows/publish-trial-images.yml:4`
**Issue:** The workflow triggers on `push: tags: ['v*']` from any branch, not just from `master`. A tag pushed from a feature branch (e.g., `v0.9.0-rc1` pushed by accident from a WIP branch) would publish potentially broken images to GHCR under a semver-looking tag, where `docker-compose.ghcr.yml` users would consume them.

**Fix:** Add a branch filter or a job condition:
```yaml
on:
  push:
    tags: ['v*']
    # Optionally restrict to tags on master:
jobs:
  build-and-push:
    if: github.ref_type == 'tag'
    # ...
```
Or use GitHub's protected tags feature to prevent non-maintainers from pushing `v*` tags.

---

_Reviewed: 2026-05-16T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
