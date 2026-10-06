---
phase: 03-admin-panel-event-management
plan: 03-05
status: checkpoint
completed: 2026-04-21
tasks_total: 3
tasks_completed: 2
one-liner: "Admin panel frontend shell with events list/detail UX — TanStack Table, state-machine transitions, class management, and entry withdraw"
subsystem: frontend
tags: [react, tanstack-table, admin-ui, events, state-machine]
dependency_graph:
  requires:
    - "03-02 (EventController, AdminEventListDto, EventDetailDto, AdminEntryDto backend endpoints)"
    - "03-04 (MinIO/storage — not consumed by this plan but wave dependency)"
  provides:
    - "AdminPanelLayout (left sidebar desktop / Sheet drawer mobile)"
    - "EventListPage (/admin/events)"
    - "EventDetailPage (/admin/events/:id)"
    - "adminApi.ts — 11 typed REST functions"
    - "adminQueryKeys.ts — factory function pattern"
    - "Hook files: useAdminEvents, useAdminEventClasses, useAdminEntries"
  affects:
    - "frontend/src/App.tsx — admin routes replaced"
    - "Plan 06 (will add championships/club/tracks/formats/categories pages to the same AdminPanelLayout)"
tech_stack:
  added:
    - "@tanstack/react-table v8 (installed)"
  patterns:
    - "Factory function query keys (adminQueryKeys) — per plan spec for entity-id keys"
    - "TanStack Table with getCoreRowModel + getSortedRowModel"
    - "React Hook Form + Zod for all admin forms"
    - "Confirm dialog before destructive state transitions (D-05)"
    - "sonner toast for mutation feedback and 409 error handling"
key_files:
  created:
    - frontend/src/components/ui/table.tsx
    - frontend/src/components/ui/tabs.tsx
    - frontend/src/components/ui/checkbox.tsx
    - frontend/src/components/ui/radio-group.tsx
    - frontend/src/components/ui/switch.tsx
    - frontend/src/lib/adminApi.ts
    - frontend/src/hooks/admin/adminQueryKeys.ts
    - frontend/src/hooks/admin/useAdminEvents.ts
    - frontend/src/hooks/admin/useAdminEventClasses.ts
    - frontend/src/hooks/admin/useAdminEntries.ts
    - frontend/src/pages/admin/AdminPanelLayout.tsx
    - frontend/src/pages/admin/events/EventListPage.tsx
    - frontend/src/pages/admin/events/EventDetailPage.tsx
    - frontend/src/pages/admin/events/EventClassSection.tsx
    - frontend/src/pages/admin/events/EntryListSection.tsx
  modified:
    - frontend/src/App.tsx (AdminPlaceholderPage route replaced with AdminPanelLayout nested routes)
    - frontend/package.json (@tanstack/react-table added)
decisions:
  - "Used radix-ui unified package imports (not @radix-ui/* individual packages) to match existing component style"
  - "shadcn CLI not used — components written manually to match existing radix-ui import style"
  - "Plan 06 placeholder routes added (/admin/championships, /admin/tracks, etc.) so AdminPanelLayout nav links resolve without 404"
  - "Add Class dialog uses numeric ID inputs as placeholder — Plan 06 will replace with select dropdowns backed by tracks/formats API"
  - "EventDetailPage fires PUBLISHED/OPEN/ENTRIES_CLOSED/IN_PROGRESS/COMPLETED transitions; confirm dialogs only for ENTRIES_CLOSED and COMPLETED per UI-SPEC D-05"
metrics:
  duration_seconds: 330
  completed_date: 2026-04-21
  tasks_count: 2
  files_created: 15
  files_modified: 2
---

# Phase 03 Plan 05: Admin Panel Events UI Summary

Admin panel frontend shell and events/entries management UI. The existing `AdminPlaceholderPage` stub has been replaced with a fully functional admin panel.

## What Was Built

### Task 1 — Scaffolding

**shadcn UI components created** (manually, matching radix-ui unified package style):
- `table.tsx` — HTML table with shadcn style wrappers (TableHeader, TableBody, TableRow, TableHead, TableCell, TableCaption)
- `tabs.tsx` — Radix Tabs with TabsList, TabsTrigger, TabsContent
- `checkbox.tsx` — Radix Checkbox with RiCheckLine indicator
- `radio-group.tsx` — Radix RadioGroup with RiCircleFill indicator
- `switch.tsx` — Radix Switch with thumb animation

**`adminApi.ts`** — 11 typed REST functions over the shared `api` (axios + auth interceptor):
`listEvents`, `getEvent`, `createEvent`, `updateEvent`, `transitionEvent`, `listEventClasses`, `addEventClass`, `updateOverrides`, `combineClasses`, `listEntriesForClass`, `withdrawEntry`

**`adminQueryKeys.ts`** — factory function pattern (events, championships, club, tracks, formats, carTagCategories) — championships/club/tracks/formats/categories keys included for Plan 06 consumption.

**Hook files:**
- `useAdminEvents.ts`: `useAdminEventsList`, `useAdminEventDetail`, `useCreateAdminEvent`, `useUpdateAdminEvent`, `useTransitionEvent`
- `useAdminEventClasses.ts`: `useAddEventClass`, `useUpdateEventClassOverrides`, `useCombineClasses`
- `useAdminEntries.ts`: `useEntriesForClass`, `useWithdrawEntry`

**`AdminPanelLayout.tsx`** — Left sidebar (240px, desktop md+) with:
- Group 1: Events, Championships
- Group 2: Tracks, Formats, Club Profile, Car Tags
- Active route highlight: primary color + 3px left border
- Mobile: top bar hamburger → Sheet drawer from left + bottom nav (5 icons)
- User name/email + logout at sidebar bottom

**`App.tsx`** — `AdminPlaceholderPage` route replaced with `AdminPanelLayout` nested routes:
- `/admin` → redirect to `/admin/events`
- `/admin/events` → `EventListPage`
- `/admin/events/:id` → `EventDetailPage`
- `/admin/championships`, `/admin/tracks`, `/admin/formats`, `/admin/club`, `/admin/categories` → `AdminComingSoonPage` (Plan 06 placeholders)

### Task 2 — Events Admin Pages

**`EventListPage`** (EVENT-01, EVENT-07):
- TanStack Table with sortable ID + Date columns
- Status badge column with D-06 color map (DRAFT/PUBLISHED/OPEN/ENTRIES_CLOSED/IN_PROGRESS/COMPLETED)
- Name column clicks navigate to `/admin/events/:id`
- Create Event dialog (react-hook-form + zod, name + eventDate)
- Empty state: "No events yet" with Create Event CTA
- Loading skeletons; error state with Retry button

**`EventDetailPage`** (EVENT-05):
- VALID_NEXT map drives transition buttons — only valid next statuses rendered
- Transition button labels per UI-SPEC copywriting contract (Publish Event / Open Entries / Close Entries / Start Event / Complete Event)
- Confirm dialogs for Close Entries (destructive) and Complete Event (default) per D-05
- HTTP 409 → toast "This transition is no longer valid" + refetch
- Tabs: Overview (edit form, disabled for non-DRAFT), Classes, Entries
- Back navigation to events list

**`EventClassSection`** (EVENT-02, EVENT-06):
- Add Class dialog (racingClassId + templateId numeric inputs — Plan 06 will replace with selects)
- Per-class cards with config snapshot summary (type/duration/laps/heats)
- Overrides badge if configOverride present; Combined badge if combinedRaceGroup set
- Edit Overrides dialog: JSON textarea with parse validation, amber left-border override indicator
- Multi-select combine: checkboxes + "Combine into Shared Race (N)" button + confirm dialog

**`EntryListSection`** (ENTRY-02):
- Empty state if no classes
- Class selector tabs/buttons when multiple classes
- Per-class table: Racer, Transponder, Status badge, Submitted, Actions
- Withdraw button for PENDING/CONFIRMED entries → confirm dialog with reason textarea
- Status badges: PENDING=yellow, CONFIRMED=green, WITHDRAWN=zinc

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] shadcn CLI not used; components written manually**
- **Found during:** Task 1
- **Issue:** The `npx shadcn@latest add` CLI would have generated components using `@radix-ui/*` individual packages, but the project uses the `radix-ui` unified package (v1.4.3). Using the CLI would have broken the import style established by all existing components (dialog.tsx, select.tsx, sheet.tsx, etc.).
- **Fix:** Created all 5 components manually following the `import { ComponentName } from "radix-ui"` pattern from existing components.
- **Files modified:** table.tsx, tabs.tsx, checkbox.tsx, radio-group.tsx, switch.tsx

**2. [Rule 2 - Missing dependency] @tanstack/react-table not installed**
- **Found during:** Task 2
- **Issue:** EventListPage requires TanStack Table (`useReactTable`, `flexRender`) but `@tanstack/react-table` was not in package.json.
- **Fix:** `npm install @tanstack/react-table`
- **Files modified:** frontend/package.json, frontend/package-lock.json

**3. [Rule 1 - Bug] TypeScript verbatimModuleSyntax type import errors**
- **Found during:** Task 1 build verification
- **Issue:** `useAdminEvents.ts` and `useAdminEventClasses.ts` imported types without `import type` syntax, violating `verbatimModuleSyntax` tsconfig option used in this project.
- **Fix:** Changed type-only imports to `import type { ... }` syntax.
- **Files modified:** useAdminEvents.ts, useAdminEventClasses.ts

**4. [Rule 1 - Bug] AdminPanelLayout bottom nav TypeScript comparison error**
- **Found during:** Task 1 build verification
- **Issue:** `end={to === '/admin'}` comparison was flagged as unintentional — the union type of nav `to` values does not include '/admin'.
- **Fix:** Changed to `end={false}` for bottom nav items (exact matching not needed there).
- **Files modified:** AdminPanelLayout.tsx

## Known Stubs

| Stub | File | Reason |
|------|------|--------|
| Add Class uses numeric ID inputs | EventClassSection.tsx | Racing class and format template select dropdowns require `/api/v1/admin/racing-classes` and `/api/v1/admin/formats` endpoints — Plan 06 will wire these. |
| Event track field not wired | EventDetailPage.tsx | Track select requires `/api/v1/admin/tracks` — Plan 06. `updateEvent` passes `trackId: data?.trackId ?? null` to preserve existing value. |
| `AdminComingSoonPage` placeholder | App.tsx | Championships, tracks, formats, club, categories routes all show "Coming in Plan 06" — they exist so nav links don't 404. |

## Threat Surface Scan

No new trust boundaries introduced beyond the plan's threat model. All admin API calls flow through the shared `api.ts` axios instance which attaches Bearer tokens. Client-side route guard (`ProtectedRoute roles={['ADMIN','RACE_DIRECTOR','REFEREE']}`) is cosmetic; backend enforces `@PreAuthorize` per threat T-03-40.

## Checkpoint Status

Task 3 is `checkpoint:human-verify` — awaiting manual verification. All automated checks pass:
- `npm run build` exits 0
- `npm run lint` exits 0 (3 warnings in pre-existing files and known TanStack Table/React Compiler warning)

## Self-Check: PASSED

Files exist:
- frontend/src/pages/admin/events/EventListPage.tsx: FOUND
- frontend/src/pages/admin/events/EventDetailPage.tsx: FOUND
- frontend/src/pages/admin/events/EventClassSection.tsx: FOUND
- frontend/src/pages/admin/events/EntryListSection.tsx: FOUND
- frontend/src/lib/adminApi.ts: FOUND
- frontend/src/hooks/admin/adminQueryKeys.ts: FOUND

Commits:
- f0fa9e5: feat(03-05): admin panel scaffolding
- a85d4a6: feat(03-05): events admin UI pages
