package dev.monkeypatch.rctiming.localday.boards;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.localday.auth.LocalCredential;
import dev.monkeypatch.rctiming.localday.auth.LocalCredentialRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import dev.monkeypatch.rctiming.localday.race.RaceState;
import dev.monkeypatch.rctiming.localday.timing.LapPassingEvent;
import dev.monkeypatch.rctiming.localday.timing.LapTimingService;
import dev.monkeypatch.rctiming.localday.timing.LiveRaceState;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import java.time.Instant;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full HTTP-shaped integration test for U9's {@link BoardController}, driven through
 * {@link MockMvc} against the real embedded-Postgres context with the real security filter chain
 * wired in. Mirrors {@code RaceControlControllerIT}'s established {@code @SpringBootTest} +
 * {@code @DynamicPropertySource} + {@code @TempDir} + {@code @DirtiesContext(AFTER_CLASS)}
 * convention, including its {@code createCredential}/{@code login}/{@code seedSchedule}/
 * {@code seedEntry}/{@code seedGridEntry} helpers (copied verbatim rather than shared, per this
 * module's existing test convention).
 *
 * <p>Every {@code GET} call in this class deliberately omits the {@code Authorization} header —
 * that itself proves anonymous access, since a request that would 401 is a test failure.
 *
 * <p>All tests share one embedded-Postgres database ({@code @DirtiesContext(AFTER_CLASS)}), so
 * {@code results_nothingFinishedYet_returnsNullRaceAndEmptyResults} — the one test asserting
 * global "no FINISHED race exists yet" state — is pinned to run first via
 * {@code @TestMethodOrder}/{@code @Order}, before any other test in this class creates a
 * FINISHED race.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BoardControllerIT {

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
    private LapTimingService lapTimingService;

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

    private void postTransition(String token, Long scheduleId, String target) throws Exception {
        mockMvc.perform(post("/api/v1/race-control/races/" + scheduleId + "/transition")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("target", target);
                        }})))
                .andExpect(status().isOk());
    }

    // --- GET /results ---

    @Test
    @Order(1)
    void results_nothingFinishedYet_returnsNullRaceAndEmptyResults() throws Exception {
        mockMvc.perform(get("/api/v1/boards/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.race").doesNotExist())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results").isEmpty());
    }

    // --- GET /now-next ---
    //
    // These four tests share one database and assert mutually exclusive global states
    // (e.g. "no next race exists"), so each is pinned with @Order and cleans up its own seeded
    // rows afterward — otherwise a later test's leftover PENDING/FINISHED row would make an
    // earlier-ordered test's "doesNotExist" assertion flaky depending on JVM test execution order.

    @Test
    @Order(2)
    void nowNext_idleAfterLastRace_returnsOnlyLastCompleted() throws Exception {
        CachedScheduleEntry race = new CachedScheduleEntry();
        race.setCloudRaceId(101L);
        race.setRoundNumber(1);
        race.setHeatNumber(1);
        race.setSequence(9102);
        race.setClassName("Buggy");
        race.setStatus(RaceState.FINISHED);
        race.setFinishedAt(Instant.now());
        Long scheduleId = cachedScheduleEntryRepository.save(race).getId();

        try {
            mockMvc.perform(get("/api/v1/boards/now-next"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentRace").doesNotExist())
                    .andExpect(jsonPath("$.nextRace").doesNotExist())
                    .andExpect(jsonPath("$.lastCompletedRace.id").value(scheduleId));
        } finally {
            cachedScheduleEntryRepository.deleteById(scheduleId);
        }
    }

    @Test
    @Order(3)
    void nowNext_idleBetweenRounds_returnsBothNextAndLastCompleted() throws Exception {
        CachedScheduleEntry finished = new CachedScheduleEntry();
        finished.setCloudRaceId(102L);
        finished.setRoundNumber(1);
        finished.setHeatNumber(1);
        finished.setSequence(9103);
        finished.setClassName("Buggy");
        finished.setStatus(RaceState.FINISHED);
        finished.setFinishedAt(Instant.now());
        Long finishedId = cachedScheduleEntryRepository.save(finished).getId();

        Long pendingId = seedSchedule(103L, 2, 1, 9104, "Buggy");

        try {
            mockMvc.perform(get("/api/v1/boards/now-next"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentRace").doesNotExist())
                    .andExpect(jsonPath("$.nextRace.id").value(pendingId))
                    .andExpect(jsonPath("$.lastCompletedRace.id").value(finishedId));
        } finally {
            cachedScheduleEntryRepository.deleteById(finishedId);
            cachedScheduleEntryRepository.deleteById(pendingId);
        }
    }

    @Test
    @Order(4)
    void nowNext_idleBeforeFirstHeat_returnsOnlyNextRace() throws Exception {
        Long scheduleId = seedSchedule(100L, 1, 1, 9101, "Buggy");

        try {
            mockMvc.perform(get("/api/v1/boards/now-next"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentRace").doesNotExist())
                    .andExpect(jsonPath("$.lastCompletedRace").doesNotExist())
                    .andExpect(jsonPath("$.nextRace.id").value(scheduleId));
        } finally {
            cachedScheduleEntryRepository.deleteById(scheduleId);
        }
    }

    @Test
    @Order(5)
    void nowNext_raceRunning_returnsCurrentRace() throws Exception {
        CachedScheduleEntry running = new CachedScheduleEntry();
        running.setCloudRaceId(104L);
        running.setRoundNumber(1);
        running.setHeatNumber(1);
        running.setSequence(9105);
        running.setClassName("Buggy");
        running.setStatus(RaceState.RUNNING);
        Long runningId = cachedScheduleEntryRepository.save(running).getId();

        try {
            mockMvc.perform(get("/api/v1/boards/now-next"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentRace.id").value(runningId));
        } finally {
            cachedScheduleEntryRepository.deleteById(runningId);
        }
    }

    // --- Happy + integration: capture-before-release pipeline via the real transition flow ---

    @Test
    @Order(6)
    void results_afterRealFinishTransition_showsCapturedPositions() throws Exception {
        Long credentialId = createCredential("Board Pipeline Official", "8001");
        String token = login(credentialId, "8001");

        Long scheduleId = seedSchedule(200L, 1, 1, 9201, "Buggy");
        Long entryId = seedEntry(701L, "8000001", "Zara Racer");
        seedGridEntry(scheduleId, entryId, 1, 1);

        postTransition(token, scheduleId, "GRID");
        postTransition(token, scheduleId, "RUNNING");

        // Inject a simulated live position directly into the in-memory LiveRaceState, the
        // simplest real path to seed a passing without needing actual decoder ingestion —
        // mirrors LapTimingServiceTest's direct-state-manipulation style.
        LiveRaceState state = lapTimingService.stateFor(scheduleId);
        state.applyLapPassing(new LapPassingEvent(scheduleId, "8000001", 1_000_000L), entryId);
        state.applyLapPassing(new LapPassingEvent(scheduleId, "8000001", 11_000_000L), entryId);

        postTransition(token, scheduleId, "FINISHED");

        mockMvc.perform(get("/api/v1/boards/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.race.id").value(scheduleId))
                .andExpect(jsonPath("$.results[0].entryId").value(entryId))
                .andExpect(jsonPath("$.results[0].racerName").value("Zara Racer"))
                .andExpect(jsonPath("$.results[0].transponderNumber").value("8000001"))
                .andExpect(jsonPath("$.results[0].position").value(1))
                .andExpect(jsonPath("$.results[0].lapsCompleted").value(2));
    }
}
