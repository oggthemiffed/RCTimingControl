---
status: pre-planning
source: brainstorm 2026-04-22
---

# Heat Structure Design — Phase 4 Input

Agreed design decisions to feed into Phase 4 (Race State Machine + Race Control API) planning.

## New Entities

### Round
One "pass" through all classes at the same type.

| Field | Type | Notes |
|---|---|---|
| `eventId` | FK | |
| `type` | `PRACTICE \| QUALIFIER \| FINAL` | |
| `roundNumber` | int | 1, 2, 3… within type |
| `sequenceInEvent` | int | Absolute run order position |
| `status` | `PENDING \| RUNNING \| COMPLETED` | |

### Race
One timed session on the track.

| Field | Type | Notes |
|---|---|---|
| `roundId` | FK | |
| `eventClassId` | FK | |
| `heatNumber` | int | Which split within this class (Heat 1, Heat 2…) |
| `sequenceInRound` | int | Run order within the round |
| `finalLetter` | varchar nullable | null for practice/qualifying; A/B/C for finals |
| `startType` | `STAGGER \| GRID` | PRACTICE/QUALIFIER → STAGGER; FINAL → GRID |
| `formatId` | FK nullable | null = inherit from EventClass |
| `formatOverrides` | jsonb | Patch on top of resolved format |
| `status` | `PENDING \| GRID \| RUNNING \| STOPPED \| FINISHED` | |

### RaceEntry
Which entries are in a specific race.

| Field | Type | Notes |
|---|---|---|
| `raceId` | FK | |
| `entryId` | FK | |
| `gridPosition` | int | Start position |
| `bumped` | boolean | Promoted from a lower final |

## EventClass — Finals Config (new fields)

Stored as additional columns or within `formatOverrides`:

- `finalsCount` — how many finals (1=A only, 2=A+B, 3=A+B+C…)
- `carsPerFinal` — e.g. 10
- `bumpCount` — how many promote from each final into the next (e.g. 2)

## Round Generator

Triggered by admin after entries close. Inputs:

1. Practice rounds (N)
2. Qualifying rounds (M)
3. Max cars per heat → determines number of heats per class per round
4. Finals config per class (finalsCount, carsPerFinal, bumpCount)

Creates all `Round` and `Race` records with correct sequencing.
Heat assignment (which cars are in Heat 1 vs Heat 2) is fixed at generation time and persists across all rounds.

## Start Order Rules

### Practice / Qualifying — STAGGER
- **Round 1**: random / entry-number order
- **Subsequent rounds**: finishing order from same heat in previous round — **best finisher goes first** (clean track advantage), slowest goes last

### Finals — GRID
- Pre-seeded from qualifying standings after all qualifying rounds complete
- Bump-up slots left empty until lower final finishes
- Auto-filled: top N finishers of lower final appended to back of higher final grid
- Admin can manually override any grid position before race starts

## Bump-Up Finals Seeding Algorithm

Example: 20 drivers, 10 per final, bumpCount=2, 2 finals (A+B).

```
Qualifying standings: rank 1 (fastest) → rank 20 (slowest)

B-Final initial grid (from qualifying):
  positions 1–10 → drivers ranked 11–20

B-Final runs. Top 2 finishers bump up.

A-Final grid:
  positions 1–8 → drivers ranked 1–8 (from qualifying)
  positions 9–10 → top 2 finishers of B-Final (appended after B-Final finishes)
```

With 3 finals (A+B+C), C runs first, top N bump to back of B, B runs, top N bump to back of A.
**A-Final grid cannot be finalised until B-Final completes.**

## Race Control Implications

- Race director steps through `sequenceInRound` (and `sequenceInEvent` for global ordering)
- After each QUALIFIER race completes, system recalculates qualifying standings
- After qualifying round closes, system auto-seeds finals grids (admin confirms/overrides)
- After each final completes, system auto-fills bump slots in next final and alerts race director

## Future Feature (TBD)
User noted one additional feature to be added later — not scoped here.
