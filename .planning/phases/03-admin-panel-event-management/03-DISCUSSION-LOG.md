# Phase 3: Admin Panel & Event Management - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-20
**Phase:** 03-admin-panel-event-management
**Areas discussed:** Admin panel layout, Event state machine UX, Championship config UX, Phase 1 deferred admin config scope

---

## Admin Panel Layout

| Option | Description | Selected |
|--------|-------------|----------|
| Left sidebar | Classic admin pattern, accommodates 6+ sections | ✓ |
| Top nav (like racer portal) | Consistent with racer portal, but crowded at this scale | |

**User's choice:** Left sidebar

| Option | Description | Selected |
|--------|-------------|----------|
| Desktop-first, mobile optional | Optimize for trackside laptop use | |
| Same mobile polish as racer portal | Bottom nav bar on mobile, thumb-friendly | ✓ |

**User's choice:** Same mobile polish as racer portal — admin may be used from a phone to manage entries

| Option | Description | Selected |
|--------|-------------|----------|
| Grouped sections | Dividers between Events & Competitions | Configuration | ✓ |
| Flat list | All items at the same level | |

**User's choice:** Grouped sections

---

## Event State Machine UX

| Option | Description | Selected |
|--------|-------------|----------|
| Status badge with action buttons | Valid next-state buttons shown; invalid never appear | ✓ |
| State dropdown | Admin picks from dropdown, invalid grayed out | |
| Wizard steps | Horizontal stepper through all states | |

**User's choice:** Status badge with action buttons

| Option | Description | Selected |
|--------|-------------|----------|
| Confirm dialog for destructive transitions | Modal before OPEN→ENTRIES_CLOSED and IN_PROGRESS→COMPLETED | ✓ |
| No confirmation dialogs | All transitions fire immediately | |

**User's choice:** Confirm dialog for destructive transitions

| Option | Description | Selected |
|--------|-------------|----------|
| Coloured status badge per row | Colour-coded: grey/blue/green/amber/red/black | ✓ |
| Plain text status | Status as text only | |

**User's choice:** Coloured status badge per row

| Option | Description | Selected |
|--------|-------------|----------|
| Inline class management on event detail page | Classes section with + Add Class button | ✓ |
| Separate class management page | Dedicated /admin/events/:id/classes route | |

**User's choice:** Inline class management

| Option | Description | Selected |
|--------|-------------|----------|
| Checkbox multi-select + Combine button | Select 2+ classes, confirm dialog | ✓ |
| Drag-and-drop grouping | More intuitive but complex | |
| Combine button per class pair | Awkward for 3+ class combinations | |

**User's choice:** Checkbox multi-select + Combine button (EVENT-06)

| Option | Description | Selected |
|--------|-------------|----------|
| View + withdraw on behalf of racer | Table with withdraw action per entry | ✓ |
| View only | Read-only table | |
| Full CRUD | Withdraw, confirm, override membership | |

**User's choice:** View + withdraw on behalf of racer (ENTRY-02)

| Option | Description | Selected |
|--------|-------------|----------|
| Nested under event detail | Click class → see entries | ✓ |
| Separate Entries nav section | Global /admin/entries route | |

**User's choice:** Nested under event detail

---

## Championship Config UX

| Option | Description | Selected |
|--------|-------------|----------|
| Championship links to events explicitly | Admin adds events one by one | ✓ |
| Championship auto-matches by class + date range | Automatic round detection | |

**User's choice:** Explicit event linking

| Option | Description | Selected |
|--------|-------------|----------|
| Editable table with presets | ROAR defaults, inline edit, preset options | ✓ |
| Editable table, no presets | Always blank | |
| JSON/YAML import | Like race format config | |

**User's choice:** Editable points table with presets (CHAMP-04)

| Option | Description | Selected |
|--------|-------------|----------|
| On the championship standings page | Exclude action per driver per round | ✓ |
| On the event detail page | Per-event exclusion management | |

**User's choice:** On championship standings page (CHAMP-09)

| Option | Description | Selected |
|--------|-------------|----------|
| Full standings table with drop scores | All rounds, best-X highlighted | ✓ |
| Summary only | Total points, no breakdown | |

**User's choice:** Full standings table with drop scores (CHAMP-10)

| Option | Description | Selected |
|--------|-------------|----------|
| Radio buttons on championship form | Qualifying / Finals / Both | ✓ |
| Per-event override | Default + per-event override | |

**User's choice:** Radio buttons on championship form (CHAMP-06)

| Option | Description | Selected |
|--------|-------------|----------|
| Part of championship creation form | TQ bonus + A-final bonus as number fields | ✓ |
| Separate bonus config page | Sub-page with more steps | |

**User's choice:** Part of championship creation form (CHAMP-07/08)

| Option | Description | Selected |
|--------|-------------|----------|
| One championship, multiple classes | 2026 Club Series covers all classes | ✓ |
| Separate championship per class | One entity per class | |

**User's choice:** One championship, multiple classes (CHAMP-03)

| Option | Description | Selected |
|--------|-------------|----------|
| Show result type prominently | DNF/DNS/DQ as visible labels | ✓ |
| Show 0 pts, no special treatment | Result type in tooltip only | |

**User's choice:** Show result type prominently (CHAMP-02)

| Option | Description | Selected |
|--------|-------------|----------|
| Inherit defaults, per-class override optional | Championship defaults + class overrides | ✓ |
| Configure per class independently | No inheritance | |

**User's choice:** Inherit championship defaults, per-class override optional

| Option | Description | Selected |
|--------|-------------|----------|
| Match by racing class entity (same DB record) | Automatic matching, no manual mapping | ✓ |
| Admin manually maps event class to championship class | Flexible but error-prone | |

**User's choice:** Match by racing class entity (automatic)

---

## Phase 1 Deferred Admin Config Scope

| Option | Description | Selected |
|--------|-------------|----------|
| Full forms, same quality as racer portal | RHF + Zod validation, error messages | ✓ |
| Minimal working forms | Basic forms, functional only | |
| API-only for now | Skip UI, use REST client | |

**User's choice:** Full forms, same quality as racer portal

| Option | Description | Selected |
|--------|-------------|----------|
| Type-specific dynamic forms | Format type dropdown, fields change | ✓ |
| JSON/YAML editor for format config | Raw textarea | |
| Both form mode + raw editor toggle | Most work, most flexible | |

**User's choice:** Type-specific dynamic forms for race format templates

| Option | Description | Selected |
|--------|-------------|----------|
| All fields on one track form | Basic + decoder/loop sections | ✓ |
| Basic track info separate from decoder config | Two-tab or two-step | |

**User's choice:** All fields on one form with clearly labelled sections

| Option | Description | Selected |
|--------|-------------|----------|
| Add/edit/archive categories, no delete | Archive preserves historical data | ✓ |
| Full CRUD including delete | Cascade complexity | |
| Add only | Seeded categories permanent | |

**User's choice:** Add/edit/archive (no hard delete) for car tag categories

| Option | Description | Selected |
|--------|-------------|----------|
| Text fields only, no logo upload | Name, URL, governing body affiliations | |
| Logo upload (image file) | Full club identity | ✓ |

**User's choice:** Logo upload included

**Notes:** User asked about open source S3-compatible Docker storage → **MinIO** selected. Open source, Docker-backed, S3-compatible. Added to docker-compose.yml. AWS S3 SDK used with MinIO endpoint in dev.

| Option | Description | Selected |
|--------|-------------|----------|
| Local disk | Files in /var/rctiming/uploads | |
| MinIO (S3-compatible) | Open source Docker container | ✓ |

**User's choice:** MinIO for object storage

---

## Claude's Discretion

- Exact sidebar width and collapse behaviour
- Admin route structure under /admin/
- Format override edit UI (inline vs diff-style)
- Points scale preset values (ROAR, BRCA defaults)
- MinIO bucket naming and upload path conventions

## Deferred Ideas

- Public championship standings (CHAMP-05) — Phase 7
- Payment-gated entry confirmation — v2
- Admin bulk entry actions — post-v1
- Multi-decoder support — post-v1 per REQUIREMENTS.md
