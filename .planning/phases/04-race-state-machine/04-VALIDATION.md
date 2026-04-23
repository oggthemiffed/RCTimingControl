---
phase: 4
slug: race-state-machine
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-23
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Testcontainers (backend) |
| **Config file** | `app/build.gradle.kts` (test section) |
| **Quick run command** | `./gradlew :app:test --tests "*.race.*" --tests "*.racecontrol.*" -x generateJooq` |
| **Full suite command** | `./gradlew :app:test` |
| **Estimated runtime** | ~120 seconds (full suite with Testcontainers) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:test --tests "*.race.*" --tests "*.racecontrol.*" -x generateJooq`
- **After every plan wave:** Run `./gradlew :app:test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** ~30 seconds (quick run)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 0 | CTRL-05 | T-04-02 | Invalid state transition returns HTTP 409 | unit | `./gradlew :app:test --tests "*.RaceStateMachineServiceTest"` | ❌ W0 | ⬜ pending |
| 04-01-02 | 01 | 0 | CTRL-05 | T-04-02 | Valid transition updates race status | unit | `./gradlew :app:test --tests "*.RaceStateMachineServiceTest"` | ❌ W0 | ⬜ pending |
| 04-02-01 | 02 | 1 | CTRL-01 | T-04-01 | POST callGrid returns 200 and transitions to GRID | integration | `./gradlew :app:test --tests "*.RaceControlControllerIT"` | ❌ W0 | ⬜ pending |
| 04-02-02 | 02 | 1 | CTRL-01 | T-04-01 | POST startRace returns 200 and transitions to RUNNING | integration | `./gradlew :app:test --tests "*.RaceControlControllerIT"` | ❌ W0 | ⬜ pending |
| 04-03-01 | 03 | 1 | CTRL-03 | T-04-03 | Marshal adjustment persists all audit fields | integration | `./gradlew :app:test --tests "*.RaceControlControllerIT"` | ❌ W0 | ⬜ pending |
| 04-03-02 | 03 | 1 | CTRL-06 | — | Unknown transponder link creates record | integration | `./gradlew :app:test --tests "*.RaceControlControllerIT"` | ❌ W0 | ⬜ pending |
| 04-04-01 | 04 | 2 | CTRL-08 | — | Abandon race saves result snapshot | integration | `./gradlew :app:test --tests "*.RaceControlControllerIT.abandon*"` | ❌ W0 | ⬜ pending |
| 04-04-02 | 04 | 2 | CTRL-04 | — | GET /results/{id}/print returns 200 with correct data | integration | `./gradlew :app:test --tests "*.RaceControlControllerIT"` | ❌ W0 | ⬜ pending |
| 04-05-01 | 05 | 2 | OFFICIAL-03 | T-04-04 | Incident report creates record linked to race | integration | `./gradlew :app:test --tests "*.RefereeControllerIT"` | ❌ W0 | ⬜ pending |
| 04-05-02 | 05 | 2 | OFFICIAL-04 | T-04-04 | Penalty application recalculates positions | integration | `./gradlew :app:test --tests "*.RefereeControllerIT"` | ❌ W0 | ⬜ pending |
| 04-06-01 | 06 | 1 | — | — | Heat splitting: 15 drivers, max 8/heat → 2 heats | unit | `./gradlew :app:test --tests "*.RoundGeneratorServiceTest"` | ❌ W0 | ⬜ pending |
| 04-06-02 | 06 | 1 | — | — | Bump-up seeding: top N of B-Final appended to A-Final | unit | `./gradlew :app:test --tests "*.RoundGeneratorServiceTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/.../domain/race/RaceStateMachineServiceTest.java` — stubs for CTRL-05
- [ ] `app/src/test/.../api/racecontrol/RaceControlControllerIT.java` — stubs for CTRL-01, CTRL-03, CTRL-06, CTRL-08, CTRL-04
- [ ] `app/src/test/.../api/racecontrol/RefereeControllerIT.java` — stubs for OFFICIAL-03, OFFICIAL-04
- [ ] `app/src/test/.../service/RoundGeneratorServiceTest.java` — stubs for round generator algorithm

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| WebSocket STOMP connection established in browser | D-06 | Requires live browser WebSocket session | Open race control page, check browser DevTools Network → WS tab for STOMP CONNECTED frame |
| Synthetic timing button hidden in non-dev profile | D-07 | Profile-dependent render | Run app with `spring.profiles.active=prod`, verify button absent in race cockpit |
| Print results opens new browser tab | D-19 | Browser tab open behaviour | Click Print Results in finished race state, verify new tab opens with print layout |
| Mobile cockpit Sheet drawer opens on small viewport | D-03 | Responsive layout | Resize browser to 375px width, verify run order appears as Sheet, not sidebar |
| Marshal absent marked without auto-penalty | D-22 | UI state verification | Mark driver absent, verify no penalty record created without explicit "Apply Penalty" action |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
