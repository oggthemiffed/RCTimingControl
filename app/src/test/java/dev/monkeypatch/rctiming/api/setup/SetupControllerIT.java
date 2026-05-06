package dev.monkeypatch.rctiming.api.setup;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.setup.dto.BootstrapRequest;
import dev.monkeypatch.rctiming.api.setup.dto.SetupProgressDto;
import dev.monkeypatch.rctiming.api.setup.dto.SetupStatusDto;
import dev.monkeypatch.rctiming.domain.club.ClubProfile;
import dev.monkeypatch.rctiming.domain.club.ClubProfileRepository;
import dev.monkeypatch.rctiming.domain.track.Track;
import dev.monkeypatch.rctiming.domain.track.TrackRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SetupControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ClubProfileRepository clubProfileRepository;

    @Autowired
    TrackRepository trackRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        trackRepository.deleteAll();
        clubProfileRepository.deleteAll();
        jdbcTemplate.execute("TRUNCATE users CASCADE");
    }

    @Test
    void getStatus_returnsSetupComplete_false_whenNoClub() {
        ResponseEntity<SetupStatusDto> response = restTemplate.getForEntity("/api/v1/setup/status", SetupStatusDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().setupComplete()).isFalse();
    }

    @Test
    void getStatus_returnsSetupComplete_true_afterClubSaved() {
        clubProfileRepository.save(minimalClub());
        ResponseEntity<SetupStatusDto> response = restTemplate.getForEntity("/api/v1/setup/status", SetupStatusDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().setupComplete()).isTrue();
    }

    @Test
    void bootstrap_createsAdminUserAndReturnsToken() {
        BootstrapRequest req = new BootstrapRequest("Admin", "User", "admin@test.com", "password123");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/api/v1/setup/bootstrap", req, AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().roles()).contains("ADMIN");
        assertThat(response.getBody().roles()).doesNotContain("RACER");
    }

    @Test
    void bootstrap_returns409_whenUsersExist() {
        BootstrapRequest req = new BootstrapRequest("Admin", "User", "admin@test.com", "password123");
        restTemplate.postForEntity("/api/v1/setup/bootstrap", req, AuthResponse.class);
        ResponseEntity<Void> second = restTemplate.postForEntity("/api/v1/setup/bootstrap", req, Void.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void getProgress_reflectsDataState() {
        // Bootstrap creates an admin user (counts toward staff check)
        BootstrapRequest req = new BootstrapRequest("Admin", "User", "admin@test.com", "password123");
        ResponseEntity<AuthResponse> bootstrapResp = restTemplate.postForEntity("/api/v1/setup/bootstrap", req, AuthResponse.class);
        assertThat(bootstrapResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String jwt = bootstrapResp.getBody().accessToken();

        // After bootstrap: staff=true (admin role user exists), others false
        SetupProgressDto progress1 = getProgress(jwt);
        assertThat(progress1.club()).isFalse();
        assertThat(progress1.track()).isFalse();
        assertThat(progress1.format()).isFalse();
        assertThat(progress1.staff()).isTrue();
        assertThat(progress1.decoder()).isFalse();

        // Add club profile: club=true, decoder still false (no host/port/protocol)
        clubProfileRepository.save(minimalClub());
        SetupProgressDto progress2 = getProgress(jwt);
        assertThat(progress2.club()).isTrue();
        assertThat(progress2.decoder()).isFalse();

        // Add track: track=true
        Track track = new Track();
        track.setName("Test Track");
        track.setCreatedAt(Instant.now());
        track.setUpdatedAt(Instant.now());
        trackRepository.save(track);
        SetupProgressDto progress3 = getProgress(jwt);
        assertThat(progress3.track()).isTrue();
    }

    @Test
    void downloadForwarderConfig_returnsEnvAttachment() {
        // Bootstrap an admin user to get a JWT
        BootstrapRequest req = new BootstrapRequest("Admin", "User", "admin@test.com", "password123");
        ResponseEntity<AuthResponse> bootstrapResp = restTemplate.postForEntity("/api/v1/setup/bootstrap", req, AuthResponse.class);
        assertThat(bootstrapResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String jwt = bootstrapResp.getBody().accessToken();

        // GET /api/v1/setup/forwarder-config-download with admin JWT
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/api/v1/setup/forwarder-config-download",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment")
                .contains("forwarder.env");

        String body = new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(body).contains("APP_SERVER_URL=");
        assertThat(body).contains("APP_DECODER_HOST=");
    }

    @Test
    void downloadForwarderConfig_includesTokenPlaceholder_notPlaintext() {
        // T-08-03: forwarder.env must contain placeholder, never a real token value
        BootstrapRequest req = new BootstrapRequest("Admin", "User", "admin@test.com", "password123");
        ResponseEntity<AuthResponse> bootstrapResp = restTemplate.postForEntity("/api/v1/setup/bootstrap", req, AuthResponse.class);
        assertThat(bootstrapResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String jwt = bootstrapResp.getBody().accessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/api/v1/setup/forwarder-config-download",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8);

        // T-08-03: The APP_FORWARDER_TOKEN line must contain the literal placeholder verbatim
        assertThat(body).contains("APP_FORWARDER_TOKEN=<paste-your-token-here>");
    }

    // --- helpers ---

    private SetupProgressDto getProgress(String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<SetupProgressDto> resp = restTemplate.exchange(
                "/api/v1/setup/progress",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                SetupProgressDto.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    private ClubProfile minimalClub() {
        ClubProfile profile = new ClubProfile();
        profile.setName("Test Club");
        profile.setTimezone("Europe/London");
        Instant now = Instant.now();
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        return profile;
    }
}
