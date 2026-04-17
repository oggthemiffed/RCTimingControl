package dev.monkeypatch.rctiming.api.racer;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.api.auth.RegisterRequest;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RacerProfileControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    // --- helper types ---

    private record RacerSession(String token, Long userId) {}

    private RacerSession registerAndLoginRacer() {
        String email = "racer-" + UUID.randomUUID() + "@test.com";
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest("Test", "Racer", email, "password123"),
                Map.class);
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(email, "password123"),
                AuthResponse.class);
        String accessToken = loginResp.getBody().accessToken();
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return new RacerSession(accessToken, userId);
    }

    private HttpHeaders racerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    // --- tests ---

    @Test
    void getProfile_returnsIdentityAndContact() {
        RacerSession session = registerAndLoginRacer();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/racer/profile", HttpMethod.GET,
                new HttpEntity<>(racerHeaders(session.token())),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("email")).isNotNull();
        assertThat(body.get("firstName")).isEqualTo("Test");
        assertThat(body.get("lastName")).isEqualTo("Racer");
        assertThat(body.get("memberships")).isInstanceOf(List.class);
        assertThat((List<?>) body.get("memberships")).isEmpty();
        assertThat(body.get("classRatings")).isInstanceOf(List.class);
        assertThat((List<?>) body.get("classRatings")).isEmpty();
    }

    @Test
    void patchProfile_updatesOnlySuppliedFields() {
        RacerSession session = registerAndLoginRacer();

        // PATCH only phoneNumber
        restTemplate.exchange(
                "/api/v1/racer/profile", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("phoneNumber", "555-1234"), racerHeaders(session.token())),
                Map.class);

        ResponseEntity<Map> getResp = restTemplate.exchange(
                "/api/v1/racer/profile", HttpMethod.GET,
                new HttpEntity<>(racerHeaders(session.token())),
                Map.class);

        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = getResp.getBody();
        assertThat(body.get("phoneNumber")).isEqualTo("555-1234");
        assertThat(body.get("firstName")).isEqualTo("Test"); // unchanged
    }

    @Test
    void patchProfile_ignoresEmailField() {
        RacerSession session = registerAndLoginRacer();
        String originalEmail = userRepository.findById(session.userId()).orElseThrow().getEmail();

        // Send raw JSON containing "email" — UpdateRacerProfileRequest record has no email component,
        // so Jackson drops it (spring.jackson.deserialization.fail-on-unknown-properties=false by default)
        String body = "{\"email\": \"hacker@evil.com\", \"firstName\": \"NewName\"}";
        HttpHeaders headers = racerHeaders(session.token());
        restTemplate.exchange(
                "/api/v1/racer/profile", HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                Map.class);

        String storedEmail = userRepository.findById(session.userId()).orElseThrow().getEmail();
        assertThat(storedEmail).isEqualTo(originalEmail);
    }

    @Test
    void patchProfile_ignoresRoleField() {
        RacerSession session = registerAndLoginRacer();

        // Attempt to elevate privileges via raw JSON
        String body = "{\"roles\": [\"ADMIN\"], \"firstName\": \"Hacker\"}";
        HttpHeaders headers = racerHeaders(session.token());
        restTemplate.exchange(
                "/api/v1/racer/profile", HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                Map.class);

        var storedRoles = userRepository.findById(session.userId()).orElseThrow().getRoles();
        assertThat(storedRoles).hasSize(1);
        assertThat(storedRoles.iterator().next().name()).isEqualTo("RACER");
    }

    @Test
    void addMembership_returns201() {
        RacerSession session = registerAndLoginRacer();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/racer/memberships", HttpMethod.POST,
                new HttpEntity<>(Map.of("governingBodyCode", "BRCA", "membershipNumber", "12345"),
                        racerHeaders(session.token())),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("governingBodyCode")).isEqualTo("BRCA");
        assertThat(response.getBody().get("membershipNumber")).isEqualTo("12345");

        // Confirm via GET
        ResponseEntity<List<Map>> listResp = restTemplate.exchange(
                "/api/v1/racer/memberships", HttpMethod.GET,
                new HttpEntity<>(racerHeaders(session.token())),
                new ParameterizedTypeReference<List<Map>>() {});
        assertThat(listResp.getBody()).hasSize(1);
    }

    @Test
    void addMembership_duplicate_returns409() {
        RacerSession session = registerAndLoginRacer();

        restTemplate.exchange(
                "/api/v1/racer/memberships", HttpMethod.POST,
                new HttpEntity<>(Map.of("governingBodyCode", "EFRA", "membershipNumber", "A001"),
                        racerHeaders(session.token())),
                Map.class);

        ResponseEntity<Map> second = restTemplate.exchange(
                "/api/v1/racer/memberships", HttpMethod.POST,
                new HttpEntity<>(Map.of("governingBodyCode", "EFRA", "membershipNumber", "A002"),
                        racerHeaders(session.token())),
                Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updateMembership_modifiesNumber() {
        RacerSession session = registerAndLoginRacer();

        restTemplate.exchange(
                "/api/v1/racer/memberships", HttpMethod.POST,
                new HttpEntity<>(Map.of("governingBodyCode", "BRCA", "membershipNumber", "OLD"),
                        racerHeaders(session.token())),
                Map.class);

        ResponseEntity<Map> updated = restTemplate.exchange(
                "/api/v1/racer/memberships/BRCA", HttpMethod.PUT,
                new HttpEntity<>(Map.of("governingBodyCode", "BRCA", "membershipNumber", "99999"),
                        racerHeaders(session.token())),
                Map.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("membershipNumber")).isEqualTo("99999");
    }

    @Test
    void removeMembership_returns204() {
        RacerSession session = registerAndLoginRacer();

        restTemplate.exchange(
                "/api/v1/racer/memberships", HttpMethod.POST,
                new HttpEntity<>(Map.of("governingBodyCode", "BRCA", "membershipNumber", "12345"),
                        racerHeaders(session.token())),
                Map.class);

        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                "/api/v1/racer/memberships/BRCA", HttpMethod.DELETE,
                new HttpEntity<>(racerHeaders(session.token())),
                Void.class);

        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<List<Map>> listResp = restTemplate.exchange(
                "/api/v1/racer/memberships", HttpMethod.GET,
                new HttpEntity<>(racerHeaders(session.token())),
                new ParameterizedTypeReference<List<Map>>() {});
        assertThat(listResp.getBody()).isEmpty();
    }
}
