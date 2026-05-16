---
phase: 10-docker-trial-environment
plan: 03
subsystem: infra
tags: [docker, gradle, jooq, protobuf, nginx, spring-boot, vite, react]

requires:
  - phase: 10-docker-trial-environment plan 01
    provides: forwarder and fake-decoder Dockerfiles
  - phase: 10-docker-trial-environment plan 02
    provides: demo-seed Dockerfile and seed SQL
provides:
  - jOOQ generated sources committed at app/src/generated/jooq (87 files)
  - Proto/gRPC generated sources committed at app/src/generated/proto (12 files)
  - docker/app/Dockerfile: multi-stage Spring Boot image (eclipse-temurin:21)
  - docker/frontend/Dockerfile: multi-stage Vite + nginx image
  - docker/nginx/nginx.conf: WebSocket-safe reverse proxy with SPA fallback
  - docker-compose.trial.yml: complete 9-service trial stack
affects: [10-docker-trial-environment plan 04, ghcr-publishing]

tech-stack:
  added: [eclipse-temurin:21-jdk-alpine, eclipse-temurin:21-jre-alpine, node:20-alpine, nginx:stable-alpine, minio/mc:latest]
  patterns:
    - Commit generated sources (jOOQ, proto) to escape gitignored build/ directory for Docker builds
    - Redirect protobuf generatedFilesBaseDir to src/generated/proto so plugin auto-adds correct source set
    - nginx resolver 127.0.0.11 with set $upstream for deferred Docker DNS resolution
    - sourceSets[main].java.srcDir for committed generated sources

key-files:
  created:
    - app/src/generated/jooq/ (87 Java files — committed jOOQ type-safe DSL)
    - app/src/generated/proto/ (12 Java files — committed gRPC stubs)
    - docker/app/Dockerfile
    - docker/frontend/Dockerfile
    - docker/nginx/nginx.conf
    - docker-compose.trial.yml
  modified:
    - app/build.gradle.kts (jOOQ directory, generatedFilesBaseDir, sourceSets)
    - .gitignore (comment explaining committed generated sources)
    - frontend/src/context/HelpContext.test.tsx (remove unused React import)

key-decisions:
  - "Redirect protobuf generatedFilesBaseDir to src/generated/proto instead of separate srcDir to avoid duplicate class errors when generateProto IS run"
  - "Use resolver 127.0.0.11 with set $upstream variables in nginx.conf so nginx -t passes without Docker network"
  - "demo-seed depends_on app:service_healthy (not postgres) so Flyway migrations run before seed SQL executes"
  - "Skip generateProto in Docker Dockerfile alongside generateJooq — both require platform-specific binaries"

patterns-established:
  - "Generated source commits: redirect plugin output dir to src/generated/<tool> and commit — works for both jOOQ and protobuf"
  - "nginx Docker upstream resolution: use resolver 127.0.0.11 valid=30s with set \$upstream variables"

requirements-completed: [SC-1, SC-4]

duration: 90min
completed: 2026-05-16
---

# Phase 10 Plan 03: Docker Trial Images and Compose Stack Summary

**Multi-stage app and frontend Docker images built from committed jOOQ/proto sources, nginx WebSocket reverse-proxy, and docker-compose.trial.yml wiring all 9 services with correct dependency ordering**

## Performance

- **Duration:** ~90 min
- **Started:** 2026-05-16T20:00:00Z
- **Completed:** 2026-05-16T21:40:00Z
- **Tasks:** 4 (Task 0 + Tasks 1-3)
- **Files modified:** ~106 files (87 jOOQ + 12 proto + 7 config)

## Accomplishments

- Resolved jOOQ codegen Docker blocker: generated 87 Java files on host, committed to `app/src/generated/jooq/`, redirected jOOQ `directory` target and added `sourceSets` entry
- Resolved protobuf/gRPC codegen Docker blocker: redirected `generatedFilesBaseDir` to `src/generated/proto/` (12 files committed), protobuf plugin auto-adds source set — no duplicate class errors
- App Docker image builds the full Spring Boot fat jar in ~70s without a live database or Docker-in-Docker
- Frontend Docker image builds the Vite SPA and serves via nginx:stable-alpine with WebSocket proxy support
- docker-compose.trial.yml defines all 9 services with healthchecks, correct dependency ordering, and D-08 port isolation (only nginx:80 exposed)

## Task Commits

1. **Task 0: Commit jOOQ generated sources and add to compile path** - `1a74c4c` (feat)
2. **Task 1: App Dockerfile** - `27d5c8a` (feat)
3. **Task 2: Frontend Dockerfile + nginx reverse-proxy config** - `a282e94` (feat)
4. **Task 3: docker-compose.trial.yml — wire all services** - `c4fb03a` (feat)

## Files Created/Modified

- `app/src/generated/jooq/` - 87 committed jOOQ generated Java files (type-safe DSL for all 26 migrations)
- `app/src/generated/proto/` - 12 committed protobuf/gRPC Java stubs (timing service)
- `app/build.gradle.kts` - jOOQ directory redirected to src/generated/jooq; protobuf generatedFilesBaseDir set to src/generated/proto; sourceSets entry for jOOQ
- `.gitignore` - Added comment explaining committed generated sources pattern
- `docker/app/Dockerfile` - Multi-stage: eclipse-temurin:21-jdk-alpine builder, eclipse-temurin:21-jre-alpine runtime with curl; skips generateJooq and generateProto
- `docker/frontend/Dockerfile` - Multi-stage: node:20-alpine builder, nginx:stable-alpine runtime
- `docker/nginx/nginx.conf` - Full nginx config: WebSocket upgrade map, SPA try_files fallback, /api/ and /ws proxy_pass to app:8080, /storage/ proxy to minio:9000/, resolver 127.0.0.11 for Docker DNS
- `docker-compose.trial.yml` - 9 services: postgres, minio, minio-init, demo-seed, app, frontend, piper, fake-decoder, forwarder
- `frontend/src/context/HelpContext.test.tsx` - Removed unused React import (blocked tsc -b in Vite build)

## Decisions Made

- Redirected protobuf `generatedFilesBaseDir` to `src/generated/proto` instead of adding a manual `srcDir` — the protobuf plugin auto-adds its output dir to the source set, so manual srcDir + plugin output caused duplicate class errors when generateProto was NOT skipped
- Used `resolver 127.0.0.11 valid=30s` with `set $upstream` variables in nginx.conf so that `nginx -t` passes in standalone containers (no Docker network) — nginx defers upstream DNS resolution to request time
- `demo-seed` depends on `app:service_healthy` rather than `postgres:service_healthy` because Flyway runs inside the app at startup — seed SQL requires the full schema to exist before inserting data
- Skipped `-x generateProto` alongside `-x generateJooq` in app Dockerfile — both require platform-specific native binaries (protoc-gen-grpc-java ELF binary, Testcontainers Docker socket) unavailable in `docker build`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Commit proto/gRPC generated sources and skip generateProto in Dockerfile**
- **Found during:** Task 1 (App Dockerfile build)
- **Issue:** Docker build failed: `protoc-gen-grpc-java-1.73.0-linux-x86_64.exe: program not found or is not executable` — the protoc gRPC plugin binary downloaded for the host architecture is not executable inside the Alpine builder container
- **Fix:** Redirected `generatedFilesBaseDir` in protobuf block to `src/generated/proto`, committed the 12 proto/gRPC Java stubs, added `-x generateProto` to Dockerfile's Gradle invocation
- **Files modified:** app/build.gradle.kts, docker/app/Dockerfile, app/src/generated/proto/ (12 new files)
- **Verification:** `./gradlew :app:compileJava -x generateJooq -x generateProto --no-daemon` exits 0; Docker image builds successfully in ~70s
- **Committed in:** 27d5c8a (Task 1 commit)

**2. [Rule 1 - Bug] Fix unused React import in HelpContext.test.tsx blocking Vite build**
- **Found during:** Task 2 (Frontend Dockerfile build)
- **Issue:** `npm run build` runs `tsc -b` which treats `noUnusedLocals: true`. `HelpContext.test.tsx` had `import React from 'react'` (unused with JSX transform) — TypeScript error TS6133
- **Fix:** Removed the unused `import React from 'react'` line
- **Files modified:** frontend/src/context/HelpContext.test.tsx
- **Verification:** Docker frontend image builds successfully; `npm run build` exits 0
- **Committed in:** a282e94 (Task 2 commit)

**3. [Rule 2 - Missing Critical] Add resolver directive to nginx.conf for Docker DNS**
- **Found during:** Task 2 (nginx -t verification)
- **Issue:** `nginx -t` failed: `host not found in upstream "app"` — nginx resolves upstream hostnames at config-test time, but "app" only exists in Docker network, not in the test container
- **Fix:** Added `resolver 127.0.0.11 valid=30s;` and changed proxy_pass targets to use `set $upstream` variables — nginx defers DNS resolution to request time when a variable is used
- **Files modified:** docker/nginx/nginx.conf
- **Verification:** `docker run --rm -v nginx.conf:/etc/nginx/nginx.conf:ro nginx:stable-alpine nginx -t` exits 0
- **Committed in:** a282e94 (Task 2 commit)

---

**Total deviations:** 3 auto-fixed (2 Rule 1 bugs, 1 Rule 2 missing critical)
**Impact on plan:** All auto-fixes essential for Docker build correctness and nginx config validation. No scope creep.

## Issues Encountered

- Proto codegen requires native `protoc-gen-grpc-java` binary downloaded for host architecture — not portable into Alpine Docker builder. Resolved by committing generated sources (same pattern as jOOQ).
- nginx DNS resolution at `-t` time requires upstream variables + Docker resolver directive for isolated testing.

## Known Stubs

None — all Docker infrastructure is wired to real service upstreams.

## Threat Flags

No new security surface introduced beyond what the plan's threat model covers. Port isolation (D-08) enforced in docker-compose.trial.yml — only nginx:80 exposed to host. All secrets use `${VAR:-default}` pattern per T-10-02.

## User Setup Required

None for this plan. Plan 04 will create `.env.example` documenting secrets to change before production deployment.

## Next Phase Readiness

- All Docker images build successfully from source
- docker-compose.trial.yml parses cleanly (`docker compose config -q` exits 0)
- Trial stack ready for Plan 04 (`.env.example`, `docker-compose.ghcr.yml`) and Plan 05 (CI/CD workflow)
- Full `docker compose -f docker-compose.trial.yml up` smoke test is the phase gate before `/gsd-verify-work`

---
*Phase: 10-docker-trial-environment*
*Completed: 2026-05-16*
