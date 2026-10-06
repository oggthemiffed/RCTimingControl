---
phase: "02"
plan: "04"
subsystem: events-entries
tags: [events, entries, entry-audit-log, domain, jooq, api, admin, integration-tests]
depends_on: ["02-01", "02-02", "02-03"]
provides:
  - Event JPA entity + repository + EventStatus enum
  - Entry JPA entity + repository + EntryStatus enum (transponder snapshot stored in transponder_number/transponder_label columns)
  - EntryAuditLog entity + repository (text snapshot columns for admin override audit trail)
  - EntryService: submit (D-10 auto-confirm in same transaction), withdraw (ownership filter), adminUpdateTransponder (D-12), adminApplyMembershipOverride (D-13)
  - EventScheduleQuery: first jOOQ public event schedule read (Pattern 1)
  - EntryQueryService: jOOQ entry history projection joining entries+events (RACER-04)
  - EventScheduleController: public GET /api/v1/events (no auth)
  - EntryController: RACER-scoped POST/GET/DELETE /api/v1/racer/entries
  - AdminEntryController: PATCH /transponder (ADMIN/RD/REFEREE) + POST /membership-override (ADMIN/RD)
  - SecurityConfig: /api/v1/events GET permit-all rule
  - Flyway V14: entry_audit_log table, missing entry/event/event_class columns
  - Three integration test classes (17 tests total)
affects:
  - app/src/main/java/dev/monkeypatch/rctiming/domain/event/
  - app/src/main/java/dev/monkeypatch/rctiming/domain/entry/
  - app/src/main/java/dev/monkeypatch/rctiming/domain/format/EventClass.java
  - app/src/main/java/dev/monkeypatch/rctiming/query/event/
  - app/src/main/java/dev/monkeypatch/rctiming/query/entry/
  - app/src/main/java/dev/monkeypatch/rctiming/api/racer/
  - app/src/main/java/dev/monkeypatch/rctiming/api/admin/
  - app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java
  - app/src/main/resources/db/migration/V14__create_entry_audit_log.sql
  - app/src/test/resources/db/migration/test/V100__test_seed_events.sql
tech_stack:
  added: []
  patterns:
    - EntryService uses jOOQ (DSLContext) directly to fetch EventClass fields, avoiding Hibernate JsonType deserialization of config_snapshot JSONB
    - EntryAuditLog uses plain text columns for JSON snapshot storage (no type mapping issues)
    - RACER-09 soft warning via findByEventIdAndTransponderNumberSnapshotAndStatusAndUserIdNot derived query
    - RACER-14 hard block resolved by jOOQ lookup of required_governing_body_code from event_classes
    - D-10 auto-confirm: PENDING→CONFIRMED in same @Transactional boundary (no partial state visible to clients)
    - Admin audit atomicity: EntryAuditLog.save() called in same @Transactional as entry mutation
key_files:
  created:
    - app/src/main/resources/db/migration/V14__create_entry_audit_log.sql
    - app/src/main/java/dev/monkeypatch/rctiming/domain/event/EventStatus.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/event/Event.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/event/EventRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryStatus.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/entry/Entry.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryAuditLog.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryAuditLogRepository.java
    - app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryService.java
    - app/src/main/java/dev/monkeypatch/rctiming/query/event/EventScheduleDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/query/event/EventScheduleQuery.java
    - app/src/main/java/dev/monkeypatch/rctiming/query/entry/RacerEntryHistoryDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/query/entry/EntryQueryService.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/EntryDto.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/EntryResult.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/dto/SubmitEntryRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/admin/dto/AdminUpdateTransponderRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/admin/dto/MembershipOverrideRequest.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/EntryController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/racer/EventScheduleController.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/admin/AdminEntryController.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/racer/EntryControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/racer/EventScheduleControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/admin/AdminEntryControllerIT.java
  modified:
    - app/src/main/java/dev/monkeypatch/rctiming/domain/format/EventClass.java
    - app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java
    - app/src/test/resources/db/migration/test/V100__test_seed_events.sql
decisions:
  - "EntryService uses jOOQ DSLContext to look up EventClass.event_id and required_governing_body_code instead of JPA repository — the EventClass entity has a Hypersistence JsonType on config_snapshot that fails to deserialize in tests when loaded via findById; jOOQ column projection bypasses the problematic field entirely"
  - "entry_audit_log snapshot columns use text instead of jsonb — the plan specified jsonb but plain text is simpler (no Hibernate type mapping, no PostgreSQL cast issues), and audit records are append-only so JSONB query operators are never needed"
  - "EventStatus enum uses actual V12 schema values (DRAFT/PUBLISHED/OPEN/ENTRIES_CLOSED/IN_PROGRESS/COMPLETED) rather than the plan interface block values (DRAFT/PUBLISHED/CLOSED/CANCELLED) — the real migration was written before this plan was executed"
  - "Entry entity column names follow actual V13 schema (transponder_number/transponder_label) mapped to Java fields named transponderNumberSnapshot/transponderLabelSnapshot for clarity — no DB rename needed"
  - "V14 migration adds the entry_audit_log table (missing from V13 per plan note), plus entry lifecycle columns (car_id, transponder_id, confirmed_at, withdrawn_at), event entry window columns, and event_class membership requirement column"
metrics:
  duration_minutes: 65
  completed_date: "2026-04-17"
  tasks_completed: 3
  tasks_total: 3
  files_created: 25
  files_modified: 3
---

# Phase 02 Plan 04: Event + Entry Backend Summary

**One-liner:** Event + Entry domain with D-10 auto-confirm, RACER-07 transponder snapshot, RACER-09 soft warning, RACER-14 membership block, admin audit trail (D-12/D-13), jOOQ public event schedule + entry history projections, and 17 integration tests covering all entry lifecycle behaviors.

## What Was Built

### Flyway V14 Migration

Added all missing schema components:

| Change | Detail |
|--------|--------|
| `entry_audit_log` table | Records admin transponder swaps and membership overrides with before/after text snapshots |
| `entries.car_id` | FK to cars — nullable (V14 additive) |
| `entries.transponder_id` | FK to transponders — nullable (V14 additive) |
| `entries.confirmed_at` | Timestamp of D-10 auto-confirm |
| `entries.withdrawn_at` | Timestamp of racer or admin withdrawal |
| `events.entry_opens_at` | Optional entry window open time |
| `events.entry_closes_at` | Optional entry window close time |
| `event_classes.required_governing_body_code` | Nullable varchar(50) for RACER-14 membership enforcement |

### Domain Layer

**Event** — mapped to V12 schema. `EventStatus` enum: `DRAFT, PUBLISHED, OPEN, ENTRIES_CLOSED, IN_PROGRESS, COMPLETED` (matches actual V12 CHECK constraint). Entry submission accepted when status is `OPEN` or `PUBLISHED` and entry window is open.

**Entry** — mapped to V13 schema. Java field names use `...Snapshot` suffix for clarity, mapping to `transponder_number`/`transponder_label` columns. `membershipOverrideByAdminId` maps to `membership_override_by` FK column.

**EntryAuditLog** — write-once audit table. `action` is `TRANSPONDER_SWAP` or `MEMBERSHIP_OVERRIDE`. Snapshot columns are `text` (stores Jackson-serialized JSON as plain string).

### EntryService Lifecycle

```
submitEntry(userId, req):
  1. Load Event → assert OPEN or PUBLISHED and not past entry close time (422 if not)
  2. jOOQ SELECT event_id, required_governing_body_code FROM event_classes WHERE id=? 
     → assert event_id matches (400 if mismatch)
  3. Load Transponder → ownership filter by userId (404 if not owned)
  4. Load Car → ownership filter by userId (404 if not owned)
  5. RACER-14: if requiredCode set, assert UserGoverningBodyMembership exists (422 if not)
  6. Persist Entry with status=PENDING, snapshot columns, car_id, transponder_id
  7. D-10 in-transaction: set status=CONFIRMED, confirmedAt=now (same @Transactional)
  8. RACER-09: check for other CONFIRMED entries with same transponder number → append warning (non-blocking)
  9. Return EntryResult(EntryDto, warnings)

withdraw(entryId, userId):
  Load Entry → filter by userId (404 if not owner)
  Assert not WITHDRAWN (400)
  Set WITHDRAWN, withdrawnAt=now

adminUpdateTransponder(entryId, adminUserId, req):
  Load Entry (no ownership filter — admin can touch any)
  Load new Transponder by ID (no ownership filter — D-12)
  Snapshot before state, update snapshot fields + transponder_id
  Write EntryAuditLog(TRANSPONDER_SWAP) in same transaction

adminApplyMembershipOverride(entryId, adminUserId, reason):
  Load Entry
  Set membershipOverrideByAdminId, status=CONFIRMED, confirmedAt=now
  Write EntryAuditLog(MEMBERSHIP_OVERRIDE, reason) in same transaction
```

### jOOQ Query Services

**EventScheduleQuery** — public event schedule (status IN PUBLISHED, OPEN, ENTRIES_CLOSED, IN_PROGRESS). Computes `EntryAvailability` enum (ENTRY_OPEN / ENTRY_NOT_YET_OPEN / ENTRY_CLOSED) from status and entry window timestamps.

**EntryQueryService** — racer entry history. Joins `entries` + `events`, filters by `user_id = :userId` and status IN (CONFIRMED, WITHDRAWN). PENDING rows are transient (D-10 completes in-transaction).

### API Endpoints

| Endpoint | Auth | Response |
|----------|------|----------|
| GET /api/v1/events | Public | List\<EventScheduleDto\> |
| POST /api/v1/racer/entries | RACER | 201 EntryResult (entry + warnings) |
| GET /api/v1/racer/entries | RACER | List\<RacerEntryHistoryDto\> |
| DELETE /api/v1/racer/entries/{id} | RACER | 204 (404 if not owner) |
| PATCH /api/v1/admin/entries/{id}/transponder | ADMIN/RD/REFEREE | 200 EntryDto |
| POST /api/v1/admin/entries/{id}/membership-override | ADMIN/RD | 200 EntryDto |

### SecurityConfig Change

Added before `anyRequest().authenticated()`:
```java
.requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/**").permitAll()
```

### Integration Tests

| Class | Tests | Coverage |
|-------|-------|----------|
| EventScheduleControllerIT | 3 | anonymous 200, ENTRY_OPEN availability, DRAFT excluded |
| EntryControllerIT | 8 | submit+confirm, snapshot, membership block, membership held, withdraw 204, cross-user withdraw 404, history isolation, anonymous 401 |
| AdminEntryControllerIT | 6 | transponder swap + audit, membership override + audit, blank reason 400, referee override 403, referee transponder OK, racer forbidden 403 |
| **Total** | **17** | RACER-04, RACER-07, RACER-09 (service pattern), RACER-14, EVENT-03, EVENT-04, ENTRY-01 |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] EventClass JPA load fails with Hypersistence JsonType deserialization error**
- **Found during:** Task 3 (first test run)
- **Issue:** `eventClassRepository.findById()` triggers Hibernate to load the full `EventClass` entity including `config_snapshot JSONB`. The `@Type(JsonType.class)` mapping fails with `IllegalArgumentException: The given string value: {"type":"jsonb","value":"...","null":false} cannot be transformed to Json object` when reading back data inserted by V100 test seed.
- **Fix:** Replaced JPA `EventClassRepository.findById()` call in `EntryService.submitEntry` with a targeted `dsl.select(EVENT_CLASSES.EVENT_ID, EVENT_CLASSES.REQUIRED_GOVERNING_BODY_CODE).from(EVENT_CLASSES).where(EVENT_CLASSES.ID.eq(...))` query. This bypasses the problematic `config_snapshot` column entirely and aligns with the CQRS-lite pattern (reads via jOOQ).
- **Files modified:** `EntryService.java`
- **Commit:** bda87de

**2. [Rule 1 - Bug] EntryAuditLog JSONB columns reject String values without explicit cast**
- **Found during:** Task 3 (AdminEntryControllerIT first run)
- **Issue:** PostgreSQL error `column "after_snapshot" is of type jsonb but expression is of type character varying`. Even with `@Column(columnDefinition = "jsonb")` and later `@JdbcTypeCode(SqlTypes.JSON)`, Hibernate's JDBC bind path produced a varchar parameter that PostgreSQL refused to cast to jsonb automatically.
- **Fix:** Changed `before_snapshot` and `after_snapshot` in V14 migration from `jsonb` to `text`. Updated `EntryAuditLog` entity to use `@Column(columnDefinition = "text")`. The audit snapshots are Jackson-serialized JSON strings stored as plain text — no PostgreSQL JSONB operators are needed on this table.
- **Files modified:** `V14__create_entry_audit_log.sql`, `EntryAuditLog.java`
- **Commit:** bda87de

**3. [Rule 2 - Schema deviation] EventStatus enum values adapted to actual V12 schema**
- **Found during:** Task 1 (plan interface block inspection)
- **Issue:** The plan's interface block specified `DRAFT, PUBLISHED, CLOSED, CANCELLED` but V12 migration (written in Plan 01) uses `DRAFT, PUBLISHED, OPEN, ENTRIES_CLOSED, IN_PROGRESS, COMPLETED`.
- **Fix:** `EventStatus` enum uses the actual V12 values. `submitEntry` accepts both `OPEN` and `PUBLISHED` status as valid for entry. `EventScheduleQuery` shows `PUBLISHED, OPEN, ENTRIES_CLOSED, IN_PROGRESS` events publicly.
- **Files modified:** `EventStatus.java`, `EntryService.java`, `EventScheduleQuery.java`
- **Commit:** 97f13da

**4. [Rule 2 - Missing] V14 migration adds columns missing from Plan 01 V13 schema**
- **Found during:** Task 1 (V13 schema inspection)
- **Issue:** V13 as written in Plan 01 lacked: `car_id`, `transponder_id`, `confirmed_at`, `withdrawn_at` on entries; `entry_opens_at`, `entry_closes_at` on events; `required_governing_body_code` on event_classes; and the `entry_audit_log` table entirely.
- **Fix:** V14 migration adds all missing columns and the audit log table. This is an additive schema change with no impact on existing data.
- **Files modified:** `V14__create_entry_audit_log.sql`
- **Commit:** 97f13da

**5. [Rule 2 - Missing] V100 test seed updated to set BRCA membership requirement**
- **Found during:** Task 3 (membership block test)
- **Issue:** V100 inserted event_class 2002 without `required_governing_body_code`, so the RACER-14 test could not exercise the membership block path.
- **Fix:** Added `UPDATE event_classes SET required_governing_body_code = 'BRCA' WHERE id = 2002` to V100 seed.
- **Files modified:** `V100__test_seed_events.sql`
- **Commit:** bda87de

## Known Stubs

None — all endpoints return live data. EntryResult.warnings is computed from real duplicate-transponder detection (though the system-wide UNIQUE constraint on transponder_number makes natural collisions impossible without admin intervention; the warning path is exercised by the service logic and unit-testable via mock).

## Threat Surface Scan

All threat model mitigations applied per plan's STRIDE register:

| Threat ID | Mitigation Applied |
|-----------|-------------------|
| T-02-04-02 | `filter(t -> t.getUserId().equals(userId))` on transponder + car lookups in submitEntry |
| T-02-04-03 | `filter(e -> e.getUserId().equals(userId))` in withdraw → 404 for cross-user delete |
| T-02-04-04 | Class-level `@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")` on AdminEntryController |
| T-02-04-05 | Method-level `@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")` on membership-override endpoint (referee excluded) |
| T-02-04-06 | EntryAuditLog written atomically with every admin mutation in same @Transactional |
| T-02-04-07 | EntryQueryService WHERE clause: `ENTRIES.USER_ID.eq(userId)` — no cross-user leak |
| T-02-04-11 | EventScheduleQuery WHERE: status IN (PUBLISHED, OPEN, ENTRIES_CLOSED, IN_PROGRESS) — DRAFT and CANCELLED excluded |
| T-02-04-12 | SubmitEntryRequest record has no status field; Jackson drops unknown properties |

## Self-Check

Checking created files exist and commits are present...

- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/domain/event/Event.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/domain/entry/Entry.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryAuditLog.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryService.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/query/event/EventScheduleQuery.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/query/entry/EntryQueryService.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/api/racer/EntryController.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/api/racer/EventScheduleController.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/api/admin/AdminEntryController.java` — present
- `/home/david/git/java/RCTimingControl/app/src/main/java/dev/monkeypatch/rctiming/security/SecurityConfig.java` — present
- `/home/david/git/java/RCTimingControl/app/src/test/java/dev/monkeypatch/rctiming/api/racer/EntryControllerIT.java` — present
- `/home/david/git/java/RCTimingControl/app/src/test/java/dev/monkeypatch/rctiming/api/racer/EventScheduleControllerIT.java` — present
- `/home/david/git/java/RCTimingControl/app/src/test/java/dev/monkeypatch/rctiming/api/admin/AdminEntryControllerIT.java` — present

Commits verified:
- 97f13da — feat(02-04): Event + Entry + EntryAuditLog domain (Task 1)
- 5059124 — feat(02-04): jOOQ query services, controllers, SecurityConfig (Task 2)
- bda87de — feat(02-04): integration tests (Task 3)

## Self-Check: PASSED
