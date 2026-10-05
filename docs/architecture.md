# Architecture

> This document covers the application (`app/`, `frontend/`, `decoder-protocol/`, `decoder-simulator/`). RCTC is the timing side of the RaceHub suite and runs only at the venue (local-only plan, #8). The forwarder, the gRPC link, the racer portal and a separate offline race-day app were removed along the way; the race-day app's plans are archived under `docs/plans/archive/`.

## Overview

Modular monolith — one Spring Boot process, single SQLite database file, installed as a background service on the laptop at the track. The complexity of a distributed system isn't warranted for single-club RC venue scale. The same process serves the built React app, the REST API and the WebSocket, so every device on the venue network (race control, check-in, the announcer, the spectator boards, officials' phones) is just a browser.

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
                      ▲
     AMB decoder ─TCP─┘ (decoder listener, inside the app)
```

```
RaceHub (cloud)                      Venue laptop (RCTC)
  booking, racer accounts   ──file──▶  RaceHub import ─▶ competitors + entries
  Entry Export v1 JSON                walk-ins added by hand ─┘
```

## Key design decisions

### CQRS-lite split

The domain module owns all writes via Hibernate/JPA. A separate query module (`query/`) uses jOOQ for read-side projections — scoring calculations, standings, results, boards. **Hibernate sessions never cross into the query module; jOOQ never lazy-loads.** This boundary is enforced by package structure, not a framework.

### Entries come from RaceHub

RaceHub owns booking, racer accounts and payment. The boundary between the two is one file: RaceHub's **Entry Export v1** JSON, which carries what timing needs and no contact, date of birth, guardian or payment data. An admin imports it into an event (`domain/racehub`):

- Each entry is matched by its RaceHub `entry_id` and applied only when its `entry_version` is higher than the one already imported, so importing the same file twice is safe.
- A withdrawn entry is marked `WITHDRAWN`, never deleted, and keeps its race history.
- RaceHub classes map to the event's classes by `rc_class_name`, with a per-event override.
- A dry run previews the changes; an import with unmapped classes or invalid rows saves nothing.

Officials add walk-ins by hand in the admin entry list. A file works with no internet at the track, and the same format could become a pull from RaceHub later.

### Competitors and transponders

A **competitor** is the person an entry belongs to: a display name, the RaceHub driver ID it came from, a BRCA number and a home club. Competitors have no login. Results, live timing and championship standings group by competitor, so one person's history carries across meetings.

Transponders belong to an **entry**, for one event: a primary and an optional secondary number. They are unique within the event, not system-wide. RaceHub snapshots them at booking; the check-in desk can swap them on the day, with an audit trail. When a lap arrives, `LapTimingService` matches the number against both transponders of the entries in the running race. A number that matches nothing, or more than one entry, goes to the referee's unknown-transponder flow and is never silently credited.

### JWT authentication

Stateless — no server-side session. Access tokens (15-min TTL) are returned in the response body. Refresh tokens (7-day TTL) are stored in an HttpOnly cookie scoped to `/api/v1/auth/refresh` only, preventing JavaScript access. Token rotation on every refresh. Only officials (`ADMIN`, `RACE_DIRECTOR`, `REFEREE`) can sign in or refresh; anyone else gets 403.

### Race format config (JSON)

Format configurations are stored as JSON text (checked with `json_valid`) and converted by a JPA `AttributeConverter` (`RaceFormatConfigConverter`). The Java type is a sealed interface (`RaceFormatConfig`) with three record subtypes (`TimedRaceConfig`, `BumpUpConfig`, `PointsFinalsConfig`). Jackson's `@JsonTypeInfo` on the interface provides polymorphic serde. A `type` discriminator column on the table enables SQL-side filtering without deserializing the blob.

Override patches (FORMAT-07) are stored in a second `configOverride` JSON column and merged at read time — base config from the template snapshot, patches applied on top. Template edits do not affect existing event classes (snapshot-at-assignment, FORMAT-06).

### Race state machine

`PENDING → GRID → RUNNING → STOPPED → RUNNING` (resume) or `RUNNING → FINISHED`. Invalid transitions return HTTP 409. Live race positions are **never persisted during a race** — calculated in memory, broadcast over STOMP, persisted only as a final result snapshot on `FINISHED`.

### TCP decoder

The AMB/MyLaps decoder client runs on a dedicated background thread (Netty 4.1, `SmartLifecycle`), completely isolated from the Tomcat thread pool. Parsed `LapPassingEvent`s are posted via `ApplicationEventPublisher` (async listener) to avoid blocking the decoder thread. The listener (`DecoderListener`) reads the decoder's host and port from the club profile, reconnects on its own, and publishes its state to `/topic/system/decoder-status`. Protocol parsing itself (`Rc4TextParser`, `EpochAnchor`, `SeqGapDetector`) lives in the shared `decoder-protocol/` module, used by the listener and `decoder-simulator/`. RC-4 text is implemented; the AMB P3 binary protocol is deferred.

## STOMP topics

| Topic | Content |
|-------|---------|
| `/topic/race/{raceId}/timing` | Live lap passings, positions, gaps |
| `/topic/race/{raceId}/state` | Race lifecycle changes |
| `/topic/race/{raceId}/marshal` | Marshal lap adjustments |
| `/topic/race/{raceId}/unknown-transponder` | A transponder that matched no entry, for the referee |
| `/topic/race/{raceId}/audio` | Announcements for the announcer's browser |
| `/topic/race/{raceId}/bump-up-alert` | Bump-up prompts when a race finishes |
| `/topic/practice/{sessionId}/timing`, `/unknown-transponder` | Open practice |
| `/topic/system/decoder-status` | Whether the decoder is connected |

## Roles

Only officials have accounts. Their roles are stackable — one account can hold any combination:

| Role | Permissions |
|------|-------------|
| `ADMIN` | Club config, user management, event setup |
| `RACE_DIRECTOR` | Race control client — start/stop, grid calls, marshal laps |
| `REFEREE` | Penalties, transponder linking, incident reports |
| Competitors | No account: entries come from the RaceHub import or are added as walk-ins |
| Anonymous | Event schedule, live timing, results, standings |

### Data, backups and packaging

The SQLite file lives in a per-machine data folder outside the install folder, so upgrades keep it. The vendor is chosen in one place (`persistence/DatabaseConfig`), Java code uses only the jOOQ DSL, JPQL and shared converters, and `PersistencePortabilityTest` fails the build on vendor-specific code, so the database could be swapped later (see [development.md](development.md)). Backups are taken when a race day closes, every night and on demand, while racing carries on; `restore` puts one back. The installers (`jpackage`, with their own Java runtime) install the app as a Windows service, a launchd daemon or a systemd unit; see [installing.md](installing.md).

## What's built

See the [README](../README.md#whats-implemented) for the current feature areas. Still to come: results export to RaceHub, a live feed relay and a streaming overlay (#27 to #29).
