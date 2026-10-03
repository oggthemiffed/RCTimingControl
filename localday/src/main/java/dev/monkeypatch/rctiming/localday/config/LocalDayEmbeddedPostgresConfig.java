package dev.monkeypatch.rctiming.localday.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provisions the embedded PostgreSQL instance that backs this module's durable local store.
 *
 * <p>The data directory is a real on-disk path (never tmpfs/in-memory) — durability across an
 * unclean shutdown (a venue laptop losing power, a crashed process, a slammed lid) is the entire
 * reason embedded Postgres was chosen over H2 for this module. {@code setCleanDataDirectory(false)}
 * is critical: the Zonky library defaults to wiping the data directory on every start, which is
 * the correct behaviour for ephemeral test databases but would silently destroy all race-day data
 * on every restart of this application.
 *
 * <p>The {@link EmbeddedPostgres} bean is declared with {@code destroyMethod = "close"} so Spring
 * stops postmaster cleanly as part of normal application-context shutdown (see
 * {@code spring.lifecycle.timeout-per-shutdown-phase} in application.yml for the grace period).
 */
@Configuration
public class LocalDayEmbeddedPostgresConfig {

    @Value("${localday.datasource.embedded-postgres.data-directory}")
    private String dataDirectoryProperty;

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        Path dataDirectory = Path.of(dataDirectoryProperty).toAbsolutePath().normalize();
        Files.createDirectories(dataDirectory);

        return EmbeddedPostgres.builder()
                .setDataDirectory(dataDirectory)
                // Never wipe existing data on startup — see class-level javadoc.
                .setCleanDataDirectory(false)
                .start();
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        return embeddedPostgres.getPostgresDatabase();
    }
}
