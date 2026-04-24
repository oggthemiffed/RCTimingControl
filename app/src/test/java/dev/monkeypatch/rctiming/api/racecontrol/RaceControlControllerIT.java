package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.api.racecontrol.dto.RunOrderItemDto;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.format.QualifyingType;
import dev.monkeypatch.rctiming.domain.format.StartType;
import dev.monkeypatch.rctiming.domain.format.TimedRaceConfig;
import dev.monkeypatch.rctiming.domain.race.MarshalAdjustment;
import dev.monkeypatch.rctiming.domain.race.MarshalAdjustmentRepository;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.race.RoundStatus;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import dev.monkeypatch.rctiming.domain.race.UnknownTransponderLink;
import dev.monkeypatch.rctiming.domain.race.UnknownTransponderLinkRepository;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RaceControlControllerIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired RaceRepository raceRepository;
    @Autowired RoundRepository roundRepository;
    @Autowired RaceEntryRepository raceEntryRepository;
    @Autowired EntryRepository entryRepository;
    @Autowired MarshalAdjustmentRepository marshalAdjustmentRepository;
    @Autowired UnknownTransponderLinkRepository unknownTransponderLinkRepository;
    @Autowired RacingClassRepository racingClassRepository;
    @Autowired EventClassRepository eventClassRepository;
    @Autowired EventRepository eventRepository;

    private String directorToken;

    @BeforeEach
    void setUp() {
        String email = "director-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("pass123"));
        user.setFirstName("Race");
        user.setLastName("Director");
        user.setRoles(Set.of(Role.RACE_DIRECTOR));
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(email, "pass123"),
                AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        directorToken = loginResp.getBody().accessToken();
    }

    // --- CTRL-01: lifecycle transitions ---

    @Test
    void callGrid_returnsOkAndTransitionsToGrid() {
        Race race = seedRace(RaceStatus.PENDING);

        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/v1/race-control/race/" + race.getId() + "/call-grid",
                HttpMethod.POST, new HttpEntity<>(directorHeaders()), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Race reloaded = raceRepository.findById(race.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RaceStatus.GRID);
    }

    @Test
    void startRace_returnsOkAndTransitionsToRunning() {
        Race race = seedRace(RaceStatus.GRID);

        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/v1/race-control/race/" + race.getId() + "/start",
                HttpMethod.POST, new HttpEntity<>(directorHeaders()), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Race reloaded = raceRepository.findById(race.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RaceStatus.RUNNING);
        assertThat(reloaded.getStartedAt()).isNotNull();
    }

    @Test
    void conflictingTransitionFromSecondSession_returns409() {
        // PENDING → RUNNING is an invalid transition (must go via GRID)
        Race race = seedRace(RaceStatus.PENDING);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/race-control/race/" + race.getId() + "/start",
                HttpMethod.POST, new HttpEntity<>(directorHeaders()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // --- CTRL-03: marshal adjustment ---

    @Test
    void marshalAdjustment_persistsAllAuditFields() {
        Race race = seedRace(RaceStatus.RUNNING);
        Entry entry = seedEntry(race);
        RaceEntry raceEntry = seedRaceEntry(race, entry);

        Map<String, Object> body = Map.of(
                "entryId", raceEntry.getEntryId(),
                "transponderNumber", entry.getTransponderNumberSnapshot(),
                "lapDelta", 1
        );

        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/v1/race-control/race/" + race.getId() + "/marshal-adjustment",
                HttpMethod.POST,
                new HttpEntity<>(body, directorHeaders()),
                Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<MarshalAdjustment> adjustments = marshalAdjustmentRepository
                .findByRaceIdOrderByAdjustedAt(race.getId());
        assertThat(adjustments).hasSize(1);
        MarshalAdjustment adj = adjustments.get(0);
        assertThat(adj.getRaceId()).isEqualTo(race.getId());
        assertThat(adj.getEntryId()).isEqualTo(raceEntry.getEntryId());
        assertThat(adj.getTransponderNumber()).isEqualTo(entry.getTransponderNumberSnapshot());
        assertThat(adj.getLapDelta()).isEqualTo(1);
        assertThat(adj.getRaceStateAtTime()).isEqualTo("RUNNING");
        assertThat(adj.getActingUserId()).isNotNull();
        assertThat(adj.getActingUserName()).isNotBlank();
        assertThat(adj.getAdjustedAt()).isNotNull();
    }

    // --- CTRL-06: unknown transponder link ---

    @Test
    void unknownTransponderLink_createsRecord() {
        Race race = seedRace(RaceStatus.RUNNING);
        String transponder = "UT-" + UUID.randomUUID().toString().substring(0, 8);

        // First POST — creates record
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/v1/race-control/race/" + race.getId() + "/unknown-transponder-link",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("transponderNumber", transponder), directorHeaders()),
                Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<UnknownTransponderLink> links = unknownTransponderLinkRepository.findAll().stream()
                .filter(l -> l.getRaceId().equals(race.getId()) && transponder.equals(l.getTransponderNumber()))
                .toList();
        assertThat(links).hasSize(1);
        assertThat(links.get(0).getLinkedEntryId()).isNull();

        // Second POST with same transponder + entryId — upsert, still one row
        Entry entry = seedEntry(race);
        restTemplate.exchange(
                "/api/v1/race-control/race/" + race.getId() + "/unknown-transponder-link",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("transponderNumber", transponder, "linkedEntryId", entry.getId()),
                        directorHeaders()),
                Void.class);

        List<UnknownTransponderLink> afterUpsert = unknownTransponderLinkRepository.findAll().stream()
                .filter(l -> l.getRaceId().equals(race.getId()) && transponder.equals(l.getTransponderNumber()))
                .toList();
        assertThat(afterUpsert).hasSize(1);
        assertThat(afterUpsert.get(0).getLinkedEntryId()).isEqualTo(entry.getId());
    }

    // --- CTRL-08: abandon ---

    @Test
    void abandonRace_savesResultSnapshotAndReturnsFinished() {
        Race race = seedRace(RaceStatus.RUNNING);

        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/v1/race-control/race/" + race.getId() + "/abandon",
                HttpMethod.POST, new HttpEntity<>(directorHeaders()), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Race reloaded = raceRepository.findById(race.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RaceStatus.FINISHED);
        assertThat(reloaded.getFinishedAt()).isNotNull();
    }

    @Test
    @Disabled("implemented in plan 07 — print results page")
    void getPrintResults_returns200WithLapHistory() {
        // TODO: plan 07 implements assertions — CTRL-04
    }

    // --- CTRL-09: skip-to ---

    @Test
    void skipToLaterRace_overridesAutoAdvance() {
        // Seed two races in the same event/round
        RoundWithClass rwc = seedRoundWithClass();
        Round round = rwc.round();
        Race race1 = seedRaceInRound(round, rwc.eventClassId(), 1, RaceStatus.PENDING);
        Race race2 = seedRaceInRound(round, rwc.eventClassId(), 2, RaceStatus.PENDING);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/race-control/race/" + race1.getId() + "/skip-to",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("targetRaceId", race2.getId()), directorHeaders()),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKey("activeRaceId");
        assertThat(((Number) resp.getBody().get("activeRaceId")).longValue()).isEqualTo(race2.getId());

        // Run-order still returns both races — skip-to does NOT mutate race status
        ResponseEntity<List<RunOrderItemDto>> runOrder = restTemplate.exchange(
                "/api/v1/race-control/event/" + round.getEventId() + "/run-order",
                HttpMethod.GET,
                new HttpEntity<>(directorHeaders()),
                new ParameterizedTypeReference<List<RunOrderItemDto>>() {});
        assertThat(runOrder.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(runOrder.getBody()).hasSizeGreaterThanOrEqualTo(2);
    }

    // --- Helpers ---

    private HttpHeaders directorHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(directorToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /** Returns [round, eventClassId] pair to avoid re-querying all EventClasses. */
    private record RoundWithClass(Round round, Long eventClassId) {}

    private RoundWithClass seedRoundWithClass() {
        Event event = new Event();
        event.setName("Test Event " + UUID.randomUUID().toString().substring(0, 8));
        event.setEventDate(java.time.LocalDate.of(2026, 6, 1));
        Instant now = Instant.now();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        event = eventRepository.save(event);

        RacingClass rc = new RacingClass();
        rc.setName("TestClass-" + UUID.randomUUID().toString().substring(0, 6));
        rc.setCreatedAt(now);
        rc.setUpdatedAt(now);
        rc = racingClassRepository.save(rc);

        EventClass ec = new EventClass();
        ec.setEventId(event.getId());
        ec.setRacingClassId(rc.getId());
        ec.setConfigSnapshot(new TimedRaceConfig(5, StartType.GRID, QualifyingType.FASTEST_LAP, 1, 0));
        ec = eventClassRepository.save(ec);
        Long eventClassId = ec.getId();

        Round round = new Round();
        round.setEventId(event.getId());
        round.setType(RoundType.PRACTICE);
        round.setRoundNumber(1);
        round.setSequenceInEvent(1);
        round.setStatus(RoundStatus.PENDING);
        round.setCreatedAt(now);
        round.setUpdatedAt(now);
        return new RoundWithClass(roundRepository.save(round), eventClassId);
    }

    private Round seedRound() {
        return seedRoundWithClass().round();
    }

    private Race seedRace(RaceStatus status) {
        RoundWithClass rwc = seedRoundWithClass();
        return seedRaceInRound(rwc.round(), rwc.eventClassId(), 1, status);
    }

    private Race seedRaceInRound(Round round, Long eventClassId, int sequenceInRound, RaceStatus status) {

        Race race = new Race();
        race.setRoundId(round.getId());
        race.setEventClassId(eventClassId);
        race.setHeatNumber(1);
        race.setSequenceInRound(sequenceInRound);
        race.setStartType(dev.monkeypatch.rctiming.domain.race.StartType.GRID);
        race.setStatus(status);
        Instant now = Instant.now();
        race.setCreatedAt(now);
        race.setUpdatedAt(now);
        return raceRepository.save(race);
    }

    private Long seedEventClass(Long eventId) {
        Instant now = Instant.now();
        RacingClass rc = new RacingClass();
        rc.setName("RC-" + UUID.randomUUID().toString().substring(0, 6));
        rc.setCreatedAt(now);
        rc.setUpdatedAt(now);
        rc = racingClassRepository.save(rc);

        EventClass ec = new EventClass();
        ec.setEventId(eventId);
        ec.setRacingClassId(rc.getId());
        ec.setConfigSnapshot(new TimedRaceConfig(5, StartType.GRID, QualifyingType.FASTEST_LAP, 1, 0));
        ec = eventClassRepository.save(ec);
        return ec.getId();
    }

    private Entry seedEntry(Race race) {
        // Load any user to link the entry to (use the director user)
        User user = userRepository.findAll().stream().findFirst().orElseThrow();
        Long eventClassId = race.getEventClassId();

        // Resolve eventId via round
        Round round = roundRepository.findById(race.getRoundId()).orElseThrow();

        Entry entry = new Entry();
        entry.setUserId(user.getId());
        entry.setEventId(round.getEventId());
        entry.setEventClassId(eventClassId);
        entry.setTransponderNumberSnapshot("T-" + UUID.randomUUID().toString().substring(0, 8));
        entry.setStatus(EntryStatus.CONFIRMED);
        Instant now = Instant.now();
        entry.setSubmittedAt(now);
        entry.setUpdatedAt(now);
        return entryRepository.save(entry);
    }

    private RaceEntry seedRaceEntry(Race race, Entry entry) {
        RaceEntry re = new RaceEntry();
        re.setRaceId(race.getId());
        re.setEntryId(entry.getId());
        re.setGridPosition(1);
        return raceEntryRepository.save(re);
    }
}
