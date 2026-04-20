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

```bash
cd frontend
npm test          # Vitest unit tests
npm run build     # Type-check + bundle (catches TS errors)
```

---

## Phase 2 — Racer Portal: Manual UAT checklist

Start both services before testing:

```bash
# Terminal 1
docker compose up -d
./gradlew :app:bootRun --args='--spring.profiles.active=dev'

# Terminal 2
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

**Get a token first:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"your@email.com","password":"yourpassword"}' \
  | jq -r .accessToken)
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
# Register a second account and get its token
TOKEN2=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Other","lastName":"Racer","email":"other@example.com","password":"password123"}' \
  | jq -r .accessToken)

# Get car ID from your account
CAR_ID=$(curl -s http://localhost:8080/api/v1/racer/cars \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

# Try to access it with the other account → 404
curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/api/v1/racer/cars/$CAR_ID \
  -H "Authorization: Bearer $TOKEN2"
# Expect: 404
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
