package dev.monkeypatch.rctiming.api.localday;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.api.localday.dto.DeviceLossResponseDto;
import dev.monkeypatch.rctiming.api.localday.dto.EventLockStatusDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheResponseDto;
import dev.monkeypatch.rctiming.api.localday.dto.SnapshotIngestResponseDto;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.localday.DeviceLossAuditRepository;
import dev.monkeypatch.rctiming.domain.localday.EventOfflineLock;
import dev.monkeypatch.rctiming.domain.localday.EventOfflineLockRepository;
import dev.monkeypatch.rctiming.domain.localday.LocaldayInstanceSecret;
import dev.monkeypatch.rctiming.domain.localday.LocaldayInstanceSecretRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DeviceLossController} (F5, R16, R17) against the real
 * Testcontainers Postgres, mirroring {@code DayLifecyclePreCacheIT}'s convention.
 */
class DeviceLossControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    LocaldayInstanceSecretRepository localdayInstanceSecretRepository;

    @Autowired
    EventOfflineLockRepository eventOfflineLockRepository;

    @Autowired
    DeviceLossAuditRepository deviceLossAuditRepository;

    private String adminToken;
    private Long adminUserId;

    @BeforeEach
    void setUp() {
        String email = "device-loss-admin-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("adminPass123"));
        user.setFirstName("DeviceLoss");
        user.setLastName("Admin");
        user.setRoles(Set.of(Role.ADMIN));
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        adminUserId = userRepository.save(user).getId();

        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, "adminPass123"), AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        adminToken = loginResp.getBody().accessToken();
    }

    // --- Happy path ---

    @Test
    void declare_validInstance_invalidatesSecretUnlocksAndFlagsIncompleteDataAndWritesAudit() {
        Long eventId = createEventInDb();
        String instanceId = "instance-lost-1";
        String secret = openAndPreCache(eventId, instanceId);
        assertThat(localdayInstanceSecretRepository.findByEventIdAndInstanceId(eventId, instanceId)
                .orElseThrow().getInvalidatedAt()).isNull();

        ResponseEntity<DeviceLossResponseDto> resp = declareDeviceLoss(eventId, instanceId, "Laptop physically destroyed on-site");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().incompleteData()).isTrue();

        LocaldayInstanceSecret secretRow =
                localdayInstanceSecretRepository.findByEventIdAndInstanceId(eventId, instanceId).orElseThrow();
        assertThat(secretRow.getInvalidatedAt()).isNotNull();

        EventOfflineLock lock = eventOfflineLockRepository.findById(eventId).orElseThrow();
        assertThat(lock.getUnlockedAt()).isNotNull();
        assertThat(lock.isIncompleteData()).isTrue();
        assertThat(lock.getIncompleteDataDeclaredAt()).isNotNull();

        assertThat(deviceLossAuditRepository.findAll().stream()
                .anyMatch(a -> a.getEventId().equals(eventId)
                        && a.getInstanceId().equals(instanceId)
                        && a.getAdminUserId().equals(adminUserId)
                        && "Laptop physically destroyed on-site".equals(a.getReason())))
                .isTrue();
    }

    // --- AE5: the original (now-superseded) instance's later snapshot push is rejected ---

    @Test
    void declare_thenOriginalInstanceSnapshotPush_rejectedAsUnauthenticated() {
        Long eventId = createEventInDb();
        String instanceId = "instance-lost-2";
        String secret = openAndPreCache(eventId, instanceId);

        declareDeviceLoss(eventId, instanceId, "Unresponsive, confirmed powered off on-site");

        ResponseEntity<Map> pushResp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/snapshots", HttpMethod.POST,
                new HttpEntity<>(snapshotBody(instanceId, 1L, "snap-after-declared-lost"),
                        instanceSecretHeaders(secret)),
                Map.class);

        // The invalidated secret (KTD9) rejects this before the generation comparison ever runs
        // — the plan's own "backstop" framing: generation-fencing exists for the case where the
        // secret hasn't been invalidated, not as the primary rejection path here.
        assertThat(pushResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- AuthZ: gated to ADMIN specifically, not RACE_DIRECTOR ---

    @Test
    void declare_raceDirectorToken_returns403() {
        Long eventId = createEventInDb();
        String instanceId = "instance-lost-3";
        openAndPreCache(eventId, instanceId);
        String raceDirectorToken = createRaceDirectorAndLogin();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(raceDirectorToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/device-loss", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", instanceId, "reason", "should be forbidden"), headers), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(localdayInstanceSecretRepository.findByEventIdAndInstanceId(eventId, instanceId)
                .orElseThrow().getInvalidatedAt()).isNull();
    }

    @Test
    void declare_noAuthHeader_returns401() {
        Long eventId = createEventInDb();
        String instanceId = "instance-lost-4";
        openAndPreCache(eventId, instanceId);

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/localday/events/" + eventId + "/device-loss",
                new HttpEntity<>(Map.of("instanceId", instanceId, "reason", "no auth")), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- Error path ---

    @Test
    void declare_unknownInstanceId_returns404() {
        Long eventId = createEventInDb();

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/device-loss", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", "never-existed", "reason", "n/a"), adminHeaders()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- helpers ---

    private String openAndPreCache(Long eventId, String instanceId) {
        restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/open", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", instanceId), adminHeaders()), EventLockStatusDto.class);
        ResponseEntity<PreCacheResponseDto> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", instanceId), adminHeaders()), PreCacheResponseDto.class);
        return resp.getBody().instanceSecret().secret();
    }

    private ResponseEntity<DeviceLossResponseDto> declareDeviceLoss(Long eventId, String instanceId, String reason) {
        return restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/device-loss", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", instanceId, "reason", reason), adminHeaders()),
                DeviceLossResponseDto.class);
    }

    private Map<String, Object> snapshotBody(String instanceId, long generation, String snapshotId) {
        return Map.of(
                "instanceId", instanceId,
                "generation", generation,
                "snapshotId", snapshotId,
                "payload", Map.of("capturedAt", Instant.now().toString(),
                        "results", List.of(), "standings", List.of(), "laps", List.of()));
    }

    private HttpHeaders instanceSecretHeaders(String secret) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(SnapshotIngestController.INSTANCE_SECRET_HEADER, secret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String createRaceDirectorAndLogin() {
        String email = "race-director-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("rdPass123"));
        user.setFirstName("Race");
        user.setLastName("Director");
        user.setRoles(Set.of(Role.RACE_DIRECTOR));
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, "rdPass123"), AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginResp.getBody().accessToken();
    }

    private Long createEventInDb() {
        Event event = new Event();
        event.setName("Device Loss Test Event " + UUID.randomUUID().toString().substring(0, 8));
        event.setEventDate(LocalDate.of(2026, 9, 1));
        Instant now = Instant.now();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return eventRepository.save(event).getId();
    }
}
