package dev.monkeypatch.build;

import org.flywaydb.core.Flyway;

/**
 * Helper called from app/build.gradle.kts to run Flyway migrations
 * against the jOOQ codegen database before generating DSL classes.
 */
public class FlywayMigrator {

    public static void migrate(String url, String user, String password, String location) {
        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations(location)
                .cleanDisabled(false)
                .load();
        flyway.migrate();
    }
}
