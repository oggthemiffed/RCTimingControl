package dev.monkeypatch.rctiming.persistence;

import dev.monkeypatch.rctiming.persistence.vendor.DatabaseVendor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * {@code rctiming.database.*}: which database the app runs on.
 *
 * @param vendor             the database; picks the dialects and the migrations folder
 * @param migrationLocations classpath folders holding Flyway scripts, each with one sub-folder per
 *                           vendor (so {@code db/migration} reads {@code db/migration/postgresql})
 */
@ConfigurationProperties(prefix = "rctiming.database")
public record DatabaseProperties(
        @DefaultValue("postgresql") DatabaseVendor vendor,
        @DefaultValue("db/migration") List<String> migrationLocations) {
}
