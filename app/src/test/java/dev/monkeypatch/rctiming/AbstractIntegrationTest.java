package dev.monkeypatch.rctiming;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared base class for all integration tests.
 *
 * <p>Every test class runs against the same temporary SQLite database, created once per test run.
 * The property source is identical for all subclasses, so Spring Boot's test context cache reuses
 * one ApplicationContext across them, as it did with the shared PostgreSQL container before #26.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    private static final Path DATA_DIRECTORY;

    static {
        try {
            DATA_DIRECTORY = Files.createTempDirectory("rctiming-test-db");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("rctiming.database.data-directory", DATA_DIRECTORY::toString);
    }
}
