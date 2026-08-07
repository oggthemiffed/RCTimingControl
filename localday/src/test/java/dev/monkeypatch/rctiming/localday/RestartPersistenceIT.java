package dev.monkeypatch.rctiming.localday;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge case: data written before a shutdown must still be there after the process restarts
 * against the same on-disk data directory. This is the entire reason embedded Postgres was
 * chosen over H2 for this module (KTD1) — proving it end to end means driving two full,
 * sequential {@link ConfigurableApplicationContext} lifecycles by hand rather than relying on
 * {@code @SpringBootTest}'s context caching, which would just reuse one context and prove
 * nothing about restart durability.
 *
 * <p>{@code server.port=0} and a random Postgres port (the module's default auto-detection)
 * keep this test isolated from other test classes running in the same JVM/Gradle worker.
 */
class RestartPersistenceIT {

    private Path dataDir;

    @AfterEach
    void cleanUp() throws IOException {
        if (dataDir != null) {
            FileSystemUtils.deleteRecursively(dataDir);
        }
    }

    @Test
    void cachedEntrySurvivesApplicationRestartAgainstSameDataDirectory() throws IOException {
        dataDir = Files.createTempDirectory("localday-restart-it");
        Path pgDataDir = dataDir.resolve("pg");

        Long savedId;

        // Passed as command-line-style application arguments (not .properties(...), which adds
        // a low-priority "default properties" source that application.yml's own value for this
        // key would win over) so this override actually takes effect instead of silently falling
        // back to the module's real ./data/localday-pg default and colluding across test runs.
        ConfigurableApplicationContext firstRun = new SpringApplicationBuilder(LocalDayApplication.class)
                .run(
                        "--server.port=0",
                        "--localday.datasource.embedded-postgres.data-directory=" + pgDataDir
                );
        try {
            CachedEntryRepository repository = firstRun.getBean(CachedEntryRepository.class);
            CachedEntry entry = new CachedEntry();
            entry.setCloudEntryId(555L);
            entry.setTransponderNumber("9998887");
            entry.setRacerName("Restart Survivor");
            savedId = repository.save(entry).getId();
        } finally {
            // Must close cleanly (this invokes the EmbeddedPostgres destroy method, i.e.
            // pg_ctl stop) before starting a second instance against the same data directory.
            firstRun.close();
        }

        ConfigurableApplicationContext secondRun = new SpringApplicationBuilder(LocalDayApplication.class)
                .run(
                        "--server.port=0",
                        "--localday.datasource.embedded-postgres.data-directory=" + pgDataDir
                );
        try {
            CachedEntryRepository repository = secondRun.getBean(CachedEntryRepository.class);
            Optional<CachedEntry> reloaded = repository.findById(savedId);

            assertThat(reloaded).isPresent();
            assertThat(reloaded.get().getRacerName()).isEqualTo("Restart Survivor");
            assertThat(reloaded.get().getTransponderNumber()).isEqualTo("9998887");
        } finally {
            secondRun.close();
        }
    }
}
