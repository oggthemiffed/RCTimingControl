# Results Export v1

Results Export v1 is the file RCTimingControl sends back to RaceHub with an event's results (#27). It is the return half of the boundary that RaceHub's Entry Export v1 starts: entries come in by import, and results go out in this file, keyed by the same RaceHub ids.

- The JSON Schema is `app/src/main/resources/resultsexport/results-export-v1.schema.json` (draft 2020-12).
- A worked example is `app/src/test/resources/resultsexport/results-export-v1-example.json`. A test checks that the app writes exactly that file for its example data and that it passes the schema, so the two can't drift apart.
- The Java model is `resultsexport/ResultsExportV1`, and the query that fills it is `query/resultsexport/ResultsExportQuery`.

## What it carries

Each export is the **whole event as it stands**, not just the race that changed. RaceHub can replace what it holds for the event with each one it accepts.

| Part | Contents |
|------|----------|
| Top level | `schema_version` (always `1`), `revision`, `generated_at`, and `source` (`system` and the club's name) |
| `event` | `racehub_event_id` (the RaceHub event the entries were imported from), `rctc_event_id`, `name`, `date`, `status` |
| `races[]` | Every finished race, in running order: its class (`rctc_event_class_id`, the `racehub_event_class_ids` its entries were booked under, and `class_name`), `round_type`, `round_number`, `heat_number`, `final_letter`, `status` (`FINISHED` or `ABANDONED`) and `finished_at` |
| `races[].results[]` | One row per finisher: `position`, `laps`, `total_time_ms`, `best_lap_ms`, `car_number`, `display_name`, the RaceHub ids, RCTC's own ids and the `penalties` given in that race |
| `championships[]` | For each championship the event is a round of: its `round_number`, and per class the full `standings` as they stand now, with this event's position and points for each driver and whether they were dropped or excluded |

Points belong to championship rounds, not to single races, so they appear only in `championships`.

### Ids

A result or standing row for an entry imported from RaceHub has `external_source: "RACEHUB"`, and carries RaceHub's `entry_id`, `driver_profile_id` and `racehub_event_class_id`. The schema requires `entry_id` and `driver_profile_id` on such a row.

A walk-in added at the track has `external_source: null` and null RaceHub ids. It is identified by `rctc_entry_id` and `rctc_competitor_id` instead. Every row carries the RCTC ids, so RaceHub can match a walk-in across exports.

### What it leaves out

No contact details, date of birth, guardian or payment data: RCTC doesn't hold any. No laps or passings: RaceHub gets summaries only (open question O6 in the local-only plan defaults to summaries).

### Abandoned races

An abandoned race is listed with `status: "ABANDONED"`. Its `results` are the order when it was abandoned, for information only.

## When it is sent

Only events whose entries were imported from RaceHub are sent, since those are the ones with a `racehub_event_id`. An export is queued:

| When | `reason` |
|------|----------|
| A race finishes or is abandoned | `RACE_FINISHED` |
| A finished race is corrected: a referee gives a penalty or the race director adjusts laps | `CORRECTION` |
| The event is marked `COMPLETED` (the race day is closed) | `DAY_CLOSE` |

> **Known gap.** A race's laps, times and positions come from the result stored when it finished, and a later correction doesn't change that stored result yet. A correction export lists a new penalty in `penalties`, but a lap adjustment after the finish doesn't show in it at all. Until that is fixed (#63), RaceHub should apply `LAP` and `TIME` penalties itself if it re-ranks.

Every export for an event takes the next `revision`, starting at 1. A higher revision always replaces a lower one, so RaceHub should ignore an export whose revision is not higher than the one it already holds.

Exports wait in the `results_outbox` table. A background sender posts the oldest one that is due, every 15 seconds. Sending never holds up race control: a race finishes the same way whether RaceHub can be reached or not.

## How it is sent

```http
POST <rctiming.racehub.results-url>
Authorization: Bearer <rctiming.racehub.token>
Content-Type: application/json
Idempotency-Key: rctc-results-<racehub_event_id>-r<revision>

{ ...Results Export v1... }
```

- Any `2xx` answer means sent.
- Anything else, or no answer, means failed. The export is tried again after 30 seconds, then after a wait that doubles each time up to 30 minutes. The sender stops at the first failure each round, so exports for an event arrive in revision order.
- When a newer export for the same event is queued, any older one that hasn't gone yet is marked `SUPERSEDED` and never sent, because the newer one carries everything it did.
- The same revision may arrive more than once (for example when RaceHub saved it but its answer was lost). The `Idempotency-Key` and the revision both let RaceHub recognise a repeat and answer `2xx` without changing anything.

Without a URL, nothing is sent and exports wait in the queue until one is set. See [installing.md](installing.md#sending-results-to-racehub) for the settings.

## Seeing and downloading exports

Admins see every export, its status (waiting, failed, sent or superseded), the attempts and the last error under **Results to RaceHub** in the admin panel, and can send a waiting one at once. An event's page has a **Download results** button that saves the file as it stands now, for any event, imported from RaceHub or not. See [api.md](api.md#admin--results-to-racehub) for the endpoints.

## Changing the format

A change that a reader of v1 could misread needs `schema_version` 2. Adding a field is not such a change: the schema allows fields it doesn't name, and a reader should ignore any it doesn't know. Update the schema, the example file and this page together.
