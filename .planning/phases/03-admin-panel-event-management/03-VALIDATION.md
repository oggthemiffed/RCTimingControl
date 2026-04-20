---
phase: 3
slug: admin-panel-event-management
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-20
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Testcontainers (backend); Vitest (frontend) |
| **Config file** | `app/src/test/` / `frontend/vitest.config.ts` |
| **Quick run command** | `cd app && ./gradlew test --tests "*.admin.*" -x integrationTest` |
| **Full suite command** | `cd app && ./gradlew test && cd ../frontend && npm run build` |
| **Estimated runtime** | ~45 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd app && ./gradlew test --tests "*.admin.*" -x integrationTest`
- **After every plan wave:** Run `cd app && ./gradlew test && cd ../frontend && npm run build`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 45 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 3-01-01 | 01 | 0 | EVENT-01 | — | N/A | migration | `cd app && ./gradlew flywayMigrate` | ❌ W0 | ⬜ pending |
| 3-01-02 | 01 | 1 | EVENT-01 | — | N/A | unit | `cd app && ./gradlew test --tests "*.EventServiceTest"` | ❌ W0 | ⬜ pending |
| 3-02-01 | 02 | 1 | EVENT-02 | — | N/A | unit | `cd app && ./gradlew test --tests "*.EventClassServiceTest"` | ❌ W0 | ⬜ pending |
| 3-03-01 | 03 | 1 | EVENT-05 | — | N/A | unit | `cd app && ./gradlew test --tests "*.RaceFormatServiceTest"` | ❌ W0 | ⬜ pending |
| 3-04-01 | 04 | 1 | CHAMP-01 | — | N/A | unit | `cd app && ./gradlew test --tests "*.ChampionshipServiceTest"` | ❌ W0 | ⬜ pending |
| 3-05-01 | 05 | 2 | EVENT-01 | — | N/A | build | `cd frontend && npm run build` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/dev/monkeypatch/rctiming/admin/EventServiceTest.java` — stubs for EVENT-01, EVENT-02
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/admin/ChampionshipServiceTest.java` — stubs for CHAMP-01 through CHAMP-10
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/admin/EventClassServiceTest.java` — stubs for EVENT-02, EVENT-07
- [ ] `app/src/test/resources/db/migration/test/V102__test_seed_admin.sql` — admin test data

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| MinIO logo upload renders in admin UI | EVENT-01 (club profile) | Requires running MinIO container and browser | Start docker-compose, navigate to Club Profile, upload logo, verify it displays |
| Event state machine transitions via UI buttons | EVENT-01 | E2E browser interaction | Navigate to event detail, click each state transition button in order |
| Championship standings drop-score rendering | CHAMP-07 | Requires real result data (Phase 7) | Scaffold only — manual verify deferred to Phase 7 |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 45s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
