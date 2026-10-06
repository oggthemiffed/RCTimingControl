# Phase 2: Racer Portal - Pattern Map

**Mapped:** 2026-04-16
**Files analyzed:** 52 new/modified files
**Analogs found:** 49 / 52 (3 have no close analog — documented in No Analog Found section)

---

## File Classification

### Backend — Flyway Migrations

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `app/src/main/resources/db/migration/V6__extend_users_racer_fields.sql` | migration | — | `V1__create_users_and_roles.sql` | exact |
| `app/src/main/resources/db/migration/V7__create_user_governing_body_memberships.sql` | migration | — | `V3__create_tracks.sql` (FK + index pattern) | role-match |
| `app/src/main/resources/db/migration/V8__create_user_class_ratings.sql` | migration | — | `V4__create_racing_classes.sql` | role-match |
| `app/src/main/resources/db/migration/V9__create_cars.sql` | migration | — | `V3__create_tracks.sql` | role-match |
| `app/src/main/resources/db/migration/V10__create_car_tags.sql` | migration | — | `V1__create_users_and_roles.sql` (two-table pattern) | role-match |
| `app/src/main/resources/db/migration/V11__create_transponders.sql` | migration | — | `V4__create_racing_classes.sql` (unique constraint) | role-match |
| `app/src/main/resources/db/migration/V12__create_events.sql` | migration | — | `V3__create_tracks.sql` | role-match |
| `app/src/main/resources/db/migration/V13__create_entries.sql` | migration | — | `V5__create_race_formats.sql` (multi-column, CHECK) | role-match |
| `app/src/test/resources/db/migration/V100__test_seed_events.sql` | migration | — | `V4__create_racing_classes.sql` (INSERT seeding) | role-match |

### Backend — Domain Entities & Repositories

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `domain/car/Car.java` | model | CRUD | `domain/raceclass/RacingClass.java` | exact |
| `domain/car/CarRepository.java` | service | CRUD | `domain/track/TrackRepository.java` | exact |
| `domain/car/CarTagCategory.java` | model | CRUD | `domain/raceclass/RacingClass.java` | exact |
| `domain/car/CarTagCategoryRepository.java` | service | CRUD | `domain/track/TrackRepository.java` | exact |
| `domain/car/CarTagValue.java` | model | CRUD | `domain/track/DecoderLoop.java` (FK to parent) | role-match |
| `domain/car/CarTagValueRepository.java` | service | CRUD | `domain/track/DecoderLoopRepository.java` | role-match |
| `domain/car/CarService.java` | service | CRUD | `domain/raceclass/RacingClassService.java` | exact |
| `domain/car/CarTagCategoryService.java` | service | CRUD | `domain/raceclass/RacingClassService.java` | exact |
| `domain/transponder/Transponder.java` | model | CRUD | `domain/raceclass/RacingClass.java` (unique constraint) | exact |
| `domain/transponder/TransponderRepository.java` | service | CRUD | `domain/user/UserRepository.java` (custom finder) | role-match |
| `domain/transponder/TransponderService.java` | service | CRUD | `domain/raceclass/RacingClassService.java` | exact |
| `domain/event/Event.java` | model | CRUD | `domain/raceclass/RacingClass.java` | exact |
| `domain/event/EventRepository.java` | service | CRUD | `domain/track/TrackRepository.java` | exact |
| `domain/entry/Entry.java` | model | CRUD | `domain/raceclass/RacingClass.java` (enum status) | exact |
| `domain/entry/EntryAuditLog.java` | model | CRUD | `domain/auth/PasswordResetToken.java` (audit trail) | role-match |
| `domain/entry/EntryRepository.java` | service | CRUD | `domain/track/TrackRepository.java` | exact |
| `domain/entry/EntryService.java` | service | CRUD | `domain/track/TrackService.java` (multi-dependency, business rules) | role-match |
| `domain/user/UserGoverningBodyMembership.java` | model | CRUD | `domain/club/GoverningBodyAffiliation.java` | role-match |
| `domain/user/UserGoverningBodyMembershipRepository.java` | service | CRUD | `domain/user/UserRepository.java` | role-match |
| `domain/user/UserClassRating.java` | model | CRUD | `domain/raceclass/RacingClass.java` | role-match |

### Backend — jOOQ Query Services

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `query/car/CarQueryService.java` | service | CRUD | no existing jOOQ service — see No Analog Found | none |
| `query/event/EventScheduleQuery.java` | service | request-response | no existing jOOQ service — see No Analog Found | none |
| `query/entry/EntryQueryService.java` | service | CRUD | no existing jOOQ service — see No Analog Found | none |

### Backend — REST Controllers & DTOs

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `api/racer/RacerProfileController.java` | controller | request-response | `api/admin/ClubProfileController.java` | role-match |
| `api/racer/CarController.java` | controller | CRUD | `api/admin/RacingClassController.java` | exact |
| `api/racer/TransponderController.java` | controller | CRUD | `api/admin/RacingClassController.java` | exact |
| `api/racer/EntryController.java` | controller | CRUD | `api/admin/TrackController.java` (complex service) | role-match |
| `api/racer/EventScheduleController.java` | controller | request-response | `api/admin/RacingClassController.java` (GET only) | role-match |
| `api/admin/CarTagCategoryController.java` | controller | CRUD | `api/admin/RacingClassController.java` | exact |
| `api/admin/AdminEntryController.java` | controller | CRUD | `api/admin/ClubProfileController.java` (PATCH-style) | role-match |
| `api/racer/dto/*.java` (request/response records) | model | — | `api/admin/dto/RacingClassDto.java` + `CreateRacingClassRequest.java` | exact |
| `security/SecurityConfig.java` (modify) | config | request-response | `security/SecurityConfig.java` | exact (modify) |

### Backend — Tests

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `api/racer/RacerProfileControllerIT.java` | test | request-response | `api/auth/AuthControllerIT.java` (RACER token pattern) | role-match |
| `api/racer/CarControllerIT.java` | test | CRUD | `api/admin/RacingClassControllerIT.java` | exact |
| `api/racer/TransponderControllerIT.java` | test | CRUD | `api/admin/RacingClassControllerIT.java` | exact |
| `api/racer/EntryControllerIT.java` | test | CRUD | `api/admin/RacingClassControllerIT.java` | exact |
| `api/admin/AdminEntryControllerIT.java` | test | CRUD | `api/admin/RacingClassControllerIT.java` | exact |
| `api/admin/CarTagCategoryIT.java` | test | CRUD | `api/admin/RacingClassControllerIT.java` | exact |
| `api/EventScheduleControllerIT.java` | test | request-response | `api/auth/AuthControllerIT.java` (no-auth endpoint) | role-match |

### Frontend — Router / App

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/App.tsx` (modify) | config | — | `frontend/src/App.tsx` | exact (modify) |

### Frontend — Layout & Pages

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `frontend/src/pages/racer/RacerPortalLayout.tsx` | component | — | `frontend/src/components/layout/AuthLayout.tsx` (layout shell) | role-match |
| `frontend/src/pages/racer/ProfilePage.tsx` | component | request-response | `frontend/src/pages/auth/RegisterPage.tsx` (form pattern) | role-match |
| `frontend/src/pages/racer/CarsPage.tsx` | component | CRUD | `frontend/src/pages/auth/RegisterPage.tsx` (form + list) | role-match |
| `frontend/src/pages/racer/TranspondersPage.tsx` | component | CRUD | `frontend/src/pages/auth/RegisterPage.tsx` (form + list) | role-match |
| `frontend/src/pages/racer/EntriesPage.tsx` | component | request-response | `frontend/src/pages/auth/LoginPage.tsx` (query + display) | role-match |
| `frontend/src/pages/events/EventSchedulePage.tsx` | component | request-response | `frontend/src/pages/auth/LoginPage.tsx` (public, read-only) | role-match |

### Frontend — Hooks

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `frontend/src/hooks/racer/useProfile.ts` | hook | request-response | `frontend/src/hooks/useAuth.ts` (context hook) | role-match |
| `frontend/src/hooks/racer/useCars.ts` | hook | CRUD | `frontend/src/hooks/useAuth.ts` | role-match |
| `frontend/src/hooks/racer/useTransponders.ts` | hook | CRUD | `frontend/src/hooks/useAuth.ts` | role-match |
| `frontend/src/hooks/racer/useEntries.ts` | hook | CRUD | `frontend/src/hooks/useAuth.ts` | role-match |

### Frontend — Components

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `frontend/src/components/racer/CarCard.tsx` | component | — | `frontend/src/components/layout/AuthLayout.tsx` (Card usage) | role-match |
| `frontend/src/components/racer/CarEditSheet.tsx` | component | CRUD | `frontend/src/pages/auth/RegisterPage.tsx` (form pattern, new component) | role-match |
| `frontend/src/components/racer/TransponderCard.tsx` | component | — | `frontend/src/components/layout/AuthLayout.tsx` (Card usage) | role-match |
| `frontend/src/components/racer/EntrySubmitForm.tsx` | component | CRUD | `frontend/src/pages/auth/RegisterPage.tsx` (multi-field form) | role-match |

---

## Pattern Assignments

### Flyway Migration: `V6__extend_users_racer_fields.sql`

**Analog:** `app/src/main/resources/db/migration/V1__create_users_and_roles.sql`

**ALTER pattern** (lines 1–5 of V1 as reference for column naming conventions):
```sql
-- lowercase SQL, timestamptz, nullable varchar with explicit length
alter table users
    add column phone_number              varchar(30),
    add column emergency_contact_name    varchar(100),
    add column emergency_contact_phone   varchar(30),
    add column phonetic_name             varchar(255);
```

---

### Flyway Migration: `V7__create_user_governing_body_memberships.sql`

**Analog:** `app/src/main/resources/db/migration/V3__create_tracks.sql`

**Table + FK + index pattern** (V3 lines 1–15):
```sql
create table tracks (
    id           bigserial    primary key,
    name         varchar(255) not null unique,
    ...
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now()
);
create index idx_decoder_loops_track_id on decoder_loops(track_id);
```
Apply same pattern: `bigserial PK`, `bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE`, `UNIQUE (user_id, governing_body_code)`, index on user_id.

---

### Flyway Migration: `V9__create_cars.sql`, `V11__create_transponders.sql`, `V12__create_events.sql`

**Analog:** `app/src/main/resources/db/migration/V4__create_racing_classes.sql`

**Simple table with unique constraint pattern** (V4 lines 1–8):
```sql
create table racing_classes (
    id          bigserial    primary key,
    name        varchar(255) not null unique,
    description text,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now()
);
```

For `transponders`: add `UNIQUE` on `transponder_number` (system-wide, RACER-05). For `events`: add `CHECK (status IN (...))`.

---

### Flyway Migration: `V13__create_entries.sql`

**Analog:** `app/src/main/resources/db/migration/V5__create_race_formats.sql`

**Multi-column table with JSONB-like CHECK pattern** (V5 lines 1–17):
```sql
create table race_format_templates (
    id         bigserial    primary key,
    name       varchar(255) not null,
    config     jsonb        not null,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now()
);
```
Apply same formatting. `entries` needs nullable FK columns, snapshot varchar columns, `CHECK (status IN ('PENDING','CONFIRMED','WITHDRAWN'))`, and a partial unique index `WHERE status != 'WITHDRAWN'`.

---

### Domain Entity: `domain/car/Car.java` and all new entities

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/raceclass/RacingClass.java`

**Full entity template** (lines 1–46):
```java
package dev.monkeypatch.rctiming.domain.car;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "primary_class_id")
    private Long primaryClassId;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Standard getters/setters — same pattern as RacingClass lines 27–46
}
```

Key deviations from `RacingClass`:
- Store `userId` as a plain `Long` FK column (not a `@ManyToOne` to `User`) to keep the Hibernate write side simple and avoid cross-aggregate lazy-loading.
- `CarTagValue` uses `@ManyToOne` to `Car` (write-side convenience only — never used for list reads).

**Analog for `CarTagValue.java` — FK to parent entity:** `domain/track/DecoderLoop.java`:
```java
// domain/track/DecoderLoop.java lines 1–15 (ManyToOne to parent)
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "track_id", nullable = false)
private Track track;
```

**Analog for `Entry.java` — enum status column:** The existing `domain/user/Role.java` plus `User.java` (ElementCollection) establish the enum pattern, but `EntryStatus` is a standalone column (not a collection). Use:
```java
// Use @Enumerated(EnumType.STRING) but map to a varchar column with a DB CHECK constraint
@Column(nullable = false, length = 20)
@Enumerated(EnumType.STRING)
private EntryStatus status = EntryStatus.PENDING;
```

---

### Repository: all new `*Repository.java` files

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/track/TrackRepository.java` (lines 1–6)

```java
package dev.monkeypatch.rctiming.domain.car;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByUserIdAndArchivedFalse(Long userId);
    // CarTagValue pattern:
    // List<CarTagValue> findByCarId(Long carId);
}
```

**Analog for `TransponderRepository.java`** — custom finder: `domain/user/UserRepository.java` (lines 1–13):
```java
public interface TransponderRepository extends JpaRepository<Transponder, Long> {
    List<Transponder> findByUserId(Long userId);
    Optional<Transponder> findByTransponderNumber(String transponderNumber);
    boolean existsByTransponderNumber(String transponderNumber);
}
```

---

### Service (Hibernate write): `domain/car/CarService.java` and `domain/transponder/TransponderService.java`

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/raceclass/RacingClassService.java`

**Full service template** (lines 1–63):
```java
package dev.monkeypatch.rctiming.domain.car;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional
public class CarService {

    private final CarRepository carRepository;

    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @Transactional(readOnly = true)
    public List<CarDto> findActiveByUser(Long userId) {
        return carRepository.findByUserIdAndArchivedFalse(userId).stream()
                .map(CarDto::from)
                .toList();
    }

    public CarDto create(Long userId, CreateCarRequest request) {
        Car car = new Car();
        car.setUserId(userId);
        car.setName(request.name());
        car.setPrimaryClassId(request.primaryClassId());
        car.setNotes(request.notes());
        car.setArchived(false);
        Instant now = Instant.now();
        car.setCreatedAt(now);
        car.setUpdatedAt(now);
        return CarDto.from(carRepository.save(car));
    }

    public void archive(Long carId, Long userId) {
        Car car = carRepository.findById(carId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + carId));
        car.setArchived(true);
        car.setUpdatedAt(Instant.now());
        // no explicit save — @Transactional dirty tracking handles it
    }

    private Car getCarOrThrow(Long carId, Long userId) {
        return carRepository.findById(carId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + carId));
    }
}
```

**Ownership check pattern** (critical for RACER security): Every service method that operates on a user-owned resource MUST filter by `userId` after `findById`. Never use `findById` alone — always chain `.filter(entity -> entity.getUserId().equals(userId))`.

**Transponder uniqueness**: The `GlobalExceptionHandler` already maps `DataIntegrityViolationException` → 409. The service does NOT need to catch it — let it propagate and the handler returns a clean 409. The DB `UNIQUE` constraint on `transponder_number` is the enforcement point.

---

### Service (Hibernate write): `domain/entry/EntryService.java`

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/domain/track/TrackService.java`

This is the most complex service — multi-dependency constructor injection pattern from TrackService (lines 1–35):
```java
@Service
@Transactional
public class EntryService {

    private final EntryRepository entryRepository;
    private final EventRepository eventRepository;
    private final TransponderRepository transponderRepository;
    private final GoverningBodyAffiliationRepository affiliationRepository;
    private final UserGoverningBodyMembershipRepository membershipRepository;

    public EntryService(EntryRepository entryRepository,
                        EventRepository eventRepository,
                        TransponderRepository transponderRepository,
                        GoverningBodyAffiliationRepository affiliationRepository,
                        UserGoverningBodyMembershipRepository membershipRepository) {
        this.entryRepository = entryRepository;
        // ...
    }

    public EntryResult submitEntry(Long userId, Long eventId, SubmitEntryRequest req) {
        // 1. Load event, assert status == OPEN
        // 2. Ownership check on transponder
        // 3. Membership check (hard block if required)
        // 4. Transponder conflict check (soft warning, non-blocking)
        // 5. Snapshot transponder details
        // 6. Save with status PENDING
        // 7. Transition to CONFIRMED in same transaction
        // Return EntryResult{entry, warnings}
    }
}
```

State transition is service-owned per D-10. Never set `entry.setStatus(CONFIRMED)` in the controller.

---

### Controller (racer-scoped): `api/racer/CarController.java`, `api/racer/TransponderController.java`

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/RacingClassController.java`

**Full CRUD controller template** (lines 1–59) — three adaptations needed:
1. URL prefix is `/api/v1/racer/cars` not `/api/v1/admin/...`
2. `@PreAuthorize("hasRole('RACER')")` instead of `hasAnyRole('ADMIN', ...)`
3. Extract authenticated userId from `SecurityContextHolder` (not from path):

```java
package dev.monkeypatch.rctiming.api.racer;

import dev.monkeypatch.rctiming.api.racer.dto.CreateCarRequest;
import dev.monkeypatch.rctiming.api.racer.dto.CarDto;
import dev.monkeypatch.rctiming.domain.car.CarService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/racer/cars")
@PreAuthorize("hasRole('RACER')")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping
    public List<CarDto> listCars(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());  // JWT subject is userId string
        return carService.findActiveByUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarDto createCar(Authentication auth, @RequestBody @Valid CreateCarRequest request) {
        Long userId = Long.parseLong(auth.getName());
        return carService.create(userId, request);
    }

    @PatchMapping("/{id}")
    public CarDto updateCar(@PathVariable Long id,
                             Authentication auth,
                             @RequestBody @Valid UpdateCarRequest request) {
        Long userId = Long.parseLong(auth.getName());
        return carService.update(id, userId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveCar(@PathVariable Long id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        carService.archive(id, userId);
    }
}
```

**JWT subject extraction pattern:** `Long.parseLong(auth.getName())` — the `JwtAuthenticationFilter` sets `claims.getSubject()` as the `Authentication.name`, and the subject is the user's `id.toString()` (see `JwtTokenService.generateAccessToken` and `AuthController.buildAuthResponse` line 151).

---

### Controller (racer-scoped): `api/racer/RacerProfileController.java`

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/ClubProfileController.java`

Get + PATCH pattern (ClubProfileController lines 34–42):
```java
@GetMapping("/profile")
public ClubProfileDto getProfile() {
    return clubProfileService.getProfile();
}

@PutMapping("/profile")
public ClubProfileDto createOrUpdateProfile(@RequestBody @Valid CreateClubProfileRequest request) {
    return clubProfileService.createOrUpdateProfile(request);
}
```
Adapt to: `@GetMapping` returns racer profile by userId from `Authentication`. `@PatchMapping` accepts a partial update DTO. Additional nested resources: `/api/v1/racer/memberships` (GET/POST/DELETE) — follow same sub-resource pattern as ClubProfileController's `/affiliations` (lines 43–65).

---

### Controller (public): `api/racer/EventScheduleController.java`

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/RacingClassController.java` (GET-only subset)

No `@PreAuthorize` — public. Requires adding `"/api/v1/events"` to `SecurityConfig.permitAll()`.

```java
@RestController
@RequestMapping("/api/v1/events")
public class EventScheduleController {

    private final EventScheduleQuery eventScheduleQuery;

    @GetMapping
    public List<EventScheduleDto> listEvents() {
        return eventScheduleQuery.getPublicSchedule();
    }
}
```

---

### Controller (admin): `api/admin/CarTagCategoryController.java`

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/RacingClassController.java` (lines 1–59)

Direct copy of `RacingClassController` with:
- URL: `/api/v1/admin/car-tag-categories`
- Service: `CarTagCategoryService`
- DTO: `CarTagCategoryDto` + `CreateCarTagCategoryRequest`

---

### Controller (admin): `api/admin/AdminEntryController.java`

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/ClubProfileController.java`

PATCH-style endpoints (no full CRUD). Pattern from ClubProfileController lines 38–65 (PUT + DELETE sub-resource pattern):
```java
@RestController
@RequestMapping("/api/v1/admin/entries")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class AdminEntryController {

    @PatchMapping("/{id}/transponder")
    public EntryDto updateTransponder(@PathVariable Long id,
                                       @RequestBody @Valid UpdateTransponderRequest request) {
        return entryService.adminUpdateTransponder(id, request);
    }

    @PostMapping("/{id}/membership-override")
    public EntryDto setMembershipOverride(@PathVariable Long id,
                                          Authentication auth,
                                          @RequestBody @Valid MembershipOverrideRequest request) {
        Long adminId = Long.parseLong(auth.getName());
        return entryService.adminSetMembershipOverride(id, adminId, request);
    }
}
```

---

### DTO Records: `api/racer/dto/*.java`

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/dto/RacingClassDto.java` and `CreateRacingClassRequest.java`

**Response DTO pattern** (RacingClassDto.java lines 1–13):
```java
public record CarDto(
        Long id,
        String name,
        Long primaryClassId,
        String notes,
        boolean archived) {

    public static CarDto from(Car car) {
        return new CarDto(car.getId(), car.getName(), car.getPrimaryClassId(),
                          car.getNotes(), car.isArchived());
    }
}
```

**Request DTO with validation** (CreateRacingClassRequest.java lines 1–9):
```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCarRequest(
        @NotBlank @Size(max = 100) String name,
        Long primaryClassId,
        @Size(max = 2000) String notes) {
}
```

---

### SecurityConfig modification: `security/SecurityConfig.java`

**Analog:** `app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java` (lines 20–41)

Add `permitAll` for public events schedule BEFORE the `anyRequest().authenticated()` catch-all (lines 27–31):
```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/error").permitAll()
        .requestMatchers("/api/v1/events", "/api/v1/events/**").permitAll()  // ADD THIS
        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "RACE_DIRECTOR", "REFEREE")
        .anyRequest().authenticated()
)
```

---

### Integration Test: `api/racer/CarControllerIT.java` and siblings

**Analog:** `app/src/test/java/dev/monkeypatch/rctiming/api/admin/RacingClassControllerIT.java`

**Full test template** (lines 1–145). Three adaptations:
1. Create a RACER user instead of ADMIN (copy `createAdminUser` and change `Set.of(Role.ADMIN)` to `Set.of(Role.RACER)`).
2. Use `registerAndLogin` helper from `AuthControllerIT` (lines 208–217) — register via the public endpoint, which auto-assigns the RACER role:

```java
// Preferred: use the public register endpoint (assigns RACER role automatically)
private String registerAndLoginRacer() {
    String email = "racer-" + UUID.randomUUID() + "@test.com";
    restTemplate.postForEntity("/api/v1/auth/register",
            Map.of("email", email, "password", "password123",
                   "firstName", "Test", "lastName", "Racer"),
            Map.class);
    ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
            "/api/v1/auth/login",
            new LoginRequest(email, "password123"),
            AuthResponse.class);
    return loginResp.getBody().accessToken();
}
```

3. `@BeforeEach` sets `racerToken` the same way `RacingClassControllerIT.setUp()` (lines 45–54) sets `adminToken`.

**Test structure template** (lines 31–145 adapted):
```java
class CarControllerIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String racerToken;
    private Long racerUserId;

    @BeforeEach
    void setUp() {
        String email = "racer-car-" + UUID.randomUUID() + "@test.com";
        // ... register and login, capture token + userId
    }

    @Test
    void createCar_returns201() { ... }

    @Test
    void archiveCar_notInActiveList() { ... }

    @Test
    void cannotAccessAnotherRacersCar_returns404() { ... }  // ownership check test

    private HttpHeaders racerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(racerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
```

---

### Frontend App.tsx modification

**Analog:** `frontend/src/App.tsx` (lines 14–48)

Replace the `'/racer/*'` wildcard route (lines 28–35) with a nested children layout:
```typescript
// REMOVE:
{
  path: '/racer/*',
  element: (
    <ProtectedRoute>
      <RacerPlaceholderPage />
    </ProtectedRoute>
  ),
},

// REPLACE WITH:
{
  path: '/racer',
  element: (
    <ProtectedRoute>
      <RacerPortalLayout />
    </ProtectedRoute>
  ),
  children: [
    { index: true, element: <Navigate to="/racer/profile" replace /> },
    { path: 'profile', element: <ProfilePage /> },
    { path: 'cars', element: <CarsPage /> },
    { path: 'transponders', element: <TranspondersPage /> },
    { path: 'entries', element: <EntriesPage /> },
  ],
},
// ADD alongside racer routes:
{ path: '/events', element: <EventSchedulePage /> },
```

---

### Frontend: `pages/racer/RacerPortalLayout.tsx`

**Analog:** `frontend/src/components/layout/AuthLayout.tsx` (layout shell pattern, lines 1–31)

AuthLayout establishes the Card-centered single-page shell. `RacerPortalLayout` is a full-viewport layout with `<Outlet>` instead of centered content. Key pattern from AuthLayout: using Tailwind `flex flex-col min-h-screen` and accepting `children` as content slot — here replaced by `<Outlet />`.

```typescript
import { Outlet, NavLink } from 'react-router-dom';
// Icons: import { User, Car, Radio, FileText } from 'lucide-react';

export default function RacerPortalLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      {/* Desktop top nav — hidden below md breakpoint */}
      <nav className="hidden md:flex items-center border-b px-6 h-14 gap-6">
        <span className="font-semibold mr-4">RC Timing</span>
        <NavLink to="/racer/profile"
          className={({ isActive }) => isActive ? 'text-primary font-medium' : 'text-muted-foreground hover:text-foreground'}>
          Profile
        </NavLink>
        {/* Cars, Transponders, Entries */}
      </nav>

      {/* Page content — pb-16 reserves space for mobile bottom nav */}
      <main className="flex-1 pb-16 md:pb-0 p-4 md:p-6">
        <Outlet />
      </main>

      {/* Mobile bottom nav — hidden at md+ */}
      <nav className="fixed bottom-0 inset-x-0 flex md:hidden border-t bg-background z-10">
        <NavLink to="/racer/profile" className="flex-1 flex flex-col items-center py-2 text-xs gap-1">
          {/* <User className="h-5 w-5" /> Profile */}
        </NavLink>
        {/* Cars, Transponders, Entries */}
      </nav>
    </div>
  );
}
```

---

### Frontend: `pages/racer/ProfilePage.tsx`, `CarsPage.tsx`, `TranspondersPage.tsx`

**Analog:** `frontend/src/pages/auth/RegisterPage.tsx`

The full form pattern (lines 1–182) applies to all racer portal forms. Key imports to copy:
```typescript
// Copy from RegisterPage.tsx lines 1–20
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { isAxiosError } from 'axios';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Form, FormField, FormItem, FormLabel, FormControl, FormMessage,
} from '@/components/ui/form';
import api from '@/lib/api';
```

Zod schema + `useForm` + `zodResolver` + `mode: 'onBlur'` (RegisterPage lines 22–51). Error handling: 400 maps field errors from `err.response.data.errors`, 409 sets a specific field error (lines 64–78).

**Portal pages add TanStack Query v5** on top of the form pattern:
```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

// Query pattern (v5 — use isPending not isLoading for new queries)
const { data: profile, isPending } = useQuery({
  queryKey: ['racer', 'profile'],
  queryFn: () => api.get('/api/v1/racer/profile').then(r => r.data),
});

// Mutation pattern with cache invalidation
const queryClient = useQueryClient();
const mutation = useMutation({
  mutationFn: (data: ProfileUpdateRequest) =>
    api.patch('/api/v1/racer/profile', data).then(r => r.data),
  onSuccess: () => {
    toast.success('Profile updated');
  },
  onSettled: () => {
    queryClient.invalidateQueries({ queryKey: ['racer', 'profile'] });
  },
});
```

---

### Frontend Hooks: `hooks/racer/useProfile.ts`, `useCars.ts`, etc.

**Analog:** `frontend/src/hooks/useAuth.ts` (lines 1–8)

`useAuth` is a thin context re-export. Racer hooks wrap TanStack Query instead of context:
```typescript
// frontend/src/hooks/racer/useCars.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

export function useCars() {
  return useQuery({
    queryKey: ['racer', 'cars'],
    queryFn: () => api.get<CarDto[]>('/api/v1/racer/cars').then(r => r.data),
  });
}

export function useArchiveCar() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (carId: number) => api.delete(`/api/v1/racer/cars/${carId}`),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['racer', 'cars'] });
    },
  });
}
```

Query key convention (consistent across all racer hooks):
```typescript
export const racerQueryKeys = {
  profile:      ['racer', 'profile'] as const,
  cars:         ['racer', 'cars'] as const,
  transponders: ['racer', 'transponders'] as const,
  entries:      ['racer', 'entries'] as const,
  eventSchedule:['events', 'schedule'] as const,
};
```

---

### Frontend Component: `components/racer/CarEditSheet.tsx`

**Analog:** `frontend/src/pages/auth/RegisterPage.tsx` (form body) + shadcn/ui Sheet docs

Sheet, SheetContent, SheetHeader, SheetTitle are NOT yet installed. Install first:
```bash
cd frontend && npx shadcn@latest add sheet select badge dialog separator
```

Form body inside Sheet reuses the exact same RHF + Zod pattern from RegisterPage:
```typescript
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';

interface CarEditSheetProps {
  car: CarDto | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CarEditSheet({ car, open, onOpenChange }: CarEditSheetProps) {
  const updateCar = useUpdateCar();

  const form = useForm<UpdateCarForm>({
    resolver: zodResolver(updateCarSchema),
    defaultValues: { name: car?.name ?? '', notes: car?.notes ?? '' },
  });

  async function onSubmit(values: UpdateCarForm) {
    await updateCar.mutateAsync({ id: car!.id, ...values });
    onOpenChange(false);
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right">
        <SheetHeader>
          <SheetTitle>Edit {car?.name}</SheetTitle>
        </SheetHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 mt-4">
            {/* FormField blocks — same pattern as RegisterPage lines 100–170 */}
            <Button type="submit" disabled={updateCar.isPending}>
              {updateCar.isPending ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Saving...</> : 'Save'}
            </Button>
          </form>
        </Form>
      </SheetContent>
    </Sheet>
  );
}
```

---

### Frontend Component: `components/racer/CarCard.tsx`, `TransponderCard.tsx`

**Analog:** `frontend/src/components/layout/AuthLayout.tsx` (Card import pattern, lines 1–8)

Card component imports:
```typescript
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from '@/components/ui/card';
```

---

### Frontend Component: `components/racer/EntrySubmitForm.tsx`

**Analog:** `frontend/src/pages/auth/RegisterPage.tsx` (multi-field form, lines 1–182)

Adds `Select` component (not yet installed — install via `npx shadcn@latest add select`). Select integration with React Hook Form:
```typescript
// From RESEARCH.md Code Examples (shadcn/ui Select + RHF pattern)
<FormField
  control={form.control}
  name="eventClassId"
  render={({ field }) => (
    <FormItem>
      <FormLabel>Racing Class</FormLabel>
      <Select onValueChange={field.onChange} defaultValue={field.value}>
        <FormControl>
          <SelectTrigger>
            <SelectValue placeholder="Select a class" />
          </SelectTrigger>
        </FormControl>
        <SelectContent>
          {classes.map(c => (
            <SelectItem key={c.id} value={String(c.id)}>{c.name}</SelectItem>
          ))}
        </SelectContent>
      </Select>
      <FormMessage />
    </FormItem>
  )}
/>
```

---

## Shared Patterns

### Authentication in Racer Controllers (Critical)

**Source:** `app/src/main/java/dev/monkeypatch/rctiming/security/JwtAuthenticationFilter.java` lines 41–44
**Apply to:** Every racer controller method that accesses user-owned data

The JWT filter sets `claims.getSubject()` (= userId string) as `Authentication.getName()`. Retrieve it with:
```java
// In any @RestController method parameter
public SomeDto method(Authentication auth, ...) {
    Long userId = Long.parseLong(auth.getName());
    // userId is now the authenticated racer's id
}
```

This is safe because `JwtAuthenticationFilter` only sets the authentication context for valid tokens. If the token is invalid or absent, `auth` will be null and `@PreAuthorize("hasRole('RACER')")` will reject the request before the method body runs.

---

### Ownership Check Pattern (All Racer Services)

**Source:** `app/src/main/java/dev/monkeypatch/rctiming/domain/raceclass/RacingClassService.java` lines 59–62 (getOrThrow helper pattern)
**Apply to:** All service methods in `domain/car/`, `domain/transponder/`, `domain/entry/`

```java
// Pattern: never use findById without ownership filter
private Car getCarOrThrow(Long carId, Long userId) {
    return carRepository.findById(carId)
            .filter(c -> c.getUserId().equals(userId))
            .orElseThrow(() -> new EntityNotFoundException("Car not found: " + carId));
}
```

Return 404 (not 403) when a resource is not found OR belongs to another user — never confirm the resource exists to a requester who does not own it.

---

### Error Handling (All New Controllers and Services)

**Source:** `app/src/main/java/dev/monkeypatch/rctiming/api/GlobalExceptionHandler.java` lines 1–63
**Apply to:** All new services — throw these exception types; `GlobalExceptionHandler` maps them

| Exception to throw | HTTP result | When to use |
|--------------------|-------------|-------------|
| `EntityNotFoundException` | 404 | Resource not found or not owned by user |
| `DataIntegrityViolationException` | 409 | Duplicate transponder number (propagated from DB UNIQUE constraint) |
| `ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY)` | 422 | Membership blocked at entry submission |
| `IllegalArgumentException` | 400 | Invalid state transition or business rule violation |

Do not add new `@ExceptionHandler` methods to `GlobalExceptionHandler` unless the exception type is entirely new.

---

### Transaction Annotation Convention (All Services)

**Source:** `app/src/main/java/dev/monkeypatch/rctiming/domain/raceclass/RacingClassService.java` lines 13–32
**Apply to:** All new `@Service` classes in `domain/` and `query/`

```java
@Service
@Transactional          // class-level default covers all write methods
public class CarService {

    @Transactional(readOnly = true)    // override on every read method
    public List<CarDto> findActiveByUser(Long userId) { ... }

    // No annotation override needed on write methods — class-level @Transactional applies
    public CarDto create(...) { ... }
}
```

jOOQ query services (`query/` package) must be `@Transactional(readOnly = true)` at the class level:
```java
@Service
@Transactional(readOnly = true)  // all jOOQ services are read-only
public class CarQueryService {
    private final DSLContext dsl;
    // ...
}
```

---

### DTO Record Convention (All New DTOs)

**Source:** `app/src/main/java/dev/monkeypatch/rctiming/api/admin/dto/RacingClassDto.java` (lines 1–13) and `CreateRacingClassRequest.java` (lines 1–9)
**Apply to:** All new `api/racer/dto/` and `api/admin/dto/` files

- Response DTOs: Java records with a static `from(Entity)` factory method
- Request DTOs: Java records with `@NotBlank`, `@Size`, `@NotNull` on fields that require validation
- Use `jakarta.validation.constraints.*` — never `javax.*`
- No Lombok — explicit getters generated by record

---

### TanStack Query v5 Key Rules (Frontend)

**Source:** `frontend/src/providers/QueryProvider.tsx` (QueryClient config) + RESEARCH.md State of the Art section
**Apply to:** All new frontend hooks and components

- Use `isPending` (NOT `isLoading`) for new queries — renamed in v5
- Use `onSettled` (not `onSuccess`) for cache invalidation to handle both success and error paths
- Always call `queryClient.invalidateQueries({ queryKey: [...] })` after mutations
- QueryClient is already configured with `retry: 1, refetchOnWindowFocus: false` in `QueryProvider.tsx`

---

### Toast Notification Convention (Frontend)

**Source:** `frontend/src/pages/auth/RegisterPage.tsx` lines 62–79
**Apply to:** All new page components and form components

```typescript
import { toast } from 'sonner';

// Success (5 seconds)
toast.success('Car saved.', { duration: 5000 });

// Error (8 seconds)
toast.error('Unable to reach server. Please try again.', { duration: 8000 });
```

`<Toaster />` is already wired in `App.tsx` (line 47). Do not add another instance.

---

## No Analog Found

Files with no close match in the codebase. The planner must use RESEARCH.md code examples as the primary reference.

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `query/car/CarQueryService.java` | service | CRUD | No jOOQ query services exist yet in the codebase — jOOQ is in `dependencies` but no generated DSL classes or query services have been written. Use RESEARCH.md Pattern 1 (Hibernate/jOOQ seam) and jOOQ 3.19 DSL documentation as reference. Key: must be `@Transactional(readOnly = true)`, must use `DSLContext` constructor injection, must use generated table/field constants from jOOQ codegen (not string SQL). |
| `query/event/EventScheduleQuery.java` | service | request-response | Same reason as above. This is the simplest jOOQ query (no joins) — good first jOOQ service to write as a reference. |
| `query/entry/EntryQueryService.java` | service | CRUD | Same reason. Requires multi-table join for entry history projection. |

**Wave 0 prerequisite for all jOOQ services:** Run Flyway migrations (V6–V13) then run jOOQ codegen to generate `CARS`, `TRANSPONDERS`, `EVENTS`, `ENTRIES`, `CAR_TAG_CATEGORIES`, `CAR_TAG_VALUES` table constants before writing any code in `query/`.

---

## Metadata

**Analog search scope:** `app/src/main/java/`, `app/src/test/java/`, `frontend/src/`
**Files scanned:** 71 (54 Java, 17 TypeScript/TSX)
**Pattern extraction date:** 2026-04-16
**Valid until:** 2026-05-16 (library versions verified in RESEARCH.md)
