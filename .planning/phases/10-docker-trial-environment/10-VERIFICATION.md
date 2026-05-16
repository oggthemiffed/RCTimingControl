---
phase: 10-docker-trial-environment
verified: 2026-05-16T21:55:00Z
status: human_needed
score: 4/5 must-haves verified (SC-3 needs override or human confirmation)
overrides_applied: 0
overrides: []
re_verification: null
gaps: []
human_verification:
  - test: "Confirm SC-3 deviation: generative mode accepted in place of replay file"
    expected: "SC-3 roadmap wording says 'replays a recorded RC-4 passing file'; implementation uses --mode=generative. RESEARCH.md line 844 documents user confirmation. Confirm this deviation is permanently accepted."
    why_human: "Roadmap success criterion SC-3 uses the word 'replays a recorded RC-4 passing file' and implementation delivers generative mode instead. The RESEARCH.md documents user confirmation on 2026-05-16 but this is not captured in a formal override block in the VERIFICATION.md frontmatter. A human must decide: accept the deviation (add override below) or restore replay-file behaviour."
  - test: "Live timing visible in race control UI with fake decoder running"
    expected: "With docker compose -f docker-compose.trial.yml up running: open race control client, start a race, observe live lap passings from transponders 101-108 appearing in the timing display every ~13 seconds (+/- jitter)"
    why_human: "Cannot verify WebSocket live timing display programmatically without a running browser and a running stack. SC-3 intent — that live timing is actually visible — requires human UAT."
  - test: "Setup wizard accessible and functional in trial environment"
    expected: "Navigate to http://localhost/setup — wizard renders, all steps navigable, decoder config step shows FORWARDER_API_TOKEN value from .env, club profile can be edited"
    why_human: "SC-4 requires the setup wizard to be 'accessible and functional'. nginx correctly proxies /api/ and serves SPA with try_files fallback, but functional correctness of the wizard UI within the Docker stack requires a running browser."
---

# Phase 10: Docker Trial Environment Verification Report

**Phase Goal:** Any club can spin up the complete system locally with `docker compose up` — including a live timing demo using replayed passing data — to evaluate features and give feedback before committing to a full deployment
**Verified:** 2026-05-16T21:55:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| SC-1 | `docker compose up` from fresh clone brings up all services with no manual config beyond copying `.env.example` | VERIFIED | `docker-compose.trial.yml` parses cleanly (`docker compose config -q` exits 0); defines all 9 services; `.env.example` exists with `cp .env.example .env` instruction and Docker Compose v2.1+ requirement |
| SC-2 | Demo-seed container populates database with sample club, track, formats, racers, completed event | VERIFIED | `docker/seed/seed.sql` (482 lines) inserts Wyvern RC Club, 2 tracks, 3 format templates, 3 racing classes, 9 accounts, 8 cars, transponders 101-108, championship, completed Round 3 with result_snapshot; idempotency guard on `club_profiles WHERE name = 'Wyvern RC Club'`; `docker/seed/Dockerfile` builds from `postgres:16-alpine` with `ON_ERROR_STOP=1` |
| SC-3 | Fake decoder container replays a recorded RC-4 passing file on a loop so live timing is visible without hardware | UNCERTAIN | Implementation uses `--mode=generative` (continuous synthetic generation) not a `.txt` replay file. RESEARCH.md line 844 documents "User confirmed generative mode is acceptable." SimulatorMain runs `--transponders=101,102,103,104,105,106,107,108 --interval-ms=13000 --jitter-ms=2500` matching seed data. Functionality equivalent but literal SC-3 wording ("recorded file") is not met. |
| SC-4 | Setup wizard (Phase 8) is accessible and functional within the trial environment | VERIFIED (infra) | nginx `/api/` proxies to `app:8080`; SPA served with `try_files $uri $uri/ /index.html` fallback; `TTS_ENABLED: "true"` and Piper TTS service wired; `demo-seed` depends_on `app: condition: service_healthy` so schema exists. Functional correctness requires human testing. |
| SC-5 | `docker-compose.ghcr.yml` pulls pre-built images from GHCR; images published by GitHub Actions on version tags | VERIFIED | `docker-compose.ghcr.yml` defines all 4 GHCR image references with `${RCTIMING_VERSION:?Set RCTIMING_VERSION in .env}` (fail-fast, no `latest`); `RCTIMING_VERSION=v0.0.1 docker compose -f docker-compose.ghcr.yml config -q` exits 0; `.github/workflows/publish-trial-images.yml` triggers on `tags: ['v*']`, matrix over 4 images, `type=semver,pattern={{version}}`, `packages: write` permission |

**Score:** 4/5 truths fully verified; SC-3 is UNCERTAIN pending override confirmation or human review.

### Deferred Items

None identified — all SC items are addressed within Phase 10.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `forwarder/src/main/java/.../ForwarderApplication.java` | `--config-file=` arg + `resolveConfig()` static method | VERIFIED | Contains `static ForwarderConfig resolveConfig(String[] args)`, `--config-file=` prefix check, `main()` delegates to `resolveConfig(args)` |
| `forwarder/src/test/.../ForwarderApplicationConfigArgTest.java` | 3 JUnit 5 tests proving config-file arg parsing | VERIFIED | File exists with 3 `@Test` methods: file-path arg, no-args fallback, unrecognised-arg fallback |
| `docker/forwarder/Dockerfile` | Multi-stage forwarder image build | VERIFIED | `FROM eclipse-temurin:21-jdk-alpine AS builder`, copies `app/`, `forwarder/`, `buildSrc/`, `gradle/`; runs `:forwarder:installDist -x test -x generateProto` |
| `docker/forwarder/entrypoint.sh` | Env-var to properties file generator | VERIFIED | Writes 6 `FORWARDER_*` env vars to `/tmp/forwarder.properties`; `exec /app/bin/forwarder --config-file=/tmp/forwarder.properties` |
| `docker/fake-decoder/Dockerfile` | Multi-stage fake-decoder image build | VERIFIED | `FROM eclipse-temurin:21-jdk-alpine AS builder`, `EXPOSE 5100`, `ENTRYPOINT ["/simulate.sh"]` |
| `docker/fake-decoder/simulate.sh` | SimulatorMain launcher with pass-through args | VERIFIED | `exec java -cp "/app/lib/*" dev.monkeypatch.rctiming.forwarder.simulator.SimulatorMain "$@"`; no hardcoded `--mode` or `--transponders` |
| `docker/seed/seed.sql` | Idempotent Wyvern RC Club seed data | VERIFIED | Contains `Wyvern RC Club` club, BCrypt hash `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`, transponders 101-108, `DEMO-FORWARDER-TOKEN-CHANGE-BEFORE-PRODUCTION` in `forwarder_token` row, all emails `@example.com`, idempotency guard |
| `docker/seed/Dockerfile` | One-shot psql seed runner image | VERIFIED | `FROM postgres:16-alpine`, COPY seed.sql, ENTRYPOINT runs `psql -v ON_ERROR_STOP=1 -f /seed.sql` |
| `app/src/generated/jooq/` | Committed jOOQ generated sources (87 files) | VERIFIED | `git ls-files app/src/generated/jooq` returns 87 files under `dev/monkeypatch/rctiming/jooq/generated`; `app/build.gradle.kts` sets `directory = "src/generated/jooq"` and `sourceSets["main"].java.srcDir("src/generated/jooq")` |
| `docker/app/Dockerfile` | Multi-stage Spring Boot image | VERIFIED | `FROM eclipse-temurin:21-jdk-alpine AS builder`, `:app:bootJar -x test -x generateJooq -x generateProto`, `apk add --no-cache curl`, `EXPOSE 8080 9090` |
| `docker/frontend/Dockerfile` | Multi-stage Vite + nginx image | VERIFIED | `FROM node:20-alpine AS builder`, `npm ci`, `npm run build`, `FROM nginx:stable-alpine`, copies `dist/` to `/usr/share/nginx/html/` |
| `docker/nginx/nginx.conf` | Reverse proxy + SPA serving config | VERIFIED | Contains `map $http_upgrade $connection_upgrade`, `try_files $uri $uri/ /index.html`, `proxy_pass $upstream_app` for `/api/` and `/ws`, `proxy_read_timeout 3600s`, `/storage/` proxying to minio, `resolver 127.0.0.11` |
| `docker-compose.trial.yml` | Complete 9-service trial stack | VERIFIED | Defines postgres, minio, minio-init, demo-seed, app, frontend, piper, fake-decoder, forwarder; only `frontend` has host port mapping; `demo-seed restart: "no"`; `--transponders=101,102,103,104,105,106,107,108`; `start_period: 90s` on app healthcheck; `SPRING_DATASOURCE_URL` present |
| `.env.example` | Documented env-var template | VERIFIED | Contains all 7 required variables; 7 occurrences of `CHANGE BEFORE PRODUCTION` (exceeds required 5); `FORWARDER_API_TOKEN=DEMO-FORWARDER-TOKEN-CHANGE-BEFORE-PRODUCTION`; header with `cp .env.example .env` and Docker Compose v2.1+ note |
| `docker-compose.ghcr.yml` | GHCR pre-built image variant | VERIFIED | 4 GHCR image references with `${RCTIMING_VERSION:?}` fail-fast syntax; no `latest` tag on rctimingcontrol images; `demo-seed` retains `build: docker/seed/`; parses cleanly with `RCTIMING_VERSION=v0.0.1` |
| `.github/workflows/publish-trial-images.yml` | Automated GHCR image publishing | VERIFIED | Valid YAML; triggers on `tags: ['v*']` only; `permissions: packages: write`; matrix over 4 images; `type=semver,pattern={{version}}`; `context: .` for all builds |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `docker/forwarder/entrypoint.sh` | `ForwarderApplication --config-file` | `exec /app/bin/forwarder --config-file=/tmp/forwarder.properties` | WIRED | Confirmed in entrypoint.sh line 8 |
| `docker/fake-decoder/simulate.sh` | `SimulatorMain` | `java -cp "/app/lib/*" dev.monkeypatch.rctiming.forwarder.simulator.SimulatorMain "$@"` | WIRED | Confirmed in simulate.sh |
| `docker/seed/seed.sql` | `forwarder_token` table | INSERT with `token_value = 'DEMO-FORWARDER-TOKEN-CHANGE-BEFORE-PRODUCTION'` | WIRED | seed.sql line 475-477 |
| `docker/seed/Dockerfile` | `seed.sql` | `psql -f /seed.sql` | WIRED | Confirmed in Dockerfile ENTRYPOINT |
| `docker/nginx/nginx.conf` | `app:8080` | `proxy_pass $upstream_app` for `/api/` and `/ws` (using Docker resolver pattern) | WIRED | nginx.conf lines 35-50; uses `set $upstream_app` variable + `resolver 127.0.0.11` |
| `docker-compose.trial.yml app service` | `demo-seed` | `depends_on: app: condition: service_healthy` on demo-seed | WIRED | Confirmed in compose file |
| `docker-compose.trial.yml app service` | `postgres` | `SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/rctiming` | WIRED | Confirmed in compose file |
| `.env.example` | `docker-compose.trial.yml` | Shared variable names `POSTGRES_PASSWORD`, `JWT_SECRET`, `HOST_PORT`, `FORWARDER_API_TOKEN` | WIRED | All 4 variables present in both files |
| `docker-compose.ghcr.yml` | GHCR registry | `image: ghcr.io/...` pinned to `RCTIMING_VERSION` | WIRED | All 4 images use `${RCTIMING_VERSION:?}` |
| `.github/workflows/publish-trial-images.yml` | GHCR | `docker/build-push-action` with `push: true` | WIRED | Confirmed in workflow steps |

### Data-Flow Trace (Level 4)

Not applicable — this phase delivers Docker infrastructure and configuration, not application components rendering dynamic data. Seed data flows from `seed.sql` through psql to the PostgreSQL schema that the app then queries via its existing domain layer (verified in prior phases).

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| docker-compose.trial.yml parses without error | `docker compose -f docker-compose.trial.yml config -q` | exits 0 | PASS |
| docker-compose.ghcr.yml parses with RCTIMING_VERSION set | `RCTIMING_VERSION=v0.0.1 docker compose -f docker-compose.ghcr.yml config -q` | exits 0 | PASS |
| GitHub Actions workflow is valid YAML | `python3 -c "import yaml; yaml.safe_load(open(...))"` | YAML_OK | PASS |
| jOOQ generated sources committed | `git ls-files app/src/generated/jooq \| wc -l` | 87 | PASS |
| seed.sql contains Wyvern RC Club sentinel | `grep -c "Wyvern RC Club" docker/seed/seed.sql` | 2 | PASS |
| `.env.example` has CHANGE BEFORE PRODUCTION >= 5 times | `grep -c "CHANGE BEFORE PRODUCTION" .env.example` | 7 | PASS |
| Live timing display in browser | Cannot test without running stack + browser | — | SKIP (human) |
| Setup wizard functional in trial environment | Cannot test without running stack + browser | — | SKIP (human) |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|------------|-------------|-------------|--------|----------|
| SC-1 | Plans 03, 04 | `docker compose up` with no manual config beyond `.env.example` | SATISFIED | `docker-compose.trial.yml` + `.env.example` verified |
| SC-2 | Plan 02 | Demo-seed container populates database | SATISFIED | `seed.sql` + `docker/seed/Dockerfile` verified |
| SC-3 | Plan 01 | Fake decoder emits live timing without hardware | UNCERTAIN | Generative mode used instead of replay file; user confirmed acceptable per RESEARCH.md line 844 |
| SC-4 | Plan 03 | Setup wizard accessible and functional | INFRA SATISFIED | nginx routing + SPA serving verified; functional check is human |
| SC-5 | Plan 04 | GHCR variant + automated publishing | SATISFIED | `docker-compose.ghcr.yml` + GHA workflow verified |

### Anti-Patterns Found

No blockers. One notable deviation:

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `docker-compose.trial.yml` + `docker/fake-decoder/simulate.sh` | SC-3 uses generative mode not replay file | INFO | SC-3 literal wording ("replays a recorded RC-4 passing file") not met; intent met; user confirmed per RESEARCH.md |

No TODO/FIXME/placeholder comments found in phase deliverables. No stub return values (`return null`, `return {}`, `return []`) in the infrastructure files. No hardcoded empty data flowing to user-visible output.

### Human Verification Required

**Item 1: SC-3 Deviation — Generative Mode vs Recorded Replay File**

**Test:** Review and accept or reject the deviation from SC-3's literal wording.
**Context:** SC-3 in ROADMAP.md says "The fake decoder container replays a recorded RC-4 passing file on a loop". The implementation uses `SimulatorMain --mode=generative` which generates synthetic passings indefinitely — no `.txt` file exists. RESEARCH.md line 844 documents that the user confirmed generative mode acceptable on 2026-05-16.
**Expected:** Decision: accept the deviation (add override to this file's frontmatter) or request a replay file be created.
**Why human:** This is a documented, intentional deviation from roadmap wording that requires explicit human sign-off to record as permanently accepted. To accept, add to this file's frontmatter:
```yaml
overrides:
  - must_have: "The fake decoder container replays a recorded RC-4 passing file on a loop so live timing is visible without hardware"
    reason: "Generative mode loops indefinitely with matching transponder IDs 101-108; user confirmed equivalent on 2026-05-16 in RESEARCH.md line 844; PlaybackMode does not loop natively"
    accepted_by: "david"
    accepted_at: "2026-05-16T00:00:00Z"
```

---

**Item 2: Live Timing Visible in Race Control UI**

**Test:** With `docker compose -f docker-compose.trial.yml up` running: open the race control client in a browser, start a race that has entries from transponders 101-108, observe live lap passings appearing.
**Expected:** Lap passings from the fake-decoder (every ~13 seconds with ±2.5s jitter) appear in the live timing table in the race control UI. Positions update in real time via WebSocket.
**Why human:** SC-3's core intent — that live timing is visible without physical hardware — can only be verified with a running browser session and a running compose stack. The infrastructure plumbing is verified (fake-decoder → forwarder → gRPC → app → WebSocket → browser), but end-to-end functional correctness requires a real smoke test.

---

**Item 3: Setup Wizard Accessible and Functional**

**Test:** With the trial stack running: navigate to `http://localhost` in a browser, then to `/setup`. Complete at least the club profile step and decoder config step.
**Expected:** Setup wizard renders (Phase 8 React UI), all 5 steps are navigable, the decoder config step shows `FORWARDER_API_TOKEN` configuration, club profile changes persist.
**Why human:** SC-4 requires the wizard to be "accessible and functional" — nginx routing and SPA fallback are verified programmatically, but functional wizard behaviour within the trial environment's wiring requires a running browser.

---

### Gaps Summary

No blocking gaps found. All required artifacts exist, are substantive (not stubs), and are wired correctly in the compose stack. The single UNCERTAIN item (SC-3) is a pre-documented intentional deviation from roadmap wording that the codebase evidence and RESEARCH.md confirm was accepted by the user during planning. Human sign-off to formalise the override is the only outstanding item.

---

_Verified: 2026-05-16T21:55:00Z_
_Verifier: Claude (gsd-verifier)_
