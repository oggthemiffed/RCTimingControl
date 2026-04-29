---
phase: 06-audio-practice
plan: "06"
subsystem: frontend/audio,frontend/practice,frontend/admin
tags: [react, typescript, stomp, web-speech-api, tts, practice-session, live-timing, shadcn-ui, tanstack-query]
dependency_graph:
  requires: ["06-03", "06-04", "06-05"]
  provides: ["audio-settings-panel", "practice-session-page", "admin-audio-page", "voice-selection-ui", "use-announcements", "use-practice-timing"]
  affects: ["CockpitPage", "ProfilePage", "App.tsx", "AdminPanelLayout"]
tech_stack:
  added: ["slider.tsx (Radix UI Slider)", "alert.tsx (minimal Alert/AlertDescription)"]
  patterns: ["TanStack Query v5 useQuery/useMutation", "dual-STOMP-subscription in usePracticeTiming", "AudioContext beep synthesis", "Web Speech API fallback", "React Hook Form + Zod"]
key_files:
  created:
    - frontend/src/lib/audioApi.ts
    - frontend/src/lib/practiceApi.ts
    - frontend/src/hooks/race-control/useAnnouncements.ts
    - frontend/src/hooks/race-control/usePracticeTiming.ts
    - frontend/src/pages/race-control/panels/AudioSettingsPanel.tsx
    - frontend/src/pages/race-control/panels/PracticeLiveTable.tsx
    - frontend/src/pages/race-control/dialogs/PracticeCreateDialog.tsx
    - frontend/src/pages/race-control/PracticeSessionPage.tsx
    - frontend/src/pages/race-control/PrintPracticeResultsPage.tsx
    - frontend/src/pages/admin/club/AdminAudioSettingsPage.tsx
    - frontend/src/pages/admin/racers/AdminRacerDetailPage.tsx
    - frontend/src/components/ui/slider.tsx
    - frontend/src/components/ui/alert.tsx
  modified:
    - frontend/src/pages/racer/ProfilePage.tsx
    - frontend/src/pages/race-control/CockpitPage.tsx
    - frontend/src/App.tsx
    - frontend/src/pages/admin/AdminPanelLayout.tsx
    - frontend/src/pages/race-control/panels/AudioSettingsPanel.test.tsx
    - frontend/src/pages/race-control/PracticeSessionPage.test.tsx
    - frontend/src/pages/admin/club/AdminAudioSettingsPage.test.tsx
    - frontend/src/test/setup.ts
decisions:
  - "PracticeSessionPage is a standalone route (not nested under RaceControlLayout) — practice sessions are not event-scoped"
  - "AudioSettingsPanel uses /api/v1/race-control/settings/audio (PATCH) not localStorage — settings persist across browser sessions"
  - "usePracticeTiming uses two separate useStomp calls (timing + unknown-transponder) — follows existing single-topic pattern, accepts two WS connections"
  - "Slider component created inline (Radix UI) as shadcn add was unavailable in CI — identical to other radix-luma components"
  - "Alert component created minimal (no Radix dependency) — not in existing shadcn setup, only needed for simple info banner"
  - "ResizeObserver mocked globally in test/setup.ts — Radix UI Slider requires it in jsdom"
  - "AdminRacerDetailPage uses inline confirm state (not AlertDialog) — avoids need for alert-dialog.tsx component"
metrics:
  duration: "~15 minutes"
  completed: "2026-04-28T21:07:21Z"
  tasks_completed: 4
  files_changed: 21
---

# Phase 6 Plan 06: Audio + Practice Frontend Summary

**One-liner:** Frontend layer for Phase 6 — AudioSettingsPanel (5 toggle switches, volume, Web Speech API), practice session live timing page with STOMP + REST seed, admin audio settings with blocklist management, voice selection in racer profile, and admin phonetic name override.

## What Was Built

### Task 1: API Modules and Hooks

- **`audioApi.ts`** — Full audio API client:
  - `listVoices()` → GET /api/v1/audio/voices → VoiceInfo[]
  - `previewNameClip(voice?)` → GET /api/v1/audio/preview?voice={id} → audio/wav
  - `getAudioSettings/patchAudioSettings` → GET/PATCH /api/v1/race-control/settings/audio
  - `getAdminAudioSettings/saveAdminAudioSettings` → GET/PUT /api/v1/admin/audio/settings
  - `getBlocklist/addBlocklistTerm/removeBlocklistTerm` → blocklist CRUD
  - `saveVoicePreference` → PUT /api/v1/racer/audio/voice
  - `getRaceClipMap(raceId)` → GET /api/v1/race/{id}/audio-clips

- **`practiceApi.ts`** — Practice session API client:
  - `createSession/getSession/listSessions` — CRUD
  - `startSession/stopSession` — state transitions
  - `getSnapshot/getResults` — timing data
  - `linkTransponder` — link unknown transponders

- **`useAnnouncements.ts`** — Audio hook (AUDIO-04, AUDIO-06, AUDIO-11):
  - Subscribes to `/topic/race/{id}/audio` STOMP topic for running-order events
  - `fallbackSpeak(text)` — Web Speech API synthesis (AUDIO-11)
  - `playBeep(improving)` — AudioContext oscillator beeps (AUDIO-04): 880Hz improving, 440Hz not
  - `testAudio()` — speaks test sentence
  - Uses refs to avoid stale closures on volume changes

- **`usePracticeTiming.ts`** — Practice live timing hook:
  - Dual STOMP subscriptions: `/topic/practice/{id}/timing` and `/topic/practice/{id}/unknown-transponder`
  - Seeds from REST snapshot via TanStack Query
  - Returns `{ rows, unknownTransponders, isLoading }`

### Task 2: AudioSettingsPanel + ProfilePage Voice Section + CockpitPage Wiring

- **`AudioSettingsPanel.tsx`** — Collapsible panel in CockpitPage sidebar:
  - 5 toggle switches (announceCountdown, announceStagger, announceLapBeep, announceFinish, announceRunningOrder)
  - Volume slider (0-100%) with localStorage persistence (`rc-audio-volume`)
  - Status dot: green (all enabled) → yellow (some enabled) → red (none enabled)
  - "▶ Test Audio" button calls `testAudio()` via `useAnnouncements`
  - Fetches/PATCHes /api/v1/race-control/settings/audio via TanStack Query

- **`ProfilePage.tsx`** additions:
  - New "Announcement Voice" Card after ability ratings
  - Voice selector dropdown (GET /api/v1/audio/voices)
  - "Preview" button: fetches GET /api/v1/audio/preview?voice={id}, plays via HTML5 Audio
  - "Save Voice Preferences" button

- **`CockpitPage.tsx`** additions:
  - Imports and renders `<AudioSettingsPanel>` in the run-order sidebar
  - AUDIO-04 beep wiring: `useLiveTiming` + `useAnnouncements` + `prevPassingRef` to detect new laps and call `playBeep(improving)`

### Task 3: Practice UI Components + Admin Audio Page + Routes

- **`PracticeLiveTable.tsx`** — TanStack Table-style table:
  - Columns: Pos, Racer, Laps, Best Lap, Best N Laps (avg), Last Lap, Status
  - Unknown transponder rows highlighted; "Unknown" badge
  - `fmtMs()` helper: m:ss.mmm or ss.mmm format

- **`PracticeCreateDialog.tsx`** — shadcn Dialog:
  - React Hook Form + Zod: session name (required) + bestLapN (default 3)
  - Calls `createSession()` and triggers `onCreated(session)` callback

- **`PracticeSessionPage.tsx`** — Full-page practice management:
  - IDLE: empty state with "Start Practice" CTA
  - RUNNING: live table + unknown transponder banner + Stop button
  - STOPPED: live table + New Session + Print Results buttons
  - `usePracticeTiming` for live data

- **`PrintPracticeResultsPage.tsx`** — Print-optimised results page:
  - Shows session name, best-N, end timestamp
  - Results table: Pos/Racer/Laps/BestLap/BestNConsec/LastLap
  - Print button (print:hidden)

- **`AdminAudioSettingsPage.tsx`** — Admin audio management:
  - 5 announcement toggles + default voice selector
  - Profanity blocklist table: add form + per-row delete button
  - "Save Settings" button

- **`App.tsx`** — New routes:
  - `/race-control/practice/:sessionId` → PracticeSessionPage
  - `/race-control/practice/:sessionId/print` → PrintPracticeResultsPage
  - `/admin/audio` → AdminAudioSettingsPage
  - `/admin/racers/:userId` → AdminRacerDetailPage

- **`AdminPanelLayout.tsx`** — "Audio Settings" nav item in Configuration group (Volume2 icon)

### Task 4: AdminRacerDetailPage — Phonetic Override (Surface 2)

- **`AdminRacerDetailPage.tsx`** — Admin phonetic name management:
  - Fetches GET /api/v1/admin/audio/racer/{userId}/phonetic
  - Phonetic name input with Save button (PUT)
  - "Clear & Regenerate Clip" with inline confirm step (PUT phonetic=null + DELETE /name-clip)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] slider.tsx missing from project**
- **Found during:** Task 2 (AudioSettingsPanel)
- **Issue:** `@/components/ui/slider` did not exist. `npx shadcn@latest add slider` timed out (no internet in CI-like environment)
- **Fix:** Created `slider.tsx` inline using `radix-ui` Slider primitive (same pattern as other components)
- **Files modified:** `frontend/src/components/ui/slider.tsx`
- **Commit:** 75ac168

**2. [Rule 3 - Blocking] alert.tsx missing from project**
- **Found during:** Task 3 (PracticeSessionPage)
- **Issue:** `@/components/ui/alert` did not exist
- **Fix:** Created minimal `Alert/AlertDescription` components using div + role="alert"
- **Files modified:** `frontend/src/components/ui/alert.tsx`
- **Commit:** 3587fac

**3. [Rule 3 - Blocking] ResizeObserver not defined in jsdom**
- **Found during:** Task 2 tests (AudioSettingsPanel.test.tsx)
- **Issue:** Radix UI Slider uses `@radix-ui/react-use-size` which calls `new ResizeObserver()`, not available in jsdom
- **Fix:** Added `globalThis.ResizeObserver = class ResizeObserver { ... }` mock to `src/test/setup.ts`
- **Files modified:** `frontend/src/test/setup.ts`
- **Commit:** 75ac168

**4. [Rule 1 - Bug] Zod .default() on number field caused React Hook Form type error**
- **Found during:** Build check
- **Issue:** `z.number().default(3)` changes the inferred type in a way incompatible with RHF's Control generic, causing TS2322 errors
- **Fix:** Removed `.default(3)` from schema; `defaultValues: { bestLapN: 3 }` in `useForm` provides the default
- **Files modified:** `frontend/src/pages/race-control/dialogs/PracticeCreateDialog.tsx`
- **Commit:** 3587fac

**5. [Rule 1 - Bug] global not valid TypeScript global name**
- **Found during:** Build check
- **Issue:** `global.ResizeObserver = ...` failed with TS2304 "Cannot find name 'global'" — Node.js global, not available in TS strict mode
- **Fix:** Changed to `globalThis.ResizeObserver = ...` (valid in all environments)
- **Files modified:** `frontend/src/test/setup.ts`
- **Commit:** 3587fac

## Verification Results

| Check | Result |
|-------|--------|
| `npm run build` | ✅ BUILD SUCCESSFUL (✓ built in 736ms) |
| `npm test -- --run` (all 30 tests) | ✅ 6 test files, 30 tests passed |
| AudioSettingsPanel.test.tsx (5 tests) | ✅ No describe.skip |
| PracticeSessionPage.test.tsx (5 tests) | ✅ No describe.skip |
| AdminAudioSettingsPage.test.tsx (5 tests) | ✅ No describe.skip |
| TypeScript strict mode | ✅ No errors |

## Known Stubs

None. All API calls are wired to real backend endpoints (from plans 03-05). Practice live timing subscribes to real STOMP topics. Audio preview fetches real WAV bytes.

## Threat Flags

None. All new routes are wrapped in `<ProtectedRoute>` with appropriate roles. No new API endpoints introduced in frontend (all consume existing backend endpoints). Audio/practice data is not sensitive (racer names, lap times).

## Self-Check: PASSED
