---
phase: 9
slug: user-manual-documentation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-05-15
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest 4.1.5 + React Testing Library 16.3.2 |
| **Config file** | `frontend/vite.config.ts` (test section) |
| **Quick run command** | `cd frontend && npm test -- --run` |
| **Full suite command** | `cd frontend && npm test -- --run` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd frontend && npm test -- --run`
- **After every plan wave:** Run `cd frontend && npm test -- --run`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** ~30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 9-01-01 | 01 | 0 | SC-1 | — | N/A | unit | `cd frontend && npm test -- --run HelpContext` | ❌ W0 | ⬜ pending |
| 9-01-02 | 01 | 0 | SC-2 | — | N/A | unit | `cd frontend && npm test -- --run MeetingGuidePage` | ❌ W0 | ⬜ pending |
| 9-01-03 | 01 | 0 | SC-3 | — | N/A | unit | `cd frontend && npm test -- --run RacerGuidePage` | ❌ W0 | ⬜ pending |
| 9-01-04 | 01 | 0 | SC-4 | — | N/A | unit | `cd frontend && npm test -- --run AdminGuidePage` | ❌ W0 | ⬜ pending |
| 9-xx-xx | later | 1 | SC-1 | — | N/A | unit | `cd frontend && npm test -- --run RaceControlLayout` | ✅ extend | ⬜ pending |
| 9-xx-xx | later | 1 | SC-1 | — | N/A | unit | `cd frontend && npm test -- --run AdminPanelLayout` | ✅ extend | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `frontend/src/context/HelpContext.test.tsx` — stubs for SC-1 (HelpProvider renders, useHelp hook throws outside provider)
- [ ] `frontend/src/pages/print/MeetingGuidePage.test.tsx` — stubs for SC-2 (renders title + print button)
- [ ] `frontend/src/pages/print/RacerGuidePage.test.tsx` — stubs for SC-3
- [ ] `frontend/src/pages/print/AdminGuidePage.test.tsx` — stubs for SC-4

Existing tests to extend (not Wave 0 — extend in relevant plan waves):
- [ ] `frontend/src/pages/admin/__tests__/AdminPanelLayout.test.tsx` — add '?' button assertion
- [ ] `frontend/src/pages/race-control/` — create `RaceControlLayout.test.tsx` stub

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Help drawer slides out on '?' click | SC-1 | DOM animation / Sheet open state requires browser interaction | Open race control, click '?', confirm drawer opens with page-specific content |
| Help drawer shows correct content per page | SC-1 | Content accuracy requires human judgement | Navigate to each wired page, open help, verify content matches the page |
| Print dialog triggered by Ctrl+P | SC-2/3/4 | Browser print API cannot be tested in Vitest | Open /print/meeting-guide in browser, Ctrl+P, confirm print layout renders cleanly |
| Documentation accuracy vs implemented features | SC-5 | Accuracy requires human review of prose content | David reviews all help articles and guide sections for factual accuracy |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
