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

class EntryControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    // V100 seed event and class IDs
    private static final long OPEN_EVENT_ID = 1001L;
    private static final long OPEN_CLASS_ID = 2001L;   // no membership requirement
    private static final long BRCA_CLASS_ID = 2002L;   // requires BRCA membership

    // --- core tests ---

    @Test
    @SuppressWarnings("unchecked")
    void submitEntry_returns201AndConfirmed() {
        RacerSession r = registerRacer("entry-submit");
        Long carId = createCar(r.token());
        Long transponderId = createTransponder(r.token(), uniqueNumber());

        var body = Map.of("eventId", OPEN_EVENT_ID, "eventClassId", OPEN_CLASS_ID,
                          "carId", carId, "transponderId", transponderId);
        var resp = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.POST,
                new HttpEntity<>(body, headersFor(r.token())), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> entry = (Map<String, Object>) resp.getBody().get("entry");
        assertThat(entry.get("status")).isEqualTo("CONFIRMED");
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitEntry_capturesTransponderSnapshot() {
        RacerSession r = registerRacer("entry-snapshot");
        Long carId = createCar(r.token());
        String tNumber = uniqueNumber();
        Long transponderId = createTransponder(r.token(), tNumber);

        var body = Map.of("eventId", OPEN_EVENT_ID, "eventClassId", OPEN_CLASS_ID,
                          "carId", carId, "transponderId", transponderId);
        var resp = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.POST,
                new HttpEntity<>(body, headersFor(r.token())), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> entry = (Map<String, Object>) resp.getBody().get("entry");
        assertThat(entry.get("transponderNumberSnapshot")).isEqualTo(tNumber);
    }

    @Test
    void submitEntry_membershipRequired_blocks422() {
        RacerSession r = registerRacer("entry-nomembership");
        Long carId = createCar(r.token());
        Long transponderId = createTransponder(r.token(), uniqueNumber());

        // eventClassId 2002 requires BRCA — racer has no membership
        var body = Map.of("eventId", OPEN_EVENT_ID, "eventClassId", BRCA_CLASS_ID,
                          "carId", carId, "transponderId", transponderId);
        var resp = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.POST,
                new HttpEntity<>(body, headersFor(r.token())), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitEntry_membershipHeld_allows201() {
        RacerSession r = registerRacer("entry-hasmembership");
        Long carId = createCar(r.token());
        Long transponderId = createTransponder(r.token(), uniqueNumber());

        // Add BRCA membership for this racer
        var membershipBody = Map.of("governingBodyCode", "BRCA", "membershipNumber", "BRCA-12345");
        restTemplate.exchange("/api/v1/racer/memberships", HttpMethod.POST,
                new HttpEntity<>(membershipBody, headersFor(r.token())), Map.class);

        var body = Map.of("eventId", OPEN_EVENT_ID, "eventClassId", BRCA_CLASS_ID,
                          "carId", carId, "transponderId", transponderId);
        var resp = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.POST,
                new HttpEntity<>(body, headersFor(r.token())), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void withdrawEntry_returns204() {
        RacerSession r = registerRacer("entry-withdraw");
        Long carId = createCar(r.token());
        Long transponderId = createTransponder(r.token(), uniqueNumber());

        // Submit entry
        var body = Map.of("eventId", OPEN_EVENT_ID, "eventClassId", OPEN_CLASS_ID,
                          "carId", carId, "transponderId", transponderId);
        var submitResp = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.POST,
                new HttpEntity<>(body, headersFor(r.token())), Map.class);
        assertThat(submitResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> entry = (Map<String, Object>) submitResp.getBody().get("entry");
        Long entryId = ((Number) entry.get("id")).longValue();

        // Withdraw
        var deleteResp = restTemplate.exchange("/api/v1/racer/entries/" + entryId,
                HttpMethod.DELETE, new HttpEntity<>(headersFor(r.token())), Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // GET history should show withdrawn entry
        var histResp = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.GET,
                new HttpEntity<>(headersFor(r.token())),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertThat(histResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        boolean hasWithdrawn = histResp.getBody().stream()
                .anyMatch(e -> "WITHDRAWN".equals(e.get("status")));
        assertThat(hasWithdrawn).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void withdrawAnotherUsersEntry_returns404() {
        RacerSession racerA = registerRacer("entry-withdrawA");
        Long carId = createCar(racerA.token());
        Long transponderId = createTransponder(racerA.token(), uniqueNumber());

        // RacerA submits
        var body = Map.of("eventId", OPEN_EVENT_ID, "eventClassId", OPEN_CLASS_ID,
                          "carId", carId, "transponderId", transponderId);
        var submitResp = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.POST,
                new HttpEntity<>(body, headersFor(racerA.token())), Map.class);
        assertThat(submitResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> entry = (Map<String, Object>) submitResp.getBody().get("entry");
        Long entryId = ((Number) entry.get("id")).longValue();

        // RacerB tries to withdraw racerA's entry
        RacerSession racerB = registerRacer("entry-withdrawB");
        var deleteResp = restTemplate.exchange("/api/v1/racer/entries/" + entryId,
                HttpMethod.DELETE, new HttpEntity<>(headersFor(racerB.token())), Map.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listEntries_returnsOwnHistory() {
        RacerSession racerA = registerRacer("entry-histA");
        Long carA = createCar(racerA.token());
        Long transpA = createTransponder(racerA.token(), uniqueNumber());

        RacerSession racerB = registerRacer("entry-histB");
        Long carB = createCar(racerB.token());
        Long transpB = createTransponder(racerB.token(), uniqueNumber());

        // RacerA submits an entry
        var bodyA = Map.of("eventId", OPEN_EVENT_ID, "eventClassId", OPEN_CLASS_ID,
                           "carId", carA, "transponderId", transpA);
        var submitA = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.POST,
                new HttpEntity<>(bodyA, headersFor(racerA.token())), Map.class);
        assertThat(submitA.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // RacerB's history should be empty
        var histB = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.GET,
                new HttpEntity<>(headersFor(racerB.token())),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertThat(histB.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(histB.getBody()).isEmpty();

        // RacerA's history should have one entry
        var histA = restTemplate.exchange("/api/v1/racer/entries", HttpMethod.GET,
                new HttpEntity<>(headersFor(racerA.token())),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertThat(histA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(histA.getBody()).hasSize(1);
    }

    @Test
    void anonymous_returns401() {
        var resp = restTemplate.getForEntity("/api/v1/racer/entries", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResp.getBody().accessToken();
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return new RacerSession(token, userId);
    }

    private Long createCar(String token) {
        var resp = restTemplate.exchange("/api/v1/racer/cars", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Test Car"), headersFor(token)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    private Long createTransponder(String token, String number) {
        var resp = restTemplate.exchange("/api/v1/racer/transponders", HttpMethod.POST,
                new HttpEntity<>(Map.of("transponderNumber", number, "label", "Tag"),
                        headersFor(token)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    private HttpHeaders headersFor(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private static int transponderCounter = 0;

    private String uniqueNumber() {
        return "T" + System.nanoTime() + (++transponderCounter);
    }
}
