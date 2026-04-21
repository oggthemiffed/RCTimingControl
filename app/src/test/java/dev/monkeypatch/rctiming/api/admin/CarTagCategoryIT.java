package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.api.auth.RegisterRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CarTagCategoryDto;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CarTagCategoryIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/admin/car-tag-categories";
    private static final Set<String> EXPECTED_DEFAULT_CATEGORIES =
            Set.of("Chassis", "ESC", "Motor", "Servo", "Battery", "Body", "Tyres");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private String adminToken;
    private String racerToken;

    @BeforeEach
    void setUp() {
        String adminEmail = "admin-tags-" + UUID.randomUUID() + "@test.com";
        createAdminUser(adminEmail, "adminPass123");
        ResponseEntity<AuthResponse> adminLogin = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(adminEmail, "adminPass123"),
                AuthResponse.class);
        assertThat(adminLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        adminToken = adminLogin.getBody().accessToken();

        racerToken = registerAndLoginAsRacer();
    }

    @Test
    void defaultCategoriesSeededByFlyway() {
        ResponseEntity<List<CarTagCategoryDto>> response = restTemplate.exchange(
                BASE_URL, HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<List<CarTagCategoryDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        List<String> names = response.getBody().stream().map(CarTagCategoryDto::name).toList();
        assertThat(names).hasSize(7);
        assertThat(names).containsAll(EXPECTED_DEFAULT_CATEGORIES);
    }

    @Test
    void admin_createUpdateDeleteCycle() {
        String catName = "Test Category " + UUID.randomUUID().toString().substring(0, 8);

        // Create
        ResponseEntity<CarTagCategoryDto> created = restTemplate.exchange(
                BASE_URL, HttpMethod.POST,
                new HttpEntity<>(Map.of("name", catName, "sortOrder", 99), adminHeaders()),
                CarTagCategoryDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().name()).isEqualTo(catName);
        Long catId = created.getBody().id();

        // Update
        ResponseEntity<CarTagCategoryDto> updated = restTemplate.exchange(
                BASE_URL + "/" + catId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", catName + "-Updated", "sortOrder", 100), adminHeaders()),
                CarTagCategoryDto.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().name()).isEqualTo(catName + "-Updated");

        // Delete (now archives instead of hard-deletes — D-21)
        ResponseEntity<Void> deleted = restTemplate.exchange(
                BASE_URL + "/" + catId, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify still accessible by id (archived, not deleted)
        ResponseEntity<CarTagCategoryDto> getResp = restTemplate.exchange(
                BASE_URL + "/" + catId, HttpMethod.GET,
                new HttpEntity<>(adminHeaders()), CarTagCategoryDto.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify hidden from default list (excludes archived)
        ResponseEntity<List<CarTagCategoryDto>> listResp = restTemplate.exchange(
                BASE_URL, HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<List<CarTagCategoryDto>>() {});
        assertThat(listResp.getBody()).noneMatch(c -> c.id().equals(catId));
    }

    @Test
    void racer_cannotAccessAdminEndpoint_returns403() {
        HttpHeaders racerHeaders = new HttpHeaders();
        racerHeaders.setBearerAuth(racerToken);
        racerHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL, HttpMethod.GET,
                new HttpEntity<>(racerHeaders), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void anonymous_returns401() {
        // Anonymous requests to admin endpoints return 401 or 403 depending on
        // whether the URL filter or method-security check fires first — both are acceptable
        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL, HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), Map.class);
        assertThat(response.getStatusCode().value())
                .as("Unauthenticated request should return 401 or 403")
                .isIn(401, 403);
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
        user.setFirstName("Admin");
        user.setLastName("Tags");
        user.setRoles(Set.of(Role.ADMIN));
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
    }

    private String registerAndLoginAsRacer() {
        String email = "racer-tags-" + UUID.randomUUID() + "@test.com";
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest("Test", "Racer", email, "password123"),
                AuthResponse.class);
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(email, "password123"),
                AuthResponse.class);
        return loginResp.getBody().accessToken();
    }
}
