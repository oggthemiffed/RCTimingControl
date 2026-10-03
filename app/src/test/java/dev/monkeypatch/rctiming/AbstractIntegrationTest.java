package dev.monkeypatch.rctiming;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared base class for all integration tests.
 *
 * Uses a manually started singleton container (no @Testcontainers annotation) so the same
 * PostgreSQL container is reused across all test classes within a single Gradle test run.
 * Spring Boot's test context cache then reuses the same ApplicationContext for all subclasses.
 *
 * @ServiceConnection auto-configures spring.datasource.* — no @DynamicPropertySource needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
