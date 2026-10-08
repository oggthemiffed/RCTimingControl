package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.checkin.CheckInResult;
import dev.monkeypatch.rctiming.domain.checkin.CheckInService;
import dev.monkeypatch.rctiming.domain.checkin.SwapResult;
import dev.monkeypatch.rctiming.domain.checkin.TransponderSlot;
import dev.monkeypatch.rctiming.domain.checkin.TransponderSwapService;
import dev.monkeypatch.rctiming.domain.competitor.CompetitorService;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryAuditLog;
import dev.monkeypatch.rctiming.domain.entry.EntryAuditLogRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.event.EventStatus;
import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.format.QualifyingType;
import dev.monkeypatch.rctiming.domain.format.TimedRaceConfig;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Check-in desk and transponder swap end to end (L11). */
class CheckInControllerIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired EventRepository eventRepository;
    @Autowired RacingClassRepository racingClassRepository;
    @Autowired EventClassRepository eventClassRepository;
    @Autowired EntryRepository entryRepository;
    @Autowired EntryAuditLogRepository auditLogRepository;
    @Autowired CompetitorService competitorService;
    @Autowired CheckInService checkInService;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired TransponderSwapService transponderSwapService;

    private Long directorId;
    private String directorToken;
    private String refereeToken;
    private String noRoleToken;
    private Event event;
    private EventClass eventClass;
    private String className;
    private String suffix;

    @BeforeEach
    void setUp() {
        User director = createUser(Set.of(Role.RACE_DIRECTOR));
        directorId = director.getId();
        directorToken = jwtTokenService.generateAccessToken(director);
        refereeToken = jwtTokenService.generateAccessToken(createUser(Set.of(Role.REFEREE)));
        noRoleToken = jwtTokenService.generateAccessToken(createUser(Set.of()));

        Instant now = Instant.now();
        suffix = UUID.randomUUID().toString().substring(0, 8);
        event = saveEvent(now);
        className = "Stock Buggy " + suffix;
        eventClass = saveEventClass(event.getId(), saveRacingClass(className, now).getId(), now);
    }

    @Test
    @SuppressWarnings("unchecked")
    void resolve_matchesPrimaryOrSecondary_andShowsRaceHubArrival() {
        Entry entry = saveEntry("Jane Doe", "P" + suffix, "S" + suffix, EntryStatus.CONFIRMED);
        entry.setRacehubArrival("ARRIVED");
        entryRepository.save(entry);

        for (String number : List.of("P" + suffix, "S" + suffix)) {
            ResponseEntity<List> resp = post(directorToken, checkIn("/resolve"),
                    Map.of("transponderNumber", number), List.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).hasSize(1);
            Map<String, Object> dto = (Map<String, Object>) resp.getBody().get(0);
            assertThat(((Number) dto.get("entryId")).longValue()).isEqualTo(entry.getId());
            assertThat(dto.get("competitorName")).isEqualTo("Jane Doe " + suffix);
            assertThat(dto.get("className")).isEqualTo(className);
            assertThat(dto.get("checkedIn")).isEqualTo(false);
            assertThat(dto.get("racehubArrival")).isEqualTo("ARRIVED");
            // So the desk can fix how the name is said (#119)
            assertThat(dto.get("competitorId")).isNotNull();
            assertThat(dto.get("spokenName")).isNull();
            assertThat((String) dto.get("speechName")).startsWith("Jane Doe");
        }
    }

    @Test
    void resolve_unknownOrWithdrawnNumber_is404() {
        saveEntry("Gone Driver", "W" + suffix, null, EntryStatus.WITHDRAWN);

        for (String number : List.of("NOPE" + suffix, "W" + suffix)) {
            ResponseEntity<Map> resp = post(directorToken, checkIn("/resolve"),
                    Map.of("transponderNumber", number), Map.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(resp.getBody()).containsEntry("error", "not_found");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_isCaseInsensitiveAndScopedToTheEvent() {
        saveEntry("Jane Doe", "A" + suffix, null, EntryStatus.CONFIRMED);
        saveEntry("John Smith", "B" + suffix, null, EntryStatus.CONFIRMED);

        ResponseEntity<List> resp = get(directorToken, checkIn("/search?query=jane doe " + suffix), List.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(((Map<String, Object>) resp.getBody().get(0)).get("competitorName"))
                .isEqualTo("Jane Doe " + suffix);

        assertThat(get(directorToken, checkIn("/search?query="), List.class).getBody()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void confirm_checksInOnce_andRecordsTheOfficial() {
        Entry entry = saveEntry("Jane Doe", "C" + suffix, null, EntryStatus.CONFIRMED);

        ResponseEntity<Map> first = post(directorToken, checkIn("/entries/" + entry.getId() + "/confirm"), null, Map.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody()).containsEntry("alreadyCheckedIn", false);
        Map<String, Object> dto = (Map<String, Object>) first.getBody().get("entry");
        assertThat(dto.get("checkedIn")).isEqualTo(true);
        Object checkedInAt = dto.get("checkedInAt");
        assertThat(checkedInAt).isNotNull();

        ResponseEntity<Map> again = post(refereeToken, checkIn("/entries/" + entry.getId() + "/confirm"), null, Map.class);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(again.getBody()).containsEntry("alreadyCheckedIn", true);
        assertThat(((Map<String, Object>) again.getBody().get("entry")).get("checkedInAt")).isEqualTo(checkedInAt);

        Entry saved = entryRepository.findById(entry.getId()).orElseThrow();
        assertThat(saved.getCheckedInByUserId()).isEqualTo(directorId);

        // The first check-in is in the audit log with who did it; the repeat by the referee is not
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select * from audit_log where entity_type = 'entry' and entity_id = ? and action = 'ENTRY_CHECKED_IN'",
                String.valueOf(entry.getId()));
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("actor_user_id")).longValue()).isEqualTo(directorId);
        assertThat(((Number) rows.get(0).get("event_id")).longValue()).isEqualTo(event.getId());
    }

    @Test
    void confirm_withdrawnOrOtherEventEntry_isRefused() {
        Entry withdrawn = saveEntry("Gone Driver", "D" + suffix, null, EntryStatus.WITHDRAWN);
        assertThat(post(directorToken, checkIn("/entries/" + withdrawn.getId() + "/confirm"), null, Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Entry live = saveEntry("Jane Doe", "E" + suffix, null, EntryStatus.CONFIRMED);
        String otherEvent = "/api/v1/race-control/events/" + (event.getId() + 100_000) + "/check-in";
        assertThat(post(directorToken, otherEvent + "/entries/" + live.getId() + "/confirm", null, Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void swap_updatesTheTransponder_andIsAudited() {
        Entry entry = saveEntry("Jane Doe", "F" + suffix, null, EntryStatus.CONFIRMED);

        ResponseEntity<Map> resp = post(refereeToken, swapUrl(entry),
                Map.of("slot", "PRIMARY", "newTransponderNumber", "G" + suffix), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("oldTransponderNumber", "F" + suffix)
                .containsEntry("newTransponderNumber", "G" + suffix)
                .containsEntry("slot", "PRIMARY");
        assertThat(entryRepository.findById(entry.getId()).orElseThrow().getTransponderNumberSnapshot())
                .isEqualTo("G" + suffix);

        List<EntryAuditLog> audit = auditLogRepository.findByEntryIdOrderByCreatedAtAsc(entry.getId());
        assertThat(audit).extracting(EntryAuditLog::getAction).containsExactly("TRANSPONDER_SWAP");

        // The new number now resolves at the desk
        assertThat(post(directorToken, checkIn("/resolve"),
                Map.of("transponderNumber", "G" + suffix), List.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void swap_toAnotherCompetitorsNumber_is409() {
        Entry entry = saveEntry("Jane Doe", "H" + suffix, null, EntryStatus.CONFIRMED);
        saveEntry("John Smith", "I" + suffix, null, EntryStatus.CONFIRMED);

        ResponseEntity<Map> resp = post(directorToken, swapUrl(entry),
                Map.of("slot", "SECONDARY", "newTransponderNumber", "I" + suffix), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).containsEntry("error", "transponder_already_assigned");
        assertThat(entryRepository.findById(entry.getId()).orElseThrow().getSecondaryTransponderNumber()).isNull();
    }

    @Test
    void swap_removingThePrimary_is400() {
        Entry entry = saveEntry("Jane Doe", "J" + suffix, null, EntryStatus.CONFIRMED);

        ResponseEntity<Map> resp = post(directorToken, swapUrl(entry),
                Map.of("slot", "PRIMARY", "newTransponderNumber", ""), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("error", "primary_required");
    }

    @Test
    void accountsWithoutAnOfficialRole_areTurnedAway() {
        Entry entry = saveEntry("Jane Doe", "K" + suffix, null, EntryStatus.CONFIRMED);

        assertThat(post(noRoleToken, checkIn("/entries/" + entry.getId() + "/confirm"), null, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post(noRoleToken, swapUrl(entry),
                Map.of("slot", "PRIMARY", "newTransponderNumber", "L" + suffix), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post(null, checkIn("/resolve"), Map.of("transponderNumber", "K" + suffix), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void concurrentConfirms_recordExactlyOneFirstCheckIn() throws Exception {
        Entry entry = saveEntry("Jane Doe", "M" + suffix, null, EntryStatus.CONFIRMED);

        List<CheckInResult> results = runConcurrently(6,
                i -> () -> checkInService.confirm(event.getId(), entry.getId(), directorId));

        assertThat(results).filteredOn(r -> r instanceof CheckInResult.Success s && !s.alreadyCheckedIn())
                .hasSize(1);
    }

    @Test
    void concurrentSwapsToTheSameFreeNumber_onlyOneCompetitorGetsIt() throws Exception {
        List<Entry> entries = IntStream.range(0, 4)
                .mapToObj(i -> saveEntry("Driver " + i, "N" + i + suffix, null, EntryStatus.CONFIRMED))
                .toList();

        List<SwapResult> results = runConcurrently(entries.size(),
                i -> () -> transponderSwapService.swap(event.getId(), entries.get(i).getId(),
                        TransponderSlot.PRIMARY, "FREE" + suffix, directorId));

        assertThat(results).filteredOn(r -> r instanceof SwapResult.Success).hasSize(1);
        assertThat(results).filteredOn(r -> r instanceof SwapResult.TransponderAlreadyAssigned)
                .hasSize(entries.size() - 1);
    }

    // --- helpers ---

    /** Starts the tasks together and waits for all of them. */
    private <T> List<T> runConcurrently(int count, java.util.function.IntFunction<Callable<T>> task)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = IntStream.range(0, count)
                    .mapToObj(i -> pool.submit(() -> {
                        start.await();
                        return task.apply(i).call();
                    }))
                    .toList();
            start.countDown();
            List<T> results = new java.util.ArrayList<>();
            for (Future<T> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private String checkIn(String path) {
        return "/api/v1/race-control/events/" + event.getId() + "/check-in" + path;
    }

    private String swapUrl(Entry entry) {
        return "/api/v1/race-control/events/" + event.getId() + "/entries/" + entry.getId() + "/transponder-swap";
    }

    private <T> ResponseEntity<T> post(String token, String url, Object body, Class<T> type) {
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers(token)), type);
    }

    private <T> ResponseEntity<T> get(String token, String url, Class<T> type) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers(token)), type);
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private User createUser(Set<Role> roles) {
        User user = new User();
        user.setEmail("official-" + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("unused");
        user.setFirstName("Test");
        user.setLastName("Official");
        user.setRoles(roles);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private Event saveEvent(Instant now) {
        Event e = new Event();
        e.setName("Check-in Event " + suffix);
        e.setEventDate(LocalDate.now());
        e.setStatus(EventStatus.IN_PROGRESS);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return eventRepository.save(e);
    }

    private RacingClass saveRacingClass(String name, Instant now) {
        RacingClass rc = new RacingClass();
        rc.setName(name);
        rc.setCreatedAt(now);
        rc.setUpdatedAt(now);
        return racingClassRepository.save(rc);
    }

    private EventClass saveEventClass(Long eventId, Long racingClassId, Instant now) {
        EventClass ec = new EventClass();
        ec.setEventId(eventId);
        ec.setRacingClassId(racingClassId);
        ec.setConfigSnapshot(new TimedRaceConfig(5,
                dev.monkeypatch.rctiming.domain.format.StartType.ROLLING,
                QualifyingType.FASTEST_LAP, 1, 3));
        ec.setCreatedAt(now);
        ec.setUpdatedAt(now);
        return eventClassRepository.save(ec);
    }

    private Entry saveEntry(String name, String primary, String secondary, EntryStatus status) {
        Instant now = Instant.now();
        Entry entry = new Entry();
        entry.setCompetitorId(competitorService.createWalkIn(name + " " + suffix).getId());
        entry.setEventId(event.getId());
        entry.setEventClassId(eventClass.getId());
        entry.setStatus(status);
        entry.setTransponderNumberSnapshot(primary);
        entry.setSecondaryTransponderNumber(secondary);
        entry.setSubmittedAt(now);
        entry.setUpdatedAt(now);
        return entryRepository.save(entry);
    }
}
