package dev.monkeypatch.rctiming.localday;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

/**
 * Happy-path context-load test: proves the Spring context starts, embedded PostgreSQL
 * initializes at the configured data directory, and Flyway applies V1 successfully.
 *
 * <p>Each test class gets its own temp data directory (embedded Postgres owns an exclusive
 * lock on the directory it manages) so this suite can run alongside others in the same JVM.
 *
 * <p>{@code @DirtiesContext(AFTER_CLASS)} forces Spring to close this context (stopping the
 * embedded Postgres process) at the end of this test class, rather than leaving it in Spring's
 * test context cache to be closed by a JVM-shutdown hook much later. Without this, JUnit's
 * static {@code @TempDir} deletes the data directory on its own {@code AfterAll} — which fires
 * before that later shutdown hook — out from under a still-running postgres process.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocalDayApplicationTests {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void localdayProperties(DynamicPropertyRegistry registry) {
        registry.add("localday.datasource.embedded-postgres.data-directory",
                () -> dataDir.resolve("pg").toString());
    }

    @Test
    void contextLoads() {
        // If the context fails to start — embedded Postgres doesn't come up, or Flyway fails
        // to apply V1 — this test fails with the underlying exception.
    }
}
