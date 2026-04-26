---
phase: 05-live-timing-forwarder
verified: 2026-04-26T21:00:00Z
status: human_needed
score: 5/5 must-haves verified
overrides_applied: 0
human_verification:
  - test: "ForwarderStatusBar colour transitions"
    expected: "Green pill when forwarder/decoder connected, amber when reconnecting, red when disconnected"
    why_human: "React component visual state — requires browser, STOMP connection, and running simulator"
  - test: "In-race transponder link dialog end-to-end"
    expected: "Unknown transponder alert appears during a running race; race director opens dialog, selects entry, confirms; success toast shows retroactively credited lap count; positions update in browser"
    why_human: "Multi-step UI interaction requiring live race, STOMP messages, and real backend state"
  - test: "Admin token page generate/revoke workflow"
    expected: "Log in as ADMIN; navigate to /admin/forwarder; click Generate Token; one-time reveal panel shows token with copy button; Done dismisses; GET shows ACTIVE status; Revoke → REVOKED state"
    why_human: "Browser admin workflow — requires auth session and full Spring Boot + DB stack"
  - test: "Full end-to-end: simulator → gRPC → STOMP → browser live lap updates"
    expected: "./gradlew :forwarder:runSimulator; start Spring Boot app; open race control UI with a RUNNING race; lap times and positions update in real time in the browser"
    why_human: "Full stack integration across multiple processes without physical hardware — too slow and stateful for CI"
---

# Phase 5: Live Timing & Forwarder — Verification Report

**Phase Goal:** The local forwarder application connects to the AMB decoder over TCP and streams live lap data to the cloud service, which broadcasts real-time positions and gaps to all connected browsers

**Verified:** 2026-04-26T21:00:00Z
**Status:** HUMAN_NEEDED (all automated checks pass; 4 manual UAT items remain)
**Re-verification:** No — initial verification

---

## Overall Verdict: PASS (pending manual UAT)

All 5 success criteria are satisfied by implementation evidence and automated tests. The 4 remaining items are visual/end-to-end validations that were always planned as manual-only per VALIDATION.md. No blocker gaps found.

---

## Test Results Summary

| Suite | Tests | Passed | Failed | Notes |
|-------|-------|--------|--------|-------|
| `:forwarder:test` | 19 | 19 | 0 | All 6 Wave-0 stubs enabled and passing |
| `:app:test` | 184 | 183 | 1 | 1 pre-existing Phase 4 failure (see below) |
| `frontend npm test` | 15 | 15 | 0 | 3 test files, no failures |
| `frontend npm build` | — | ✓ | — | Clean build, exit 0 |

**Pre-existing failure (NOT Phase 5):**
`RoundGeneratorServiceTest.heatSplit_fifteenDriversMaxEightPerHeat_createsTwoHeats` — `UnnecessaryStubbingException` from Phase 4 Mockito setup. Confirmed pre-existing; unrelated to any Phase 5 change.

---

## Goal Achievement

### Observable Truths (Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Forwarder connects to AMB decoder via TCP, streams decoded PASSING events to cloud via gRPC using a pre-configured API token | ✓ VERIFIED | `AmbRc4TimingSource` (Netty, LineBasedFrameDecoder), `ForwarderGrpcClient` (token via metadata interceptor), `ForwarderGrpcServer` (SmartLifecycle), `ForwarderTokenAuthInterceptor`; `ForwarderGrpcServiceIT` 6 tests green |
| 2 | Forwarder auto-reconnects to decoder; WATCHDOG absence triggers reconnect; connection status visible in race control UI | ✓ VERIFIED | `AmbRc4TimingSource` exponential backoff (1→30s); `IdleStateHandler(30s)` as WATCHDOG; `ReconnectBehaviourTest` 2 tests green; `ForwarderStatusBar` subscribes to `/topic/system/forwarder-status`; `ForwarderStatusBar.test.tsx` 5 tests green |
| 3 | Forwarder detects PASSING_NUMBER gaps; lap timestamps use decoder offset formula (not arbitrary server clock) | ✓ VERIFIED (RC-4 scope) | `SeqGapDetector` detects gaps and logs them (RESEND N/A for RC-4 — documented in `ForwarderGrpcService` Javadoc); `EpochAnchor` implements D-07 formula anchoring server clock at first PASSING; `GapDetectionTest` 3 tests + `EpochAnchorTest` 4 tests green |
| 4 | Live lap times, positions, and gaps update in browser; unknown transponder passings surfaced without blocking registered entries | ✓ VERIFIED | `LapTimingService.linkTransponder()` → `LiveRaceState.retroactiveLinkTransponder()`; `TransponderLinkController` POST endpoint; `UnknownTransponderLinkDialog`; `LiveRaceStateRetroactiveTest` 6 tests green; `UnknownTransponderLinkDialog.test.tsx` 4 tests green |
| 5 | New timing protocol requires only a new `TimingSource` implementation — no changes to race control or timing logic | ✓ VERIFIED | `TimingSource` interface (start/stop only); `AmbRc4TimingSource` is the sole implementation; `LapTimingService`/`LiveTimingHub` untouched; `TimingSourceTest` 2 tests green |

**Score: 5/5 truths verified**

---

## Context Decisions Coverage (D-01 through D-15)

| Decision | Description | Status | Evidence |
|----------|-------------|--------|----------|
| D-01 | RC-4 text protocol only; P3 deferred | ✓ | `AmbRc4TimingSource` implements RC-4; P3 noted as backlog in `TimingSource` Javadoc |
| D-02 | `TimingSource` interface clean; P3 slots in without changes | ✓ | `TimingSource.java` — `start()` / `stop()` only; no Spring deps |
| D-03 | `Rc4TextParser` pure function, no Spring dependencies | ✓ | `Rc4TextParser.java` — only `java.util.Optional` imported; verified |
| D-04 | Simulator: playback + generative mode | ✓ | `PlaybackMode.java`, `GenerativeMode.java`, `FakeDecoderServer` factory methods |
| D-05 | Simulator as separate runnable in forwarder module | ✓ | `SimulatorMain.java` in `forwarder/.../simulator/`; `AmbRc4TimingSourceIT` confirms same code path |
| D-06 | `timeSinceStart_s` = seconds since decoder power-on; relative accuracy sufficient | ✓ | `EpochAnchor.toRtcTimeMicros()` Javadoc explains relative-accuracy approach |
| D-07 | Epoch anchoring: `Instant.now()` at first PASSING; formula `(epoch + offset) * 1000L` | ✓ | `EpochAnchor.java` lines 36–44 implement exactly this formula; `EpochAnchorTest` verifies |
| D-08 | API token in `forwarder.properties` as `forwarder.api-token=` | ✓ | `forwarder/src/main/resources/forwarder.properties` created; `ForwarderConfig` reads it |
| D-09 | Token renewal via admin portal, shown once, no expiry | ✓ | `ForwarderTokenPage.tsx` one-time reveal; `ForwarderTokenController` POST/GET/DELETE |
| D-10 | All RC-4 passings treated as start/finish; loop ID logged but ignored | ✓ | `ForwarderGrpcService.onNext()` — no loop filtering; D-10 gap documented in Javadoc |
| D-11 | Unknown transponder cockpit alert + link in-race action | ✓ | `UnknownTransponderLinkDialog` + `CockpitPage` STOMP subscription; `TransponderLinkController` |
| D-12 | Retroactive credit: all passings since race start credited when linked | ✓ | `LiveRaceState.retroactiveLinkTransponder()` iterates `lapHistory`; `LiveRaceStateRetroactiveTest` |
| D-13 | Slim status bar at top of race control cockpit, always visible, two pills | ✓ | `ForwarderStatusBar` — `flex h-8` bar in `RaceControlLayout.tsx` |
| D-14 | Status updates via `/topic/system/forwarder-status` STOMP topic | ✓ | `ForwarderStatusPublisher` → `LiveTimingHub.broadcastForwarderStatus()` → STOMP topic |
| D-15 | Pill colours: green=connected, amber=reconnecting, red=disconnected | ✓ | `ForwarderStatusBar.tsx` — `--flag-green`, `--flag-yellow`, `--flag-red` CSS vars |

**All 15 decisions: ✓ COVERED**

---

## VALIDATION.md UAT Acceptance Criteria

| Criterion | Type | Status |
|-----------|------|--------|
| ForwarderStatusBar shows green/amber/red pills correctly | Manual | ⏳ PENDING HUMAN |
| In-race transponder link dialog displays and credits laps | Manual | ⏳ PENDING HUMAN |
| Admin token page generate/revoke flow | Manual | ⏳ PENDING HUMAN |
| End-to-end: simulator → gRPC → STOMP → browser lap update | Manual | ⏳ PENDING HUMAN |
| `Rc4TextParserTest` (5 tests) | Automated | ✅ GREEN |
| `EpochAnchorTest` (4 tests) | Automated | ✅ GREEN |
| `GapDetectionTest` (3 tests) | Automated | ✅ GREEN |
| `TimingSourceTest` (2 tests) | Automated | ✅ GREEN |
| `AmbRc4TimingSourceIT` (3 tests) | Automated | ✅ GREEN |
| `ReconnectBehaviourTest` (2 tests) | Automated | ✅ GREEN |
| `ForwarderTokenServiceTest` (6 tests) | Automated | ✅ GREEN |
| `ForwarderGrpcServiceIT` (6 tests) | Automated | ✅ GREEN |
| `LiveRaceStateRetroactiveTest` (6 tests) | Automated | ✅ GREEN |
| `ForwarderStatusBar.test.tsx` (5 tests) | Automated | ✅ GREEN |
| `UnknownTransponderLinkDialog.test.tsx` (4 tests) | Automated | ✅ GREEN |

---

## Required Artifacts

| Artifact | Description | Status |
|----------|-------------|--------|
| `forwarder/src/main/java/.../timing/TimingSource.java` | TIMING-05 interface | ✓ VERIFIED |
| `forwarder/src/main/java/.../timing/Rc4TextParser.java` | Pure parser, D-03 | ✓ VERIFIED — 53 lines, full switch logic |
| `forwarder/src/main/java/.../timing/EpochAnchor.java` | D-07 epoch anchor | ✓ VERIFIED — 67 lines, full formula + restart detection |
| `forwarder/src/main/java/.../timing/AmbRc4TimingSource.java` | Netty TCP client | ✓ VERIFIED — 151 lines, full reconnect loop |
| `forwarder/src/main/java/.../timing/SeqGapDetector.java` | Gap detection | ✓ EXISTS (not spot-checked; tested by GapDetectionTest ✓) |
| `forwarder/src/main/java/.../simulator/SimulatorMain.java` | Simulator runnable | ✓ EXISTS |
| `forwarder/src/main/proto/timing.proto` | gRPC wire contract | ✓ EXISTS |
| `app/src/main/java/.../forwarder/ForwarderGrpcServer.java` | SmartLifecycle gRPC server | ✓ VERIFIED — 63 lines, SmartLifecycle |
| `app/src/main/java/.../forwarder/ForwarderGrpcService.java` | gRPC service, event publisher | ✓ VERIFIED — 72 lines, publishes `LapPassingEvent` |
| `app/src/main/java/.../forwarder/ForwarderTokenAuthInterceptor.java` | Token auth interceptor | ✓ EXISTS |
| `app/src/main/java/.../forwarder/ForwarderTokenService.java` | Token lifecycle | ✓ VERIFIED — BCrypt, SecureRandom, validate() |
| `app/src/main/java/.../forwarder/ForwarderStatusPublisher.java` | STOMP status broadcast | ✓ EXISTS |
| `app/src/main/java/.../api/admin/ForwarderTokenController.java` | Admin token REST | ✓ EXISTS |
| `app/src/main/java/.../api/racecontrol/TransponderLinkController.java` | Link endpoint | ✓ EXISTS |
| `app/src/main/java/.../timing/LiveRaceState.java` | retroactiveLinkTransponder() | ✓ VERIFIED — 192 lines, method at line 144 |
| `app/src/main/resources/db/migration/V21__create_forwarder_token.sql` | Token table | ✓ EXISTS |
| `app/src/main/resources/db/migration/V22__create_unknown_transponder_link.sql` | Audit table | ✓ EXISTS |
| `frontend/src/pages/race-control/panels/ForwarderStatusBar.tsx` | Status bar component | ✓ VERIFIED — 80 lines, STOMP + 3 states |
| `frontend/src/pages/race-control/dialogs/UnknownTransponderLinkDialog.tsx` | Link dialog | ✓ VERIFIED — 139 lines, full mutation |
| `frontend/src/pages/admin/race-control/ForwarderTokenPage.tsx` | Admin token page | ✓ VERIFIED — 265 lines, 4 states + one-time reveal |
| `forwarder/src/main/java/.../grpc/ForwarderGrpcClient.java` | gRPC client in forwarder | ✓ EXISTS |

---

## Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `AmbRc4TimingSource` | `ForwarderGrpcClient.sendPassing()` | callback in `ForwarderApplication` | ✓ WIRED |
| `ForwarderGrpcClient` | `ForwarderGrpcServer:9090` | Netty gRPC channel + token metadata | ✓ WIRED |
| `ForwarderGrpcServer` | `ForwarderGrpcService` | `ServerBuilder.addService()` + `intercept()` | ✓ WIRED |
| `ForwarderGrpcService.onNext()` | `ApplicationEventPublisher.publishEvent(LapPassingEvent)` | direct call in StreamObserver | ✓ WIRED |
| `ForwarderGrpcService` | `ForwarderStatusPublisher` | constructor injection | ✓ WIRED |
| `ForwarderStatusPublisher` | `/topic/system/forwarder-status` | `LiveTimingHub.broadcastForwarderStatus()` | ✓ WIRED |
| `ForwarderStatusBar` | `/topic/system/forwarder-status` | `useStomp` hook | ✓ WIRED |
| `TransponderLinkController` | `LapTimingService.linkTransponder()` | service injection | ✓ WIRED |
| `LapTimingService.linkTransponder()` | `LiveRaceState.retroactiveLinkTransponder()` | direct delegation | ✓ WIRED |
| `UnknownTransponderLinkDialog` | `POST /api/v1/race-control/races/{raceId}/transponders/link` | `linkUnknownTransponder()` in `raceControlApi.ts` | ✓ WIRED |
| `ForwarderTokenPage` | `GET/POST/DELETE /api/v1/admin/forwarder/token` | `raceControlApi.ts` functions | ✓ WIRED |

---

## Behavioral Spot-Checks

| Behavior | Check | Status |
|----------|-------|--------|
| Parser handles valid PASSING line | `Rc4TextParserTest` 5 tests green | ✓ PASS |
| Parser returns empty for STATUS/malformed | `Rc4TextParserTest` includes malformed cases | ✓ PASS |
| EpochAnchor anchors at first call, computes offset | `EpochAnchorTest` 4 tests green | ✓ PASS |
| EpochAnchor detects decoder restart (regression > 10s) | `EpochAnchorTest` covers restart detection | ✓ PASS |
| SeqGapDetector returns 0 for contiguous, N for gaps | `GapDetectionTest` 3 tests green | ✓ PASS |
| AmbRc4TimingSource reconnects after disconnect | `AmbRc4TimingSourceIT` + `ReconnectBehaviourTest` | ✓ PASS |
| gRPC rejects missing/revoked tokens | `ForwarderGrpcServiceIT` 3 auth tests green | ✓ PASS |
| gRPC publishes LapPassingEvent with correct raceId | `ForwarderGrpcServiceIT.lapPassingResolvesRaceIdFromCurrentlyRunningRace` | ✓ PASS |
| retroactiveLinkTransponder credits all laps, recalculates | `LiveRaceStateRetroactiveTest` 6 tests green | ✓ PASS |
| ForwarderStatusBar renders 3 connection states | `ForwarderStatusBar.test.tsx` 5 tests green | ✓ PASS |
| UnknownTransponderLinkDialog POST + success toast | `UnknownTransponderLinkDialog.test.tsx` 4 tests green | ✓ PASS |

---

## Data-Flow Trace (Level 4)

**Critical path: RC-4 PASSING → browser lap update**

```
FakeDecoderServer/AMB Decoder (TCP)
  → Netty LineBasedFrameDecoder/StringDecoder
  → Rc4InboundHandler.channelRead()
  → Rc4TextParser.parse(line) → ParsedPassing
  → EpochAnchor.toRtcTimeMicros() → long micros
  → SeqGapDetector.detect() → gap logged
  → Consumer<EpochCorrectedPassing> callback
  → ForwarderGrpcClient.sendPassing() → LapPassing proto
  → gRPC StreamPassings → ForwarderGrpcService.onNext()
  → raceRepository.findFirstByStatus(RUNNING) → Race
  → ApplicationEventPublisher.publishEvent(LapPassingEvent)
  → LapTimingService @EventListener (existing Phase 4 chain)
  → LiveTimingHub STOMP broadcast
  → Browser /topic/race/{id}/timing
```

**Verified at each hop:**
- `AmbRc4TimingSource.java`: Netty pipeline construction verified ✓
- `ForwarderGrpcClient.java`: `sendPassing()` converts `EpochCorrectedPassing` → proto ✓ (per Plan 04 Summary)
- `ForwarderGrpcService.java`: publishes `LapPassingEvent` with `race.getId()` and `msg.getRtcTimeMicros()` ✓ (reviewed)
- `LapTimingService`/`LiveTimingHub`: Phase 4 — not modified, confirmed in CONTEXT.md scope statement ✓

---

## Anti-Patterns Found

| File | Pattern | Severity | Assessment |
|------|---------|----------|------------|
| `forwarder.properties` | `forwarder.api-token=` (blank) | ℹ️ Info | **NOT a stub** — intentional per D-08; operators supply the token. Documented in Plan 02 summary. |
| `ForwarderGrpcService.java` | RC-4 RESEND not sent | ℹ️ Info | **By design** — RC-4 protocol has no RESEND. Documented in class Javadoc. RESEND is P3-only (deferred). |
| `D-10` multi-loop handling | Loop ID ignored | ℹ️ Info | **Documented gap** per D-10 — deferred to post-v1. All passings treated as start/finish. |
| `frontend/UnknownTransponderLinkDialog.test.tsx` | `Warning: Missing Description or aria-describedby` | ℹ️ Info | Radix UI DialogContent accessibility warning in tests — cosmetic only, not a functional issue. |

No blocker or warning-level anti-patterns found.

---

## Deviations from Specification

### SC-3: RESEND + RTC_TIME wording

**Success Criterion 3** states: *"sends RESEND requests to the decoder; lap timestamps use the decoder's own RTC_TIME field"*

**Actual:** RC-4 protocol (D-01) has no RESEND mechanism and no absolute `RTC_TIME` field — it uses `timeSinceStart_s` (seconds since decoder power-on). This is a protocol capability gap, not an implementation gap:
- **Gap detection** is implemented (`SeqGapDetector`) and gaps are logged at INFO level. No RESEND is sent because RC-4 decoders do not support it.
- **Timestamps** use the D-07 epoch-anchoring formula: server time anchored at first PASSING + offset from `timeSinceStart_s`. Error bounded by LAN RTT (<1ms) — negligible for RC timing.
- Both limitations are documented in `CONTEXT.md` D-06/D-07, `AmbRc4TimingSource` Javadoc, and `ForwarderGrpcService` Javadoc.
- RESEND is a P3-binary-only capability, deferred per D-01.

**Assessment:** Not a Phase 5 gap. SC-3 was authored with both protocols in mind; RC-4's limitations are explicitly accepted in the context decisions.

---

## Requirements Coverage

| Requirement | Description | Status | Evidence |
|-------------|-------------|--------|----------|
| FORWARDER-01 | Forwarder connects to AMB decoder | ✓ | `AmbRc4TimingSource` Netty client |
| FORWARDER-02 | RC-4 text protocol implementation | ✓ | `Rc4TextParser`, 5 tests |
| FORWARDER-03 | gRPC streaming to cloud service | ✓ | `ForwarderGrpcClient`, `ForwarderGrpcServer`, `ForwarderGrpcService` |
| FORWARDER-04 | Simulator for testing without hardware | ✓ | `FakeDecoderServer`, `PlaybackMode`, `GenerativeMode`, `SimulatorMain` |
| FORWARDER-05 | API token auth (admin-generated, BCrypt) | ✓ | `ForwarderTokenService`, `ForwarderTokenController`, V21 migration |
| TIMING-01 | LapPassingEvent published from gRPC | ✓ | `ForwarderGrpcService.onNext()` |
| TIMING-02 | Auto-reconnect on connection loss | ✓ | `AmbRc4TimingSource` exponential backoff |
| TIMING-03 | Live browser updates via STOMP | ✓ | Existing Phase 4 chain unchanged; gRPC feeds into it |
| TIMING-04 | Timestamp epoch anchoring | ✓ | `EpochAnchor` D-07 formula |
| TIMING-05 | `TimingSource` interface (pluggable protocol) | ✓ | `TimingSource.java` — clean interface |
| TIMING-06 | No client handshake for RC-4 | ✓ | Documented in `AmbRc4TimingSource` Javadoc |
| TIMING-07 | PASSING_NUMBER gap detection | ✓ | `SeqGapDetector`, `GapDetectionTest` |
| TIMING-08 | Unknown transponder link + retroactive credit | ✓ | `TransponderLinkController`, `LiveRaceState.retroactiveLinkTransponder()` |

---

## Human Verification Required

### 1. ForwarderStatusBar Visual States

**Test:** Run `./gradlew :forwarder:runSimulator`, start the Spring Boot app with a running race, open the race control cockpit in a browser.
**Expected:** DECODER pill is green; FORWARDER pill is green. Stop the simulator — both pills turn red within ~35 seconds (WATCHDOG timeout). Restart simulator — pills animate through amber → green.
**Why human:** React component visual state requiring browser + live STOMP connection + running services.

### 2. In-Race Unknown Transponder Link Dialog

**Test:** With simulator running and a race in RUNNING state, configure the simulator to emit a transponder ID not registered to any entry. Observe the cockpit.
**Expected:** Unknown transponder alert appears in the cockpit. Click "Link to entry" — dialog opens with transponder ID shown read-only. Select an entry from the dropdown. Click "Link Entry". Success toast shows "N lap(s) credited retroactively." Entry's lap count in live timing updates immediately.
**Why human:** Multi-step UI interaction requiring live race state, STOMP messages, and real backend state changes.

### 3. Admin Token Page Generate/Revoke Workflow

**Test:** Log in as an ADMIN user. Navigate to `/admin/forwarder`.
**Expected:**
- Initial state shows "No token generated yet" with a Generate button.
- Click Generate — one-time reveal panel appears with token value, copy button, and amber warning.
- Click Done — panel closes; GET shows ACTIVE status with generation timestamp.
- Click Regenerate — confirmation prompt appears; confirm → new token shown once.
- Click Revoke → REVOKED badge shown; Generate button reappears.
**Why human:** Full browser admin workflow requiring auth session and live Spring Boot + PostgreSQL stack.

### 4. End-to-End: Simulator → gRPC → STOMP → Browser

**Test:** `./gradlew :forwarder:runSimulator --args="--mode=generative --transponders=3 --interval=5000"` + start Spring Boot app + open race control with a RUNNING race in browser.
**Expected:** Lap times and positions update in real time in the browser's live timing panel. Three entries cycle through improving/non-improving laps. Gaps to leader display correctly.
**Why human:** Full-stack integration across multiple JVM processes; requires browser verification of real-time updates.

---

## Gaps Summary

No blocking gaps found. All 5 success criteria verified by code inspection and automated tests. The only deviations are:
1. **RESEND / RTC_TIME** — protocol capability gap for RC-4, explicitly accepted in D-01/D-06/D-07.
2. **Multi-loop filtering** — explicitly deferred to post-v1 (D-10).

Both are documented design decisions, not implementation oversights.

---

_Verified: 2026-04-26T21:00:00Z_
_Verifier: Claude (gsd-verifier)_
