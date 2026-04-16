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

**TCP decoder client:** Netty 4.1.x with a custom `ByteToMessageDecoder` for the AMB P3 binary protocol (0x8E/0x8F frame delimiters, TLV body, 0x8D byte-stuffing)

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
| **TCP Receiver** | Netty component parsing AMB P3 frames, emits `LapPassingEvent`s |
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

- Lap timestamps from the `RTC_TIME` field of decoder PASSING records (hardware clock), not server receipt time. Store as UTC `TIMESTAMPTZ` or `BIGINT` microseconds.
- **Do not store live race positions in the database during a race** — calculate in memory, broadcast over WebSocket, persist only the final result snapshot on `FINISHED`.
- `MyLapsProtocolParser` must be a pure function (`byte[] → LapPassingEvent`) with no Spring dependencies. Protocol I/O is separate from domain logic.
- Championship points: calculate on demand from result snapshots; do not increment incrementally.
- Transponder numbers are unique system-wide. Entry records a transponder snapshot at submission time.
- Race format config is snapshot-at-assignment — template edits do not affect existing events (FORMAT-06).

## AMB P3 Protocol (Highest-Risk Unknown)

The AMB/MyLaps decoder TCP protocol is proprietary. The forwarder must implement it:
- Frame delimiters: `0x8E` (start) / `0x8F` (end)
- Body: TLV (type-length-value)
- Byte stuffing: `0x8D` escape byte
- Requires `FIRST_CONTACT` handshake on initial connection
- Monitor `PASSING_NUMBER` for gaps; send RESEND requests on gaps
- WATCHDOG record absence = lost decoder connection

**Before starting the forwarder:** Register at mylaps.com/developers for the SDK, or capture Wireshark traces from the club's existing RCResults installation. Build a TCP simulator (fake decoder) for development and testing without physical hardware.

## Build Order (Planned)

1. Domain Foundation — entities, Flyway, basic CRUD, no UI
2. Racer Portal — auth, event entry, self-service frontend
3. Admin Panel — event/championship CRUD, admin frontend
4. Race State Machine + Race Control API — lifecycle commands, HTTP 409 on bad transitions
5. WebSocket Live Timing Infrastructure — STOMP config, domain event → broadcast (use synthetic events)
6. Forwarder + AMB TCP Receiver — Netty parser, gRPC streaming, simulator for testing
7. Results & Championship Standings — result snapshots, best-X-from-Y scoring, PDF export
