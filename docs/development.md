# Development Guide

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 (LTS) |
| Docker | 24+ (optional: only for Piper announcer voices and the trial stack) |
| Node.js | 20+ |
| Gradle | via wrapper (`./gradlew`) |

## Quick start (Makefile)

A `Makefile` at the repo root wraps all common tasks. Run `make` (or `make help`) to see the full list.

No database server to install: the backend keeps its data in a SQLite file under `app/data/db`, created and migrated on first start.

```bash
make dev-start   # Backend (dev profile) + frontend in background, plus Piper if Docker is available
make stop        # Kill backend + frontend (and Piper)
make clean-db    # Delete the dev database; the next start migrates and seeds a fresh one
make test-fast   # Run integration tests skipping jOOQ codegen
```

See [Makefile targets](#makefile-targets) below for the full reference.

### When to re-run jOOQ codegen

The generated jOOQ sources are committed (`app/src/generated/jooq`). Re-run `./gradlew :app:generateJooq` after changing or adding a migration. Codegen migrates a throwaway SQLite file (`app/build/jooq-codegen/schema.db`) and reads the schema from it, so it needs no Docker.

---

## Manual environment setup

If you prefer to run services individually (e.g. in separate terminal tabs):

### 1. Optional: start Piper

```bash
make up
# or: docker compose up -d
```

This starts **Piper** (text to speech) on `localhost:10200` for announcer voices. Without it the app runs normally with announcements off.

Club logos and TTS clips are stored on local disk under `storage.local-path` (defaults to `./data/uploads`, overridable via `STORAGE_LOCAL_PATH`) and served back by the app itself at `/storage/**` — no object-storage server to start.

### 2. Backend

```bash
make dev
# or: ./gradlew :app:bootRun --args='--spring.profiles.active=dev'
```

On first run, Flyway creates the database file and applies the baseline migrations and the dev seed data:

**Baseline (`db/migration/sqlite/`, V1–V6), grouped by area:**
- `V1` — users and roles, refresh tokens, club profile, governing bodies
- `V2` — tracks, decoder loops, lap thresholds, racing classes, format templates, events and event classes
- `V3` — competitors, entries, entry audit log, RaceHub class mappings
- `V4` — rounds, races, race entries, marshal adjustments/absences/penalties, penalties, incident reports, unknown transponder links
- `V5` — result snapshots and championships
- `V6` — practice sessions and laps, profanity blocklist

The baseline replaced the PostgreSQL history (V1–V36) in #26; `V1` lists the type conventions.

**Dev seeds (`db/seed/sqlite/`, V1000+):**
- `V1001` — tracks, racing classes and race format templates
- `V1002` — admin1 and race director accounts, club profile, "Club Championship Round 1" event (IN_PROGRESS) with 6 competitors and entries (transponders 101–106), 6 rounds (P1/P2/Q1/Q2/Q3/Final A), races and race entries

The dev profile keeps the database in `app/data/db` (relative to `app/`, gitignored). `make clean-db` deletes it.

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
| `RCTIMING_DATA_DIR` | per-user app-data folder (dev profile: `app/data/db`) | Folder holding the SQLite database file `rctiming.db` |

Outside the dev profile the database lives in the user's app-data folder: `%LOCALAPPDATA%\RCTimingControl` on Windows, `~/Library/Application Support/RCTimingControl` on macOS, and `$XDG_DATA_HOME/rctimingcontrol` (or `~/.local/share/rctimingcontrol`) on Linux.

The dev JWT secret is baked into `application.yml` as a fallback default — fine for development, must be overridden in production via environment variable.

### Backups

The app copies its database into a backup folder when an event is marked completed (the race day closes) and every night, and keeps the newest copies. Admins can see them and take one now under **Admin → Backups**. Backing up is safe while a race is running. The database makes the copy in one read transaction (`VACUUM INTO` on SQLite), and laps keep committing meanwhile.

| Setting | Default | Meaning |
|---------|---------|---------|
| `RCTIMING_BACKUP_DIRECTORY` (`rctiming.backup.directory`) | `backups` inside the data directory | Where backups go; may be a USB stick or network share |
| `RCTIMING_BACKUP_KEEP` (`rctiming.backup.keep`) | `14` | How many backups to keep; older ones are deleted |
| `RCTIMING_BACKUP_NIGHTLY_CRON` (`rctiming.backup.nightly-cron`) | `0 0 2 * * *` | When the nightly backup runs (server time); `-` turns it off |

To restore, stop the app, then run:

```bash
java -jar app.jar restore /media/usb/rctiming/rctiming-20261004-220000-day-close.db
```

It finds the data directory the way the app does (`RCTIMING_DATA_DIR`, `--rctiming.database.data-directory=...`, or the default folder), checks the backup is sound, and moves the current database aside as `rctiming.db.before-restore-<time>`. Then it puts the backup in its place. It refuses while the app has the database open. When the app next starts, it migrates the restored database if it came from an older version. With Docker, stop the stack and run `docker compose run --rm app restore /app/data/db/backups/<file>`.

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
│   ├── auth/            # Officials' login and refresh
│   ├── admin/           # Admin CRUD controllers (club, tracks, formats, entries, competitors, RaceHub import)
│   │   └── dto/
│   ├── pub/             # Public read endpoints (event schedule, results, championships)
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

### Unit tests

```bash
./gradlew :app:test --tests "dev.monkeypatch.rctiming.domain.*"
./gradlew :app:test --tests "dev.monkeypatch.rctiming.service.*"
```

Covers format config serialization, race state machine transitions, round generator logic, and bump-up seeding.

### Integration tests (no Docker)

```bash
./gradlew :app:test
```

Each test run creates a temporary SQLite database, shared by all integration test classes (one Spring context). `SqliteConcurrencyIT` writes laps while readers poll, and `CrashRecoveryIT` kills the app mid-session and checks no committed lap is lost.

**Phase 1:** `AuthControllerIT`, `SecurityIT`, `ClubControllerIT`, `TrackControllerIT`, `RacingClassControllerIT`, `FormatControllerIT`

**Phase 2:** `CarControllerIT`, `CarTagCategoryIT`, `RacerProfileControllerIT`, `TransponderControllerIT`, `EntryControllerIT`, `EventScheduleControllerIT`, `AdminEntryControllerIT`

**Phase 3:** `AdminEventControllerIT`, `AdminChampionshipControllerIT`, `AdminEntryManagementIT`

**Phase 4:** `RaceStateMachineServiceTest` (unit, 4 tests), `RoundGeneratorServiceTest` (unit, 2 tests), `PreRaceReadinessControllerIT` (4 tests), `RaceControlControllerIT` (7 tests + 1 @Disabled pending Phase 7), `RefereeControllerIT` (5 tests)

---

## Navigating the app

| Path | Access | Description |
|------|--------|-------------|
| `/login` | Public | Officials' login |
| `/events` | Public | Event schedule |
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
| `make dev-start` | Start everything: backend (dev) + frontend (background), plus Piper if Docker is available |
| `make stop` | Kill backend and frontend processes |
| `make up` | Start Piper (`docker compose up -d`); skipped without Docker |
| `make down` | `docker compose down` |
| `make clean-db` | Delete the dev SQLite database (re-runs all seeds on next start) |
| `make dev` | Backend only, foreground |
| `make ui` | Frontend only, foreground |
| `make build` | Compile backend (no tests, no jOOQ codegen) |
| `make test` | Full integration test suite — app + decoder simulator |
| `make test-fast` | Tests skipping jOOQ codegen |
| `make simulator` | Run fake decoder in generative mode on :5100 |
| `make simulator-playback` | Replay a .dump file through the fake decoder |
| `make ui-build` | TypeScript check + production bundle |
| `make ui-lint` | ESLint |
| `make clean` | Stop everything, `./gradlew clean`, remove `frontend/dist` |

## Useful commands

```bash
# Compile check only
./gradlew :app:compileJava

# Generate jOOQ sources (needed after schema changes)
./gradlew :app:generateJooq
```


## Developer workflow

### Picking up a piece of work

1. Check [GitHub Issues](https://github.com/oggthemiffed/RCTimingControl/issues) for the next task
2. Read `CLAUDE.md` for architecture context before starting — it's the primary reference for any AI-assisted session
3. Read `docs/PROJECT.md` for requirements and `docs/REQUIREMENTS.md` for the full requirement list

### Making a change

1. Start the dev environment: `make dev-start`
2. Implement the change, following the patterns in `CLAUDE.md` (domain module for writes, jOOQ query module for reads, no cross-module Hibernate)
3. If adding a database column or table, add a Flyway migration (`V{next}__description.sql`) and re-run `./gradlew :app:generateJooq`
4. Write or update tests — backend integration tests in `app/src/test/`, frontend unit tests in `frontend/src/`
5. Run `make test-fast` (backend) and `npm test` in `frontend/` before pushing — add `./gradlew :decoder-protocol:test` if you touched the protocol parser

### Schema changes

Any new `V*__.sql` migration file requires jOOQ codegen to be re-run before the code will compile:

```bash
./gradlew :app:generateJooq
```

Flyway applies migrations automatically on backend startup — you do not need to run them manually.

Migrations live in one folder per database: `app/src/main/resources/db/migration/{vendor}/` (dev seeds in `db/seed/{vendor}/`, the trial demo club in `db/demo/{vendor}/`, test data in `app/src/test/resources/db/testdata/{vendor}/`). Until the first release on SQLite, the baseline may still be edited in place; after that, every change is a new migration.

### Keeping the database swappable

The database is chosen in one place. `rctiming.database.vendor` picks it, and `DatabaseConfig` (in `persistence/`) applies it to Flyway, Hibernate and jOOQ. The vendor-specific values themselves live in `persistence/vendor/DatabaseVendor`: the JDBC URL, connection settings (for SQLite: WAL, `synchronous=NORMAL`, foreign keys on, a 5 second busy timeout), how many write connections it allows, and the dialects.

The app opens two pools on the database: a write pool (one connection for SQLite, used by JPA, Flyway and Spring transactions) and a read pool of `rctiming.database.read-connections` (default 4) whose connections refuse writes. jOOQ queries outside a transaction go to the read pool; inside a transaction they share the writer.

Column types the databases disagree on are mapped by converters in `persistence/convert/`, not by the schema: timestamps are `Instant` stored as UTC microseconds, date-only values are ISO text, and JSON columns are text read through `JsonTextConverter` subclasses.

Everywhere else the code stays database-neutral:

- Read queries use the jOOQ DSL only: no `DSL.sql(...)`, `field("...")` or other plain-SQL strings.
- Repositories use derived queries or JPQL, never `nativeQuery = true`, and entities carry no `columnDefinition`.
- A vendor-specific statement that can't be avoided goes behind an interface in `persistence/vendor/`, with one implementation per database.

`PersistencePortabilityTest` scans the main sources, and the compiled classes for any jOOQ method marked `@PlainSQL` (so SQL in a variable is caught too), and fails the build if any of this slips in.

Moving to another database means:

1. Add a constant to `DatabaseVendor` with its migrations folder name, jOOQ and Hibernate dialects, JDBC URL, connection settings and write-connection limit.
2. Add baseline migrations under `db/migration/{vendor}/` (and the dev seeds and test data folders).
3. Add the JDBC driver and Flyway database support to `app/build.gradle.kts`.
4. Set `rctiming.database.vendor`, and point the jOOQ codegen in `app/build.gradle.kts` at the new migrations.
5. Run the whole test suite against it.

### Submitting a PR

```bash
gh pr create
```

Reference the GitHub Issue number in the PR description (`Closes #N`). PRs should include passing tests — `make test` runs the full suite, including the decoder simulator tests.

### Working with Claude Code

Open the project in Claude Code from the repo root. `CLAUDE.md` is loaded automatically and gives Claude the full architecture context. For a new feature or bug fix, describe the GitHub Issue and Claude will use the existing patterns.

Key docs to reference in a session:
- `CLAUDE.md` — stack, architecture, module boundaries, rules
- `docs/PROJECT.md` — what the system is and its requirements
- `docs/REQUIREMENTS.md` — full requirement list (AUTH, RACER, EVENT, etc.)
- `docs/architecture.md` — deeper architecture notes
- `.planning/phases/` — historical decision log per phase (useful if you hit an unexpected behaviour)
