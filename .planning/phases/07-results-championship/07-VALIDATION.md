---
phase: 7
slug: results-championship
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-05-01
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito + Testcontainers (backend); Vitest + React Testing Library (frontend) |
| **Config file** | `app/src/test/` hierarchy; `frontend/vitest.config.ts` |
| **Quick run command** | `./gradlew :app:test --tests "*Championship*" --tests "*Result*"` |
| **Full suite command** | `./gradlew :app:test && cd frontend && npm test` |
| **Estimated runtime** | ~60 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:test --tests "*Championship*" --tests "*Result*"`
- **After every plan wave:** Run `./gradlew :app:test && cd frontend && npm test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 7-W0-01 | W0 | 0 | RESULT-01 | — | N/A | unit stub | `./gradlew :app:test --tests "*PublicResultsControllerTest*"` | ❌ W0 | ⬜ pending |
| 7-W0-02 | W0 | 0 | CHAMP-05 | — | N/A | unit stub | `./gradlew :app:test --tests "*PublicChampionshipControllerTest*"` | ❌ W0 | ⬜ pending |
| 7-W0-03 | W0 | 0 | CHAMP-05 | — | N/A | unit stub | `./gradlew :app:test --tests "*ChampionshipStandingsQueryTest*"` | ❌ W0 | ⬜ pending |
| 7-W0-04 | W0 | 0 | RESULT-03 | — | N/A | unit stub | `./gradlew :app:test --tests "*RacerResultHistoryQueryTest*"` | ❌ W0 | ⬜ pending |
| 7-xx-01 | TBD | 1+ | RESULT-01 | T-7-01 | Public endpoint returns 404 (not 403) for unknown race IDs | integration | `./gradlew :app:test --tests "*PublicResultsControllerTest*"` | ❌ W0 | ⬜ pending |
| 7-xx-02 | TBD | 1+ | RESULT-02 | — | N/A | integration | Covered by existing Phase 4 ResultSnapshotService tests | ✅ | ⬜ pending |
| 7-xx-03 | TBD | 1+ | RESULT-03 | T-7-02 | History uses Authentication.getName() — no IDOR via request param | integration | `./gradlew :app:test --tests "*RacerResultHistoryQueryTest*"` | ❌ W0 | ⬜ pending |
| 7-xx-04 | TBD | 1+ | RESULT-04 | — | N/A | unit | `./gradlew :app:test --tests "*ResultSnapshotQueryCarTagTest*"` | ❌ W0 | ⬜ pending |
| 7-xx-05 | TBD | 1+ | RESULT-05 | — | N/A | integration | Covered by RESULT-01 test — verify lapHistory non-empty | Within W0-01 | ⬜ pending |
| 7-xx-06 | TBD | 1+ | CHAMP-05 | T-7-01 | Public endpoint returns 404 (not 403) for unknown championship | integration | `./gradlew :app:test --tests "*PublicChampionshipControllerTest*"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/public/PublicResultsControllerTest.java` — stubs for RESULT-01, RESULT-05
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/api/public/PublicChampionshipControllerTest.java` — stubs for CHAMP-05
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQueryTest.java` — stubs for CHAMP-05 (drop logic, bonuses)
- [ ] `app/src/test/java/dev/monkeypatch/rctiming/query/results/RacerResultHistoryQueryTest.java` — stubs for RESULT-03

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Expandable lap time rows work on mobile (tap to expand) | RESULT-05 | Touch interaction not testable in Vitest | Open `/results/:raceId` on a mobile device or DevTools mobile emulation; tap a racer row and verify lap times expand |
| Drop scores visually distinguished (grey/strikethrough) | CHAMP-05 | Visual rendering | Open `/championships/:id`; verify dropped rounds are visually distinct from counted rounds |
| Car tags visible beneath name in print view when enabled | RESULT-04 | Print layout | Enable admin setting, open results page, use browser print preview, verify car tags appear under racer name |
| No admin controls visible on public championship page | CHAMP-05 | Visual / security | Open `/championships/:id` without login; verify no management controls (edit, delete, configure) are rendered |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
