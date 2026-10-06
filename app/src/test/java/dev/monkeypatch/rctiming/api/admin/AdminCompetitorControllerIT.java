package dev.monkeypatch.rctiming.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.domain.competitor.Competitor;
import dev.monkeypatch.rctiming.domain.competitor.CompetitorRepository;
import dev.monkeypatch.rctiming.domain.competitor.CompetitorService;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.infrastructure.tts.PiperTtsClient;
import dev.monkeypatch.rctiming.infrastructure.tts.TtsUnavailableException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** How a competitor's name is said aloud: set, cleared and heard (#119). */
class AdminCompetitorControllerIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompetitorService competitorService;
    @Autowired CompetitorRepository competitorRepository;

    @MockitoBean PiperTtsClient piperClient;

    @Test
    void anAdminSetsAndClearsASpokenName_andTheListShowsIt() {
        String token = loginAs(Set.of(Role.ADMIN));
        Competitor c = competitorService.createWalkIn("Siobhan Keane " + UUID.randomUUID());

        ResponseEntity<JsonNode> set = put(token, c.getId(), "  Shiv-awn Keen ");
        assertThat(set.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(set.getBody().get("spokenName").asText()).isEqualTo("Shiv-awn Keen");
        assertThat(competitorRepository.findById(c.getId()).orElseThrow().getSpokenName()).isEqualTo("Shiv-awn Keen");

        JsonNode listed = list(token, c.getId());
        assertThat(listed.get("spokenName").asText()).isEqualTo("Shiv-awn Keen");
        assertThat(listed.get("displayName").asText()).isEqualTo(c.getDisplayName());

        ResponseEntity<JsonNode> cleared = put(token, c.getId(), "");
        assertThat(cleared.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cleared.getBody().get("spokenName").isNull()).isTrue();
        assertThat(competitorRepository.findById(c.getId()).orElseThrow().getSpokenName()).isNull();
    }

    @Test
    void aSpokenNameOverTheLimitIsRefused() {
        String token = loginAs(Set.of(Role.ADMIN));
        Competitor c = competitorService.createWalkIn("Long Name " + UUID.randomUUID());

        ResponseEntity<JsonNode> resp = put(token, c.getId(), "x".repeat(101));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anUnknownCompetitorIsNotFound() {
        String token = loginAs(Set.of(Role.ADMIN));

        assertThat(put(token, 999_999_999L, "x").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void onlyAnAdminCanChangeASpokenName() {
        Competitor c = competitorService.createWalkIn("Ref Edit " + UUID.randomUUID());

        ResponseEntity<JsonNode> resp = put(loginAs(Set.of(Role.REFEREE)), c.getId(), "x");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(competitorRepository.findById(c.getId()).orElseThrow().getSpokenName()).isNull();
    }

    @Test
    void previewReturnsTheSpokenAudio() {
        String token = loginAs(Set.of(Role.ADMIN));
        byte[] wav = {0x52, 0x49, 0x46, 0x46};
        when(piperClient.synthesize(eq("Shiv-awn Keen"), anyString())).thenReturn(wav);

        ResponseEntity<byte[]> resp = restTemplate.exchange("/api/v1/admin/competitors/spoken-name/preview",
                HttpMethod.POST, new HttpEntity<>(Map.of("text", " Shiv-awn Keen "), headers(token)), byte[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("audio/wav"));
        assertThat(resp.getBody()).isEqualTo(wav);
        verify(piperClient).synthesize(eq("Shiv-awn Keen"), anyString());
    }

    @Test
    void previewIs503WhenThePiperVoiceIsDown() {
        String token = loginAs(Set.of(Role.ADMIN));
        when(piperClient.synthesize(anyString(), anyString())).thenThrow(new TtsUnavailableException("down"));

        ResponseEntity<JsonNode> resp = restTemplate.exchange("/api/v1/admin/competitors/spoken-name/preview",
                HttpMethod.POST, new HttpEntity<>(Map.of("text", "x"), headers(token)), JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private ResponseEntity<JsonNode> put(String token, long id, String spokenName) {
        return restTemplate.exchange("/api/v1/admin/competitors/" + id + "/spoken-name", HttpMethod.PUT,
                new HttpEntity<>(Map.of("spokenName", spokenName), headers(token)), JsonNode.class);
    }

    private JsonNode list(String token, long id) {
        JsonNode all = restTemplate.exchange("/api/v1/admin/competitors", HttpMethod.GET,
                new HttpEntity<>(headers(token)), JsonNode.class).getBody();
        for (JsonNode n : all) {
            if (n.get("id").asLong() == id) return n;
        }
        throw new AssertionError("competitor " + id + " not listed");
    }

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private String loginAs(Set<Role> roles) {
        String email = "spoken-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("pass12345"));
        user.setFirstName("Staff");
        user.setLastName("User");
        user.setRoles(roles);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        var login = restTemplate.postForEntity("/api/v1/auth/login", new LoginRequest(email, "pass12345"), AuthResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        return login.getBody().accessToken();
    }
}
