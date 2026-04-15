# Requirements: RCTimingControl

**Defined:** 2026-04-15
**Updated:** 2026-04-15
**Core Value:** Racers can enter events online and manage their own car/transponder details, while officials run a full race meeting from any Windows or Linux machine — with live timing fed directly from AMB/MyLaps hardware via a local forwarder agent to a cloud-hosted service.

## v1 Requirements

### Authentication

- [ ] **AUTH-01**: Racer can self-register with email and password
- [ ] **AUTH-02**: Racer can log in and remain logged in across browser sessions
- [ ] **AUTH-03**: Racer can reset password via email link
- [ ] **AUTH-04**: Admin can log in with elevated privileges to access admin panel and race control

### Racer Profile & Equipment

- [ ] **RACER-01**: Racer can create and edit their profile (name, contact details)
- [ ] **RACER-02**: Racer can add and edit cars (name, primary class, notes); primary class is used for filtering entries and is not a hard constraint
- [ ] **RACER-03**: Racer owns transponders independently of cars; transponder is selected at entry time, not permanently assigned to a car
- [ ] **RACER-04**: Racer can view their own entry history and past race results
- [ ] **RACER-05**: Transponder numbers are unique system-wide; duplicate registration is rejected and flagged for admin resolution
- [ ] **RACER-06**: Cars are archived not deleted; archived cars preserve race history and cannot be used for new entries
- [ ] **RACER-07**: Entry records a transponder snapshot at submission time; subsequent changes to the racer's transponder list do not affect existing entries
- [ ] **RACER-08**: Race director can update the transponder assignment on an entry before race start (equipment swap); change is audit-logged
- [ ] **RACER-09**: System warns when the same transponder is used in multiple entries at the same event (potential timing conflict if classes run simultaneously)
- [ ] **RACER-10**: Admin can define and manage car tag categories (default set installed: Chassis, ESC, Motor, Servo, Battery, Body, Tyres)
- [ ] **RACER-11**: Racer can add free-text values for any tag category to each of their cars
- [ ] **RACER-12**: Racer has an ability rating (0–100) per racing class, used to seed qualifying heats; updated automatically from championship points after each round

### Racing Classes

- [ ] **RACECLASS-01**: Admin can define and manage racing classes (name, description)

### Event Management

- [ ] **EVENT-01**: Admin can create an event with a name, date, and venue
- [ ] **EVENT-02**: Admin can add racing classes to an event and assign a race format to each class
- [ ] **EVENT-03**: Racer can enter an event online via the portal, selecting their class, car, and transponder
- [ ] **EVENT-04**: Public event schedule is visible without login
- [ ] **EVENT-05**: Events follow a state machine: DRAFT → PUBLISHED → OPEN → ENTRIES_CLOSED → IN_PROGRESS → COMPLETED; invalid transitions are rejected
- [ ] **EVENT-06**: Admin can combine two or more classes into a single race (run together, score separately) for events with low class turnout
- [ ] **ENTRY-01**: Entry has a lifecycle (PENDING → CONFIRMED → WITHDRAWN); racer can withdraw before entries close
- [ ] **ENTRY-02**: Admin can view and manage all entries per event and per class

### Race Format Configuration

- [ ] **FORMAT-01**: Admin can configure a standard timed race (duration, start type, qualifying type, min/max lap times)
- [ ] **FORMAT-02**: Admin can configure bump-up finals (qualifying heats, heat duration, best heats count, grid size, bump spots — default 2); number of finals is calculated automatically from actual entry count at event time
- [ ] **FORMAT-03**: Admin can configure a Reedy race format (rounds, round duration)
- [ ] **FORMAT-04**: Admin can configure points finals (qualifying heats, finals count, final duration)
- [ ] **FORMAT-05**: Race format config is type-discriminated; only fields valid for the chosen type are accepted and required fields are validated
- [ ] **FORMAT-06**: Assigning a format to an event class takes a snapshot of the template at assignment time; subsequent template edits do not affect existing events
- [ ] **FORMAT-07**: Admin can override individual format config fields at the event-class level without modifying the underlying template
- [ ] **FORMAT-08**: Start type is configurable per class per phase: STAGGER (car numbers called sequentially at configured interval), GRID (all start simultaneously on buzzer), ROLLING (time starts on first crossing after buzzer); qualifying and finals may use different start types
- [ ] **FORMAT-09**: Qualifying type is configurable: FTQ (fastest time across all heats), ROUND_BY_ROUND (points per finishing position each round), FASTEST_LAP (best single lap from any heat), CONSECUTIVE_LAPS (best N consecutive laps)
- [ ] **FORMAT-10**: Minimum lap time is configurable per class; crossings faster than this threshold are ignored (prevents loop double-counting and track-cutting)
- [ ] **FORMAT-11**: Maximum last lap time is configurable per class; race closes if no crossing occurs within this window after the clock expires (prevents infinite wait for broken cars)
- [ ] **FORMAT-12**: Gap between successive races is configurable per class
- [ ] **FORMAT-13**: Stagger start interval is configurable (default 1 second between car number calls)
- [ ] **FORMAT-14**: For bump-up events, the system automatically calculates the number of finals and generates final grid assignments from qualifying results using the configured grid size and bump spots; the lowest final may have fewer than grid_size racers
- [ ] **FORMAT-15**: Championship points for bump-up events are assigned from a single class-wide finishing order: A-final positions fill first, then non-promoted finishers from each lower final in finish order cascading down

### Local Forwarder

- [ ] **FORWARDER-01**: A separate forwarder application runs on the club's local network, connects to the AMB decoder, and forwards timing data to the cloud service; the cloud service cannot reach the decoder directly
- [ ] **FORWARDER-02**: Forwarder connects to the decoder via TCP using the AMB P3 binary protocol (0x8E/0x8F frame delimiters, TLV body, 0x8D byte-stuffing); serial transport is not required for v1
- [ ] **FORWARDER-03**: Forwarder streams decoded timing events to the cloud service via gRPC bidirectional streaming; the cloud can send RESEND requests back to the forwarder over the same stream
- [ ] **FORWARDER-04**: Forwarder is implemented as a Java Gradle submodule within the same repository, sharing domain model classes with the main application
- [ ] **FORWARDER-05**: Forwarder authenticates with the cloud service using a pre-configured API token before streaming begins

### Live Timing & AMB/MyLaps Integration

- [ ] **TIMING-01**: Cloud service receives timing data from the forwarder via gRPC; the forwarder owns the AMB P3 TCP connection and all protocol parsing
- [ ] **TIMING-02**: Forwarder auto-reconnects to the decoder on TCP connection loss; WATCHDOG record absence is the primary lost-connection signal; both forwarder↔decoder and forwarder↔cloud connection status are visible in the race control UI
- [ ] **TIMING-03**: Live lap times, positions, and gaps are displayed in the browser during a race via WebSocket
- [ ] **TIMING-04**: Lap times use the RTC_TIME field from PASSING records (decoder's embedded hardware timestamp), not server receipt time
- [ ] **TIMING-05**: The decoder integration uses a defined TimingSource interface; switching to a new protocol requires only a new implementation class with no changes to race control or timing logic
- [ ] **TIMING-06**: Forwarder performs the FIRST_CONTACT handshake with the decoder on initial connection before passing data flows
- [ ] **TIMING-07**: Forwarder monitors PASSING_NUMBER for gaps; detected gaps trigger a RESEND request to the decoder
- [ ] **TIMING-08**: At race start the system builds an in-memory transponder→entry map for the race; passing events with unregistered transponders are logged and surfaced in the race control UI

### Race Control

- [ ] **CTRL-01**: Race director can start and stop a race from the browser race control client
- [ ] **CTRL-02**: Race control displays the grid call — which cars are due on track next
- [ ] **CTRL-03**: Race director can add or remove marshal laps for on-track incidents, with a full audit trail
- [ ] **CTRL-04**: Race results can be exported as a printable/PDF sheet at the venue
- [ ] **CTRL-05**: Server enforces race state machine (PENDING → GRID → RUNNING → FINISHED); conflicting commands from multiple browser sessions are rejected
- [ ] **CTRL-06**: Unknown transponder passings can be retrospectively linked to an entry by the race director, with full audit trail
- [ ] **CTRL-07**: Race control displays the marshal list for the current race (the drivers who were in the previous race)
- [ ] **CTRL-08**: Race director can abandon a race in progress; results up to the abandonment point are saved and the meeting advances normally
- [ ] **CTRL-09**: Race director can skip to or re-run a specific race and round number

### Audio Announcements

- [ ] **AUDIO-01**: Race control browser produces voice announcements throughout the meeting using the Web Speech API; all announcement types are individually configurable on/off
- [ ] **AUDIO-02**: Countdown announcements fire at configurable intervals before each race (default: 10m, 5m, 2m, 1m, 30s) announcing race number and time remaining
- [ ] **AUDIO-03**: In STAGGER start mode, car numbers are called at the configured stagger interval; each driver starts when their car number is called
- [ ] **AUDIO-04**: Each lap crossing produces a high-pitched beep if the driver is improving on their best result, low-pitched otherwise
- [ ] **AUDIO-05**: When a driver finishes, a longer beep followed by their car number is announced
- [ ] **AUDIO-06**: Running order is announced at 2-minute intervals for the first 10 minutes of a race, then at 5-minute intervals
- [ ] **AUDIO-07**: Admin can enable or disable individual announcement types from the settings panel

### Race Official Views

- [ ] **OFFICIAL-01**: Race steward has a dedicated view showing live proximity alerts (which cars are closing on others)
- [ ] **OFFICIAL-02**: Race steward view highlights backmarker situations (lapped cars approaching leaders)
- [ ] **OFFICIAL-03**: Race referee can raise an incident report against a specific car during or after a race
- [ ] **OFFICIAL-04**: Race referee can apply a lap or time penalty to a car, which immediately updates live standings

### Practice

- [ ] **PRACTICE-01**: System supports timed open practice sessions using the decoder; live lap times are displayed and results are printable after the session
- [ ] **PRACTICE-02**: Practice display shows each racer's best run of N consecutive laps (configurable) to indicate sustained pace, not just best single lap

### Championship & Scoring

- [ ] **CHAMP-01**: Admin can configure a championship with "best X from Y rounds" scoring (default: best 4 of 6)
- [ ] **CHAMP-02**: Championship scoring handles DNF, DNS, and DQ correctly (all count toward Y rounds attended; DQ/DNS score zero points)
- [ ] **CHAMP-03**: Separate championship standings per racing class
- [ ] **CHAMP-04**: Admin can configure the points scale (points per finishing position) per championship
- [ ] **CHAMP-05**: Public championship standings table is live on the web
- [ ] **CHAMP-06**: Championship can score from qualifying results, final results, or both; if both, points from each source are summed per round
- [ ] **CHAMP-07**: Championship can award a configurable bonus point to the top qualifier (TQ) per class per round
- [ ] **CHAMP-08**: Championship can award a configurable bonus point to the A-final winner per class per round
- [ ] **CHAMP-09**: Individual drivers can be excluded from championship points for a specific round (DQ, non-eligible equipment, factory driver); exclusion is audit-logged
- [ ] **CHAMP-10**: Championship standings can be displayed in best-to-worst order per driver to surface drop scores and remaining title contenders

### Results

- [ ] **RESULT-01**: Final race results are published after each race
- [ ] **RESULT-02**: Results correctly reflect any marshal lap adjustments and penalties applied
- [ ] **RESULT-03**: Per-racer result history is viewable on the racer's portal page
- [ ] **RESULT-04**: Printed/PDF results optionally display a racer's car tag values beneath their name; controlled by an admin display setting
- [ ] **RESULT-05**: Result records include full individual lap time data (every lap, not just totals and best lap)

## v2 Requirements

### Notifications

- **NOTF-01**: Racer receives email confirmation when their event entry is accepted
- **NOTF-02**: Racer receives reminder email before an event they've entered

### Extended Official Tools

- **OFF-02**: Video review integration for disputed incidents
- **OFF-03**: Protest and appeal workflow with documented resolution

### Multi-Club / Federation Support

- **FED-01**: Multiple clubs can share the platform with data isolation
- **FED-02**: Federation-level standings across clubs

### Advanced Reporting

- **RPT-01**: Admin can export season statistics (best lap, average lap, DNF rate per racer)
- **RPT-02**: Racer can export their own season summary

## Out of Scope

| Feature | Reason |
|---------|--------|
| Payment processing | PCI compliance burden; club collects fees at the track |
| Native mobile app | Browser-based UI is mobile-responsive; dedicated app adds no v1 value |
| Offline mode | System requires network at the venue; no offline race control |
| Public entry list | Not requested; reduces racer privacy before an event |
| Social / community features | Not a social platform — timing and management only |
| Kafka / message broker | Single-club deployment; in-process STOMP broker is sufficient |
| Serial transport for forwarder | TCP is sufficient for v1; TimingSource interface supports future addition |
| Non-P3 timing protocols | TimingSource interface supports future protocols; AMB P3 only for v1 |

## Traceability

Populated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | — | Pending |
| AUTH-02 | — | Pending |
| AUTH-03 | — | Pending |
| AUTH-04 | — | Pending |
| RACER-01 | — | Pending |
| RACER-02 | — | Pending |
| RACER-03 | — | Pending |
| RACER-04 | — | Pending |
| RACER-05 | — | Pending |
| RACER-06 | — | Pending |
| RACER-07 | — | Pending |
| RACER-08 | — | Pending |
| RACER-09 | — | Pending |
| RACER-10 | — | Pending |
| RACER-11 | — | Pending |
| RACER-12 | — | Pending |
| RACECLASS-01 | — | Pending |
| EVENT-01 | — | Pending |
| EVENT-02 | — | Pending |
| EVENT-03 | — | Pending |
| EVENT-04 | — | Pending |
| EVENT-05 | — | Pending |
| EVENT-06 | — | Pending |
| ENTRY-01 | — | Pending |
| ENTRY-02 | — | Pending |
| FORMAT-01 | — | Pending |
| FORMAT-02 | — | Pending |
| FORMAT-03 | — | Pending |
| FORMAT-04 | — | Pending |
| FORMAT-05 | — | Pending |
| FORMAT-06 | — | Pending |
| FORMAT-07 | — | Pending |
| FORMAT-08 | — | Pending |
| FORMAT-09 | — | Pending |
| FORMAT-10 | — | Pending |
| FORMAT-11 | — | Pending |
| FORMAT-12 | — | Pending |
| FORMAT-13 | — | Pending |
| FORMAT-14 | — | Pending |
| FORMAT-15 | — | Pending |
| FORWARDER-01 | — | Pending |
| FORWARDER-02 | — | Pending |
| FORWARDER-03 | — | Pending |
| FORWARDER-04 | — | Pending |
| FORWARDER-05 | — | Pending |
| TIMING-01 | — | Pending |
| TIMING-02 | — | Pending |
| TIMING-03 | — | Pending |
| TIMING-04 | — | Pending |
| TIMING-05 | — | Pending |
| TIMING-06 | — | Pending |
| TIMING-07 | — | Pending |
| TIMING-08 | — | Pending |
| CTRL-01 | — | Pending |
| CTRL-02 | — | Pending |
| CTRL-03 | — | Pending |
| CTRL-04 | — | Pending |
| CTRL-05 | — | Pending |
| CTRL-06 | — | Pending |
| CTRL-07 | — | Pending |
| CTRL-08 | — | Pending |
| CTRL-09 | — | Pending |
| AUDIO-01 | — | Pending |
| AUDIO-02 | — | Pending |
| AUDIO-03 | — | Pending |
| AUDIO-04 | — | Pending |
| AUDIO-05 | — | Pending |
| AUDIO-06 | — | Pending |
| AUDIO-07 | — | Pending |
| OFFICIAL-01 | — | Pending |
| OFFICIAL-02 | — | Pending |
| OFFICIAL-03 | — | Pending |
| OFFICIAL-04 | — | Pending |
| PRACTICE-01 | — | Pending |
| PRACTICE-02 | — | Pending |
| CHAMP-01 | — | Pending |
| CHAMP-02 | — | Pending |
| CHAMP-03 | — | Pending |
| CHAMP-04 | — | Pending |
| CHAMP-05 | — | Pending |
| CHAMP-06 | — | Pending |
| CHAMP-07 | — | Pending |
| CHAMP-08 | — | Pending |
| CHAMP-09 | — | Pending |
| CHAMP-10 | — | Pending |
| RESULT-01 | — | Pending |
| RESULT-02 | — | Pending |
| RESULT-03 | — | Pending |
| RESULT-04 | — | Pending |
| RESULT-05 | — | Pending |

**Coverage:**
- v1 requirements: 83 total
- Mapped to phases: 0 (pending roadmap creation)
- Unmapped: 83 ⚠️

---
*Requirements defined: 2026-04-15*
*Last updated: 2026-04-15 after requirements review session*
