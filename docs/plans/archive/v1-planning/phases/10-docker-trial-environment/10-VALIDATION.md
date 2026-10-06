---
phase: 10
slug: docker-trial-environment
status: ready
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-16
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> This is a Docker infrastructure phase — most verification is build/parse/smoke,
> not unit tests. The single Java change gets a real JUnit test.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (forwarder module only); Docker CLI for everything else |
| **Config file** | `forwarder/build.gradle.kts` (`useJUnitPlatform()`) — already present |
| **Quick run command** | `docker compose -f docker-compose.trial.yml config -q` |
| **Full suite command** | `docker compose -f docker-compose.trial.yml build` |
| **Estimated runtime** | config parse ~2s; full build ~5-10min cold, ~1min cached |

---

## Sampling Rate

- **After every task commit:** Run `docker compose -f docker-compose.trial.yml config -q` (once the file exists); for Java change run `./gradlew :forwarder:test --tests "*ForwarderApplication*" -q`
- **After every plan wave:** Build the Docker images touched by that wave
- **Before `/gsd-verify-work`:** Full `docker compose -f docker-compose.trial.yml up` smoke test — all services healthy
- **Max feedback latency:** ~10s for config/test; image builds longer (cached)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 10-01-01 | 01 | 1 | SC-3 | — | N/A | unit | `./gradlew :forwarder:test --tests "*ForwarderApplication*" -q` | ✅ | ⬜ pending |
| 10-01-02 | 01 | 1 | SC-3 | — | N/A | build | `docker build -f docker/forwarder/Dockerfile -t rctiming-forwarder:test .` | ❌ W1 | ⬜ pending |
| 10-01-03 | 01 | 1 | SC-3 | T-10-04 | fake-decoder no host port | build | `docker build -f docker/fake-decoder/Dockerfile -t rctiming-fake-decoder:test .` | ❌ W1 | ⬜ pending |
| 10-02-01 | 02 | 1 | SC-2 | T-10-03 | seed uses @example.com emails | smoke | smoke test: `psql -c "SELECT name FROM club_profiles"` | ❌ W1 | ⬜ pending |
| 10-02-02 | 02 | 1 | SC-2 | — | N/A | build | `docker build -f docker/seed/Dockerfile -t rctiming-seed:test docker/seed/` | ❌ W1 | ⬜ pending |
| 10-03-00 | 03 | 2 | SC-1 | — | N/A | check | `git ls-files app/src/generated/jooq \| head -1` non-empty | ❌ W2 | ⬜ pending |
| 10-03-01 | 03 | 2 | SC-1 | — | N/A | build | `docker build -f docker/app/Dockerfile -t rctiming-app:test .` | ❌ W2 | ⬜ pending |
| 10-03-02 | 03 | 2 | SC-1/SC-4 | — | N/A | build | `docker build -f docker/frontend/Dockerfile -t rctiming-frontend:test .` + `nginx -t` | ❌ W2 | ⬜ pending |
| 10-03-03 | 03 | 2 | SC-1 | T-10-01/T-10-02 | no host ports for db/grpc/piper; default secrets flagged | parse | `docker compose -f docker-compose.trial.yml config -q` | ❌ W2 | ⬜ pending |
| 10-04-01 | 04 | 3 | SC-1 | T-10-02 | all secrets carry "change before production" comment | parse | `docker compose -f docker-compose.trial.yml config -q` | ❌ W3 | ⬜ pending |
| 10-04-02 | 04 | 3 | SC-5 | T-10-05 | no `latest` tag; version pinned | parse | `RCTIMING_VERSION=v0.0.1 docker compose -f docker-compose.ghcr.yml config -q` | ❌ W3 | ⬜ pending |
| 10-04-03 | 04 | 3 | SC-5 | — | N/A | parse | `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/publish-trial-images.yml'))"` | ❌ W3 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. No test-stub Wave 0 plan is needed:
- The infrastructure files (Dockerfiles, compose, nginx, seed SQL, GHA workflow) are verified by build/parse/smoke, not unit tests.
- The one Java change (`ForwarderApplication --config-file`) gets a real JUnit test created inside Plan 01 — a single test does not warrant a separate Wave 0 plan.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Live timing visible in race control client | SC-3 | Requires a browser, a running race, and visual confirmation of lap rows updating | `docker compose -f docker-compose.trial.yml up -d`; open `http://localhost`; log in as race director (seed account); start a race in the seeded upcoming event; confirm laps appear |
| Setup wizard reachable and functional | SC-4 | Browser navigation + interactive form submission | Open `http://localhost`; navigate to Admin → Setup (or `/setup`); confirm wizard steps render and a logo upload succeeds (MinIO-backed) |
| GHCR images pull and run | SC-5 | Requires images published by a real `v*` tag push to GitHub | After a `v*` tag triggers the workflow: `RCTIMING_VERSION=v1.0.0 docker compose -f docker-compose.ghcr.yml up` |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or are documented manual-only
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (none required — infra phase)
- [x] No watch-mode flags
- [x] Feedback latency acceptable (config/test ~10s; builds cached)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** ready
