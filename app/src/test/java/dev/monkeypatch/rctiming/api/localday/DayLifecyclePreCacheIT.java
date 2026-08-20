package dev.monkeypatch.rctiming.api.localday;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.api.localday.dto.CloseDayResponseDto;
import dev.monkeypatch.rctiming.api.localday.dto.EventLockStatusDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheResponseDto;
import dev.monkeypatch.rctiming.domain.car.Car;
import dev.monkeypatch.rctiming.domain.car.CarRepository;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.format.QualifyingType;
import dev.monkeypatch.rctiming.domain.format.StartType;
import dev.monkeypatch.rctiming.domain.format.TimedRaceConfig;
import dev.monkeypatch.rctiming.domain.localday.EventSyncGenerationRepository;
import dev.monkeypatch.rctiming.domain.localday.LocaldayCredentialRepository;
import dev.monkeypatch.rctiming.domain.localday.LocaldayInstanceSecretRepository;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.race.RoundStatus;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DayLifecyclePreCacheIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EntryRepository entryRepository;

    @Autowired
    CarRepository carRepository;

    @Autowired
    RacingClassRepository racingClassRepository;

    @Autowired
    EventClassRepository eventClassRepository;

    @Autowired
    RoundRepository roundRepository;

    @Autowired
    RaceRepository raceRepository;

    @Autowired
    LocaldayCredentialRepository localdayCredentialRepository;

    @Autowired
    LocaldayInstanceSecretRepository localdayInstanceSecretRepository;

    @Autowired
    EventSyncGenerationRepository eventSyncGenerationRepository;

    private String adminToken;
    private Long adminUserId;

    @BeforeEach
    void setUp() {
        String email = "localday-admin-" + UUID.randomUUID() + "@test.com";
        createAdminUser(email, "adminPass123");
        adminUserId = userRepository.findByEmail(email).orElseThrow().getId();

        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(email, "adminPass123"),
                AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        adminToken = loginResp.getBody().accessToken();
    }

    // --- Day lifecycle ---

    @Test
    void openDay_freshEvent_returnsLockedTrueAndReflectedInStatus() {
        Long eventId = createEventInDb();

        ResponseEntity<EventLockStatusDto> openResp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/open", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-open-1"), adminHeaders()), EventLockStatusDto.class);

        assertThat(openResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(openResp.getBody().locked()).isTrue();
        assertThat(openResp.getBody().lockedAt()).isNotNull();
        assertThat(openResp.getBody().generation()).isEqualTo(1L);

        ResponseEntity<EventLockStatusDto> statusResp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/lock-status", HttpMethod.GET,
                new HttpEntity<>(adminHeaders()), EventLockStatusDto.class);

        assertThat(statusResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResp.getBody().locked()).isTrue();
        assertThat(statusResp.getBody().lockedAt()).isNotNull();
        assertThat(statusResp.getBody().generation()).isEqualTo(1L);
    }

    @Test
    void closeDay_syncComplete_returnsClosedAndUnlocks() {
        Long eventId = createEventInDb();
        restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/open", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-open-2"), adminHeaders()), EventLockStatusDto.class);

        ResponseEntity<CloseDayResponseDto> closeResp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/close", HttpMethod.POST,
                new HttpEntity<>(Map.of("syncComplete", true), adminHeaders()), CloseDayResponseDto.class);

        assertThat(closeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(closeResp.getBody().status()).isEqualTo("closed");

        ResponseEntity<EventLockStatusDto> statusResp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/lock-status", HttpMethod.GET,
                new HttpEntity<>(adminHeaders()), EventLockStatusDto.class);
        assertThat(statusResp.getBody().locked()).isFalse();
    }

    @Test
    void closeDay_syncNotComplete_returnsPendingAndStaysLocked() {
        Long eventId = createEventInDb();
        restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/open", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-open-3"), adminHeaders()), EventLockStatusDto.class);

        ResponseEntity<CloseDayResponseDto> closeResp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/close", HttpMethod.POST,
                new HttpEntity<>(Map.of("syncComplete", false), adminHeaders()), CloseDayResponseDto.class);

        assertThat(closeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(closeResp.getBody().status()).isEqualTo("pending");

        ResponseEntity<EventLockStatusDto> statusResp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/lock-status", HttpMethod.GET,
                new HttpEntity<>(adminHeaders()), EventLockStatusDto.class);
        assertThat(statusResp.getBody().locked()).isTrue();
    }

    @Test
    void lockStatus_neverOpened_returnsLockedFalseNot404() {
        Long eventId = createEventInDb();

        ResponseEntity<EventLockStatusDto> statusResp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/lock-status", HttpMethod.GET,
                new HttpEntity<>(adminHeaders()), EventLockStatusDto.class);

        assertThat(statusResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResp.getBody().locked()).isFalse();
        assertThat(statusResp.getBody().generation()).isEqualTo(0L);
    }

    @Test
    void openDay_nonexistentEvent_returns404() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/localday/events/999999999/lifecycle/open", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-missing-event"), adminHeaders()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void openDay_calledTwice_generationIncrementsEachTime() {
        Long eventId = createEventInDb();

        ResponseEntity<EventLockStatusDto> resp1 = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/open", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-open-twice"), adminHeaders()), EventLockStatusDto.class);
        ResponseEntity<EventLockStatusDto> resp2 = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/open", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-open-twice"), adminHeaders()), EventLockStatusDto.class);

        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp2.getBody().generation()).isGreaterThan(resp1.getBody().generation());
    }

    @Test
    void openDay_blankInstanceId_returns400() {
        Long eventId = createEventInDb();

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/open", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", ""), adminHeaders()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(eventSyncGenerationRepository.findById(eventId)).isEmpty();
    }

    // --- Pre-cache ---

    @Test
    void preCache_confirmedEntryAndRace_returnsResolvedDisplayData() {
        Long eventId = createEventInDb();
        RacingClass racingClass = createRacingClassInDb("PreCacheClass");
        Long eventClassId = createEventClassInDb(eventId, racingClass.getId());

        User racer = new User();
        racer.setEmail("racer-" + UUID.randomUUID() + "@test.com");
        racer.setPasswordHash(passwordEncoder.encode("racerPass123"));
        racer.setFirstName("Ricky");
        racer.setLastName("Racer");
        racer.setRoles(Set.of(Role.RACER));
        Instant now = Instant.now();
        racer.setCreatedAt(now);
        racer.setUpdatedAt(now);
        racer = userRepository.save(racer);

        Car car = new Car();
        car.setUserId(racer.getId());
        car.setName("Team Losi 22S");
        car.setArchived(false);
        car.setCreatedAt(now);
        car.setUpdatedAt(now);
        car = carRepository.save(car);

        Entry entry = new Entry();
        entry.setUserId(racer.getId());
        entry.setEventId(eventId);
        entry.setEventClassId(eventClassId);
        entry.setCarId(car.getId());
        entry.setTransponderNumberSnapshot("TX-12345");
        entry.setStatus(EntryStatus.CONFIRMED);
        entry.setSubmittedAt(now);
        entry.setUpdatedAt(now);
        entryRepository.save(entry);

        Round round = new Round();
        round.setEventId(eventId);
        round.setType(RoundType.QUALIFIER);
        round.setRoundNumber(1);
        round.setSequenceInEvent(1);
        round.setStatus(RoundStatus.PENDING);
        round.setCreatedAt(now);
        round.setUpdatedAt(now);
        round = roundRepository.save(round);

        Race race = new Race();
        race.setRoundId(round.getId());
        race.setEventClassId(eventClassId);
        race.setHeatNumber(1);
        race.setSequenceInRound(1);
        race.setStartType(dev.monkeypatch.rctiming.domain.race.StartType.GRID);
        race.setStatus(RaceStatus.PENDING);
        race.setCreatedAt(now);
        race.setUpdatedAt(now);
        raceRepository.save(race);

        ResponseEntity<PreCacheResponseDto> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-A"), adminHeaders()), PreCacheResponseDto.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        PreCacheResponseDto body = resp.getBody();
        assertThat(body.event().id()).isEqualTo(eventId);
        assertThat(body.entries()).hasSize(1);
        var entryDto = body.entries().get(0);
        assertThat(entryDto.transponderNumber()).isEqualTo("TX-12345");
        assertThat(entryDto.racerName()).isEqualTo("Ricky Racer");
        assertThat(entryDto.carName()).isEqualTo("Team Losi 22S");
        assertThat(entryDto.className()).isEqualTo(racingClass.getName());

        assertThat(body.schedule()).hasSize(1);
        var scheduleDto = body.schedule().get(0);
        assertThat(scheduleDto.roundNumber()).isEqualTo(1);
        assertThat(scheduleDto.heatNumber()).isEqualTo(1);
        assertThat(scheduleDto.sequence()).isEqualTo(1);
        assertThat(scheduleDto.className()).isEqualTo(racingClass.getName());
        assertThat(scheduleDto.status()).isEqualTo("PENDING");
    }

    @Test
    void preCache_mintsCredentialForOfficial_pinVerifiesAgainstHash() {
        Long eventId = createEventInDb();

        ResponseEntity<PreCacheResponseDto> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-B"), adminHeaders()), PreCacheResponseDto.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var credentialDto = resp.getBody().officialCredentials().stream()
                .filter(c -> c.cloudUserId().equals(adminUserId))
                .findFirst()
                .orElseThrow();
        assertThat(credentialDto.pin()).matches("\\d{6}");

        var credentialRow = localdayCredentialRepository.findByEventIdAndUserId(eventId, adminUserId).orElseThrow();
        assertThat(passwordEncoder.matches(credentialDto.pin(), credentialRow.getSecretHash())).isTrue();
    }

    @Test
    void preCache_calledTwice_pinReplacedAndSingleRow() {
        Long eventId = createEventInDb();

        ResponseEntity<PreCacheResponseDto> resp1 = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-C"), adminHeaders()), PreCacheResponseDto.class);
        String pin1 = resp1.getBody().officialCredentials().stream()
                .filter(c -> c.cloudUserId().equals(adminUserId)).findFirst().orElseThrow().pin();

        ResponseEntity<PreCacheResponseDto> resp2 = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-C"), adminHeaders()), PreCacheResponseDto.class);
        String pin2 = resp2.getBody().officialCredentials().stream()
                .filter(c -> c.cloudUserId().equals(adminUserId)).findFirst().orElseThrow().pin();

        assertThat(pin2).isNotEqualTo(pin1);

        long rowCount = localdayCredentialRepository.findAll().stream()
                .filter(c -> c.getEventId().equals(eventId) && c.getUserId().equals(adminUserId))
                .count();
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void preCache_calledTwice_doesNotChangeSyncGeneration() {
        Long eventId = createEventInDb();

        restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/open", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-precache-gen"), adminHeaders()), EventLockStatusDto.class);
        long generationAfterOpen = eventSyncGenerationRepository.findById(eventId).orElseThrow().getGeneration();

        restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-precache-gen"), adminHeaders()), PreCacheResponseDto.class);
        restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-precache-gen"), adminHeaders()), PreCacheResponseDto.class);

        long generationAfterPreCache = eventSyncGenerationRepository.findById(eventId).orElseThrow().getGeneration();
        assertThat(generationAfterPreCache).isEqualTo(generationAfterOpen);
    }

    @Test
    void preCache_blankInstanceId_returns400AndCreatesNoCredentialRow() {
        Long eventId = createEventInDb();

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", ""), adminHeaders()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(localdayCredentialRepository.findByEventIdAndUserId(eventId, adminUserId)).isEmpty();
    }

    @Test
    void preCache_missingInstanceId_returns400AndCreatesNoCredentialRow() {
        Long eventId = createEventInDb();

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of(), adminHeaders()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(localdayCredentialRepository.findByEventIdAndUserId(eventId, adminUserId)).isEmpty();
    }

    @Test
    void preCache_mintsInstanceSecret_distinctFromPinsAndVerifiable() {
        Long eventId = createEventInDb();

        ResponseEntity<PreCacheResponseDto> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-D"), adminHeaders()), PreCacheResponseDto.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secret = resp.getBody().instanceSecret().secret();
        assertThat(secret).isNotBlank();

        boolean matchesAnyPin = resp.getBody().officialCredentials().stream()
                .anyMatch(c -> c.pin().equals(secret));
        assertThat(matchesAnyPin).isFalse();

        var secretRow = localdayInstanceSecretRepository.findByEventIdAndInstanceId(eventId, "instance-D").orElseThrow();
        assertThat(passwordEncoder.matches(secret, secretRow.getSecretHash())).isTrue();
    }

    // --- AuthN/AuthZ ---

    @Test
    void openDay_noAuthHeader_returns401() {
        Long eventId = createEventInDb();

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/localday/events/" + eventId + "/lifecycle/open", null, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void openDay_racerToken_returns403() {
        Long eventId = createEventInDb();
        String racerToken = createRacerAndLogin();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(racerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/open", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-racer-forbidden"), headers), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void preCache_racerToken_returns403AndCreatesNoCredentialRows() {
        Long eventId = createEventInDb();
        String racerToken = createRacerAndLogin();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(racerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "instance-rejected"), headers), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(localdayInstanceSecretRepository.findByEventIdAndInstanceId(eventId, "instance-rejected")).isEmpty();
        assertThat(localdayCredentialRepository.findByEventIdAndUserId(eventId, adminUserId)).isEmpty();
    }

    @Test
    void preCache_noAuthHeader_returns401AndCreatesNoCredentialRows() {
        Long eventId = createEventInDb();

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/localday/events/" + eventId + "/pre-cache", null, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(localdayCredentialRepository.findByEventIdAndUserId(eventId, adminUserId)).isEmpty();
    }

    // --- helpers ---

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private void createAdminUser(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName("Localday");
        user.setLastName("Admin");
        user.setRoles(Set.of(Role.ADMIN));
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
    }

    private String createRacerAndLogin() {
        String email = "racer-login-" + UUID.randomUUID() + "@test.com";
        User racer = new User();
        racer.setEmail(email);
        racer.setPasswordHash(passwordEncoder.encode("racerPass123"));
        racer.setFirstName("Only");
        racer.setLastName("Racer");
        racer.setRoles(Set.of(Role.RACER));
        Instant now = Instant.now();
        racer.setCreatedAt(now);
        racer.setUpdatedAt(now);
        userRepository.save(racer);

        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(email, "racerPass123"),
                AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginResp.getBody().accessToken();
    }

    private Long createEventInDb() {
        Event event = new Event();
        event.setName("Localday Test Event " + UUID.randomUUID().toString().substring(0, 8));
        event.setEventDate(LocalDate.of(2026, 9, 1));
        Instant now = Instant.now();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return eventRepository.save(event).getId();
    }

    private RacingClass createRacingClassInDb(String name) {
        RacingClass rc = new RacingClass();
        rc.setName(name + "-" + UUID.randomUUID().toString().substring(0, 6));
        Instant now = Instant.now();
        rc.setCreatedAt(now);
        rc.setUpdatedAt(now);
        return racingClassRepository.save(rc);
    }

    private Long createEventClassInDb(Long eventId, Long racingClassId) {
        EventClass ec = new EventClass();
        ec.setEventId(eventId);
        ec.setRacingClassId(racingClassId);
        ec.setConfigSnapshot(new TimedRaceConfig(5, StartType.GRID, QualifyingType.FASTEST_LAP, 1, 0));
        return eventClassRepository.save(ec).getId();
    }
}
