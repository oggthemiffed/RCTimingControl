---
plan: 01-03
phase: 01-domain-foundation
status: complete
started: 2026-04-16
completed: 2026-04-16
tasks_total: 2
tasks_completed: 2
deviation_count: 1
self_check: PASSED
---

## Summary

Race format configuration domain: sealed interface with Jackson polymorphism, 3 format
record subtypes, Hypersistence Utils JSONB entities, and format service with override
merge logic. 12/12 unit tests pass (8 serde + 4 merge) without Docker.

## What Was Built

**Task 1 — Sealed interface, records, enums, serde tests:**
- `RaceFormatConfig` — sealed interface with `@JsonTypeInfo` + `@JsonSubTypes` (discriminator on interface, not subtypes)
- `TimedRaceConfig` — record: `durationMinutes`, `startType`, `qualifyingType`, `racePaddingMinutes`, `staggerIntervalSeconds`
- `BumpUpConfig` — record: `qualifyingHeats`, `heatDurationMinutes`, `bestHeatsCount`, `gridSize`, `bumpSpots`, `qualifyingStartType`, `finalsStartType`, `qualifyingType`, `racePaddingMinutes`, `staggerIntervalSeconds`
- `PointsFinalsConfig` — record: `qualifyingHeats`, `finalsCount`, `finalDurationMinutes`, `heatDurationMinutes`, `qualifyingStartType`, `finalsStartType`, `qualifyingType`, `racePaddingMinutes`, `staggerIntervalSeconds`
- `StartType` — enum: STAGGER, GRID, ROLLING
- `QualifyingType` — enum: FTQ, ROUND_BY_ROUND, FASTEST_LAP, CONSECUTIVE_LAPS
- `RaceFormatConfigSerdeTest` — 8 tests: JSON round-trips for all 3 formats, YAML round-trip, unknown type rejection (InvalidTypeIdException), extra field tolerance, enum serde

**Task 2 — Entities, service, merge tests:**
- `RaceFormatTemplate` — `@Entity` with `@Type(JsonType.class)` JSONB `config` column
- `RaceFormatTemplateRepository` — JpaRepository
- `EventClass` — `@Entity` with `configSnapshot` (RaceFormatConfig, not null) + `configOverride` (Map<String,Object>, nullable), `@ManyToOne template`
- `EventClassRepository` — JpaRepository
- `RaceFormatService` — `getEffectiveConfig(EventClass)` merges snapshot+override map; `assignTemplateToEventClass(RaceFormatTemplate)` deep-copies via serialize/deserialize
- `RaceFormatServiceTest` — 4 tests: null override passthrough, field override applies, unknown override keys ignored, deep copy verified

## Key Files Created

```
key-files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/RaceFormatConfig.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/TimedRaceConfig.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/BumpUpConfig.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/PointsFinalsConfig.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/StartType.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/QualifyingType.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/RaceFormatTemplate.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/EventClass.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/RaceFormatService.java
    - app/src/test/java/dev/monkeypatch/rctiming/domain/format/RaceFormatConfigSerdeTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/domain/format/RaceFormatServiceTest.java
  modified:
    - app/build.gradle.kts (added useJUnitPlatform())
```

## Verification

- `./gradlew :app:test --tests "*.RaceFormatConfigSerdeTest"`: 8/8 PASS
- `./gradlew :app:test --tests "*.RaceFormatServiceTest"`: 4/4 PASS
- `@JsonTypeInfo` appears only on `RaceFormatConfig.java` (sealed interface)
- `JsonBinaryType` not used anywhere — `JsonType.class` used throughout
- `grep -r "localStorage" frontend/src/`: no token storage in localStorage

## Deviations

- **useJUnitPlatform()** added to `app/build.gradle.kts` — omitted from plan 01-01 build scaffold; required for JUnit 5 test discovery. Corrective action applied during plan 01-03.

## Requirements Coverage

- FORMAT-01: TimedRaceConfig (timed race format)
- FORMAT-02: BumpUpConfig (bump-up format)
- FORMAT-04: PointsFinalsConfig (points finals format)
- FORMAT-05: RaceFormatTemplate entity
- FORMAT-06: configSnapshot deep copy on assignment
- FORMAT-07: configOverride map on EventClass
- FORMAT-08: StartType enum
- FORMAT-09: QualifyingType enum
- FORMAT-10: racePaddingMinutes on all configs
- FORMAT-11: staggerIntervalSeconds on all configs
- FORMAT-12: BumpUpConfig.bestHeatsCount (best-X-from-Y)
- FORMAT-13: BumpUpConfig.bumpSpots
- FORMAT-14: JSON/YAML round-trip verified by serde tests

## Self-Check: PASSED
