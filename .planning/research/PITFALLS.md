# Domain Pitfalls: RC Club Timing and Management System

**Domain:** RC club race timing, AMB/MyLaps decoder integration, championship scoring
**Researched:** 2026-04-15
**Confidence note:** External search and WebFetch unavailable in this environment.
Findings derive from training-data knowledge of AMB/MyLaps protocol implementations,
race timing system codebases, Spring WebSocket internals, and sports scoring logic.
Confidence levels reflect that limitation — treat MEDIUM items as requiring validation
against current MyLaps documentation before implementation.

---

## Critical Pitfalls

Mistakes that cause rewrites, data loss, or trust-destroying race result errors.

---

### Pitfall C1: TCP Stream Treated as Message-Delimited

**What goes wrong:** The MyLaps decoder speaks a binary TCP protocol. Developers assume
each `read()` call on the socket delivers exactly one complete record. In reality TCP is a
stream — a single read can return a partial record, two merged records, or anything in between.
Code that assumes record-at-a-time behaviour silently drops or misparses passing data,
causing phantom laps, missed laps, or garbage timing values.

**Why it happens:** Java's `InputStream.read()` returns "however many bytes are available."
A new developer sees records arriving cleanly during testing (low network load, small packets)
and never triggers the boundary case. Production traffic — many transponders, close passes —
produces back-to-back records in one TCP segment.

**Consequences:** Missed transponder hits (lap not counted), corrupted lap times, race result
errors. Extremely hard to debug because failures are non-deterministic.

**Prevention:**
- Implement a proper framing layer. Accumulate bytes into a `ByteBuffer`. Parse only when a
  complete record is available according to the protocol's length or delimiter fields.
- Wrap the socket read loop in a dedicated thread reading into a ring buffer or `ByteArrayOutputStream`,
  then pass complete frames to a parser.
- Write a unit test that feeds the parser a valid record split across multiple byte arrays at
  every possible split point. This must pass before any race use.

**Warning signs:**
- "Occasionally misses a lap" reports that cannot be reproduced in testing.
- Log entries showing garbled transponder IDs (values far outside valid range).
- Lap count in system differs from decoder's own display.

**Phase:** Decoder integration phase (Phase 1 or equivalent). This is a day-one decision —
get the framing layer correct before building anything on top.

**Confidence:** HIGH — this is a fundamental TCP property, not MyLaps-specific.

---

### Pitfall C2: No Reconnection Loop — One Disconnect Kills the Race

**What goes wrong:** The TCP connection to the decoder is established at application start or
when a race begins. If the decoder reboots, the network cable is bumped, or a WiFi blip occurs,
the socket throws an exception. Without automatic reconnection, timing stops for the rest of the
event. Officials have no indication anything is wrong until a racer complains about missing laps.

**Why it happens:** Happy-path implementation. Developers test with a permanently-connected
decoder. The failure mode only appears on-site at a race meeting.

**Consequences:** Silent timing loss mid-race. Partial race results. Loss of confidence in the
system. May require re-running heats.

**Prevention:**
- Implement an exponential-backoff reconnection loop as a first-class architectural component,
  not an afterthought. The decoder connection must be modelled as "always trying to be connected"
  with state: CONNECTED, RECONNECTING(attempt=N, next_retry=T).
- Surface connection state visibly in the race control UI — a persistent status indicator
  (green/amber/red). Officials must be able to see at a glance whether the decoder is live.
- Buffer the reconnection loop independently of race state. A reconnection must not reset race
  timers or clear accumulated lap data.
- On reconnect, re-send any subscription or initialisation commands the decoder requires to
  resume sending hit records.

**Warning signs:**
- No connection state indicator in the UI design.
- Decoder connection established once at startup with no retry logic.
- Exception handler for socket errors that just logs and returns.

**Phase:** Decoder integration phase. Connection resilience is not a "nice to have" — it is
a correctness requirement for the race control client.

**Confidence:** HIGH — general distributed systems principle, universally applicable here.

---

### Pitfall C3: Decoder Clock vs Server Clock Drift Corrupts Lap Times

**What goes wrong:** The MyLaps decoder records passing timestamps using its own internal
clock. The server assigns receipt timestamps when it processes the TCP message. If there is
any processing delay, network delay, or clock skew between the decoder and the server, lap
times computed as `(server receipt time) - (previous server receipt time)` will be wrong.

**Why it happens:** Developers use `System.currentTimeMillis()` when storing a passing event,
not the timestamp embedded in the decoder's protocol record. During testing with zero load,
processing delay is negligible. Under real race load (many transponders, WebSocket broadcasts,
database writes all happening concurrently) processing can lag 50–200 ms, which is significant
for RC lap times that may be as short as 10–15 seconds.

**Consequences:** Lap times appear slightly different from the decoder's own display. In close
races, the fastest lap winner may be incorrect. Championship points decided by fastest lap are
wrong.

**Prevention:**
- Always use the timestamp provided by the decoder in the protocol record, not the server
  clock. Parse and store the decoder-native timestamp as the canonical passing time.
- Use the server receipt time only as a sanity check (flag records received more than N seconds
  after their decoder timestamp as suspicious, possible replay or clock jump).
- If the decoder provides a UTC timestamp, store it directly. If it provides only a
  session-relative offset (common in some AMB protocol variants), ensure the session base time
  is captured at race start and never drifted by server-side corrections.

**Warning signs:**
- Lap times computed as difference between `Instant.now()` calls rather than parsed decoder timestamps.
- No unit tests comparing parsed lap time to raw byte sequence.

**Phase:** Decoder integration phase. Must be correct before timing data is trusted.

**Confidence:** MEDIUM — timestamp handling varies by MyLaps protocol version. Validate against
current decoder documentation. The principle (use decoder time, not server time) is HIGH confidence.

---

### Pitfall C4: Duplicate Transponder Hits Not Deduplicated

**What goes wrong:** A transponder crossing the finish loop often triggers multiple reads in
rapid succession (the transponder is in range for ~50–100 ms, the decoder may record several
hits). If all hits are inserted as laps, racers accumulate phantom laps. The first hit is the
real crossing; subsequent hits are noise.

**Why it happens:** The protocol forwards all readings. Some implementations naively insert
every received record without checking whether a hit from the same transponder already arrived
within a deduplication window.

**Consequences:** Double or triple lap counts. Race ending prematurely (racer "completes" more
laps than the race distance). Incorrect fastest-lap records.

**Prevention:**
- Apply a per-transponder deduplication window at the point of ingestion (before any race
  state logic). Typical window: 1–3 seconds. Discard any hit from transponder T if a hit from
  T was already recorded within the window.
- Make the window configurable — RC car lap times vary dramatically by track (8 seconds to
  60+ seconds). A window suited to one track may be wrong for another.
- Log discarded duplicate hits at DEBUG level for post-race diagnostics.
- Write a unit test: feed two hits from the same transponder 0.5 s apart and assert only one
  lap is created. Feed two hits 5 s apart and assert two laps are created.

**Warning signs:**
- Lap count in system occasionally exceeds physical lap count.
- No deduplication logic visible in the decoder ingestion layer.

**Phase:** Decoder integration phase. Deduplication is part of the "raw hit → lap record"
translation layer.

**Confidence:** HIGH — standard problem in all transponder timing systems.

---

### Pitfall C5: Race State Machine Has No Authoritative Owner

**What goes wrong:** Race state (READY, RUNNING, FINISHING, ENDED) is stored in multiple
places — the server, the browser session, the WebSocket message payload — and they diverge.
Two race control browser windows are open; one starts the race. The second window, not having
received the state update (it connected 50 ms before the start broadcast), still shows READY
and lets the operator issue a second "start" command.

**Why it happens:** State is treated as a UI concern. The server validates commands but does
not hold the canonical state machine; it trusts whatever the client sends.

**Consequences:** A race is "started" twice, resetting the start timestamp. Lap zero is
computed incorrectly. Race results cannot be reproduced.

**Prevention:**
- The server owns the race state machine. Period. State transitions happen only when the server
  validates and applies a command. The server rejects duplicate start/stop commands that are
  invalid for the current state.
- Race state is derived from the database / in-memory store — not from the WebSocket session.
  A newly connected client receives current state on connect, not a default.
- Implement a server-side state machine with explicit transition guards:
  `READY → start() → RUNNING` (guard: state == READY)
  `RUNNING → finish() → FINISHING`
  `FINISHING → end() → ENDED`
  Each transition is atomic and persisted before broadcasting.
- Command handlers are idempotent where possible (issuing "stop" on an already-stopped race
  is a no-op, not an error).

**Warning signs:**
- Race start timestamp set by the browser rather than the server at the moment of command
  acceptance.
- No guard condition on state transition commands.
- Two open browser tabs can produce inconsistent UI state.

**Phase:** Race control phase. Design the state machine before building the UI.

**Confidence:** HIGH — general principle; particularly important in multi-client WebSocket systems.

---

### Pitfall C6: WebSocket Broadcast Blocks the Timing Ingestion Thread

**What goes wrong:** The thread reading from the decoder TCP socket is also responsible for
broadcasting lap events over WebSocket to all connected clients. When many clients are
connected, or a client is on a slow connection, the broadcast blocks. The decoder socket
read loop stalls. TCP receive buffer fills up. The decoder interprets the stall as a network
problem and may close the connection or, worse, silently drop records.

**Why it happens:** Simplest implementation: decoder thread → parse → broadcast → loop. Works
with one or two browser clients in a local test. Falls over at a real event with 10 spectator
devices connected.

**Consequences:** Missed transponder hits under broadcast load. Timing gaps during busy
moments (race start, large group on track).

**Prevention:**
- Decouple decoder ingestion from broadcast delivery using an in-process queue (e.g.,
  `java.util.concurrent.LinkedBlockingQueue` or Spring's `ApplicationEventPublisher`).
- Decoder thread: read → parse → enqueue. Separate broadcast thread: dequeue → send to
  WebSocket sessions.
- The decoder read loop must never block on IO operations other than the decoder socket itself.
- Size the queue with a bound and a drop policy (newest wins) so a stalled WebSocket client
  cannot cause unbounded memory growth.

**Warning signs:**
- Decoder read and WebSocket send happen in the same method with no queue between them.
- Performance degrades as the number of connected browser clients increases.

**Phase:** Decoder integration + race control phases. The queue architecture must be
established in the decoder integration phase; broadcast scaling is validated when the race
control UI is built.

**Confidence:** HIGH — standard producer/consumer problem.

---

## Moderate Pitfalls

---

### Pitfall M1: Transponder-to-Car Assignment Changed Mid-Race

**What goes wrong:** A racer's transponder falls off, is swapped, or is reassigned to a
different car between heats (or worse, during a heat). The timing system resolves transponder
IDs to car numbers at ingestion time. If the assignment changes mid-race, earlier laps belong
to the new assignment, not the old one, producing scrambled results.

**Why it happens:** The racer portal allows transponder reassignment at any time. The timing
system does not snapshot assignments at race start.

**Consequences:** A racer's lap record is attributed to the wrong car, or is lost entirely.
Requires manual result correction.

**Prevention:**
- Snapshot transponder-to-car-to-racer assignments at race start. Store the snapshot with
  the race record. Transponder resolution during a race uses the snapshot, not the live
  assignment table.
- Post-race, the admin can correct assignments via a result amendment flow — but the snapshot
  is the source of truth for the automated count.
- The racer portal must warn (or block) transponder changes when the racer is entered in a
  heat that is RUNNING or FINISHING.

**Warning signs:**
- Transponder resolution queries the live assignment table at lap ingestion time.
- No race-start snapshot concept in the data model.

**Phase:** Racer portal + decoder integration intersection. Identify the snapshot mechanism
in the data model during the integration phase; enforce the portal restriction when the
self-service portal is built.

**Confidence:** HIGH — this is a classic racing data integrity problem.

---

### Pitfall M2: Duplicate Transponder Registration

**What goes wrong:** Racer A registers transponder #12345. Racer B also registers transponder
#12345 (duplicate physical transponder number, or a data entry error). Both are entered in the
same race. Every passing of #12345 is credited to... both? The first match? The last one? The
behaviour is undefined and the results are wrong.

**Why it happens:** No uniqueness constraint on transponder ID within an event.

**Consequences:** Laps attributed to wrong racer. Results uncorrectable without admin intervention.

**Prevention:**
- Database unique constraint on (transponder_id, event_id) or globally on (transponder_id)
  if a transponder can only be used by one racer per meeting.
- At race entry confirmation, validate that no other entered racer in the same race/class holds
  the same transponder.
- Show a clear error in the racer portal: "Transponder #12345 is already registered to [Name]
  for this event."
- Admin override should be available but require explicit acknowledgement.

**Warning signs:**
- No UNIQUE constraint on transponder ID at the entry or event level.
- Transponder validation done only at UI layer, not enforced in the database.

**Phase:** Racer portal phase.

**Confidence:** HIGH — straightforward data integrity requirement.

---

### Pitfall M3: "Best X from Y" Scoring Breaks on DNF / DNS / DQ

**What goes wrong:** The championship scoring algorithm counts only finished results toward
the "best X from Y" pool. A racer who DNS (did not start) a round has fewer than Y results —
the algorithm must handle this without crashing or producing negative points. A racer who DQs
gets zero points for that round, but that zero is still a countable result (not excluded from
the pool). DNS may be excluded from the pool altogether (only races entered count as rounds).
Different clubs have different rules. Getting this wrong silently awards incorrect standings.

**Why it happens:** Happy-path implementation: sort scores descending, take top X. This breaks
when result arrays are shorter than X, when DQ results are excluded from the pool instead of
included as zeros, or when the club's interpretation of "rounds entered" differs from "rounds
held."

**Consequences:** Incorrect championship winner. Loss of trust. Retroactive corrections
required after standings are published.

**Prevention:**
- Model all result states explicitly: FINISHED, DNF, DNS, DQ, NOT_ENTERED.
- Document the club's scoring rule as an explicit policy: does DNS count as a round toward Y?
  Does DQ score zero and count? Is a missing result (not entered) treated as DNS?
- Encode the policy in a dedicated scoring service, tested exhaustively with edge cases:
  racer with fewer than Y rounds, racer with all DNFs, tie on points with identical best-lap
  tiebreaker, etc.
- Make the DNF/DNS/DQ treatment configurable per championship, not hard-coded.

**Warning signs:**
- Scoring logic written as a single SQL query with no explicit status filtering logic.
- No test cases for racers with partial result sets.
- "Best X from Y" implemented as `ORDER BY points DESC LIMIT X` without status awareness.

**Phase:** Championship scoring phase. Write the scoring tests before writing the scoring code.

**Confidence:** HIGH — this class of bug is endemic to sports scoring systems.

---

### Pitfall M4: Late Result Corrections Do Not Invalidate Cached Standings

**What goes wrong:** Race results are corrected after the event (marshal lap added, DQ issued).
The championship standings shown on the public website are derived from a cached or
pre-computed standings table. The correction updates the race result but the standings cache
is not invalidated. The public sees incorrect standings for hours or days.

**Why it happens:** Standings computation is expensive (scan all results for all racers across
all rounds), so it is computed once and cached. The cache invalidation path is not connected
to the result amendment flow.

**Prevention:**
- Treat championship standings as a derived view, not a stored entity.
- On any result amendment (marshal lap, status change, DQ), invalidate and recompute standings
  for the affected championship.
- For performance, recompute asynchronously but mark standings as "pending recomputation"
  immediately — the UI shows a "recalculating..." indicator rather than stale data.
- Alternatively, compute standings on-demand with a short TTL cache (acceptable for small
  clubs where standings are queried infrequently).

**Warning signs:**
- Standings stored in a `championship_standing` table that is only updated at event close.
- No cache invalidation logic connected to result amendment operations.

**Phase:** Championship scoring phase + results publication phase.

**Confidence:** HIGH — cache invalidation is a well-known class of bug; this domain has a
specific trigger (result amendments) that is easy to miss.

---

### Pitfall M5: Race Control Client Loses Connection — Race Continues Uncontrolled

**What goes wrong:** The race control browser loses its WebSocket connection to the server
mid-race (network blip, laptop sleep). The server continues running the race (accepting
transponder hits, counting laps). When the client reconnects, it does not know the current
race state and prompts the operator to "start a race." The operator cannot tell whether the
ongoing race needs to be stopped.

**Why it happens:** Race state not surfaced to a reconnecting client. No "reconnect and resume"
flow. The operator must guess.

**Consequences:** Operator accidentally starts a second race. Timing continues for the wrong
heat. Results require manual correction.

**Prevention:**
- On WebSocket reconnect, the server immediately sends full current race state to the newly
  connected session: current race, laps completed per car, time elapsed, race phase.
- The race control UI enters a "reconnecting" state during the blip, then restores to the
  correct race view on reconnect — not to the default "ready" view.
- Server-side: track "which race control sessions are connected." If zero sessions are connected
  for more than N seconds during a RUNNING race, flag an alert (surfaced when the next session
  connects). Do not stop the race automatically — that would be worse.

**Warning signs:**
- WebSocket disconnect handler clears server-side race state.
- No "send state on connect" handler.
- Race control UI always initialises from a blank/ready state.

**Phase:** Race control phase.

**Confidence:** HIGH.

---

### Pitfall M6: In-Memory Lap Data Lost on Server Restart

**What goes wrong:** Accumulated lap records for a running race are stored only in memory
(e.g., a `Map<RaceId, List<LapRecord>>`). The server process is restarted (crash, deploy,
power failure). All lap data for ongoing races is lost. The race cannot be resumed.

**Why it happens:** Pursuing simplicity. Persisting every lap record on receipt adds latency.
Developers defer persistence "until later" and the architecture never gets revisited.

**Consequences:** Lost race results. Re-running heats. If this happens during a championship
final the club loses confidence in the system entirely.

**Prevention:**
- Persist every accepted lap record to the database synchronously (or in a durable write-ahead
  queue) before acknowledging it. For RC club scale (dozens of transponders, not thousands)
  the insert latency is negligible.
- Keep the in-memory structure as a read cache, not the source of truth.
- Test: kill the server mid-race, restart, verify all recorded laps are recoverable.

**Warning signs:**
- `List<LapRecord>` held in a singleton Spring bean with no persistence.
- Database schema has no lap_record table (laps only computed post-race).

**Phase:** Decoder integration phase (data model decision).

**Confidence:** HIGH.

---

## Minor Pitfalls

---

### Pitfall m1: Entry Deadline Edge Cases With Timezone Ambiguity

**What goes wrong:** Event entry deadline stored as a date without timezone. The server runs
in UTC. Racers see a deadline of "midnight Sunday" which the server interprets as UTC midnight
— three hours earlier than the club's local time. Racers who try to enter at 11 PM local time
find the entry closed.

**Prevention:**
- Store all deadlines as UTC timestamps, not dates.
- Display deadlines in the user's local timezone (browser-side formatting with `Intl.DateTimeFormat`).
- Admin UI for entering deadlines should include the club's timezone explicitly and preview
  the UTC equivalent.

**Phase:** Racer portal phase.

**Confidence:** HIGH.

---

### Pitfall m2: Spring WebSocket SockJS Heartbeat Masking Broken Connections

**What goes wrong:** Spring's SockJS implementation sends heartbeat frames. If the client
does not respond (tab backgrounded, laptop lid closed), the server-side session appears alive.
The server may accumulate thousands of "connected" sessions that are actually zombies. Broadcast
time grows linearly with session count, including dead sessions. Memory grows unboundedly.

**Prevention:**
- Configure `setHeartbeatTime` appropriately and set a send timeout: connections that fail to
  deliver a frame within N seconds are closed.
- Use Spring's `WebSocketSession.isOpen()` check before sending, and close sessions that throw
  on send.
- Monitor active session count as a health metric.

**Phase:** Race control / results display phase.

**Confidence:** MEDIUM — behaviour depends on Spring WebSocket version and SockJS transport
selected. Validate against current Spring documentation.

---

### Pitfall m3: Lap Number Off-by-One at Race Start

**What goes wrong:** A transponder hit received before the race officially starts (car sitting
on the start line, rolling across the loop to take position) is counted as Lap 1. All
subsequent lap numbers are offset by one. The race ends one lap early.

**Prevention:**
- Lap records received before the race enters RUNNING state must be discarded or flagged as
  pre-start hits — never counted as race laps.
- Implement an explicit warm-up window: hits received within N seconds of race start that
  are clearly pre-start passes (same transponder crossing immediately before the start signal)
  are excluded.
- Log all discarded pre-start hits for race official review.

**Phase:** Decoder integration phase (lap ingestion rules).

**Confidence:** HIGH — classic problem in loop-based timing.

---

### Pitfall m4: Marshal Lap Adjustment Breaks Fastest Lap Calculation

**What goes wrong:** An official adds a marshal lap to compensate for a track incident (car
stopped, given a free lap). The marshal lap is inserted as a synthetic record with a very short
or zero duration. Fastest lap calculation picks the marshal lap as the fastest lap of the race.

**Prevention:**
- Marshal laps must be flagged with a `synthetic = true` marker.
- Fastest lap calculation must exclude synthetic laps.
- All result displays (leaderboard, results sheet) must clearly mark marshal laps with a
  visual indicator so officials can verify correctness.

**Phase:** Race control phase (marshal lap feature).

**Confidence:** HIGH.

---

### Pitfall m5: Concurrent Championship Score Recalculation Race Condition

**What goes wrong:** Two result amendments arrive near-simultaneously (marshal lap added for
Car A, DQ applied to Car B). Two async championship recalculation tasks are triggered. Both
read the results table at roughly the same time, compute standings, and write. One overwrites
the other's computation with partially-stale data.

**Prevention:**
- Championship recalculation must be serialised per championship — use a single-threaded
  executor per championship ID, or a database advisory lock, or an optimistic version column
  on the standings table.
- Alternatively, treat standings as fully on-demand computed (no async job) — compute from
  scratch on every read with a short cache TTL.

**Phase:** Championship scoring phase.

**Confidence:** HIGH — standard concurrency problem; specific trigger is domain-specific.

---

## Phase-Specific Warning Summary

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Decoder TCP integration | Stream framing (C1) | ByteBuffer accumulator + split-boundary unit tests |
| Decoder TCP integration | Connection resilience (C2) | Reconnection loop as first-class component |
| Decoder TCP integration | Clock source (C3) | Use decoder timestamp, not server clock |
| Decoder TCP integration | Duplicate hits (C4) | Per-transponder deduplication window |
| Decoder TCP integration | Thread model (C6) | Producer/consumer queue between ingestion and broadcast |
| Decoder TCP integration | Lap before race start (m3) | Discard pre-start hits explicitly |
| Decoder TCP integration | Data model (M6) | Persist every lap to DB on receipt |
| Race control UI | State machine ownership (C5) | Server owns state; client is read-only view |
| Race control UI | Client reconnect (M5) | Send full state on WebSocket connect |
| Race control UI | WebSocket zombie sessions (m2) | Send timeout + `isOpen()` guard |
| Race control UI | Marshal lap fastest-lap (m4) | Synthetic flag on marshal laps |
| Racer portal | Mid-race transponder swap (M1) | Snapshot assignments at race start |
| Racer portal | Duplicate transponder (M2) | DB unique constraint + entry validation |
| Racer portal | Entry deadline timezone (m1) | Store UTC, display local |
| Championship scoring | DNF/DNS/DQ edge cases (M3) | Explicit result status model + exhaustive tests |
| Championship scoring | Stale standings after correction (M4) | Invalidate on result amendment |
| Championship scoring | Concurrent recalculation (m5) | Serialised executor or on-demand compute |

---

## Sources

All findings from training-data knowledge of:
- AMB/MyLaps decoder TCP protocol (X2/P3/Orbits protocol family)
- General TCP stream framing (IETF RFC-level behaviour of TCP byte-stream semantics)
- Spring WebSocket / SockJS documentation patterns
- Sports scoring system post-mortems and design literature
- Race timing system codebases (open-source implementations observed in training data)

**External verification recommended before implementation:**
- MyLaps developer portal for current decoder protocol specification and timestamp format
- Spring Framework current release notes for WebSocket/SockJS configuration
- Confirm deduplication window values against actual decoder pass-width behaviour for RC hardware
