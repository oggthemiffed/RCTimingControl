---
phase: 5
slug: live-timing-forwarder
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-26
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito + Testcontainers (backend); Vitest + React Testing Library (frontend) |
| **Config file** | `app/src/test/` — AbstractIntegrationTest base class; `frontend/vitest.config.ts` |
| **Quick run command** | `./gradlew :forwarder:test --tests "*Parser*" :app:test --tests "*.timing.*"` |
| **Full suite command** | `./gradlew :app:test :forwarder:test && cd frontend && npm run test` |
| **Estimated runtime** | ~60 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :forwarder:test --tests "*Parser*" :app:test --tests "*.timing.*"`
- **After every plan wave:** Run `./gradlew :app:test :forwarder:test && cd frontend && npm run test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 5-01-01 | 01 | 0 | FORWARDER-02 | — | N/A | unit | `./gradlew :forwarder:test --tests "*.Rc4TextParserTest"` | ❌ W0 | ⬜ pending |
| 5-01-02 | 01 | 0 | TIMING-07 | — | N/A | unit | `./gradlew :forwarder:test --tests "*.GapDetectionTest"` | ❌ W0 | ⬜ pending |
| 5-01-03 | 01 | 0 | TIMING-04 | — | N/A | unit | `./gradlew :forwarder:test --tests "*.EpochAnchorTest"` | ❌ W0 | ⬜ pending |
| 5-01-04 | 01 | 1 | FORWARDER-01, TIMING-02 | — | N/A | integration | `./gradlew :forwarder:test --tests "*.AmbRc4TimingSourceIT"` | ❌ W0 | ⬜ pending |
| 5-01-05 | 01 | 1 | TIMING-05 | — | N/A | unit | `./gradlew :forwarder:test --tests "*.TimingSourceTest"` | ❌ W0 | ⬜ pending |
| 5-02-01 | 02 | 0 | FORWARDER-05 | Forwarder impersonation | Token validation rejects missing/invalid tokens; BCrypt hash in DB | unit | `./gradlew :app:test --tests "*.ForwarderTokenServiceTest"` | ❌ W0 | ⬜ pending |
| 5-02-02 | 02 | 1 | FORWARDER-03, TIMING-01 | Token replay | Revoked tokens rejected via DB status check | integration | `./gradlew :app:test --tests "*.ForwarderGrpcServiceIT"` | ❌ W0 | ⬜ pending |
| 5-03-01 | 03 | 1 | TIMING-08 | Wrong transponder linked | Audit record persisted with actor userId | unit | `./gradlew :app:test --tests "*.LiveRaceStateRetroactiveTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/Rc4TextParserTest.java` — stubs for FORWARDER-02, TIMING-07
- [ ] `forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/EpochAnchorTest.java` — stubs for TIMING-04
- [ ] `forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/GapDetectionTest.java` — stubs for TIMING-07
- [ ] `forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/AmbRc4TimingSourceIT.java` — requires simulator running on loopback
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderGrpcServiceIT.java` — stubs for FORWARDER-03, TIMING-01
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenServiceTest.java` — stubs for FORWARDER-05
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/timing/LiveRaceStateRetroactiveTest.java` — stubs for TIMING-08, D-12

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| ForwarderStatusBar shows green/amber/red pills correctly | TIMING-02 | React component visual state | Start simulator, open race control UI, verify pill colours change on connect/disconnect/reconnect |
| In-race transponder link dialog displays and credits laps | TIMING-08 | Multi-step UI interaction | Start race, emit unknown transponder, verify alert appears; use link dialog; verify lap count credited retroactively |
| Admin token page generate/revoke flow | FORWARDER-05 | Browser admin workflow | Log in as ADMIN, navigate to forwarder token page, generate token, copy, revoke |
| End-to-end: simulator → gRPC → STOMP → browser lap update | TIMING-03 | Full stack integration without hardware | Run simulator + app; open live timing panel; verify lap times appear in real time |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
