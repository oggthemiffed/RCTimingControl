---
status: complete
phase: 02-racer-portal
source: [02-VERIFICATION.md]
started: 2026-04-17
updated: 2026-04-22
---

## Setup

```bash
make up
./gradlew :app:bootRun --args='--spring.profiles.active=dev'
cd frontend && npm run dev
```

Open `http://localhost:5173`. Register a new account (gets `RACER` role by default), or use seed account `racer1@example.com` / `Racer1Pass!`.

## Tests

### 1. Portal layout and routing
expected: Top nav visible on desktop (md+), bottom nav visible on mobile (<md); `/racer` redirects to `/racer/profile`; active link highlighted; `/racer` while logged out redirects to login
result: Pass

### 2. Profile page — edit and save
expected: Displays name, email, phone fields and empty memberships list; editing first name and clicking Save shows "Profile updated." toast and reflects new name; clearing phone saves successfully
result: Pass

### 3. Profile page — governing body memberships
expected: Click Add membership, enter code `BRCA` and a number → row appears; trying to add `BRCA` again shows "Already registered with this body." toast; Remove deletes the row
result: Pass

### 4. Cars page — add and edit
expected: Empty state shows "No cars added" with Add car button; adding a car closes sheet and shows card; clicking card opens CarEditSheet pre-filled; saving updated name reflects on card
result: Pass

### 5. Cars page — archive
expected: Open car sheet, click Archive → confirmation prompt; on confirm, sheet closes and card disappears from grid
result: Pass

### 6. Public event schedule — no auth required
expected: `GET /api/v1/events` returns HTTP 200 without Authorization header (may be empty array if no events seeded)
result: Pass

### 7. Ownership isolation (backend)
expected: Car created by racer1 returns 404 when fetched with racer2's token
```bash
export TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"racer1@example.com","password":"Racer1Pass!"}' | jq -r .accessToken)
export TOKEN2=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"racer2@example.com","password":"Racer2Pass!"}' | jq -r .accessToken)
export CAR_ID=$(curl -s http://localhost:8080/api/v1/racer/cars \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')
curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/api/v1/racer/cars/$CAR_ID \
  -H "Authorization: Bearer $TOKEN2"
# Expect: 404
```
result: Pass

### 8. Transponder uniqueness (backend)
expected: Second registration of the same transponder number returns 409
result: Pass

## Summary

total: 8
passed: 8
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps
