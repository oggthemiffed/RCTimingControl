package dev.monkeypatch.rctiming.persistence;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** The configured vendor reaches Flyway, Hibernate and jOOQ through {@link DatabaseConfig}. */
class DatabaseConfigIT extends AbstractIntegrationTest {

    @Autowired DatabaseProperties properties;
    @Autowired DSLContext dsl;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired Flyway flyway;

    @Test
    void vendorDrivesTheDialectsAndMigrationFolders() {
        var vendor = properties.vendor();

        assertThat(dsl.dialect()).isEqualTo(vendor.jooqDialect());
        assertThat(entityManagerFactory.getProperties().get("hibernate.dialect"))
                .isEqualTo(vendor.hibernateDialect());
        assertThat(flyway.getConfiguration().getLocations())
                .extracting(Object::toString)
                .containsExactly(
                        "classpath:db/migration/" + vendor.migrationFolder(),
                        "classpath:db/testdata/" + vendor.migrationFolder());
    }
}
