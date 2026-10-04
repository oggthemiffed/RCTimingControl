package dev.monkeypatch.rctiming.api.auth;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private static final String BASE_URL = "/api/v1/auth";

    /** Creates a user with a unique email and the given roles; returns the email. Password is "password123". */
    private String createUser(Set<Role> roles) {
        String email = "auth-" + UUID.randomUUID() + "@example.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRoles(roles);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return email;
    }

    @Test
    void login_validCredentials_returns200WithTokenAndCookie() {
        String email = createUser(Set.of(Role.RACE_DIRECTOR));

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
        String email = createUser(Set.of(Role.RACE_DIRECTOR));

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
        String email = createUser(Set.of(Role.RACE_DIRECTOR));

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
    void login_accountWithNoOfficialRole_returns403() {
        String email = createUser(Set.of());

        ResponseEntity<Void> response = restTemplate.postForEntity(
                BASE_URL + "/login",
                new LoginRequest(email, "password123"),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    void login_noOfficialRoleAndWrongPassword_returns401() {
        // The 403 is only given once the password is right, so it reveals nothing new
        String email = createUser(Set.of());

        ResponseEntity<Void> response = restTemplate.postForEntity(
                BASE_URL + "/login",
                new LoginRequest(email, "wrongPassword"),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_accountThatLostItsOfficialRoles_returns403() {
        String email = createUser(Set.of(Role.REFEREE));
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                BASE_URL + "/login", new LoginRequest(email, "password123"), AuthResponse.class);
        String rawCookieValue = extractCookieValue(loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("refresh_token=")).findFirst().orElseThrow(), "refresh_token");
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setRoles(Set.of());
        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "refresh_token=" + rawCookieValue);
        ResponseEntity<Void> refreshResponse = restTemplate.exchange(
                BASE_URL + "/refresh",
                org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(headers),
                Void.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void registerAndPasswordResetEndpoints_areGone() {
        for (String path : List.of("/register", "/password-reset/request", "/password-reset/confirm")) {
            ResponseEntity<Void> response = restTemplate.postForEntity(BASE_URL + path, "{}", Void.class);
            // No longer permitted anonymously and no longer mapped
            assertThat(response.getStatusCode().value()).as(path).isIn(401, 403, 404);
        }
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

    // --- helpers ---

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
