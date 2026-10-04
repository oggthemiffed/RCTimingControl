package dev.monkeypatch.rctiming.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.domain.competitor.Competitor;
import dev.monkeypatch.rctiming.domain.competitor.CompetitorRepository;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RaceHub Entry Export v1 import (L7, #15), driven by the fixtures in {@code racehub/}.
 */
class RaceHubImportIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntryRepository entryRepository;
    @Autowired CompetitorRepository competitorRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;

    private String adminToken;
    private String run;
    private long eventId;
    private long buggyClassId;
    private long truckClassId;

    @BeforeEach
    void setUp() {
        run = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        adminToken = loginAs(Set.of(Role.ADMIN));

        eventId = jdbc.queryForObject("""
                insert into events (name, event_date, status, created_at, updated_at)
                values (?, current_date + 14, 'OPEN', now(), now()) returning id""",
                Long.class, "RaceHub import " + run);
        buggyClassId = createEventClass("RH Buggy " + run);
        truckClassId = createEventClass("RH Truck " + run);
    }

    @Test
    void newImport_createsCompetitorsAndEntries() {
        ResponseEntity<JsonNode> resp = importFixture("entries-v1-initial.json", false);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = resp.getBody();
        assertThat(body.get("applied").asBoolean()).isTrue();
        assertThat(body.get("racehubEventName").asText()).isEqualTo("Club Round 3");
        assertThat(body.get("revision").asLong()).isEqualTo(4);
        assertSummary(body, 2, 0, 0, 0, 0, 1);

        Entry ada = entry("a1");
        assertThat(ada.getEventId()).isEqualTo(eventId);
        assertThat(ada.getEventClassId()).isEqualTo(buggyClassId);
        assertThat(ada.getStatus()).isEqualTo(EntryStatus.CONFIRMED);
        assertThat(ada.getUserId()).isNull();
        assertThat(ada.getTransponderNumberSnapshot()).isEqualTo("71" + run);
        assertThat(ada.getSecondaryTransponderNumber()).isEqualTo("72" + run);
        assertThat(ada.getExternalEntryVersion()).isEqualTo(1L);
        assertThat(ada.getRacehubArrival()).isEqualTo("NOT_ARRIVED");

        Competitor adaDriver = competitorRepository.findById(ada.getCompetitorId()).orElseThrow();
        assertThat(adaDriver.getExternalSource()).isEqualTo("RACEHUB");
        assertThat(adaDriver.getExternalId()).isEqualTo("drv-ada-" + run);
        assertThat(adaDriver.getDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(adaDriver.getBrcaNumber()).isEqualTo("BR12345");
        assertThat(adaDriver.getHomeClub()).isEqualTo("Analytical RC");

        // Class matched by rc_class_name, ignoring case
        Entry grace = entry("b2");
        assertThat(grace.getEventClassId()).isEqualTo(truckClassId);
        assertThat(grace.getRacehubArrival()).isEqualTo("ARRIVED");

        // Withdrawn and never imported: nothing to create
        assertThat(findEntry("c3")).isNull();
    }

    @Test
    void dryRun_returnsPreviewAndSavesNothing() {
        ResponseEntity<JsonNode> resp = importFixture("entries-v1-initial.json", true);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("dryRun").asBoolean()).isTrue();
        assertThat(resp.getBody().get("applied").asBoolean()).isFalse();
        assertSummary(resp.getBody(), 2, 0, 0, 0, 0, 1);
        assertThat(findEntry("a1")).isNull();
        assertThat(competitorRepository.findByExternalSourceAndExternalId("RACEHUB", "drv-ada-" + run)).isEmpty();
    }

    @Test
    void replay_isANoOp() {
        importFixture("entries-v1-initial.json", false);
        Instant updatedAt = entry("a1").getUpdatedAt();

        ResponseEntity<JsonNode> resp = importFixture("entries-v1-initial.json", false);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertSummary(resp.getBody(), 0, 0, 0, 2, 0, 1);
        assertThat(entry("a1").getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(entryRepository.findByEventId(eventId)).hasSize(2);
    }

    @Test
    void higherVersion_updatesTheEntry() {
        importFixture("entries-v1-initial.json", false);
        long adaId = entry("a1").getId();

        ResponseEntity<JsonNode> resp = importFixture("entries-v1-update.json", false);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertSummary(resp.getBody(), 0, 1, 1, 0, 0, 1);
        Entry ada = entry("a1");
        assertThat(ada.getId()).isEqualTo(adaId);
        assertThat(ada.getExternalEntryVersion()).isEqualTo(2L);
        assertThat(ada.getTransponderNumberSnapshot()).isEqualTo("75" + run);
        assertThat(ada.getSecondaryTransponderNumber()).isNull();
        assertThat(ada.getRacehubArrival()).isEqualTo("ARRIVED");
        assertThat(competitorRepository.findById(ada.getCompetitorId()).orElseThrow().getHomeClub())
                .isEqualTo("Difference Engine RC");
    }

    @Test
    void lowerVersion_isIgnored() {
        importFixture("entries-v1-initial.json", false);
        importFixture("entries-v1-update.json", false);

        ResponseEntity<JsonNode> resp = importFixture("entries-v1-stale.json", false);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertSummary(resp.getBody(), 0, 0, 0, 0, 1, 0);
        Entry ada = entry("a1");
        assertThat(ada.getExternalEntryVersion()).isEqualTo(2L);
        assertThat(ada.getTransponderNumberSnapshot()).isEqualTo("75" + run);
        assertThat(competitorRepository.findById(ada.getCompetitorId()).orElseThrow().getDisplayName())
                .isEqualTo("Ada Lovelace");
    }

    @Test
    void withdrawal_marksWithdrawnAndKeepsRaceHistory() {
        importFixture("entries-v1-initial.json", false);
        long graceId = entry("b2").getId();
        // Grace has already raced: a race entry links her to a heat
        long roundId = jdbc.queryForObject("""
                insert into rounds (event_id, type, round_number, sequence_in_event)
                values (?, 'QUALIFIER', 1, 1) returning id""", Long.class, eventId);
        long raceId = jdbc.queryForObject("""
                insert into races (round_id, event_class_id, heat_number, sequence_in_round, start_type, status)
                values (?, ?, 1, 1, 'STAGGER', 'FINISHED') returning id""", Long.class, roundId, truckClassId);
        jdbc.update("insert into race_entries (race_id, entry_id, grid_position) values (?, ?, 1)", raceId, graceId);

        ResponseEntity<JsonNode> resp = importFixture("entries-v1-update.json", false);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Entry grace = entryRepository.findById(graceId).orElseThrow();
        assertThat(grace.getStatus()).isEqualTo(EntryStatus.WITHDRAWN);
        assertThat(grace.getWithdrawnAt()).isNotNull();
        assertThat(grace.getExternalEntryVersion()).isEqualTo(4L);
        assertThat(jdbc.queryForObject("select count(*) from race_entries where entry_id = ?", Integer.class, graceId))
                .isEqualTo(1);
    }

    @Test
    void unmappedClass_blocksTheImportAndIsListed() {
        ResponseEntity<JsonNode> preview = importFixture("entries-v1-unmapped-class.json", true);
        assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preview.getBody().get("blocked").asBoolean()).isTrue();
        JsonNode unmapped = preview.getBody().get("unmappedClasses");
        assertThat(unmapped).hasSize(1);
        assertThat(unmapped.get(0).get("racehubEventClassId").asText()).isEqualTo("rh-class-electric-" + run);
        assertThat(unmapped.get(0).get("rcClassName").asText()).isEqualTo("RH Electric Touring " + run);
        assertThat(unmapped.get(0).get("entryCount").asInt()).isEqualTo(1);

        ResponseEntity<JsonNode> blocked = importFixture("entries-v1-unmapped-class.json", false);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(blocked.getBody().get("applied").asBoolean()).isFalse();
        assertThat(findEntry("u1")).isNull();
    }

    @Test
    void classMapping_resolvesAnUnmappedClass() {
        var put = restTemplate.exchange("/api/v1/admin/events/" + eventId + "/racehub-class-mappings",
                HttpMethod.PUT, new HttpEntity<>(List.of(Map.of(
                        "racehubEventClassId", "rh-class-electric-" + run, "eventClassId", buggyClassId)), adminHeaders()),
                JsonNode.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);

        var get = restTemplate.exchange("/api/v1/admin/events/" + eventId + "/racehub-class-mappings",
                HttpMethod.GET, new HttpEntity<>(adminHeaders()), JsonNode.class);
        assertThat(get.getBody()).hasSize(1);
        assertThat(get.getBody().get(0).get("eventClassId").asLong()).isEqualTo(buggyClassId);

        ResponseEntity<JsonNode> resp = importFixture("entries-v1-unmapped-class.json", false);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entry("u1").getEventClassId()).isEqualTo(buggyClassId);
    }

    @Test
    void classMapping_rejectsAClassFromAnotherEvent() {
        var put = restTemplate.exchange("/api/v1/admin/events/" + eventId + "/racehub-class-mappings",
                HttpMethod.PUT, new HttpEntity<>(List.of(Map.of(
                        "racehubEventClassId", "rh-class-electric-" + run, "eventClassId", 2001L)), adminHeaders()),
                String.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void duplicateTransponder_isAWarningNotAnError() {
        ResponseEntity<JsonNode> resp = importFixture("entries-v1-duplicate-transponder.json", false);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("blocked").asBoolean()).isFalse();
        assertThat(resp.getBody().get("errors")).isEmpty();
        JsonNode warnings = resp.getBody().get("warnings");
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0).asText())
                .contains("71" + run).contains("Ada Lovelace").contains("Grace Hopper");
        assertThat(entryRepository.findByEventId(eventId)).hasSize(2);
    }

    @Test
    void wrongSchemaVersion_isRejected() {
        String json = fixture("entries-v1-initial.json").replace("\"schema_version\": 1", "\"schema_version\": 2");
        var resp = restTemplate.exchange("/api/v1/admin/events/" + eventId + "/racehub-import",
                HttpMethod.POST, new HttpEntity<>(json, adminHeaders()), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void nonAdmin_isForbidden() {
        String refereeToken = loginAs(Set.of(Role.REFEREE));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(refereeToken);
        var resp = restTemplate.exchange("/api/v1/admin/events/" + eventId + "/racehub-import?dryRun=true",
                HttpMethod.POST, new HttpEntity<>(fixture("entries-v1-initial.json"), headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private ResponseEntity<JsonNode> importFixture(String name, boolean dryRun) {
        return restTemplate.exchange("/api/v1/admin/events/" + eventId + "/racehub-import?dryRun=" + dryRun,
                HttpMethod.POST, new HttpEntity<>(fixture(name), adminHeaders()), JsonNode.class);
    }

    private String fixture(String name) {
        try {
            String raw = new ClassPathResource("racehub/" + name).getContentAsString(StandardCharsets.UTF_8);
            return raw.replace("{{run}}", run);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void assertSummary(JsonNode body, int created, int updated, int withdrawn,
                                      int unchanged, int stale, int skipped) {
        JsonNode s = body.get("summary");
        assertThat(List.of(s.get("created").asInt(), s.get("updated").asInt(), s.get("withdrawn").asInt(),
                s.get("unchanged").asInt(), s.get("stale").asInt(), s.get("skipped").asInt()))
                .as("created, updated, withdrawn, unchanged, stale, skipped")
                .containsExactly(created, updated, withdrawn, unchanged, stale, skipped);
    }

    private Entry entry(String prefix) {
        Entry e = findEntry(prefix);
        assertThat(e).as("entry " + prefix).isNotNull();
        return e;
    }

    private Entry findEntry(String prefix) {
        return entryRepository.findByExternalSourceAndExternalEntryId("RACEHUB", prefix + "-" + run).orElse(null);
    }

    private long createEventClass(String racingClassName) {
        long racingClassId = jdbc.queryForObject("""
                insert into racing_classes (name, created_at, updated_at) values (?, now(), now()) returning id""",
                Long.class, racingClassName);
        return jdbc.queryForObject("""
                insert into event_classes (event_id, racing_class_id, config_snapshot, created_at, updated_at)
                values (?, ?, '{"type":"TIMED"}'::jsonb, now(), now()) returning id""",
                Long.class, eventId, racingClassId);
    }

    private String loginAs(Set<Role> roles) {
        String email = "racehub-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("pass12345"));
        user.setFirstName("Staff");
        user.setLastName("User");
        user.setRoles(roles);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
        var login = restTemplate.postForEntity("/api/v1/auth/login", new LoginRequest(email, "pass12345"), AuthResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        return login.getBody().accessToken();
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        return headers;
    }
}
