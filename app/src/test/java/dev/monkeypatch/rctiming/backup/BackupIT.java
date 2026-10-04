package dev.monkeypatch.rctiming.backup;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.admin.BackupController;
import dev.monkeypatch.rctiming.api.auth.AuthResponse;
import dev.monkeypatch.rctiming.api.auth.LoginRequest;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.event.EventService;
import dev.monkeypatch.rctiming.domain.event.EventStatus;
import dev.monkeypatch.rctiming.domain.practice.PracticeSessionRepository;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.persistence.DatabaseProperties;
import dev.monkeypatch.rctiming.practice.PracticeSessionService;
import dev.monkeypatch.rctiming.timing.LapPassingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Database backups (#22): taken on demand, at day close and while laps are being written. */
class BackupIT extends AbstractIntegrationTest {

    @Autowired BackupService backupService;
    @Autowired DatabaseProperties databaseProperties;
    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EventRepository eventRepository;
    @Autowired EventService eventService;
    @Autowired PracticeSessionService practiceSessionService;
    @Autowired PracticeSessionRepository practiceSessionRepository;
    @Autowired ApplicationEventPublisher eventPublisher;

    @TempDir Path backupFolder;

    @Test
    void anAdminTakesABackupNowAndSeesItListed() throws SQLException {
        HttpHeaders admin = adminHeaders();

        ResponseEntity<BackupFile> taken = restTemplate.exchange("/api/v1/admin/backups", HttpMethod.POST,
                new HttpEntity<>(admin), BackupFile.class);
        assertThat(taken.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(taken.getBody().reason()).isEqualTo("manual");

        ResponseEntity<BackupController.BackupsDto> listed = restTemplate.exchange("/api/v1/admin/backups",
                HttpMethod.GET, new HttpEntity<>(admin), BackupController.BackupsDto.class);
        assertThat(listed.getBody().backups()).extracting(BackupFile::name).contains(taken.getBody().name());

        Path file = backupService.directory().resolve(taken.getBody().name());
        databaseProperties.vendor().checkBackup(file);
        assertThat(count(file, "SELECT count(*) FROM events")).isPositive();
    }

    @Test
    void backupsAreForAdminsOnly() {
        assertThat(restTemplate.getForEntity("/api/v1/admin/backups", String.class).getStatusCode())
                .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void onlyTheNewestBackupsAreKept() {
        BackupService keepTwo = new BackupService(databaseProperties, new BackupProperties(backupFolder, 2, "-"));

        keepTwo.backup("manual");
        keepTwo.backup("manual");
        BackupFile newest = keepTwo.backup("nightly");

        assertThat(keepTwo.list()).hasSize(2).first().isEqualTo(newest);
    }

    @Test
    void closingTheRaceDayTakesABackup() {
        Event event = new Event();
        event.setName("Day close " + UUID.randomUUID());
        event.setEventDate(LocalDate.now());
        event.setStatus(EventStatus.IN_PROGRESS);
        event.setCreatedAt(Instant.now());
        event.setUpdatedAt(Instant.now());
        long eventId = eventRepository.save(event).getId();

        eventService.transition(eventId, EventStatus.COMPLETED);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(backupService.list()).extracting(BackupFile::reason).contains("day-close"));
    }

    @Test
    void aBackupTakenWhileLapsAreWrittenOpensCleanly() throws Exception {
        practiceSessionRepository.findRunningSession()
                .ifPresent(running -> practiceSessionService.stop(running.getId()));
        long sessionId = practiceSessionService
                .create(new PracticeSessionService.CreateRequest("Backup race", null, 3), null).id();
        practiceSessionService.start(sessionId);
        AtomicBoolean racing = new AtomicBoolean(true);
        CompletableFuture<Integer> decoder = CompletableFuture.supplyAsync(() -> {
            int passings = 0;
            while (racing.get() || passings < 100) {
                eventPublisher.publishEvent(new LapPassingEvent(0, String.valueOf(950 + passings % 6), passings * 3_000_000L));
                passings++;
            }
            return passings;
        });
        try {
            BackupService duringRace = new BackupService(databaseProperties, new BackupProperties(backupFolder, 5, "-"));
            await().atMost(Duration.ofSeconds(20)).until(() -> lapsSoFar(sessionId) > 20);

            BackupFile backup = duringRace.backup("manual");

            racing.set(false);
            decoder.get();
            Path file = backupFolder.resolve(backup.name());
            databaseProperties.vendor().checkBackup(file);
            long lapsInBackup = count(file, "SELECT count(*) FROM practice_laps WHERE practice_session_id = " + sessionId);
            assertThat(lapsInBackup).isPositive().isLessThanOrEqualTo(lapsSoFar(sessionId));
        } finally {
            racing.set(false);
            practiceSessionService.stop(sessionId);
        }
    }

    private long lapsSoFar(long sessionId) throws SQLException {
        Path live = databaseProperties.effectiveDataDirectory().resolve("rctiming.db");
        return count(live, "SELECT count(*) FROM practice_laps WHERE practice_session_id = " + sessionId);
    }

    private static long count(Path database, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             ResultSet rs = connection.createStatement().executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private HttpHeaders adminHeaders() {
        String email = "admin-backup-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("adminPass123"));
        user.setFirstName("Admin");
        user.setLastName("Backups");
        user.setRoles(Set.of(Role.ADMIN));
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        AuthResponse auth = restTemplate.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, "adminPass123"), AuthResponse.class).getBody();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(auth.accessToken());
        return headers;
    }
}
