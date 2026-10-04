package dev.monkeypatch.rctiming.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.monkeypatch.rctiming.persistence.vendor.DatabaseVendor;
import org.hibernate.cfg.AvailableSettings;
import org.jooq.ConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The one place that knows which database the app runs on (#26). It applies the vendor from
 * {@code rctiming.database.vendor} to the connection pools, Flyway, Hibernate and jOOQ. No other
 * class may name a vendor-specific class or dialect; {@code PersistencePortabilityTest} enforces
 * that.
 *
 * <p>There are two pools on the same database. JPA, Flyway and anything inside a transaction use
 * the write pool, which the vendor may limit to one connection. jOOQ read queries outside a
 * transaction use a read-only pool.
 */
@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class DatabaseConfig {

    private final DatabaseProperties properties;
    private final DatabaseVendor vendor;
    private final String jdbcUrl;

    public DatabaseConfig(DatabaseProperties properties) {
        this.properties = properties;
        this.vendor = properties.vendor();
        Path dataDirectory = properties.effectiveDataDirectory();
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't create the data directory " + dataDirectory, e);
        }
        this.jdbcUrl = vendor.jdbcUrl(dataDirectory);
    }

    @Bean
    @Primary
    DataSource dataSource() {
        HikariConfig config = poolConfig("rctiming-write", vendor.maxWriteConnections());
        return new HikariDataSource(config);
    }

    @Bean
    @Qualifier("read")
    DataSource readDataSource() {
        HikariConfig config = poolConfig("rctiming-read", properties.readConnections());
        config.setConnectionInitSql(vendor.readOnlySessionSql());
        return new HikariDataSource(config);
    }

    @Bean
    ConnectionProvider jooqConnectionProvider(DataSource dataSource, @Qualifier("read") DataSource readDataSource) {
        return new TransactionRoutingConnectionProvider(dataSource, readDataSource);
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

    private HikariConfig poolConfig(String name, int size) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(name);
        config.setJdbcUrl(jdbcUrl);
        config.setDataSourceProperties(vendor.connectionProperties());
        config.setMaximumPoolSize(size);
        config.setMinimumIdle(1);
        return config;
    }
}
