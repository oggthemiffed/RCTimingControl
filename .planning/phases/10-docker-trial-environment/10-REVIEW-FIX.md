---
phase: 10-docker-trial-environment
fixed_at: 2026-05-16T00:00:00Z
review_path: .planning/phases/10-docker-trial-environment/10-REVIEW.md
iteration: 1
findings_in_scope: 9
fixed: 8
skipped: 1
status: partial
---

# Phase 10: Code Review Fix Report

**Fixed at:** 2026-05-16T00:00:00Z
**Source review:** .planning/phases/10-docker-trial-environment/10-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 9 (CR-01, CR-02, CR-03, WR-01, WR-02, WR-03, WR-04, WR-05, WR-06)
- Fixed: 8
- Skipped: 1 (WR-03 — verification only, no actionable code change required)

## Fixed Issues

### CR-01: `demo-seed` in `docker-compose.ghcr.yml` builds from source

**Files modified:** `docker-compose.ghcr.yml`, `.github/workflows/publish-trial-images.yml`
**Commit:** 71a6db2
**Applied fix:** Added `seed` to the CI publish matrix in `publish-trial-images.yml` (image: seed, dockerfile: docker/seed/Dockerfile). Replaced `build: docker/seed/` in `docker-compose.ghcr.yml` with `image: ghcr.io/oggthemiffed/rctimingcontrol/seed:${RCTIMING_VERSION:?Set RCTIMING_VERSION in .env}` matching the pattern of all other services in that file.

---

### CR-02: Forwarder entrypoint writes empty token when `FORWARDER_API_TOKEN` is unset

**Files modified:** `docker/forwarder/entrypoint.sh`
**Commit:** cbd9910
**Applied fix:** Added `: "${FORWARDER_API_TOKEN:?FORWARDER_API_TOKEN must be set}"` as the first command after `set -e`. The `:?` expansion aborts the container with a clear error if the variable is unset or empty, preventing silent authentication with a blank token.

---

### CR-03: nginx WebSocket proxy block missing `X-Forwarded-Proto` header

**Files modified:** `docker/nginx/nginx.conf`
**Commit:** ec9ebea
**Applied fix:** Added `proxy_set_header X-Forwarded-Proto $scheme;` to both the `/api/` location block and the `/ws` location block. Also added `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;` to the `/ws` block which was also missing it.

---

### WR-01: `app` service does not depend on `minio-init` completing

**Files modified:** `docker-compose.trial.yml`, `docker-compose.ghcr.yml`
**Commit:** 17418aa
**Applied fix:** Added `minio-init: condition: service_completed_successfully` to the `app` service `depends_on` block in both compose files, ensuring the MinIO bucket exists before the Spring application starts.

---

### WR-02: `minio-init` uses `restart: "no"` instead of `restart: on-failure`

**Files modified:** `docker-compose.trial.yml`, `docker-compose.ghcr.yml`
**Commit:** 9ed7535
**Applied fix:** Changed `restart: "no"` to `restart: on-failure` for the `minio-init` service in both compose files. This allows Docker to automatically retry the one-shot init container if `mc` exits non-zero due to a transient MinIO availability issue.

---

### WR-04: `docker/app/Dockerfile` copies a glob `app-*.jar`

**Files modified:** `docker/app/Dockerfile`, `app/build.gradle.kts`
**Commit:** 674513b
**Applied fix:** Added `tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") { archiveFileName.set("app.jar") }` to `app/build.gradle.kts`. Updated the Dockerfile `COPY` instruction from `app-*.jar` to the stable `app.jar` name, eliminating the fragile glob that would fail if multiple jars existed in `build/libs/`.

---

### WR-05: `simulate.sh` lacks startup validation before `exec java`

**Files modified:** `docker/fake-decoder/simulate.sh`
**Commit:** 16dc613
**Applied fix:** Added a jar count check at the top of `simulate.sh`: counts `*.jar` files in `/app/lib/`, prints an actionable error to stderr and exits 1 if none are found. The `exec java` line is unchanged.

---

### WR-06: `piper` service has no `depends_on` from `app`

**Files modified:** `docker-compose.trial.yml`, `docker-compose.ghcr.yml`
**Commit:** 3ed6d80
**Applied fix:** Added `piper: condition: service_started` to the `app` service `depends_on` block in both compose files. Since piper has no healthcheck defined, `service_started` is the best available condition — it ensures piper is at least launched before the app initialises its TTS client.

---

## Skipped Issues

### WR-03: `fake-decoder` Dockerfile duplicates the full Gradle build

**File:** `docker/fake-decoder/Dockerfile`
**Reason:** Verification-only finding — confirmed `SimulatorMain` is packaged correctly.

`SimulatorMain` exists at `forwarder/src/main/java/dev/monkeypatch/rctiming/forwarder/simulator/SimulatorMain.java` and is a regular Java main source file. It is compiled into the forwarder jar by the `application` plugin's `installDist` task and will appear under `/app/lib/` inside the fake-decoder image. The `-cp "/app/lib/*"` classpath in `simulate.sh` will load it correctly. The duplicate build concern (two CI matrix jobs each running the full Gradle build) is a performance issue addressed longer-term; it is not a correctness bug. No code change is required.

---

_Fixed: 2026-05-16T00:00:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
