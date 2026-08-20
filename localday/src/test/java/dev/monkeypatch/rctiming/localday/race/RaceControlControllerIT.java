package dev.monkeypatch.rctiming.localday.race;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.localday.auth.LocalCredential;
import dev.monkeypatch.rctiming.localday.auth.LocalCredentialRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full HTTP-shaped integration test for U8's {@link RaceControlController}, driven through
 * {@link MockMvc} against the real embedded-Postgres context with the real security filter chain
 * wired in. Mirrors {@code CheckInAndReassignmentControllerIT}'s established
 * {@code @SpringBootTest} + {@code @DynamicPropertySource} + {@code @TempDir} +
 * {@code @DirtiesContext(AFTER_CLASS)} convention for this module's real-database integration
 * tests, including its {@code createCredential}/{@code login} helpers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RaceControlControllerIT {

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
    private CachedScheduleEntryRepository cachedScheduleEntryRepository;

    @Autowired
    private CachedRaceEntryRepository cachedRaceEntryRepository;

    @Autowired
    private CachedEntryRepository cachedEntryRepository;

    @Autowired
    private MarshalAdjustmentRepository marshalAdjustmentRepository;

    @Autowired
    private RaceResultEntryRepository raceResultEntryRepository;

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

    private Long seedSchedule(Long cloudRaceId, int roundNumber, int heatNumber, int sequence, String className) {
        CachedScheduleEntry race = new CachedScheduleEntry();
        race.setCloudRaceId(cloudRaceId);
        race.setRoundNumber(roundNumber);
        race.setHeatNumber(heatNumber);
        race.setSequence(sequence);
        race.setClassName(className);
        return cachedScheduleEntryRepository.save(race).getId();
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

    private Long seedGridEntry(Long scheduleId, Long cachedEntryId, Integer gridPosition, Integer carNumber) {
        CachedRaceEntry re = new CachedRaceEntry();
        re.setCachedScheduleId(scheduleId);
        re.setCachedEntryId(cachedEntryId);
        re.setGridPosition(gridPosition);
        re.setCarNumber(carNumber);
        return cachedRaceEntryRepository.save(re).getId();
    }

    // --- GET /races ---

    @Test
    void listRaces_returnsSeededRowsInSequenceOrder() throws Exception {
        Long credentialId = createCredential("List Test Official", "1111");
        String token = login(credentialId, "1111");

        // High, test-unique sequence numbers so this assertion is immune to schedule rows other
        // tests in this shared-context class may have already seeded.
        Long buggyId = seedSchedule(10L, 1, 1, 9002, "Buggy");
        Long truckId = seedSchedule(11L, 1, 2, 9001, "Truck");

        String response = mockMvc.perform(get("/api/v1/race-control/races")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Long> orderedIds = new java.util.ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode node : objectMapper.readTree(response)) {
            long id = node.get("id").asLong();
            if (id == buggyId || id == truckId) {
                orderedIds.add(id);
            }
        }
        assertThat(orderedIds).containsExactly(truckId, buggyId);
    }

    // --- GET /races/{id} ---

    @Test
    void raceDetail_resolvesGridEntryRacerNames() throws Exception {
        Long credentialId = createCredential("Detail Test Official", "2222");
        String token = login(credentialId, "2222");

        Long scheduleId = seedSchedule(20L, 1, 1, 1, "Stadium Truck");
        Long entryId = seedEntry(401L, "5555555", "Eve Racer");
        seedGridEntry(scheduleId, entryId, 1, 7);

        mockMvc.perform(get("/api/v1/race-control/races/" + scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.race.id").value(scheduleId))
                .andExpect(jsonPath("$.grid[0].racerName").value("Eve Racer"))
                .andExpect(jsonPath("$.grid[0].transponderNumber").value("5555555"))
                .andExpect(jsonPath("$.grid[0].gridPosition").value(1));
    }

    @Test
    void raceDetail_unknownId_returns404() throws Exception {
        Long credentialId = createCredential("NotFound Test Official", "3333");
        String token = login(credentialId, "3333");

        mockMvc.perform(get("/api/v1/race-control/races/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("race_not_found"));
    }

    // --- GET /races/{id}/live ---

    @Test
    void live_noLiveStateYet_returns200WithEmptyRows() throws Exception {
        Long credentialId = createCredential("Live Test Official", "4444");
        String token = login(credentialId, "4444");

        Long scheduleId = seedSchedule(30L, 1, 1, 1, "Buggy");

        mockMvc.perform(get("/api/v1/race-control/races/" + scheduleId + "/live")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.rows").isEmpty());
    }

    @Test
    void live_unknownRaceId_returns404() throws Exception {
        Long credentialId = createCredential("Live 404 Official", "4445");
        String token = login(credentialId, "4445");

        mockMvc.perform(get("/api/v1/race-control/races/999999/live")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("race_not_found"));
    }

    // --- POST /races/{id}/transition ---

    @Test
    void transition_pendingToGrid_succeedsAndPersists() throws Exception {
        Long credentialId = createCredential("Transition Test Official", "5555");
        String token = login(credentialId, "5555");

        Long scheduleId = seedSchedule(40L, 1, 1, 1, "Buggy");

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("target", "GRID");
        }});

        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/transition")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRID"));

        // Regression check: transition() only mutates in-memory state — the controller must
        // explicitly save it. A follow-up GET on a fresh read confirms it was actually persisted.
        mockMvc.perform(get("/api/v1/race-control/races/" + scheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.race.status").value("GRID"));

        Optional<CachedScheduleEntry> persisted = cachedScheduleEntryRepository.findById(scheduleId);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getStatus()).isEqualTo(RaceState.GRID);
    }

    @Test
    void transition_illegalTransition_returns409() throws Exception {
        Long credentialId = createCredential("Illegal Transition Official", "6666");
        String token = login(credentialId, "6666");

        Long scheduleId = seedSchedule(41L, 1, 1, 1, "Buggy");

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("target", "RUNNING");
        }});

        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/transition")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("illegal_transition"));
    }

    @Test
    void transition_missingTargetField_returns400NotServerError() throws Exception {
        Long credentialId = createCredential("Null Target Official", "6667");
        String token = login(credentialId, "6667");

        Long scheduleId = seedSchedule(42L, 1, 1, 1, "Buggy");

        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/transition")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_target_state"));
    }

    @Test
    void transition_toRunning_setsStartedAt() throws Exception {
        Long credentialId = createCredential("StartedAt Test Official", "5556");
        String token = login(credentialId, "5556");

        Long scheduleId = seedSchedule(43L, 1, 1, 1, "Buggy");

        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/transition")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("target", "GRID");
                        }})))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/transition")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("target", "RUNNING");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        Optional<CachedScheduleEntry> persisted = cachedScheduleEntryRepository.findById(scheduleId);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getStartedAt()).isNotNull();
    }

    @Test
    void transition_toFinishedWithNoLiveState_doesNotThrowAndPersistsNoResultRows() throws Exception {
        Long credentialId = createCredential("Empty Finish Official", "5557");
        String token = login(credentialId, "5557");

        Long scheduleId = seedSchedule(44L, 1, 1, 1, "Buggy");

        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/transition")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("target", "GRID");
                        }})))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/transition")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("target", "RUNNING");
                        }})))
                .andExpect(status().isOk());

        // No lap events were ever ingested for this race — LapTimingService.peek() will be empty.
        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/transition")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("target", "FINISHED");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        assertThat(raceResultEntryRepository.findByRaceIdOrderByPositionAsc(scheduleId)).isEmpty();
    }

    // --- POST /races/{id}/marshal-adjustment ---

    @Test
    void marshalAdjustment_succeedsAndPersistsAuditRow() throws Exception {
        Long credentialId = createCredential("Marshal Test Official", "7777");
        String token = login(credentialId, "7777");

        Long scheduleId = seedSchedule(50L, 1, 1, 1, "Buggy");
        Long entryId = seedEntry(501L, "6666666", "Frank Racer");
        seedGridEntry(scheduleId, entryId, 1, 1);

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("cachedEntryId", entryId);
            put("lapDelta", 1);
        }});

        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/marshal-adjustment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raceId").value(scheduleId))
                .andExpect(jsonPath("$.entryId").value(entryId))
                .andExpect(jsonPath("$.transponderNumber").value("6666666"))
                .andExpect(jsonPath("$.lapDelta").value(1))
                .andExpect(jsonPath("$.actingUserName").value("Marshal Test Official"));

        List<MarshalAdjustment> adjustments = marshalAdjustmentRepository.findAllByRaceIdOrderByAdjustedAtAsc(scheduleId);
        assertThat(adjustments).hasSize(1);
        assertThat(adjustments.get(0).getEntryId()).isEqualTo(entryId);
        assertThat(adjustments.get(0).getLapDelta()).isEqualTo(1);
    }

    @Test
    void marshalAdjustment_entryNotInThisRacesGrid_returns404() throws Exception {
        Long credentialId = createCredential("Marshal WrongRace Official", "7778");
        String token = login(credentialId, "7778");

        Long scheduleId = seedSchedule(51L, 1, 1, 1, "Buggy");
        // Entry exists, but was never seeded into scheduleId's grid — e.g. it only raced elsewhere.
        Long entryId = seedEntry(502L, "6666667", "Gina Racer");

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("cachedEntryId", entryId);
            put("lapDelta", 1);
        }});

        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/marshal-adjustment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("entry_not_in_race"));

        assertThat(marshalAdjustmentRepository.findAllByRaceIdOrderByAdjustedAtAsc(scheduleId)).isEmpty();
    }

    @Test
    void marshalAdjustment_invalidLapDelta_returns400() throws Exception {
        Long credentialId = createCredential("Marshal BadDelta Official", "7779");
        String token = login(credentialId, "7779");

        Long scheduleId = seedSchedule(52L, 1, 1, 1, "Buggy");
        Long entryId = seedEntry(503L, "6666668", "Hugh Racer");
        seedGridEntry(scheduleId, entryId, 1, 1);

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("cachedEntryId", entryId);
            put("lapDelta", 5);
        }});

        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/marshal-adjustment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_lap_delta"));
    }

    // --- POST /races/{id}/advance-round ---

    @Test
    void advanceRound_appliesFinishingOrderToNextRoundGrid() throws Exception {
        Long credentialId = createCredential("Advance Test Official", "8888");
        String token = login(credentialId, "8888");

        Long firstScheduleId = seedSchedule(60L, 1, 1, 1, "Buggy");
        Long secondScheduleId = seedSchedule(61L, 2, 1, 2, "Buggy");

        Long entryOneId = seedEntry(601L, "7000001", "Grace Racer");
        Long entryTwoId = seedEntry(602L, "7000002", "Hank Racer");
        Long entryThreeId = seedEntry(603L, "7000003", "Ivy Racer");

        // Seed the next round's grid with arbitrary starting positions.
        seedGridEntry(secondScheduleId, entryOneId, 1, 1);
        seedGridEntry(secondScheduleId, entryTwoId, 2, 2);
        seedGridEntry(secondScheduleId, entryThreeId, 3, 3);

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("nextScheduleId", secondScheduleId);
            put("entryIdsInFinishingOrder", List.of(entryThreeId, entryOneId, entryTwoId));
        }});

        mockMvc.perform(post("/api/v1/race-control/races/" + firstScheduleId + "/advance-round")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.race.id").value(secondScheduleId));

        mockMvc.perform(get("/api/v1/race-control/races/" + secondScheduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grid[0].cachedEntryId").value(entryThreeId))
                .andExpect(jsonPath("$.grid[0].gridPosition").value(1))
                .andExpect(jsonPath("$.grid[1].cachedEntryId").value(entryOneId))
                .andExpect(jsonPath("$.grid[1].gridPosition").value(2))
                .andExpect(jsonPath("$.grid[2].cachedEntryId").value(entryTwoId))
                .andExpect(jsonPath("$.grid[2].gridPosition").value(3));
    }

    @Test
    void advanceRound_unknownNextScheduleId_returns404() throws Exception {
        Long credentialId = createCredential("Advance NotFound Official", "9999");
        String token = login(credentialId, "9999");

        Long firstScheduleId = seedSchedule(70L, 1, 1, 1, "Buggy");

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("nextScheduleId", 999999L);
            put("entryIdsInFinishingOrder", List.of());
        }});

        mockMvc.perform(post("/api/v1/race-control/races/" + firstScheduleId + "/advance-round")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("target_race_not_found"));
    }

    @Test
    void advanceRound_unknownSourceRaceId_returns404() throws Exception {
        Long credentialId = createCredential("Advance SourceNotFound Official", "9998");
        String token = login(credentialId, "9998");

        Long nextScheduleId = seedSchedule(71L, 2, 1, 2, "Buggy");

        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("nextScheduleId", nextScheduleId);
            put("entryIdsInFinishingOrder", List.of());
        }});

        mockMvc.perform(post("/api/v1/race-control/races/999999/advance-round")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("race_not_found"));
    }

    // --- AE4-adjacent: no-auth rejection ---

    @Test
    void transition_noAuthorizationHeader_returns401() throws Exception {
        String requestBody = objectMapper.writeValueAsString(new HashMap<>() {{
            put("target", "GRID");
        }});

        mockMvc.perform(post("/api/v1/race-control/races/1/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
}
