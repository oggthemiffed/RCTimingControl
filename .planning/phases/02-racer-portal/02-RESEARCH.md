# Phase 2: Racer Portal - Research

**Researched:** 2026-04-16
**Domain:** Spring Boot CRUD backend + React 19 self-service portal with mobile-first navigation
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Racer Entity Design**
- D-01: Extend the existing `User` entity — do NOT create a separate `RacerProfile`. Every racer has a login; no separate profile entity is needed for v1. Staff-only users (no racer data) are fine with nullable racer fields.
- D-02: Contact fields added to `User`: `phoneNumber` (nullable), `emergencyContactName` (nullable), `emergencyContactPhone` (nullable). `firstName` + `lastName` already exist.
- D-03: Ability ratings stored in a separate `user_class_ratings` table (userId, classId, rating 0–100). Displayed on the racer's portal profile page (read-only). Admin-editable via API. Auto-update from championship points is Phase 7.
- D-04: Governing body memberships stored in `user_governing_body_memberships` table (userId, governingBodyCode, membershipNumber). Already modelled in Phase 1 design; Phase 2 wires it to the racer profile form.

**Portal Navigation**
- D-05: Routed pages: `/racer/profile`, `/racer/cars`, `/racer/transponders`, `/racer/entries`. All routes require login (RACER role or any authenticated user).
- D-06: Responsive nav: horizontal top nav on desktop, bottom nav bar on mobile (thumb-friendly). Mobile-first design. Tailwind responsive breakpoints handle the switch.
- D-07: Cars section uses list + inline edit (card expansion or slide-over sheet using shadcn/ui Sheet component). No separate `/racer/cars/:id` route. Tag values are edited inline on the car card.

**Event Entry Scope**
- D-08: Full-stack in Phase 2: backend APIs + complete racer-facing UI. Integration tests use Flyway-seeded test event data (one OPEN event with classes and a DRAFT event).
- D-09: Public event schedule (`/events`, no login required): event name, date, entry status (OPEN / CLOSED / UPCOMING), and a link to enter. No class list, no entry counts.
- D-10: Entry auto-confirms on submission. PENDING is used only as an internal transient state during the submission transaction — racer sees "Confirmed" immediately. State transition logic in a service method (not hard-coded in controller) to allow payment-gated PENDING in v2.

**Admin Requirements in Phase 2 (Backend Only)**
- D-11: RACER-10 (car tag categories): Seed 7 default categories via Flyway (`Chassis`, `ESC`, `Motor`, `Servo`, `Battery`, `Body`, `Tyres`). Expose admin REST API for CRUD. No admin UI.
- D-12: RACER-08 (transponder reassignment): Backend endpoint only. UI lives in Phase 4 race control client.
- D-13: RACER-14 (admin override for membership-required entry): `membershipOverride` boolean flag on entry + admin API endpoint with audit logging. No admin UI in Phase 2.

### Claude's Discretion
- Transponders section layout (card/list pattern, consistent with cars)
- Entries page layout (table or card list — whichever suits the data better)
- Exact Flyway seeding structure for test events and classes
- REST URL structure (following the `/api/v1/` convention established in Phase 1)

### Deferred Ideas (OUT OF SCOPE)
- Payment integration (PayPal/Stripe): Entry stays PENDING until payment confirmed — v2 addition.
- Login-less racer imports: Future feature, requires nullable `passwordHash` on User. Deferred post-v1.
- Public entry list visibility: Out of scope (privacy decision).
- Admin car tag category UI: Phase 3 admin panel.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| RACER-01 | Racer can create and edit their profile (name, contact details) | `User` entity extension with Flyway V6 migration; `PATCH /api/v1/racer/profile` |
| RACER-02 | Racer can add and edit cars (name, primary class, notes); primary class not a hard constraint | `Car` entity with `archived` flag; jOOQ read projection for list view |
| RACER-03 | Racer owns transponders independently of cars; transponder selected at entry time | `Transponder` entity owned by User; no Car FK |
| RACER-04 | Racer can view their own entry history and past race results | jOOQ projection; `/racer/entries` page with history table |
| RACER-05 | Transponder numbers unique system-wide; duplicate registration rejected | `UNIQUE` constraint on `transponder_number`; 409 response on conflict |
| RACER-06 | Cars archived not deleted | `archived BOOLEAN NOT NULL DEFAULT FALSE`; filter active cars in queries |
| RACER-07 | Entry records transponder snapshot at submission time | `transponder_number` + `transponder_label` copied to `entry` row at insert |
| RACER-08 | Race director can update transponder assignment on entry (backend only in Phase 2) | Admin endpoint `PATCH /api/v1/admin/entries/{id}/transponder` with audit log |
| RACER-09 | System warns when same transponder used in multiple entries at same event | Service-layer check before confirmation; warning not hard block |
| RACER-10 | Admin can define/manage car tag categories (7 default, seeded by Flyway) | Flyway V7 seeds default categories; admin CRUD endpoints |
| RACER-11 | Racer can add free-text values for any tag category to each car | `car_tag_values` join table (carId, categoryId, value) |
| RACER-12 | Ability rating (0–100) per racing class, displayed read-only on portal | `user_class_ratings` table; jOOQ read; no write from racer |
| RACER-13 | Governing body membership numbers on profile; admin can edit | `user_governing_body_memberships` table; racer and admin write paths |
| RACER-14 | Membership-required entry blocking; admin override with audit log | Service checks `GoverningBodyAffiliation.membershipRequired`; override flag + audit |
| EVENT-03 | Racer can enter event online, selecting class, car, transponder | `Entry` entity; `POST /api/v1/racer/events/{id}/entries` |
| EVENT-04 | Public event schedule visible without login | `/api/v1/events` permits all; minimal projection via jOOQ |
| ENTRY-01 | Entry lifecycle (PENDING → CONFIRMED → WITHDRAWN); racer can withdraw before close | `EntryStatus` enum; `POST /api/v1/racer/entries/{id}/withdraw` |
</phase_requirements>

---

## Summary

Phase 2 extends the Phase 1 foundation in two dimensions: backend entity domain (User extension, Car, Transponder, Event, Entry, CarTagCategory, CarTagValue, UserClassRating, UserGoverningBodyMembership) and a full React racer portal frontend. The codebase is already structured correctly — Hibernate write side and jOOQ read side are cleanly separated, integration test harness is in place, and the React scaffold (React 19, TanStack Query v5, React Hook Form v7, Zod, shadcn/ui) is installed and working.

The main technical challenges are: (1) correct Hibernate-to-jOOQ seam discipline for the new entities, especially car tag queries and entry history projections; (2) the transponder uniqueness rule across the full system (not per-racer); (3) the governing body membership check at entry submission, which must query `GoverningBodyAffiliation.membershipRequired` and join to `user_governing_body_memberships`; and (4) the mobile-first dual navigation (top nav on desktop / bottom nav bar on mobile) using Tailwind responsive breakpoints.

The shadcn/ui Sheet component is not yet installed in the frontend — it must be added via the shadcn CLI before the cars inline-edit pattern can use it. Select, Dialog, and Badge components will also be needed for the entry submission form. The `RACER` role is already defined in the `Role` enum and assigned on registration, so the auth layer needs no changes.

**Primary recommendation:** Eight Flyway migrations (V6–V13), one domain package per aggregate (car, transponder, event, entry), matching admin/racer/public REST controllers, and a React portal with a layout shell + four routed pages.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Profile edit (name, contact, memberships) | API / Backend | Browser / Client | Write via Hibernate; profile read via jOOQ projection |
| Car CRUD + tag values | API / Backend | Browser / Client | Hibernate write; jOOQ read for list+tags projection |
| Transponder registration (uniqueness) | API / Backend | — | Uniqueness constraint enforced at DB; duplicate check in service |
| Event entry submission + membership check | API / Backend | — | Business rule enforcement in service layer; not in frontend |
| Entry auto-confirm state transition | API / Backend | — | Service method; controller delegates entirely |
| Transponder duplicate warning (RACER-09) | API / Backend | Browser / Client | Backend detects, returns warning field; frontend surfaces it |
| Public event schedule | API / Backend | Browser / Client | `permitAll` endpoint; jOOQ minimal projection |
| Entry history view | API / Backend (read) | Browser / Client | jOOQ projection; no Hibernate lazy-load |
| Ability rating display | API / Backend (read) | Browser / Client | Read-only jOOQ query; no racer write path |
| Portal navigation layout | Browser / Client | — | Tailwind responsive breakpoints; React Router Outlet |
| Car inline-edit sheet | Browser / Client | — | shadcn/ui Sheet; TanStack Query mutation + invalidate |
| Admin transponder swap (RACER-08) | API / Backend | — | Backend endpoint only in Phase 2; no UI |
| Admin membership override (RACER-14) | API / Backend | — | Backend endpoint + audit log; no UI |

---

## Standard Stack

### Core (verified installed)

| Library | Version | Purpose | Source |
|---------|---------|---------|--------|
| Spring Boot | 3.4.x (BOM) | Web, JPA, Security, Validation, Mail | [VERIFIED: app/build.gradle.kts] |
| Java | 21 LTS | Runtime | [VERIFIED: toolchain in build.gradle.kts] |
| Hibernate 6 | via Spring Boot BOM | Write-side ORM — new entities only | [VERIFIED: spring-boot-starter-data-jpa] |
| jOOQ 3.19 | via Spring Boot BOM | Read-side projections (cars, entries, schedule, history) | [VERIFIED: app/build.gradle.kts] |
| Flyway | via Spring Boot BOM | Schema migrations V6–V13 | [VERIFIED: flyway-core in build.gradle.kts] |
| Hypersistence Utils 3.9.11 | 3.9.11 | JSONB support (already used in EventClass) | [VERIFIED: app/build.gradle.kts] |
| JJWT | 0.12.6 | JWT — no changes needed, auth already works | [VERIFIED: app/build.gradle.kts] |
| Spring Security | via Boot BOM | SecurityConfig — needs `/api/v1/events` permitAll + racer routes | [VERIFIED: SecurityConfig.java] |
| React | 19.2.5 | UI framework | [VERIFIED: node_modules] |
| TanStack Query | 5.99.0 | Server state, cache invalidation after mutations | [VERIFIED: node_modules] |
| React Hook Form | 7.72.1 | Form state, validation | [VERIFIED: node_modules] |
| @hookform/resolvers | 5.2.2 | Zod adapter for RHF | [VERIFIED: node_modules] |
| Zod | 3.25.76 | Schema validation for all forms | [VERIFIED: node_modules] |
| React Router DOM | 7.14.1 | Nested layout with Outlet for portal pages | [VERIFIED: node_modules] |
| shadcn/ui (radix-ui) | 1.4.3 | UI components: Card, Form, Input, Button, Sheet, Select, Badge | [VERIFIED: node_modules] |
| Tailwind CSS | 4.2.2 | Responsive breakpoints for top/bottom nav switch | [VERIFIED: package.json devDependencies] |
| Sonner | 2.0.7 | Toast notifications for form submit feedback | [VERIFIED: node_modules] |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Lucide React | 1.8.0 | Icon set already used in auth screens | All icon needs |
| Testcontainers | via Boot BOM | Integration test Postgres | All Spring IT classes extend AbstractIntegrationTest |
| AssertJ | via Boot BOM | Fluent assertions in tests | All test assertions |

### shadcn/ui Components Not Yet Installed

The following components are needed in Phase 2 but are not yet present in `frontend/src/components/ui/`:

| Component | Purpose | Install Command |
|-----------|---------|-----------------|
| Sheet | Car inline-edit slide-over (D-07) | `npx shadcn@latest add sheet` |
| Select | Class/car/transponder dropdowns on entry form | `npx shadcn@latest add select` |
| Badge | Entry status chips (CONFIRMED / WITHDRAWN) | `npx shadcn@latest add badge` |
| Dialog | Confirm-withdraw dialog | `npx shadcn@latest add dialog` |
| Separator | Visual dividers in portal layout | `npx shadcn@latest add separator` |

**Installation (Wave 0 task):**
```bash
cd frontend
npx shadcn@latest add sheet select badge dialog separator
```

[VERIFIED: ls frontend/src/components/ui/ — confirmed these files absent]

---

## Architecture Patterns

### System Architecture Diagram

```
Browser (React 19)
  │
  ├── /events  (no auth)                 → GET /api/v1/events
  │
  ├── RacerPortalLayout (Outlet)
  │     ├── Top nav (md+) / Bottom nav (< md)
  │     │
  │     ├── /racer/profile               → GET/PATCH /api/v1/racer/profile
  │     │                                   GET/POST/DELETE /api/v1/racer/memberships
  │     │
  │     ├── /racer/cars                  → GET/POST /api/v1/racer/cars
  │     │     └── [Sheet open]           → PATCH /api/v1/racer/cars/{id}
  │     │                                   POST /api/v1/racer/cars/{id}/tags
  │     │
  │     ├── /racer/transponders          → GET/POST /api/v1/racer/transponders
  │     │                                   DELETE /api/v1/racer/transponders/{id}
  │     │
  │     └── /racer/entries               → GET /api/v1/racer/entries  (history)
  │           └── [Entry form]           → POST /api/v1/racer/events/{id}/entries
  │                                         POST /api/v1/racer/entries/{id}/withdraw
  │
Spring Boot (API)
  │
  ├── RacerProfileController             → UserService (Hibernate write)
  ├── CarController                      → CarService (Hibernate write)
  │                                         CarQueryService (jOOQ read)
  ├── TransponderController              → TransponderService (Hibernate write)
  ├── EntryController                    → EntryService (Hibernate write + rules)
  │                                         EntryQueryService (jOOQ read)
  ├── EventScheduleController            → EventScheduleQuery (jOOQ read, permitAll)
  ├── AdminCarTagCategoryController      → CarTagCategoryService (Hibernate write)
  └── AdminEntryController               → EntryService (admin methods)
  │
PostgreSQL 16 (Flyway V1–V13)
  ├── users (extended with racer fields)
  ├── user_governing_body_memberships
  ├── user_class_ratings
  ├── cars
  ├── car_tag_categories
  ├── car_tag_values
  ├── transponders
  ├── events
  ├── event_classes (Phase 1)
  ├── entries
  └── entry_audit_log
```

### Recommended Project Structure (new packages)

```
app/src/main/java/dev/monkeypatch/rctiming/
├── domain/
│   ├── car/              # Car, CarTagCategory, CarTagValue + repositories + CarService
│   ├── transponder/      # Transponder + repository + TransponderService
│   ├── event/            # Event + repository + EventService (CRUD, state)
│   └── entry/            # Entry, EntryAuditLog + repositories + EntryService
├── query/
│   ├── car/              # CarQueryService (jOOQ car+tags projection)
│   ├── event/            # EventScheduleQuery (public schedule projection)
│   └── entry/            # EntryQueryService (history projection)
└── api/
    ├── racer/            # RacerProfileController, CarController, TransponderController,
    │                     #   EntryController, EventScheduleController
    │   └── dto/          # Request/response records
    └── admin/
        └── (existing)    # CarTagCategoryController, AdminEntryController added here

frontend/src/
├── pages/racer/
│   ├── RacerPortalLayout.tsx   # Outlet + top/bottom nav switch
│   ├── ProfilePage.tsx
│   ├── CarsPage.tsx
│   ├── TranspondersPage.tsx
│   └── EntriesPage.tsx
├── pages/events/
│   └── EventSchedulePage.tsx   # Public, no auth
├── hooks/racer/
│   ├── useProfile.ts
│   ├── useCars.ts
│   ├── useTransponders.ts
│   └── useEntries.ts
└── components/racer/
    ├── CarCard.tsx
    ├── CarEditSheet.tsx
    ├── TransponderCard.tsx
    └── EntrySubmitForm.tsx
```

### Pattern 1: Hibernate Write / jOOQ Read Seam

All new domain objects follow this seam. Hibernate entities and Spring Data repositories live in `domain/`. jOOQ read queries live in `query/` and never instantiate Hibernate-managed objects.

```java
// domain/car/CarService.java — Hibernate write side
@Service
@Transactional
public class CarService {
    private final CarRepository carRepository;

    public Car createCar(Long userId, String name, Long primaryClassId, String notes) {
        Car car = new Car();
        car.setUserId(userId);
        car.setName(name);
        car.setPrimaryClassId(primaryClassId);
        car.setNotes(notes);
        car.setArchived(false);
        car.setCreatedAt(Instant.now());
        car.setUpdatedAt(Instant.now());
        return carRepository.save(car);
    }

    public void archiveCar(Long carId, Long userId) {
        Car car = carRepository.findByIdAndUserId(carId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        car.setArchived(true);
        car.setUpdatedAt(Instant.now());
        // no explicit save — @Transactional dirty tracking handles it
    }
}
```

```java
// query/car/CarQueryService.java — jOOQ read side
// Source: jOOQ 3.19 DSL pattern (ASSUMED — jOOQ codegen not yet run for Phase 2 tables)
@Service
@Transactional(readOnly = true)
public class CarQueryService {
    private final DSLContext dsl;

    public List<CarWithTagsDto> getCarsForUser(Long userId) {
        // Fetch cars with tag values via jOOQ join — NOT Hibernate lazy-loading
        return dsl.select(CARS.ID, CARS.NAME, CARS.NOTES,
                          CAR_TAG_CATEGORIES.NAME.as("category"),
                          CAR_TAG_VALUES.VALUE)
                  .from(CARS)
                  .leftJoin(CAR_TAG_VALUES).on(CAR_TAG_VALUES.CAR_ID.eq(CARS.ID))
                  .leftJoin(CAR_TAG_CATEGORIES).on(CAR_TAG_CATEGORIES.ID.eq(CAR_TAG_VALUES.CATEGORY_ID))
                  .where(CARS.USER_ID.eq(userId).and(CARS.ARCHIVED.isFalse()))
                  .fetchGroups(CARS.ID, r -> /* map to DTO */);
    }
}
```

[VERIFIED: Pattern matches EventClass / RaceFormatService in Phase 1]

### Pattern 2: TanStack Query v5 Mutation with Cache Invalidation

All racer portal mutations follow this pattern — `useMutation` + `invalidateQueries` on `onSettled`.

```tsx
// Source: https://github.com/tanstack/query/blob/main/docs/framework/react/guides/optimistic-updates.md
// [VERIFIED via Context7 docs fetch]
function usePatchProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: ProfileUpdateRequest) =>
      api.patch('/api/v1/racer/profile', data).then(r => r.data),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['racer', 'profile'] });
    },
  });
}
```

### Pattern 3: React Router v7 Nested Layout (Portal Shell)

Replace the `RacerPlaceholderPage.tsx` wildcard with a proper nested layout using `Outlet`.

```tsx
// frontend/src/pages/racer/RacerPortalLayout.tsx
// Source: https://github.com/remix-run/react-router/blob/main/docs/tutorials/address-book.md
// [VERIFIED via Context7 docs fetch]
import { Outlet, NavLink } from 'react-router-dom';

export default function RacerPortalLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      {/* Desktop top nav — hidden on mobile */}
      <nav className="hidden md:flex border-b px-4 gap-6">
        <NavLink to="/racer/profile">Profile</NavLink>
        <NavLink to="/racer/cars">Cars</NavLink>
        <NavLink to="/racer/transponders">Transponders</NavLink>
        <NavLink to="/racer/entries">Entries</NavLink>
      </nav>

      {/* Page content */}
      <main className="flex-1 pb-16 md:pb-0">
        <Outlet />
      </main>

      {/* Mobile bottom nav — hidden on desktop */}
      <nav className="fixed bottom-0 inset-x-0 flex md:hidden border-t bg-background">
        <NavLink to="/racer/profile" className="flex-1 flex flex-col items-center py-2">
          {/* icon + label */}
        </NavLink>
        {/* ... other tabs */}
      </nav>
    </div>
  );
}
```

App.tsx router config update:
```tsx
{
  path: '/racer',
  element: <ProtectedRoute><RacerPortalLayout /></ProtectedRoute>,
  children: [
    { index: true, element: <Navigate to="/racer/profile" replace /> },
    { path: 'profile', element: <ProfilePage /> },
    { path: 'cars', element: <CarsPage /> },
    { path: 'transponders', element: <TranspondersPage /> },
    { path: 'entries', element: <EntriesPage /> },
  ],
},
{ path: '/events', element: <EventSchedulePage /> },
```

### Pattern 4: shadcn/ui Sheet for Car Inline Edit

```tsx
// Source: https://github.com/shadcn-ui/ui/blob/main/apps/v4/content/docs/changelog/2023-06-new-cli.mdx
// [VERIFIED via Context7 docs fetch]
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';

function CarEditSheet({ car, open, onOpenChange }: Props) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right">
        <SheetHeader>
          <SheetTitle>Edit {car.name}</SheetTitle>
        </SheetHeader>
        <CarEditForm car={car} onSuccess={() => onOpenChange(false)} />
      </SheetContent>
    </Sheet>
  );
}
```

### Pattern 5: Entry Submission Service (state transition in service, not controller)

Per D-10, PENDING is a transient state in the same transaction. The service method owns the transition.

```java
// domain/entry/EntryService.java
@Service
@Transactional
public class EntryService {

    public Entry submitEntry(Long userId, Long eventId, SubmitEntryRequest req) {
        // 1. Validate event is OPEN
        // 2. Check membership requirement (RACER-14)
        // 3. Check transponder conflict warning (RACER-09) — non-blocking
        // 4. Snapshot transponder details (RACER-07)
        Entry entry = new Entry();
        entry.setStatus(EntryStatus.PENDING);
        entry.setTransponderSnapshot(req.transponderNumber(), req.transponderLabel());
        // ...
        entry = entryRepository.save(entry);
        // 5. Confirm (transition PENDING → CONFIRMED in same transaction)
        entry.setStatus(EntryStatus.CONFIRMED);
        return entry;
        // Service returns CONFIRMED; payment hook would interrupt here in v2
    }
}
```

[ASSUMED — service structure pattern; confirmed approach from D-10]

### Pattern 6: Flyway Test Data Seeding

Phase 2 integration tests need an OPEN event with classes. Use a `R__` repeatable migration or a fixed versioned seed migration in the test resources.

```
app/src/test/resources/db/migration/
└── V100__test_seed_events.sql   # Versioned, test-only seed
```

```sql
-- V100__test_seed_events.sql (test resources only)
INSERT INTO events (id, name, date, status) VALUES
    (1001, 'Test Open Event', current_date, 'OPEN'),
    (1002, 'Test Draft Event', current_date + 30, 'DRAFT');
-- Insert event_classes linking to racing_classes and format_templates seeded earlier
```

Place in `src/test/resources/db/migration/` — Flyway test configuration picks up both locations. [ASSUMED — test seed path; need to verify Flyway test config picks up test/resources]

### Anti-Patterns to Avoid

- **Hibernate in the read side:** Do NOT use `carRepository.findAll()` for list views that include tags. The N+1 problem on `carTagValues` is real. Use `CarQueryService` (jOOQ join query) for all list projections.
- **Hard-coding CONFIRMED in the controller:** Entry confirmation must go through `EntryService.submitEntry()` so v2 can intercept with payment. Do not set `entry.setStatus(CONFIRMED)` in the controller.
- **`/racer/*` wildcard route in App.tsx:** Remove the existing wildcard `path: '/racer/*'` and replace with the children array pattern. The wildcard blocks nested Outlet routing.
- **SecurityConfig gap:** `/api/v1/events` must be explicitly added to `permitAll` in `SecurityConfig`. The current config has `anyRequest().authenticated()` as the fallback — the public schedule will 401 without the permitAll rule.
- **Missing `@Transactional(readOnly = true)` on jOOQ services:** Always annotate read-only jOOQ service methods; prevents unnecessary dirty-checking overhead.
- **Car "delete" instead of archive:** Never issue `DELETE` against a car row. Use `archived = true`. The check is enforced at the service layer — no hard delete method on `CarRepository`.
- **Transponder uniqueness checked only per-user:** The uniqueness constraint on `transponder_number` is system-wide (RACER-05). The DB unique constraint enforces it; the service should catch `DataIntegrityViolationException` and return HTTP 409 with a meaningful message.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Form validation | Custom regex/validation logic | Zod schema + `zodResolver` | Already wired in auth screens; handles nested objects, custom messages |
| Server state cache invalidation | Manual state synchronisation | TanStack Query `invalidateQueries` | Handles stale checks, background refetch, deduplication |
| Slide-over / drawer panel | Custom CSS slide panel | shadcn/ui `Sheet` | Accessibility (focus trap, Escape to close, ARIA) already handled |
| Select dropdown with accessible keyboard nav | `<select>` element | shadcn/ui `Select` (Radix) | ARIA combobox, keyboard navigation built-in |
| Toast notifications | `alert()` or custom notification state | `sonner` (already installed) | Already wired via `<Toaster />` in App.tsx |
| Transponder uniqueness check | Application-level de-dupe loop | PostgreSQL UNIQUE constraint + service catches `DataIntegrityViolationException` | DB enforces atomically; service translates to 409 |
| Role-based endpoint guards | Manual `if (user.hasRole("RACER"))` in controllers | `@PreAuthorize("hasRole('RACER')")` + `@EnableMethodSecurity` already on | Method security already enabled in `SecurityConfig` |
| JSONB column mapping | Custom `AttributeConverter` | Hypersistence Utils `@Type(JsonType.class)` | Already used in `EventClass`; consistent pattern |

**Key insight:** The Phase 1 scaffold already provides all the primitives. Phase 2 is composition, not infrastructure.

---

## Common Pitfalls

### Pitfall 1: `anyRequest().authenticated()` Blocks Public Event Schedule
**What goes wrong:** `GET /api/v1/events` returns 401 for anonymous users.
**Why it happens:** `SecurityConfig` has `anyRequest().authenticated()` as the catch-all. The public event schedule is not in the `permitAll` list.
**How to avoid:** Add `.requestMatchers("/api/v1/events", "/api/v1/events/**").permitAll()` to `SecurityConfig.filterChain` before the catch-all.
**Warning signs:** IT test for public schedule fails with 401 when called without auth token.

### Pitfall 2: Existing `/racer/*` Wildcard Breaks Nested Routing
**What goes wrong:** React Router cannot match `/racer/profile`, `/racer/cars`, etc. because the wildcard `path: '/racer/*'` absorbs all traffic into `RacerPlaceholderPage` without child route matching.
**Why it happens:** `createBrowserRouter` with a wildcard does not create a parent/child relationship — it matches everything to one component.
**How to avoid:** Replace the wildcard entry in `App.tsx` with a `children:` array on `path: '/racer'`. The layout component renders `<Outlet />`.
**Warning signs:** All `/racer/*` routes render the placeholder page regardless of path.

### Pitfall 3: jOOQ Code Generation Not Configured for Phase 2 Tables
**What goes wrong:** `CarQueryService` references `CARS`, `CAR_TAG_VALUES` etc. which don't exist in the generated jOOQ DSL yet.
**Why it happens:** jOOQ's type-safe DSL is generated from the live schema. New Flyway migrations must run before code generation, and the codegen step must be part of the build.
**How to avoid:** Wave 0 must include: run Flyway migrations, run jOOQ codegen, confirm generated classes exist before writing query services. Check `app/build.gradle.kts` for jOOQ codegen plugin configuration — it may need to be added if not already present. [ASSUMED — jOOQ codegen plugin config not yet verified in build.gradle.kts; Phase 1 jOOQ was added as a dependency but codegen may not be configured]
**Warning signs:** Compilation error on `CARS.*` DSL references; `@Generated` classes missing in `build/generated-sources/jooq/`.

### Pitfall 4: Entry Transponder Conflict Warning vs Hard Block (RACER-09 vs RACER-14)
**What goes wrong:** RACER-09 (transponder conflict warning) and RACER-14 (membership block) have different severity: RACER-09 is a **warning** (soft, non-blocking), RACER-14 is a **block** (hard, blocking). Treating both as hard blocks breaks the entry flow.
**Why it happens:** Both look like validation, easy to implement both as service exceptions.
**How to avoid:** Service returns a result type or response DTO that includes `{ warnings: [], blocked: boolean, blockReason: String }`. Controller maps `blocked=true` to 422, `blocked=false+warnings` to 200 with warning body.
**Warning signs:** Racers cannot submit entries when another racer at the same event has the same transponder but their classes run at different times.

### Pitfall 5: Car Tag Values — N+1 on Hibernate `@OneToMany`
**What goes wrong:** Loading a list of 20 cars issues 21 queries (1 for cars + 20 for tag values).
**Why it happens:** Hibernate lazy-loads `carTagValues` per car when the collection is accessed in the controller/serializer.
**How to avoid:** All car list endpoints go through `CarQueryService` (jOOQ), not `carRepository.findAll()`. Keep `carTagValues` as a `@OneToMany` on `Car` for write-side convenience only — never use it for list reads.
**Warning signs:** Slow car list response; Hibernate SQL log shows repeated `SELECT * FROM car_tag_values WHERE car_id = ?` per row.

### Pitfall 6: Flyway Migration Numbering Collision
**What goes wrong:** Phase 2 migrations V6–V12 conflict with future phases if Phase 1 left V5 as the last migration but left a gap assumption.
**Why it happens:** Without a numbering plan, parallel development phases can conflict.
**How to avoid:** Phase 2 uses V6–V12 (or higher). Verify the last migration in `src/main/resources/db/migration/` is V5 before writing V6.
**Warning signs:** Flyway startup error "Migration V6 already exists" or checksum mismatch.

[VERIFIED: last migration is V5__create_race_formats.sql — Phase 2 starts at V6]

---

## Database Schema Design

### New Flyway Migrations (V6–V13)

**V6 — Extend users table (racer contact fields)**
```sql
ALTER TABLE users
    ADD COLUMN phone_number              varchar(30),
    ADD COLUMN emergency_contact_name    varchar(100),
    ADD COLUMN emergency_contact_phone   varchar(30),
    ADD COLUMN phonetic_name             varchar(255);
-- phonetic_name included early per AUDIO-12 (Phase 6 will use it; no harm adding now)
```

**V7 — Governing body memberships per user**
```sql
CREATE TABLE user_governing_body_memberships (
    id                   bigserial     PRIMARY KEY,
    user_id              bigint        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    governing_body_code  varchar(20)   NOT NULL,
    membership_number    varchar(50)   NOT NULL,
    created_at           timestamptz   NOT NULL DEFAULT now(),
    updated_at           timestamptz   NOT NULL DEFAULT now(),
    UNIQUE (user_id, governing_body_code)
);
CREATE INDEX idx_ugbm_user_id ON user_governing_body_memberships(user_id);
```

**V8 — User class ratings**
```sql
CREATE TABLE user_class_ratings (
    user_id          bigint  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    racing_class_id  bigint  NOT NULL REFERENCES racing_classes(id) ON DELETE CASCADE,
    rating           smallint NOT NULL DEFAULT 0 CHECK (rating BETWEEN 0 AND 100),
    PRIMARY KEY (user_id, racing_class_id)
);
```

**V9 — Cars**
```sql
CREATE TABLE cars (
    id               bigserial     PRIMARY KEY,
    user_id          bigint        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name             varchar(100)  NOT NULL,
    primary_class_id bigint        REFERENCES racing_classes(id) ON DELETE SET NULL,
    notes            text,
    archived         boolean       NOT NULL DEFAULT FALSE,
    created_at       timestamptz   NOT NULL DEFAULT now(),
    updated_at       timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX idx_cars_user_id ON cars(user_id);
```

**V10 — Car tag categories and values**
```sql
CREATE TABLE car_tag_categories (
    id          bigserial     PRIMARY KEY,
    name        varchar(100)  NOT NULL UNIQUE,
    sort_order  smallint      NOT NULL DEFAULT 0,
    created_at  timestamptz   NOT NULL DEFAULT now()
);
-- Default categories (RACER-10, D-11)
INSERT INTO car_tag_categories (name, sort_order) VALUES
    ('Chassis', 1), ('ESC', 2), ('Motor', 3), ('Servo', 4),
    ('Battery', 5), ('Body', 6), ('Tyres', 7);

CREATE TABLE car_tag_values (
    id           bigserial  PRIMARY KEY,
    car_id       bigint     NOT NULL REFERENCES cars(id) ON DELETE CASCADE,
    category_id  bigint     NOT NULL REFERENCES car_tag_categories(id) ON DELETE CASCADE,
    value        text       NOT NULL,
    UNIQUE (car_id, category_id)
);
CREATE INDEX idx_ctv_car_id ON car_tag_values(car_id);
```

**V11 — Transponders**
```sql
CREATE TABLE transponders (
    id                  bigserial     PRIMARY KEY,
    user_id             bigint        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    transponder_number  varchar(20)   NOT NULL UNIQUE,   -- system-wide uniqueness (RACER-05)
    label               varchar(100),
    created_at          timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX idx_transponders_user_id ON transponders(user_id);
CREATE INDEX idx_transponders_number ON transponders(transponder_number);
```

**V12 — Events table**

Note: `event_classes` already exists from Phase 1 (V5), but the `events` table has not been created yet. Phase 1 `event_classes` has a `template_id` FK but no `event_id` FK because events were deferred to Phase 3. Phase 2 needs a minimal `events` table for the public schedule (EVENT-04) and entry submission (EVENT-03). Phase 3 will add admin CRUD for events; Phase 2 just seeds test data.

```sql
CREATE TABLE events (
    id          bigserial     PRIMARY KEY,
    name        varchar(255)  NOT NULL,
    event_date  date          NOT NULL,
    status      varchar(20)   NOT NULL DEFAULT 'DRAFT'
                              CHECK (status IN ('DRAFT','PUBLISHED','OPEN','ENTRIES_CLOSED','IN_PROGRESS','COMPLETED')),
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now()
);

-- Link event_classes to events (backfill Phase 1 table)
ALTER TABLE event_classes ADD COLUMN event_id bigint REFERENCES events(id) ON DELETE CASCADE;
-- nullable for now; Phase 3 will make it NOT NULL once admin event CRUD exists
```

**V13 — Entries**
```sql
CREATE TABLE entries (
    id                      bigserial     PRIMARY KEY,
    user_id                 bigint        NOT NULL REFERENCES users(id),
    event_id                bigint        NOT NULL REFERENCES events(id),
    event_class_id          bigint        REFERENCES event_classes(id) ON DELETE SET NULL,
    transponder_number      varchar(20)   NOT NULL,   -- snapshot at submission (RACER-07)
    transponder_label       varchar(100),              -- snapshot at submission
    status                  varchar(20)   NOT NULL DEFAULT 'PENDING'
                                          CHECK (status IN ('PENDING','CONFIRMED','WITHDRAWN')),
    membership_override     boolean       NOT NULL DEFAULT FALSE,  -- RACER-14
    membership_override_by  bigint        REFERENCES users(id),
    membership_override_at  timestamptz,
    membership_override_note text,
    submitted_at            timestamptz   NOT NULL DEFAULT now(),
    updated_at              timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX idx_entries_user_id ON entries(user_id);
CREATE INDEX idx_entries_event_id ON entries(event_id);
CREATE UNIQUE INDEX idx_entries_no_duplicate ON entries(user_id, event_id, event_class_id)
    WHERE status != 'WITHDRAWN';
```

---

## Code Examples

### Entry Submission — Membership Check Pattern

```java
// domain/entry/EntryService.java
// Source: Pattern derived from RACER-14 requirement + GoverningBodyAffiliation.membershipRequired
// [ASSUMED — service method structure; business logic from REQUIREMENTS.md]
private void checkMembershipRequirement(Long userId, Long eventId) {
    // Get club's governing body affiliations with membershipRequired = true
    List<GoverningBodyAffiliation> required = affiliationRepository
        .findByMembershipRequiredTrue();
    if (required.isEmpty()) return;

    // Get racer's membership numbers
    Set<String> racerCodes = membershipRepository
        .findByUserId(userId)
        .stream()
        .map(UserGoverningBodyMembership::getGoverningBodyCode)
        .collect(Collectors.toSet());

    boolean hasRequiredMembership = required.stream()
        .anyMatch(aff -> racerCodes.contains(aff.getCode()));

    if (!hasRequiredMembership) {
        throw new MembershipRequiredException(
            "Entry blocked: club requires governing body membership");
    }
}
```

### React Query Key Convention

```typescript
// Consistent query key structure for all racer portal queries
// [ASSUMED — project convention; no prior art in Phase 1 for racer queries]
export const racerQueryKeys = {
  profile: ['racer', 'profile'] as const,
  cars: ['racer', 'cars'] as const,
  car: (id: number) => ['racer', 'cars', id] as const,
  transponders: ['racer', 'transponders'] as const,
  entries: ['racer', 'entries'] as const,
  eventSchedule: ['events', 'schedule'] as const,
};
```

### Select Integration with React Hook Form v7 (for entry submission form)

```tsx
// Source: https://github.com/shadcn-ui/ui/blob/main/apps/v4/content/docs/forms/react-hook-form.mdx
// [VERIFIED via Context7 docs fetch]
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

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| React 18 | React 19 (installed: 19.2.5) | Concurrent features available; no breaking changes for this use case |
| React Router v6 `Routes/Route` | React Router v7 `createBrowserRouter` with children array | Nested layouts via Outlet; data APIs available if needed |
| TanStack Query v4 `isLoading` | TanStack Query v5 `isPending` | `isLoading` renamed `isPending` for new queries — use `isPending` in all v5 code |
| shadcn/ui v2 CLI `shadcn-ui` | shadcn/ui v4 CLI `shadcn` | Install command is `npx shadcn@latest add <component>` |

**TanStack Query v5 naming note:** [VERIFIED: installed version 5.99.0] In v5 the `isLoading` property on `useQuery` became `isPending`. Code generated using v4 docs will have a subtle bug. Always use `isPending` for initial load state in v5.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | jOOQ codegen plugin is not yet configured in `app/build.gradle.kts`; Phase 2 Wave 0 must add it | Pitfall 3, Patterns | If codegen already exists, Wave 0 step can be skipped; low risk |
| A2 | Test-only Flyway migrations can be placed in `src/test/resources/db/migration/` and will be picked up by the Testcontainers test config | Pattern 6 | If not configured, test seed data must be inserted programmatically in `@BeforeEach`; workable fallback |
| A3 | `event_classes` table (Phase 1, V5) does not yet have an `event_id` FK column; V12 adds it as nullable | Schema design | If Phase 1 already added `event_id` to event_classes, skip that part of V12 |
| A4 | `RACER` role enum value already exists in `Role.java` and is assigned on registration | Architecture | Verified `UserService.createRacer` sets `Set.of(Role.RACER)` — LOW risk |
| A5 | shadcn/ui Sheet, Select, Badge, Dialog, Separator not installed; install in Wave 0 | Standard Stack | Confirmed by `ls frontend/src/components/ui/` — only button, card, form, input, label, sonner present |

[A4 is actually VERIFIED — `UserService.createRacer` confirmed; marked ASSUMED above in error — it is LOW risk verified.]

---

## Open Questions

1. **jOOQ codegen build config**
   - What we know: `jooq` is in `dependencies` of `build.gradle.kts` but no codegen plugin block is visible in the checked file.
   - What's unclear: Is codegen configured in the root `build.gradle.kts` or via a separate Gradle plugin? Has it been run before?
   - Recommendation: Wave 0 task should check for `org.jooq.jooq-codegen` plugin; add if absent and run codegen against the dev Postgres (Docker Compose) before writing query services.

2. **Event state machine scope in Phase 2**
   - What we know: `EVENT-05` (DRAFT → PUBLISHED → OPEN state machine) is assigned to Phase 3. Phase 2 needs events to be in `OPEN` state for entry submission testing.
   - What's unclear: Should Phase 2 add the full `status` column with all valid states but skip the transition enforcement? Or just a minimal `status varchar` with OPEN/DRAFT for seeding?
   - Recommendation: Add the full status column with the CHECK constraint in V12 (correct from day one) but do NOT implement state machine transition enforcement — that is Phase 3. Seed test data directly as `OPEN`.

3. **`event_classes` to `events` FK**
   - What we know: Phase 1's `event_classes` table exists (V5) with `template_id` but no `event_id`. Events are Phase 3 admin CRUD.
   - What's unclear: Should Phase 2 add `event_id` FK to `event_classes` now (as nullable), or leave it until Phase 3?
   - Recommendation: Add as nullable in V12 (event_classes alteration). Phase 3 adds NOT NULL via data migration once all event_classes have events assigned.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 | Backend compile/run | Yes | OpenJDK 21.0.10 LTS | — |
| Gradle | Build tool | Yes | 8.14.2 | — |
| Node.js | Frontend build | Yes | 20.19.2 | — |
| Docker | Dev Postgres, Testcontainers | Yes | 26.1.5 | — |
| PostgreSQL (Docker Compose) | Local dev + jOOQ codegen | Not checked (docker compose ps returned empty) | — | Run `docker compose up -d` |
| shadcn/ui Sheet/Select/Badge/Dialog/Separator | Frontend components | No (not installed) | — | `npx shadcn@latest add <component>` |

**Missing dependencies with fallback:**
- Docker Compose Postgres not running: run `docker compose up -d` before jOOQ codegen or local development.
- shadcn/ui components: install via CLI in Wave 0 (no internet unavailability concern — CLI fetches from registry).

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + AssertJ + Testcontainers (backend); no frontend test framework installed yet |
| Config file | `app/build.gradle.kts` — `tasks.withType<Test> { useJUnitPlatform() }` |
| Quick run command | `./gradlew :app:test --tests "*.racer.*" -x generateJooq` |
| Full suite command | `./gradlew :app:test` |

Frontend: no Vitest config detected in `frontend/` (no `vitest.config.*` found). CLAUDE.md specifies Vitest + React Testing Library. Wave 0 should scaffold frontend test config if frontend unit tests are required.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| RACER-01 | Profile fields updated and returned | Integration | `./gradlew :app:test --tests "*RacerProfileControllerIT*"` | No — Wave 0 |
| RACER-02 | Car create / edit / archive | Integration | `./gradlew :app:test --tests "*CarControllerIT*"` | No — Wave 0 |
| RACER-03 | Transponder create and list | Integration | `./gradlew :app:test --tests "*TransponderControllerIT*"` | No — Wave 0 |
| RACER-04 | Entry history returns past entries | Integration | `./gradlew :app:test --tests "*EntryControllerIT*"` | No — Wave 0 |
| RACER-05 | Duplicate transponder returns 409 | Integration | `./gradlew :app:test --tests "*TransponderControllerIT*duplicate*"` | No — Wave 0 |
| RACER-06 | Archived car not in active list | Integration | `./gradlew :app:test --tests "*CarControllerIT*archive*"` | No — Wave 0 |
| RACER-07 | Entry snapshot transponder number | Integration | `./gradlew :app:test --tests "*EntryControllerIT*snapshot*"` | No — Wave 0 |
| RACER-08 | Admin transponder swap audit log | Integration | `./gradlew :app:test --tests "*AdminEntryControllerIT*transponder*"` | No — Wave 0 |
| RACER-09 | Duplicate transponder at event = warning not block | Integration | `./gradlew :app:test --tests "*EntryControllerIT*conflict*"` | No — Wave 0 |
| RACER-10 | Tag categories seeded by Flyway | Integration | `./gradlew :app:test --tests "*CarTagCategoryIT*"` | No — Wave 0 |
| RACER-11 | Tag values saved and returned on car | Integration | `./gradlew :app:test --tests "*CarControllerIT*tags*"` | No — Wave 0 |
| RACER-13 | Membership number stored and returned | Integration | `./gradlew :app:test --tests "*RacerProfileControllerIT*membership*"` | No — Wave 0 |
| RACER-14 | Entry blocked when membership required, override allows | Integration | `./gradlew :app:test --tests "*EntryControllerIT*membership*"` | No — Wave 0 |
| EVENT-03 | Entry submission happy path | Integration | `./gradlew :app:test --tests "*EntryControllerIT*submit*"` | No — Wave 0 |
| EVENT-04 | Public event schedule — no auth required | Integration | `./gradlew :app:test --tests "*EventScheduleControllerIT*public*"` | No — Wave 0 |
| ENTRY-01 | Withdraw entry changes status to WITHDRAWN | Integration | `./gradlew :app:test --tests "*EntryControllerIT*withdraw*"` | No — Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.racer.*" --tests "dev.monkeypatch.rctiming.api.admin.*"`
- **Per wave merge:** `./gradlew :app:test`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/racer/RacerProfileControllerIT.java` — REQ RACER-01, RACER-13
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/racer/CarControllerIT.java` — REQ RACER-02, RACER-06, RACER-11
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/racer/TransponderControllerIT.java` — REQ RACER-03, RACER-05
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/racer/EntryControllerIT.java` — REQ RACER-04, RACER-07, RACER-09, RACER-14, EVENT-03, ENTRY-01
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/admin/AdminEntryControllerIT.java` — REQ RACER-08
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/admin/CarTagCategoryIT.java` — REQ RACER-10
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/EventScheduleControllerIT.java` — REQ EVENT-04
- [ ] `app/src/test/resources/db/migration/V100__test_seed_events.sql` — Flyway seed for OPEN event

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | Inherited | JWT filter from Phase 1; no changes |
| V3 Session Management | Inherited | HttpOnly refresh cookie from Phase 1; no changes |
| V4 Access Control | Yes | `@PreAuthorize("hasRole('RACER')")` on racer endpoints; `hasAnyRole('ADMIN',...)` on admin override endpoints |
| V5 Input Validation | Yes | `@Valid` on all request bodies; Zod schemas on frontend; string length constraints on Car.name, Transponder.number |
| V6 Cryptography | Not applicable | No new crypto; passwords handled by Phase 1 BCrypt |

### Known Threat Patterns for This Stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Racer accesses another racer's car/transponder/entry by ID | Elevation of Privilege | Service layer checks `userId == authenticated user id`; never just `findById` without ownership check |
| Admin membership override without audit | Tampering | `membership_override_by` + `membership_override_at` + `membership_override_note` columns; logged |
| Transponder number enumeration | Information Disclosure | 409 on duplicate gives no information about which user owns the transponder |
| Entry submission to CLOSED event | Tampering | Service validates `event.status == OPEN` before allowing entry |
| Racer withdraws after entries close | Tampering | Service checks `event.status` is still OPEN before allowing withdrawal |
| Forced browsing to `/api/v1/admin/**` | Elevation of Privilege | SecurityConfig `hasAnyRole` on `/api/v1/admin/**` already enforced |

---

## Sources

### Primary (HIGH confidence)
- [VERIFIED: app/build.gradle.kts] — Confirmed all backend dependency versions
- [VERIFIED: frontend/package.json + node_modules] — Confirmed all frontend library versions
- [VERIFIED: app/src/main/resources/db/migration/] — Confirmed migration history (V1–V5), next migration is V6
- [VERIFIED: app/src/main/java/.../SecurityConfig.java] — Confirmed security rules and `@EnableMethodSecurity`
- [VERIFIED: app/src/main/java/.../user/User.java] — Confirmed User entity fields (no racer contact fields yet)
- [VERIFIED: app/src/main/java/.../user/UserService.java] — Confirmed `RACER` role assigned on registration
- [VERIFIED: frontend/src/components/ui/ ls] — Confirmed missing shadcn components
- Context7 `/shadcn-ui/ui` — Sheet component pattern
- Context7 `/tanstack/query` — useMutation + invalidateQueries pattern
- Context7 `/remix-run/react-router` — Outlet nested layout pattern

### Secondary (MEDIUM confidence)
- Phase 1 CONTEXT.md patterns — EventClass entity (snapshot+override, Hypersistence Utils) as reference for Phase 2 entity design
- AuthControllerIT.java — Established integration test pattern (TestRestTemplate, AbstractIntegrationTest, `registerAndLogin` helper)

### Tertiary (LOW confidence)
- jOOQ codegen build config presence/absence — not verified in root `build.gradle.kts`
- Flyway test resource path pickup — assumed based on Spring Boot test convention

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all versions verified from installed artifacts
- Schema design: HIGH — follows established V1–V5 patterns directly
- Architecture patterns: HIGH — verified from existing code + Context7
- Pitfalls: HIGH — majority verified from actual code examination
- jOOQ codegen config: LOW — not fully verified in build files

**Research date:** 2026-04-16
**Valid until:** 2026-05-16 (stable stack, 30-day window)
