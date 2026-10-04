package dev.monkeypatch.rctiming.persistence;

import dev.monkeypatch.rctiming.persistence.vendor.DatabaseVendor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one place that knows which database the app runs on (#26). It applies the vendor from
 * {@code rctiming.database.vendor} to Flyway, Hibernate and jOOQ. No other class may name a
 * vendor-specific class or dialect; {@code PersistencePortabilityTest} enforces that.
 */
@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class DatabaseConfig {

    private final DatabaseVendor vendor;
    private final DatabaseProperties properties;

    public DatabaseConfig(DatabaseProperties properties) {
        this.properties = properties;
        this.vendor = properties.vendor();
    }

    @Bean
    FlywayConfigurationCustomizer vendorMigrationLocations() {
        String[] locations = properties.migrationLocations().stream()
                .map(location -> "classpath:" + location + "/" + vendor.migrationFolder())
                .toArray(String[]::new);
        return configuration -> configuration.locations(locations);
    }

    @Bean
    HibernatePropertiesCustomizer vendorHibernateDialect() {
        return hibernateProperties -> hibernateProperties.put(AvailableSettings.DIALECT, vendor.hibernateDialect());
    }

    @Bean
    DefaultConfigurationCustomizer vendorJooqDialect() {
        return configuration -> configuration.set(vendor.jooqDialect());
    }
}
