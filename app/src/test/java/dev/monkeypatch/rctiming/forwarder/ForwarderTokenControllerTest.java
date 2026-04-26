package dev.monkeypatch.rctiming.forwarder;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.forwarder.dto.ForwarderTokenGenerateResponseDto;
import dev.monkeypatch.rctiming.forwarder.dto.ForwarderTokenStatusDto;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ForwarderTokenController (Plan 05-03).
 * Tests: 403 for non-ADMIN, 401 for unauthenticated, full lifecycle.
 */
class ForwarderTokenControllerTest extends AbstractIntegrationTest {

    private static final String ENDPOINT = "/api/v1/admin/forwarder/token";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ForwarderTokenRepository forwarderTokenRepository;

    private String adminToken;
    private String racerToken;

    @BeforeEach
    void setUp() {
        // Clean token table so each test starts from a known empty state
        forwarderTokenRepository.deleteAll();

        String adminEmail = "fwd-admin-" + UUID.randomUUID() + "@test.com";
        createUser(adminEmail, "adminPass123", Set.of(Role.ADMIN));
        adminToken = login(adminEmail, "adminPass123");

        String racerEmail = "fwd-racer-" + UUID.randomUUID() + "@test.com";
        createUser(racerEmail, "racerPass123", Set.of(Role.RACER));
        racerToken = login(racerEmail, "racerPass123");
    }

    @Test
    void getReturnsNoneWhenNoTokenGenerated() {
        ResponseEntity<ForwarderTokenStatusDto> response = restTemplate.exchange(
                ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(adminHeaders(adminToken)), ForwarderTokenStatusDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("NONE");
    }

    @Test
    void postGeneratesAndReturnsPlaintextOnce() {
        // POST returns 201 with plaintext token
        ResponseEntity<ForwarderTokenGenerateResponseDto> postResp = restTemplate.exchange(
                ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(adminHeaders(adminToken)), ForwarderTokenGenerateResponseDto.class);
        assertThat(postResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(postResp.getBody()).isNotNull();
        String token = postResp.getBody().token();
        assertThat(token).isNotNull().hasSizeGreaterThan(40);

        // Subsequent GET reveals status=ACTIVE but NOT the token value
        ResponseEntity<ForwarderTokenStatusDto> getResp = restTemplate.exchange(
                ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(adminHeaders(adminToken)), ForwarderTokenStatusDto.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody().status()).isEqualTo("ACTIVE");
        // Response body serialised as JSON must not contain the plaintext token
        ResponseEntity<String> rawGet = restTemplate.exchange(
                ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(adminHeaders(adminToken)), String.class);
        assertThat(rawGet.getBody()).doesNotContain(token);
    }

    @Test
    void deleteRevokesToken() {
        // Generate first
        restTemplate.exchange(ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(adminHeaders(adminToken)), ForwarderTokenGenerateResponseDto.class);

        // Revoke
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                ENDPOINT, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders(adminToken)), Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Status is REVOKED
        ResponseEntity<ForwarderTokenStatusDto> getResp = restTemplate.exchange(
                ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(adminHeaders(adminToken)), ForwarderTokenStatusDto.class);
        assertThat(getResp.getBody().status()).isEqualTo("REVOKED");
    }

    @Test
    void nonAdminUserGets403OnPost() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(adminHeaders(racerToken)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unauthenticatedGets401OnGet() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> resp = restTemplate.exchange(
                ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- helpers ---

    private HttpHeaders adminHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String login(String email, String password) {
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(email, password),
                AuthResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().accessToken();
    }

    private void createUser(String email, String password, Set<Role> roles) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRoles(roles);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
    }
}
