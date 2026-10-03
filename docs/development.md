# Development Guide

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 (LTS) |
| Docker | 24+ |
| Node.js | 20+ |
| Gradle | via wrapper (`./gradlew`) |

## Quick start (Makefile)

A `Makefile` at the repo root wraps all common tasks. Run `make` (or `make help`) to see the full list.

**First-time setup** — run jOOQ codegen before the first start:

```bash
make up                       # Start PostgreSQL
./gradlew :app:generateJooq   # Generate jOOQ sources from live schema
make dev-start                # Start backend + frontend in background
```

**Subsequent starts** (schema unchanged):

```bash
make dev-start   # Docker + backend (dev profile) + frontend, all in background
make stop        # Kill backend + frontend + docker
make clean-db    # Wipe the database volume and restart fresh (re-runs all seeds)
make test-fast   # Run integration tests skipping jOOQ codegen
```

See [Makefile targets](#makefile-targets) below for the full reference.

### When to re-run jOOQ codegen

Re-run `./gradlew :app:generateJooq` whenever new Flyway migration files appear (i.e. after pulling commits that add `V*__.sql` files). If you skip this and try to compile, you'll get `package dev.monkeypatch.rctiming.jooq.generated.tables does not exist` errors.

---

## Manual environment setup

If you prefer to run services individually (e.g. in separate terminal tabs):

### 1. Start dev infrastructure

```bash
make up
# or: docker compose up -d
```

This starts:
- **PostgreSQL 16** on `localhost:5432` — database `rctiming_dev`, user/pass `rctiming`
- **Mailpit** on `localhost:1025` (SMTP) / `localhost:8025` (web UI) — catches all outgoing email
- **MinIO** on `localhost:9000` (S3 API) / `localhost:9001` (console) — object storage for club logos

### 2. Backend

```bash
make dev
# or: ./gradlew :app:bootRun --args='--spring.profiles.active=dev'
```

On first run, Flyway applies all migrations and dev seed data automatically:

**Phase 1 (V1–V5):**
- `V1` — users and roles
- `V2` — club profile and governing body affiliations
- `V3` — tracks, decoder loops, lap thresholds
- `V4` — racing classes
- `V5` — race format templates and event classes (JSONB)

**Phase 2 (V6–V14):**
- `V6` — user profile fields (phone, emergency contact, phonetic name)
- `V7` — governing body memberships (unique per user+code)
- `V8` — user class ratings (read-only, set by officials)
- `V9` — cars
- `V10` — car tag categories + values (7 default categories seeded)
- `V11` — transponders (system-wide unique transponder numbers)
- `V12` — events + event classes (JSONB config snapshot)
- `V13` — entries (transponder snapshot, partial unique index)
- `V14` — entry audit log

**Phase 3 (V15–V16):**
- `V15` — event track FK, event class racing_class FK, combined race groups, MinIO logo URL
- `V16` — championships

**Phase 4 (V17–V19):**
- `V17` — rounds, races, race_entries tables; EventClass finals config columns
- `V18` — marshal_adjustments, marshal_absences, marshal_penalties, incident_reports, penalties, unknown_transponder_links
- `V19` — result_snapshots (JSONB positions + lap_history)

**Phase 5 (V21–V22):**
- `V21` — forwarder_token (BCrypt hash, status, timestamps)
- `V22` — unknown_transponder_link (audit of retroactive transponder→entry links)

**Dev seeds (V1000–V1003):**
- `V1000` — racer1/racer2/admin1 accounts
- `V1001/V1002` — racing classes and corrected race format templates
- `V1003` — full race day: 6 racers, RACE_DIRECTOR account, club profile, "Club Championship Round 1" event (IN_PROGRESS), 6 rounds (P1/P2/Q1/Q2/Q3/Final A), races and race entries

The dev profile connects to `localhost:5432/rctiming_dev`. No additional setup needed.

### 3. Frontend

```bash
make ui
# or: cd frontend && npm run dev
```

Vite dev server starts on `http://localhost:5173` with API proxy to `localhost:8080`.

---

## Configuration

### Environment variables

| Variable | Default (dev) | Description |
|----------|---------------|-------------|
| `JWT_SECRET` | base64-encoded dev key | HMAC-SHA256 signing key — **change in production** |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/rctiming_dev` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `rctiming` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `rctiming` | Database password |

The dev JWT secret is baked into `application.yml` as a fallback default — fine for development, must be overridden in production via environment variable.

### Production checklist

- Set `JWT_SECRET` to a cryptographically random 256-bit base64 value
- Set `secure=true` on the refresh cookie (requires HTTPS): override `ResponseCookie.from(...).secure(true)` in `AuthController`
- Set `spring.profiles.active=prod` and configure datasource via env vars
- Do **not** use `ddl-auto=update` or `create-drop` — Flyway manages the schema

---

## Module structure

```
app/src/main/java/dev/monkeypatch/rctiming/
├── api/
│   ├── auth/            # Register, login, refresh, password reset
│   ├── admin/           # Admin CRUD controllers (club, tracks, formats, car tags, entry overrides)
│   │   └── dto/
│   ├── racer/           # Racer-scoped controllers (profile, cars, transponders, entries, events)
│   │   └── dto/
│   └── racecontrol/     # Race lifecycle commands, marshal, referee, result snapshots
│       └── dto/
├── domain/
│   ├── user/            # User entity + RacerProfileService, memberships, class ratings
│   ├── auth/            # RefreshToken, PasswordResetToken, PasswordResetService
│   ├── club/            # ClubProfile, GoverningBodyAffiliation, ClubProfileService
│   ├── track/           # Track, DecoderLoop, TrackLapThreshold, TrackService
│   ├── raceclass/       # RacingClass, RacingClassService
│   ├── format/          # RaceFormatConfig (sealed), RaceFormatTemplate, RaceFormatService
│   ├── car/             # Car, CarTagCategory, CarTagValue, CarService, CarTagCategoryService
│   ├── transponder/     # Transponder, TransponderService
│   ├── event/           # Event, EventStatus, EventRepository
│   ├── entry/           # Entry, EntryStatus, EntryAuditLog, EntryService
│   └── race/            # Race, Round, RaceEntry, RaceStatus, RaceStateMachineService
│                        # MarshalAdjustment, MarshalAbsence, Penalty, IncidentReport
├── query/               # jOOQ read-side (never uses Hibernate)
│   ├── car/             # CarQueryService, CarWithTagsDto
│   ├── event/           # EventScheduleQuery, EventScheduleDto, AdminEventQueryService
│   ├── entry/           # EntryQueryService, RacerEntryHistoryDto
│   └── racecontrol/     # PreRaceReadinessQuery, RunOrderQuery, ResultSnapshotQuery
├── service/             # Pure service layer
│   ├── RoundGeneratorService.java   # Snake-draft heat assignment, round sequencing
│   ├── BumpUpSeedingService.java    # Finals seeding + bump-up promotion chain
│   ├── QualifyingStandingsService.java  # FTQ standings sort
│   └── ResultSnapshotService.java   # Persists final result on FINISHED transition
├── timing/              # In-process live timing
│   ├── LapPassingEvent.java         # Domain event from TCP receiver
│   ├── LiveRaceState.java           # In-memory position model (synchronized)
│   ├── LapTimingService.java        # ConcurrentHashMap<raceId, LiveRaceState>
│   └── LiveTimingHub.java           # STOMP broadcasts on /timing, /state, /marshal
├── security/
│   ├── JwtTokenService.java
│   ├── JwtAuthenticationFilter.java
│   ├── WebSocketJwtChannelInterceptor.java  # JWT on STOMP CONNECT frame
│   └── SecurityConfig.java
└── config/
    ├── JacksonConfig.java           # Primary JSON mapper + yamlObjectMapper bean
    └── websocket/WebSocketConfig.java  # STOMP broker on /ws/timing
```

---

## Local Race Day Program (`localday/` + `frontend-local/`)

An independent application — its own Spring Boot backend and its own React frontend, sharing no code with `app/`/`frontend/` except the pure protocol parser in `decoder-protocol/`. See [architecture.md](architecture.md#local-race-day-program-split-architecture) for the design rationale.

### Quick start

No Docker needed — `localday/` ships an **embedded PostgreSQL** (durable, on-disk, not in-memory) so it can run on a bare laptop with nothing pre-installed beyond Java.

```bash
# Terminal 1 — backend (embedded Postgres starts automatically; Flyway migrates on boot)
./gradlew :localday:bootRun

# Terminal 2 — frontend (Vite proxies /api and /ws to localhost:8081)
cd frontend-local && npm run dev
```

Open **http://localhost:5173**. The officials' login screen walks you through day setup — pre-cache an event from the cloud (requires `app/` running and reachable), or open offline if this instance already has a usable cache from an earlier pre-cache/open.

The embedded Postgres data directory defaults to `./data/localday-pg` under the module's working directory; override with `LOCALDAY_PG_DATA_DIR`. The decoder host/port default to `localhost:5100` (RC-4 text); override with `LOCALDAY_DECODER_HOST` / `LOCALDAY_DECODER_PORT`.

### Exercising race control without physical hardware

Use the same fake decoder simulator the cloud forwarder uses — it's a plain TCP server that doesn't care which client connects to it:

```bash
make simulator   # fake decoder on :5100 — see docs/forwarder.md
```

> **Never run this simulator (or a real decoder) with both `forwarder/` and `localday/` connected to it at once** — see [docs/forwarder.md](forwarder.md#local-race-day-program-exclusivity).

### Module structure

```
localday/src/main/java/dev/monkeypatch/rctiming/localday/
├── auth/            # LocalSessionService, LocalCredential — day-scoped picker+PIN login
├── daylifecycle/    # PreCacheClient, DayLifecycleService — pre-cache pull, open/close
├── domain/          # CachedEntry, CachedScheduleEntry, CachedRaceEntry, LapPassing (JPA)
├── race/            # RaceStateMachineService, RoundGeneratorService, BumpUpSeedingService,
│                    # MarshalAdjustment, RaceResultEntry, RaceControlController
├── checkin/         # CheckInController, TransponderReassignmentController
├── boards/          # BoardController — anonymous now/next + results read API
├── timing/          # LapTimingService, LiveRaceState — in-memory live positions
├── sync/            # SnapshotPushService, SnapshotQueueRepository — periodic cloud push
├── config/          # LocalSecurityConfig, WebSocketConfig
└── testsupport/      # E2eSeedController — e2e-profile-only Playwright fixture seeding
```

### Running its tests

```bash
./gradlew :decoder-protocol:test   # shared protocol parser — no infra needed
./gradlew :localday:test           # unit + integration — embedded Postgres, no Docker
cd frontend-local && npm test      # Vitest
```

See [testing.md](testing.md#local-race-day-program-localday--frontend-local--decoder-protocol) for the full matrix, including the Playwright e2e suite.

### The `e2e` Spring profile

`frontend-local/e2e/` is a Playwright suite that drives a real `:localday` instance end to end. It needs a day already open with a seeded official and race schedule, but the whole point of these tests is proving the system works with **zero cloud connectivity** — so rather than standing up `app/` just to fake a pre-cache, the suite seeds `:localday`'s database directly via a test-only endpoint:

```bash
./gradlew :localday:bootRun --args="--spring.profiles.active=e2e"
```

Under the `e2e` profile only, `E2eSeedController` registers `POST /api/v1/test-support/seed` (permitted unauthenticated in `LocalSecurityConfig`, same way — this endpoint and its permission do not exist at all outside this profile). It's idempotent: each call clears and re-inserts a fresh day-open + official + two-round race schedule, so several spec files can share one running instance. See `localday/src/main/java/.../testsupport/E2eSeedController.java` for exactly what it seeds.

```bash
cd frontend-local
npx playwright install chromium   # once
npm run test:e2e                  # BASE_URL defaults to http://localhost:5173
```

---

## Running tests

### Unit tests (no Docker)

```bash
./gradlew :app:test --tests "dev.monkeypatch.rctiming.domain.*"
./gradlew :app:test --tests "dev.monkeypatch.rctiming.service.*"
```

Covers format config serialization, race state machine transitions, round generator logic, and bump-up seeding.

### Integration tests (requires Docker)

```bash
./gradlew :app:test
```

Uses Testcontainers with `@ServiceConnection` — spins up a real PostgreSQL container. Tests cover all implemented endpoints.

**Phase 1:** `AuthControllerIT`, `SecurityIT`, `ClubControllerIT`, `TrackControllerIT`, `RacingClassControllerIT`, `FormatControllerIT`

**Phase 2:** `CarControllerIT`, `CarTagCategoryIT`, `RacerProfileControllerIT`, `TransponderControllerIT`, `EntryControllerIT`, `EventScheduleControllerIT`, `AdminEntryControllerIT`

**Phase 3:** `AdminEventControllerIT`, `AdminChampionshipControllerIT`, `AdminEntryManagementIT`

**Phase 4:** `RaceStateMachineServiceTest` (unit, 4 tests), `RoundGeneratorServiceTest` (unit, 2 tests), `PreRaceReadinessControllerIT` (4 tests), `RaceControlControllerIT` (7 tests + 1 @Disabled pending Phase 7), `RefereeControllerIT` (5 tests)

---

## Navigating the app

| Path | Access | Description |
|------|--------|-------------|
| `/login` | Public | Login |
| `/register` | Public | Racer self-registration |
| `/events` | Public | Event schedule |
| `/racer/*` | Any authenticated | Profile, cars, transponders, entries |
| `/admin/*` | ADMIN / RACE_DIRECTOR / REFEREE | Admin panel |
| `/admin/race-control` | ADMIN / RACE_DIRECTOR / REFEREE | Select in-progress event for race control |
| `/race-control/event/:id` | ADMIN / RACE_DIRECTOR / REFEREE | Race control cockpit |
| `/race-control/event/:id/referee` | ADMIN / RACE_DIRECTOR / REFEREE | Referee timing view |
| `/race-control/event/:id/results/:raceId` | ADMIN / RACE_DIRECTOR / REFEREE | Print results |

---

## Makefile targets

Run `make help` to see all targets. Quick reference:

| Target | What it does |
|--------|-------------|
| `make dev-start` | Start everything: Docker + backend (dev) + frontend (background) |
| `make stop` | Kill backend and frontend processes |
| `make up` | `docker compose up -d` |
| `make down` | `docker compose down` |
| `make clean-db` | Drop pgdata volume and restart fresh (re-runs all seeds) |
| `make dev` | Backend only, foreground |
| `make ui` | Frontend only, foreground |
| `make build` | Compile backend (no tests, no jOOQ codegen) |
| `make test` | Full integration test suite — app + forwarder |
| `make test-fast` | Tests skipping jOOQ codegen |
| `make forwarder` | Run the forwarder (connect to decoder or simulator) |
| `make simulator` | Run fake decoder in generative mode on :5100 |
| `make simulator-playback` | Replay a .dump file through the fake decoder |
| `make forwarder-build` | Compile forwarder module only |
| `make forwarder-test` | Run forwarder unit + integration tests |
| `make ui-build` | TypeScript check + production bundle |
| `make ui-lint` | ESLint |
| `make clean` | Stop everything, `./gradlew clean`, remove `frontend/dist` |

## Useful commands

```bash
# Compile check only
./gradlew :app:compileJava

# View Flyway migration state
./gradlew :app:flywayInfo

# Generate jOOQ sources (needed after schema changes)
./gradlew :app:generateJooq
```

## Email in development

Password reset emails are caught by Mailpit. Open `http://localhost:8025` to view them. No real email is ever sent in dev — all SMTP traffic goes to `localhost:1025`.

---

## Developer workflow

### Picking up a piece of work

1. Check [GitHub Issues](https://github.com/oggthemiffed/RCTimingControl/issues) for the next task
2. Read `CLAUDE.md` for architecture context before starting — it's the primary reference for any AI-assisted session
3. Read `docs/PROJECT.md` for requirements and `docs/REQUIREMENTS.md` for the full requirement list

### Making a change

1. Start the dev environment: `make dev-start`
2. Implement the change, following the patterns in `CLAUDE.md` (domain module for writes, jOOQ query module for reads, no cross-module Hibernate)
3. If adding a database column or table, add a Flyway migration (`V{next}__description.sql`) and re-run `./gradlew :app:generateJooq`
4. Write or update tests — backend integration tests in `app/src/test/`, frontend unit tests in `frontend/src/` (or `localday/src/test/` and `frontend-local/src/` for the Local Race Day Program)
5. Run `make test-fast` (backend) and `npm test` in `frontend/` before pushing — add `./gradlew :localday:test :decoder-protocol:test` and `npm test` in `frontend-local/` if you touched either of those modules

### Schema changes

Any new `V*__.sql` migration file requires jOOQ codegen to be re-run before the code will compile:

```bash
make up
./gradlew :app:generateJooq
```

Flyway applies migrations automatically on backend startup — you do not need to run them manually.

### Submitting a PR

```bash
gh pr create
```

Reference the GitHub Issue number in the PR description (`Closes #N`). PRs should include passing tests — `make test` runs the full suite including forwarder tests.

### Working with Claude Code

Open the project in Claude Code from the repo root. `CLAUDE.md` is loaded automatically and gives Claude the full architecture context. For a new feature or bug fix, describe the GitHub Issue and Claude will use the existing patterns.

Key docs to reference in a session:
- `CLAUDE.md` — stack, architecture, module boundaries, rules
- `docs/PROJECT.md` — what the system is and its requirements
- `docs/REQUIREMENTS.md` — full requirement list (AUTH, RACER, EVENT, etc.)
- `docs/architecture.md` — deeper architecture notes, including the Local Race Day Program split
- `docs/plans/2026-08-06-001-feat-offline-race-day-resilience-split-plan.md` — the Local Race Day Program's requirements, decisions, and implementation units
- `.planning/phases/` — historical decision log per phase (useful if you hit an unexpected behaviour)
