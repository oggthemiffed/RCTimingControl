package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.admin.dto.CarTagCategoryDto;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.domain.car.CarTagCategoryRepository;
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

class CarTagCategoryArchiveIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/admin/car-tag-categories";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CarTagCategoryRepository carTagCategoryRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        String email = "admin-archive-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("adminPass123"));
        user.setFirstName("Admin");
        user.setLastName("Archive");
        user.setRoles(Set.of(Role.ADMIN));
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(email, "adminPass123"),
                AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        adminToken = loginResp.getBody().accessToken();
    }

    @Test
    void deleteCategory_nowArchivesInsteadOfDeleting() {
        // Create a category
        String catName = "ArchiveTest-" + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<CarTagCategoryDto> created = restTemplate.exchange(
                BASE_URL, HttpMethod.POST,
                new HttpEntity<>(Map.of("name", catName, "sortOrder", 50), adminHeaders()),
                CarTagCategoryDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long catId = created.getBody().id();

        // DELETE archives instead of hard-deleting
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                BASE_URL + "/" + catId, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders()), Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Default list (excludes archived) should NOT contain the category
        ResponseEntity<List<CarTagCategoryDto>> defaultList = restTemplate.exchange(
                BASE_URL, HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<List<CarTagCategoryDto>>() {});
        assertThat(defaultList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(defaultList.getBody()).noneMatch(c -> c.id().equals(catId));

        // ?includeArchived=true should contain it
        ResponseEntity<List<CarTagCategoryDto>> allList = restTemplate.exchange(
                BASE_URL + "?includeArchived=true", HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<List<CarTagCategoryDto>>() {});
        assertThat(allList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allList.getBody()).anyMatch(c -> c.id().equals(catId));

        // Row still exists in DB (not hard-deleted)
        assertThat(carTagCategoryRepository.findById(catId)).isPresent();
        assertThat(carTagCategoryRepository.findById(catId).get().isArchived()).isTrue();
    }

    @Test
    void listCategories_defaultExcludesArchived() {
        // Create two categories
        String name1 = "ListTest-A-" + UUID.randomUUID().toString().substring(0, 6);
        String name2 = "ListTest-B-" + UUID.randomUUID().toString().substring(0, 6);

        ResponseEntity<CarTagCategoryDto> cat1 = restTemplate.exchange(
                BASE_URL, HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name1, "sortOrder", 60), adminHeaders()),
                CarTagCategoryDto.class);
        ResponseEntity<CarTagCategoryDto> cat2 = restTemplate.exchange(
                BASE_URL, HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name2, "sortOrder", 61), adminHeaders()),
                CarTagCategoryDto.class);

        Long id1 = cat1.getBody().id();
        Long id2 = cat2.getBody().id();

        // Archive cat1
        restTemplate.exchange(BASE_URL + "/" + id1, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders()), Void.class);

        // Default list excludes archived cat1
        ResponseEntity<List<CarTagCategoryDto>> defaultList = restTemplate.exchange(
                BASE_URL, HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<List<CarTagCategoryDto>>() {});
        List<Long> defaultIds = defaultList.getBody().stream().map(CarTagCategoryDto::id).toList();
        assertThat(defaultIds).doesNotContain(id1);
        assertThat(defaultIds).contains(id2);

        // ?includeArchived=true shows both
        ResponseEntity<List<CarTagCategoryDto>> allList = restTemplate.exchange(
                BASE_URL + "?includeArchived=true", HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<List<CarTagCategoryDto>>() {});
        List<Long> allIds = allList.getBody().stream().map(CarTagCategoryDto::id).toList();
        assertThat(allIds).contains(id1, id2);
    }

    @Test
    void unarchiveCategory_restoresToDefaultList() {
        // Create and archive a category
        String catName = "UnarchiveTest-" + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<CarTagCategoryDto> created = restTemplate.exchange(
                BASE_URL, HttpMethod.POST,
                new HttpEntity<>(Map.of("name", catName, "sortOrder", 70), adminHeaders()),
                CarTagCategoryDto.class);
        Long catId = created.getBody().id();

        // Archive it
        restTemplate.exchange(BASE_URL + "/" + catId, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders()), Void.class);

        // Confirm it is hidden from default list
        ResponseEntity<List<CarTagCategoryDto>> beforeUnarchive = restTemplate.exchange(
                BASE_URL, HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<List<CarTagCategoryDto>>() {});
        assertThat(beforeUnarchive.getBody()).noneMatch(c -> c.id().equals(catId));

        // Unarchive
        ResponseEntity<Void> unarchiveResp = restTemplate.exchange(
                BASE_URL + "/" + catId + "/unarchive", HttpMethod.POST,
                new HttpEntity<>(adminHeaders()), Void.class);
        assertThat(unarchiveResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Now visible in default list again
        ResponseEntity<List<CarTagCategoryDto>> afterUnarchive = restTemplate.exchange(
                BASE_URL, HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<List<CarTagCategoryDto>>() {});
        assertThat(afterUnarchive.getBody()).anyMatch(c -> c.id().equals(catId));
    }

    @Test
    void archiveCategory_notFound_returns404() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                BASE_URL + "/9999999", HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders()), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- helpers ---

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
