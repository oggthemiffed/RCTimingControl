# UAT — Phase 08: First-Run Setup Wizard

**What this tests:** The complete wizard flow a new club admin sees when launching RC Timing for the first time — from the bootstrap account-creation gate through all five setup steps to the completion screen.

**Services needed:**
```bash
# For wizard UAT — start WITHOUT dev seed data (no pre-seeded club profile):
make stop && make clean-db && make dev-start SEED=no

# Logs:
tail -f /tmp/rc-backend.log   # watch for "Started RcTimingApplication"
tail -f /tmp/rc-frontend.log

# Normal dev start (includes seed data — NOT suitable for wizard UAT):
make dev-start
```

**Stop when done:** `make stop`

---

## Before you start

- Use a **private / incognito** browser window so there's no stale auth session.
- **IMPORTANT:** The dev seed creates a club profile, which makes the wizard think setup is already done. Always use `SEED=no` for wizard UAT:
  ```bash
  make stop && make clean-db && make dev-start SEED=no
  ```
- Wait for `Started RcTimingApplication` in `/tmp/rc-backend.log` before opening the browser.
- Backend URL: `http://localhost:8080`
- Frontend URL: `http://localhost:5173`

---

## Section 1 — Bootstrap Gate (Plan 04)

> **What:** Before any admin exists, the wizard shows a pre-gate account-creation form instead of the wizard sidebar.

### 1.1 Unauthenticated redirect
- [x] Open `http://localhost:5173/`
- [x] **Expect:** URL changes to `http://localhost:5173/setup` (SetupGuard redirected you)
- [x] **Expect:** A centred card appears — NOT the wizard sidebar

### 1.2 Bootstrap form content
- [x] The card has title **"Set Up RC Timing"**
- [x] Subtitle says something like "Create your admin account to get started"
- [x] Five fields are visible: **First name, Last name, Email, Password, Confirm password**
- [x] A single submit button (e.g. "Create Admin Account" or "Get Started")

### 1.3 Password mismatch validation
- [x] Enter mismatched passwords and submit
- [x] **Expect:** Inline error message — form does not submit

### 1.4 Successful bootstrap
- [x] Submit with valid values:
  - First name: `Test`, Last name: `Admin`
  - Email: `admin@example.com`, Password: `Password1!`, Confirm: `Password1!`
- [x] **Expect:** No redirect to a login page — you land directly in the wizard
- [x] **Expect:** The centred card disappears; the **wizard sidebar** appears on the left

### 1.5 Wizard sidebar structure
- [x] Sidebar header shows brand (e.g. "RC Timing — Setup")
- [x] Five numbered step items are visible:
  1. Club Profile
  2. Track
  3. Race Format
  4. Staff Account
  5. Decoder Config
- [x] A "Skip wizard" or similar exit link is visible at the bottom

** whats the 'manage more in admin' link all about?

### 1.6 Replay protection
- [ ] While still logged in, open a new tab and navigate to `http://localhost:5173/setup`
- [ ] **Expect:** Wizard sidebar loads — NOT the bootstrap gate (already bootstrapped)

** white screen -> installHook.js:1 Maximum update depth exceeded. This can happen when a component calls setState inside useEffect, but useEffect either doesn't have a dependency array, or one of the dependencies changes on every render. **

---

## Section 2 — Step 1: Club Profile (Plan 05)

> **What:** The first mandatory wizard step. Saves club name and contact details via the existing club-profiles API. No Skip button.

### 2.1 Step 1 rendering
- [ ] The main panel shows **"Club Profile"** heading
- [ ] A short description/subtitle is present
- [ ] Required field: **Club name**
- [ ] Optional fields: timezone, email, phone, website URL (or similar contact fields)
- [ ] **No "Skip for now" button** — this step is mandatory
- [ ] A **"Save and Continue"** button is present

** We shoul dhave a selector for the timezone, as no one really rememebrs is, default to the current user tz**

### 2.2 Validation — empty name
- [x] Submit with club name blank
- [x] **Expect:** Inline error on the Club name field — form does not submit

### 2.3 Successful save
- [ ] Enter **Club name:** `Test RC Club`, fill optional fields as desired
- [ ] Click Save and Continue
- [ ] **Expect:** Toast/success notification (e.g. "Club profile saved")
- [ ] **Expect:** Sidebar Step 1 shows a green checkmark (or similar complete indicator)
- [ ] **Expect:** Main panel automatically advances to Step 2 (Track)

** adminApi.ts:402  PUT http://localhost:5173/api/v1/admin/club/profile 400 (Bad Request) -> unable to save **

---

## Section 3 — Step 2: Track (Plan 05)

> **What:** Saves a single track record. Has a Skip button.

### 3.1 Step 2 rendering
- [ ] Heading: **"Track"** (or similar)
- [ ] Fields: track name (required), length in metres, optional notes
- [ ] Three buttons: **Back**, **Skip for now**, **Save and Continue**

### 3.2 Skip behaviour
- [ ] Click **"Skip for now"**
- [ ] **Expect:** Advances to Step 3 (Race Format)
- [ ] **Expect:** Sidebar Step 2 remains incomplete (hollow circle, not green check)

### 3.3 Come back and save (optional but recommended)
- [ ] Click Step 2 in the sidebar (or click Back from Step 3)
- [ ] Fill in track name: `Main Circuit`, length: `200`
- [ ] Click Save and Continue
- [ ] **Expect:** Toast, sidebar Step 2 becomes green, advances to Step 3

---

## Section 4 — Step 3: Race Format (Plan 05)

> **What:** Saves a single race format (e.g. "5 Minute Club Race"). Has a Skip button.

### 4.1 Step 3 rendering
- [ ] Heading: **"Race Format"** (or similar)
- [ ] Fields include: format name, format type or duration
- [ ] **Skip for now** and **Save and Continue** buttons present

### 4.2 Successful save
- [ ] Enter format name: `5 Min Club Race`, set type/duration as appropriate
- [ ] Click Save and Continue
- [ ] **Expect:** Toast, sidebar Step 3 green, advances to Step 4

---

## Section 5 — Step 4: Staff Account (Plan 05)

> **What:** Creates a non-RACER staff user via `POST /api/v1/setup/staff`. Has role checkboxes.

### 5.1 Step 4 rendering
- [ ] Heading: **"Staff Account"** (or similar)
- [ ] User fields: First name, Last name, Email, Password (at least these four)
- [ ] Three role checkboxes: **Admin**, **Race Director**, **Referee**
- [ ] **Skip for now** and **Save and Continue** buttons

### 5.2 Validation — no role selected
- [ ] Leave all role checkboxes unchecked and submit
- [ ] **Expect:** Inline error (role is required) — form does not submit

### 5.3 Successful staff creation
- [ ] Fill in: First name `Race`, Last name `Director`, email `rd@example.com`, password `Password1!`
- [ ] Check **Race Director**
- [ ] Click Save and Continue
- [ ] **Expect:** Toast, sidebar Step 4 green, advances to Step 5 (Decoder Config placeholder)

---

## Section 6 — Re-entry and navigation (Plans 04 & 05)

> **What:** After partially completing the wizard, closing and reopening should resume at the right step.

### 6.1 Resume at first incomplete step
- [ ] Refresh the page (`F5` or navigate to `http://localhost:5173/setup`)
- [ ] **Expect:** Wizard sidebar renders immediately (no bootstrap gate — you're already logged in)
- [ ] **Expect:** Main panel shows the **first incomplete step**, NOT Step 1 if it's already done

### 6.2 Free navigation from Admin panel
- [ ] Navigate to `http://localhost:5173/admin`
- [ ] **Expect:** AdminPanelLayout sidebar has a **"Setup Wizard"** nav entry
- [ ] Click it — **expect:** navigates to `/setup` with wizard sidebar visible
- [ ] In this re-entry mode, sidebar step items should be **clickable** (allow jumping to any step, including completed ones)

---

## Section 7 — Backend security (Plan 03)

> **What:** Verify the three new backend endpoints enforce `ADMIN` role.

### 7.1 Unauthenticated access → 401
- [ ] Open browser DevTools → Network tab
- [ ] (Or use the terminal curl commands below — either is fine)
```bash
# Should return 401
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/setup/forwarder-config-download
```
- [ ] **Expect:** HTTP **401**

### 7.2 Forwarder config download — content
- [ ] While logged in as admin, navigate to Step 5 (Decoder Config) — or use curl with JWT:
```bash
# Get JWT via bootstrap (replace token below with your actual login response)
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"Password1!"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/setup/forwarder-config-download
```
- [ ] **Expect:** Response contains:
  - `APP_SERVER_URL=`
  - `APP_FORWARDER_TOKEN=<paste-your-token-here>` (literal placeholder — T-08-03)
  - `APP_DECODER_HOST=`
  - `APP_DECODER_PORT=`
  - `APP_DECODER_PROTOCOL=`
- [ ] **Expect:** The token line does **NOT** contain any long alphanumeric string (no real token exposed)

---

## Section 8 — Decoder Config step (Plan 06 — implement after this UAT)

> **Note:** Step 5 (Decoder Config) and the completion screen are implemented in Plan 06, which runs after this UAT is approved. The placeholder ("Decoder Config (Plan 06)") in the sidebar is expected.

- [ ] Confirm the sidebar shows Step 5 placeholder without crashing
- [ ] Note any visual issues to report before Plan 06 executes

---

## Sign-off

| Section | Result | Notes |
|---------|--------|-------|
| 1 — Bootstrap Gate | Pass / Fail / Partial | |
| 2 — Club Profile (Step 1) | Pass / Fail / Partial | |
| 3 — Track (Step 2) | Pass / Fail / Partial | |
| 4 — Race Format (Step 3) | Pass / Fail / Partial | |
| 5 — Staff Account (Step 4) | Pass / Fail / Partial | |
| 6 — Re-entry & Navigation | Pass / Fail / Partial | |
| 7 — Backend Security | Pass / Fail / Partial | |
| 8 — Step 5 Placeholder | Pass / Fail / Partial | |

**Overall:** Pass / Fail  
**Approved to continue to Plan 06:** Yes / No  
**Issues to fix first:**
