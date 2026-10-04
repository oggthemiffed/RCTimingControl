# Architecture

> This document covers the application (`app/`, `frontend/`, `decoder-protocol/`, `decoder-simulator/`). A separate offline race-day app was retired in #21; its plans are archived under `docs/plans/archive/`.

## Overview

Modular monolith — one Spring Boot process, single SQLite database file, single deployment. The complexity of a distributed system isn't warranted for single-club RC venue scale.

```
┌─────────────────────────────────────────────────┐
│                 Spring Boot App                 │
│                                                 │
│  ┌──────────────┐  ┌──────────────────────────┐ │
│  │  REST APIs   │  │   WebSocket / STOMP      │ │
│  │  /api/v1/    │  │   (live timing hub)      │ │
│  └──────┬───────┘  └──────────────────────────┘ │
│         │                                       │
│  ┌──────▼──────────────────────────────────┐    │
│  │           Domain Core                   │    │
│  │  JPA entities · services · repositories │    │
│  └──────┬──────────────────────────────────┘    │
│         │                                       │
│  ┌──────▼────────────┐  ┌───────────────────┐   │
│  │  Flyway migrations│  │  jOOQ read queries │   │
│  │  (schema owner)   │  │  (scoring, results)│   │
│  └───────────────────┘  └───────────────────┘   │
└─────────────────────┬───────────────────────────┘
                      │
        SQLite (one database file)
```

## Key design decisions

### CQRS-lite split

The domain module owns all writes via Hibernate/JPA. A separate query module (planned Phase 4+) uses jOOQ for read-side projections — scoring calculations, standings, lap aggregates. **Hibernate sessions never cross into the query module; jOOQ never lazy-loads.** This boundary is enforced by package structure, not a framework.

### JWT authentication

Stateless — no server-side session. Access tokens (15-min TTL) are returned in the response body. Refresh tokens (7-day TTL) are stored in an HttpOnly cookie scoped to `/api/v1/auth/refresh` only, preventing JavaScript access. Token rotation on every refresh. Only officials (`ADMIN`, `RACE_DIRECTOR`, `REFEREE`) can sign in or refresh; anyone else gets 403.

### Race format config (JSONB)

Format configurations are stored as JSON text (checked with `json_valid`) and converted by a JPA `AttributeConverter` (`RaceFormatConfigConverter`). The Java type is a sealed interface (`RaceFormatConfig`) with three record subtypes (`TimedRaceConfig`, `BumpUpConfig`, `PointsFinalsConfig`). Jackson's `@JsonTypeInfo` on the interface provides polymorphic serde. A `type` discriminator column on the table enables SQL-side filtering without deserializing the blob.

Override patches (FORMAT-07) are stored in a second `configOverride` JSONB column and merged at read time — base config from the template snapshot, patches applied on top. Template edits do not affect existing event classes (snapshot-at-assignment, FORMAT-06).

### Race state machine

`PENDING → GRID → RUNNING → STOPPED → RUNNING` (resume) or `RUNNING → FINISHED`. Invalid transitions return HTTP 409. Live race positions are **never persisted during a race** — calculated in memory, broadcast over STOMP, persisted only as a final result snapshot on `FINISHED`.

### TCP decoder

The AMB/MyLaps decoder client runs on a dedicated background thread (Netty 4.1, `SmartLifecycle`), completely isolated from the Tomcat thread pool. Parsed `LapPassingEvent`s are posted via `ApplicationEventPublisher` (async listener) to avoid blocking the decoder thread. Protocol parsing itself (`RC4TextParser`, `EpochAnchor`, `SeqGapDetector`, the P3 binary decoder) lives in the shared `decoder-protocol/` module, used by `app/`'s decoder listener and `decoder-simulator/`.

## STOMP topics

| Topic | Content |
|-------|---------|
| `/topic/race/{raceId}/timing` | Live lap passings, positions, gaps |
| `/topic/race/{raceId}/state` | Race lifecycle changes |
| `/topic/race/{raceId}/marshal` | Marshal lap adjustments |

## Roles

Only officials have accounts. Their roles are stackable — one account can hold any combination:

| Role | Permissions |
|------|-------------|
| `ADMIN` | Club config, user management, event setup |
| `RACE_DIRECTOR` | Race control client — start/stop, grid calls, marshal laps |
| `REFEREE` | Penalties, transponder linking, incident reports |
| Competitors | No account: entries come from the RaceHub import or are added as walk-ins |
| Anonymous | Event schedule, live timing, results, standings |

## Phase roadmap

All ten phases are complete — see the [README](../README.md#whats-implemented) for the current feature-area breakdown.
