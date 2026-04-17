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

class TransponderControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    // --- helpers ---

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

    private Map createTransponder(String token, String number, String label) {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/racer/transponders", HttpMethod.POST,
                new HttpEntity<>(Map.of("transponderNumber", number, "label", label),
                        racerHeaders(token)),
                Map.class);
        return resp.getBody();
    }

    // --- tests ---

    @Test
    void createTransponder_returns201() {
        RacerSession session = registerAndLoginRacer();
        String number = "T-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/racer/transponders", HttpMethod.POST,
                new HttpEntity<>(Map.of("transponderNumber", number, "label", "My tag"),
                        racerHeaders(session.token())),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("transponderNumber")).isEqualTo(number);
        assertThat(response.getBody().get("label")).isEqualTo("My tag");
        assertThat(response.getBody().get("id")).isNotNull();
    }

    @Test
    void listTransponders_returnsUsersOwnOnly() {
        RacerSession racerA = registerAndLoginRacer();
        RacerSession racerB = registerAndLoginRacer();

        String numA = "A-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String numB = "B-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        createTransponder(racerA.token(), numA, "Racer A transponder");
        createTransponder(racerB.token(), numB, "Racer B transponder");

        ResponseEntity<List<Map>> listA = restTemplate.exchange(
                "/api/v1/racer/transponders", HttpMethod.GET,
                new HttpEntity<>(racerHeaders(racerA.token())),
                new ParameterizedTypeReference<List<Map>>() {});

        assertThat(listA.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map> items = listA.getBody();
        assertThat(items).isNotNull();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("transponderNumber")).isEqualTo(numA);
    }

    @Test
    void createTransponder_duplicate_returns409() {
        RacerSession racerA = registerAndLoginRacer();
        RacerSession racerB = registerAndLoginRacer();

        String sharedNumber = "DUP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        restTemplate.exchange(
                "/api/v1/racer/transponders", HttpMethod.POST,
                new HttpEntity<>(Map.of("transponderNumber", sharedNumber, "label", "first"),
                        racerHeaders(racerA.token())),
                Map.class);

        ResponseEntity<Map> second = restTemplate.exchange(
                "/api/v1/racer/transponders", HttpMethod.POST,
                new HttpEntity<>(Map.of("transponderNumber", sharedNumber, "label", "second"),
                        racerHeaders(racerB.token())),
                Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void deleteTransponder_returns204() {
        RacerSession session = registerAndLoginRacer();
        String number = "DEL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Map created = createTransponder(session.token(), number, "to delete");
        Long id = ((Number) created.get("id")).longValue();

        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                "/api/v1/racer/transponders/" + id, HttpMethod.DELETE,
                new HttpEntity<>(racerHeaders(session.token())),
                Void.class);

        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<List<Map>> listResp = restTemplate.exchange(
                "/api/v1/racer/transponders", HttpMethod.GET,
                new HttpEntity<>(racerHeaders(session.token())),
                new ParameterizedTypeReference<List<Map>>() {});
        assertThat(listResp.getBody()).isEmpty();
    }

    @Test
    void deleteAnotherUsersTransponder_returns404() {
        RacerSession racerA = registerAndLoginRacer();
        RacerSession racerB = registerAndLoginRacer();

        String number = "XOWN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 7);
        Map created = createTransponder(racerA.token(), number, "racerA transponder");
        Long id = ((Number) created.get("id")).longValue();

        // racerB attempts to delete racerA's transponder
        ResponseEntity<Map> deleteResp = restTemplate.exchange(
                "/api/v1/racer/transponders/" + id, HttpMethod.DELETE,
                new HttpEntity<>(racerHeaders(racerB.token())),
                Map.class);

        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void anonymous_returns401() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/racer/transponders", HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
