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

## Race control — Check-in & transponder swap

Open to any official (`ADMIN`, `RACE_DIRECTOR` or `REFEREE`). Withdrawn entries are never returned. Check-in recorded here is authoritative on the day; `racehubArrival` is RaceHub's arrival mark, shown read-only (`ARRIVED`, `NOT_ARRIVED`, or null for entries not imported).

```http
POST /race-control/events/{eventId}/check-in/resolve   { "transponderNumber": "1234567" }
GET  /race-control/events/{eventId}/check-in/search?query=jane
POST /race-control/events/{eventId}/check-in/entries/{entryId}/confirm
```

`resolve` matches the primary or secondary transponder and returns a list, because one competitor may use a transponder in more than one class. Each item is `{ "entryId": 101, "competitorName": "Jane Doe", "className": "Stock Buggy", "transponderNumber": "1234567", "secondaryTransponderNumber": null, "checkedIn": false, "checkedInAt": null, "racehubArrival": "ARRIVED" }`. **404** `{ "error": "not_found" }` when nothing matches. `search` is a case-insensitive name match. `confirm` returns `{ "entry": {...}, "alreadyCheckedIn": false }` and is idempotent: a repeat keeps the first check-in time. **409** `entry_withdrawn`.

```http
POST /race-control/events/{eventId}/entries/{entryId}/transponder-swap
Content-Type: application/json

{ "slot": "PRIMARY", "newTransponderNumber": "7654321" }
```

`slot` is `PRIMARY` or `SECONDARY`. A blank number removes the secondary. **200 OK** with `{ "entryId": 101, "slot": "PRIMARY", "oldTransponderNumber": "1234567", "newTransponderNumber": "7654321" }`; the change is written to the entry audit log as `TRANSPONDER_SWAP`, and laps count on the new number from the next passing. **409** `transponder_already_assigned` if another competitor's entry in the event uses the number (the same competitor's other entries may share it), or `entry_withdrawn`. **400** `same_as_other_transponder` or `primary_required`.

The pre-race readiness grid call (`GET /race-control/race/{raceId}/pre-race-readiness`) carries `checkedIn` and `racehubArrival` on each slot.

---

## Spectator boards

No authentication: these are the read-only feeds behind the venue TV boards at `/boards/now-next` and `/boards/results`. Each takes an optional `eventId`; without it the board shows the event with a race on track, or failing that the most recent event in progress, so it keeps its event between races. The board is empty (every field `null`, `results` empty) only when the requested `eventId` doesn't exist, or none was given and no event is racing or in progress.

```http
GET /boards/now-next?eventId=7
```
**200 OK** — `{ "eventId": 7, "eventName": "Club Round 3", "currentRace": {...} | null, "nextRace": {...} | null, "lastCompletedRace": {...} | null }`. Each race is `{ "raceId": 501, "label": "Qualifying 1 — Stock Buggy — Heat 2", "roundType": "QUALIFIER", "roundNumber": 1, "className": "Stock Buggy", "heatNumber": 2, "finalLetter": null, "status": "RUNNING" }`. `currentRace` is the `RUNNING` race, falling back to a `STOPPED` one; `nextRace` is a race already called to the grid, else the first `PENDING` race in run order (so a race the director skipped ahead to shows once its grid is called); `lastCompletedRace` is the most recently finished.

```http
GET /boards/results?eventId=7
```
**200 OK** — `{ "eventId": 7, "eventName": "Club Round 3", "race": {...} | null, "results": [ResultRow] }`: the last finished race and its result snapshot rows (the same rows as `GET /results/{raceId}`). `results` is empty until the snapshot is written.

```http
GET /boards/races/{raceId}/live-timing
```
**200 OK** — the race's current live timing rows (the same shape as the `/topic/race/{raceId}/timing` frames), or `[]` before the first passing. Boards use it once to fill the table, then follow STOMP.

**Anonymous STOMP.** A client may `CONNECT` to `/ws/timing` with no `Authorization` header. Such a session may only `SUBSCRIBE` to `/topic/race/{raceId}/timing` and `/topic/race/{raceId}/state`; any other subscription and every `SEND` is dropped. A `CONNECT` carrying a token that does not validate is still refused.

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
