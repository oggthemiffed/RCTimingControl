package dev.monkeypatch.rctiming;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared base class for all integration tests.
 *
 * Uses a manually started singleton container (no @Testcontainers annotation) so the same
 * PostgreSQL container is reused across all test classes within a single Gradle test run.
 * Spring Boot's test context cache then reuses the same ApplicationContext for all subclasses.
 *
 * @ServiceConnection auto-configures spring.datasource.* — no @DynamicPropertySource needed.
 *
 * Phase 5: app.grpc.port=0 causes ForwarderGrpcServer to bind on a random OS-assigned port,
 * preventing port conflicts when multiple Spring test contexts run in the same JVM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.grpc.port=0")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
