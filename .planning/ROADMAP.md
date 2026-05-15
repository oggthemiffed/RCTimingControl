# Roadmap: RCTimingControl

## Overview

Ten phases deliver a complete RC club management and race timing system. The build order follows a strict dependency chain: domain entities and auth unlock the racer portal; event management and format config unlock race control; race control unlocks live timing integration; audio and practice layer on top of a running timing system; results and championship scoring close out with post-race publishing. A first-run wizard and user documentation round out the system for new club installations, with a Docker-based trial environment enabling clubs to evaluate the system without committing to a full deployment.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Domain Foundation** - Core entities, database schema, auth, club/track/class/format config APIs (completed 2026-04-15)
- [x] **Phase 2: Racer Portal** - Racer self-service (profile, cars, transponders), online event entry, public schedule (completed 2026-04-15)
- [x] **Phase 3: Admin Panel & Event Management** - Admin event/class/entry management, event state machine, championship setup (completed 2026-04-20)
- [x] **Phase 4: Race Control** - Browser-based race control client, race state machine, marshal laps, referee tools (completed 2026-04-24)
- [x] **Phase 5: Live Timing & Forwarder** - AMB P3 forwarder, gRPC streaming, WebSocket live timing display (completed 2026-04-26)
- [x] **Phase 6: Audio & Practice** - Voice announcements (Web Speech API + TTS), open practice sessions
- [x] **Phase 7: Results & Championship** - Post-race result snapshots, championship standings, PDF export (completed 2026-05-03)
- [x] **Phase 8: First-Run Setup Wizard** - Guided onboarding for new club installations: club profile, tracks, formats, staff accounts, decoder config, forwarder test (completed 2026-05-14)
- [ ] **Phase 9: User Manual & Documentation** - In-app help system and printed/PDF race meeting guide for officials, racers, and admins
- [ ] **Phase 10: Docker Trial Environment** - docker-compose stack with demo seed data and fake decoder replay so clubs can evaluate the full system locally with a single command

## Phase Details

### Phase 1: Domain Foundation
**Goal**: The database schema, core entities, and admin configuration APIs exist so that clubs, tracks, classes, and race formats can be managed and auth works end-to-end
**Depends on**: Nothing (first phase)
**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, CLUB-01, CLUB-02, TRACK-01, TRACK-02, TRACK-03, TRACK-04, RACECLASS-01, FORMAT-01, FORMAT-02, FORMAT-04, FORMAT-05, FORMAT-06, FORMAT-07, FORMAT-08, FORMAT-09, FORMAT-10, FORMAT-11, FORMAT-12, FORMAT-13, FORMAT-14
**Success Criteria** (what must be TRUE):
  1. A racer can self-register with email/password and remain logged in across browser sessions, and can reset a forgotten password via email
  2. A staff user can log in and access only the tools their assigned roles permit (ADMIN, RACE_DIRECTOR, REFEREE)
  3. An admin can create club profile and governing body affiliations, define tracks with lap time thresholds and decoder loop configuration, and define racing classes
  4. An admin can create, edit, and delete race format templates (timed, bump-up finals, points finals) including all configurable fields; format config exports to JSON and re-imports cleanly
  5. Assigning a format template to an event class snapshots the config; editing the template afterwards does not change the existing assignment
**Plans**: TBD
**UI hint**: yes

### Phase 2: Racer Portal
**Goal**: Racers can manage their own profile, cars, and transponders through a self-service web portal and submit online entries to published events
**Depends on**: Phase 1
**Requirements**: RACER-01, RACER-02, RACER-03, RACER-04, RACER-05, RACER-06, RACER-07, RACER-08, RACER-09, RACER-10, RACER-11, RACER-12, RACER-13, RACER-14, EVENT-03, EVENT-04, ENTRY-01
**Success Criteria** (what must be TRUE):
  1. Racer can create and edit their profile including governing body membership numbers, and add or edit cars with tag categories and values; cars are archived not deleted
  2. Racer can register transponders (rejected if duplicate system-wide) and select a transponder when submitting an entry; the transponder is snapshotted at submission time
  3. Racer can submit an entry to a published event, selecting class, car, and transponder; submission is blocked if the club requires governing body membership and the racer has no matching number (admin can override)
  4. Racer can view and withdraw their own entries before entries close, and view their entry history and past results
  5. Public event schedule is visible to anyone without login
**Plans**: TBD
**UI hint**: yes

### Phase 3: Admin Panel & Event Management
**Goal**: Admins can create and configure events end-to-end — setting up classes, assigning formats, managing entries, and configuring championships — so a complete meeting structure exists before race day
**Depends on**: Phase 1
**Requirements**: EVENT-01, EVENT-02, EVENT-05, EVENT-06, EVENT-07, ENTRY-02, CHAMP-01, CHAMP-02, CHAMP-03, CHAMP-04, CHAMP-06, CHAMP-07, CHAMP-08, CHAMP-09, CHAMP-10
**Success Criteria** (what must be TRUE):
  1. Admin can create an event with a name, date, track association, and move it through its full state machine (DRAFT → PUBLISHED → OPEN → ENTRIES_CLOSED → IN_PROGRESS → COMPLETED); invalid transitions are rejected
  2. Admin can add racing classes to an event, assign race format templates (with override capability), and combine low-turnout classes into a single combined race
  3. Admin can view and manage all entries per event and per class
  4. Admin can create a championship, configure best-X-from-Y scoring, a custom points scale, and bonus points for TQ and A-final winner; standings display correctly with drops and tiebreaks
**Plans**: 6 plans
Plans:
- [x] 03-01-PLAN.md — Schema foundation: V15/V16 migrations, entity column additions, IllegalStateTransitionException + GlobalExceptionHandler
- [x] 03-02-PLAN.md — Event backend: EventService, state machine, EventController, EventClass override/combine, admin entry management (EVENT-01/02/05/06/07, ENTRY-02)
- [x] 03-03-PLAN.md — Championship backend: entities, ChampionshipService, points scale, exclusions, standings query scaffold (CHAMP-01..04, CHAMP-06..10)
- [x] 03-04-PLAN.md — Infrastructure + config services: MinIO + S3 client, ObjectStorageService, logo upload endpoint, CarTagCategory archive pattern
- [x] 03-05-PLAN.md — Admin panel frontend shell + events/entries UI: layout, routes, adminApi/adminQueryKeys scaffolding, event CRUD + state transitions + classes + entries
- [x] 03-06-PLAN.md — Championships + config forms UI: championship detail (6 tabs), points scale editor (ROAR/BRCA presets), club/tracks/formats/car-tag-categories admin pages (CHAMP-01..04, CHAMP-06..10)
**UI hint**: yes

### Phase 4: Race Control
**Goal**: A race director can run a complete race meeting from any browser — calling the grid, starting and stopping races, applying marshal laps, and handling incidents — with all commands enforced server-side
**Depends on**: Phase 3
**Requirements**: CTRL-01, CTRL-02, CTRL-03, CTRL-04, CTRL-05, CTRL-06, CTRL-07, CTRL-08, CTRL-09, OFFICIAL-01, OFFICIAL-02, OFFICIAL-03, OFFICIAL-04
**Pre-requisite schema work (must be first plan in this phase)**:
  - `EventClass` gains finals config fields: `finalsCount`, `carsPerFinal`, `bumpCount` (admin UI in Event → Classes tab)
  - New `Round` entity: type (PRACTICE/QUALIFIER/FINAL), roundNumber, sequenceInEvent, status
  - New `Race` entity: round, eventClass, heatNumber, sequenceInRound, finalLetter, startType (STAGGER/GRID), formatId override, status
  - New `RaceEntry` entity: race, entry, gridPosition, bumped flag
  - Round generator: admin wizard that creates all Round + Race records once entries close, splitting drivers into fixed heats
  - See `.planning/phases/04-race-state-machine/04-HEAT-STRUCTURE-SPEC.md` for full design
**Success Criteria** (what must be TRUE):
  1. Admin can configure finals per class (finalsCount, carsPerFinal, bumpCount) and trigger the round generator after entries close; all rounds and heats appear in correct run order
  2. Race director can start and stop a race; conflicting commands from a second browser window are rejected with HTTP 409; the race follows PENDING → GRID → RUNNING → FINISHED state machine
  3. Stagger start heats use finishing order from previous round as start order (best finisher goes first); round 1 uses entry order
  4. After qualifying closes, finals grids are auto-seeded from qualifying standings; admin can override any grid position
  5. After each bump-up final completes, the top N finishers are automatically appended to the back of the next final's grid; race director is alerted before starting the next final
  6. Race control displays the marshal list (drivers from previous race) and the grid call (cars due on track next) for the current race
  7. Race director can add or remove marshal laps with a full audit trail; results update immediately
  8. Race director can abandon a race in progress, skip to a specific race/round, and link an unknown transponder passing to an entry retroactively
  9. Race referee can raise incident reports and apply lap or time penalties that immediately update live standings
  10. Race results can be exported as a printable PDF sheet at the venue
**Plans:** 5/7 plans executed
Plans:
- [x] 04-01-PLAN.md — Wave 0 test scaffolding (@Disabled stubs for state machine + race control + referee + round generator)
- [x] 04-02-PLAN.md — V17/V18 schema migrations + race domain entities + RaceStateMachineService (CTRL-05)
- [x] 04-03-PLAN.md — RoundGeneratorService + BumpUpSeedingService + QualifyingStandingsService (heat structure, supports phase success criteria 1/3/4/5)
- [x] 04-04-PLAN.md — PreRaceReadinessQuery + controller (CTRL-02, CTRL-07)
- [x] 04-05-PLAN.md — Frontend PreRaceReadinessPanel + referee alert algorithms (CTRL-02, CTRL-07, OFFICIAL-01, OFFICIAL-02)
- [x] 04-06-PLAN.md — WebSocket/STOMP infrastructure + LiveRaceState + RaceControlController + SyntheticTimingService (CTRL-01, CTRL-03, CTRL-06, CTRL-08, CTRL-09)
- [x] 04-07-PLAN.md — RefereeController + ResultSnapshotService (V19) + cockpit React shell + PrintResultsPage (CTRL-04, OFFICIAL-03, OFFICIAL-04)
**UI hint**: yes

### Phase 5: Live Timing & Forwarder
**Goal**: The local forwarder application connects to the AMB decoder over TCP and streams live lap data to the cloud service, which broadcasts real-time positions and gaps to all connected browsers
**Depends on**: Phase 4
**Requirements**: FORWARDER-01, FORWARDER-02, FORWARDER-03, FORWARDER-04, FORWARDER-05, TIMING-01, TIMING-02, TIMING-03, TIMING-04, TIMING-05, TIMING-06, TIMING-07, TIMING-08
**Success Criteria** (what must be TRUE):
  1. The forwarder (separate Java Gradle submodule) connects to the AMB decoder via TCP, completes the FIRST_CONTACT handshake, and streams decoded PASSING events to the cloud service via gRPC bidirectional streaming using a pre-configured API token
  2. The forwarder auto-reconnects to the decoder on connection loss; WATCHDOG absence triggers reconnect; both forwarder-to-decoder and forwarder-to-cloud connection status are visible in the race control UI
  3. The forwarder detects PASSING_NUMBER gaps and sends RESEND requests to the decoder; lap timestamps use the decoder's own RTC_TIME field, not server clock
  4. Live lap times, positions, and gaps update in the browser in real time during a race; unknown transponder passings are surfaced in race control UI without blocking lap counting for registered entries
  5. Switching to a new timing protocol requires only a new TimingSource implementation class with no changes to race control or timing logic
**Plans**: 5 plans
Plans:
- [x] 05-01-PLAN.md — Wave 0 test scaffolding: @Disabled JUnit 5 stubs for all Phase 5 test targets
- [x] 05-02-PLAN.md — Forwarder module: RC-4 TCP client (Netty), Rc4TextParser, AmbRc4TimingSource, EpochAnchor, SeqGapDetector, simulator (playback + generative)
- [x] 05-03-PLAN.md — Token management: ForwarderToken entity, V21 migration, ForwarderTokenService, ForwarderTokenController (FORWARDER-05)
- [x] 05-04-PLAN.md — gRPC server infrastructure in :app: ForwarderGrpcServer, ForwarderTokenAuthInterceptor, ForwarderGrpcService, ForwarderStatusPublisher, LiveRaceState.retroactiveLinkTransponder, V22 migration, TransponderLinkController
- [x] 05-05-PLAN.md — Frontend: ForwarderStatusBar, UnknownTransponderLinkDialog, ForwarderTokenPage, API functions, types

### Phase 6: Audio & Practice
**Goal**: The race control browser produces voice announcements throughout the meeting and officials can run open practice sessions with live lap display
**Depends on**: Phase 5
**Requirements**: AUDIO-01, AUDIO-02, AUDIO-03, AUDIO-04, AUDIO-05, AUDIO-06, AUDIO-07, AUDIO-08, AUDIO-09, AUDIO-10, AUDIO-11, AUDIO-12, AUDIO-13, AUDIO-14, AUDIO-15, PRACTICE-01, PRACTICE-02
**Success Criteria** (what must be TRUE):
  1. Race control browser produces voice announcements for countdown intervals, stagger car-number calls, per-lap beeps (improving/not-improving), and finish announcements; all announcement types are individually togglable
  2. Pre-generated TTS audio clips for a race are fetched and cached by the client during grid preparation; if a clip is unavailable at playback time, Web Speech API synthesis is used as a non-blocking fallback
  3. Racer profile includes a phonetic spelling field; the server generates a TTS name clip on profile create/update using a configured TTS provider; racer can preview the clip and select a preferred voice; profanity blocklist screens both display name and phonetic spelling before saving
  4. Admin can run an open practice session using the decoder; live lap times are displayed and each racer's best N consecutive laps are shown; results are printable after the session
**Plans**: 6 plans
Plans:
- [x] 06-01-PLAN.md — Wave 0 test scaffolding (@Disabled JUnit 5 stubs, Vitest describe.skip blocks)
- [x] 06-02-PLAN.md — Schema + domain foundation: V23 migration, PracticeSession/PracticeLap entities, User.preferredVoiceId, ClubProfile audio settings, profanity blocklist, Piper docker-compose
- [x] 06-03-PLAN.md — Piper TTS infrastructure: PiperTtsClient (Wyoming TCP), TtsClipService, ProfanityFilter, AudioController, name clip generation on profile save
- [x] 06-04-PLAN.md — Race audio pre-generation: AudioPreGenerationService (GRID listener), AudioClipController, AdminAudioController (settings + blocklist)
- [x] 06-05-PLAN.md — Practice session backend: PracticeTimingService, LivePracticeState, PracticeTimingHub, PracticeSessionController, best-N consecutive algorithm
- [x] 06-06-PLAN.md — Frontend: AudioSettingsPanel, ProfilePage voice section, AdminAudioSettingsPage, PracticeSessionPage, PracticeLiveTable, hooks, routes
**UI hint**: yes

### Phase 7: Results & Championship
**Goal**: Final race results are published after each race, championship standings update automatically, and per-racer history is visible on the portal
**Depends on**: Phase 6
**Requirements**: RESULT-01, RESULT-02, RESULT-03, RESULT-04, RESULT-05, CHAMP-05
**Success Criteria** (what must be TRUE):
  1. Final race results are published publicly after each race, correctly reflecting all marshal lap adjustments and penalties, including every individual lap time
  2. Championship standings table is live on the web with no login required, showing results in best-to-worst order per driver with drop scores visible
  3. Per-racer result history is viewable on the racer's portal page; printed results optionally display car tag values beneath the racer's name (controlled by admin setting)
**Plans**: 6 plans
Plans:
**Wave 1**
- [x] 07-01-PLAN.md — Wave 0: test stubs (@Disabled for all Phase 7 test targets) + shadcn Collapsible install
- [x] 07-02-PLAN.md — V24 migration (car_number + show_car_tags_in_results) + RaceEntry/ClubProfile entities + car_number assignment in generators + ResultSnapshotService wire-up (RESULT-01, RESULT-02, RESULT-04)
- [x] 07-03-PLAN.md — ChampionshipStandingsQuery full implementation (best-X-from-Y, bonuses, DNS, drop logic) + RacerResultHistoryQuery + DTO (RESULT-03, CHAMP-05)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 07-04-PLAN.md — Public REST controllers (PublicResultsController, PublicChampionshipController, RacerResultsController) + SecurityConfig permitAll + car tag enrichment in ResultSnapshotQuery + EventScheduleDto enrichment (RESULT-01, RESULT-02, RESULT-04, RESULT-05, CHAMP-05)

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 07-05-PLAN.md — Frontend: PublicResultsPage (expandable lap rows), PublicChampionshipPage, EventSchedulePage (implemented), usePublicResultSnapshot hook, App.tsx routing (RESULT-01, RESULT-05, CHAMP-05)
- [x] 07-06-PLAN.md — Frontend: RacerResultsPage + RacerEventHistoryCard + nav tab + admin car tags Switch toggle in ClubProfilePage (RESULT-03, RESULT-04)
**UI hint**: yes

### Phase 8: First-Run Setup Wizard
**Goal**: A brand-new club installation can go from empty database to ready-to-run-a-meeting by following a guided multi-step wizard, without needing to read external documentation
**Depends on**: Phase 7
**Requirements**: TBD
**Success Criteria** (what must be TRUE):
  1. On first boot (no club record exists), the app redirects to the setup wizard; once completed, the redirect no longer triggers
  2. Wizard covers all mandatory setup steps in order: club profile (name, logo, contact), at least one track, at least one race format template, at least one staff account (admin/race director/referee), and decoder hardware configuration (IP address, RC-4 vs P3 protocol selection)
  3. Each wizard step is individually completable and skippable; the wizard is re-entrant — closing it mid-way and returning resumes from the last incomplete step
  4. A "test connection" action on the decoder config step verifies forwarder reachability and reports success/failure in the UI before the user proceeds
  5. A setup completion summary shows all configured items with links to edit them; the wizard is accessible again from Admin settings for reconfiguration
**Plans**: 6 plans
Plans:
- [x] 08-01-PLAN.md — Wave 0: backend + frontend test stubs (@Disabled / describe.skip) for SC-1..SC-4 + T-08-01..T-08-03
- [x] 08-02-PLAN.md — Backend core: V25 migration + ClubProfile decoder fields + UserService.createAdmin (T-08-01 replay guard) + SetupController/Service + 3 DTOs + SecurityConfig permitAll
- [x] 08-03-PLAN.md — Backend: PATCH /setup/decoder-config + POST /setup/staff + GET /setup/forwarder-config-download (T-08-03 placeholder)
- [x] 08-04-PLAN.md — Frontend foundation: setupApi + hooks + SetupGuard (T-08-02 mitigation) + SetupLayout shell + AdminBootstrapGate + App.tsx wiring + AdminPanelLayout nav entry
- [x] 08-05-PLAN.md — Wizard steps 1–4: ClubProfileStep, TrackStep, FormatStep, StaffStep + step orchestration in SetupLayout
- [x] 08-06-PLAN.md — Step 5 DecoderConfigStep (form + token UX + env download + 30s test connection polling) + SetupCompletePage + final phase checkpoint
**UI hint**: yes

### Phase 9: User Manual & Documentation
**Goal**: Comprehensive user-facing documentation covers every role — officials running a meeting, racers using the portal, and admins configuring the system — available both in-app and as a printable PDF
**Depends on**: Phase 8
**Requirements**: TBD
**Success Criteria** (what must be TRUE):
  1. An in-app help system is accessible from every major page (contextual "?" link); each help page explains the current screen's purpose, key actions, and common mistakes
  2. A printable "Race Meeting Guide" PDF covers the full race-day workflow for officials: setting up the event, calling the grid, starting/stopping races, applying marshal laps, handling incidents, and publishing results
  3. A racer quick-start guide covers registration, adding cars and transponders, and submitting an event entry
  4. An admin configuration guide covers club setup, track and format management, championship creation, and user/role management
  5. All documentation is versioned alongside the codebase and kept accurate against the implemented features
**Plans**: 4 plans
Plans:
**Wave 1**
- [x] 09-01-PLAN.md — Wave 0 test scaffolding: HelpContext + print page stubs

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 09-02-PLAN.md — HelpProvider infrastructure + layout '?' buttons + print guide shell pages

**Wave 3** *(blocked on Wave 2 completion)*
- [ ] 09-03-PLAN.md — Help article content (11 articles) + useHelp() wiring in 12 target pages

**Wave 4** *(blocked on Wave 3 completion)*
- [ ] 09-04-PLAN.md — Print guide comprehensive content + human review checkpoint (SC-5)
**UI hint**: yes

### Phase 10: Docker Trial Environment
**Goal**: Any club can spin up the complete system locally with `docker compose up` — including a live timing demo using replayed passing data — to evaluate features and give feedback before committing to a full deployment
**Depends on**: Phase 9
**Requirements**: TBD
**Success Criteria** (what must be TRUE):
  1. `docker compose up` from a fresh clone brings up all services (PostgreSQL, Spring Boot app, frontend, Piper TTS, forwarder, fake decoder) with no manual config steps beyond copying a `.env.example` file
  2. A demo-seed container runs once on first boot and populates the database with a sample club, track, race formats, racer accounts, and a completed historical event so the system is not a blank slate
  3. The fake decoder container replays a recorded RC-4 passing file on a loop so live timing is visible in the race control client without physical AMB hardware
  4. The setup wizard (Phase 8) is accessible and functional within the trial environment, allowing clubs to reconfigure it to match their own club details
  5. A `docker-compose.ghcr.yml` variant pulls pre-built images from GitHub Container Registry (GHCR) so non-technical clubs do not need to build from source; images are published automatically by a GitHub Actions workflow on each version tag
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Domain Foundation | 7/7 | Complete | 2026-04-15 |
| 2. Racer Portal | 5/5 | Complete | 2026-04-15 |
| 3. Admin Panel & Event Management | 6/6 | Complete | 2026-04-20 |
| 4. Race Control | 7/7 | Complete | 2026-04-24 |
| 5. Live Timing & Forwarder | 5/5 | Complete | 2026-04-26 |
| 6. Audio & Practice | 6/6 | Complete | 2026-04-30 |
| 7. Results & Championship | 7/7 | Complete | 2026-05-03 |
| 8. First-Run Setup Wizard | 0/TBD | Not started | - |
| 9. User Manual & Documentation | 1/4 | In Progress|  |
| 10. Docker Trial Environment | 0/TBD | Not started | - |


## Backlog

### Phase 999.1: TLS/HTTPS Production Deployment — Let's Encrypt (BACKLOG)

**Goal:** Secure the production deployment with TLS everywhere — HTTPS for the app server and TLS for the gRPC forwarder connection — removing accepted risks T-05-11 and T-05-20 from the Phase 5 security register.
**Requirements:** TBD
**Plans:** 1/4 plans executed

**Context:**
- T-05-11: Token plaintext intercepted on venue LAN (accepted for v1 — HTTPS not provisioned)
- T-05-20: gRPC token transmitted without TLS (accepted for v1 — venue LAN)
- Let's Encrypt is the natural fit for a self-hosted club deployment (free, auto-renewing)
- Options to explore: Caddy as a reverse proxy (handles cert renewal automatically), Certbot + nginx, or Spring Boot native TLS with cert provisioned externally
- gRPC TLS: `ManagedChannelBuilder` with TLS (remove `usePlaintext()` in `ForwarderGrpcClient`); server-side requires cert + key in `ForwarderGrpcServer`
- Consider: venue LAN vs internet-facing deployment — Let's Encrypt requires a public domain for ACME challenge; internal deployments may need a self-signed CA instead

Plans:
- [ ] TBD (promote with /gsd-review-backlog when ready)
