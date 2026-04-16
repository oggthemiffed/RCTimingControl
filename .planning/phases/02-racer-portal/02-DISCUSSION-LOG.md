# Phase 2: Racer Portal - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-16
**Phase:** 02-racer-portal
**Areas discussed:** Racer entity design, Portal navigation, Event entry scope, Admin tasks in Phase 2

---

## Racer Entity Design

### Where should racer-specific data live?

| Option | Description | Selected |
|--------|-------------|----------|
| Extend User entity | Add racer fields to User table; simple, no join | ✓ |
| Separate RacerProfile entity | User stays auth-only, RacerProfile links 1:1 | |

**User's choice:** Extend User entity
**Notes:** "yes as each racer should have a login. In the future i would like to be able to import users that are not needing a login — would this choice here make this more difficult?" → Acknowledged: login-less imports would require a future Flyway migration to make `passwordHash` nullable, plus auth-layer guards. Noted as deferred idea, not a v1 concern.

---

### What contact fields go on the racer profile?

| Option | Description | Selected |
|--------|-------------|----------|
| Phone number only | firstName + lastName already exist; add phoneNumber | |
| Phone + address | Phone + postal address | |
| Phone + emergency contact | Phone + emergency contact name/phone | ✓ |

**User's choice:** Phone + emergency contact
**Notes:** RC racing is a physical sport; emergency contact is appropriate for liability/safety reasons.

---

### Ability rating (RACER-12) scope in Phase 2

| Option | Description | Selected |
|--------|-------------|----------|
| Store field, admin API only | Table + API; no racer-facing UI | |
| Store + display on portal | Table + API + read-only display on racer profile | ✓ |
| Defer entirely to Phase 7 | Skip for now | |

**User's choice:** Store + display on portal
**Notes:** No additional notes.

---

## Portal Navigation

### Portal page structure

| Option | Description | Selected |
|--------|-------------|----------|
| Routed pages | /racer/profile, /racer/cars, /racer/transponders, /racer/entries | ✓ |
| Single-page tabs | One /racer route with tab navigation | |

**User's choice:** Routed pages
**Notes:** "sounds like a sensible plan, we want to keep an eye on compatibility with mobile devices and lower res monitors"

---

### Mobile navigation adaptation

| Option | Description | Selected |
|--------|-------------|----------|
| Bottom nav bar on mobile | Bottom bar on mobile, horizontal top nav on desktop | ✓ |
| Hamburger menu | Collapsible drawer on mobile | |
| Horizontal nav always | Scrollable nav at all widths | |

**User's choice:** Bottom nav bar on mobile (Recommended)
**Notes:** No additional notes.

---

### Car detail page approach

| Option | Description | Selected |
|--------|-------------|----------|
| List + inline edit | Card expansion / slide-over on same page | ✓ |
| Car detail route (/racer/cars/:id) | Each car has its own page | |

**User's choice:** List + inline edit (Recommended)
**Notes:** No additional notes.

---

## Event Entry Scope

### What does Phase 2 deliver for EVENT-03/EVENT-04?

| Option | Description | Selected |
|--------|-------------|----------|
| Full stack: backend + UI, seeded test events | All APIs + full racer UI + Flyway seeded events | ✓ |
| Backend APIs + public schedule only | APIs + public schedule; entry form deferred | |
| Backend APIs only, stub UI | All APIs + placeholder pages | |

**User's choice:** Full stack: backend + UI, seeded test events
**Notes:** No additional notes.

---

### Public event schedule content

| Option | Description | Selected |
|--------|-------------|----------|
| Event name, date, open/closed status | Clean list — name, date, status, entry link | ✓ |
| Name, date, status + classes | Adds class list per event | |
| Name, date, status + entry count | Adds entry count | |

**User's choice:** Event name, date, open/closed status (Recommended)
**Notes:** No additional notes.

---

### Entry confirmation on submission

| Option | Description | Selected |
|--------|-------------|----------|
| Auto-confirm on submission | PENDING is internal/transient; racer sees Confirmed | ✓ |
| PENDING until admin confirms | Entry stays PENDING; admin confirms in Phase 3 | |

**User's choice:** Auto-confirm on submission
**Notes:** "lets do option 1 just now, what we really want to do later is integrate something like paypal payment for the event so we can put the entry into the pending status until payment is confirmed, this sounds like a later addition but we should note that for later" → Noted as deferred idea: payment integration (PayPal/Stripe) would keep entries PENDING until payment confirmed.

---

## Admin Tasks in Phase 2

### Scope for RACER-10, RACER-08, RACER-14

| Option | Description | Selected |
|--------|-------------|----------|
| Backend API only, no UI | Flyway seed + APIs; admin UI is Phase 3 | ✓ |
| Tag categories get basic admin UI | RACER-10 gets Phase 2 admin UI | |

**User's choice:** Backend API only, no UI (Recommended)
**Notes:** No additional notes.

---

## Claude's Discretion

- Transponders section layout (card/list pattern consistent with cars)
- Entries page layout (table or card list)
- Exact Flyway seeding structure for test events and classes
- REST URL structure (following `/api/v1/` convention)

## Deferred Ideas

- Payment integration (PayPal/Stripe): PENDING-until-paid entry flow — v2
- Login-less racer imports — post-v1
- Admin car tag category UI — Phase 3
