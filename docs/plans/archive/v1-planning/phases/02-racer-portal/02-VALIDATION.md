---
phase: 2
slug: racer-portal
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-16
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Testcontainers (backend); Vitest + React Testing Library (frontend) |
| **Config file** | `app/src/test/` (backend); `frontend/vitest.config.ts` (frontend) |
| **Quick run command** | `./gradlew :app:test --tests "*.racer.*" --tests "*.entry.*"` |
| **Full suite command** | `./gradlew :app:test && cd frontend && npm run test` |
| **Estimated runtime** | ~60 seconds (backend with Testcontainers) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:test --tests "*.racer.*" --tests "*.entry.*"`
- **After every plan wave:** Run `./gradlew :app:test && cd frontend && npm run test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 02-xx-01 | TBD | TBD | RACER-01 | — | Profile update scoped to authenticated user only | integration | `./gradlew :app:test --tests "*.RacerProfileControllerTest"` | ❌ W0 | ⬜ pending |
| 02-xx-02 | TBD | TBD | RACER-02 | — | Car CRUD scoped to owner | integration | `./gradlew :app:test --tests "*.CarControllerTest"` | ❌ W0 | ⬜ pending |
| 02-xx-03 | TBD | TBD | RACER-03 | — | Transponder uniqueness enforced system-wide | integration | `./gradlew :app:test --tests "*.TransponderControllerTest"` | ❌ W0 | ⬜ pending |
| 02-xx-04 | TBD | TBD | RACER-06 | — | Transponder snapshotted at entry submission time | integration | `./gradlew :app:test --tests "*.EntryServiceTest"` | ❌ W0 | ⬜ pending |
| 02-xx-05 | TBD | TBD | RACER-12 | — | Membership check blocks entry when required | integration | `./gradlew :app:test --tests "*.EntrySubmissionTest"` | ❌ W0 | ⬜ pending |
| 02-xx-06 | TBD | TBD | EVENT-04 | — | Public event schedule returns 200 without auth | integration | `./gradlew :app:test --tests "*.PublicEventControllerTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/dev/monkeypatch/timing/racer/RacerProfileControllerTest.java` — stubs for RACER-01
- [ ] `app/src/test/java/dev/monkeypatch/timing/racer/CarControllerTest.java` — stubs for RACER-02, RACER-03, RACER-10
- [ ] `app/src/test/java/dev/monkeypatch/timing/racer/TransponderControllerTest.java` — stubs for RACER-04, RACER-05, RACER-08
- [ ] `app/src/test/java/dev/monkeypatch/timing/entry/EntryServiceTest.java` — stubs for RACER-06, RACER-12, RACER-13, RACER-14, ENTRY-01
- [ ] `app/src/test/java/dev/monkeypatch/timing/entry/EntrySubmissionTest.java` — integration test stubs for EVENT-03, EVENT-04
- [ ] `app/src/test/java/dev/monkeypatch/timing/event/PublicEventControllerTest.java` — stubs for EVENT-04 (anonymous access)
- [ ] `app/src/test/resources/db/migration/` — Flyway test seed data (one OPEN event, one DRAFT event)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Bottom nav bar renders on mobile viewport | D-06 | Requires browser viewport check | Resize browser to <640px, verify bottom nav appears instead of top nav |
| Car tag inline edit (Sheet slide-over) | D-07 | React interaction, not covered by RTL unit test | Add a car, click edit, verify Sheet opens with tag fields |
| Entry withdrawal UI before entries close | RACER-11 | Requires event in OPEN state with close date in future | Submit entry, navigate to /racer/entries, click Withdraw, confirm dialog |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
