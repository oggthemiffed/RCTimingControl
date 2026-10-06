# Phase 1: Domain Foundation - Research

**Researched:** 2026-04-16
**Domain:** Spring Boot 3.4 modular monolith — JPA/jOOQ/Flyway persistence, JWT auth, JSONB config
**Confidence:** HIGH (stack fully verified against Maven Central and Spring BOM)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Gradle Kotlin DSL (not Maven)
- **D-02:** Multi-module project from day one: root project includes `:app` (Spring Boot main application) and `:forwarder` (stub until Phase 5). `settings.gradle.kts` at repo root includes both modules.
- **D-03:** Java package root: `dev.monkeypatch.rctiming`
- **D-04:** Internal Spring Boot packages organised by layer: `domain/`, `api/`, `query/`, `security/`, `config/`
- **D-05:** React frontend lives at `frontend/` in the repo root (Vite project, served independently)
- **D-06:** Local development Postgres via Docker Compose at repo root. `docker-compose.yml` spins up `postgres:16`. Integration tests use Testcontainers (own isolated Postgres per test run).
- **D-07:** Frontend scope in Phase 1: auth screens only (register, login, password reset). Full React + Vite + Tailwind CSS + shadcn/ui scaffold created with routing.
- **D-08:** Backend scope covers all Phase 1 requirements (AUTH, CLUB, TRACK, RACECLASS, FORMAT). All APIs verified by Spring Boot integration tests (Testcontainers).
- **D-09:** Password reset email: `Spring JavaMailSender` with SMTP transport. Dev/test uses MailPit via Docker Compose.
- **D-10:** Three format types as a **sealed interface + Jackson polymorphism** (`TIMED`, `BUMP_UP`, `POINTS_FINALS`). Each subtype is a Java `record`.
- **D-11:** Hibernate JSONB storage via **Hypersistence Utils** (`@Type(JsonType.class)` for Hibernate 6).
- **D-12:** Format config import/export supports **both JSON and YAML** (`jackson-dataformat-yaml`). Auto-detect from `Content-Type`.
- **D-13:** FORMAT-07 override storage: **snapshot + second JSONB override column** on `EventClass`. Merge at read time.

### Claude's Discretion

- Specific Flyway migration numbering scheme
- REST API URL structure (e.g. `/api/v1/` prefix or not)
- Exact `application.yml` profile structure for dev/prod/test

### Deferred Ideas (OUT OF SCOPE)

- Full admin config UI (club, track, format template CRUD forms) — Phase 3
- Forwarder implementation — Phase 5
- Multi-decoder operation — post-v1
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | Racer can self-register with email and password | Spring Security + BCrypt password hashing; `POST /api/v1/auth/register` endpoint; `User` entity with `RACER` role |
| AUTH-02 | Racer can log in and remain logged in across browser sessions | JJWT 0.12.x access token (15 min) + HttpOnly refresh token cookie (7 days); `POST /api/v1/auth/login` and `/api/v1/auth/refresh` endpoints |
| AUTH-03 | Racer can reset password via email link | Spring JavaMailSender; signed time-limited reset token stored in `password_reset_tokens` table; MailPit for dev |
| AUTH-04 | Staff users can log in with elevated privileges; access gated by role | Same JWT auth as AUTH-02; `OncePerRequestFilter` extracts roles from JWT claims; `@PreAuthorize` on controller methods |
| AUTH-05 | Stackable roles (`ADMIN`, `RACE_DIRECTOR`, `REFEREE`); a user may hold any combination | `user_roles` join table; Spring Security `GrantedAuthority` list from JWT claims |
| CLUB-01 | Admin can configure zero or more governing body affiliations | `GoverningBodyAffiliation` entity; `POST/PUT/DELETE /api/v1/admin/club/affiliations`; `membershipRequired` boolean flag |
| CLUB-02 | Admin can configure club profile (name, contact, GPS, timezone, logo SVG/PNG) | `ClubProfile` entity (singleton row); logo stored as `BYTEA` or external file reference; IANA timezone validated |
| TRACK-01 | Admin can define and manage tracks (name, venue notes, optional length) | `Track` entity; standard CRUD |
| TRACK-02 | Configurable minimum lap time per racing class (track-wide default + class override) | `TrackLapThreshold` entity with `track_id`, `racing_class_id` (nullable = default), `min_lap_ms` |
| TRACK-03 | Configurable maximum last-lap time per racing class | Same `TrackLapThreshold` entity; add `max_last_lap_ms` column |
| TRACK-04 | Decoder loop configuration per track | `DecoderLoop` entity: `loop_id`, `display_name`, `type` (enum: `FINISH_LINE`, `CHICANE`, `OTHER`), `is_scoring_loop` flag; multi-loop model accommodated but multi-decoder deferred |
| RACECLASS-01 | Admin can define and manage racing classes (name, description) | `RacingClass` entity; standard CRUD |
| FORMAT-01 | Timed race config (duration, start type, qualifying type) | `TimedRaceConfig` record in sealed interface |
| FORMAT-02 | Bump-up finals config (heats, duration, best heats, grid size, bump spots) | `BumpUpConfig` record |
| FORMAT-04 | Points finals config (heats, finals count, duration) | `PointsFinalsConfig` record |
| FORMAT-05 | Type-discriminated format config; only valid fields accepted | Sealed interface + `@JsonTypeInfo` discriminator; `@Valid` on API request bodies |
| FORMAT-06 | Assignment to event class snapshots the template | `config_snapshot JSONB` column on `EventClass`; copy-on-assign in domain service |
| FORMAT-07 | Override individual fields at event-class level | `config_override JSONB` column on `EventClass`; merge at read time |
| FORMAT-08 | Start type configurable per class per phase: `STAGGER`, `GRID`, `ROLLING` | `StartType` enum field in each config record |
| FORMAT-09 | Qualifying type: `FTQ`, `ROUND_BY_ROUND`, `FASTEST_LAP`, `CONSECUTIVE_LAPS` | `QualifyingType` enum field in config records that support qualifying |
| FORMAT-10 | Gap between successive races configurable per class | `racePaddingMinutes` int field in base config |
| FORMAT-11 | Stagger start interval configurable (default 1 second) | `staggerIntervalSeconds` int field in config |
| FORMAT-12 | Bump-up: auto-calculate finals and grid assignments | Domain service calculation on entry count; data stored but calculation is on-demand |
| FORMAT-13 | Bump-up: class-wide finishing order for championship points | Affects result snapshot shape; data model records enough to reconstruct this |
| FORMAT-14 | Format configs can be exported as JSON/YAML and re-imported with validation | `GET /api/v1/admin/formats/{id}/export`, `POST /api/v1/admin/formats/import`; `jackson-dataformat-yaml`; validate via `@Valid` on the sealed interface |
</phase_requirements>

---

## Summary

Phase 1 is a greenfield Spring Boot 3.4.x multi-module Gradle project that must deliver: a complete Flyway-managed PostgreSQL schema, JPA entities for all domain objects, a stateless JWT auth system with access token + refresh cookie, admin REST APIs for club/track/raceclass/format management, and JSONB-stored race format configuration with a sealed-interface Java model. The React frontend scaffold (auth screens only) rounds out the phase.

The stack is fully locked by user decisions. All version numbers have been verified against Maven Central and the Spring Boot 3.4.7 BOM — no training-data version drift. One **critical infrastructure finding** is that the system's globally-installed Gradle (9.3.1) is incompatible with Spring Boot 3.4.x. The project must include a Gradle wrapper pinned to 8.14.x. Docker and Docker Compose are not installed on the development machine, meaning the Docker Compose dev database cannot start locally — integration tests via Testcontainers will work as long as the Docker daemon is available at runtime (a CI/build environment concern rather than a code concern).

The highest complexity area is the jOOQ code generation pipeline: jOOQ DSL must be generated from the live schema (after Flyway migrations run), which requires either a Testcontainers-based generation run at build time or a pre-migrated local database. The Flyway→jOOQ→compile dependency chain needs to be wired in Gradle carefully.

**Primary recommendation:** Use Spring Boot 3.4.7, pin Gradle wrapper to 8.14.x, use `@ServiceConnection` Testcontainers pattern for integration tests, and drive jOOQ codegen off Flyway migrations using the `nu.studer.jooq` Gradle plugin with a Testcontainers JDBC URL.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| User registration / login / password reset | API / Backend | — | Stateless auth; credentials never reach browser except in POST body |
| JWT token issuance and validation | API / Backend (Security filter) | — | Token signed on server; client stores in memory / HttpOnly cookie |
| Refresh token rotation | API / Backend | Browser (cookie storage) | Server mints and rotates; browser holds opaque HttpOnly cookie |
| Club / track / raceclass / format CRUD | API / Backend | — | Write-side business logic, Hibernate entities |
| JSONB format config storage and merge | API / Backend (Domain service) | Database / Storage | Merge logic is Java; storage is PostgreSQL JSONB |
| Format config export/import (JSON + YAML) | API / Backend | — | Jackson serialisation; Content-Type detection on request |
| React auth screens (login, register, reset) | Browser / Client | — | React Hook Form + Zod validation; calls backend API |
| Auth token storage in browser | Browser / Client | — | Access token: in-memory React state; refresh token: HttpOnly cookie (set by backend) |
| Route protection (ProtectedRoute) | Browser / Client | — | Client-side guard only; backend enforces independently |
| Schema migrations | Database / Storage | — | Flyway owns the schema; never Hibernate DDL |
| Type-safe read queries | Database / Storage (jOOQ) | API / Backend | jOOQ query module; no Hibernate involvement on read side |

---

## Standard Stack

### Core — Backend

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.4.7 | Application framework | Current 3.4.x GA; BOM manages all sub-versions |
| Java | 21 (Temurin LTS) | Runtime | Current LTS; Spring Boot 3.x preferred baseline; virtual threads |
| Gradle (wrapper) | 8.14.x | Build tool | Spring Boot 3.4 requires Gradle 7.6.4+ or 8.x — NOT Gradle 9 |
| spring-boot-starter-web | (BOM) | REST controllers | Standard MVC stack |
| spring-boot-starter-data-jpa | (BOM) | Hibernate 6.6.18 entities + repositories | Write-side only |
| spring-boot-starter-security | (BOM) | Security filter chain | JWT filter, role enforcement |
| spring-boot-starter-validation | (BOM) | Bean Validation / `@Valid` | Request body validation |
| spring-boot-starter-mail | (BOM) | JavaMailSender | Password reset emails |
| spring-boot-starter-actuator | (BOM) | Health checks | Readiness/liveness probes |
| JJWT (`jjwt-api` + `jjwt-impl` + `jjwt-jackson`) | 0.12.6 | JWT creation and parsing | Current 0.12.x GA; cleaner builder API than 0.11 |
| Flyway (`flyway-core` + `flyway-database-postgresql`) | 10.20.1 (BOM) | Schema migrations | SQL-first; matches BOM version |
| jOOQ | 3.19.24 (BOM) | Type-safe read queries | BOM-managed; matches project decision 3.19.x |
| Hypersistence Utils | 3.9.11 (`-hibernate-63` artifact) | JSONB `@Type(JsonType.class)` mapping | Supports Hibernate 6.3–6.6; Spring Boot 3.4 ships 6.6.18 |
| `jackson-dataformat-yaml` | 2.19.0 (BOM-managed via Jackson) | YAML import of format configs | Adds YAML support to existing Jackson ObjectMapper |
| PostgreSQL JDBC driver | (BOM) | Database connectivity | `org.postgresql:postgresql` |
| Testcontainers (`testcontainers` + `postgresql`) | 1.20.6 (BOM) | Isolated PostgreSQL for integration tests | Spring Boot BOM-managed version |

### Core — Frontend

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19.2.5 (latest `^19`) | UI framework | React 19 is current GA; `^19` is correct constraint |
| React DOM | 19.2.5 | DOM renderer | Match React version |
| Vite | 8.0.8 (latest `^8`) | Build tool + dev server | Vite 8 is current GA; replaces Create React App |
| TypeScript | 6.0.2 (latest `^6`) | Type safety | Current TS version |
| Tailwind CSS | 4.2.2 (latest `^4`) | Utility-first styling | Tailwind v4 is current GA; shadcn preset determines exact config |
| shadcn/ui | no version pin (copy-paste model) | Component primitives | Init via `npx shadcn@latest init --preset b1GyYWRfE`; generates into `src/components/ui/` |
| `@tanstack/react-query` | 5.99.0 (`^5`) | Server state management | v5 is locked decision; no v4 |
| React Hook Form | 7.72.1 (`^7`) | Form state + validation | v7 required by UI-SPEC |
| Zod | 4.3.6 (`^3`) | Schema validation | Use with RHF resolver |
| React Router DOM | 7.14.1 (`^7`) | Client routing | v7 — use `createBrowserRouter` Data Router API |
| Axios | 1.15.0 (`^1`) | HTTP client | Interceptors needed for 401 retry and token attachment |
| `@stomp/stompjs` | 7.3.0 (`^7`) | WebSocket STOMP client | Installed now; wired in Phase 5 |
| lucide-react | 0.503.0 (`^0`) | Icon set | Used by shadcn; `Loader2` for spinners |

> **Note on React version:** npm registry shows React 19.2.5 as latest as of research date. The UI-SPEC says `^18` but React 19 is current GA. The planner should confirm whether to pin `^18` for stability or use `^19`. Both work with React Router 7 and TanStack Query 5. [ASSUMED: React 19 breaking changes are acceptable; verify if shadcn preset is tested against React 19]

### Supporting — Build Infrastructure

| Library | Purpose | Version |
|---------|---------|---------|
| `nu.studer.jooq` Gradle plugin | Drives jOOQ code generation from Flyway-migrated schema | 9.x (verify latest at plugins.gradle.org) |
| `org.flywaydb.flyway` Gradle plugin | Runs Flyway migrations during jOOQ codegen build step | matches Flyway 10.x |
| Spring Boot Gradle plugin (`org.springframework.boot`) | Bootable JAR, dependency management | 3.4.7 |
| `io.spring.dependency-management` Gradle plugin | Imports Spring Boot BOM | 1.1.x |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Hypersistence Utils `JsonType` | Manual `AttributeConverter<RaceFormatConfig, String>` | Converter is simpler but loses JSONB type (stores as `text`); PostgreSQL JSONB operators unavailable |
| JJWT 0.12.x | Spring Security OAuth2 Resource Server (built-in JWT support) | OAuth2RS is simpler for RS256 but introduces needless complexity for a self-contained auth system; JJWT gives full control over token structure |
| Gradle wrapper 8.14.x | Use system Gradle 9.3.1 | Spring Boot 3.4.x plugin is not compatible with Gradle 9 (issue #43573); must use wrapper |
| `nu.studer.jooq` plugin | Manual jOOQ code generation (CLI) | CLI generation is harder to integrate with Testcontainers; plugin handles the Flyway→codegen dependency chain cleanly |

**Installation (Gradle KTS — `:app` module):**
```kotlin
// build.gradle.kts (:app)
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jooq:jooq")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.9.11")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}
```

**Version verification (confirmed 2026-04-16):**
```
Spring Boot BOM 3.4.7:  hibernate=6.6.18.Final, jooq=3.19.24, flyway=10.20.1, testcontainers=1.20.6
JJWT latest:            0.12.6 (verified Maven Central)
Hypersistence Utils:    3.9.11 hibernate-63 artifact (Hibernate 6.3–6.6 range)
React:                  19.2.5 (latest)
Vite:                   8.0.8 (latest)
@tanstack/react-query:  5.99.0 (latest v5)
React Router DOM:       7.14.1 (latest v7)
Tailwind CSS:           4.2.2 (latest v4)
Zod:                    4.3.6 (latest)
```
[VERIFIED: npm registry, Maven Central search, Spring Boot BOM POM file]

---

## Architecture Patterns

### System Architecture Diagram

```
Browser (React SPA)
  │  POST /api/v1/auth/login
  │  POST /api/v1/auth/refresh  (uses HttpOnly cookie)
  │  POST /api/v1/auth/register
  │  POST /api/v1/auth/password-reset/request
  │  POST /api/v1/auth/password-reset/confirm
  │  GET/POST/PUT/DELETE /api/v1/admin/**  (Bearer token)
  ▼
Spring Boot :app
  ├── SecurityFilterChain
  │     JwtAuthenticationFilter (OncePerRequestFilter)
  │       └── validates Bearer token, loads UserDetails, populates SecurityContext
  ├── api/auth/AuthController
  │     ├── register → UserService.createRacer() → BCrypt hash → save User
  │     ├── login    → authenticate → JJWT issue access token → set refresh HttpOnly cookie
  │     ├── refresh  → validate refresh token → rotate → issue new access token
  │     └── password-reset → PasswordResetService (JavaMailSender → MailPit in dev)
  ├── api/admin/**Controllers  (ADMIN role required)
  │     ├── ClubProfileController     → ClubProfileService   → ClubProfile entity
  │     ├── TrackController           → TrackService         → Track + DecoderLoop + TrackLapThreshold entities
  │     ├── RacingClassController     → RacingClassService   → RacingClass entity
  │     └── RaceFormatController      → RaceFormatService    → RaceFormatTemplate entity (JSONB config)
  ├── domain/  (JPA entities, repositories — Hibernate 6)
  │     User, Role, PasswordResetToken
  │     ClubProfile, GoverningBodyAffiliation
  │     Track, DecoderLoop, TrackLapThreshold
  │     RacingClass
  │     RaceFormatTemplate (config: JSONB via Hypersistence JsonType)
  │     EventClass (config_snapshot + config_override JSONB)
  ├── query/   (jOOQ read queries — generated DSL)
  │     [minimal in Phase 1 — primarily write-side; jOOQ DSL generated but not heavily used until Phase 3+]
  └── Flyway  →  PostgreSQL 16
        V1__create_users.sql
        V2__create_club.sql
        V3__create_tracks.sql
        V4__create_racing_classes.sql
        V5__create_race_formats.sql

JavaMailSender ──────────────────→ MailPit (Docker Compose, dev only)
                                  SMTP relay (production)
```

### Recommended Project Structure

```
/ (repo root)
├── settings.gradle.kts          # includes(":app", ":forwarder")
├── build.gradle.kts             # root — common plugin versions, allprojects config
├── gradle/wrapper/
│   └── gradle-wrapper.properties # distributionUrl = gradle-8.14.x-bin.zip  ← CRITICAL
├── docker-compose.yml           # postgres:16 + mailpit
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/dev/monkeypatch/rctiming/
│       │   ├── RcTimingApplication.java
│       │   ├── api/
│       │   │   ├── auth/          AuthController, request/response DTOs
│       │   │   └── admin/         ClubProfileController, TrackController,
│       │   │                      RacingClassController, RaceFormatController
│       │   ├── domain/
│       │   │   ├── user/          User, Role, UserRepository, UserService
│       │   │   ├── auth/          PasswordResetToken, PasswordResetService
│       │   │   ├── club/          ClubProfile, GoverningBodyAffiliation, repositories
│       │   │   ├── track/         Track, DecoderLoop, TrackLapThreshold, repositories
│       │   │   ├── raceclass/     RacingClass, RacingClassRepository
│       │   │   └── format/        RaceFormatTemplate, RaceFormatConfig (sealed),
│       │   │                      TimedRaceConfig, BumpUpConfig, PointsFinalsConfig,
│       │   │                      EventClass, RaceFormatService
│       │   ├── query/             jOOQ generated classes (build output), read query services
│       │   ├── security/
│       │   │   ├── JwtTokenService.java
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   └── SecurityConfig.java
│       │   └── config/
│       │       ├── JacksonConfig.java   (registers yaml mapper, ObjectMapper config)
│       │       └── MailConfig.java
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   └── db/migration/
│       │       ├── V1__create_users_and_roles.sql
│       │       ├── V2__create_club.sql
│       │       ├── V3__create_tracks.sql
│       │       ├── V4__create_racing_classes.sql
│       │       └── V5__create_race_formats.sql
│       └── test/java/dev/monkeypatch/rctiming/
│           ├── AbstractIntegrationTest.java   (shared @Testcontainers base class)
│           ├── api/auth/                      AuthControllerIT.java
│           ├── api/admin/                     TrackControllerIT.java, FormatControllerIT.java ...
│           └── domain/format/                 RaceFormatConfigSerdeTest.java (unit)
├── forwarder/
│   ├── build.gradle.kts         # stub — no implementation
│   └── src/main/java/.gitkeep
└── frontend/                    # Vite project root
    ├── package.json
    ├── vite.config.ts
    ├── tailwind.config.ts
    ├── components.json
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── lib/          api.ts, auth.ts, utils.ts
        ├── hooks/        useAuth.ts
        ├── components/   ui/ (shadcn), layout/AuthLayout.tsx
        ├── pages/        auth/, admin/, racer/
        └── providers/    AuthProvider.tsx, QueryProvider.tsx
```

---

### Pattern 1: JWT Stateless Auth with Refresh Cookie

**What:** Access token (short-lived, in-memory on client) + Refresh token (long-lived, HttpOnly cookie). Spring Security filter validates Bearer token on each request. No session state on server.

**When to use:** Always — this is the only auth pattern in the project.

**Key implementation points:**
- `JwtAuthenticationFilter extends OncePerRequestFilter` — extracts `Authorization: Bearer` header, validates with JJWT, populates `SecurityContextHolder`
- Login endpoint issues access token in response body AND sets `refresh_token` HttpOnly cookie with `SameSite=Lax`
- Refresh endpoint reads cookie, validates, rotates (old token invalidated, new issued), returns new access token
- Store refresh tokens in `password_reset_tokens`-style table for rotation/revocation

```java
// Source: JJWT 0.12.x documentation (Context7 /jwtk/jjwt)
// Token creation
SecretKey key = Keys.hmacShaKeyFor(secretBytes); // from application.yml, base64-encoded
String token = Jwts.builder()
    .subject(userId.toString())
    .claim("roles", roles)   // List<String> e.g. ["RACER"] or ["ADMIN","RACE_DIRECTOR"]
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + accessTokenTtlMs))
    .signWith(key)
    .compact();

// Token parsing
Claims claims = Jwts.parser()
    .verifyWith(key)
    .build()
    .parseSignedClaims(token)
    .getPayload();
String subject = claims.getSubject();
List<String> roles = claims.get("roles", List.class);
```

```java
// Source: [ASSUMED] — standard Spring Security config pattern for stateless JWT
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "RACE_DIRECTOR", "REFEREE")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

---

### Pattern 2: Sealed Interface + Jackson Polymorphism for JSONB Config

**What:** `RaceFormatConfig` is a sealed Java interface with three `record` subtypes. Jackson uses `@JsonTypeInfo` + `@JsonSubTypes` to serialize/deserialize with a `"type"` discriminator. Hypersistence Utils maps the column as true PostgreSQL JSONB.

**When to use:** Any entity field that stores polymorphic config (Phase 1: `RaceFormatTemplate.config`, `EventClass.configSnapshot`, `EventClass.configOverride`).

```java
// Source: CONTEXT.md D-10 (locked decision), Hypersistence Utils docs (Context7 /vladmihalcea/hypersistence-utils)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TimedRaceConfig.class,    name = "TIMED"),
    @JsonSubTypes.Type(value = BumpUpConfig.class,       name = "BUMP_UP"),
    @JsonSubTypes.Type(value = PointsFinalsConfig.class, name = "POINTS_FINALS")
})
public sealed interface RaceFormatConfig
    permits TimedRaceConfig, BumpUpConfig, PointsFinalsConfig {}

public record TimedRaceConfig(
    int durationMinutes,
    StartType startType,
    QualifyingType qualifyingType,
    int racePaddingMinutes,
    int staggerIntervalSeconds
) implements RaceFormatConfig {}

// Entity field mapping (Hibernate 6 + Hypersistence Utils 3.9.11, hibernate-63 artifact)
@Entity
public class RaceFormatTemplate {
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private RaceFormatConfig config;
}
```

---

### Pattern 3: Testcontainers + @ServiceConnection Integration Tests

**What:** Spring Boot 3.4 `@ServiceConnection` annotation wires Testcontainers automatically to Spring datasource configuration. No manual JDBC URL property overrides needed.

**When to use:** All integration tests — `AuthControllerIT`, `TrackControllerIT`, `FormatControllerIT`, etc.

```java
// Source: Spring Boot 3.4 docs (Context7 /websites/spring_io_spring-boot_3_4)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");
    // @ServiceConnection auto-configures spring.datasource.* from container ports
}
```

---

### Pattern 4: jOOQ Code Generation from Flyway Migrations

**What:** The `nu.studer.jooq` Gradle plugin drives jOOQ DSL generation. It depends on `flywayMigrate` so the schema is always current before generation runs. Use a Testcontainers JDBC URL pointing to a temporary PostgreSQL instance at build time, or a local Docker Compose database.

**When to use:** Build-time step — jOOQ classes must be generated before compilation. Gradle task dependency: `flywayMigrate → jooqCodegen → compileJava`.

```kotlin
// Source: jOOQ 3.19 docs (Context7 /websites/jooq_doc_3_19)
tasks.named("jooqCodegen") {
    dependsOn(tasks.named("flywayMigrate"))
    inputs.files(fileTree("src/main/resources/db/migration"))
}
```

> **Build-time database strategy:** The `nu.studer.jooq` plugin needs a live JDBC connection during `./gradlew build`. Options:
> 1. **Local Docker Compose** (preferred for developer machines when Docker is available): start `docker-compose up -d postgres` before building.
> 2. **Testcontainers JDBC URL** in the jOOQ plugin config: `url = "jdbc:tc:postgresql:16:///rctiming_codegen"` — starts a container automatically during the build. Requires Testcontainers and Docker daemon available in the Gradle process.
> 3. **Commit generated sources** to git under `app/src/generated/` and regenerate only when migrations change. This avoids the build-time Docker dependency.
>
> **Recommendation for Phase 1:** Commit generated sources (option 3). It is the lowest-friction approach for a team without guaranteed Docker in CI. Add a `./gradlew jooqCodegen` step to the contributing guide.
> [ASSUMED: team prefers committed generated sources over build-time Docker requirement; the planner should confirm]

---

### Pattern 5: Format Config Export/Import (JSON + YAML)

**What:** Export returns serialized `RaceFormatConfig` as JSON or YAML. Import accepts either, auto-detects from `Content-Type`, validates via Jackson + Bean Validation before saving.

```java
// Source: CONTEXT.md D-12 (locked decision) — [ASSUMED: implementation pattern below]
// Export endpoint — returns JSON by default
@GetMapping(value = "/api/v1/admin/formats/{id}/export",
            produces = {MediaType.APPLICATION_JSON_VALUE, "application/yaml"})
public ResponseEntity<RaceFormatConfig> exportFormat(@PathVariable Long id,
        HttpServletRequest request) { ... }

// Import endpoint — auto-detects from Content-Type
@PostMapping(value = "/api/v1/admin/formats/import",
             consumes = {MediaType.APPLICATION_JSON_VALUE, "application/yaml"})
public ResponseEntity<RaceFormatTemplateDto> importFormat(
        @RequestBody @Valid RaceFormatConfig config) { ... }
```

Jackson `ObjectMapper` must be configured to support both media types. Spring Boot auto-configures JSON; YAML requires registering `YAMLFactory` as an additional mapper or using `@Primary` carefully.

---

### Anti-Patterns to Avoid

- **Hibernate DDL generation in any real environment:** `spring.jpa.hibernate.ddl-auto` must be `validate` in dev/prod and `none` in tests (Testcontainers starts clean; Flyway runs on app startup). Never `update` or `create-drop`.
- **Crossing the Hibernate/jOOQ seam:** Hibernate entities and JPA repositories stay in `domain/`. jOOQ DSL and query services stay in `query/`. A read-only admin list endpoint should use jOOQ, not a `findAll()` JPA call. (In Phase 1 the distinction is minor, but establishing the pattern now prevents drift.)
- **Storing the access JWT in localStorage:** XSS vulnerability. Access token lives in `AuthProvider` React state only. Refresh token is HttpOnly cookie.
- **Using system Gradle 9:** Spring Boot 3.4.x Gradle plugin is incompatible with Gradle 9. The project MUST use a Gradle wrapper (`gradlew`) pinned to 8.14.x.
- **Registering the YAML ObjectMapper as `@Primary`:** Breaks Spring MVC JSON serialization. Use a named `yamlObjectMapper` bean; inject by `@Qualifier` in the format import/export service.
- **Putting format validation logic in the Jackson deserializer:** Prefer `@Valid` on the controller method combined with a `Validator` call in the import service. This keeps deserialisation and validation concerns separate.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSONB ↔ Java object mapping | Custom `AttributeConverter` | Hypersistence Utils `@Type(JsonType.class)` | Converter uses `text` column type, losing PostgreSQL JSONB operators; Hypersistence correctly maps to `jsonb` |
| JWT creation/parsing | Manual Base64 + HMAC | JJWT 0.12.x | Subtle algorithm confusion attacks, expiry edge cases, header manipulation — well-known library handles all |
| Password hashing | Custom hash + salt | Spring Security `BCryptPasswordEncoder` | BCrypt includes adaptive work factor; rolling your own is a security anti-pattern |
| Database migration | Runtime DDL in code | Flyway SQL migrations | Deterministic, version-controlled, reversible |
| Integration test database | Shared dev database | Testcontainers `PostgreSQLContainer` | Test isolation; parallel test runs; CI portability |
| Typed SQL queries (read side) | String-based JPQL or native SQL | jOOQ generated DSL | Type safety, refactoring support, no SQL injection risk |
| Email delivery in tests | Real SMTP or mocked `JavaMailSender` | MailPit via Docker Compose | Inspect actual email content including token links; no mock blind spots |

---

## Common Pitfalls

### Pitfall 1: Gradle 9 / Spring Boot 3.4 Incompatibility

**What goes wrong:** Developer runs `gradle build` using system Gradle 9.3.1. Spring Boot 3.4.x Gradle plugin API changed in 3.5 for Gradle 9 compatibility. Build fails with cryptic plugin errors.

**Why it happens:** Spring Boot 3.4.x plugin targets Gradle 7.6.4–8.x. Gradle 9 removed APIs the plugin uses (issue #43573 in spring-projects/spring-boot, resolved in 3.5). [VERIFIED: web search]

**How to avoid:** Always include `gradle/wrapper/gradle-wrapper.properties` in the repo with `distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.2-bin.zip`. All CI and developers use `./gradlew`, never `gradle` directly.

**Warning signs:** `Caused by: groovy.lang.MissingMethodException` or `Could not find method ... for arguments` during Gradle plugin configuration.

---

### Pitfall 2: Hypersistence Utils Artifact Mismatch

**What goes wrong:** Dependency declared as `hypersistence-utils-hibernate-62` while Spring Boot 3.4.7 ships Hibernate 6.6.18. Silent runtime failures or `ClassNotFoundException` when JSONB mapping activates.

**Why it happens:** Hypersistence Utils publishes separate artifacts per Hibernate version range: `-hibernate-60`, `-hibernate-62`, `-hibernate-63`. Hibernate 6.6.x falls in the 6.3+ range. [VERIFIED: Maven Central artifact listing]

**How to avoid:** Use `io.hypersistence:hypersistence-utils-hibernate-63:3.9.11` exactly. Verify when upgrading Spring Boot.

**Warning signs:** `org.hibernate.HibernateException: Unknown type` or `No Dialect mapping` at startup when entities with `@Type(JsonType.class)` are loaded.

---

### Pitfall 3: jOOQ Code Generation Failing at Build Time (No Database)

**What goes wrong:** `./gradlew build` fails because jOOQ plugin cannot connect to a PostgreSQL instance to generate DSL classes.

**Why it happens:** jOOQ's code generator reverse-engineers the live schema. With the `nu.studer.jooq` plugin, the build needs a JDBC connection at codegen time, which requires a running PostgreSQL instance.

**How to avoid:** Either (a) commit generated sources to git under `app/src/generated/jooq/` and only regenerate manually via `./gradlew jooqCodegen` after migrations change, or (b) use Testcontainers JDBC URL (`jdbc:tc:postgresql:16:///`) in the plugin config (requires Docker daemon during build). Option (a) is recommended for Phase 1. [ASSUMED: see discussion in Pattern 4]

**Warning signs:** `Connection refused` errors during `jooqCodegen` Gradle task.

---

### Pitfall 4: Sealed Interface Serialisation Without Jackson Type Info

**What goes wrong:** `RaceFormatConfig` stored as `{}` or fails to deserialize because the `"type"` discriminator field is missing from the persisted JSON.

**Why it happens:** `@JsonTypeInfo` must be on the interface, not the record subtypes. If the annotation is absent or mis-configured, Jackson serializes just the fields without the discriminator. On read, Jackson cannot determine which subtype to instantiate.

**How to avoid:** Place `@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")` and `@JsonSubTypes(...)` directly on the `sealed interface RaceFormatConfig`. Write a unit test that round-trips each subtype through `ObjectMapper` → `String` → `RaceFormatConfig` and asserts the deserialized type is correct.

**Warning signs:** `InvalidTypeIdException: Could not resolve type id 'null'` on read, or stored JSON missing the `"type"` key.

---

### Pitfall 5: Password Reset Token Enumeration

**What goes wrong:** The `POST /api/v1/auth/password-reset/request` endpoint returns different responses for registered vs. unregistered emails, allowing attackers to enumerate valid user emails.

**Why it happens:** Standard "check if email exists, return error if not" logic leaks information.

**How to avoid:** Always return `200 OK` with "If an account exists for that email, a reset link has been sent." regardless of whether the email is registered. This is specified in the UI-SPEC and MUST be enforced at the controller level, not just the frontend.

**Warning signs:** Integration test that sends reset request for nonexistent email and checks response code/body — should return 200 with generic message.

---

### Pitfall 6: Flyway + jOOQ Schema Drift in Tests

**What goes wrong:** Integration tests run Flyway migrations against the Testcontainers DB but compile against outdated jOOQ generated classes, causing compile errors or silent wrong column references.

**Why it happens:** If generated sources are committed but migrations have changed since the last codegen run, the generated DSL is out of date.

**How to avoid:** CI pipeline must run `./gradlew jooqCodegen` and fail the build if the output differs from committed sources. Add a `gradlew jooqCodegenCheck` task (or a Git diff check) in the CI pipeline.

---

### Pitfall 7: React Router v6 vs v7 API Differences

**What goes wrong:** Implementation uses React Router v6 `<Routes>/<Route>` API but the installed package is v7, causing routing to silently fail or produce deprecation warnings.

**Why it happens:** React Router v7 ships as `react-router-dom` (same package name as v6) but introduces the Data Router as the only recommended approach. Some v6 imperative patterns changed.

**How to avoid:** Use `createBrowserRouter` + `RouterProvider` from day one (as specified in UI-SPEC). Do not use `<BrowserRouter>` wrapper pattern from v6. [VERIFIED: npm registry shows react-router-dom 7.14.1 as latest]

---

## Code Examples

### JJWT 0.12.x Token Issue and Verify

```java
// Source: JJWT README (Context7 /jwtk/jjwt)
// Issue
SecretKey signingKey = Keys.hmacShaKeyFor(
    Decoders.BASE64.decode(base64EncodedSecret));

String accessToken = Jwts.builder()
    .subject(user.getId().toString())
    .claim("roles", user.getRoles().stream()
                        .map(Role::name)
                        .toList())
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + 900_000L)) // 15 min
    .signWith(signingKey)
    .compact();

// Verify and extract
Claims claims = Jwts.parser()
    .verifyWith(signingKey)
    .build()
    .parseSignedClaims(accessToken)
    .getPayload();
```

### Hypersistence Utils JSONB Entity (Hibernate 6)

```java
// Source: Hypersistence Utils README (Context7 /vladmihalcea/hypersistence-utils)
@Entity
@Table(name = "race_format_templates")
public class RaceFormatTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private RaceFormatConfig config;
}
```

### Flyway Migration Naming Convention

```sql
-- Source: Flyway docs (Context7 /flyway/flyway)
-- Convention: V{version}__{description}.sql  (double underscore separator)
-- e.g.:
-- V1__create_users_and_roles.sql
-- V2__create_club.sql
-- V3__create_tracks.sql
-- V4__create_racing_classes.sql
-- V5__create_race_formats.sql
```

### @ServiceConnection Integration Test Base Class

```java
// Source: Spring Boot 3.4 reference docs (Context7 /websites/spring_io_spring-boot_3_4)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    // Flyway runs automatically on context startup
    // jOOQ DSL uses same datasource URL
}
```

### jOOQ Gradle Plugin + Flyway Integration (Kotlin DSL)

```kotlin
// Source: jOOQ 3.19 docs (Context7 /websites/jooq_doc_3_19)
// app/build.gradle.kts
tasks.named("jooqCodegen") {
    dependsOn(tasks.named("flywayMigrate"))
    inputs.files(fileTree("src/main/resources/db/migration"))
    outputs.dir("src/generated/jooq")  // committed to git
}
```

### React Router v7 Data Router Setup

```typescript
// Source: [ASSUMED] — React Router v7 Data Router API (createBrowserRouter)
// frontend/src/App.tsx
import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './components/ProtectedRoute';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
// ...

const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  {
    path: '/admin/*',
    element: <ProtectedRoute roles={['ADMIN', 'RACE_DIRECTOR', 'REFEREE']}><AdminPlaceholderPage /></ProtectedRoute>
  },
  {
    path: '/racer/*',
    element: <ProtectedRoute><RacerPlaceholderPage /></ProtectedRoute>
  },
  { path: '*', element: <NotFoundPage /> },
]);

export default function App() {
  return <RouterProvider router={router} />;
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Spring Boot 2.x + Hibernate 5 | Spring Boot 3.4.x + Hibernate 6 | Nov 2022 | Jakarta EE namespace change (`javax.` → `jakarta.`); all imports must use `jakarta.*` |
| JJWT 0.11 builder API | JJWT 0.12 fluent builder | 2023 | Method names changed (`setClaims()` → `.claims()`; `setExpiration()` → `.expiration()`; `signWith(key, algorithm)` → `.signWith(key)`) |
| Testcontainers manual properties override | `@ServiceConnection` | Spring Boot 3.1+ | Eliminates `@DynamicPropertySource` boilerplate |
| Flyway 9.x | Flyway 10.x | 2023 | PostgreSQL support moved to `flyway-database-postgresql` artifact — must add separately |
| `@TypeDef` package-info.java (Hibernate 5) | `@Type(JsonType.class)` directly on field (Hibernate 6) | Hypersistence Utils 3.x | Simpler; no package-info required |
| React Router 6 `<BrowserRouter>` | React Router 7 `createBrowserRouter` | RR7 release | Data Router is the only recommended API; `<BrowserRouter>` still works but is legacy pattern |
| Vite 5.x | Vite 8.x | 2025 | API stable; upgrade is non-breaking for standard configs |
| Tailwind CSS v3 | Tailwind CSS v4 | 2025 | Config model changed significantly; `tailwind.config.ts` optional; shadcn preset handles v4 config |
| Spring Security 5 `HttpSecurity.cors().and().csrf()` | Spring Security 6 lambda DSL | Spring Boot 3.0+ | Old chained DSL deprecated; use `csrf(AbstractHttpConfigurer::disable)` lambda form |

**Deprecated / outdated:**
- `javax.persistence.*` imports: replaced by `jakarta.persistence.*` in Hibernate 6. Any copy-pasted code from pre-2022 sources will fail to compile.
- Flyway `flyway-core` alone for PostgreSQL: Flyway 10+ requires `flyway-database-postgresql` as a separate dependency. [VERIFIED: Spring Boot BOM POM]
- React 18 as latest: React 19.2.5 is current GA as of April 2026. [VERIFIED: npm registry]

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | React 19 breaking changes are acceptable for this project | Standard Stack (Core — Frontend) | If shadcn preset is not tested against React 19, UI components may have subtle issues. Mitigation: run `npx shadcn@latest init` after React 19 install and check for warnings. |
| A2 | Committed generated jOOQ sources (option 3) is preferred over build-time Docker for Phase 1 | Pattern 4, Pitfall 3 | If team wants clean automated builds without manual codegen step, option 2 (Testcontainers JDBC URL in plugin) is more correct. Requires Docker in Gradle build environment. |
| A3 | A single `AbstractIntegrationTest` base class with a shared static container is the right Testcontainers pattern | Pattern 3 | If tests need isolated schema state, per-test containers are slower but safer. Flyway's repeatable migrations with `cleanMigrate` may be needed. |
| A4 | `/api/v1/` URL prefix is used for all REST endpoints | Code examples, UI-SPEC endpoints | The UI-SPEC hardcodes `/api/v1/auth/*` — this is now effectively locked by the UI-SPEC even though Claude's Discretion covers URL structure. Deviating would require updating the UI-SPEC too. |
| A5 | Refresh tokens are stored in the database (not stateless) | Pattern 1 | Stateless refresh tokens (signed JWT in cookie) skip the DB lookup but cannot be revoked. For a club system where "log out everywhere" is not critical, stateless is simpler. Planner should choose and document the approach. |

---

## Open Questions

1. **React 18 vs React 19**
   - What we know: React 19.2.5 is current GA. UI-SPEC says `^18`. shadcn/ui is tested against both.
   - What's unclear: Whether `^18` was an intentional pin or just the version at the time of UI-SPEC authorship.
   - Recommendation: Use `^19` unless there is a specific reason to stay on 18. If in doubt, start with 19 and note any shadcn init warnings.

2. **jOOQ code generation strategy (build-time vs committed sources)**
   - What we know: Requires a running PostgreSQL to generate. Docker is not installed on dev machine.
   - What's unclear: Whether CI/CD environment has Docker (needed for Testcontainers codegen).
   - Recommendation: Commit generated sources for Phase 1. Revisit for Phase 3+ when the query module is used heavily.

3. **Refresh token: stateless JWT cookie vs database-backed**
   - What we know: HttpOnly cookie is specified. Storage mechanism not locked in CONTEXT.md.
   - What's unclear: Whether token revocation (logout, password change) needs to work immediately or on expiry.
   - Recommendation: Use database-backed refresh tokens (single `refresh_tokens` table). The complexity is low and immediate revocation on password-reset is a security requirement (users who just reset their password should not remain logged in on other devices).

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 (Temurin) | Spring Boot :app compilation | ✓ | 21.0.10 LTS | — |
| Gradle (system) | Build | ✓ (9.3.1) | 9.3.1 — WRONG VERSION | Use Gradle wrapper 8.14.x (must be committed) |
| Gradle wrapper 8.14.x | Spring Boot 3.4.x build | ✗ (not yet set up) | — | Wave 0: initialise wrapper with `gradle wrapper --gradle-version 8.14.2` |
| Node.js | Frontend scaffold + npm installs | ✓ | v20.19.2 | — |
| npm | Frontend dependencies | ✓ | 9.2.0 | — |
| Docker daemon | Docker Compose dev DB + Testcontainers | ✗ | — | See note below |
| Docker Compose | Local `postgres:16` + MailPit | ✗ | — | See note below |
| PostgreSQL client (`psql`) | Manual DB inspection | ✗ | — | Use `docker exec -it postgres psql` once Docker is available |

**Missing dependencies with no fallback:**
- **Docker daemon:** Testcontainers requires Docker to be running for integration tests. Without Docker, `./gradlew test` will fail at any `@Testcontainers` test. This is a dev environment setup concern, not a code concern — integration tests will pass in any environment with Docker (CI, team machines). Wave 0 of the plan should document the Docker install requirement.
- **Gradle wrapper 8.14.x:** Must be created as a Wave 0 task (`gradle wrapper --gradle-version 8.14.2`) before any build tasks. System Gradle 9.3.1 cannot run Spring Boot 3.4.x builds.

**Missing dependencies with fallback:**
- Docker Compose dev database: Testcontainers handles the integration test database independently of Docker Compose. The dev database (for running the app locally) requires Docker Compose, but tests do not depend on it.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test + Testcontainers (backend); Vitest (frontend) |
| Config file (backend) | None needed — Spring Boot auto-configures test context |
| Config file (frontend) | `vite.config.ts` (Vitest config embedded) |
| Quick run command (backend) | `./gradlew :app:test --tests "*.unit.*"` |
| Full suite command (backend) | `./gradlew :app:test` |
| Quick run command (frontend) | `npm run test --prefix frontend` |
| Full suite command (frontend) | `npm run test --prefix frontend -- --coverage` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | `POST /api/v1/auth/register` creates user with hashed password; returns 201 | Integration | `./gradlew :app:test --tests "*AuthControllerIT*register*"` | ❌ Wave 0 |
| AUTH-01 | Duplicate email returns 409 | Integration | `./gradlew :app:test --tests "*AuthControllerIT*duplicateEmail*"` | ❌ Wave 0 |
| AUTH-02 | `POST /api/v1/auth/login` returns access token; sets refresh cookie | Integration | `./gradlew :app:test --tests "*AuthControllerIT*login*"` | ❌ Wave 0 |
| AUTH-02 | `POST /api/v1/auth/refresh` with valid cookie returns new access token | Integration | `./gradlew :app:test --tests "*AuthControllerIT*refresh*"` | ❌ Wave 0 |
| AUTH-03 | Password reset request always returns 200 (email enumeration prevention) | Integration | `./gradlew :app:test --tests "*AuthControllerIT*passwordReset*"` | ❌ Wave 0 |
| AUTH-03 | Reset confirm with valid token changes password; token invalidated | Integration | `./gradlew :app:test --tests "*AuthControllerIT*resetConfirm*"` | ❌ Wave 0 |
| AUTH-03 | Reset confirm with expired/used token returns 400/410 | Integration | `./gradlew :app:test --tests "*AuthControllerIT*expiredToken*"` | ❌ Wave 0 |
| AUTH-04 | `/api/v1/admin/**` returns 403 for `RACER` role | Integration | `./gradlew :app:test --tests "*SecurityIT*adminForbiddenForRacer*"` | ❌ Wave 0 |
| AUTH-05 | User with multiple roles can access endpoints requiring any of those roles | Integration | `./gradlew :app:test --tests "*SecurityIT*stackableRoles*"` | ❌ Wave 0 |
| CLUB-01 | CRUD for governing body affiliations; `membershipRequired` flag persisted | Integration | `./gradlew :app:test --tests "*ClubControllerIT*"` | ❌ Wave 0 |
| CLUB-02 | Club profile create/update persists all fields including GPS and timezone | Integration | `./gradlew :app:test --tests "*ClubControllerIT*profile*"` | ❌ Wave 0 |
| TRACK-01 | CRUD for tracks | Integration | `./gradlew :app:test --tests "*TrackControllerIT*"` | ❌ Wave 0 |
| TRACK-02 | Min lap threshold stored per class with track-wide default fallback | Integration | `./gradlew :app:test --tests "*TrackControllerIT*threshold*"` | ❌ Wave 0 |
| TRACK-03 | Max last-lap threshold stored and retrievable | Integration | `./gradlew :app:test --tests "*TrackControllerIT*threshold*"` | ❌ Wave 0 |
| TRACK-04 | Decoder loop CRUD; scoring/non-scoring flag persisted | Integration | `./gradlew :app:test --tests "*TrackControllerIT*loop*"` | ❌ Wave 0 |
| RACECLASS-01 | CRUD for racing classes | Integration | `./gradlew :app:test --tests "*RacingClassControllerIT*"` | ❌ Wave 0 |
| FORMAT-05 | TIMED/BUMP_UP/POINTS_FINALS configs each accept only their valid fields | Unit | `./gradlew :app:test --tests "*RaceFormatConfigSerdeTest*"` | ❌ Wave 0 |
| FORMAT-05 | Invalid format type discriminator returns 400 | Integration | `./gradlew :app:test --tests "*FormatControllerIT*invalidType*"` | ❌ Wave 0 |
| FORMAT-06 | Template edit after assignment does not affect EventClass snapshot | Integration | `./gradlew :app:test --tests "*FormatControllerIT*snapshot*"` | ❌ Wave 0 |
| FORMAT-07 | Override fields merged correctly at read time; null overrides do not overwrite | Unit | `./gradlew :app:test --tests "*RaceFormatServiceTest*merge*"` | ❌ Wave 0 |
| FORMAT-08/09/10/11 | Enum values for StartType, QualifyingType stored and retrieved correctly | Unit | `./gradlew :app:test --tests "*RaceFormatConfigSerdeTest*"` | ❌ Wave 0 |
| FORMAT-14 | JSON export round-trips cleanly (all subtypes) | Unit | `./gradlew :app:test --tests "*RaceFormatConfigSerdeTest*roundTrip*"` | ❌ Wave 0 |
| FORMAT-14 | YAML import accepted with `Content-Type: application/yaml` | Integration | `./gradlew :app:test --tests "*FormatControllerIT*yamlImport*"` | ❌ Wave 0 |
| FORMAT-14 | Import with invalid schema returns 400 | Integration | `./gradlew :app:test --tests "*FormatControllerIT*importInvalid*"` | ❌ Wave 0 |
| UI AUTH-01/02 | Frontend: register form → success toast → redirect to login | Manual verification | Run Vite dev server + Spring Boot; use browser | — |
| UI AUTH-03 | Frontend: forgot-password always shows success message | Manual verification | Trigger with registered and unregistered email | — |

### Sampling Rate

- **Per task commit:** `./gradlew :app:test --tests "*unit*"` (< 5 seconds, no Docker needed)
- **Per wave merge:** `./gradlew :app:test` (full integration suite with Testcontainers, ~60–90 sec)
- **Phase gate:** Full backend suite green + `tsc --noEmit` passes + manual auth UI verification before `/gsd-verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/dev/monkeypatch/rctiming/AbstractIntegrationTest.java` — shared `@ServiceConnection` Testcontainers base class
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/auth/AuthControllerIT.java` — covers AUTH-01 through AUTH-05
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/admin/ClubControllerIT.java` — covers CLUB-01, CLUB-02
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/admin/TrackControllerIT.java` — covers TRACK-01 through TRACK-04
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/admin/RacingClassControllerIT.java` — covers RACECLASS-01
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/admin/FormatControllerIT.java` — covers FORMAT-05, FORMAT-06, FORMAT-14
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/domain/format/RaceFormatConfigSerdeTest.java` — FORMAT-05, FORMAT-07, FORMAT-08/09/10/11, FORMAT-14 (unit, no Docker)
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/domain/format/RaceFormatServiceTest.java` — FORMAT-07 merge logic (unit)
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/security/SecurityIT.java` — AUTH-04, AUTH-05
- [ ] Gradle wrapper initialisation: `gradle wrapper --gradle-version 8.14.2` — required before any `./gradlew` commands

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | Yes | Spring Security BCrypt; JJWT token issuance; password reset with time-limited tokens |
| V3 Session Management | Yes | Stateless JWT; HttpOnly refresh token cookie; token rotation on refresh; invalidation on password reset |
| V4 Access Control | Yes | `@PreAuthorize` / `.hasAnyRole()` on admin endpoints; role extracted from JWT claims |
| V5 Input Validation | Yes | Bean Validation `@Valid` on all request bodies; Zod schemas on frontend forms |
| V6 Cryptography | Yes | BCrypt for passwords (never hand-roll); HMAC-SHA256 signing key for JWT (never hand-roll) |

### Known Threat Patterns for Stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| SQL injection via jOOQ queries | Tampering | jOOQ parameterized queries — never string concatenation in DSL |
| JWT algorithm confusion (alg:none) | Spoofing | JJWT 0.12 `verifyWith(key)` rejects unsigned tokens; key required |
| XSS → token theft | Information Disclosure | Access token in memory only (not localStorage); HttpOnly cookie for refresh |
| CSRF via cookie-based auth | Elevation of Privilege | Access token in Bearer header (not cookie) prevents CSRF; refresh endpoint uses `SameSite=Lax` cookie |
| Email enumeration via password reset | Information Disclosure | Always return 200 regardless of email existence (enforced server-side) |
| Brute-force login | Elevation of Privilege | [ASSUMED] Spring Security rate limiting or application-level attempt counter — not in scope for Phase 1 but document as known gap |
| JSONB injection via import endpoint | Tampering | `@Valid` + sealed interface deserialization rejects unknown types; `@JsonIgnoreProperties(ignoreUnknown = true)` prevents unexpected field injection |

---

## Sources

### Primary (HIGH confidence)
- [Spring Boot 3.4 reference docs](https://docs.spring.io/spring-boot/3.4/reference/) — via Context7 `/websites/spring_io_spring-boot_3_4`
- [Spring Boot 3.4.7 BOM POM](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/3.4.7/spring-boot-dependencies-3.4.7.pom) — verified Hibernate 6.6.18, jOOQ 3.19.24, Flyway 10.20.1, Testcontainers 1.20.6
- [JJWT README](https://github.com/jwtk/jjwt) — via Context7 `/jwtk/jjwt`; confirmed 0.12.x API syntax
- [jOOQ 3.19 manual](https://www.jooq.org/doc/3.19/manual/) — via Context7 `/websites/jooq_doc_3_19`; Gradle KTS codegen config
- [Hypersistence Utils README](https://github.com/vladmihalcea/hypersistence-utils) — via Context7 `/vladmihalcea/hypersistence-utils`; `@Type(JsonType.class)` Hibernate 6 pattern
- [Flyway docs](https://github.com/flyway/flyway) — via Context7 `/flyway/flyway`; migration naming, Gradle integration
- Maven Central search — verified all Java library versions
- npm registry — verified React 19.2.5, Vite 8.0.8, React Router 7.14.1, Tailwind 4.2.2, TanStack Query 5.99.0

### Secondary (MEDIUM confidence)
- [Spring Boot Gradle 9 issue #43573](https://github.com/spring-projects/spring-boot/issues/43573) — confirmed Spring Boot 3.4.x incompatibility with Gradle 9; web search verified
- [Spring Boot 3.5.0 release blog](https://spring.io/blog/2025/05/22/spring-boot-3-5-0-available-now/) — confirms Gradle 9 support arrived in 3.5, not 3.4

### Tertiary (LOW confidence — marked ASSUMED)
- Spring Security lambda DSL filter chain pattern — training knowledge, consistent with Spring Security 6 docs
- React Router v7 `createBrowserRouter` usage pattern — training knowledge, consistent with installed version

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all versions verified against Maven Central, npm registry, and Spring Boot BOM
- Architecture: HIGH — patterns derived from official documentation and locked CONTEXT.md decisions
- Pitfalls: HIGH (Gradle 9 incompatibility, Flyway artifact split, Hypersistence artifact naming) / MEDIUM (JSONB serde, React Router version gap)
- Security: HIGH — ASVS mapping well-established for JWT + Spring Security stack

**Research date:** 2026-04-16
**Valid until:** 2026-07-16 (90 days — Spring Boot 3.4.x is stable release train; verify before major library upgrades)
