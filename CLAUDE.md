# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

A web-based RC club management and race timing system replacing RCResults. Two user roles: **racers** (self-service portal for profile, cars, transponders, online event entry) and **officials** (browser-based race control client for running a full meeting). Live lap timing is received from AMB/MyLaps decoder hardware over TCP via a separate forwarder application.

See `.planning/PROJECT.md` for the authoritative requirements summary and `.planning/REQUIREMENTS.md` for the full v1 requirement list (88 requirements across AUTH, RACER, CLUB, TRACK, EVENT, FORMAT, FORWARDER, TIMING, CTRL, AUDIO, OFFICIAL, PRACTICE, CHAMP, RESULT domains).

## Planned Stack

**Backend:** Spring Boot 3.4.x, Java 21 (LTS), Maven or Gradle (Kotlin DSL)

**Frontend:** React 18 + Vite, TypeScript, Tailwind CSS + shadcn/ui, TanStack Query v5, TanStack Table v8, React Hook Form v7, Zod, `@stomp/stompjs` (native WebSocket — no SockJS)

**Persistence:** PostgreSQL 16, Flyway for migrations

**ORM — write side (domain module):** Spring Data JPA + Hibernate 6. Entity lifecycle, associations, and repositories. Hibernate sessions must not cross into the read/query module.

**ORM — read side (query module):** jOOQ 3.19.x. Type-safe SQL for all projections and aggregations: scoring calculations, championship standings, lap aggregates, results views. jOOQ's DSL is generated from the live schema. No Hibernate involvement on this side.

**Race format config:** Stored as PostgreSQL JSONB with a `type` discriminator column. Validated against a sealed Java class hierarchy on write. Override patches (FORMAT-07) stored as a second JSONB column and merged at read time. Supports JSON import/export (FORMAT-14). Use `jsonb_pretty(config)` in diagnostic queries.

**Auth:** Spring Security + JWT (stateless). JJWT 0.12.x. JWT in `Authorization: Bearer` header for REST; passed in STOMP `CONNECT` frame for WebSocket.

**Real-time:** Spring WebSocket with STOMP in-memory broker. STOMP topics:
- `/topic/race/{raceId}/timing` — live lap passings, positions, gaps
- `/topic/race/{raceId}/state` — race lifecycle changes
- `/topic/race/{raceId}/marshal` — marshal lap adjustments

**TCP decoder client:** Netty 4.1.x. Two protocols must be supported: (1) **RC-4 text** (`LineBasedFrameDecoder`, port 5100) for firmware < 4.5 decoders — the dominant club hardware; (2) **AMB P3 binary** (`ByteToMessageDecoder`, 0x8E/0x8F delimiters, TLV body, 0x8D byte-stuffing, port 5403) for firmware ≥ 4.5. See `docs/AMB_DECODER_PROTOCOL.md`.

**Forwarder:** Separate Java Gradle submodule. Connects to the AMB decoder via TCP, forwards timing events to the cloud service via gRPC bidirectional streaming. Shares domain model classes with the main application.

**Testing:** JUnit 5 + Mockito + Testcontainers (backend); Vitest + React Testing Library (frontend)

### Do Not Use
- Spring Boot 2.x (EOL)
- Spring WebFlux / reactive stack (unnecessary complexity at venue scale)
- HTMX (incompatible with WebSocket-driven live timing UI)
- Next.js (no SSR needed — all data fetching is client-side)
- Create React App (unmaintained)
- Redux (Zustand or TanStack Query cover all state management needs)
- SockJS (venue LAN in 2026 does not need WebSocket fallback)
- RabbitMQ / Kafka (in-process STOMP broker is sufficient for single-club deployment)
- Liquibase (Flyway plain-SQL is simpler)
- `spring.jpa.hibernate.ddl-auto=update` or `create-drop` in any non-throwaway environment

## Architecture

**Modular monolith** — one Spring Boot process, single PostgreSQL database, single deployment.

### Component Boundaries

| Component | Responsibility |
|-----------|---------------|
| **Racer Portal API** | Profile, cars, transponders, event entry CRUD |
| **Admin Panel API** | Event/championship creation and configuration |
| **Race Control API** | Race lifecycle commands, marshal laps, grid calls |
| **Domain Core** | Business logic, aggregates, domain events |
| **Race State Machine** | Enforces `PENDING → GRID → RUNNING → STOPPED/FINISHED` transitions |
| **TCP Receiver** | Netty component parsing AMB decoder frames (RC-4 text or P3 binary), emits `LapPassingEvent`s |
| **Live Timing Hub** | Broadcasts real-time updates to browsers via STOMP |
| **Domain module** | Hibernate entities, JPA repositories, write-side business logic |
| **Query module** | jOOQ read queries — scoring, standings, lap aggregates, results projections |

The domain and query modules are a hard seam: Hibernate sessions stay in the domain module; jOOQ queries stay in the query module. Neither crosses into the other's territory. This maps to a CQRS-lite split within the monolith — write-side logic operates on JPA-managed objects, read-side never lazy-loads.

### TCP Decoder → Live Display Flow

```
AMB Decoder (TCP) → TCP Receiver (Netty) → LapTimingService
  → calculates lap time, resolves transponder→car→racer, persists LapTime
  → LiveTimingHub → STOMP broadcast → browser clients
```

The TCP receiver runs on a **dedicated background thread** (via `SmartLifecycle` or `ApplicationRunner`), completely isolated from the Tomcat thread pool. It posts parsed `LapPassingEvent`s to `LapTimingService` via `ApplicationEventPublisher` (async listener).

### Race State Machine

`PENDING → GRID → RUNNING → STOPPED → RUNNING` (resume) or `RUNNING → FINISHED`

`RaceState` is an enum on the `Race` entity. `RaceStateMachine` service exposes one method per command. Invalid transitions throw `IllegalStateTransitionException` (HTTP 409). Every successful transition publishes a domain event that `LiveTimingHub` broadcasts to `/topic/race/{id}/state`.

Marshal laps are **not** state transitions — they are `MarshalAdjustment` records (+1/−1) with full audit trail that trigger position recalculation and re-broadcast.

### Roles

Staff roles are **stackable** — a single user account can hold any combination:

| Role | Permissions |
|------|-------------|
| `ADMIN` | Club config, user/role management, event and championship setup, all entries |
| `RACE_DIRECTOR` | Race control client — start/stop races, call grid, marshal laps, abandon/skip |
| `REFEREE` | Apply lap/time penalties, link unknown transponders, raise incident reports |
| `RACER` | Own profile, cars, transponders, entries only |
| Anonymous | Event schedule, live timing, results, championship standings |

### Key Data Design Notes

- Lap timestamps: for P3 binary decoders use the `RTC_TIME` field (GPS/NTP-synchronised UTC microseconds). For RC-4 text decoders use server-anchored offset (no absolute timestamp in protocol). Store as UTC `TIMESTAMPTZ` or `BIGINT` microseconds.
- **Do not store live race positions in the database during a race** — calculate in memory, broadcast over WebSocket, persist only the final result snapshot on `FINISHED`.
- `MyLapsProtocolParser` must be a pure function (`byte[] → LapPassingEvent`) with no Spring dependencies. Protocol I/O is separate from domain logic.
- Championship points: calculate on demand from result snapshots; do not increment incrementally.
- Transponder numbers are unique system-wide. Entry records a transponder snapshot at submission time.
- Race format config is snapshot-at-assignment — template edits do not affect existing events (FORMAT-06).

## AMB Decoder Protocol (Two Protocols — Choose by Firmware)

See `docs/AMB_DECODER_PROTOCOL.md` for the full reference. Summary:

**RC-4 text protocol (firmware < 4.5, port 5100) — implement this first:**
- SOH-prefixed, tab-separated ASCII lines, CRLF terminated
- Two record types: `#` (STATUS/heartbeat every 5s) and `@` (PASSING)
- PASSING fields: `decoderId TAB seqNum TAB transponderId TAB timeSinceStart_s TAB hits TAB strength TAB status TAB crc`
- `timeSinceStart_s` is float seconds since decoder power-on — NOT a Unix timestamp
- Convert to wall clock: anchor server time at first record, add offset for each subsequent record
- No client handshake, no RESEND, no WATCHDOG — just read lines
- Port 5100 confirmed from club hardware captures

**AMB P3 binary protocol (firmware ≥ 4.5, port 5403) — implement second:**
- Frame delimiters: `0x8E` (start) / `0x8F` (end), TLV body, `0x8D` byte-stuffing
- `RTC_TIME` field = uint64 microseconds since Unix epoch (UTC) — absolute timestamp
- Monitor `PASSING_NUMBER` for gaps; RESEND requests on gaps
- WATCHDOG record absence = lost decoder connection
- No client handshake required

**Firmware 4.5 boundary:** firmware 4.5 disables MRT transponders (common cheap club transponders). Most clubs stay on firmware ≤ 4.4 and use port 5100 text protocol.

**Before starting the forwarder:** Build a TCP simulator (fake decoder) emitting RC-4 text records for development without physical hardware. Wireshark captures from the club's RCResults installation confirm the port in use.

## Build Order (Planned)

1. Domain Foundation — entities, Flyway, basic CRUD, no UI
2. Racer Portal — auth, event entry, self-service frontend
3. Admin Panel — event/championship CRUD, admin frontend
4. Race State Machine + Race Control API — lifecycle commands, HTTP 409 on bad transitions
5. WebSocket Live Timing Infrastructure — STOMP config, domain event → broadcast (use synthetic events)
6. Forwarder + AMB TCP Receiver — Netty parser, gRPC streaming, simulator for testing
7. Results & Championship Standings — result snapshots, best-X-from-Y scoring, PDF export

## General Good Developer Rules

1. If you raise any processes, start services or ui services, you MUST stop them after you are finished.
2. Use the `gh` CLI for all GitHub operations (push, PR creation, issue management). The repo uses HTTPS via `gh auth` — do not use SSH git remotes. Remote URL: `https://github.com/oggthemiffed/RCTimingControl.git`.
3. If you see the context getting filled up to a serious level (75% and above) please stop and give me a restart prompt to continue the task after i have cleared the context
4. **Sensitive documentation must NEVER be committed to the repo.** Any doc you generate that contains any of the following must be saved as `docs/local-*.md` or `*.local.md` (both patterns are gitignored) and never staged or committed:
   - Registry paths, container image locations, or package repository URLs specific to this project
   - GitHub/CI/CD configuration steps, settings URLs, or account-specific setup instructions
   - Internal tooling, build system details, or deployment pipeline internals
   - Anything describing how the infrastructure, release process, or development environment is set up internally
   - Account names, usernames, organisation names, or service endpoints
   - Anything you would not want a member of the general public or a competitor to read

   When in doubt, ask: "would this tell a stranger something useful about how this project is built or operated?" If yes — local file only.

   Examples that MUST be local: GitHub Actions setup guides, GHCR configuration steps, branch protection runbooks, environment variable references, deployment checklists, service account details.

   Examples that are safe to commit: architecture decisions, API contracts, user-facing feature docs, contribution guidelines (no internal URLs), quickstart guides for end users.