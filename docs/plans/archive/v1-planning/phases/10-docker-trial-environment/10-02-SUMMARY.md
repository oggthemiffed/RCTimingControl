---
phase: 10-docker-trial-environment
plan: "02"
subsystem: infra
tags: [docker, postgresql, seed-data, demo-data]

requires:
  - phase: 01-domain-foundation
    provides: Flyway migrations V1-V26 defining the full schema that seed targets

provides:
  - docker/seed/seed.sql — idempotent Wyvern RC Club demo data (9 users, 2 tracks, 3 formats, 8 racers, championship, historical results)
  - docker/seed/Dockerfile — one-shot psql runner image based on postgres:16-alpine
  - ACTIVE forwarder_token row matching DEMO-FORWARDER-TOKEN-CHANGE-BEFORE-PRODUCTION

affects:
  - 10-docker-trial-environment (Plans 03 and 04 depend on this seed service)

tech-stack:
  added: []
  patterns:
    - "PL/pgSQL DO block with dual guards (schema presence + idempotency) for safe one-shot seeding"
    - "RETURNING id INTO variable chain for FK-safe inserts without hard-coded IDs"
    - "postgres:16-alpine as one-shot seed runner base (includes psql)"
    - "psql -v ON_ERROR_STOP=1 for non-zero exit on seed failure"

key-files:
  created:
    - docker/seed/seed.sql
    - docker/seed/Dockerfile
  modified: []

key-decisions:
  - "Seed uses PL/pgSQL RETURNING id INTO variable chain to maintain FK relationships without hardcoded IDs"
  - "Guard 1 raises EXCEPTION (not NOTICE) on missing schema — makes premature runs fail loudly for compose restart logic"
  - "Guard 2 checks club_profiles WHERE name = 'Wyvern RC Club' — single idempotency sentinel"
  - "forwarder_token token_value set to DEMO-FORWARDER-TOKEN-CHANGE-BEFORE-PRODUCTION matching Plan 04 .env.example"
  - "Entries seeded for both Round 3 (completed) and Round 4 (open) event classes separately"
  - "race_entries includes car_number column (added by V24 migration) for result display compatibility"

patterns-established:
  - "PL/pgSQL DECLARE section with typed bigint variables for all FK-chained IDs"
  - "Idempotency sentinel on parent table (club_profiles) covers all child data — no per-table guards needed"

requirements-completed: [SC-2]

duration: 15min
completed: 2026-05-16
---

# Phase 10 Plan 02: Demo Seed Container Summary

**Idempotent Wyvern RC Club seed SQL populating 9 accounts, 8 transponders (101-108), championship with 3 completed rounds, and one-shot postgres:16-alpine runner Dockerfile with ON_ERROR_STOP=1**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-05-16T (wave 1 parallel execution)
- **Completed:** 2026-05-16
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Authored 482-line idempotent seed SQL wrapped in PL/pgSQL DO block with schema guard and idempotency guard
- Seeded complete mid-season scenario: Wyvern RC Club, 2 tracks with decoder loops, 3 race format templates, 3 racing classes, 9 user accounts (1 admin + 8 racers), 8 cars and transponders (IDs 101-108), 6-round championship (links for rounds 3 and 4), Round 3 completed race with result_snapshot and Round 4 open with confirmed entries
- Created minimal Dockerfile (`FROM postgres:16-alpine`, COPY seed.sql, `psql -v ON_ERROR_STOP=1 -f /seed.sql`) that builds successfully
- `docker build -f docker/seed/Dockerfile -t rctiming-seed:test docker/seed/` exits 0

## Task Commits

Each task was committed atomically:

1. **Task 1: Author idempotent Wyvern RC Club seed SQL** - `85ae665` (feat)
2. **Task 2: Demo-seed Dockerfile (one-shot psql runner)** - `5d0f00a` (feat)

## Files Created/Modified

- `docker/seed/seed.sql` - Full Wyvern RC Club demo seed data: governing body, users+roles, club profile, tracks+decoder_loops, racing classes, race format templates, events, event_classes, cars, transponders 101-108, entries (both events), championship+classes+event_links+points_scale, historical round/race/race_entries/result_snapshot, ACTIVE forwarder_token
- `docker/seed/Dockerfile` - One-shot psql runner: FROM postgres:16-alpine, COPY seed.sql, ENTRYPOINT with ON_ERROR_STOP=1

## Decisions Made

- Used PL/pgSQL `RETURNING id INTO` variable chain throughout rather than hardcoded IDs — makes seed portable across any database state
- Guard 1 raises EXCEPTION (not NOTICE) so that a premature run before Flyway migrations results in a non-zero compose exit, enabling the service to be retried
- Entries created separately for Round 3 (completed) and Round 4 (open) events — each entry set references its own event_class_id, keeping FK chain clean
- race_entries includes `car_number` column (added by V24 migration) populated 1-8 for the historical race

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The migration schema reading confirmed all column names before writing SQL. The `car_number` column on `race_entries` was added by V24 (not V17), which was discovered by checking and handled correctly.

## Known Stubs

None. All seeded data is complete and realistic. No placeholder values flow to UI rendering beyond the intentional demo token documented as requiring rotation.

## Threat Flags

No new threat surface introduced. Threat model from plan covers all seeded data concerns:
- T-10-03: All emails use @example.com, no real person data
- T-10-07: Demo forwarder token documented as trial-only
- T-10-08: Idempotency guard prevents re-run corruption

## User Setup Required

None - this is a seed data container. libpq env vars (PGHOST/PGUSER/PGPASSWORD/PGDATABASE) are supplied by docker-compose (Plan 03).

## Next Phase Readiness

- `docker/seed/` is complete and ready for Plan 03 to wire it into `docker-compose.trial.yml` as a `demo-seed` service with `depends_on: postgres: condition: service_healthy`
- Plan 03 should set `restart: "no"` and `condition: service_completed_successfully` on the demo-seed service
- The forwarder_token `DEMO-FORWARDER-TOKEN-CHANGE-BEFORE-PRODUCTION` must match the `FORWARDER_API_TOKEN` default in Plan 04's `.env.example`

---
*Phase: 10-docker-trial-environment*
*Completed: 2026-05-16*
