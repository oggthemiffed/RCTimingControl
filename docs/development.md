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
| `make test` | Full integration test suite |
| `make test-fast` | Tests skipping jOOQ codegen |
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
