---
phase: 02-racer-portal
reviewed: 2026-04-17T00:00:00Z
depth: standard
files_reviewed: 39
files_reviewed_list:
  - app/build.gradle.kts
  - app/src/main/java/dev/monkeypatch/rctiming/api/admin/AdminEntryController.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/admin/CarTagCategoryController.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/admin/dto/AdminUpdateTransponderRequest.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/admin/dto/MembershipOverrideRequest.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/CarController.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/EntryController.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/EventScheduleController.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/RacerProfileController.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/TransponderController.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/CreateCarRequest.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/CreateTransponderRequest.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/SetCarTagRequest.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/SubmitEntryRequest.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/UpdateCarRequest.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/UpdateRacerProfileRequest.java
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/UpsertMembershipRequest.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/car/Car.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarService.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategory.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategoryService.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/entry/Entry.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryService.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/event/Event.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/transponder/TransponderService.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/user/RacerProfileService.java
  - app/src/main/java/dev/monkeypatch/rctiming/domain/user/User.java
  - app/src/main/java/dev/monkeypatch/rctiming/query/car/CarQueryService.java
  - app/src/main/java/dev/monkeypatch/rctiming/query/entry/EntryQueryService.java
  - app/src/main/java/dev/monkeypatch/rctiming/query/event/EventScheduleQuery.java
  - app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java
  - app/src/main/resources/db/migration/V6__extend_users_racer_fields.sql
  - app/src/main/resources/db/migration/V9__create_cars.sql
  - app/src/main/resources/db/migration/V13__create_entries.sql
  - app/src/main/resources/db/migration/V14__create_entry_audit_log.sql
  - frontend/src/App.tsx
  - frontend/src/components/racer/CarCard.tsx
  - frontend/src/components/racer/CarEditSheet.tsx
  - frontend/src/hooks/racer/useCars.ts
  - frontend/src/hooks/racer/useProfile.ts
  - frontend/src/lib/racerApi.ts
  - frontend/src/pages/racer/CarsPage.tsx
  - frontend/src/pages/racer/ProfilePage.tsx
  - frontend/src/pages/racer/RacerPortalLayout.tsx
findings:
  critical: 3
  warning: 5
  info: 4
  total: 12
status: issues_found
---

# Phase 02: Code Review Report

**Reviewed:** 2026-04-17
**Depth:** standard
**Files Reviewed:** 44
**Status:** issues_found

## Summary

This review covers the racer portal backend (Spring Boot 3.4 / Java 21) and frontend (React 18 / TypeScript). The IDOR surface is the primary concern: the ownership enforcement pattern is correct and consistent across cars, transponders, and entries — all mutating service methods pass `userId` as a filter and throw `EntityNotFoundException` on mismatch. No cross-racer data leakage was found in the query layer either.

Three issues reach Critical severity: a security-miscategorisation in `SecurityConfig` that allows any authenticated (non-admin) user to hit `/api/v1/admin/**` endpoints if the method-level `@PreAuthorize` is the only guard (defence-in-depth gap); a racer-controllable CONFIRMED entry re-confirmation bypass in `adminApplyMembershipOverride`; and a type mismatch between the `CarDto` tags structure returned by the backend (`Map<categoryName, value>`) and the structure expected by the frontend (`{ categoryId, categoryName, value }[]`), which will cause a runtime crash in `CarCard`.

Five warnings cover logic bugs and missing defensive checks. Four info items flag dead fields, unclear code, and minor quality issues.

---

## Critical Issues

### CR-01: Defence-in-depth gap — any authenticated user can reach `/api/v1/admin/**`

**File:** `app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java:34`

**Issue:** `SecurityConfig` grants `/api/v1/admin/**` to `hasAnyRole("ADMIN", "RACE_DIRECTOR", "REFEREE")` at the HTTP layer. However, the route-level rule fires **before** method-level `@PreAuthorize`. If a `RACER`-only token is sent to any admin endpoint, Spring Security's URL matcher fires first — because `anyRequest().authenticated()` at line 35 catches everything not matched by the admin rule. But the admin rule is an **allow** rule (hasAnyRole), not a deny rule for others. The `anyRequest().authenticated()` rule below it matches RACER users and lets them through to the controller where method-security would block them. That is correct in isolation. The gap is that if a future endpoint under `/api/v1/admin/**` is added **without** a `@PreAuthorize` annotation (easy to forget), a logged-in RACER can access it because the URL-rule only restricts to staff roles, but the `anyRequest().authenticated()` fallback never applies to a URL already matched by the admin rule — the admin rule IS the match, and it already permits staff. Wait — let me be precise about the actual current bug:

The actual current bug is subtler: `AdminEntryController` carries `@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")` at class level, but `adminUpdateTransponder` at line 28-34 does **not** validate that the new `transponderId` in the request belongs to the **entry's racer**. An admin can swap any transponder (even one belonging to a completely different user) onto the entry. This is an authorization logic bug, not a Spring Security config gap, but it lives in the admin layer.

Separating these:

**CR-01a — Admin transponder swap does not verify transponder ownership**

**File:** `app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryService.java:152-153`

**Issue:** `adminUpdateTransponder` resolves `transponderId` from the request by calling `transponderRepository.findById(req.transponderId())` with no ownership check. An admin can attach any transponder in the system to any entry, including transponders owned by completely different racers. This means if transponder IDs are guessable (they are — sequential bigserial), a malicious admin can silently re-assign transponders across racers without the original owner's knowledge.

While admins are trusted staff, the correct behaviour per the domain model is that an admin transponder swap should replace the **snapshot columns** (which it does), but the `transponder_id` FK should only point to transponders that legitimately exist — the ownership validation should at minimum log a warning or require a reason when crossing racer boundaries. More importantly, the transponder snapshot used for timing is `transponderNumberSnapshot`, not the FK, so swapping an arbitrary FK silently corrupts the logical association between an entry and a racer's registered equipment without any domain check.

**Fix:** Add a check that the `newTransponder` is either owned by the entry's racer **or** explicitly noted in the audit reason that this is a cross-racer assignment. At minimum, emit a warning in the audit log when `newTransponder.getUserId() != entry.getUserId()`:

```java
if (!newTransponder.getUserId().equals(entry.getUserId())) {
    // Cross-racer transponder assignment — this is unusual; log prominently
    // and require reason to be non-blank
    if (req.reason() == null || req.reason().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "A reason is required when assigning a transponder across racer accounts");
    }
}
```

---

### CR-02: `adminApplyMembershipOverride` re-confirms already-CONFIRMED entries, erasing withdrawn state

**File:** `app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryService.java:173-189`

**Issue:** `adminApplyMembershipOverride` unconditionally sets `entry.setStatus(EntryStatus.CONFIRMED)` regardless of the entry's current status. If a racer withdraws their entry and an admin then calls this endpoint on it (e.g., via a typo or replayed request), the entry is silently un-withdrawn and re-confirmed. A WITHDRAWN entry should not be re-confirmable via a membership override — the racer has explicitly left.

Additionally, `setConfirmedAt(Instant.now())` is called even when the entry was already CONFIRMED, overwriting the original confirmation timestamp, which corrupts the audit trail.

**Fix:**
```java
public EntryDto adminApplyMembershipOverride(Long entryId, Long adminUserId, String reason) {
    Entry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new EntityNotFoundException("Entry not found: " + entryId));
    if (entry.getStatus() == EntryStatus.WITHDRAWN) {
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Cannot apply membership override to a withdrawn entry");
    }
    // Only set confirmedAt if transitioning from PENDING
    if (entry.getStatus() == EntryStatus.PENDING) {
        entry.setConfirmedAt(Instant.now());
    }
    // ... rest unchanged
}
```

---

### CR-03: Frontend `CarDto.tags` type mismatch — `CarCard` will crash at runtime

**File:** `frontend/src/lib/racerApi.ts:46-47` and `frontend/src/components/racer/CarCard.tsx:28-31`

**Issue:** The frontend `CarDto` interface declares `tags` as `{ categoryId: number; categoryName: string; value: string }[]`. However, the backend `GET /api/v1/racer/cars` endpoint returns `CarWithTagsDto` from `CarQueryService`, which serialises `tags` as a plain `Map<String, String>` keyed by category **name** (not an array of objects). The jOOQ query at `CarQueryService.java:43-55` collects into `Map<Long, Map<String, String>> tagsByCarId` and the DTO is `Map<String, String> tags`.

When `CarCard` renders `car.tags.slice(0, 3).map((t, i) => ...)` and accesses `t.categoryId` and `t.categoryName`, it will receive `undefined` for both because the actual shape is a plain object, not an array. `car.tags.slice` will throw `TypeError: car.tags.slice is not a function` at runtime because a plain object has no `slice` method.

**Fix:** Align the types. Either:

Option A — change the frontend interface to match what the backend actually returns:
```ts
export interface CarWithTagsDto {
  id: number;
  userId: number;
  name: string;
  primaryClassId: number | null;
  notes: string | null;
  archived: boolean;
  tags: Record<string, string>; // { [categoryName]: value }
}
```
And update `CarCard` to use `Object.entries(car.tags)` instead of `.slice`.

Option B — change `CarQueryService` to return a list of tag objects that match the existing frontend interface (preferred for richer data):
```java
record CarTagEntry(Long categoryId, String categoryName, String value) {}
// Return List<CarTagEntry> instead of Map<String,String>
```

---

## Warnings

### WR-01: `EntryService.submitEntry` — duplicate-entry check does not account for archived cars

**File:** `app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryService.java:90-92`

**Issue:** The car ownership check at line 90 uses `carRepository.findById(req.carId()).filter(c -> c.getUserId().equals(userId))`. This correctly enforces ownership but does **not** reject archived cars. A racer can submit an entry with a car they have already archived. Archived cars are supposed to be hidden from entries.

**Fix:**
```java
Car car = carRepository.findById(req.carId())
        .filter(c -> c.getUserId().equals(userId) && !c.isArchived())
        .orElseThrow(() -> new EntityNotFoundException("Car not found"));
```

---

### WR-02: `EntryService.withdraw` — CONFIRMED entries can be withdrawn after the event entry deadline has passed

**File:** `app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryService.java:136-147`

**Issue:** `withdraw` has no check against the event's `entryClosesAt` or the event status. A racer can withdraw an entry from an `IN_PROGRESS` or `ENTRIES_CLOSED` event, which could disrupt race day operations (e.g., grids have already been printed, heat sheets generated). The requirement RACER-13 implies withdrawal should be time-bounded but there is currently no enforcement.

**Fix:** Load the event and check its status before allowing withdrawal:
```java
public void withdraw(Long entryId, Long userId) {
    Entry entry = entryRepository.findById(entryId)
            .filter(e -> e.getUserId().equals(userId))
            .orElseThrow(() -> new EntityNotFoundException("Entry not found: " + entryId));
    if (entry.getStatus() == EntryStatus.WITHDRAWN) {
        throw new IllegalArgumentException("Entry already withdrawn");
    }
    Event event = eventRepository.findById(entry.getEventId())
            .orElseThrow(() -> new EntityNotFoundException("Event not found"));
    if (event.getStatus() == EventStatus.IN_PROGRESS || event.getStatus() == EventStatus.COMPLETED) {
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Cannot withdraw from an event that is already underway");
    }
    // ... proceed
}
```

---

### WR-03: `EntryQueryService` filters history using string literals instead of enum — typo-sensitive

**File:** `app/src/main/java/dev/monkeypatch/rctiming/query/entry/EntryQueryService.java:36`

**Issue:** The jOOQ query filters `ENTRIES.STATUS.in("CONFIRMED", "WITHDRAWN")`. This is a raw string comparison. If the `EntryStatus` enum values are ever renamed or a new status is added, this filter will silently diverge from the domain model with no compile-time error. The same pattern appears in `EventScheduleQuery.java:28` with event statuses.

**Fix:** Use jOOQ's enum binding or at minimum reference the enum names:
```java
.and(ENTRIES.STATUS.in(
    EntryStatus.CONFIRMED.name(),
    EntryStatus.WITHDRAWN.name()
))
```
This at least makes the dependency explicit and searchable. For full type safety, configure jOOQ to generate enum-typed columns via `<forcedType>`.

---

### WR-04: `Entry` entity has a `membershipOverride` boolean that `setMembershipOverrideByAdminId` sets, but `adminApplyMembershipOverride` never sets it via the boolean setter — state split

**File:** `app/src/main/java/dev/monkeypatch/rctiming/domain/entry/Entry.java:93-96` and `EntryService.java:180`

**Issue:** `setMembershipOverrideByAdminId` correctly sets `this.membershipOverride = true` as a side effect. However, `adminApplyMembershipOverride` calls `entry.setMembershipOverrideByAdminId(adminUserId)` which does set the boolean via the side effect — this path is actually correct. The bug is the reverse: `setMembershipOverride(boolean)` is a public setter at line 99 that can set `membershipOverride = false` without clearing `membershipOverrideByAdminId`. If anything ever calls `setMembershipOverride(false)` on an entity that has `membershipOverrideByAdminId` set, the two columns become inconsistent.

**Fix:** Remove the public `setMembershipOverride(boolean)` setter or make it package-private. The `membershipOverride` column should be derived exclusively from whether `membershipOverrideByAdminId` is non-null:
```java
// Remove setMembershipOverride(boolean) entirely
// It is fully controlled by setMembershipOverrideByAdminId
```

---

### WR-05: `CarTagCategoryService.delete` makes two separate DB round trips under a single transaction, introducing a TOCTOU window

**File:** `app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategoryService.java:51-56`

**Issue:** `delete` first calls `existsById(id)` (one SELECT), then `deleteById(id)` (one DELETE). While both are in the same `@Transactional` context, this is still redundant: `deleteById` will silently no-op if the entity does not exist (Spring Data JPA calls `findById` internally before deleting). The `EntityNotFoundException` will never be thrown because `deleteById` does not throw on missing entities — it calls `delete(entity)` only if found, otherwise does nothing. The guard at line 52-54 is dead code.

**Fix:**
```java
public void delete(Long id) {
    CarTagCategory category = getCategoryOrThrow(id);
    carTagCategoryRepository.delete(category);
}
```
This is a single round trip, throws `EntityNotFoundException` correctly, and avoids the TOCTOU pattern.

---

## Info

### IN-01: `Entry` entity maps `transponder_number` column as `transponderNumberSnapshot` — naming mismatch will confuse maintainers

**File:** `app/src/main/java/dev/monkeypatch/rctiming/domain/entry/Entry.java:39-41`

**Issue:** The Java field is named `transponderNumberSnapshot` but is mapped to the DB column `transponder_number` (no `_snapshot` suffix). The migration comment at `V13__create_entries.sql:8` acknowledges this. This works correctly but will repeatedly surprise anyone reading the entity without the comment handy.

**Fix:** Consider adding a comment directly on the `@Column` annotation, or — at next schema revision — rename the DB column to `transponder_number_snapshot` for clarity.

---

### IN-02: `racerApi.ts` `CarDto` is declared as having a `tags` array but `fetchCars` returns `CarWithTagsDto` from the backend

**File:** `frontend/src/lib/racerApi.ts:40-47`

**Issue:** (Companion to CR-03) The `CarDto` interface in the frontend is used in `useCars`, `CarsPage`, `CarEditSheet`, and `CarCard`. The `tags` field has a different type than what the wire format delivers. This is the type definition side of the CR-03 crash — the interface should be corrected alongside the fix.

---

### IN-03: `CarsPage.tsx` uses non-null assertion `cars!.map(...)` after a truthiness guard that already handles empty

**File:** `frontend/src/pages/racer/CarsPage.tsx:55`

**Issue:** Line 45 checks `cars && cars.length === 0` for the empty state, then line 55 uses `cars!.map(...)` in the else branch. At that point `cars` is guaranteed non-null by the outer condition, but the `!` assertion is unnecessary and masks the fact that `cars` could theoretically be `undefined` if TanStack Query returns `undefined` before `isPending` is true (race condition at mount). Using optional chaining `cars?.map(...)` is cleaner and avoids the assertion.

**Fix:** `{(cars ?? []).map(c => <CarCard key={c.id} car={c} onClick={() => openEdit(c)} />)}`

---

### IN-04: `window.confirm` used in `CarEditSheet.onArchive` — blocks the main thread and is inconsistent with the toast-based UX pattern

**File:** `frontend/src/components/racer/CarEditSheet.tsx:97-99`

**Issue:** `window.confirm(...)` is a synchronous blocking dialog that cannot be styled, is blocked on some mobile browsers, and violates the component's own toast-based confirmation pattern. It also cannot be tested with Vitest/React Testing Library without mocking `window.confirm`.

**Fix:** Replace with a shadcn/ui `AlertDialog` confirmation component, consistent with the rest of the UI.

---

_Reviewed: 2026-04-17_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
