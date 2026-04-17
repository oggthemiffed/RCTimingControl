package dev.monkeypatch.rctiming.api.racer;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.api.auth.RegisterRequest;
import dev.monkeypatch.rctiming.api.racer.dto.CarDto;
import dev.monkeypatch.rctiming.query.car.CarWithTagsDto;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CarControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    private String racerToken;

    @BeforeEach
    void setUp() {
        racerToken = registerAndLoginAsRacer();
    }

    @Test
    void createCar_returns201_andCreatedRow() {
        ResponseEntity<CarDto> response = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Test Car", "notes", "My first car"), racerHeaders()),
                CarDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Test Car");
        assertThat(response.getBody().archived()).isFalse();
    }

    @Test
    void listCars_excludesArchived() {
        // Create a car
        ResponseEntity<CarDto> created = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Active Car"), racerHeaders()),
                CarDto.class);
        Long carId = created.getBody().id();

        // Archive it
        restTemplate.exchange("/api/v1/racer/cars/" + carId, HttpMethod.DELETE,
                new HttpEntity<>(racerHeaders()), Void.class);

        // List should not contain the archived car
        ResponseEntity<List<CarWithTagsDto>> listResp = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.GET,
                new HttpEntity<>(racerHeaders()),
                new ParameterizedTypeReference<List<CarWithTagsDto>>() {});

        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Long> ids = listResp.getBody().stream().map(CarWithTagsDto::id).toList();
        assertThat(ids).doesNotContain(carId);
    }

    @Test
    void updateCar_appliesPartialFields() {
        // Create a car
        ResponseEntity<CarDto> created = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Original Name", "notes", "Original notes"), racerHeaders()),
                CarDto.class);
        Long carId = created.getBody().id();

        // PATCH only name
        ResponseEntity<CarDto> updated = restTemplate.exchange(
                "/api/v1/racer/cars/" + carId, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("name", "Updated Name"), racerHeaders()),
                CarDto.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().name()).isEqualTo("Updated Name");
        // notes should remain unchanged
        assertThat(updated.getBody().notes()).isEqualTo("Original notes");
    }

    @Test
    void archiveCar_returns204_andRemovesFromList() {
        // Create a car
        ResponseEntity<CarDto> created = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "To Archive"), racerHeaders()),
                CarDto.class);
        Long carId = created.getBody().id();

        // Archive it
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                "/api/v1/racer/cars/" + carId, HttpMethod.DELETE,
                new HttpEntity<>(racerHeaders()), Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify it's gone from list
        ResponseEntity<List<CarWithTagsDto>> listResp = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.GET,
                new HttpEntity<>(racerHeaders()),
                new ParameterizedTypeReference<List<CarWithTagsDto>>() {});
        List<Long> ids = listResp.getBody().stream().map(CarWithTagsDto::id).toList();
        assertThat(ids).doesNotContain(carId);
    }

    @Test
    void cannotAccessAnotherRacersCar_returns404() {
        // Racer A creates a car
        ResponseEntity<CarDto> created = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Racer A Car"), racerHeaders()),
                CarDto.class);
        Long racerACarId = created.getBody().id();

        // Racer B registers and logs in
        String racerBToken = registerAndLoginAsRacer();

        // Racer B tries to GET racer A's car — must return 404
        HttpHeaders racerBHeaders = new HttpHeaders();
        racerBHeaders.setBearerAuth(racerBToken);
        racerBHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> getResp = restTemplate.exchange(
                "/api/v1/racer/cars/" + racerACarId, HttpMethod.GET,
                new HttpEntity<>(racerBHeaders), Map.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void setTag_persistsValueOnCar() {
        // Create a car
        ResponseEntity<CarDto> created = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Tag Test Car"), racerHeaders()),
                CarDto.class);
        Long carId = created.getBody().id();

        // Set tag: categoryId=1 is "Chassis" (seeded by V10)
        ResponseEntity<Void> tagResp = restTemplate.exchange(
                "/api/v1/racer/cars/" + carId + "/tags", HttpMethod.POST,
                new HttpEntity<>(Map.of("categoryId", 1, "value", "Team Associated B6.4"), racerHeaders()),
                Void.class);
        assertThat(tagResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // GET /racer/cars should show the tag in the tags map
        ResponseEntity<List<CarWithTagsDto>> listResp = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.GET,
                new HttpEntity<>(racerHeaders()),
                new ParameterizedTypeReference<List<CarWithTagsDto>>() {});

        CarWithTagsDto car = listResp.getBody().stream()
                .filter(c -> c.id().equals(carId))
                .findFirst()
                .orElseThrow();
        assertThat(car.tags()).containsEntry("Chassis", "Team Associated B6.4");
    }

    @Test
    void setTag_overwritesExistingValue() {
        // Create a car
        ResponseEntity<CarDto> created = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Overwrite Tag Car"), racerHeaders()),
                CarDto.class);
        Long carId = created.getBody().id();

        // Set tag twice with different values
        restTemplate.exchange("/api/v1/racer/cars/" + carId + "/tags", HttpMethod.POST,
                new HttpEntity<>(Map.of("categoryId", 1, "value", "First Value"), racerHeaders()),
                Void.class);
        restTemplate.exchange("/api/v1/racer/cars/" + carId + "/tags", HttpMethod.POST,
                new HttpEntity<>(Map.of("categoryId", 1, "value", "Second Value"), racerHeaders()),
                Void.class);

        // GET should show second value
        ResponseEntity<List<CarWithTagsDto>> listResp = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.GET,
                new HttpEntity<>(racerHeaders()),
                new ParameterizedTypeReference<List<CarWithTagsDto>>() {});

        CarWithTagsDto car = listResp.getBody().stream()
                .filter(c -> c.id().equals(carId))
                .findFirst()
                .orElseThrow();
        assertThat(car.tags()).containsEntry("Chassis", "Second Value");
    }

    @Test
    void deleteTag_removesValue() {
        // Create a car and set a tag
        ResponseEntity<CarDto> created = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Delete Tag Car"), racerHeaders()),
                CarDto.class);
        Long carId = created.getBody().id();

        restTemplate.exchange("/api/v1/racer/cars/" + carId + "/tags", HttpMethod.POST,
                new HttpEntity<>(Map.of("categoryId", 1, "value", "To Delete"), racerHeaders()),
                Void.class);

        // Delete the tag
        ResponseEntity<Void> deleteTagResp = restTemplate.exchange(
                "/api/v1/racer/cars/" + carId + "/tags/1", HttpMethod.DELETE,
                new HttpEntity<>(racerHeaders()), Void.class);
        assertThat(deleteTagResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify tag is gone
        ResponseEntity<List<CarWithTagsDto>> listResp = restTemplate.exchange(
                "/api/v1/racer/cars", HttpMethod.GET,
                new HttpEntity<>(racerHeaders()),
                new ParameterizedTypeReference<List<CarWithTagsDto>>() {});

        CarWithTagsDto car = listResp.getBody().stream()
                .filter(c -> c.id().equals(carId))
                .findFirst()
                .orElseThrow();
        assertThat(car.tags()).doesNotContainKey("Chassis");
    }

    // --- helpers ---

    private String registerAndLoginAsRacer() {
        String email = "racer-car-" + UUID.randomUUID() + "@test.com";
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest("Test", "Racer", email, "password123"),
                AuthResponse.class);
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(email, "password123"),
                AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginResp.getBody().accessToken();
    }

    private HttpHeaders racerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(racerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
