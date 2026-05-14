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


### 1.6 Replay protection
- [x] While still logged in, open a new tab and navigate to `http://localhost:5173/setup`
- [x] **Expect:** Wizard sidebar loads — NOT the bootstrap gate (already bootstrapped)


---

## Section 2 — Step 1: Club Profile (Plan 05)

> **What:** The first mandatory wizard step. Saves club name and contact details via the existing club-profiles API. No Skip button.

### 2.1 Step 1 rendering
- [x] The main panel shows **"Club Profile"** heading
- [x] A short description/subtitle is present
- [x] Required field: **Club name**
- [x] Optional fields: timezone, email, phone, website URL (or similar contact fields)
- [x] **No "Skip for now" button** — this step is mandatory
- [x ] A **"Save and Continue"** button is present

** We shoul dhave a selector for the timezone, as no one really rememebrs is, default to the current user tz**

### 2.2 Validation — empty name
- [x] Submit with club name blank
- [x] **Expect:** Inline error on the Club name field — form does not submit

### 2.3 Successful save
- [x] Enter **Club name:** `Test RC Club`, fill optional fields as desired
- [x] Click Save and Continue
- [x] **Expect:** Toast/success notification (e.g. "Club profile saved")
- [x] **Expect:** Sidebar Step 1 shows a green checkmark (or similar complete indicator)
- [x] **Expect:** Main panel automatically advances to Step 2 (Track)

** acan we change the placeholder example text to be more generic and not targetted at an actual club **

---

## Section 3 — Step 2: Track (Plan 05)

> **What:** Saves a single track record. Has a Skip button.

### 3.1 Step 2 rendering
- [x] Heading: **"Track"** (or similar)
- [x] Fields: track name (required), length in metres, optional notes
- [x] Three buttons: **Back**, **Skip for now**, **Save and Continue**

** we have a link to manage more in admin --- not sure if that is right **

### 3.2 Skip behaviour
- [x] Click **"Skip for now"**
- [x] **Expect:** Advances to Step 3 (Race Format)
- [x] **Expect:** Sidebar Step 2 remains incomplete (hollow circle, not green check)

### 3.3 Come back and save (optional but recommended)
- [x] Click Step 2 in the sidebar (or click Back from Step 3)
- [x] Fill in track name: `Main Circuit`, length: `200`
- [x] Click Save and Continue
- [x] **Expect:** Toast, sidebar Step 2 becomes green, advances to Step 3

---

## Section 4 — Step 3: Race Format (Plan 05)

> **What:** Saves a single race format (e.g. "5 Minute Club Race"). Has a Skip button.

### 4.1 Step 3 rendering
- [x] Heading: **"Race Format"** (or similar)
- [x] Fields include: format name, format type or duration
- [x] **Skip for now** and **Save and Continue** buttons present

### 4.2 Successful save
- [x] Enter format name: `5 Min Club Race`, set type/duration as appropriate
- [x] Click Save and Continue
- [x] **Expect:** Toast, sidebar Step 3 green, advances to Step 4

---

## Section 5 — Step 4: Staff Account (Plan 05)

> **What:** Creates a non-RACER staff user via `POST /api/v1/setup/staff`. Has role checkboxes.

### 5.1 Step 4 rendering
- [x] Heading: **"Staff Account"** (or similar)
- [x] User fields: First name, Last name, Email, Password (at least these four)
- [x] Three role checkboxes: **Admin**, **Race Director**, **Referee**
- [x] **Skip for now** and **Save and Continue** buttons

### 5.2 Validation — no role selected
- [x] Leave all role checkboxes unchecked and submit
- [x] **Expect:** Inline error (role is required) — form does not submit

### 5.3 Successful staff creation
- [x] Fill in: First name `Race`, Last name `Director`, email `rd@example.com`, password `Password1!`
- [x] Check **Race Director**
- [x] Click Save and Continue
- [x] **Expect:** Toast, sidebar Step 4 green, advances to Step 5 (Decoder Config placeholder)

---

## Section 6 — Re-entry and navigation (Plans 04 & 05)

> **What:** After partially completing the wizard, closing and reopening should resume at the right step.

### 6.1 Resume at first incomplete step
- [x] Refresh the page (`F5` or navigate to `http://localhost:5173/setup`)
- [x] **Expect:** Wizard sidebar renders immediately (no bootstrap gate — you're already logged in)
- [x] **Expect:** Main panel shows the **first incomplete step**, NOT Step 1 if it's already done

** Fixed: login now redirects back to /setup via ?from= param (committed 2ef6685)

### 6.2 Free navigation from Admin panel
- [x] Navigate to `http://localhost:5173/admin`
- [x] **Expect:** AdminPanelLayout sidebar has a **"Setup Wizard"** nav entry
- [x] Click it — **expect:** navigates to `/setup` with wizard sidebar visible
- [x] In this re-entry mode, sidebar step items should be **clickable** (allow jumping to any step, including completed ones)

---

## Section 7 — Backend security (Plan 03)

> **What:** Verify the three new backend endpoints enforce `ADMIN` role.

### 7.1 Unauthenticated access → 401
- [x] Open browser DevTools → Network tab
- [x] (Or use the terminal curl commands below — either is fine)
```bash
# Should return 401
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/setup/forwarder-config-download
```
- [x] **Expect:** HTTP **401**

### 7.2 Forwarder config download — content
- [x] While logged in as admin, navigate to Step 5 (Decoder Config) — or use curl with JWT:
```bash
# Get JWT via bootstrap (replace token below with your actual login response)
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"Password1!"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/setup/forwarder-config-download
```
- [x] **Expect:** Response contains:
  - `APP_SERVER_URL=`
  - `APP_FORWARDER_TOKEN=<paste-your-token-here>` (literal placeholder — T-08-03)
  - `APP_DECODER_HOST=`
  - `APP_DECODER_PORT=`
  - `APP_DECODER_PROTOCOL=`
- [x] **Expect:** The token line does **NOT** contain any long alphanumeric string (no real token exposed)


** Impossibru as step 5 is just a placeholder screen on the set up wizard **
---

## Section 8 — Decoder Config step (Plan 06 — implement after this UAT)

> **Note:** Step 5 (Decoder Config) and the completion screen are implemented in Plan 06, which runs after this UAT is approved. The placeholder ("Decoder Config (Plan 06)") in the sidebar is expected.

- [x] Confirm the sidebar shows Step 5 placeholder without crashing
- [x] Note any visual issues to report before Plan 06 executes

---

## Sign-off

| Section | Result | Notes |
|---------|--------|-------|
| 1 — Bootstrap Gate | Pass | |
| 2 — Club Profile (Step 1) | Pass | Timezone selector added; generic placeholder |
| 3 — Track (Step 2) | Pass | "Manage more in Admin" link removed |
| 4 — Race Format (Step 3) | Pass | |
| 5 — Staff Account (Step 4) | Pass | |
| 6 — Re-entry & Navigation | Pass | 6.1 fixed via login ?from= redirect |
| 7 — Backend Security | Pass | 7.2 verified via curl |
| 8 — Step 5 Placeholder | Pass | Placeholder renders cleanly; full implementation in Plan 06 |

**Overall:** Pass  
**Approved to continue to Plan 06:** Yes  
**Issues to fix first:** None — all issues resolved during UAT cycle
