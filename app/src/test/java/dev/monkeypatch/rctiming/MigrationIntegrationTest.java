package dev.monkeypatch.rctiming;

import org.junit.jupiter.api.Test;

// Testcontainers boots the Spring context with Flyway auto-applied on startup.
// If V25 has a syntax error the context load fails and this test errors.
// No explicit assertions needed — a passing test proves Flyway V25 applied cleanly.
// See VALIDATION.md CTRL-wizard-02.
class MigrationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void migrationV25AppliesWithoutError() {
        // context load success is the assertion
    }
}
