---
phase: 10-docker-trial-environment
plan: 01
subsystem: infra
tags: [docker, java, gradle, forwarder, simulator, eclipse-temurin]

requires:
  - phase: 05-forwarder
    provides: ForwarderApplication, ForwarderConfig, SimulatorMain — the binaries that the Docker images wrap

provides:
  - ForwarderApplication.resolveConfig() static method with --config-file= arg support
  - docker/forwarder/Dockerfile multi-stage image built via :forwarder:installDist
  - docker/forwarder/entrypoint.sh writing forwarder.properties from FORWARDER_* env vars
  - docker/fake-decoder/Dockerfile multi-stage image exposing port 5100
  - docker/fake-decoder/simulate.sh launching SimulatorMain with pass-through args

affects: [10-docker-trial-environment/10-03, 10-docker-trial-environment/10-04]

tech-stack:
  added: [eclipse-temurin:21-jdk-alpine, eclipse-temurin:21-jre-alpine, docker multi-stage build]
  patterns:
    - "installDist (Gradle application plugin) used for forwarder/fake-decoder container images instead of fat jar"
    - "generateProto skipped in Docker build (-x generateProto) to avoid protoc-gen-grpc-java execute permission issue in Alpine container"
    - "entrypoint.sh generates properties file from env vars and uses exec to make JVM PID 1"

key-files:
  created:
    - forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderApplicationConfigArgTest.java
    - docker/forwarder/Dockerfile
    - docker/forwarder/entrypoint.sh
    - docker/fake-decoder/Dockerfile
    - docker/fake-decoder/simulate.sh
  modified:
    - forwarder/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderApplication.java

key-decisions:
  - "resolveConfig() is package-private static (not private) so tests can call it without invoking main() which blocks forever on Thread.currentThread().join()"
  - "Both forwarder and fake-decoder Dockerfiles use -x generateProto to avoid protoc-gen-grpc-java execute permission failure inside Alpine container — compiled Java code does not require proto regeneration during image build"
  - "simulate.sh passes all args through via \"$@\" so compose command block supplies --mode=generative and transponder IDs at runtime — no hardcoding in image"

patterns-established:
  - "Pattern: Gradle installDist produces bin/ + lib/ layout; fake-decoder overrides main class via java -cp /app/lib/* rather than the bin/forwarder script"
  - "Pattern: entrypoint.sh uses POSIX sh heredoc (cat > file <<PROPS...PROPS) to write .properties from env vars before exec"

requirements-completed: [SC-3]

duration: 4min
completed: 2026-05-16
---

# Phase 10 Plan 01: Docker Foundation for Forwarder and Fake-Decoder Summary

**ForwarderApplication gains --config-file= arg support (tested via TDD); forwarder and fake-decoder Docker images build using eclipse-temurin:21 + Gradle installDist multi-stage pattern**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-05-16T20:09:50Z
- **Completed:** 2026-05-16T20:14:02Z
- **Tasks:** 3 (plus TDD RED commit)
- **Files modified:** 6

## Accomplishments
- Added `resolveConfig(String[])` to ForwarderApplication — loads from filesystem path when `--config-file=<path>` is supplied, falls back to classpath default otherwise; fully backward-compatible
- Proved both branches with 3 JUnit 5 tests (TDD: RED commit then GREEN); all pass via `./gradlew :forwarder:test --tests "*ForwarderApplicationConfigArgTest*"`
- Built `docker/forwarder/Dockerfile` multi-stage image and `entrypoint.sh` that writes `/tmp/forwarder.properties` from `FORWARDER_*` env vars then execs the forwarder with `--config-file`
- Built `docker/fake-decoder/Dockerfile` multi-stage image and `simulate.sh` that launches `SimulatorMain` via `java -cp /app/lib/*` with all args passed through for compose to supply at runtime
- Both Docker builds exit 0 (verified)

## Task Commits

1. **TDD RED: ForwarderApplicationConfigArgTest** - `18b942b` (test)
2. **Task 1: Add resolveConfig() to ForwarderApplication** - `0c0315e` (feat)
3. **Task 2: Forwarder Dockerfile + entrypoint.sh** - `47e5bc3` (feat)
4. **Task 3: Fake-decoder Dockerfile + simulate.sh** - `408b618` (feat)

## Files Created/Modified
- `forwarder/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderApplication.java` - Added `resolveConfig(String[])` static method; `main()` delegates to it
- `forwarder/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderApplicationConfigArgTest.java` - 3 JUnit 5 tests covering: file-path arg, no-args fallback, unrecognised-arg fallback
- `docker/forwarder/Dockerfile` - Multi-stage: eclipse-temurin:21-jdk-alpine builder runs `:forwarder:installDist -x test -x generateProto`; 21-jre-alpine runtime copies `install/forwarder/` and entrypoint
- `docker/forwarder/entrypoint.sh` - POSIX sh heredoc writes `/tmp/forwarder.properties` from 6 `FORWARDER_*` env vars; `exec /app/bin/forwarder --config-file=/tmp/forwarder.properties`
- `docker/fake-decoder/Dockerfile` - Same multi-stage pattern; `EXPOSE 5100`; `ENTRYPOINT ["/simulate.sh"]`
- `docker/fake-decoder/simulate.sh` - `exec java -cp "/app/lib/*" dev.monkeypatch.rctiming.forwarder.simulator.SimulatorMain "$@"`

## Decisions Made
- `resolveConfig()` is package-private (not private) so the test class in the same package can call it directly without triggering `main()` which blocks forever on `Thread.currentThread().join()`
- Both Docker images skip `generateProto` (`-x generateProto`) because the `protoc-gen-grpc-java` binary downloaded by Gradle into the Alpine container cache lacks execute permission — this causes a fatal error. The Java code is compiled without issue because `compileJava` dependencies on proto-generated sources are satisfied by the cached sources in the Gradle layer
- `simulate.sh` uses `"$@"` for pass-through arguments — `--mode=generative`, `--transponders=`, `--interval-ms=`, and `--jitter-ms=` are supplied by the compose `command:` block (Plan 03) so they stay co-located with the seed data that determines transponder IDs

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Added -x generateProto to Docker Gradle build command**
- **Found during:** Task 2 (Forwarder Dockerfile verification - docker build)
- **Issue:** `./gradlew :forwarder:installDist -x test --no-daemon` inside Alpine Docker container fails with `protoc-gen-grpc-java-1.73.0-linux-x86_64.exe: program not found or is not executable` — the binary is downloaded into the Gradle cache but lacks execute permission in the container environment
- **Fix:** Added `-x generateProto` to the Gradle RUN command in both Dockerfiles. Proto sources are already compiled in `compileJava` via cached intermediate layers so the build succeeds
- **Files modified:** docker/forwarder/Dockerfile, docker/fake-decoder/Dockerfile
- **Verification:** Both `docker build` commands exit 0
- **Committed in:** `47e5bc3`, `408b618` (Task 2 and Task 3 commits)

---

**Total deviations:** 1 auto-fixed (Rule 1 - bug in Gradle proto plugin execution in Alpine)
**Impact on plan:** Required fix for Docker build to succeed. The `-x generateProto` flag is correct because proto code generation requires the protoc binary to be executable, and is not needed during runtime image build when the Java bytecode is already produced.

## Issues Encountered
- `protoc-gen-grpc-java` execute permission failure inside eclipse-temurin:21-jdk-alpine — downloaded binary is not marked executable in Docker Gradle cache layer. Fixed by skipping `generateProto` task (the compiled classes are present from `compileJava` which runs against already-generated sources in the Gradle cache).

## Known Stubs
None — this plan only adds infrastructure files (Dockerfiles, shell scripts) and a Java method. No UI or data-rendering code.

## Threat Flags
None — Dockerfile EXPOSE 5100 on fake-decoder is documentation only per T-10-04; the host port binding is enforced by compose (Plan 03), not the Dockerfile.

## Next Phase Readiness
- Plan 02 (app Dockerfile + frontend Dockerfile) can proceed independently — these images do not depend on forwarder/fake-decoder
- Plan 03 (docker-compose.trial.yml) now has the forwarder and fake-decoder image build definitions it needs to wire the services
- Plan 04 (.env.example) can reference FORWARDER_* env vars confirmed in entrypoint.sh

---
*Phase: 10-docker-trial-environment*
*Completed: 2026-05-16*
