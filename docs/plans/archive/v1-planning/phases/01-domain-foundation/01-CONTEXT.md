# Phase 1: Domain Foundation - Context

**Gathered:** 2026-04-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 1 delivers the database schema, core JPA entities, Flyway migrations, auth (JWT-based register/login/password-reset), and admin REST APIs for club profile, governing body affiliations, tracks, racing classes, and race format templates (timed / bump-up / points finals with JSONB config). It also scaffolds the React frontend with auth screens wired up.

Admin configuration forms (club, track, format template CRUD UI) are **deferred to Phase 3**. Phase 1 verifies all backend APIs via integration tests; only the auth UI (login, register, password reset) is built as a React frontend in this phase.

</domain>

<decisions>
## Implementation Decisions

### Build Tool
- **D-01:** Gradle Kotlin DSL (not Maven)
- **D-02:** Multi-module project from day one: root project includes `:app` (Spring Boot main application) and `:forwarder` (stub until Phase 5). `settings.gradle.kts` at repo root includes both modules. The forwarder stub has a `build.gradle.kts` and empty source tree — no implementation yet.

### Project Structure
- **D-03:** Java package root: `dev.monkeypatch.rctiming`
- **D-04:** Internal Spring Boot packages organised by layer:
  - `domain/` — JPA entities, Spring Data repositories, domain services
  - `api/` — REST controllers, request/response DTOs
  - `query/` — jOOQ read queries, projections, aggregations
  - `security/` — JWT filter, Spring Security config, JJWT token service
  - `config/` — Spring configuration beans
- **D-05:** React frontend lives at `frontend/` in the repo root (Vite project, served independently). Not nested inside the Gradle module.
- **D-06:** Local development Postgres via Docker Compose at repo root. `docker-compose.yml` spins up `postgres:16` with a `rctiming_dev` database. Integration tests use Testcontainers (own isolated Postgres per test run, not Docker Compose).

### Auth & Admin UI Scope in Phase 1
- **D-07:** Frontend scope in Phase 1: auth screens only (register, login, password reset). Full React + Vite + Tailwind CSS + shadcn/ui scaffold is created with routing; admin config forms are deferred to Phase 3.
- **D-08:** Backend scope covers all Phase 1 requirements (AUTH, CLUB, TRACK, RACECLASS, FORMAT). All APIs are verified by Spring Boot integration tests (Testcontainers) — no admin UI needed for verification.
- **D-09:** Password reset email: `Spring JavaMailSender` with SMTP transport. Dev/test uses **MailPit** (or Mailhog) via Docker Compose — zero real emails sent locally. Production uses any SMTP relay (configurable via `application.properties` / environment variables). No provider SDK lock-in.

### Format Config Java Modeling
- **D-10:** Three format types modeled as a **sealed interface + Jackson polymorphism**:
  ```java
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes({
    @Type(value = TimedRaceConfig.class,       name = "TIMED"),
    @Type(value = BumpUpConfig.class,           name = "BUMP_UP"),
    @Type(value = PointsFinalsConfig.class,     name = "POINTS_FINALS")
  })
  sealed interface RaceFormatConfig
      permits TimedRaceConfig, BumpUpConfig, PointsFinalsConfig {}
  ```
  Each subtype is a Java `record` containing only its valid fields. Exhaustive `switch` expressions enforce type safety at compile time.

- **D-11:** Hibernate JSONB storage via **Hypersistence Utils** (`io.hypersistence:hypersistence-utils`). Entity field annotated `@Type(JsonBinaryType.class)` mapping `RaceFormatConfig` directly. No manual ObjectMapper wiring in entities.

- **D-12:** Format config import/export (FORMAT-14) supports **both JSON and YAML**. Jackson handles both via `jackson-dataformat-yaml`. Export endpoint returns JSON by default; import endpoint auto-detects from `Content-Type` header (`application/json` vs `application/yaml`). JSON remains the canonical interchange format; YAML is for human authoring.

- **D-13:** FORMAT-07 event-class override storage: **snapshot + second JSONB override column**.
  - `EventClass` entity holds two JSONB columns: `config_snapshot` (full copy of the format template at assignment time, satisfying FORMAT-06) and `config_override` (nullable patch, a `Map<String, Object>` containing only the overridden fields).
  - Effective config = merge(snapshot, override) computed at read time in the domain service.
  - Schema evolution (new required fields) handled by Flyway data migrations backfilling defaults. Jackson uses `@JsonIgnoreProperties(ignoreUnknown = true)` so missing fields default to null/default rather than failing.

### Claude's Discretion
- Specific Flyway migration numbering scheme
- REST API URL structure (e.g. `/api/v1/` prefix or not)
- Exact `application.yml` profile structure for dev/prod/test

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Stack & Architecture
- `CLAUDE.md` — Authoritative stack spec (Spring Boot 3.4.x, Java 21, Gradle or Maven, PostgreSQL 16, Flyway, Hibernate 6 write / jOOQ 3.19 read, JJWT 0.12.x, React 18 + Vite + Tailwind + shadcn/ui). Also defines component boundaries, TCP decoder flow, race state machine, and the Hibernate/jOOQ seam. **Read in full before planning.**

### Requirements
- `.planning/REQUIREMENTS.md` — Full v1 requirement list. Phase 1 requirements:
  - AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05
  - CLUB-01, CLUB-02
  - TRACK-01, TRACK-02, TRACK-03, TRACK-04
  - RACECLASS-01
  - FORMAT-01, FORMAT-02, FORMAT-04, FORMAT-05, FORMAT-06, FORMAT-07, FORMAT-08, FORMAT-09, FORMAT-10, FORMAT-11, FORMAT-12, FORMAT-13, FORMAT-14
  *(FORMAT-03 is not in the Phase 1 requirements list — skip it)*

### Roadmap
- `.planning/ROADMAP.md` §"Phase 1: Domain Foundation" — Goal, success criteria, and full requirements list for this phase.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None — blank slate. No existing source code.

### Established Patterns
- None yet — Phase 1 establishes the patterns all subsequent phases follow.

### Integration Points
- Phase 2 (Racer Portal) builds on the auth and user entity created in Phase 1
- Phase 3 (Admin Panel) builds the UI for the club/track/format APIs created in Phase 1
- Phase 5 (Live Timing) uses the Forwarder submodule stub created in Phase 1

</code_context>

<specifics>
## Specific Ideas

- User noted YAML as a nicer format for hand-authoring race format configs — resolved as both JSON + YAML support (D-12)
- User confirmed awareness of format schema evolution risk (field renames require data migrations) and accepted the snapshot+override approach with that understanding (D-13)

</specifics>

<deferred>
## Deferred Ideas

- Full admin config UI (club, track, format template CRUD forms) — Phase 3
- Forwarder implementation — Phase 5
- Multi-decoder operation — post-v1 (per REQUIREMENTS.md TRACK-04)

</deferred>

---

*Phase: 01-domain-foundation*
*Context gathered: 2026-04-16*
