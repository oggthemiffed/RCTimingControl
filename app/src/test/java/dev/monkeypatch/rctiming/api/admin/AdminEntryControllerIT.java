package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.api.auth.RegisterRequest;
import dev.monkeypatch.rctiming.domain.entry.EntryAuditLog;
import dev.monkeypatch.rctiming.domain.entry.EntryAuditLogRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminEntryControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntryAuditLogRepository entryAuditLogRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private String adminToken;
    private Long adminUserId;

    private static final long OPEN_EVENT_ID = 1001L;
    private static final long OPEN_CLASS_ID = 2001L;

    @BeforeEach
    void setUp() {
        String email = "admin-entry-" + UUID.randomUUID() + "@test.com";
        adminUserId = createAdminUser(email, "adminPass123", Set.of(Role.ADMIN));
        var loginResp = restTemplate.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, "adminPass123"), AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        adminToken = loginResp.getBody().accessToken();
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminUpdateTransponder_returns200AndWritesAudit() {
        // Register racer, create transponder T1, submit entry
        RacerSession racer = registerRacer("audit-transponder");
        Long carId = createCar(racer.token());
        Long t1Id = createTransponder(racer.token(), uniqueNumber());

        var submitBody = Map.of("eventId", OPEN_EVENT_ID, "eventClassId", OPEN_CLASS_ID,
                                "carId", carId, "transponderId", t1Id);
        var submitResp = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.POST,
                new HttpEntity<>(submitBody, headersFor(racer.token())), Map.class);
        assertThat(submitResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long entryId = ((Number) ((Map<String, Object>) submitResp.getBody().get("entry")).get("id")).longValue();

        // Admin creates a second transponder (not owned by racer — D-12 admin backend)
        Long t2Id = createTransponder(racer.token(), uniqueNumber());
        String t2Number = getTransponderNumber(racer.token(), t2Id);

        // Admin PATCH transponder swap
        var patchBody = Map.of("transponderId", t2Id, "reason", "admin swap test");
        var patchResp = restTemplate.exchange("/api/v1/admin/entries/" + entryId + "/transponder",
                HttpMethod.PATCH, new HttpEntity<>(patchBody, adminHeaders()), Map.class);

        assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResp.getBody().get("transponderNumberSnapshot")).isEqualTo(t2Number);

        // Verify audit log row written
        List<EntryAuditLog> logs = entryAuditLogRepository.findByEntryIdOrderByCreatedAtAsc(entryId);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo("TRANSPONDER_SWAP");
        assertThat(logs.get(0).getAdminUserId()).isEqualTo(adminUserId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminMembershipOverride_returns200AndWritesAudit() {
        // Register racer without BRCA membership and submit to open class (confirmed)
        // Then manually create a PENDING entry to simulate a membership-blocked state
        // by using the entry service path directly via repo
        RacerSession racer = registerRacer("audit-override");
        Long carId = createCar(racer.token());
        Long transpId = createTransponder(racer.token(), uniqueNumber());

        // Submit to open class (no membership required) → CONFIRMED
        var submitBody = Map.of("eventId", OPEN_EVENT_ID, "eventClassId", OPEN_CLASS_ID,
                                "carId", carId, "transponderId", transpId);
        var submitResp = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.POST,
                new HttpEntity<>(submitBody, headersFor(racer.token())), Map.class);
        assertThat(submitResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long entryId = ((Number) ((Map<String, Object>) submitResp.getBody().get("entry")).get("id")).longValue();

        // Admin applies membership override (re-confirms and writes audit)
        var overrideBody = Map.of("reason", "captain verified membership at gate");
        var overrideResp = restTemplate.exchange("/api/v1/admin/entries/" + entryId + "/membership-override",
                HttpMethod.POST, new HttpEntity<>(overrideBody, adminHeaders()), Map.class);

        assertThat(overrideResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(overrideResp.getBody().get("status")).isEqualTo("CONFIRMED");

        // Verify audit log
        List<EntryAuditLog> logs = entryAuditLogRepository.findByEntryIdOrderByCreatedAtAsc(entryId);
        assertThat(logs).hasSizeGreaterThanOrEqualTo(1);
        EntryAuditLog overrideLog = logs.stream()
                .filter(l -> "MEMBERSHIP_OVERRIDE".equals(l.getAction()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No MEMBERSHIP_OVERRIDE audit row found"));
        assertThat(overrideLog.getReason()).isEqualTo("captain verified membership at gate");
        assertThat(overrideLog.getAdminUserId()).isEqualTo(adminUserId);
    }

    @Test
    void adminMembershipOverride_blankReason_returns400() {
        var resp = restTemplate.exchange("/api/v1/admin/entries/1/membership-override",
                HttpMethod.POST, new HttpEntity<>(Map.of("reason", ""), adminHeaders()),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void refereeAttemptMembershipOverride_returns403() {
        // Create referee user
        String email = "referee-" + UUID.randomUUID() + "@test.com";
        createAdminUser(email, "refPass123", Set.of(Role.REFEREE));
        var loginResp = restTemplate.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, "refPass123"), AuthResponse.class);
        String refereeToken = loginResp.getBody().accessToken();

        // Referee tries membership override (only ADMIN/RACE_DIRECTOR allowed)
        var resp = restTemplate.exchange("/api/v1/admin/entries/1/membership-override",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("reason", "ref attempt"), headersFor(refereeToken)),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @SuppressWarnings("unchecked")
    void refereeTransponderSwap_returns200() {
        // Referee can call PATCH /transponder (class-level rule includes REFEREE)
        String email = "referee2-" + UUID.randomUUID() + "@test.com";
        createAdminUser(email, "refPass456", Set.of(Role.REFEREE));
        var loginResp = restTemplate.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, "refPass456"), AuthResponse.class);
        String refereeToken = loginResp.getBody().accessToken();

        // Create an entry to swap
        RacerSession racer = registerRacer("referee-swap");
        Long carId = createCar(racer.token());
        Long t1Id = createTransponder(racer.token(), uniqueNumber());
        Long t2Id = createTransponder(racer.token(), uniqueNumber());

        var submitBody = Map.of("eventId", OPEN_EVENT_ID, "eventClassId", OPEN_CLASS_ID,
                                "carId", carId, "transponderId", t1Id);
        var submitResp = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.POST,
                new HttpEntity<>(submitBody, headersFor(racer.token())), Map.class);
        assertThat(submitResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long entryId = ((Number) ((Map<String, Object>) submitResp.getBody().get("entry")).get("id")).longValue();

        var patchBody = Map.of("transponderId", t2Id, "reason", "referee swap");
        var patchResp = restTemplate.exchange("/api/v1/admin/entries/" + entryId + "/transponder",
                HttpMethod.PATCH, new HttpEntity<>(patchBody, headersFor(refereeToken)), Map.class);
        assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void racerOnAdminEndpoint_returns403() {
        RacerSession racer = registerRacer("racer-admin-attempt");
        var resp = restTemplate.exchange("/api/v1/admin/entries/1/transponder",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("transponderId", 1), headersFor(racer.token())),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- helpers ---

    private record RacerSession(String token, Long userId) {}

    private RacerSession registerRacer(String prefix) {
        String email = prefix + "-" + UUID.randomUUID() + "@test.com";
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest("Test", "Racer", email, "password123"),
                AuthResponse.class);
        var loginResp = restTemplate.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, "password123"), AuthResponse.class);
        String token = loginResp.getBody().accessToken();
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return new RacerSession(token, userId);
    }

    private Long createAdminUser(String email, String password, Set<Role> roles) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setRoles(roles);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user).getId();
    }

    private Long createCar(String token) {
        var resp = restTemplate.exchange("/api/v1/racer/cars", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Test Car"), headersFor(token)), Map.class);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    private Long createTransponder(String token, String number) {
        var resp = restTemplate.exchange("/api/v1/racer/transponders", HttpMethod.POST,
                new HttpEntity<>(Map.of("transponderNumber", number, "label", "Tag"),
                        headersFor(token)), Map.class);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    @SuppressWarnings("unchecked")
    private String getTransponderNumber(String token, Long transponderId) {
        var body = restTemplate.exchange("/api/v1/racer/transponders", HttpMethod.GET,
                new HttpEntity<>(headersFor(token)), List.class);
        List<Map<String, Object>> transponders = (List<Map<String, Object>>) body.getBody();
        return transponders.stream()
                .filter(t -> transponderId.equals(((Number) t.get("id")).longValue()))
                .map(t -> (String) t.get("transponderNumber"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Transponder not found: " + transponderId));
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(adminToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders headersFor(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private static int counter = 0;

    private String uniqueNumber() {
        return "A" + System.nanoTime() + (++counter);
    }
}
