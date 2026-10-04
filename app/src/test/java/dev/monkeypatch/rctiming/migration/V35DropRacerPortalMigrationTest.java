package dev.monkeypatch.rctiming.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V35 (L10, #18) on a database that still holds racer accounts: the RACER role goes, the
 * racer keeps their row but loses their sessions, and an official is untouched.
 */
class V35DropRacerPortalMigrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void start() {
        POSTGRES.start();
    }

    @AfterAll
    static void stop() {
        POSTGRES.stop();
    }

    @Test
    void racerLosesRoleAndSessionsWhileOfficialKeepsBoth() throws SQLException {
        migrateTo("34");
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("insert into users (id, email, password_hash, first_name, last_name) values "
                    + "(1, 'racer@example.com', 'x', 'Ricky', 'Racer'), "
                    + "(2, 'official@example.com', 'x', 'Olly', 'Official')");
            s.execute("insert into user_roles (user_id, role) values (1, 'RACER'), (2, 'RACER'), (2, 'REFEREE')");
            s.execute("insert into refresh_tokens (user_id, token_hash, expires_at) values "
                    + "(1, 'racer-token', now() + interval '1 day'), "
                    + "(2, 'official-token', now() + interval '1 day')");
        }

        migrateTo("35");

        try (Connection c = connect(); Statement s = c.createStatement()) {
            assertThat(strings(s, "select email from users order by id"))
                    .containsExactly("racer@example.com", "official@example.com");
            assertThat(strings(s, "select user_id || ':' || role from user_roles order by user_id"))
                    .containsExactly("2:REFEREE");
            assertThat(strings(s, "select token_hash || ':' || revoked from refresh_tokens order by user_id"))
                    .containsExactly("racer-token:true", "official-token:false");
            assertThat(strings(s, "select table_name from information_schema.tables where table_name in "
                    + "('cars', 'transponders', 'car_tag_categories', 'car_tag_values', 'user_class_ratings', "
                    + "'user_governing_body_memberships', 'password_reset_tokens')"))
                    .isEmpty();
        }
    }

    private static void migrateTo(String version) {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration/postgresql")
                .target(version)
                .load()
                .migrate();
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static List<String> strings(Statement s, String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) values.add(rs.getString(1));
        }
        return values;
    }
}
