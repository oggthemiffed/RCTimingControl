---
phase: 08-first-run-setup-wizard
plan: 06
subsystem: ui
tags: [react, typescript, vite, tanstack-query, zod, react-hook-form, shadcn, tailwind, decoder, completion]

requires:
  - phase: 08-04
    provides: SetupLayout wizard shell, useSetupProgress hook, setupApi.ts
  - phase: 08-03
    provides: PATCH /api/v1/setup/decoder-config, GET /api/v1/setup/forwarder-config-download
  - phase: 08-05
    provides: Steps 1-4 (ClubProfile, Track, Format, Staff), SetupLayout step orchestration

provides:
  - DecoderConfigStep: Step 5 — decoder host/protocol/port form, embedded token UX, .env download, 30s Test Connection polling
  - SetupCompletePage: Setup complete summary with 5 configured/skipped cards and Go to Admin Panel CTA
  - SetupLayout: fully orchestrates all 5 steps + completion screen; sentinels removed

affects:
  - Phase 08 closed — all 5 wizard steps + completion screen implemented

tech-stack:
  added: []
  patterns:
    - "Protocol→port auto-fill: useEffect on watch('decoderProtocol') + userEditedPortRef to preserve user edits"
    - "Test Connection polling: refetchInterval + attempts counter + dataUpdatedAt as useEffect dependency (not data reference — TanStack Query structural sharing)"
    - "Forwarder token as plaintext API key: stored in token_value column, validated by direct equality (not bcrypt)"
    - "forwarder.env download: embeds live token when one exists, sentinel string when none generated yet"

key-files:
  created:
    - frontend/src/pages/setup/steps/DecoderConfigStep.tsx
    - frontend/src/pages/setup/SetupCompletePage.tsx
    - app/src/main/resources/db/migration/V26__forwarder_token_plaintext.sql
  modified:
    - frontend/src/pages/setup/SetupLayout.tsx
    - frontend/src/pages/setup/__tests__/DecoderConfigStep.test.tsx
    - frontend/src/lib/raceControlApi.ts
    - app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderToken.java
    - app/src/main/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenService.java
    - app/src/main/java/dev/monkeypatch/rctiming/api/setup/SetupService.java
    - app/src/test/java/dev/monkeypatch/rctiming/forwarder/ForwarderTokenServiceTest.java
    - app/src/test/java/dev/monkeypatch/rctiming/api/setup/SetupControllerIT.java
    - app/src/test/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQueryTest.java

key-decisions:
  - "Token storage changed from bcrypt to plaintext — forwarder tokens are cryptographically random 32-byte API keys; bcrypt is inappropriate for high-entropy tokens and prevented embedding the token in the .env download"
  - "staff progress check changed to count >= 2 non-RACER users — bootstrap admin alone should not pre-complete Step 4; wizard intent is to create an additional operational staff account"
  - "Test Connection uses statusQuery.dataUpdatedAt as useEffect dependency instead of statusQuery.data — TanStack Query structural sharing returns the same object reference for identical DISCONNECTED responses, so data never changes reference even when a new fetch completes"
  - "RacerResultHistoryQueryTest isolation: TRUNCATE CASCADE in @BeforeEach is the correct fix for shared Testcontainers DB — @Transactional rollback doesn't help because PostgreSQL sequences don't roll back"

requirements-completed: [SC-2, SC-4, SC-5]

uat-approved: true
uat-date: 2026-05-14

duration: ~1.5h (including UAT feedback fixes)
completed: 2026-05-14
---

# Phase 8 Plan 06: Decoder Config Step + Completion Screen Summary

**Final wizard step (Decoder Config) and completion summary screen; UAT-confirmed end-to-end flow; forwarder token storage redesigned from bcrypt to plaintext for .env download UX; staff progress logic corrected**

## Performance

- **Duration:** ~1.5h
- **Started:** 2026-05-14
- **Completed:** 2026-05-14
- **Tasks:** 3 of 3 (including human checkpoint — UAT approved)
- **Files modified:** 9 (5 frontend, 4 backend + 1 migration)

## Accomplishments

- **DecoderConfigStep**: decoder host/protocol/port form with protocol→port auto-fill (RC4→5100, P3→5403); port preserved when user has manually edited it; embedded forwarder token UX (Generate/Regenerate with inline confirm, one-time reveal); blob download of forwarder.env; 30s Test Connection polling (15 attempts × 2s) showing Connected badge or timeout Alert; Save and Finish / Skip buttons
- **SetupCompletePage**: "Setup Complete" heading, 5 summary cards (Club Profile / Track / Race Format / Staff / Decoder) showing Configured or Skipped badge with Edit links, Go to Admin Panel primary button
- **SetupLayout**: case 5 → DecoderConfigStep, default (6+) → SetupCompletePage; sentinel placeholder strings removed
- **V26 migration**: added `token_value` column to `forwarder_token` table
- **ForwarderTokenService**: switched from bcrypt to plaintext storage in `token_value`; `validate()` uses direct equality; `getCurrentStatus()` exposes `tokenValue` for env generation
- **SetupService.generateForwarderEnv**: embeds live token or `<no-token-generate-one-first>` sentinel
- **SetupService.getProgress**: `staff` now requires `count >= 2` non-RACER users

## UAT Feedback Fixed

1. **Staff Step pre-completed**: Bootstrap admin (ADMIN role) was satisfying `staff = anyMatch(r != RACER)`. Fixed to `count >= 2` so wizard correctly shows Step 4 as incomplete on fresh setup.
2. **forwarder.env token placeholder**: Users expected the downloaded .env to contain their generated token. Root cause: bcrypt prevented plaintext recovery. Fixed by storing token as plaintext (API key semantics, not password semantics). Downloaded file now contains the actual token.
3. **Backend test failure (RacerResultHistoryQueryTest)**: Shared Testcontainers DB accumulates rows across test runs; sequences don't roll back with @Transactional. Fixed with `TRUNCATE ... CASCADE RESTART IDENTITY` in @BeforeEach.

## Task Commits

1. **Plan 06 implementation** — `87f4e4d` (feat: implement Step 5 Decoder Config and completion screen)
2. **UAT feedback fixes** — `c0b4ab3` (fix: embed token in forwarder.env; fix staff step pre-completion)
3. **Test fixes** — `11f8a70` (test: fix test isolation and update assertions for new token/staff design)

## Threat Model Updates

- T-08-03 retired: original mitigation (never store plaintext) was misapplied — forwarder tokens are API keys, not passwords. New design stores plaintext in `token_value`; bcrypt hash column retained for schema compatibility but no longer used for validation.

## Self-Check: PASSED

- `npm run build`: PASSED
- `npx vitest run src/pages/setup/__tests__/`: 9 tests passed (4 SetupGuard + 5 DecoderConfigStep)
- `./gradlew test`: 258 tests, 0 failed
- UAT full wizard flow: APPROVED 2026-05-14

---
*Phase: 08-first-run-setup-wizard*
*Completed: 2026-05-14*
