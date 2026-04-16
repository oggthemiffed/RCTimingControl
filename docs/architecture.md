# Architecture

## Overview

Modular monolith — one Spring Boot process, single PostgreSQL database, single deployment. The complexity of a distributed system isn't warranted for single-club RC venue scale.

```
┌─────────────────────────────────────────────────┐
│                 Spring Boot App                 │
│                                                 │
│  ┌──────────────┐  ┌──────────────────────────┐ │
│  │  REST APIs   │  │   WebSocket / STOMP      │ │
│  │  /api/v1/    │  │   (Phase 5 — not yet)    │ │
│  └──────┬───────┘  └──────────────────────────┘ │
│         │                                       │
│  ┌──────▼──────────────────────────────────┐    │
│  │           Domain Core                   │    │
│  │  JPA entities · services · repositories │    │
│  └──────┬──────────────────────────────────┘    │
│         │                                       │
│  ┌──────▼────────────┐  ┌───────────────────┐   │
│  │  Flyway migrations│  │  jOOQ read queries │   │
│  │  (schema owner)   │  │  (Phase 4+)        │   │
│  └───────────────────┘  └───────────────────┘   │
└─────────────────────┬───────────────────────────┘
                      │
              PostgreSQL 16
```

## Key design decisions

### CQRS-lite split

The domain module owns all writes via Hibernate/JPA. A separate query module (planned Phase 4+) uses jOOQ for read-side projections — scoring calculations, standings, lap aggregates. **Hibernate sessions never cross into the query module; jOOQ never lazy-loads.** This boundary is enforced by package structure, not a framework.

### JWT authentication

Stateless — no server-side session. Access tokens (15-min TTL) are returned in the response body. Refresh tokens (7-day TTL) are stored in an HttpOnly cookie scoped to `/api/v1/auth/refresh` only, preventing JavaScript access. Token rotation on every refresh. All refresh tokens revoked on password reset.

### Race format config (JSONB)

Format configurations are stored as JSONB in PostgreSQL using Hypersistence Utils. The Java type is a sealed interface (`RaceFormatConfig`) with three record subtypes (`TimedRaceConfig`, `BumpUpConfig`, `PointsFinalsConfig`). Jackson's `@JsonTypeInfo` on the interface provides polymorphic serde. A `type` discriminator column on the table enables SQL-side filtering without deserializing the blob.

Override patches (FORMAT-07) are stored in a second `configOverride` JSONB column and merged at read time — base config from the template snapshot, patches applied on top. Template edits do not affect existing event classes (snapshot-at-assignment, FORMAT-06).

### Race state machine (Phase 4)

`PENDING → GRID → RUNNING → STOPPED → RUNNING` (resume) or `RUNNING → FINISHED`. Invalid transitions return HTTP 409. Live race positions are **never persisted during a race** — calculated in memory, broadcast over STOMP, persisted only as a final result snapshot on `FINISHED`.

### TCP decoder (Phase 6)

The AMB/MyLaps decoder client runs on a dedicated background thread (Netty 4.1, `SmartLifecycle`), completely isolated from the Tomcat thread pool. Parsed `LapPassingEvent`s are posted via `ApplicationEventPublisher` (async listener) to avoid blocking the decoder thread.

## Planned STOMP topics (Phase 5)

| Topic | Content |
|-------|---------|
| `/topic/race/{raceId}/timing` | Live lap passings, positions, gaps |
| `/topic/race/{raceId}/state` | Race lifecycle changes |
| `/topic/race/{raceId}/marshal` | Marshal lap adjustments |

## Roles

Staff roles are stackable — one user account can hold any combination:

| Role | Permissions |
|------|-------------|
| `ADMIN` | Club config, user management, event setup |
| `RACE_DIRECTOR` | Race control client — start/stop, grid calls, marshal laps |
| `REFEREE` | Penalties, transponder linking, incident reports |
| `RACER` | Own profile, cars, transponders, entries |
| Anonymous | Event schedule, live timing, results, standings |

## Phase roadmap

| Phase | What it delivers |
|-------|-----------------|
| 1 ✓ | Domain model, Flyway, JWT auth, admin APIs, frontend auth |
| 2 | Racer portal — profile, cars, transponders, event entry |
| 3 | Admin panel UI — event + championship creation and config |
| 4 | Race state machine + race control API |
| 5 | WebSocket live timing infrastructure |
| 6 | Forwarder + AMB TCP decoder (Netty, gRPC) |
| 7 | Results + championship standings, PDF export |
