package dev.monkeypatch.rctiming.api.localday;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.api.localday.dto.EventLockStatusDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheResponseDto;
import dev.monkeypatch.rctiming.api.localday.dto.SnapshotIngestResponseDto;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.localday.EventSnapshotState;
import dev.monkeypatch.rctiming.domain.localday.EventSnapshotStateRepository;
import dev.monkeypatch.rctiming.domain.localday.EventSyncGenerationRepository;
import dev.monkeypatch.rctiming.domain.localday.LocaldaySnapshotRepository;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link SnapshotIngestController} against the real Testcontainers Postgres
 * (mirrors {@code DayLifecyclePreCacheIT}'s convention) — KTD4's generation-fencing and
 * snapshotId-idempotency checks, and KTD9's instance-secret authentication.
 */
class SnapshotIngestControllerIT extends AbstractIntegrationTest {

    private static final String SNAPSHOTS_PATH = "/snapshots";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EventSyncGenerationRepository eventSyncGenerationRepository;

    @Autowired
    LocaldaySnapshotRepository localdaySnapshotRepository;

    @Autowired
    EventSnapshotStateRepository eventSnapshotStateRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        String email = "snapshot-admin-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("adminPass123"));
        user.setFirstName("Snapshot");
        user.setLastName("Admin");
        user.setRoles(Set.of(Role.ADMIN));
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, "adminPass123"), AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        adminToken = loginResp.getBody().accessToken();
    }

    // --- Happy path ---

    @Test
    void ingest_generationEqualToStored_acceptedAndUpdatesSnapshotState() {
        Instance inst = openAndPreCache();

        ResponseEntity<SnapshotIngestResponseDto> resp = push(inst, inst.generation, "snap-1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().status()).isEqualTo("accepted");

        EventSnapshotState state = eventSnapshotStateRepository.findById(inst.eventId).orElseThrow();
        assertThat(state.getLastSyncedAt()).isNotNull();
        assertThat(state.getPayload()).contains("Touring Stock");
    }

    @Test
    void ingest_generationHigherThanStored_acceptedAndAdvancesStoredGeneration() {
        Instance inst = openAndPreCache();

        ResponseEntity<SnapshotIngestResponseDto> resp = push(inst, inst.generation + 5, "snap-higher");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(eventSyncGenerationRepository.findById(inst.eventId).orElseThrow().getGeneration())
                .isEqualTo(inst.generation + 5);
    }

    // --- Error path (KTD4) ---

    @Test
    void ingest_generationLowerThanStored_rejectedWith409AndDoesNotOverwrite() {
        Instance inst = openAndPreCache();
        // A second open() for the same event claims a strictly higher generation, simulating a
        // replacement instance having taken over — the original instance's own generation is now stale.
        openLifecycle(inst.eventId, "replacement-" + inst.instanceId);
        long higherGeneration = eventSyncGenerationRepository.findById(inst.eventId).orElseThrow().getGeneration();
        assertThat(higherGeneration).isGreaterThan(inst.generation);

        ResponseEntity<SnapshotIngestResponseDto> resp = push(inst, inst.generation, "snap-stale");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().status()).isEqualTo("superseded");
        assertThat(eventSyncGenerationRepository.findById(inst.eventId).orElseThrow().getGeneration())
                .isEqualTo(higherGeneration);
        assertThat(eventSnapshotStateRepository.findById(inst.eventId)).isEmpty();
    }

    // --- Idempotency ---

    @Test
    void ingest_repeatedSnapshotId_idempotentNoDuplicateRow() {
        Instance inst = openAndPreCache();
        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(snapshotBody(inst.instanceId, inst.generation, "snap-dup"), instanceHeaders(inst.secret));

        ResponseEntity<SnapshotIngestResponseDto> resp1 = restTemplate.exchange(
                url(inst.eventId), HttpMethod.POST, request, SnapshotIngestResponseDto.class);
        ResponseEntity<SnapshotIngestResponseDto> resp2 = restTemplate.exchange(
                url(inst.eventId), HttpMethod.POST, request, SnapshotIngestResponseDto.class);

        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.OK);

        long rowCount = localdaySnapshotRepository.findAll().stream()
                .filter(s -> s.getEventId().equals(inst.eventId) && s.getSnapshotId().equals("snap-dup"))
                .count();
        assertThat(rowCount).isEqualTo(1);
    }

    // --- Concurrent-generation convergence: proves the atomic compare-and-update, not a race ---

    @Test
    void ingest_concurrentDifferentGenerations_convergesToHigherRegardlessOfOrder() throws Exception {
        Instance inst = openAndPreCache();
        long low = inst.generation + 5;
        long high = inst.generation + 10;

        CountDownLatch bothReady = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<ResponseEntity<SnapshotIngestResponseDto>> lowPush = CompletableFuture.supplyAsync(() -> {
                bothReady.countDown();
                await(go);
                return push(inst, low, "snap-concurrent-low");
            }, pool);
            CompletableFuture<ResponseEntity<SnapshotIngestResponseDto>> highPush = CompletableFuture.supplyAsync(() -> {
                bothReady.countDown();
                await(go);
                return push(inst, high, "snap-concurrent-high");
            }, pool);

            bothReady.await();
            go.countDown();
            CompletableFuture.allOf(lowPush, highPush).get();
        } finally {
            pool.shutdown();
        }

        assertThat(eventSyncGenerationRepository.findById(inst.eventId).orElseThrow().getGeneration()).isEqualTo(high);
    }

    // --- AuthN (KTD9) ---

    @Test
    void ingest_missingInstanceSecretHeader_returns400() {
        Instance inst = openAndPreCache();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = restTemplate.exchange(url(inst.eventId), HttpMethod.POST,
                new HttpEntity<>(snapshotBody(inst.instanceId, inst.generation, "snap-no-header"), headers), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ingest_wrongInstanceSecret_returns401AndCreatesNoRows() {
        Instance inst = openAndPreCache();

        ResponseEntity<Map> resp = restTemplate.exchange(url(inst.eventId), HttpMethod.POST,
                new HttpEntity<>(snapshotBody(inst.instanceId, inst.generation, "snap-wrong-secret"),
                        instanceHeaders("not-the-real-secret")),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(eventSnapshotStateRepository.findById(inst.eventId)).isEmpty();
        assertThat(localdaySnapshotRepository.findAll().stream()
                .noneMatch(s -> s.getEventId().equals(inst.eventId) && s.getSnapshotId().equals("snap-wrong-secret")))
                .isTrue();
    }

    @Test
    void ingest_unknownInstanceId_returns401() {
        Instance inst = openAndPreCache();

        ResponseEntity<Map> resp = restTemplate.exchange(url(inst.eventId), HttpMethod.POST,
                new HttpEntity<>(snapshotBody("never-pre-cached-instance", inst.generation, "snap-unknown"),
                        instanceHeaders(inst.secret)),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- helpers ---

    private record Instance(Long eventId, String instanceId, String secret, long generation) {
    }

    private Instance openAndPreCache() {
        String instanceId = "instance-" + UUID.randomUUID();
        Long eventId = createEventInDb();
        openLifecycle(eventId, instanceId);

        ResponseEntity<PreCacheResponseDto> preCacheResp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/pre-cache", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", instanceId), adminHeaders()), PreCacheResponseDto.class);
        String secret = preCacheResp.getBody().instanceSecret().secret();
        long generation = eventSyncGenerationRepository.findById(eventId).orElseThrow().getGeneration();
        return new Instance(eventId, instanceId, secret, generation);
    }

    private void openLifecycle(Long eventId, String instanceId) {
        ResponseEntity<EventLockStatusDto> resp = restTemplate.exchange(
                "/api/v1/localday/events/" + eventId + "/lifecycle/open", HttpMethod.POST,
                new HttpEntity<>(Map.of("instanceId", instanceId), adminHeaders()), EventLockStatusDto.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<SnapshotIngestResponseDto> push(Instance inst, long generation, String snapshotId) {
        return restTemplate.exchange(url(inst.eventId), HttpMethod.POST,
                new HttpEntity<>(snapshotBody(inst.instanceId, generation, snapshotId), instanceHeaders(inst.secret)),
                SnapshotIngestResponseDto.class);
    }

    private String url(Long eventId) {
        return "/api/v1/localday/events/" + eventId + SNAPSHOTS_PATH;
    }

    private HttpHeaders instanceHeaders(String secret) {
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

    private Map<String, Object> snapshotBody(String instanceId, long generation, String snapshotId) {
        return Map.of(
                "instanceId", instanceId,
                "generation", generation,
                "snapshotId", snapshotId,
                "payload", Map.of(
                        "capturedAt", Instant.now().toString(),
                        "results", List.of(Map.of("className", "Touring Stock", "rows", List.of())),
                        "standings", List.of(),
                        "laps", List.of()));
    }

    private Long createEventInDb() {
        Event event = new Event();
        event.setName("Snapshot Ingest Test Event " + UUID.randomUUID().toString().substring(0, 8));
        event.setEventDate(LocalDate.of(2026, 9, 1));
        Instant now = Instant.now();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return eventRepository.save(event).getId();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
