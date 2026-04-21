# Testing Guide

## Automated tests

### Backend integration tests

Requires Docker (Testcontainers spins up a real PostgreSQL container automatically).

```bash
# Run all tests
./gradlew :app:test

# Run a specific test class
./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.racer.CarControllerIT"

# Run tests for a specific phase
./gradlew :app:test --tests "*racer*"

# Skip jOOQ codegen (faster when schema hasn't changed)
./gradlew :app:test -x generateJooq
```

### Frontend

No unit test suite is set up yet. Use the type-checker and linter to catch issues:

```bash
cd frontend
npm run build     # Type-check + bundle (tsc -b && vite build)
npm run lint      # ESLint
```

### Running tests for a specific phase

```bash
# Phase 3 — admin panel (events, championships, storage)
./gradlew :app:test --tests "dev.monkeypatch.rctiming.api.admin.*"

# Phase 2 — racer portal
./gradlew :app:test --tests "*racer*"
```

---

## Starting the full dev environment

Always use `make dev` (or the explicit commands below) — the `--spring.profiles.active=dev` flag is required to load the datasource config from `application-dev.yml`. Running without it causes an immediate "Failed to configure a DataSource" error.

```bash
# Recommended — starts docker services, backend (dev profile), and frontend:
make dev-start

# Or run each service manually in separate terminals:
make up                                                             # Terminal 1: PostgreSQL + Mailpit + MinIO
./gradlew :app:bootRun --args='--spring.profiles.active=dev'       # Terminal 2: backend
cd frontend && npm run dev                                          # Terminal 3: frontend
```

## Phase 2 — Racer Portal: Manual UAT checklist

Start both services before testing:

```bash
make up
./gradlew :app:bootRun --args='--spring.profiles.active=dev'

# Separate terminal:
cd frontend && npm run dev
```

Open `http://localhost:5173`. Register a new account (it gets the `RACER` role by default).

---

### 1. Portal layout and routing

| Step | Action | Expected |
|------|--------|----------|
| 1.1 | Navigate to `/racer` while logged in | Redirects to `/racer/profile` |
| 1.2 | View on desktop (≥768px wide) | Horizontal top nav with brand + Profile / Cars / Transponders / Entries links |
| 1.3 | View on mobile (<768px) | Top nav hidden; fixed bottom nav with icon + label for each section |
| 1.4 | Click each nav link | Active link has visible highlight; correct page renders |
| 1.5 | Navigate to `/racer` while logged out | Redirected to login |

---

### 2. Profile page

| Step | Action | Expected |
|------|--------|----------|
| 2.1 | Open `/racer/profile` | Displays your name, email, phone fields, empty memberships list, read-only class ratings section |
| 2.2 | Edit first name, click **Save** | Toast "Profile updated." appears; page reflects new name |
| 2.3 | Clear phone field, click **Save** | Saves successfully (phone is optional) |
| 2.4 | Click **Add membership**, enter code `BRCA` and membership number | Row appears in memberships list |
| 2.5 | Try to add `BRCA` again | Toast error "Already registered with this body." |
| 2.6 | Click remove on the BRCA row | Row disappears from list |

---

### 3. Cars page

| Step | Action | Expected |
|------|--------|----------|
| 3.1 | Open `/racer/cars` with no cars | Empty state: "No cars added" with an **Add car** button |
| 3.2 | Click **Add car**, fill in name (e.g. "Serpent 411"), click **Save** | Sheet closes; car card appears in the grid |
| 3.3 | Click the car card | `CarEditSheet` slides in from the right with the car's details pre-filled |
| 3.4 | Change the name, click **Save** | Sheet closes; card shows updated name; toast confirms save |
| 3.5 | Open the car again, click **Archive** | Confirmation prompt appears; on confirm, sheet closes and card disappears from grid |
| 3.6 | Add two more cars | Grid shows multiple cards; layout is responsive |

---

### 4. Backend-only checks (curl)

These don't require the frontend and verify backend behaviour directly.

> **Important:** run all commands in the **same terminal session** — shell variables like `$TOKEN` are not shared between tabs or separate shell invocations.

**Dev seed accounts** — automatically created when the `dev` profile is active (no manual registration needed):

| Email | Password | Role |
|-------|----------|------|
| `racer1@example.com` | `Racer1Pass!` | RACER |
| `racer2@example.com` | `Racer2Pass!` | RACER |

**Get tokens (run once at the start of a test session):**
```bash
export TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"racer1@example.com","password":"Racer1Pass!"}' \
  | jq -r .accessToken)

export TOKEN2=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"racer2@example.com","password":"Racer2Pass!"}' \
  | jq -r .accessToken)

# Verify both were captured:
echo "TOKEN:  ${TOKEN:0:20}..."
echo "TOKEN2: ${TOKEN2:0:20}..."
```

**Public event schedule (no auth):**
```bash
curl -s http://localhost:8080/api/v1/events | jq .
# Expect: 200 with array of events (may be empty if none seeded)
```

**Transponder uniqueness (409 on duplicate):**
```bash
# First registration succeeds
curl -s -X POST http://localhost:8080/api/v1/racer/transponders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"transponderNumber":"12345"}' | jq .status

# Second registration with same number → 409
curl -s -X POST http://localhost:8080/api/v1/racer/transponders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"transponderNumber":"12345"}' | jq .status
```

**Ownership isolation (another racer cannot see your car):**
```bash
# Get car ID from racer1's account (add a car first if none exist)
export CAR_ID=$(curl -s http://localhost:8080/api/v1/racer/cars \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

# Try to access it with racer2 → 404
curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/api/v1/racer/cars/$CAR_ID \
  -H "Authorization: Bearer $TOKEN2"
# Expect: 404
```

---

---

## Phase 3 — Admin Panel: Manual UAT checklist

**Dev seed accounts** (all created automatically in dev mode):

| Email | Password | Role |
|-------|----------|------|
| `racer1@example.com` | `Racer1Pass!` | RACER |
| `racer2@example.com` | `Racer2Pass!` | RACER |
| `admin1@example.com` | `Admin1Pass!` | ADMIN |

Start services (MinIO is required for logo upload):

```bash
make up                                                        # starts postgres + mailpit + minio
./gradlew :app:bootRun --args='--spring.profiles.active=dev'  # separate terminal
cd frontend && npm run dev                                     # separate terminal
```

MinIO console available at http://localhost:9001 (user: `minioadmin`, pass: `minioadmin`).

---

### 1. Admin panel shell and routing

| Step | Action | Expected |
|------|--------|----------|
| 1.1 | Log in as `admin1@example.com`, navigate to `/admin` | Redirects to `/admin/events`; left sidebar visible on desktop |
| 1.2 | View on desktop (≥768px) | Fixed left sidebar with two nav groups: *Events & Competitions* and *Configuration* |
| 1.3 | View on mobile (<768px) | Hamburger button visible; click opens Sheet drawer with same nav |
| 1.4 | Log in as a RACER, navigate to `/admin` | Redirected away (role guard) |

---

### 2. Events list and create

| Step | Action | Expected |
|------|--------|----------|
| 2.1 | Navigate to `/admin/events` | Table loads; columns: ID, Name, Date, Status, Track |
| 2.2 | Click **Create Event**, fill in name and date, submit | Dialog closes; new row appears with `DRAFT` badge |
| 2.3 | Empty name → submit | Validation error shown inline |

---

### 3. Event detail and state machine

| Step | Action | Expected |
|------|--------|----------|
| 3.1 | Click a `DRAFT` event row | Detail page opens; tabs: Overview / Classes / Entries |
| 3.2 | Click **Publish Event** | Confirm dialog appears; on confirm, badge changes to `PUBLISHED` (purple) |
| 3.3 | Click **Open Entries** | Confirm dialog; badge changes to `OPEN` (green) |
| 3.4 | Click **Close Entries** | Confirm dialog; badge changes to `CLOSED` (amber) |
| 3.5 | Click **Complete Event** | Confirm dialog; badge changes to `COMPLETED` (grey) |
| 3.6 | Force an invalid transition via curl (see section 6) | UI shows error toast |

---

### 4. Event class management

| Step | Action | Expected |
|------|--------|----------|
| 4.1 | On event detail, Classes tab → **Add Class** | Dialog with racingClassId + templateId inputs |
| 4.2 | Submit with valid IDs | New class card appears with snapshot config |
| 4.3 | Click **Edit Overrides** on a class | JSON editor dialog opens; save a field override |
| 4.4 | Check two class checkboxes → **Combine** | Both cards merged with `COMBINED` indicator |

---

### 5. Entry list and withdraw

| Step | Action | Expected |
|------|--------|----------|
| 5.1 | Entries tab on an event with entries | Table shows racer name, car, transponder, status per class |
| 5.2 | Click **Withdraw** on an entry, enter reason | Confirm dialog; status changes to `WITHDRAWN` |

---

### 6. Backend-only checks (curl) — Phase 3

> Run all commands in the **same terminal session**.

**Get admin token:**
```bash
export ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin1@example.com","password":"Admin1Pass!"}' \
  | jq -r .accessToken)
echo "ADMIN: ${ADMIN_TOKEN:0:20}..."
```

**Event CRUD:**
```bash
# Create event
export EVENT_ID=$(curl -s -X POST http://localhost:8080/api/v1/admin/events \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Round 1","eventDate":"2026-06-01T10:00:00Z"}' | jq -r .id)

# List events
curl -s http://localhost:8080/api/v1/admin/events \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[0]'

# Transition: DRAFT → PUBLISHED
curl -s -X POST "http://localhost:8080/api/v1/admin/events/$EVENT_ID/transition" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetState":"PUBLISHED"}' | jq .status

# Invalid transition → expect 409
curl -s -o /dev/null -w "%{http_code}" \
  -X POST "http://localhost:8080/api/v1/admin/events/$EVENT_ID/transition" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetState":"DRAFT"}'
# Expect: 409
```

**Championship CRUD:**
```bash
# Create championship
export CHAMP_ID=$(curl -s -X POST http://localhost:8080/api/v1/admin/championships \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"2026 Club Championship","season":2026,"bestResultsCount":8,"totalRounds":10}' \
  | jq -r .id)

# List championships
curl -s http://localhost:8080/api/v1/admin/championships \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[0].name'

# Link event to championship
curl -s -X POST "http://localhost:8080/api/v1/admin/championships/$CHAMP_ID/events" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT_ID\",\"roundNumber\":1}" | jq .roundNumber
```

**Car-tag-category archive (soft-delete):**
```bash
# List active categories
curl -s http://localhost:8080/api/v1/admin/car-tag-categories \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[0] | {id, name}'

export CAT_ID=$(curl -s http://localhost:8080/api/v1/admin/car-tag-categories \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

# Archive
curl -s -X DELETE "http://localhost:8080/api/v1/admin/car-tag-categories/$CAT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -o /dev/null -w "%{http_code}"
# Expect: 204

# Confirm gone from default listing
curl -s http://localhost:8080/api/v1/admin/car-tag-categories \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '[.[].id] | contains(["'"$CAT_ID"'"])'
# Expect: false

# Unarchive
curl -s -X POST "http://localhost:8080/api/v1/admin/car-tag-categories/$CAT_ID/unarchive" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -o /dev/null -w "%{http_code}"
# Expect: 200
```

**Racer cannot access admin endpoints:**
```bash
export TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"racer1@example.com","password":"Racer1Pass!"}' | jq -r .accessToken)

curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/api/v1/admin/events \
  -H "Authorization: Bearer $TOKEN"
# Expect: 403
```

---

## Known stubs (not yet testable)

The following pages exist as placeholders and will be implemented in a later phase:

| Route | Status |
|-------|--------|
| `/racer/transponders` | Placeholder — backend API is live, UI coming in Plan 06 |
| `/racer/entries` | Placeholder — backend API is live, UI coming in Plan 06 |
| `/events` | Placeholder — backend API is live (`/api/v1/events`), UI coming in Plan 06 |

Use the curl examples above to test the backend endpoints in the meantime.
