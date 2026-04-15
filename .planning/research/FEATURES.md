# Feature Landscape: RC Club Timing & Management System

**Domain:** RC (radio control) club race timing and management
**Researched:** 2026-04-15
**Confidence:** MEDIUM — web tools unavailable; findings based on training knowledge of RCResults,
Race Coordinator, MyRCM, and IFMAR/regional club practice. Validated against PROJECT.md context and
the explicit gap analysis the project owner provided.

---

## Competitive Landscape Summary

| System | Deployment | Self-service | Online entry | Cross-platform | Open/hosted |
|--------|------------|-------------|--------------|----------------|-------------|
| RCResults (rc-timing.com) | Windows desktop client + website | None | None | Windows only | Proprietary |
| Race Coordinator | Windows desktop | None | None | Windows only | Proprietary |
| MyRCM (myrcm.ch) | Web (Swiss-centric) | Partial | Some events | Web | SaaS |
| RCPro / iRace | Various | Partial | Partial | Varies | Varies |
| **This system** | Java backend, browser client | Full | Full | Any browser | Self-hosted |

The incumbents share a common failure mode: they were built when Windows desktop was the only
viable option for real-time TCP communication. The browser-based model with WebSocket or SSE for
live timing is now entirely feasible and is the primary architectural differentiator.

---

## 1. Racer / Member Management

### Table Stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Racer profile (name, contact, club membership) | Every system has this; admins must know who is racing | Low | First-class entity; drives all other features |
| Racer login / authentication | Required for self-service and online entry | Low | Standard username/password + email verify |
| Admin user management (list, search, edit any racer) | Admins need to fix data, approve memberships | Low | Role-gated view of same data |
| Membership / active status flag | Clubs gate event entry to paid members | Low | Boolean + optional expiry date |
| Password reset via email | Users forget passwords; no admin should have to reset manually | Low | Standard email flow |

### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Racer self-registration | Removes admin burden; racers own their own data | Low | This is a stated gap vs RCResults |
| Profile edit without admin involvement | Racers update contact details, name corrections | Low | Required for self-service model |
| Membership renewal tracking (expiry dates, reminders) | Clubs manually chase renewals; automation saves effort | Medium | Email reminder pipeline needed |

### Anti-Features (v1)

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Payment processing for membership fees | PCI compliance, gateway integration, recurring billing — enormous scope | Note membership status; let club handle payments offline or via Stripe separately |
| Social features (forums, messaging between racers) | RC clubs already have Facebook groups; nobody wants another | Hard no — scope creep with no adoption benefit |
| Mobile push notifications | Requires native app or FCM integration; v1 is web-only | Email is sufficient for v1 |

---

## 2. Car & Transponder Management

### Table Stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Car registration (name, class, make/model) | Timing must associate transponder reads with a racer and class | Low | Core entity; required before any race |
| Transponder number on car | AMB/MyLaps transponders are numbered; decoder reads the number | Low | Single transponder per car in most club racing |
| Multiple cars per racer | Racers compete in multiple classes with different cars | Low | One-to-many: racer → cars |
| Class assignment on car | Car belongs to a class (e.g., 1:10 Touring, 1:8 Buggy) | Low | Drives heat seeding, results separation |

### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Racer self-service transponder management | Stated gap in RCResults; admin no longer manually enters transponder numbers | Low-Med | The "add transponder to car" self-service portal is explicitly requested |
| Transponder loan pool (club-owned transponders) | Clubs lend transponders to visitors; currently tracked on paper | Medium | Track which club transponder is assigned to which racer for an event |
| Transponder conflict detection | Two cars with same transponder number → duplicate reads → wrong results | Low | Validate uniqueness at event check-in or assignment time |
| Car history (past events, results) | Racers like seeing their car's race history | Medium | Useful but not required for v1 |

### Anti-Features (v1)

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Multiple transponders per car (backup/primary) | Rare edge case; complicates decoder parsing and result attribution | Single transponder per car; racers swap physically if needed |
| Car technical inspection tracking (motor specs, weight) | Club-specific rules that vary enormously; becomes unmaintainable | Leave on paper/clipboard; out of scope |
| Car photos / media uploads | Storage, CDN, resize pipeline — no timing value | Out of scope for v1 |

---

## 3. Event Management

### Table Stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Create event (name, date, location, status) | Foundation of everything else | Low | Status: draft → open → in-progress → completed |
| Add classes to event | Events have multiple classes racing simultaneously or sequentially | Low | Class is a reusable entity; event selects which classes run |
| Define race format per class (heats + final, round-robin, etc.) | Different classes may race under different formats | Medium | At minimum: practice rounds + timed heats + final(s) |
| Set heat duration (minutes) | Race duration varies by class (3 min, 5 min, 8 min are common) | Low | Per-class or per-session config |
| Event schedule visible publicly | Racers check what time their class runs | Low | Public read; no login required |
| Event status lifecycle | Admins move events through draft → open-for-entry → closed → complete | Low | Determines what actions are available |

### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Online event entry / registration | Stated gap; currently done via email or on-the-day sign-up | Medium | Requires racer account, car selection, class selection |
| Entry close date / maximum entries per class | Admins cap class sizes; system enforces it | Low | Prevents oversubscription |
| Entry list visible publicly before event | Builds anticipation; lets racers see who's entered | Low | Public read of confirmed entries |
| Waitlist for full classes | Graceful overflow when class is at capacity | Medium | Queue management + notify when spot opens |
| Practice session scheduling (non-timed) | Clubs run practice before race day; schedule it in same system | Low | Session type = "practice" — no results recorded |

### Anti-Features (v1)

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Payment for event entry | See membership payment anti-feature reasoning | Track paid/unpaid status; collect payment outside system |
| Multi-venue support | Single-club system; multi-venue adds geographic complexity | Venue is a config field, not a first-class entity |
| Event templates / cloning | Convenience feature; admins can recreate manually for v1 | Add in v2 once event structure is stable |
| External calendar sync (iCal/Google) | Low value; adds sync maintenance burden | Provide a simple public HTML schedule page |

---

## 4. Race Session Management (On-the-day Operations)

### Table Stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Grid call — display which cars are next on track | Race official calls racers to grid; currently done by shouting | Low | Show car number, racer name, class, heat number |
| Heat seeding (seed by qualifying time or draw) | Heats must be drawn; fairness is expected | Medium | Initial draw by class ranking or random; re-seed after qualifying |
| Manage heat order (which heat runs next) | Official controls pace of the day; can swap heats | Low | Queue of pending sessions |
| Start race / end race controls | Official commands the race; timer starts/stops | Low | Critical path; must be reliable |
| Marshall lap adjustments | Add/remove laps for on-track incidents (racer displaced by another) | Low-Med | Audit trail important; undo required |
| Race result review before publishing | Official checks results before they go public | Low | "Provisional" vs "Final" status on results |

### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Browser-based race control client | Runs on any OS laptop at venue; no Windows-only installer | High | This is the primary stated gap vs RCResults; WebSocket for live updates |
| Live timing display during race | Racers and spectators watch current positions, gaps, lap counts | Medium | Broadcast from backend via WebSocket/SSE; read-only client |
| Bulk marshall lap entry | For pile-up incidents affecting multiple cars | Low | Select multiple cars, add/remove N laps at once |
| Race re-run workflow | Serious incident requires race to restart cleanly | Medium | Clear lap data for session, restart — keep audit trail |

### Anti-Features (v1)

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Offline race control (disconnected from backend) | Requires local state sync; enormous complexity for a rare scenario | Require network at venue — stated in PROJECT.md out-of-scope |
| Animated race visualization / track map | Fun but irrelevant to operations; large frontend effort | Results table with live updates is sufficient |
| Commentary / PA system integration | Niche hardware integration; zero portability | Out of scope permanently |
| Automated penalty detection | Race incidents require human judgment; automation creates disputes | Keep marshall laps as the human-driven mechanism |

---

## 5. Timing & Lap Counting

### Table Stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| AMB/MyLaps decoder TCP integration | Club hardware; without this there is no timing | High | Parse MyLaps TCP protocol — binary or text depending on decoder model |
| Transponder read → lap count mapping | Core function: read transponder number, look up car, increment lap | Medium | Handle duplicate reads (same transponder within too-short interval = filter) |
| Lap time recording (timestamp per crossing) | Foundation for all results and standings | Low | Store raw crossing times; derive lap time from delta |
| Fastest lap tracking | Displayed in results; used for qualifying positions | Low | Derived from lap time records |
| Total race time calculation | Results are by laps completed + time for final lap (not total elapsed) | Low | Standard RC results format: "15 laps, 5:23.456" |
| False start / late start handling | Race clock starts; car not on track yet gets credited with partial first lap | Medium | Some systems use a "scoring loop" pass to start individual car timers |

### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Duplicate transponder read filtering | Decoder sends every pass including close-together bounces; must filter | Medium | Minimum lap time threshold (e.g., ignore reads within N seconds of last read for that transponder) |
| Decoder connection status indicator | If TCP connection drops, official must know immediately | Low | Status indicator in race control UI; reconnect logic |
| Lap-by-lap breakdown in results | Post-race detail showing each lap time; racers review their own pace | Medium | Store per-lap times, not just totals |
| Multiple decoder support | Large clubs run multiple loops (start/finish + split points) | High | Track split times; probably v2 |

### Anti-Features (v1)

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Support for non-AMB/MyLaps hardware | Each timing system has different protocol; fragmentation | Design protocol layer cleanly so others can be added later, but only implement MyLaps |
| Manual lap counting fallback (click-to-count) | Paper and pencil is the right fallback; software manual counting is error-prone | Keep it out; train officials on paper backup |
| Video lap validation | Camera integration is enormous scope and club-specific hardware | Out of scope |

---

## 6. Championship / Points Scoring

### Table Stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Championship series grouping events | A series has N events; overall standings span all of them | Low | Championship entity links to ordered events |
| Points awarded by finishing position | Standard: 1st = 30 pts, 2nd = 25 pts, etc. (configurable scale) | Low | Configurable points table per championship |
| "Best X from Y rounds" scoring | Explicitly stated club format; drop worst rounds | Medium | Filter per-racer scores before summing; configurable X and Y |
| Championship standings table (public) | Racers check where they stand | Low | Derived query; aggregate per racer per class |
| Class-specific standings | Each class has separate championship | Low | Championship is scoped to a class |

### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Configurable points scales | Clubs use different tables (BRCA scale, club-specific) | Low | Stored as ordered array of point values per position |
| Tie-break rules (head-to-head result, most wins) | Required when two racers have equal points | Medium | Define tie-break rule at championship creation time |
| Mid-series racer addition | Racer joins championship after round 1 — gets zeros for missed rounds | Low | Design from start; easy to get wrong if not planned |
| Historical championship archives | Past season standings remain accessible | Low | No deletion; status = "archived" |
| Points preview (what if current standings change) | Racers calculate "what do I need to win" | High | Complex query; defer to v2 |

### Anti-Features (v1)

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Multiple simultaneous scoring methods per championship | Over-engineering; pick one method per championship | Keep it simple: one configurable points table, one drop rule |
| Inter-club championships (results from multiple systems) | Requires data exchange agreements, different data models | Single-club scope for v1 |
| Handicap / age-group scoring | Small clubs may want this but it fragments results display | Add only if club explicitly needs it after v1 ships |

---

## 7. Results Display

### Table Stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Race results published after each race | Racers check results immediately | Low | Public page; no login required |
| Results by event, class, heat/final | Navigation: event → class → session → results | Low | URL structure matters for bookmarking |
| Finishing order with laps, time, and gap to leader | Standard results layout all RC systems use | Low | "1. John Smith — 15 laps, 5:23.456 (+0.000)" |
| Fastest lap noted in results | Racers compare lap pace | Low | Highlight fastest lap per session |
| Results printable / exportable from venue | Race officials post paper results at the track | Low | Print-friendly CSS or PDF export per session |

### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Live results during race (auto-updating) | Spectators and waiting racers watch positions change | Medium | WebSocket/SSE push from timing backend |
| Per-racer results history | "Show me all my results across all events" | Medium | Requires login; aggregate query across sessions |
| Lap-by-lap breakdown accessible post-race | Racers review where they lost time | Medium | Stored if timing backend records per-lap times |
| Public results with no login required | Racers share links; parents watch remotely | Low | This is an architectural default, not a feature — just don't gate results |

### Anti-Features (v1)

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Results comparison / analytics dashboard | Chart.js charting, trend analysis — no operational value at v1 | Raw results are sufficient; add analytics in v2+ |
| Embed widget for club website | iframe or JS widget adds cross-origin complexity | Provide a clean public URL racers can link to |
| Results export to EFRA / BRCA national federation | Federation formats vary; requires external agreement | Out of scope; add only if club needs BRCA reporting |

---

## 8. Online Entry / Registration

### Table Stakes

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Racer selects event and class to enter | Core workflow; replaces email / on-the-day signup | Low-Med | Requires racer login, active car in that class |
| Entry confirmation (in-system status) | Racer knows they are entered; admin sees who is in | Low | Email confirmation on successful entry |
| Entry withdrawal by racer before close | Life happens; racer cancels without admin involvement | Low | Entry status: pending / confirmed / withdrawn |
| Admin entry management (add, remove, view list) | Admin adds late entries, removes no-shows, bulk imports | Low | Admin view mirrors racer view with extra controls |
| Entry list visible to admins before event | Plan the day, seed heats, call the grid | Low | Derived from confirmed entries |

### Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Automatic class check (car must be in right class) | Prevents entries for cars not eligible for a class | Low | Validate car's class against event's class at entry time |
| Admin can open/close entry window per class | Fine-grained control: entries open for Buggy but closed for Touring | Low | Per-class entry status flag |
| Late entry flag (entered after close) | Admins sometimes allow late entries; need to distinguish for scheduling | Low | Boolean flag on entry record |
| Waiting list with auto-promotion | When an entry withdraws, next on waitlist gets notified and promoted | Medium | Requires email notification pipeline |

### Anti-Features (v1)

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Payment collection at entry | See recurring payment anti-feature note | Collect entry fees at the track; track "paid" boolean in system if needed |
| Entry forms with custom questions (t-shirt sizes, etc.) | Generalised form builder is scope creep | Hard-code the fields needed for RC entry |
| Guest / anonymous entry (no account required) | Creates orphaned entries with no racer profile; breaks transponder linking | Require account creation; make it fast |

---

## Feature Dependencies

```
Racer account
  └── Car registration
        └── Transponder assignment
              └── Online event entry
                    └── Heat seeding
                          └── Grid call

AMB/MyLaps TCP connection
  └── Transponder read stream
        └── Lap counting & timing
              └── Live timing display
                    └── Race results (provisional)
                          └── Race results (final, after marshal review)
                                └── Championship points
                                      └── Championship standings

Championship definition
  └── Event linked to championship
        └── Points awarded per race result
              └── Best-X-from-Y aggregation
                    └── Standings table
```

Key blocking dependencies:
- No live timing without AMB/MyLaps TCP integration (this is the hardest technical risk)
- No online entry without racer account + car + transponder chain
- No championship standings without per-event race results
- Race control (start/stop, grid call, marshal laps) can be built before TCP integration is complete — stub timing feed with test data

---

## MVP Recommendation

Prioritize in this order:

**Phase 1 — Identity and data model (no racing yet)**
1. Racer registration and login
2. Car registration with class assignment
3. Transponder assignment to car
4. Admin user management

**Phase 2 — Event administration (no timing yet)**
5. Event creation and class configuration
6. Online event entry (racer self-service)
7. Entry list and heat draw tooling
8. Public event schedule

**Phase 3 — Race control and live timing (the hard phase)**
9. AMB/MyLaps TCP decoder integration
10. Lap counting and real-time results
11. Browser-based race control client (start/stop, grid call, marshal laps)
12. Live timing display (WebSocket broadcast)

**Phase 4 — Results and championships**
13. Provisional → final results publishing
14. Per-racer results history
15. Championship definition and scoring
16. Championship standings (public)

Defer to v2:
- Membership renewal reminders (email pipeline adds ops complexity)
- Transponder loan pool (useful but not blocking)
- Waitlist with auto-promotion (complexity vs value at v1 scale)
- Lap-by-lap breakdown in results (requires storing more timing data; worth it but not v1)
- Multiple decoder support / split timing

---

## Confidence Notes

- **Table stakes features:** HIGH confidence — these are consistent across all RC club timing systems regardless of technology era. Any system missing these loses adoption.
- **Differentiator features:** HIGH confidence for the three stated gaps (self-service portal, online entry, cross-platform race control). MEDIUM confidence for others — derived from observed competitor weaknesses.
- **Anti-features:** MEDIUM confidence — the payment/PCI argument is unambiguous; others are judgment calls based on v1 scope discipline.
- **AMB/MyLaps protocol specifics:** MEDIUM confidence — the protocol exists and is documented (Orbits/AMBrc protocol), but exact message formats depend on decoder model (AMB RC4, MyLaps ProChip, etc.). This needs dedicated technical research before Phase 3.

---

## Sources

- PROJECT.md — project owner's explicit gap analysis vs RCResults (authoritative for this project)
- Training knowledge of RCResults feature set (Windows client, no self-service, no online entry) — MEDIUM confidence, last verified pre-August 2025
- Training knowledge of Race Coordinator (Windows-only, timing + heat management, no online entry) — MEDIUM confidence
- Training knowledge of MyRCM (myrcm.ch — Swiss-hosted, partial self-service, limited online entry) — MEDIUM confidence
- Training knowledge of AMB/MyLaps Orbits protocol (TCP-based, decoder pushes crossing records) — MEDIUM confidence; needs verification against current decoder firmware docs
- General RC club operations knowledge (BRCA, EFMAR formats, heat structures, points scales) — HIGH confidence for structural patterns, LOW confidence for specific federation rules
