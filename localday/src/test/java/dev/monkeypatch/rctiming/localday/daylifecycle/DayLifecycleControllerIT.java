package dev.monkeypatch.rctiming.localday.daylifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.localday.auth.LocalCredential;
import dev.monkeypatch.rctiming.localday.auth.LocalCredentialRepository;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLoginResponse;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheInstanceSecretDto;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full HTTP-shaped integration test for {@link DayLifecycleController}, driven through
 * {@link MockMvc} against the real {@code LocalSecurityConfig} filter chain — mirrors
 * {@code LocalAuthControllerIT}'s convention. {@link PreCacheClient} is replaced with a
 * {@code @MockitoBean} so no real cloud call is made; the exact request/response shapes it
 * produces are covered by {@code PreCacheClientTest}, and the service orchestration is covered
 * by {@code DayLifecycleServiceIT} — this class only proves routing, status-code mapping, and
 * the pre-cache/open/status-permitAll vs close-authenticated security wiring.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DayLifecycleControllerIT {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void localdayProperties(DynamicPropertyRegistry registry) {
        registry.add("localday.datasource.embedded-postgres.data-directory",
                () -> dataDir.resolve("pg").toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocalCredentialRepository localCredentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private PreCacheClient preCacheClient;

    // --- /status is reachable without a token ---

    @Test
    void status_reachableWithoutToken_returnsDefaultNotSetUpFirstTime() throws Exception {
        mockMvc.perform(get("/api/v1/day-lifecycle/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").isNotEmpty());
    }

    // --- /pre-cache is reachable without a token, and maps a successful pull to 200 ---

    @Test
    void preCache_reachableWithoutToken_success_returns200() throws Exception {
        when(preCacheClient.login("official@club.test", "hunter2"))
                .thenReturn(new CloudLoginResponse("tok-1", "1", "official@club.test", "Race", "Director", List.of("ADMIN")));
        when(preCacheClient.preCache(eq(50L), eq("tok-1"), anyString()))
                .thenReturn(new CloudPreCacheResponse(List.of(), List.of(), List.of(), List.of(),
                        new CloudPreCacheInstanceSecretDto("ignored", "secret-50")));

        mockMvc.perform(post("/api/v1/day-lifecycle/pre-cache")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("eventId", 50);
                            put("email", "official@club.test");
                            put("password", "hunter2");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRE_CACHED"))
                .andExpect(jsonPath("$.eventId").value(50));
    }

    @Test
    void preCache_cloudUnreachable_returns503() throws Exception {
        when(preCacheClient.login(anyString(), anyString()))
                .thenThrow(new CloudUnreachableException("simulated", new java.net.ConnectException()));

        mockMvc.perform(post("/api/v1/day-lifecycle/pre-cache")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("eventId", 51);
                            put("email", "official@club.test");
                            put("password", "hunter2");
                        }})))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("cloud_unreachable"));
    }

    // --- /open is reachable without a token; offline open with nothing cached fails clearly ---

    @Test
    void open_offlineWithNothingCached_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/day-lifecycle/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("eventId", 777_777);
                            put("email", null);
                            put("password", null);
                        }})))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("offline_open_unavailable"));
    }

    // --- /close is NOT permitAll: default-deny requires a valid local session token ---

    @Test
    void close_noAuthorizationHeader_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/day-lifecycle/close"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void close_validLocalSessionToken_returns200() throws Exception {
        LocalCredential credential = new LocalCredential();
        credential.setOfficialName("Session Official");
        credential.setSecretHash(passwordEncoder.encode("9999"));
        localCredentialRepository.save(credential);

        String loginResponse = mockMvc.perform(post("/api/v1/local-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("credentialId", credential.getId());
                            put("secret", "9999");
                        }})))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("sessionToken").asText();

        // No prior pre-cache in this test, so DayLifecycleState.cloudEventId is null and the
        // service's best-effort cloud-close call is skipped entirely — nothing to stub on
        // preCacheClient here.
        mockMvc.perform(post("/api/v1/day-lifecycle/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("closed"));
    }
}
