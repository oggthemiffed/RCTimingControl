---
phase: "02"
plan: "05"
subsystem: racer-portal-ui
tags: [frontend, react, routing, profile, cars, tanstack-query, rhf, zod, shadcn]
depends_on: ["02-01", "02-02", "02-03"]
provides:
  - App.tsx nested router: /racer parent with children (profile, cars, transponders, entries)
  - RacerPortalLayout: responsive shell with top-nav (md+) and bottom-nav (mobile)
  - ProfilePage: profile display, edit form with dirty tracking, memberships CRUD, read-only class ratings
  - CarsPage: responsive card grid with empty state, Sheet-based inline edit and create
  - CarCard: accessible clickable card with keyboard support and tag badges
  - CarEditSheet: Sheet form with RHF + Zod, create/edit/archive modes
  - useProfile, useUpdateProfile, useAddMembership, useRemoveMembership hooks
  - useCars, useCreateCar, useUpdateCar, useArchiveCar hooks
  - racerQueryKeys.ts: centralized query key constants
  - racerApi.ts: typed axios wrappers for all racer REST endpoints
  - Placeholder pages: TranspondersPage, EntriesPage, EventSchedulePage (replaced in Plan 06)
affects:
  - frontend/src/App.tsx
  - frontend/src/pages/racer/
  - frontend/src/pages/events/
  - frontend/src/components/racer/
  - frontend/src/hooks/racer/
  - frontend/src/lib/racerApi.ts
tech_stack:
  added: []
  patterns:
    - TanStack Query v5 hooks with onSettled cache invalidation (not onSuccess/onError)
    - isPending (not isLoading) for all query and mutation pending states
    - Zod schema keeps primaryClassId as string in form state; parsed to number in onSubmit (RHF transform compatibility)
    - Separate membership form (useForm) alongside profile form in same component
    - Placeholder components in same directory as real pages so router compiles before Plan 06
key_files:
  created:
    - frontend/src/App.tsx (modified)
    - frontend/src/pages/racer/RacerPortalLayout.tsx
    - frontend/src/pages/racer/ProfilePage.tsx
    - frontend/src/pages/racer/CarsPage.tsx
    - frontend/src/pages/racer/TranspondersPage.tsx
    - frontend/src/pages/racer/EntriesPage.tsx
    - frontend/src/pages/events/EventSchedulePage.tsx
    - frontend/src/components/racer/CarCard.tsx
    - frontend/src/components/racer/CarEditSheet.tsx
    - frontend/src/hooks/racer/useProfile.ts
    - frontend/src/hooks/racer/useCars.ts
    - frontend/src/hooks/racer/racerQueryKeys.ts
    - frontend/src/lib/racerApi.ts
  modified:
    - frontend/src/App.tsx
decisions:
  - "primaryClassId kept as string in CarForm (Zod schema no .transform()) — RHF generic type parameter resolution breaks when Zod output type differs from input type (TFieldValues constraint); parse to number manually in onSubmit"
  - "node_modules installed in worktree for build verification — worktree does not inherit main repo node_modules; used npm install --prefer-offline; node_modules excluded from commit via .gitignore"
  - "Placeholder pages (TranspondersPage, EntriesPage, EventSchedulePage) created in Task 1 alongside real pages so router compiles immediately after Plan 05; Plan 06 replaces content"
  - "CarsPage empty state uses 02-UI-SPEC.md heading/body copy: 'No cars added' + descriptive body; generic 'No cars yet' from plan action block overridden by spec"
  - "Top nav active link uses 'text-primary font-medium border-b-2 border-primary pb-0.5' per D-06 spec (border underline indicator)"
metrics:
  duration_minutes: 5
  completed_date: "2026-04-17"
  tasks_completed: 3
  tasks_total: 3
  files_created: 13
  files_modified: 1
---

# Phase 02 Plan 05: Racer Portal React UI Summary

**One-liner:** React router rewritten to nested /racer children, responsive portal shell (top/bottom nav), ProfilePage with full profile + memberships CRUD + read-only ratings, CarsPage with Sheet inline edit/create/archive, all backed by TanStack Query v5 hooks.

## What Was Built

### Task 1: Router, Layout, Query Keys, API Helpers, Placeholders

**App.tsx router rewrite** — Replaced the `/racer/*` wildcard entry with a nested structure:

```typescript
{
  path: '/racer',
  element: <ProtectedRoute><RacerPortalLayout /></ProtectedRoute>,
  children: [
    { index: true, element: <Navigate to="/racer/profile" replace /> },
    { path: 'profile', element: <ProfilePage /> },
    { path: 'cars', element: <CarsPage /> },
    { path: 'transponders', element: <TranspondersPage /> },
    { path: 'entries', element: <EntriesPage /> },
  ],
},
{ path: '/events', element: <EventSchedulePage /> },
```

**RacerPortalLayout** — Responsive portal shell using Tailwind responsive utilities:
- Desktop (md+): horizontal top nav with `hidden md:flex`, active link `text-primary font-medium border-b-2 border-primary`
- Mobile (below md): fixed bottom nav with `fixed bottom-0 md:hidden h-14`, active item `text-primary`
- Main content padded `pb-16 md:pb-0` to clear bottom nav on mobile

**racerQueryKeys.ts** — Centralized constants: `profile`, `memberships`, `cars`, `transponders`, `entries`, `eventSchedule`

**racerApi.ts** — Typed axios wrappers: `fetchProfile`, `patchProfile`, `fetchMemberships`, `addMembership`, `removeMembership`, `fetchCars`, `createCar`, `updateCar`, `archiveCar`. All interfaces exported: `RacerProfileDto`, `MembershipDto`, `ClassRatingDto`, `UpdateRacerProfileRequest`, `UpsertMembershipRequest`, `CarDto`, `CreateCarRequest`, `UpdateCarRequest`.

### Task 2: ProfilePage + useProfile Hooks

**Hook file** — Four exports all using TanStack Query v5 conventions:
- `useProfile()` — `useQuery` with `racerQueryKeys.profile`
- `useUpdateProfile()` — `useMutation` with `onSettled` invalidation
- `useAddMembership()` — `useMutation` with `onSettled` invalidation
- `useRemoveMembership()` — `useMutation` with `onSettled` invalidation

**ProfilePage sections:**
1. **Profile** — Email (disabled, read-only), firstName + lastName (grid), phoneticName, phoneNumber (tel), emergency contact name + phone (tel). Save button disabled when `!isDirty`. CTA: "Save changes".
2. **Governing body memberships** — List with Trash2 remove buttons + inline add form (body code + number). 409 → toast "Already registered with this body."
3. **Ability ratings** — Read-only list with visual progress bar (`w-[{rating}%] bg-primary/30`) and Badge. No edit controls.

**Threat mitigation T-02-05-01 applied:** `UpdateRacerProfileRequest` type has no `email`, `roles`, `id`, or `passwordHash` fields. TypeScript prevents adding them. `profile!.email` is only bound to a disabled read-only Input, never sent in the PATCH body.

### Task 3: CarsPage + CarCard + CarEditSheet + useCars Hooks

**useCars hooks** — `useCars`, `useCreateCar`, `useUpdateCar`, `useArchiveCar` all invalidating `racerQueryKeys.cars` on `onSettled`.

**CarCard** — Accessible card: `role="button"`, `tabIndex={0}`, `onKeyDown` (Enter/Space). Shows name, `Class #N` if primaryClassId set, up to 3 tag badges plus overflow count badge.

**CarEditSheet** — Sheet from right side (`side="right"`). Handles both modes:
- Create: title "Add a car", submit "Add car"
- Edit: title "Edit {name}", submit "Save car", Archive button (destructive variant) with `window.confirm` dialog

**CarsPage** — Responsive grid `grid-cols-1 md:grid-cols-2 lg:grid-cols-3`. Loading: 3 skeleton cards. Empty state: "No cars added" heading + descriptive body per 02-UI-SPEC.md. Add car button at top-right.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Zod transform() on primaryClassId caused RHF generic type constraint failure**
- **Found during:** Task 3 (first build attempt)
- **Issue:** `z.string().transform(v => Number(v))` changes the inferred output type from `string` to `number | null`. RHF's `useForm<CarForm>` then has `TFieldValues` with `primaryClassId: number | null`, which is incompatible with the form's `Control` type constraint requiring all fields to be bindable as string inputs. TypeScript emitted multiple type assignment errors.
- **Fix:** Removed `.transform()` from the schema. `primaryClassId` stays as `string | undefined` in `CarForm`. Added a `.refine()` for numeric validation. Manually parsed to `Number(v)` in `onSubmit` before building the API payload.
- **Files modified:** `CarEditSheet.tsx`
- **Commit:** 89b6896

## Shadcn Components Consumed

| Component | Used In | Import Path |
|-----------|---------|-------------|
| Button | All pages, sheets, forms | `@/components/ui/button` |
| Card, CardHeader, CardTitle, CardContent | ProfilePage (3 cards), CarCard | `@/components/ui/card` |
| Form, FormField, FormItem, FormLabel, FormControl, FormMessage | ProfilePage (2 forms), CarEditSheet | `@/components/ui/form` |
| Input | All form fields | `@/components/ui/input` |
| Sheet, SheetContent, SheetHeader, SheetTitle, SheetFooter | CarEditSheet | `@/components/ui/sheet` |
| Badge | CarCard tags, ProfilePage memberships + ratings | `@/components/ui/badge` |
| Separator | ProfilePage (profile/emergency divider + membership section), CarEditSheet | `@/components/ui/separator` |
| Sonner (toast) | All mutation success/error feedback | `sonner` |

## Copywriting Decisions (from 02-UI-SPEC.md)

| Element | String Used | Source |
|---------|-------------|--------|
| Profile save CTA | "Save changes" | 02-UI-SPEC.md copywriting table |
| Profile save success | "Profile updated" | 02-UI-SPEC.md copywriting table |
| Empty memberships | "No memberships on file. Add your governing body membership number below." | 02-UI-SPEC.md copywriting table |
| Empty ability ratings | "No ability ratings assigned yet." | 02-UI-SPEC.md copywriting table |
| Empty cars heading | "No cars added" | 02-UI-SPEC.md copywriting table |
| Empty cars body | "Add a car to get started — you'll be able to record your setup and use it for event entries." | 02-UI-SPEC.md copywriting table |
| Add car CTA | "Add car" | 02-UI-SPEC.md copywriting table |
| Save car CTA | "Save car" | 02-UI-SPEC.md copywriting table |
| Car save success | "Car saved" | 02-UI-SPEC.md copywriting table |
| Car archive success | "{name} archived" | 02-UI-SPEC.md copywriting table |

## Scope Deferred to Plan 06

| Deferred Item | Reason |
|---------------|--------|
| TranspondersPage full implementation | Plan scope boundary; placeholder created |
| EntriesPage full implementation | Plan scope boundary; placeholder created |
| EventSchedulePage full implementation | Plan scope boundary; placeholder created |
| Full tag CRUD UX in CarEditSheet | RACER-10/11 deferred; CarCard displays existing tags read-only; CarEditSheet handles name + primaryClassId + notes only |

## Known Stubs

| Stub | File | Reason |
|------|------|--------|
| `TranspondersPage` | `frontend/src/pages/racer/TranspondersPage.tsx` | Placeholder for Plan 06; renders "Transponders — coming in Plan 06." |
| `EntriesPage` | `frontend/src/pages/racer/EntriesPage.tsx` | Placeholder for Plan 06; renders "Entries — coming in Plan 06." |
| `EventSchedulePage` | `frontend/src/pages/events/EventSchedulePage.tsx` | Placeholder for Plan 06; renders "Event Schedule — coming in Plan 06." |

These stubs do not block Plan 05's goal (Profile + Cars UI). Plan 06 replaces them with full implementations.

## Threat Surface Scan

No new threat surface introduced. All mutations go through the existing `api` axios instance with Bearer token. No `dangerouslySetInnerHTML` used anywhere. `UpdateRacerProfileRequest` type enforces T-02-05-01 (no email/roles in PATCH body). T-02-05-04 enforced by `ProtectedRoute` wrapping `RacerPortalLayout`.

## Self-Check

All 13 created/modified files verified present. All 3 task commits verified:
- d47dd31 — feat(02-05): App.tsx router rewrite + RacerPortalLayout + query keys + racerApi + placeholders
- b082168 — feat(02-05): ProfilePage + useProfile hooks
- 89b6896 — feat(02-05): CarsPage + CarCard + CarEditSheet + useCars hooks

## Self-Check: PASSED
