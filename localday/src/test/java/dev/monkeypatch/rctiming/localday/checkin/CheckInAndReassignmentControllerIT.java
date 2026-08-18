package dev.monkeypatch.rctiming.localday.checkin;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.localday.auth.LocalCredential;
import dev.monkeypatch.rctiming.localday.auth.LocalCredentialRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full HTTP-shaped integration test for U7's check-in and transponder-reassignment endpoints:
 * {@link CheckInController} + {@link TransponderReassignmentController} +
 * {@code LocalSecurityConfig}'s real security filter chain, driven through {@link MockMvc}
 * against the real embedded-Postgres context. Mirrors {@code LocalAuthControllerIT}'s established
 * {@code @SpringBootTest} + {@code @DynamicPropertySource} + {@code @TempDir} +
 * {@code @DirtiesContext(AFTER_CLASS)} convention for this module's real-database integration
 * tests.
 *
 * <p>Covers AE1 (transponder reassignment, full authenticated HTTP round trip, real DB
 * persistence, zero cloud dependency — naturally satisfied since this module has none) and the
 * AE4-adjacent no-auth-rejection case for these newly-added write endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CheckInAndReassignmentControllerIT {

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
    private LocalCredentialRepository credentialRepository;

    @Autowired
    private CachedEntryRepository cachedEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createCredential(String name, String secret) {
        LocalCredential c = new LocalCredential();
        c.setOfficialName(name);
        c.setSecretHash(passwordEncoder.encode(secret));
        c.setRecovery(false);
        return credentialRepository.save(c).getId();
    }

    private String login(Long credentialId, String secret) throws Exception {
        String body = objectMapper.writeValueAsString(new HashMap<>() {{
            put("credentialId", credentialId);
            put("secret", secret);
        }});
        String response = mockMvc.perform(post("/api/v1/local-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("sessionToken").asText();
    }

    private Long seedEntry(Long cloudEntryId, String transponderNumber, String racerName) {
        CachedEntry entry = new CachedEntry();
        entry.setCloudEntryId(cloudEntryId);
        entry.setTransponderNumber(transponderNumber);
        entry.setRacerName(racerName);
        entry.setCarName("TC-01");
        entry.setClassName("Touring Stock");
        return cachedEntryRepository.save(entry).getId();
    }

    // --- AE1: authenticated round trip through /api/v1/transponders/reassign ---

    @Test
    void reassign_authenticatedRequest_returns200_andDurablyPersistsChange() throws Exception {
        Long credentialId = createCredential("Reassign Test Official", "2468");
        String token = login(credentialId, "2468");

        Long entryId = seedEntry(101L, "1111111", "Alice Racer");

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("cachedEntryId", entryId);
            put("newTransponderNumber", "9999999");
        }});

        mockMvc.perform(post("/api/v1/transponders/reassign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cachedEntryId").value(entryId))
                .andExpect(jsonPath("$.oldTransponderNumber").value("1111111"))
                .andExpect(jsonPath("$.newTransponderNumber").value("9999999"));

        Optional<CachedEntry> persisted = cachedEntryRepository.findById(entryId);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getTransponderNumber()).isEqualTo("9999999");
    }

    @Test
    void reassign_conflictingTransponderNumber_returns409() throws Exception {
        Long credentialId = createCredential("Conflict Test Official", "1357");
        String token = login(credentialId, "1357");

        Long entryOneId = seedEntry(201L, "2222222", "Bob Racer");
        seedEntry(202L, "3333333", "Carol Racer");

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("cachedEntryId", entryOneId);
            put("newTransponderNumber", "3333333");
        }});

        mockMvc.perform(post("/api/v1/transponders/reassign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("transponder_already_assigned"));
    }

    // --- Check-in resolve/search/confirm, authenticated round trip ---

    @Test
    void resolveSearchAndConfirm_authenticatedRequests_workEndToEnd() throws Exception {
        Long credentialId = createCredential("CheckIn Test Official", "8642");
        String token = login(credentialId, "8642");

        Long entryId = seedEntry(301L, "4444444", "Dana Racer");

        String resolveBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("transponderNumber", "4444444");
        }});
        mockMvc.perform(post("/api/v1/checkin/resolve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cachedEntryId").value(entryId))
                .andExpect(jsonPath("$.checkedIn").value(false));

        mockMvc.perform(get("/api/v1/checkin/search")
                        .header("Authorization", "Bearer " + token)
                        .param("query", "dana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cachedEntryId").value(entryId));

        mockMvc.perform(post("/api/v1/checkin/" + entryId + "/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedIn").value(true))
                .andExpect(jsonPath("$.alreadyCheckedIn").value(false));

        // Second confirm reports alreadyCheckedIn distinctly.
        mockMvc.perform(post("/api/v1/checkin/" + entryId + "/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedIn").value(true))
                .andExpect(jsonPath("$.alreadyCheckedIn").value(true));
    }

    // --- AE4-adjacent: no-auth rejection for these new write endpoints ---

    @Test
    void reassign_noAuthorizationHeader_returns401() throws Exception {
        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("cachedEntryId", 1L);
            put("newTransponderNumber", "0000000");
        }});

        mockMvc.perform(post("/api/v1/transponders/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkInConfirm_noAuthorizationHeader_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/checkin/1/confirm"))
                .andExpect(status().isUnauthorized());
    }
}
