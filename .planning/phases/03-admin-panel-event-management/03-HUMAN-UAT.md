---
status: complete
phase: 03-admin-panel-event-management
source: [03-05-PLAN.md task 3, 03-06-PLAN.md task 4]
started: 2026-04-22
updated: 2026-04-22
---

## Setup

```
docker compose up -d postgres minio
./gradlew :app:bootRun
cd frontend && npm run dev
```

Log in as an admin account before starting.

Teardown after testing: Ctrl-C frontend and backend, then `docker compose down`.

## Tests

### 1. Admin layout navigation
expected: All sidebar links (Events, Championships, Tracks, Formats, Club Profile, Car Tags) navigate correctly; active link is highlighted; mobile hamburger opens sheet drawer
result: Pass

### 2. Event list loads and Create Event dialog works
expected: /admin/events shows event table; Create Event dialog opens; submitting creates a row
result: Pass

### 3. Event detail — state machine transitions
expected: Opening an event shows Overview/Classes/Entries tabs; transition buttons (Publish, Open Entries, etc.) advance status; invalid transitions show 409 toast
result: Pass

### 4. Event detail — add class and format overrides
expected: Classes tab shows Add Class dialog with racing class + template selects; added class appears with format summary; Edit Overrides dialog accepts JSON and saves
result: Pass

### 5. Championship list and create
expected: /admin/championships shows table; Create Championship dialog opens with name, scoringSource radio (QUALIFYING/FINALS/BOTH), bestX/Y fields, bonus point fields; submitting creates a row
result: Pass

### 6. Championship detail — 6 tabs visible
expected: Opening a championship shows exactly 6 tabs: Config, Classes, Events, Points Scale, Standings, Exclusions
result: Pass

### 7. Championship Config tab — save
expected: Editing name or scoringSource and clicking Save shows success toast; reload confirms changes persisted
result: Pass

### 8. Championship Points Scale — ROAR preset
expected: Click ROAR → table fills with 10 rows (20, 17, 15, 13, 12, 11, 10, 9, 8, 7); Save → toast; reload → ROAR values persist
result: Pass

### 9. Championship Points Scale — BRCA preset
expected: Click BRCA → table fills with 10 rows (100, 95, 91, 88, 85, 83, 81, 79, 77, 75); Save → toast; reload → BRCA values persist
result: Pass

### 10. Championship Events tab — link and duplicate round rejection
expected: Link Event dialog lets you select an event and set a round number; linking works; linking a second event to the same round number returns a 409 toast "Round X is already assigned"
result: Pass

### 11. Championship Classes tab — add and remove
expected: Add Class dialog shows racing class select + optional bestX/Y override fields; added class appears in table; Remove button deletes the row
result: Pass

### 12. Championship Exclusions tab — create and delete
expected: Add Exclusion dialog takes driver ID, event select, reason textarea; created exclusion appears with createdBy/createdAt; Delete removes the row
result: Pass — governing body membership numbers now shown alongside driver name in combobox and exclusions table (fixed post-UAT)

### 13. Championship Standings tab — empty state
expected: Standings tab renders "No standings available yet — race results not recorded" (Phase 3 scaffold behaviour — expected)
result: Pass

### 14. Club Profile — logo upload
expected: /admin/club shows logo card (placeholder if none); Upload Logo opens file picker restricted to image types; uploading a PNG shows preview; reload — logo persists
result: Pass — fixed post-UAT: logo upload failed when no profile had been saved yet; controller now auto-creates a blank singleton profile so upload works regardless of save order

### 15. Club Profile — profile form save
expected: Editing club name/email/timezone and clicking Save Profile shows success toast; reload confirms values persisted
result: Pass

### 16. Format templates — create with type-switcher
expected: /admin/formats Create Template dialog opens; changing Format Type from TIMED to BUMP_UP causes variant fields to switch with a fade-in animation; saving creates a row with the correct type shown
result: Pass — note: "copy from existing template" feature requested as future enhancement, backlogged

### 17. Tracks CRUD
expected: /admin/tracks shows track list (may be empty); Create Track dialog creates a row; Edit updates it; Delete removes it
result: Pass

### 18. Car tag categories — archive/unarchive flow
expected: /admin/categories shows active categories; Create Category adds a row; Delete (archive) removes it from the default list; toggling "Show archived" switch reveals the archived row with an Unarchive button; Unarchive restores it to the active list
result: Pass — fixed post-UAT: `control` was missing from useForm destructure in CategoryFormDialog causing crash on open; color picker (already implemented) now works correctly

## Summary

total: 18
passed: 18
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

- Format templates: "copy from existing template" as basis for new one — backlogged as future enhancement
