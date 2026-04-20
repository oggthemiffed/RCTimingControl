# API Reference

Base URL: `http://localhost:8080/api/v1`

All protected endpoints require `Authorization: Bearer <access_token>`.

---

## Auth

### Register

```http
POST /auth/register
Content-Type: application/json

{
  "firstName": "David",
  "lastName": "Anderson",
  "email": "david@example.com",
  "password": "supersecret123"
}
```

**201 Created**
```json
{
  "accessToken": "eyJ...",
  "id": "1",
  "email": "david@example.com",
  "firstName": "David",
  "lastName": "Anderson",
  "roles": ["RACER"]
}
```

Sets `refresh_token` HttpOnly cookie (7-day TTL, path `/api/v1/auth/refresh`).

---

### Login

```http
POST /auth/login
Content-Type: application/json

{
  "email": "david@example.com",
  "password": "supersecret123"
}
```

**200 OK** — same body as register. Sets `refresh_token` cookie.  
**401 Unauthorized** — invalid credentials (no detail returned, by design).

---

### Refresh access token

Requires the `refresh_token` cookie (sent automatically by the browser).

```http
POST /auth/refresh
```

**200 OK** — new access token + rotated refresh cookie.  
**401 Unauthorized** — cookie missing, expired, or revoked.

---

### Request password reset

```http
POST /auth/password-reset/request
Content-Type: application/json

{
  "email": "david@example.com"
}
```

Always returns **200 OK** regardless of whether the email exists (prevents enumeration). A reset link is sent to Mailpit in dev: `http://localhost:8025`.

---

### Confirm password reset

```http
POST /auth/password-reset/confirm
Content-Type: application/json

{
  "token": "<token-from-email>",
  "newPassword": "newpassword456"
}
```

**200 OK** on success. All existing refresh tokens for the user are revoked.

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

## Racer — Profile

All racer endpoints require `Authorization: Bearer <token>` and the `RACER` role.

### Get profile

```http
GET /racer/profile
Authorization: Bearer <token>
```

```json
{
  "id": 1,
  "firstName": "David",
  "lastName": "Anderson",
  "email": "david@example.com",
  "phoneNumber": "07700 900000",
  "emergencyContactName": "Jane Anderson",
  "emergencyContactPhone": "07700 900001",
  "phoneticName": "DAY-vid",
  "memberships": [
    { "governingBodyCode": "BRCA", "membershipNumber": "12345" }
  ],
  "classRatings": [
    { "racingClassName": "1:10 Electric Touring Car", "rating": "CLUB" }
  ]
}
```

---

### Update profile

```http
PATCH /racer/profile
Authorization: Bearer <token>
Content-Type: application/json

{
  "phoneNumber": "07700 900000",
  "emergencyContactName": "Jane Anderson",
  "emergencyContactPhone": "07700 900001",
  "phoneticName": "DAY-vid"
}
```

All fields are optional — only provided fields are updated. Email and roles cannot be changed via this endpoint.

---

### Governing body memberships

```http
GET    /racer/memberships
POST   /racer/memberships       # 201; 409 if already registered with this body
DELETE /racer/memberships/{code}  # 204
```

POST body:
```json
{ "governingBodyCode": "BRCA", "membershipNumber": "12345" }
```

---

## Racer — Cars

### List cars

```http
GET /racer/cars
Authorization: Bearer <token>
```

Returns the authenticated racer's non-archived cars with their tag values.

---

### Create car

```http
POST /racer/cars
Authorization: Bearer <token>
Content-Type: application/json

{ "name": "Serpent 411", "notes": "17.5T blinky spec" }
```

**201 Created**

---

### Update car

```http
PATCH /racer/cars/{id}
Authorization: Bearer <token>
Content-Type: application/json

{ "name": "Serpent 411 v2", "notes": "Updated notes" }
```

**404** if the car does not belong to the authenticated racer.

---

### Archive car

```http
DELETE /racer/cars/{id}
Authorization: Bearer <token>
```

**204** — sets `archived = true`. Archived cars are excluded from list responses.

---

### Set tag value on a car

```http
POST /racer/cars/{id}/tags
Authorization: Bearer <token>
Content-Type: application/json

{ "categoryId": 1, "value": "Yokomo YD-2" }
```

Upserts the tag value for the given category. One value per category per car.

---

### Remove tag value from a car

```http
DELETE /racer/cars/{id}/tags/{categoryId}
Authorization: Bearer <token>
```

**204**

---

## Admin — Car tag categories

Requires `ADMIN`, `RACE_DIRECTOR`, or `REFEREE` role.

```http
GET    /admin/car-tag-categories
POST   /admin/car-tag-categories          # 201
PUT    /admin/car-tag-categories/{id}
DELETE /admin/car-tag-categories/{id}     # 204
```

POST/PUT body:
```json
{ "name": "Chassis", "sortOrder": 1 }
```

Seven default categories are seeded: Chassis, ESC, Motor, Servo, Battery, Body, Tyres.

---

## Racer — Transponders

### List transponders

```http
GET /racer/transponders
Authorization: Bearer <token>
```

---

### Register transponder

```http
POST /racer/transponders
Authorization: Bearer <token>
Content-Type: application/json

{ "transponderNumber": "12345" }
```

**201 Created** — **409** if that transponder number is already registered by any racer in the system.

---

### Delete transponder

```http
DELETE /racer/transponders/{id}
Authorization: Bearer <token>
```

**204** — **404** if the transponder does not belong to the authenticated racer.

---

## Events — Public schedule

No authentication required.

```http
GET /events
GET /events/{id}
```

Returns published events with their classes, entry availability, and entry window dates.

---

## Racer — Entries

### List entry history

```http
GET /racer/entries
Authorization: Bearer <token>
```

Returns the racer's full entry history across all events, joined with event details.

---

### Submit entry

```http
POST /racer/entries
Authorization: Bearer <token>
Content-Type: application/json

{
  "eventClassId": 1,
  "carId": 42,
  "transponderNumber": "12345"
}
```

**201 Created** — entry is auto-confirmed in the same transaction.

**Response:**
```json
{
  "entry": { "id": 99, "status": "CONFIRMED", ... },
  "warnings": ["Transponder 12345 is already registered to another entry in this event"]
}
```

**422 Unprocessable Entity** — if the event class requires a governing body membership and the racer has no matching number (and no admin override).

---

### Withdraw entry

```http
DELETE /racer/entries/{id}
Authorization: Bearer <token>
```

**204** — transitions entry to `WITHDRAWN`. **404** if the entry does not belong to the authenticated racer.

---

## Admin — Entry management

Requires `ADMIN` or `RACE_DIRECTOR` role.

### Swap transponder on an entry

```http
PATCH /admin/entries/{id}/transponder
Authorization: Bearer <token>
Content-Type: application/json

{ "newTransponderNumber": "99999" }
```

Updates the transponder snapshot on the entry and writes an audit log row (`TRANSPONDER_SWAP`).

---

### Apply membership override

```http
POST /admin/entries/{id}/membership-override
Authorization: Bearer <token>
Content-Type: application/json

{ "reason": "Membership card verified in person" }
```

Requires `ADMIN` or `RACE_DIRECTOR` role (not available to `REFEREE`). Sets `membershipOverride = true` on the entry and writes an audit log row (`MEMBERSHIP_OVERRIDE`). Confirms a previously blocked entry.

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
