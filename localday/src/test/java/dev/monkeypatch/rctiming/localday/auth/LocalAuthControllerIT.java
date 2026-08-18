package dev.monkeypatch.rctiming.localday.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full HTTP-shaped integration test: {@link LocalAuthController} + {@code LocalSecurityConfig}'s
 * real security filter chain, driven through {@link MockMvc} against the real embedded-Postgres
 * context. Mirrors {@code MarshalAdjustmentRepositoryIT}'s established
 * {@code @SpringBootTest} + {@code @DynamicPropertySource} + {@code @TempDir} +
 * {@code @DirtiesContext(AFTER_CLASS)} convention for this module's real-database integration
 * tests.
 *
 * <p>Covers the fixed API contract's exact status codes and JSON shapes (200/401/423) end to
 * end, plus AE4 (protected-endpoint rejection): since no protected controller exists yet in
 * {@code :localday}, a trivial protected test-only endpoint is registered via a nested
 * {@link TestConfiguration} purely to prove the security filter chain's default-deny +
 * {@link LocalSessionAuthenticationFilter} wiring works for endpoints outside
 * {@code /api/v1/local-auth/**}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocalAuthControllerIT {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void localdayProperties(DynamicPropertyRegistry registry) {
        registry.add("localday.datasource.embedded-postgres.data-directory",
                () -> dataDir.resolve("pg").toString());
    }

    @TestConfiguration
    static class ProtectedTestEndpointConfig {
        @Bean
        ProtectedTestController protectedTestController() {
            return new ProtectedTestController();
        }
    }

    @RestController
    static class ProtectedTestController {
        @GetMapping("/api/v1/localday-test/protected")
        String protectedEndpoint() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalCredentialRepository credentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createCredential(String name, String secret, boolean recovery) {
        LocalCredential c = new LocalCredential();
        c.setOfficialName(name);
        c.setSecretHash(passwordEncoder.encode(secret));
        c.setRecovery(recovery);
        return credentialRepository.save(c).getId();
    }

    // --- Public endpoints reachable without a token ---

    @Test
    void officialsEndpoint_reachableWithoutToken() throws Exception {
        createCredential("Public List Official", "1111", false);

        mockMvc.perform(get("/api/v1/local-auth/officials"))
                .andExpect(status().isOk());
    }

    // --- Login round trip: 200 / 401 / 423 ---

    @Test
    void login_correctSecret_returns200WithSessionTokenAndOfficialName() throws Exception {
        Long id = createCredential("Jane Doe", "1234", false);

        mockMvc.perform(post("/api/v1/local-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                            put("credentialId", id);
                            put("secret", "1234");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionToken").isNotEmpty())
                .andExpect(jsonPath("$.officialName").value("Jane Doe"))
                .andExpect(jsonPath("$.credentialId").value(id));
    }

    @Test
    void login_wrongSecret_returns401WithInvalidCredentialError() throws Exception {
        Long id = createCredential("Wrong Secret Official", "1234", false);

        mockMvc.perform(post("/api/v1/local-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                            put("credentialId", id);
                            put("secret", "0000");
                        }})))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credential"));
    }

    @Test
    void login_lockedCredential_returns423WithRetryAfterSeconds() throws Exception {
        Long id = createCredential("Lockout Official", "1234", false);

        String badBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("credentialId", id);
            put("secret", "wrong");
        }});

        // Three consecutive wrong attempts crosses LOCK_THRESHOLD.
        mockMvc.perform(post("/api/v1/local-auth/login").contentType(MediaType.APPLICATION_JSON).content(badBody))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/local-auth/login").contentType(MediaType.APPLICATION_JSON).content(badBody))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/local-auth/login").contentType(MediaType.APPLICATION_JSON).content(badBody))
                .andExpect(status().isUnauthorized());

        // The next attempt — even with the CORRECT secret — must see 423 Locked, not 200/401.
        String correctBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("credentialId", id);
            put("secret", "1234");
        }});
        mockMvc.perform(post("/api/v1/local-auth/login").contentType(MediaType.APPLICATION_JSON).content(correctBody))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error").value("locked"))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());
    }

    // --- Recovery ---

    @Test
    void recover_validRecoveryCredential_returns200AndUnlocksTarget() throws Exception {
        Long recoveryId = createCredential("Recovery Official", "5678", true);
        Long targetId = createCredential("Locked Target Official", "1234", false);

        String badBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("credentialId", targetId);
            put("secret", "wrong");
        }});
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/local-auth/login").contentType(MediaType.APPLICATION_JSON).content(badBody));
        }

        String recoverBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("recoveryCredentialId", recoveryId);
            put("recoverySecret", "5678");
            put("targetCredentialId", targetId);
        }});
        mockMvc.perform(post("/api/v1/local-auth/recover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recoverBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlocked").value(true));

        // Target should now be able to log in again with the correct secret.
        String correctBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("credentialId", targetId);
            put("secret", "1234");
        }});
        mockMvc.perform(post("/api/v1/local-auth/login").contentType(MediaType.APPLICATION_JSON).content(correctBody))
                .andExpect(status().isOk());
    }

    @Test
    void recover_invalidRecoverySecret_returns401() throws Exception {
        Long recoveryId = createCredential("Recovery Official Two", "5678", true);
        Long targetId = createCredential("Target Official Two", "1234", false);

        String recoverBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("recoveryCredentialId", recoveryId);
            put("recoverySecret", "wrongsecret");
            put("targetCredentialId", targetId);
        }});
        mockMvc.perform(post("/api/v1/local-auth/recover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recoverBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_recovery_credential"));
    }

    // --- AE4: protected endpoint rejection / public endpoint exemption ---

    @Test
    void protectedEndpoint_noAuthorizationHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/localday-test/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/localday-test/protected")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_validSessionToken_returns200() throws Exception {
        Long id = createCredential("Protected Access Official", "9999", false);

        String loginBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("credentialId", id);
            put("secret", "9999");
        }});
        String response = mockMvc.perform(post("/api/v1/local-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("sessionToken").asText();

        mockMvc.perform(get("/api/v1/localday-test/protected")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
