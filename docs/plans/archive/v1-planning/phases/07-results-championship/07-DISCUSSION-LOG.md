# Phase 7: Results & Championship - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-01
**Phase:** 07-results-championship
**Areas discussed:** Public results access, Racer history page, Car tags in printed results, Championship standings public page

---

## Public Results Access

| Option | Description | Selected |
|--------|-------------|----------|
| New public route /results/:raceId | Clean public URL, no login required. Reuses PrintResultsPage component. | ✓ |
| Unprotect existing race-control route | Make /race-control/event/:eventId/results/:raceId publicly accessible. | |
| Via event schedule page only | Results only accessible by navigating from /events. | |

**User's choice:** New public route /results/:raceId

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — link from event schedule | Once race FINISHED, results link appears on /events | ✓ |
| No — direct link only | Results accessible via URL but not discoverable from schedule | |

**User's choice:** Yes — link from event schedule

| Option | Description | Selected |
|--------|-------------|----------|
| Expandable per-racer row | Click to expand and see all lap times | ✓ |
| Always visible below results table | All lap times always shown in second table | |
| You decide | Leave to implementer | |

**User's choice:** Expandable per-racer row

---

## Racer History Page

| Option | Description | Selected |
|--------|-------------|----------|
| List of events, each expandable | Grouped by event, click to see race results | ✓ |
| Flat list of races chronologically | Every race as individual rows | |
| You decide | Leave structure to implementer | |

**User's choice:** List of events the racer entered, each expandable to show race results

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — each race links to /results/:raceId | Racer can tap to see full results | ✓ |
| No — show racer's own data only | Self-contained, no navigation away | |

**User's choice:** Yes — link to public results page

---

## Car Tags in Printed Results

| Option | Description | Selected |
|--------|-------------|----------|
| All car tags for entered car | All key/value pairs from entry snapshot | ✓ |
| Admin selects which tag categories | Configurable per category | |

**User's choice:** All car tags

| Option | Description | Selected |
|--------|-------------|----------|
| Club settings page | Global toggle in Admin → Club settings | ✓ |
| Per-event setting | Each event has its own toggle | |
| You decide | Leave location to implementer | |

**User's choice:** Club profile / settings page (global toggle)

---

## Championship Standings Public Page

| Option | Description | Selected |
|--------|-------------|----------|
| New public route /championships/:id | Clean public URL, separate from admin page | ✓ |
| Make admin page publicly accessible | Unprotect /admin/championships/:id | |
| Link from event schedule only | No dedicated URL | |

**User's choice:** New public route /championships/:id

| Option | Description | Selected |
|--------|-------------|----------|
| Standings with drop scores visible | Per-round scores, dropped rounds distinguished | ✓ |
| Standings totals only | Driver and total points only | |

**User's choice:** Standings table with drop scores visible (best-to-worst order per CHAMP-10)

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — championship linked from events | Standings link on event schedule if event is in a championship | ✓ |
| No — direct link only | Not surfaced from event schedule | |

**User's choice:** Yes — championship linked from associated events

---

## Claude's Discretion

- Exact visual styling of drop-score indicator (greyed out vs struck through vs badge)
- Pagination or infinite scroll on racer history
- Whether public championship page reuses ChampionshipStandingsTable component or builds its own

## Deferred Ideas

None — discussion stayed within phase scope.
