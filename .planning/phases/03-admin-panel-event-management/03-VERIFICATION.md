---
phase: 03-admin-panel-event-management
verified: 2026-04-25T14:41:01Z
status: gaps_found
score: 13/14 must-haves verified
overrides_applied: 0
gaps:
  - truth: "Admin can view and manage all entries per event and per class (ENTRY-02)"
    status: failed
    reason: "Backend AdminEntryController is missing GET /events/{eventId}/classes/{classId} and POST /{id}/withdraw endpoints. Plan 03-02 SUMMARY claimed these were 'already implemented in Phase 2' — but the Phase 2 AdminEntryController only has PATCH /{id}/transponder and POST /{id}/membership-override. Frontend adminApi.ts calls /api/v1/admin/entries/events/{eventId}/classes/{classId} and /api/v1/admin/entries/{entryId}/withdraw but these backend routes do not exist."
    artifacts:
      - path: "app/src/main/java/dev/monkeypatch/rctiming/api/admin/AdminEntryController.java"
        issue: "Missing GET /events/{eventId}/classes/{classId} endpoint and POST /{id}/withdraw endpoint"
      - path: "app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryService.java"
        issue: "Has withdraw(entryId, userId) method but no adminWithdraw(entryId, adminId, reason) method"
    missing:
      - "GET @GetMapping(\"/events/{eventId}/classes/{classId}\") in AdminEntryController returning List<AdminEntryDto>"
      - "POST @PostMapping(\"/{id}/withdraw\") in AdminEntryController delegating to entryService.adminWithdraw"
      - "adminWithdraw(Long entryId, Long adminId, String reason) in EntryService with audit log"
      - "AdminEntryQueryService in app/src/main/java/dev/monkeypatch/rctiming/query/entry/ (file not found)"
      - "AdminEntryProjection record in query/entry package (file not found)"
      - "Constructor injection of AdminEntryQueryService in AdminEntryController"
---

# Phase 03: Admin Panel & Event Management — Verification Report

**Phase Goal:** Admins can create and configure events end-to-end — setting up classes, assigning formats, managing entries, and configuring championships — so a complete meeting structure exists before race day
**Verified:** 2026-04-25T14:41:01Z
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (from ROADMAP.md Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Admin can create an event with name/date/track and move it through the full state machine; invalid transitions rejected | VERIFIED | EventController + EventStateMachineService exist; EventControllerIT has 8 tests including 409 on invalid transition; state machine VALID_TRANSITIONS table confirmed |
| 2 | Admin can add racing classes to an event, assign format templates with override capability, and combine low-turnout classes | VERIFIED | EventClassController exists with POST (add), PUT /{classId}/overrides, POST /combine; EventClassService has setCombinedRaceGroup wiring; EventClassControllerIT with 6 tests passes |
| 3 | Admin can view and manage all entries per event and per class (ENTRY-02) | FAILED | Backend AdminEntryController missing GET entries-by-class and POST withdraw endpoints; frontend calls URLs that return 404 |
| 4 | Admin can create a championship, configure best-X-from-Y scoring, custom points scale, bonus points; standings display correctly | VERIFIED | ChampionshipController with full CRUD, points scale replace-all, exclusions; ChampionshipControllerIT with 13 tests; frontend ChampionshipDetailPage with 6 tabs including PointsScaleEditor with ROAR/BRCA presets |

**Score:** 3/4 truths verified (from roadmap success criteria)

### Derived Must-Haves from Plan Frontmatter (Combined)

| # | Must-Have | Status | Evidence |
|---|-----------|--------|----------|
| 1 | PostgreSQL schema V15 (events.track_id, event_classes.racing_class_id/combined_race_group, club_profiles.logo_url, car_tag_categories.archived) | VERIFIED | V15__phase3_admin_schema.sql exists with all required columns |
| 2 | PostgreSQL schema V16 (championships, championship_classes, championship_event_links, championship_points_scale, championship_exclusions) | VERIFIED | V16__create_championships.sql exists with all 5 tables and constraints |
| 3 | IllegalStateTransitionException mapped to HTTP 409 via GlobalExceptionHandler | VERIFIED | GlobalExceptionHandler has @ExceptionHandler(IllegalStateTransitionException.class) returning HTTP 409 (3 occurrences of IllegalStateTransitionException in that file) |
| 4 | POST /api/v1/admin/events creates DRAFT event; POST /{id}/transition enforces state machine (EVENT-01, EVENT-05) | VERIFIED | EventController @RequestMapping confirmed; EventStateMachineService throws IllegalStateTransitionException on invalid transitions |
| 5 | POST /api/v1/admin/events/{id}/classes adds class with config snapshot; PUT overrides; POST combine (EVENT-02, EVENT-06) | VERIFIED | EventClassController @RequestMapping confirmed; EventClassService.setCombinedRaceGroup wired |
| 6 | GET /api/v1/admin/events returns jOOQ-backed list with LEFT JOIN tracks for trackName (EVENT-07) | VERIFIED | AdminEventQueryService has leftJoin(TRACKS) confirmed |
| 7 | GET /api/v1/admin/events/{id}/classes/{classId}/entries and POST /api/v1/admin/entries/{id}/withdraw (ENTRY-02) | FAILED | AdminEntryController does not have these endpoints; frontend calls 404 URLs |
| 8 | Championship CRUD with best-X-from-Y, scoring_source, tq_bonus, afinal_winner_bonus (CHAMP-01, CHAMP-06, CHAMP-07, CHAMP-08) | VERIFIED | ChampionshipController @RequestMapping("/api/v1/admin/championships") confirmed; all entity fields present |
| 9 | POST /api/v1/admin/championships/{id}/classes adds racing class (CHAMP-03) | VERIFIED | ChampionshipController has @PostMapping("/{id}/classes") |
| 10 | POST /api/v1/admin/championships/{id}/events links event with round number (CHAMP-10) | VERIFIED | ChampionshipController has @PostMapping("/{id}/events"); duplicate round returns 409 tested |
| 11 | PUT /api/v1/admin/championships/{id}/points-scale replaces scale atomically (CHAMP-04) | VERIFIED | ChampionshipService.replacePointsScale uses deleteAllByChampionshipId + saveAll in @Transactional |
| 12 | POST /api/v1/admin/championships/{id}/exclusions creates audited exclusion with actingAdminId from JWT (CHAMP-02, CHAMP-09) | VERIFIED | ChampionshipController uses Long.parseLong(auth.getName()); ChampionshipService.createExclusion persists createdBy |
| 13 | MinIO S3 client with forcePathStyle(true); PUT /api/v1/admin/club/logo multipart endpoint persisting logo_url (D-22) | VERIFIED | MinioConfig has pathStyleAccessEnabled(true); ClubProfileController has PUT /logo; LogoUploadService sets logoUrl |
| 14 | CarTagCategoryService archive soft-delete; GET ?includeArchived; POST /{id}/unarchive (D-21) | VERIFIED | CarTagCategoryService has setArchived(true), no deleteById; CarTagCategoryController has includeArchived param |

**Score:** 13/14 must-haves verified

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `app/src/main/resources/db/migration/V15__phase3_admin_schema.sql` | VERIFIED | Exists with all required ALTER TABLE statements |
| `app/src/main/resources/db/migration/V16__create_championships.sql` | VERIFIED | Exists with all 5 championship tables |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/event/IllegalStateTransitionException.java` | VERIFIED | Exists, extends RuntimeException |
| `app/src/main/java/dev/monkeypatch/rctiming/api/GlobalExceptionHandler.java` | VERIFIED | Has IllegalStateTransitionException handler mapping to HTTP 409 |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/event/EventStateMachineService.java` | VERIFIED | Exists, throws IllegalStateTransitionException |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/event/EventService.java` | VERIFIED | Exists, delegates to stateMachineService.transition |
| `app/src/main/java/dev/monkeypatch/rctiming/api/admin/EventController.java` | VERIFIED | @RequestMapping("/api/v1/admin/events") confirmed |
| `app/src/main/java/dev/monkeypatch/rctiming/api/admin/EventClassController.java` | VERIFIED | @RequestMapping("/api/v1/admin/events/{eventId}/classes") confirmed |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/format/EventClassService.java` | VERIFIED | setCombinedRaceGroup, convertValue snapshot confirmed |
| `app/src/main/java/dev/monkeypatch/rctiming/query/event/AdminEventQueryService.java` | VERIFIED | leftJoin(TRACKS) confirmed |
| `app/src/main/java/dev/monkeypatch/rctiming/api/admin/AdminEntryController.java` | PARTIAL | Exists but missing GET entries-by-class and POST withdraw endpoints |
| `app/src/main/java/dev/monkeypatch/rctiming/query/entry/AdminEntryQueryService.java` | MISSING | File not found; only EntryQueryService.java exists in this package |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/entry/EntryService.java` | PARTIAL | Has withdraw() method but no adminWithdraw(entryId, adminId, reason) |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/championship/Championship.java` (+ 4 entities) | VERIFIED | All 13 championship domain files present |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/championship/ChampionshipService.java` | VERIFIED | All 12+ public methods present including replacePointsScale, createExclusion |
| `app/src/main/java/dev/monkeypatch/rctiming/api/admin/ChampionshipController.java` | VERIFIED | @RequestMapping("/api/v1/admin/championships"), all endpoints present |
| `app/src/main/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQuery.java` | VERIFIED | Returns empty list scaffold; Phase 7 will fill in |
| `app/src/main/java/dev/monkeypatch/rctiming/config/MinioConfig.java` | VERIFIED | pathStyleAccessEnabled(true), ApplicationRunner bucket auto-create |
| `app/src/main/java/dev/monkeypatch/rctiming/infrastructure/storage/S3ObjectStorageService.java` | VERIFIED | Implements ObjectStorageService |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/club/LogoUploadService.java` | VERIFIED | ALLOWED_CONTENT_TYPES, MAX_BYTES validation present |
| `app/src/main/java/dev/monkeypatch/rctiming/domain/car/CarTagCategoryService.java` | VERIFIED | archiveCategory/unarchiveCategory present; deleteById absent |
| `frontend/src/lib/adminApi.ts` | VERIFIED | championships, club, tracks, formats, carTagCategories sub-objects present; listEntriesForClass and withdrawEntry present (frontend calls non-existent backend URLs) |
| `frontend/src/hooks/admin/adminQueryKeys.ts` | VERIFIED | Factory function pattern with all required key groups |
| `frontend/src/pages/admin/AdminPanelLayout.tsx` | VERIFIED | Outlet + sidebar confirmed |
| `frontend/src/pages/admin/events/EventListPage.tsx` | VERIFIED | statusColor map, useAdminEventsList hook present |
| `frontend/src/pages/admin/events/EventDetailPage.tsx` | VERIFIED | VALID_NEXT map, useTransitionEvent, EntryListSection references |
| `frontend/src/pages/admin/championships/ChampionshipDetailPage.tsx` | VERIFIED | 6 tabs (Tabs/TabsContent), useCreateExclusion/useDeleteExclusion present |
| `frontend/src/pages/admin/championships/PointsScaleEditor.tsx` | VERIFIED | ROAR_PRESET/BRCA constants, useReplacePointsScale present |
| `frontend/src/pages/admin/club/ClubProfilePage.tsx` | VERIFIED | uploadLogo present (4 occurrences), accept attribute for image types |
| `frontend/src/pages/admin/categories/CarTagCategoriesPage.tsx` | VERIFIED | useCarTagCategories, includeArchived present |
| `frontend/src/App.tsx` | VERIFIED | AdminPanelLayout, /admin/events, championships routes confirmed |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| EventController | EventService | constructor injection + eventService. delegation | VERIFIED | eventService.create/update/transition present |
| EventService | EventStateMachineService | stateMachineService.transition() | VERIFIED | transition() delegates to state machine |
| EventStateMachineService | IllegalStateTransitionException | throw new IllegalStateTransitionException | VERIFIED | 1 occurrence confirmed |
| AdminEventQueryService | EVENTS + TRACKS jOOQ tables | leftJoin(TRACKS).on(TRACKS.ID.eq(EVENTS.TRACK_ID)) | VERIFIED | 1 occurrence confirmed |
| AdminEntryController | AdminEntryQueryService | missing | FAILED | AdminEntryQueryService does not exist; controller has no injection |
| ChampionshipController | ChampionshipService | championshipService. delegation | VERIFIED | All endpoints delegate to service |
| ChampionshipService | championship_points_scale | deleteAllByChampionshipId + saveAll | VERIFIED | Transactional replace-all confirmed |
| ChampionshipService | championship_exclusions audit | createdBy field from JWT | VERIFIED | Long.parseLong(auth.getName()) in controller |
| ChampionshipController | ChampionshipStandingsQuery | standingsQuery.computeStandings | VERIFIED | GET /{id}/standings endpoint confirmed |
| ClubProfileController | LogoUploadService | logoUploadService.uploadLogo (multipart) | VERIFIED | PUT /logo endpoint wired |
| LogoUploadService | club_profiles.logo_url | setLogoUrl + save | VERIFIED | setArchived pattern confirmed in CarTagCategoryService |
| EventDetailPage | adminApi.transitionEvent | useMutation + invalidateQueries | VERIFIED | transitionEvent (2 occurrences), invalidateQueries in EventDetailPage |
| EntryListSection | adminApi.withdrawEntry | useWithdrawEntry hook | VERIFIED (frontend only) | Frontend wired; backend endpoint absent |
| App.tsx | AdminPanelLayout | RequireRole + nested routes | VERIFIED | AdminPanelLayout confirmed in App.tsx |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| EVENT-01 | 03-02 | Admin can create an event with name, date, venue | VERIFIED | EventController POST creates DRAFT event; list/detail endpoints working |
| EVENT-02 | 03-02 | Admin can add racing classes and assign race formats | VERIFIED | EventClassController add/override/combine endpoints; config snapshot via ObjectMapper.convertValue |
| EVENT-05 | 03-02 | Events follow state machine; invalid transitions rejected | VERIFIED | EventStateMachineService VALID_TRANSITIONS; HTTP 409 on invalid; 2 tests covering 409 |
| EVENT-06 | 03-02 | Admin can combine classes into single race | VERIFIED | EventClassService.combineClasses sets shared combined_race_group |
| EVENT-07 | 03-02 | Admin associates event with track; track name joins | VERIFIED | events.track_id FK (V15); AdminEventQueryService leftJoin TRACKS |
| ENTRY-02 | 03-02 | Admin can view and manage all entries per event and class | FAILED | GET entries-by-class and POST withdraw backend endpoints do not exist; AdminEntryQueryService missing |
| CHAMP-01 | 03-03 | Championship best-X-from-Y scoring | VERIFIED | best_x_from_y_x/y columns; ChampionshipService.create persists them |
| CHAMP-02 | 03-03 | Scoring handles DNF/DNS/DQ + exclusion mechanism | PARTIAL | Plan implemented exclusion rows (CHAMP-09 overlap). DNF/DNS/DQ handling is Phase 7 (scoring computation). Schema foundation exists. |
| CHAMP-03 | 03-03 | Separate standings per racing class | VERIFIED | championship_classes table; ChampionshipClassDto with racingClassId |
| CHAMP-04 | 03-03 | Admin can configure points scale per championship | VERIFIED | championship_points_scale table; replacePointsScale atomic replace-all; PointsScaleEditor with ROAR/BRCA presets |
| CHAMP-06 | 03-03 | Scoring source (qualifying/finals/both) | VERIFIED | scoring_source enum column with CHECK constraint; ScoringSource enum; scoringSource radio in UI |
| CHAMP-07 | 03-03 | TQ bonus points | VERIFIED | tq_bonus_points column; tqBonusPoints field in Championship entity |
| CHAMP-08 | 03-03 | A-final winner bonus points | VERIFIED | afinal_winner_bonus_points column; afinalWinnerBonusPoints field |
| CHAMP-09 | 03-03 | Driver exclusions with audit log | VERIFIED | championship_exclusions table; createdBy from JWT; GET /exclusions list in UI |
| CHAMP-10 | 03-03 | Standings display with drops and tiebreaks | PARTIAL | Scaffold in place (returns empty list, Phase 7 fills scoring); championship_event_links with round_number enables ordering. Full standings display deferred to Phase 7. |

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `app/src/main/java/dev/monkeypatch/rctiming/query/championship/ChampionshipStandingsQuery.java` | `return List.of()` — intentional Phase 3 scaffold | Info | By design; Phase 7 implements scoring join; documented in ROADMAP |
| `.planning/ROADMAP.md` | `- [ ] 03-06-PLAN.md` not updated to `- [x]` after completion | Info | Tracking inconsistency; 03-06-SUMMARY.md shows `status: complete`; UAT 18/18 passed |

### Behavioral Spot-Checks

| Behavior | Check | Status |
|---------- |-------|--------|
| Migrations have correct SQL | `grep -c "track_id bigint references tracks(id)" V15*.sql` → 1 | PASS |
| State machine throws on invalid | `grep -c "throw new IllegalStateTransitionException" EventStateMachineService.java` → 1 | PASS |
| AdminEntryController has withdraw endpoint | `grep -c "withdraw" AdminEntryController.java` → 0 | FAIL |
| AdminEntryQueryService exists | `ls query/entry/AdminEntryQueryService.java` → not found | FAIL |
| ChampionshipService replacePointsScale is atomic | `grep -c "deleteAllByChampionshipId" ChampionshipService.java` → 1 | PASS |
| MinIO pathStyle enabled | `grep -c "pathStyleAccessEnabled" MinioConfig.java` → 1 | PASS |
| CarTagCategoryService has no hard-delete | `grep -c "deleteById" CarTagCategoryService.java` → 0 | PASS |
| Frontend app routes championship pages | `grep -c "championships" App.tsx` → 4 | PASS |
| PointsScaleEditor has both presets | `grep -c "ROAR_PRESET\|BRCA" PointsScaleEditor.tsx` → 5 | PASS |

### Gaps Summary

**One gap blocking full goal achievement:**

The backend admin entry management endpoints for ENTRY-02 were never implemented. Plan 03-02 Task 3 was supposed to add:
- `GET /api/v1/admin/entries/events/{eventId}/classes/{classId}` — list entries per class with racer names (jOOQ join)
- `POST /api/v1/admin/entries/{entryId}/withdraw` — admin withdraw with reason and audit log

The 03-02 SUMMARY stated that "AdminEntryController was already implemented in Phase 2 (plan 02-04); no changes needed" — but the actual Phase 2 AdminEntryController only has `PATCH /{id}/transponder` and `POST /{id}/membership-override`. Neither the entry listing endpoint nor the admin withdraw endpoint exist.

The frontend (`EntryListSection.tsx`, `adminApi.ts`) is correctly wired to call these missing endpoints. The human UAT step 14 ("Click 'Withdraw' on an entry — Confirm. Row updates to WITHDRAWN badge") was listed as Pass — this either tested without actual entries in the class (empty state shown), or the backend returned a 404 that the frontend silently ignored.

The `AdminEntryQueryService` and `AdminEntryProjection` records specified in Plan 03-02 are also absent from the `query/entry/` package.

**Note on CHAMP-02 and CHAMP-10:** These requirements have different literal text in REQUIREMENTS.md vs what Phase 3 plans implemented. CHAMP-02 ("DNF/DNS/DQ scoring") and CHAMP-10 ("standings best-to-worst order") are scoring/display features that depend on race results data created in Phase 7. The Phase 3 plans implemented the admin-side exclusion mechanism (CHAMP-09) and event-link/round-number foundation that Phase 7 will use to compute standings. These are partial — schema and admin tooling laid, but scoring computation deferred to Phase 7. This is architecturally correct per the roadmap.

---

_Verified: 2026-04-25T14:41:01Z_
_Verifier: Claude (gsd-verifier)_
