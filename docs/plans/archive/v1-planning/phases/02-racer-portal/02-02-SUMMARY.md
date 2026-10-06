---
phase: "02"
plan: "02"
subsystem: cars
tags: [cars, car-tags, jooq, domain, api, integration-tests]
depends_on: ["02-01"]
provides:
  - Car JPA entity + repository + ownership-enforced service
  - CarTagCategory entity + repository + service (admin CRUD)
  - CarTagValue entity + repository (upsert/delete)
  - CarQueryService: first jOOQ service in codebase (Pattern 1 seam)
  - CarController at /api/v1/racer/cars (RACER-scoped CRUD + tags)
  - CarTagCategoryController at /api/v1/admin/car-tag-categories (admin CRUD)
  - CarControllerIT: 8 integration tests
  - CarTagCategoryIT: 4 integration tests
affects:
  - app/src/main/java/dev/monkeypatch/rctiming/domain/car/
  - app/src/main/java/dev/monkeypatch/rctiming/query/car/
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/
  - app/src/main/java/dev/monkeypatch/rctiming/api/admin/
tech_stack:
  added:
    - jOOQ DSL queries (first use in codebase via CarQueryService)
  patterns:
    - Pattern 1 Hibernate/jOOQ seam established: Hibernate write side, jOOQ read side
    - Ownership filter: getCarOrThrow(carId, userId) filters by userId after findById
    - Two round-trip jOOQ projection (cars then tags) avoids Cartesian explosion
    - CarTagValue uses ManyToOne(LAZY) to Car (write-side convenience only)
    - CarTagCategory.sortOrder mapped as smallint to match DB schema (columnDefinition = "smallint")
key_files:
  created:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/car/Car.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategory.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategoryRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagValue.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagValueRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarService.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategoryService.java
    - app/src/main/java/dev/monkeypatch/rctiming/query/car/CarQueryService.java
    - app/src/main/java/dev/monkeypatch/rctiming/query/car/CarWithTagsDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/CarController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/CarDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/CreateCarRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/UpdateCarRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/SetCarTagRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/admin/CarTagCategoryController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/admin/dto/CarTagCategoryDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/admin/dto/CreateCarTagCategoryRequest.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/racer/CarControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/admin/CarTagCategoryIT.java
  modified: []
decisions:
  - "CarTagCategory.sortOrder mapped as smallint (short in Java with columnDefinition='smallint') to match V10 migration column type; using int caused Hibernate schema-validation failure SchemaManagementException: found [int2 (SMALLINT)] but expecting [integer (INTEGER)]"
  - "CarQueryService uses two round-trips (select cars, then select tags IN carIds) rather than LEFT JOIN; avoids Cartesian explosion on cars x tags and is more legible"
  - "CarTagValue stores categoryId as plain Long FK column (not ManyToOne to CarTagCategory) for read-side simplicity; category name is resolved in CarQueryService JOIN"
  - "anonymous_returns401 test accepts 401 or 403; Spring Security returns 403 for URL-level hasAnyRole matchers when no auth token is present, consistent with existing SecurityIT behavior"
metrics:
  duration_minutes: 12
  completed_date: "2026-04-17"
  tasks_completed: 3
  tasks_total: 3
  files_created: 20
  files_modified: 0
---

# Phase 02 Plan 02: Cars Domain + API Summary

**One-liner:** Car CRUD domain with ownership enforcement, first jOOQ read projection (CarQueryService), admin tag-category CRUD, and 12 integration tests covering RACER-02/06/10/11.

## What Was Built

### Car Domain Package Structure

```
domain/car/
  Car.java                     — @Entity @Table(name="cars"), userId FK as Long
  CarRepository.java           — findByUserIdAndArchivedFalse(Long)
  CarTagCategory.java          — @Entity @Table(name="car_tag_categories"), sortOrder as short
  CarTagCategoryRepository.java — findAllByOrderBySortOrderAsc()
  CarTagValue.java             — @Entity @Table(name="car_tag_values"), ManyToOne(LAZY) to Car
  CarTagValueRepository.java   — findByCar_Id, findByCar_IdAndCategoryId, deleteByCar_IdAndCategoryId
  CarService.java              — create/update/archive/setTag/deleteTag with getCarOrThrow ownership filter
  CarTagCategoryService.java   — findAll/findById/create/update/delete (analog of RacingClassService)

query/car/
  CarWithTagsDto.java          — record: id, userId, name, primaryClassId, notes, archived, Map<String,String> tags
  CarQueryService.java         — @Transactional(readOnly=true), DSLContext, two-query projection

api/racer/
  CarController.java           — /api/v1/racer/cars, @PreAuthorize("hasRole('RACER')")
  dto/CarDto.java              — mutation response (no tags)
  dto/CreateCarRequest.java    — @NotBlank @Size(max=100) name
  dto/UpdateCarRequest.java    — all nullable (PATCH semantics)
  dto/SetCarTagRequest.java    — @NotNull categoryId, @NotBlank value

api/admin/
  CarTagCategoryController.java — /api/v1/admin/car-tag-categories, @PreAuthorize("hasAnyRole('ADMIN','RACE_DIRECTOR','REFEREE')")
  dto/CarTagCategoryDto.java   — id, name, sortOrder with from(CarTagCategory)
  dto/CreateCarTagCategoryRequest.java — @NotBlank name, nullable sortOrder
```

### jOOQ DSL Imports Used

```java
import dev.monkeypatch.rctiming.jooq.generated.tables.Cars;       // Cars.CARS
import dev.monkeypatch.rctiming.jooq.generated.tables.CarTagCategories;  // CAR_TAG_CATEGORIES
import dev.monkeypatch.rctiming.jooq.generated.tables.CarTagValues;      // CAR_TAG_VALUES
```

Fields used: `CARS.ID`, `CARS.USER_ID`, `CARS.NAME`, `CARS.PRIMARY_CLASS_ID`, `CARS.NOTES`, `CARS.ARCHIVED`, `CARS.CREATED_AT`, `CTC.ID`, `CTC.NAME`, `CTV.CAR_ID`, `CTV.CATEGORY_ID`, `CTV.VALUE`

### Hibernate Relationship Choices

- `Car.userId`: stored as plain `Long` FK column — avoids cross-aggregate lazy loading to `User`
- `CarTagValue.car`: stored as `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="car_id")` — write-side convenience for setting the FK; never used for list reads (all reads go through `CarQueryService`)
- `CarTagValue.categoryId`: stored as plain `Long` FK column — category name resolved via JOIN in `CarQueryService`

### Integration Test Count

| Test Class | Tests | Coverage |
|-----------|-------|----------|
| CarControllerIT | 8 | create/list/update/archive/ownership/setTag/overwriteTag/deleteTag |
| CarTagCategoryIT | 4 | default 7 categories seeded/admin CRUD/racer 403/anonymous 401-or-403 |
| **Total** | **12** | RACER-02, RACER-06, RACER-10, RACER-11 |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] CarTagCategory.sortOrder type mismatch: int vs smallint**
- **Found during:** Task 3 (first test run)
- **Issue:** Hibernate schema validation failed: `found [int2 (Types#SMALLINT)], but expecting [integer (Types#INTEGER)]`. The V10 migration declares `sort_order smallint not null`, but the entity mapped it as `int`. jOOQ generated the field as `Short`, confirming the schema type.
- **Fix:** Changed `private int sortOrder` to `private short sortOrder` with `@Column(columnDefinition = "smallint")`. Getter returns `int` (widening), setter accepts `short`. Service updated to use `.shortValue()` when passing Integer from request.
- **Files modified:** `CarTagCategory.java`, `CarTagCategoryService.java`
- **Commit:** 727ef39 (service fix picked up by 02-03 agent commit 553a5d7)

**2. [Rule 1 - Bug] Test deserialization failure on 404 error body**
- **Found during:** Task 3 (admin_createUpdateDeleteCycle test)
- **Issue:** After DELETE, GET returned 404 with `application/problem+json` body. Attempting to deserialize it as `CarTagCategoryDto` threw `HttpMessageNotReadableException: Unrecognized field "type"`.
- **Fix:** Changed the verify-gone assertion to use `Map.class` as the response type, which accepts any JSON structure.
- **Files modified:** `CarTagCategoryIT.java`
- **Commit:** 727ef39

**3. [Rule 1 - Bug] anonymous_returns401 expected 401 but got 403**
- **Found during:** Task 3 (anonymous test)
- **Issue:** Spring Security returns 403 for URL-level `hasAnyRole` matchers when no authentication token is present. The existing `SecurityIT.adminEndpoint_withNoAuth_returns401or403()` documents this dual behavior.
- **Fix:** Changed assertion to `isIn(401, 403)` to match established project behavior.
- **Files modified:** `CarTagCategoryIT.java`
- **Commit:** 727ef39

## Known Stubs

None — all service methods are wired to real DB operations. CarQueryService returns live data from Postgres via jOOQ. No placeholder or hardcoded values in any response path.

## Threat Surface Scan

| Flag | File | Description |
|------|------|-------------|
| (none) | — | All endpoints follow established security patterns. CarController enforces RACER role + userId ownership filter. CarTagCategoryController enforces admin role. No new trust boundaries introduced. |

## Self-Check

All created files verified present:
- `app/src/main/java/dev/monkeypatch/rctiming/domain/car/Car.java` — FOUND
- `app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarService.java` — FOUND
- `app/src/main/java/dev/monkeypatch/rctiming/query/car/CarQueryService.java` — FOUND
- `app/src/main/java/dev/monkeypatch/rctiming/api/racer/CarController.java` — FOUND
- `app/src/main/java/dev/monkeypatch/rctiming/api/admin/CarTagCategoryController.java` — FOUND
- `app/src/test/java/dev/monkeypatch/rctiming/api/racer/CarControllerIT.java` — FOUND
- `app/src/test/java/dev/monkeypatch/rctiming/api/admin/CarTagCategoryIT.java` — FOUND

Commits verified:
- 306650a — feat(02-02): Car + CarTag entities, repositories, services, and DTOs
- 7107f89 — feat(02-02): CarQueryService (jOOQ projection) + CarWithTagsDto
- 727ef39 — feat(02-02): CarController, CarTagCategoryController, and integration tests

## Self-Check: PASSED
