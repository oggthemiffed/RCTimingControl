package dev.monkeypatch.rctiming.backup;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.RcTimingApplication;
import dev.monkeypatch.rctiming.domain.championship.Championship;
import dev.monkeypatch.rctiming.domain.championship.ChampionshipRepository;
import dev.monkeypatch.rctiming.domain.championship.ScoringSource;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.persistence.DatabaseProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Back up, wipe, restore (#22): the restore command puts a backup in place and the app starts on
 * it with the events and championships intact. It refuses while the app has the database open.
 */
class RestoreIT extends AbstractIntegrationTest {

    @Autowired BackupService backupService;
    @Autowired DatabaseProperties databaseProperties;
    @Autowired EventRepository eventRepository;
    @Autowired ChampionshipRepository championshipRepository;

    @TempDir Path newInstall;

    @Test
    void aRestoredBackupStartsWithTheEventsAndChampionshipsIntact() throws Exception {
        Championship championship = new Championship();
        championship.setName("Restore series " + UUID.randomUUID());
        championship.setScoringSource(ScoringSource.FINALS);
        championship.setCreatedAt(Instant.now());
        championship.setUpdatedAt(Instant.now());
        championshipRepository.save(championship);
        List<String> events = eventRepository.findAll().stream().map(e -> e.getId() + " " + e.getName()).sorted().toList();
        List<String> championships = championshipRepository.findAll().stream().map(Championship::getName).sorted().toList();
        Path backup = backupService.directory().resolve(backupService.backup("manual").name());

        // The new install already has a database of its own, which the restore must set aside
        Files.writeString(newInstall.resolve("rctiming.db"), "");
        int exit = RestoreCommand.run(new String[] {
                backup.toString(), "--rctiming.database.data-directory=" + newInstall});

        assertThat(exit).isZero();
        // The restore leaves a note for the app's next start to put in the audit log
        assertThat(newInstall.resolve("restore-pending.properties")).exists();
        try (Stream<Path> files = Files.list(newInstall)) {
            assertThat(files.map(f -> f.getFileName().toString())).anyMatch(n -> n.startsWith("rctiming.db.before-restore-"));
        }
        try (ConfigurableApplicationContext restored = new SpringApplicationBuilder(RcTimingApplication.class)
                .web(WebApplicationType.NONE)
                .run("--rctiming.database.data-directory=" + newInstall,
                        "--app.decoder.listener.enabled=false",
                        "--tts.enabled=false",
                        "--rctiming.backup.nightly-cron=-")) {
            assertThat(restored.getBean(EventRepository.class).findAll().stream()
                    .map(e -> e.getId() + " " + e.getName()).sorted().toList()).isEqualTo(events);
            assertThat(restored.getBean(ChampionshipRepository.class).findAll().stream()
                    .map(Championship::getName).sorted().toList()).isEqualTo(championships);
            // The restored database says it was restored, by whom, from which backup; the note is used up
            List<java.util.Map<String, Object>> rows = restored.getBean(org.jooq.DSLContext.class)
                    .fetch("select * from audit_log where action = 'DATABASE_RESTORED'").intoMaps();
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).get("source")).isEqualTo("CLI");
            assertThat(rows.get(0).get("actor_label").toString())
                    .isEqualTo("cli:" + System.getProperty("user.name"));
            assertThat(rows.get(0).get("summary").toString()).contains(backup.toString());
        }
        assertThat(newInstall.resolve("restore-pending.properties")).doesNotExist();
    }

    @Test
    void aRestoreRunWithSudoLeavesTheDatabaseWithTheServiceUser() throws Exception {
        // The installed Linux service has its own user; only root can hand a file to another user
        assumeTrue("root".equals(System.getProperty("user.name")));
        UserPrincipal serviceUser = newInstall.getFileSystem().getUserPrincipalLookupService()
                .lookupPrincipalByName("nobody");
        Files.setOwner(newInstall, serviceUser);
        Path backup = backupService.directory().resolve(backupService.backup("manual").name());

        int exit = RestoreCommand.run(new String[] {
                backup.toString(), "--rctiming.database.data-directory=" + newInstall});

        assertThat(exit).isZero();
        PosixFileAttributes restored = Files.readAttributes(newInstall.resolve("rctiming.db"), PosixFileAttributes.class);
        PosixFileAttributes folder = Files.readAttributes(newInstall, PosixFileAttributes.class);
        assertThat(restored.owner()).isEqualTo(serviceUser);
        assertThat(restored.group()).isEqualTo(folder.group());
    }

    @Test
    void restoreRefusesWhileTheAppHasTheDatabaseOpen() {
        Path backup = backupService.directory().resolve(backupService.backup("manual").name());
        Path live = databaseProperties.effectiveDataDirectory();

        int exit = RestoreCommand.run(new String[] {backup.toString(), "--rctiming.database.data-directory=" + live});

        assertThat(exit).isEqualTo(1);
        assertThat(eventRepository.count()).isPositive();
    }

    @Test
    void restoreRejectsAFileThatIsNotABackup() throws Exception {
        Path notABackup = Files.writeString(newInstall.resolve("notes.db"), "not a database");

        int exit = RestoreCommand.run(new String[] {
                notABackup.toString(), "--rctiming.database.data-directory=" + newInstall.resolve("data")});

        assertThat(exit).isEqualTo(1);
        assertThat(newInstall.resolve("data/rctiming.db")).doesNotExist();
    }
}
