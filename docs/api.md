# API Reference

Base URL: `http://localhost:8080/api/v1`

All protected endpoints require `Authorization: Bearer <access_token>`.

---

## Auth

### Login

```http
POST /auth/login
Content-Type: application/json

{
  "email": "david@example.com",
  "password": "supersecret123"
}
```

**200 OK**
```json
{
  "accessToken": "eyJ...",
  "id": "1",
  "email": "david@example.com",
  "firstName": "David",
  "lastName": "Anderson",
  "roles": ["ADMIN", "RACE_DIRECTOR"]
}
```

Sets `refresh_token` HttpOnly cookie (7-day TTL, path `/api/v1/auth/refresh`).  
**401 Unauthorized** — invalid credentials (no detail returned, by design).  
**403 Forbidden** — the account holds no official role (`ADMIN`, `RACE_DIRECTOR`, `REFEREE`). Only officials sign in; there is no self-registration or password reset.

---

### Refresh access token

Requires the `refresh_token` cookie (sent automatically by the browser).

```http
POST /auth/refresh
```

**200 OK** — new access token + rotated refresh cookie.  
**401 Unauthorized** — cookie missing, expired, or revoked.

---

## Admin — Club

All admin endpoints require a staff role: `ADMIN`, `RACE_DIRECTOR`, or `REFEREE`.

### Get club profile

```http
GET /admin/club/profile
Authorization: Bearer <token>
```

```json
{
  "id": 1,
  "name": "Southside RC Club",
  "email": "info@southsiderc.com",
  "phone": "07700 900000",
  "websiteUrl": "https://southsiderc.com",
  "latitude": 51.5,
  "longitude": -0.1,
  "timezone": "Europe/London",
  "logoType": "SVG"
}
```

---

### Create / update club profile

```http
PUT /admin/club/profile
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Southside RC Club",
  "email": "info@southsiderc.com",
  "phone": "07700 900000",
  "websiteUrl": "https://southsiderc.com",
  "latitude": 51.5074,
  "longitude": -0.1278,
  "timezone": "Europe/London",
  "logoType": "SVG"
}
```

Upserts the singleton club profile row. `timezone` must be a valid IANA timezone ID.

---

### Governing body affiliations

```http
GET    /admin/club/affiliations
POST   /admin/club/affiliations          # 201
PUT    /admin/club/affiliations/{id}
DELETE /admin/club/affiliations/{id}     # 204
```

Request body:
```json
{
  "code": "BRCA",
  "displayName": "British Radio Car Association",
  "membershipRequired": true
}
```

---

## Admin — Tracks

### List / get tracks

```http
GET /admin/tracks
GET /admin/tracks/{id}
```

Response includes nested `decoderLoops` and `lapThresholds` arrays.

---

### Create track

```http
POST /admin/tracks
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Main Circuit",
  "venueNotes": "Carpet surface, 90m",
  "trackLength": 90.0
}
```

---

### Decoder loops

```http
POST   /admin/tracks/{trackId}/loops
PUT    /admin/tracks/loops/{loopId}
DELETE /admin/tracks/loops/{loopId}     # 204
```

Request body:
```json
{
  "loopId": "LOOP_01",
  "displayName": "Start/Finish",
  "loopType": "START_FINISH",
  "isScoringLoop": true
}
```

`loopType` values: `START_FINISH`, `SPLIT`, `PIT_ENTRY`, `PIT_EXIT`

---

### Lap thresholds

```http
POST   /admin/tracks/{trackId}/thresholds
DELETE /admin/tracks/thresholds/{thresholdId}     # 204
```

Request body:
```json
{
  "racingClassId": null,
  "minLapMs": 15000,
  "maxLastLapMs": 90000
}
```

`racingClassId: null` = track-wide default. Set to a class ID to override for a specific class.

---

## Admin — Racing Classes

```http
GET    /admin/classes
GET    /admin/classes/{id}
POST   /admin/classes          # 201
PUT    /admin/classes/{id}
DELETE /admin/classes/{id}     # 204
```

Request body:
```json
{
  "name": "1:10 Electric Touring Car",
  "description": "17.5T blinky spec"
}
```

---

## Admin — Race Formats

### CRUD

```http
GET    /admin/formats
GET    /admin/formats/{id}
POST   /admin/formats          # 201
PUT    /admin/formats/{id}
DELETE /admin/formats/{id}     # 204
```

The `config` field is type-discriminated by `type`. Three format types are supported:

**Timed race** (`TIMED`):
```json
{
  "name": "5-Minute Qualifier",
  "config": {
    "type": "TIMED",
    "durationSeconds": 300,
    "warmupSeconds": 30,
    "startType": "Le_MANS",
    "qualifyingType": "BEST_LAP"
  }
}
```

**Bump-up** (`BUMP_UP`):
```json
{
  "name": "Bump-Up Final",
  "config": {
    "type": "BUMP_UP",
    "durationSeconds": 300,
    "warmupSeconds": 0,
    "startType": "ROLLING",
    "qualifyingType": "BEST_LAP",
    "bumpsPerFinal": 2
  }
}
```

**Points finals** (`POINTS_FINALS`):
```json
{
  "name": "Points Finals",
  "config": {
    "type": "POINTS_FINALS",
    "durationSeconds": 300,
    "warmupSeconds": 0,
    "startType": "ROLLING",
    "qualifyingType": "BEST_LAP",
    "numberOfFinals": 3,
    "pointsTable": [10, 8, 6, 5, 4, 3, 2, 1]
  }
}
```

---

### Export format config

```http
GET /admin/formats/{id}/export
Authorization: Bearer <token>
Accept: application/json          # or: application/yaml
```

Returns the config as JSON or YAML depending on the `Accept` header.

---

### Import format config

```http
POST /admin/formats/import?name=My+Template
Authorization: Bearer <token>
Content-Type: application/json    # or: application/yaml

{
  "type": "TIMED",
  "durationSeconds": 300,
  "warmupSeconds": 30,
  "startType": "LE_MANS",
  "qualifyingType": "BEST_LAP"
}
```

YAML example:
```http
POST /admin/formats/import?name=Imported+Qualifier
Authorization: Bearer <token>
Content-Type: application/yaml

type: TIMED
durationSeconds: 300
warmupSeconds: 30
startType: LE_MANS
qualifyingType: BEST_LAP
```

**201 Created** — returns the full `RaceFormatTemplateDto`.

---

---

## Events — Public schedule

No authentication required.

```http
GET /events
GET /events/{id}
```

Returns published events with their classes, entry availability, and entry window dates.

---

## Admin — Entry management

### Add a walk-in entry

```http
POST /admin/entries
Authorization: Bearer <token>
Content-Type: application/json

{
  "eventId": 5,
  "eventClassId": 11,
  "competitorName": "Wendy Walkin",
  "primaryTransponder": "1234567",
  "secondaryTransponder": "7654321"
}
```

Requires `ADMIN` or `RACE_DIRECTOR`. Give either `competitorId` (an existing competitor) or `competitorName` (a new one), not both. **201 Created** with `{ "entry": {...}, "warnings": [...] }`; a transponder another active entry in the event uses is a warning. **409** if the competitor already has an entry in the class; **422** if the event is completed.

---

### Withdraw an entry

```http
POST /admin/entries/{id}/withdraw
Authorization: Bearer <token>
Content-Type: application/json

{ "reason": "Driver went home" }
```

Requires `ADMIN` or `RACE_DIRECTOR`. Marks the entry `WITHDRAWN` and writes an audit log row.

---

## Cloud — Local Race Day Program lifecycle & sync

The endpoints below are the cloud (`app/`) side of the independent **Local Race Day Program** (`localday/` + `frontend-local/`) — see [architecture.md](architecture.md#local-race-day-program-split-architecture). Unlike everything above, authentication here is split into two different models by design: the lifecycle/pre-cache endpoints use the normal `Authorization: Bearer <access_token>` (an `ADMIN` or `RACE_DIRECTOR` logged into the cloud), while the snapshot-ingest endpoint uses a separate per-day-instance secret, because its caller is a venue machine, not a logged-in browser session.

### Pre-cache an event for offline use

```http
POST /localday/events/{eventId}/pre-cache
Authorization: Bearer <access_token>
Content-Type: application/json

{ "instanceId": "a1b2c3d4-...-venue-laptop-1" }
```

Requires `ADMIN` or `RACE_DIRECTOR`. Mints (or re-mints, idempotently per `instanceId`) a day-scoped PIN credential for every official plus a per-instance sync secret, and returns everything `localday/` needs to go fully offline:

**200 OK**
```json
{
  "event": { "id": 42, "name": "Club Championship Round 1", "eventDate": "2026-09-12" },
  "entries": [{ "entryId": 101, "transponderNumber": "11111", "racerName": "Alice Racer", "carName": "Car A", "className": "Stock Buggy" }],
  "schedule": [{ "raceId": 501, "roundNumber": 1, "heatNumber": 1, "sequence": 1, "className": "Stock Buggy", "finalLetter": null, "status": "PENDING" }],
  "formatConfigs": [{ "eventClassId": 7, "className": "Stock Buggy", "config": { "type": "TIMED_RACE", "...": "..." } }],
  "officialCredentials": [{ "cloudUserId": 3, "officialName": "Dave Director", "pin": "482913" }],
  "instanceSecret": { "instanceId": "a1b2c3d4-...-venue-laptop-1", "instanceSecret": "..." }
}
```

`officialCredentials[].pin` and `instanceSecret.instanceSecret` are only ever returned here, in plaintext, at pre-cache time — `localday/` stores a hash and uses the plaintext PIN purely for the local picker+PIN login (see [Local Race Day Program API](#local-race-day-program-api-localday) below).

### Open / close the event day

```http
POST /localday/events/{eventId}/lifecycle/open
Authorization: Bearer <access_token>
Content-Type: application/json

{ "instanceId": "a1b2c3d4-...-venue-laptop-1" }
```

**200 OK**
```json
{ "eventId": 42, "locked": true, "lockedAt": "2026-09-12T08:00:00Z", "unlockedAt": null, "generation": 1 }
```

Locks the event against direct cloud-side edits (R2) and stamps a fresh `generation` — strictly higher than any previously issued for this event — that `localday/`'s snapshot pushes must meet or exceed (see Snapshot ingest below).

```http
POST /localday/events/{eventId}/lifecycle/close
Authorization: Bearer <access_token>
Content-Type: application/json

{ "syncComplete": true }
```

**200 OK** — `{ "status": "closed" }` or `{ "status": "pending" }`. `syncComplete` is advisory only; the server's own pending-sync count is the actual gate (AE2).

```http
GET /localday/events/{eventId}/lifecycle/lock-status
Authorization: Bearer <access_token>
```

**200 OK** — same shape as `open`'s response; polled by the public event page to decide whether to show the "live data may be delayed" indicator (R15).

### Snapshot ingest (machine-authenticated)

```http
POST /localday/events/{eventId}/snapshots
X-Localday-Instance-Secret: <instanceSecret from pre-cache>
Content-Type: application/json

{ "instanceId": "a1b2c3d4-...-venue-laptop-1", "generation": 1, "snapshotId": "b7e1...", "payload": { "...": "laps, results, standings, current heat/next-up — opaque to the cloud" } }
```

**200 OK** — `{ "status": "accepted", "generation": 1 }`.
**409 Conflict** — `{ "status": "superseded", "generation": 2 }`: the instance's `generation` is strictly lower than the event's current stored generation (a device-loss declaration happened, or a stale device reconnected) — the snapshot is rejected, never merged. A repeated `snapshotId` for the same event is accepted idempotently (no duplicate row, no duplicate recompute).

No `Authorization` header — authenticated instead by `X-Localday-Instance-Secret`, checked against the secret minted for this `instanceId` at pre-cache time, before the generation check runs.

### Device-loss declaration

```http
POST /localday/events/{eventId}/device-loss
Authorization: Bearer <access_token>
Content-Type: application/json

{ "instanceId": "a1b2c3d4-...-venue-laptop-1", "reason": "Laptop died mid-meeting, replaced with spare" }
```

**200 OK**
```json
{ "eventId": 42, "instanceId": "a1b2c3d4-...-venue-laptop-1", "incompleteData": true, "declaredAt": "2026-09-12T14:32:00Z" }
```

Requires `ADMIN` specifically (narrower than the `ADMIN`-or-`RACE_DIRECTOR` gate on the endpoints above) — confirms on-site that the original device is physically down, not merely unreachable. Writes an audit record, invalidates the original instance's sync secret, unlocks the day for a replacement instance to open, and permanently sets an incomplete-data flag that never auto-clears (R16, R17).

---

## Local Race Day Program API (`:localday`)

A **separate backend**, not a route prefix under the cloud `app/` — these run on `localday/`'s own port (`:8081` in development) and use a different auth model entirely: a day-scoped local session, not the cloud's JWT. Error bodies here are `{"error": "<code>"}`, not the RFC 9457 shape used above.

Base URL (dev): `http://localhost:8081/api/v1`

### Local auth

`GET /local-auth/officials`, `POST /local-auth/login`, and `POST /local-auth/recover` are unauthenticated by design — login *is* the authentication step, and the officials list has to be fetchable before anyone is logged in. Every other endpoint below requires `Authorization: Bearer <sessionToken>` from a successful login.

```http
GET /local-auth/officials
```
**200 OK** — `[{ "credentialId": 3, "officialName": "Dave Director", "recovery": false }]`

```http
POST /local-auth/login
Content-Type: application/json

{ "credentialId": 3, "secret": "482913" }
```
**200 OK** — `{ "sessionToken": "...", "officialName": "Dave Director", "credentialId": 3 }`
**401 Unauthorized** — `{ "error": "invalid_credential" }`
**423 Locked** — `{ "error": "locked", "retryAfterSeconds": 60 }` (three consecutive wrong PINs locks the credential with exponential backoff, capped at 5 minutes)

```http
POST /local-auth/recover
Content-Type: application/json

{ "recoveryCredentialId": 1, "recoverySecret": "...", "targetCredentialId": 3 }
```
**200 OK** — `{ "unlocked": true }`. A locally-cached secondary official/admin credential can unlock another official's locked session without cloud connectivity.

### Day lifecycle (local side)

```http
POST /day-lifecycle/pre-cache
POST /day-lifecycle/open
Content-Type: application/json

{ "eventId": 42, "email": "director@example.com", "password": "..." }
```

Both unauthenticated by design (no local session exists yet) — they call the cloud endpoints above using the official's *cloud* credentials, one time, to pull the pre-cache payload. `open` with a blank/absent `email`/`password` means "attempt offline" — reuses whatever this instance already has cached and sets a sticky `splitBrainWarning` flag, since an offline open cannot confirm exclusivity with the cloud.

**200 OK** (all three of pre-cache/open/status share this shape):
```json
{ "status": "OPEN", "eventId": 42, "generation": 1, "splitBrainWarning": false, "pendingSyncCount": 0, "lastPreCachedAt": "2026-09-12T07:55:00Z", "superseded": false }
```

```http
POST /day-lifecycle/close
Authorization: Bearer <sessionToken>
```
**200 OK** — `{ "status": "closed", "pendingSyncCount": 0 }` or `{ "status": "pending", "pendingSyncCount": 3 }`. Unlike pre-cache/open, this requires a session — an official is always logged in locally by the time they close the day.

```http
GET /day-lifecycle/status
```
**200 OK** — same shape as pre-cache/open; unauthenticated, polled by the login screen to decide what to show before anyone logs in.

### Race control

All endpoints below require `Authorization: Bearer <sessionToken>` (R9) — the anonymous spectator view is the separate `/boards/*` API further down.

```http
GET /race-control/races
```
**200 OK** — `[{ "id": 501, "roundNumber": 1, "heatNumber": 1, "sequence": 1, "className": "Stock Buggy", "finalLetter": null, "status": "PENDING", "startedAt": null, "finishedAt": null }]`

```http
GET /race-control/races/{id}
```
**200 OK** — the race plus its grid: `{ "race": { ... }, "grid": [{ "cachedEntryId": 101, "racerName": "Alice Racer", "transponderNumber": "11111", "carNumber": 1, "gridPosition": 1, "bumped": false }] }`

```http
POST /race-control/races/{id}/transition
Content-Type: application/json

{ "target": "RUNNING" }
```
**200 OK** — the updated race. **409 Conflict** — `{ "error": "illegal_transition" }` if another official already moved it, or a double-submitted click raced the optimistic-lock check. Valid targets follow `PENDING → GRID → RUNNING → STOPPED → RUNNING` (resume) or `RUNNING/STOPPED → FINISHED`.

```http
POST /race-control/races/{id}/marshal-adjustment
Content-Type: application/json

{ "cachedEntryId": 101, "lapDelta": 1 }
```
**200 OK** — `{ "raceId": 501, "entryId": 101, "transponderNumber": "11111", "lapDelta": 1, "actingUserName": "Dave Director" }`. `lapDelta` must be `1` or `-1`; the entry must be in this race's grid. Triggers a live-position recalculation and a STOMP re-broadcast, same as a real lap crossing.

```http
POST /race-control/races/{id}/advance-round
Content-Type: application/json

{ "nextScheduleId": 502, "entryIdsInFinishingOrder": [103, 101, 102] }
```
**200 OK** — the next race's updated detail (grid reordered to match the finishing order). Only reorders grid rows that already exist on the next race — it does not create new ones, so the next round's entrants must already be known.

```http
GET /race-control/races/{id}/live
```
**200 OK** — `{ "raceId": 501, "rows": [{ "entryId": 101, "position": 1, "driverName": "Alice Racer", "lapsCompleted": 5, "lastLapMs": 12450, "bestLapMs": 12100, "gapToLeaderMs": 0 }] }`. A one-off fallback for the initial paint before the first STOMP `/topic/race/{id}/timing` message arrives — officials' and spectators' live tables are STOMP-driven after that.

### Check-in & transponder reassignment

```http
GET /checkin/search?query=alice
POST /checkin/resolve                      { "transponderNumber": "11111" }
POST /checkin/{cachedEntryId}/confirm
```
`confirm` returns `{ "entryId": 101, "racerName": "Alice Racer", "checkedIn": true, "checkedInAt": "...", "alreadyCheckedIn": false }` — idempotent, `alreadyCheckedIn` tells the UI whether this was a no-op re-confirm.

```http
POST /transponders/reassign
Content-Type: application/json

{ "cachedEntryId": 101, "newTransponderNumber": "22222" }
```
**200 OK** — `{ "entryId": 101, "oldTransponderNumber": "11111", "newTransponderNumber": "22222" }`. **409 Conflict** — `{ "error": "transponder_already_assigned" }` if another entry already has it. Audit-logged with the acting official's identity (R8).

### Anonymous boards

No authentication — this is the deliberate read-only counterpart to race control (R9).

```http
GET /boards/now-next
```
**200 OK** — `{ "currentRace": { ... } | null, "nextRace": { ... } | null, "lastCompletedRace": { ... } | null }`. `currentRace` resolves to the `RUNNING` race, falling back to a `STOPPED` one; `nextRace` is the earliest `PENDING`/`GRID` race. All three can be `null` before the first heat.

```http
GET /boards/results
```
**200 OK** — `{ "race": { ... } | null, "results": [{ "position": 1, "racerName": "Alice Racer", "carNumber": 1, "lapsCompleted": 12, "bestLapMs": 11980 }] }` — the most recently finished race's result rows.

---

## Error responses

All errors use [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid timezone: Europe/Londonn"
}
```

Validation errors include a field-level `errors` map:
```json
{
  "status": 400,
  "errors": {
    "name": "must not be blank",
    "email": "must be a well-formed email address"
  }
}
```
