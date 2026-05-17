package dev.monkeypatch.rctiming.api.auth;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    private static final String BASE_URL = "/api/v1/auth";
    private static int counter = 0;

    /** Generate a unique email per test to avoid cross-test state */
    private String uniqueEmail() {
        return "test" + (++counter) + "@example.com";
    }

    @Test
    void register_validRequest_returns201() {
        String email = uniqueEmail();
        RegisterRequest request = new RegisterRequest("Alice", "Smith", email, "password123");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE_URL + "/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().email()).isEqualTo(email);
        assertThat(response.getBody().roles()).contains("RACER");
    }

    @Test
    void register_duplicateEmail_returns409() {
        String email = uniqueEmail();
        RegisterRequest request = new RegisterRequest("Bob", "Jones", email, "password123");

        ResponseEntity<AuthResponse> first = restTemplate.postForEntity(
                BASE_URL + "/register", request, AuthResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> second = restTemplate.postForEntity(
                BASE_URL + "/register", request, Map.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_invalidEmail_returns400() {
        RegisterRequest request = new RegisterRequest("Carol", "Brown", "not-an-email", "password123");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                BASE_URL + "/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_validCredentials_returns200WithTokenAndCookie() {
        String email = uniqueEmail();
        restTemplate.postForEntity(BASE_URL + "/register",
                new RegisterRequest("Dave", "Lee", email, "password123"), AuthResponse.class);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE_URL + "/login",
                new LoginRequest(email, "password123"),
                AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();

        // Check Set-Cookie header contains HttpOnly refresh_token
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotEmpty();
        String cookieHeader = cookies.get(0);
        assertThat(cookieHeader).contains("refresh_token=");
        assertThat(cookieHeader).containsIgnoringCase("HttpOnly");
    }

    @Test
    void login_invalidPassword_returns401() {
        String email = uniqueEmail();
        restTemplate.postForEntity(BASE_URL + "/register",
                new RegisterRequest("Eve", "White", email, "password123"), AuthResponse.class);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                BASE_URL + "/login",
                new LoginRequest(email, "wrongPassword"),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_nonexistentEmail_returns401() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                BASE_URL + "/login",
                new LoginRequest("nobody@nowhere.com", "anypassword"),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_validCookie_returnsNewAccessToken() {
        String email = uniqueEmail();
        restTemplate.postForEntity(BASE_URL + "/register",
                new RegisterRequest("Frank", "Black", email, "password123"), AuthResponse.class);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                BASE_URL + "/login",
                new LoginRequest(email, "password123"),
                AuthResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Extract the raw cookie value from Set-Cookie header
        List<String> setCookieHeaders = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeaders).isNotEmpty();
        // Find the refresh_token cookie (may be one of several Set-Cookie values)
        String refreshCookieHeader = setCookieHeaders.stream()
                .filter(c -> c.startsWith("refresh_token="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No refresh_token cookie in: " + setCookieHeaders));
        String rawCookieValue = extractCookieValue(refreshCookieHeader, "refresh_token");
        assertThat(rawCookieValue).isNotBlank();

        // POST to refresh endpoint with the cookie in Cookie header
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "refresh_token=" + rawCookieValue);
        ResponseEntity<AuthResponse> refreshResponse = restTemplate.exchange(
                BASE_URL + "/refresh",
                org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(headers),
                AuthResponse.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        assertThat(refreshResponse.getBody().accessToken()).isNotBlank();
    }

    @Test
    void refresh_invalidCookie_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "refresh_token=garbage-invalid-token-value");
        RequestEntity<Void> request = RequestEntity
                .post(URI.create(BASE_URL + "/refresh"))
                .headers(headers)
                .build();

        ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void passwordResetRequest_existingEmail_returns200() {
        String email = uniqueEmail();
        restTemplate.postForEntity(BASE_URL + "/register",
                new RegisterRequest("Grace", "Green", email, "password123"), AuthResponse.class);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                BASE_URL + "/password-reset/request",
                new PasswordResetRequestDto(email),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void passwordResetRequest_nonexistentEmail_returns200() {
        // Email enumeration prevention: must return 200 even for unknown emails
        ResponseEntity<Void> response = restTemplate.postForEntity(
                BASE_URL + "/password-reset/request",
                new PasswordResetRequestDto("unknown@nowhere.com"),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void passwordResetConfirm_expiredToken_returns400or410() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                BASE_URL + "/password-reset/confirm",
                new PasswordResetConfirmDto("invalidtokenvalue", "newPassword123"),
                Map.class);

        assertThat(response.getStatusCode().value())
                .as("Expected 400 or 410 for invalid/expired reset token")
                .isIn(400, 410);
    }

    // --- helpers ---

    protected String registerAndLogin(String email, String password) {
        restTemplate.postForEntity(BASE_URL + "/register",
                new RegisterRequest("Test", "User", email, password), AuthResponse.class);
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                BASE_URL + "/login",
                new LoginRequest(email, password),
                AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginResp.getBody().accessToken();
    }

    protected HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private String extractCookieValue(String setCookieHeader, String cookieName) {
        for (String part : setCookieHeader.split(";")) {
            part = part.trim();
            if (part.startsWith(cookieName + "=")) {
                return part.substring((cookieName + "=").length());
            }
        }
        return null;
    }
}
