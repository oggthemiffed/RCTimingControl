---
phase: 8
slug: first-run-setup-wizard
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-05-03
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito + Testcontainers (backend); Vitest + React Testing Library (frontend) |
| **Config file** | `build.gradle.kts` / `frontend/vite.config.ts` |
| **Quick run command** | `./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.setup.SetupControllerIT,dev.monkeypatch.rctiming.api.setup.SetupServiceTest"` |
| **Full suite command** | `./gradlew test && cd frontend && npm test -- --run` |
| **Estimated runtime** | ~60 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick test command
- **After every plan wave:** Run full suite
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 08-01-01 | 01 | 1 | CTRL-wizard-01 | T-08-01 | Bootstrap endpoint returns 409 if any user exists | integration | `./gradlew :app:test --tests "*.SetupControllerIT#bootstrap_returns409_whenUsersExist"` | ❌ W0 | ⬜ pending |
| 08-01-02 | 01 | 1 | CTRL-wizard-02 | — | Flyway V25 migration applies without error | integration | `./gradlew :app:test --tests "*.MigrationIntegrationTest"` | ❌ W0 | ⬜ pending |
| 08-02-01 | 02 | 2 | CTRL-wizard-03 | — | Setup progress returns correct booleans from DB state | unit | `./gradlew :app:test --tests "*.SetupServiceTest"` | ❌ W0 | ⬜ pending |
| 08-03-01 | 04 | 4 | CTRL-wizard-04 | — | SetupGuard redirects only when no club exists | unit | `cd frontend && npm test -- --run src/pages/setup/__tests__/SetupGuard` | ❌ W0 | ⬜ pending |
| 08-03-02 | 04 | 4 | CTRL-wizard-05 | — | SetupGuard does not redirect when path starts with /setup | unit | `cd frontend && npm test -- --run src/pages/setup/__tests__/SetupGuard` | ❌ W0 | ⬜ pending |
| 08-04-01 | 03 | 3 | CTRL-wizard-06 | — | forwarder-config-download returns .env attachment | integration | `./gradlew :app:test --tests "*.SetupControllerIT#downloadForwarderConfig_returnsEnvAttachment"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/setup/SetupControllerIT.java` — stubs for bootstrap + forwarder-config-download
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/setup/SetupServiceTest.java` — wizard progress unit tests
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/MigrationIntegrationTest.java` — V25 migration smoke test
- [ ] `frontend/src/pages/setup/__tests__/SetupGuard.test.tsx` — redirect logic stubs
- [ ] `frontend/src/pages/setup/__tests__/DecoderConfigStep.test.tsx` — test-connection polling UI stubs (SC-4)
- [ ] `frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx` — Setup Wizard nav entry (SC-5)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Full wizard flow end-to-end in browser | CTRL-wizard-all | Multi-page UX flow; no E2E framework installed | Open browser → fresh DB → verify redirect → complete all 5 steps → verify completion summary |
| forwarder.env download has correct placeholder | CTRL-wizard-env | File download assertion requires browser tooling | Download file from decoder step → verify token field shows placeholder comment |
| Re-entrant wizard resumes correct step | CTRL-wizard-reentrant | Session state across page refreshes | Complete 2 steps → close browser → reopen → verify wizard resumes at step 3 |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
