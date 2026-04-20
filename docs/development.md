# Development Guide

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 (LTS) |
| Docker | 24+ |
| Node.js | 20+ |
| Gradle | via wrapper (`./gradlew`) |

## Environment setup

### 1. Start dev infrastructure

```bash
docker compose up -d
```

This starts:
- **PostgreSQL 16** on `localhost:5432` — database `rctiming_dev`, user/pass `rctiming`
- **Mailpit** on `localhost:1025` (SMTP) / `localhost:8025` (web UI) — catches all outgoing email

### 2. Backend

```bash
./gradlew :app:bootRun --args='--spring.profiles.active=dev'
```

On first run, Flyway applies all migrations automatically:

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

The dev profile connects to `localhost:5432/rctiming_dev`. No additional setup needed.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Vite dev server starts on `http://localhost:5173` with API proxy to `localhost:8080`.

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

## Module structure

```
app/src/main/java/dev/monkeypatch/rctiming/
├── api/
│   ├── auth/            # Register, login, refresh, password reset
│   ├── admin/           # Admin CRUD controllers (club, tracks, formats, car tags, entry overrides)
│   │   └── dto/
│   ├── racer/           # Racer-scoped controllers (profile, cars, transponders, entries, events)
│   │   └── dto/
│   └── GlobalExceptionHandler.java
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
│   └── entry/           # Entry, EntryStatus, EntryAuditLog, EntryService
├── query/               # jOOQ read-side (never uses Hibernate)
│   ├── car/             # CarQueryService, CarWithTagsDto
│   ├── event/           # EventScheduleQuery, EventScheduleDto
│   └── entry/           # EntryQueryService, RacerEntryHistoryDto
├── security/
│   ├── JwtTokenService.java
│   ├── JwtAuthenticationFilter.java
│   └── SecurityConfig.java
└── config/
    └── JacksonConfig.java   # Primary JSON mapper + yamlObjectMapper bean
```

## Running tests

### Unit tests (no Docker)

```bash
./gradlew :app:test --tests "dev.monkeypatch.rctiming.domain.*"
```

Covers format config serialization round-trips and service merge logic.

### Integration tests (requires Docker)

```bash
./gradlew :app:test
```

Uses Testcontainers with `@ServiceConnection` — spins up a real PostgreSQL container. Tests cover all implemented endpoints.

**Phase 1 test classes:**
- `AuthControllerIT` — register, login, refresh, password reset
- `SecurityIT` — unauthenticated access, role enforcement
- `ClubControllerIT` — club profile and affiliation CRUD
- `TrackControllerIT` — track + nested loop/threshold management
- `RacingClassControllerIT` — class CRUD
- `FormatControllerIT` — format CRUD, JSON/YAML export+import

**Phase 2 test classes:**
- `CarControllerIT` — car CRUD, ownership isolation, tag management (8 tests)
- `CarTagCategoryIT` — admin tag category CRUD, role enforcement (4 tests)
- `RacerProfileControllerIT` — profile get/patch, membership add/remove/duplicate (8 tests)
- `TransponderControllerIT` — transponder CRUD, system-wide uniqueness, cross-user isolation (6 tests)
- `EntryControllerIT` — submit, withdraw, membership block, duplicate transponder warning, ownership (12 tests)
- `EventScheduleControllerIT` — public schedule access, anonymous access (3 tests)
- `AdminEntryControllerIT` — transponder swap, membership override, audit log writes (4 tests)

See [Testing guide](testing.md) for the full manual UAT checklist.

## Useful commands

```bash
# Build without tests
./gradlew :app:build -x test

# Compile check only
./gradlew :app:compileJava

# View Flyway migration state
./gradlew :app:flywayInfo

# Stop dev infrastructure
docker compose down

# Wipe database (destructive)
docker compose down -v
```

## Email in development

Password reset emails are caught by Mailpit. Open `http://localhost:8025` to view them. No real email is ever sent in dev — all SMTP traffic goes to `localhost:1025`.
