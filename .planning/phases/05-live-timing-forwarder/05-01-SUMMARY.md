---
phase: 05-live-timing-forwarder
plan: "01"
subsystem: forwarder
tags: [test-scaffolding, wave-0, junit5, disabled-stubs]
dependency_graph:
  requires: []
  provides:
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/Rc4TextParserTest.java
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/EpochAnchorTest.java
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/GapDetectionTest.java
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/AmbRc4TimingSourceIT.java
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/TimingSourceTest.java
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/ReconnectBehaviourTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenServiceTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderGrpcServiceIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/timing/LiveRaceStateRetroactiveTest.java
  affects: []
tech_stack:
  added:
    - "JUnit Jupiter 5.10.2 (testImplementation in :forwarder)"
    - "junit-platform-launcher 1.10.2 (testRuntimeOnly in :forwarder)"
  patterns:
    - "@Disabled class-level stubs with Assertions.fail() bodies — Wave-0 Nyquist compliance"
key_files:
  created:
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/Rc4TextParserTest.java
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/EpochAnchorTest.java
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/GapDetectionTest.java
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/AmbRc4TimingSourceIT.java
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/TimingSourceTest.java
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/timing/ReconnectBehaviourTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenServiceTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderGrpcServiceIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/timing/LiveRaceStateRetroactiveTest.java
  modified:
    - forwarder/build.gradle.kts
decisions:
  - "Wave-0 stubs use class-level @Disabled (not method-level) to match plan spec; all 9 files compile and skip cleanly"
  - "forwarder/build.gradle.kts adds only junit-jupiter:5.10.2 and junit-platform-launcher:1.10.2 — no Netty/gRPC/protobuf until Plan 02"
metrics:
  duration: "3 minutes"
  completed: "2026-04-26"
  tasks_completed: 3
  files_created: 9
  files_modified: 1
---

# Phase 05 Plan 01: Wave-0 Test Scaffolding Summary

**One-liner:** Nine @Disabled JUnit 5 stub test files created across :forwarder and :app modules, enabling Nyquist compliance for Plans 02–04 verify commands.

## Objective

Wave 0 test scaffolding — create one @Disabled JUnit 5 test stub per Wave-1+ deliverable so all subsequent tasks have an `<automated>` verify command pointing to a real file. All stubs compile under `./gradlew :forwarder:test :app:test` with every test method annotated `@Disabled`.

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Upgrade forwarder/build.gradle.kts with JUnit 5 | c7c28c1 | forwarder/build.gradle.kts |
| 2 | Six @Disabled stubs in :forwarder test tree | f6f0768 | 6 new test files |
| 3 | Three @Disabled stubs in :app test tree | b91c018 | 3 new test files |

## Test Stubs Created

### :forwarder module (6 files, 19 test methods)

| File | Requirements | Methods |
|------|-------------|---------|
| Rc4TextParserTest.java | FORWARDER-02, TIMING-07 | 5 |
| EpochAnchorTest.java | TIMING-04 | 4 |
| GapDetectionTest.java | TIMING-07 | 3 |
| AmbRc4TimingSourceIT.java | FORWARDER-01, TIMING-02 | 3 |
| TimingSourceTest.java | TIMING-05 | 2 |
| ReconnectBehaviourTest.java | TIMING-02 | 2 |

### :app module (3 files, 17 test methods)

| File | Requirements | Methods |
|------|-------------|---------|
| ForwarderTokenServiceTest.java | FORWARDER-05 | 6 |
| ForwarderGrpcServiceIT.java | FORWARDER-03, TIMING-01 | 6 |
| LiveRaceStateRetroactiveTest.java | TIMING-08, D-12 | 5 |

## Verification Results

```
./gradlew :forwarder:test :app:test --tests "dev.monkeypatch.rctiming.forwarder.*" --tests "dev.monkeypatch.rctiming.timing.LiveRaceStateRetroactiveTest"
BUILD SUCCESSFUL — all tests skipped via @Disabled
```

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

All 9 test files are intentional Wave-0 stubs. Each method body calls `Assertions.fail("Wave 1 — implement in Plan NN")` so that removing `@Disabled` without implementing the test will immediately fail red. This is the intended design per T-05-01 threat mitigation.

## Threat Flags

None — only test code was created; no production code was added or modified.

## Self-Check: PASSED

- [x] forwarder/src/test/.../Rc4TextParserTest.java exists
- [x] forwarder/src/test/.../EpochAnchorTest.java exists
- [x] forwarder/src/test/.../GapDetectionTest.java exists
- [x] forwarder/src/test/.../AmbRc4TimingSourceIT.java exists
- [x] forwarder/src/test/.../TimingSourceTest.java exists
- [x] forwarder/src/test/.../ReconnectBehaviourTest.java exists
- [x] app/src/test/.../ForwarderTokenServiceTest.java exists
- [x] app/src/test/.../ForwarderGrpcServiceIT.java exists
- [x] app/src/test/.../LiveRaceStateRetroactiveTest.java exists
- [x] Commit c7c28c1 exists (Task 1)
- [x] Commit f6f0768 exists (Task 2)
- [x] Commit b91c018 exists (Task 3)
- [x] `./gradlew :forwarder:test :app:test` exits 0
