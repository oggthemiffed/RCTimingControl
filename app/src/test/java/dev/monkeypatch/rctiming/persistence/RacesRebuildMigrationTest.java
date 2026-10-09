package dev.monkeypatch.rctiming.persistence;

import dev.monkeypatch.rctiming.persistence.vendor.DatabaseVendor;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V20 rebuilds {@code races}, which every other test only ever sees empty. Here a club database at V19 holding a race
 * with rows that cascade from it is upgraded, on one connection set up as the app's write pool sets it up. If the
 * script ever ran inside Flyway's transaction, foreign keys would stay on and dropping races would delete them.
 */
class RacesRebuildMigrationTest {

    @TempDir Path dataDirectory;

    private SingleConnectionDataSource dataSource;

    @AfterEach
    void close() {
        if (dataSource != null) {
            dataSource.destroy();
        }
    }

    @Test
    void upgradingKeepsEveryRaceAndWhatHangsOffItAndAllowsARollingStart() {
        DatabaseVendor vendor = DatabaseVendor.SQLITE;
        dataSource = new SingleConnectionDataSource(vendor.jdbcUrl(dataDirectory), true);
        dataSource.setConnectionProperties(vendor.connectionProperties());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        migrate("19");
        // Rows a club would have, without building their parents: only the rebuild of races is under test
        jdbc.execute("PRAGMA foreign_keys = OFF");
        jdbc.update("""
                insert into races (id, round_id, event_class_id, heat_number, sequence_in_round, start_type, status,
                                   abandoned_at, bump_slots)
                values (7, 1, 1, 1, 1, 'GRID', 'FINISHED', 123, 2)""");
        jdbc.update("insert into race_entries (race_id, entry_id) values (7, 1)");
        jdbc.update("""
                insert into result_snapshots (race_id, finished_at, positions_json, lap_history_json)
                values (7, 456, '[]', '[]')""");
        // A later race that was deleted: its id must not be handed out again
        jdbc.update("""
                insert into races (id, round_id, event_class_id, heat_number, sequence_in_round, start_type, status)
                values (9, 1, 1, 1, 2, 'STAGGER', 'PENDING')""");
        jdbc.update("delete from races where id = 9");
        jdbc.execute("PRAGMA foreign_keys = ON");

        migrate(null);

        assertThat(jdbc.queryForObject("select count(*) from race_entries where race_id = 7", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from result_snapshots where race_id = 7", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForMap("select start_type, abandoned_at, bump_slots from races where id = 7"))
                .containsEntry("start_type", "GRID").containsEntry("abandoned_at", 123).containsEntry("bump_slots", 2);
        assertThat(jdbc.queryForObject("select seq from sqlite_sequence where name = 'races'", Integer.class))
                .isEqualTo(9);
        assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);

        jdbc.update("update races set start_type = 'ROLLING' where id = 7");
        assertThatThrownBy(() -> jdbc.update("update races set start_type = 'FLYING' where id = 7"))
                .hasMessageContaining("races_start_type_check");
    }

    private void migrate(String target) {
        var config = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/sqlite");
        if (target != null) {
            config.target(target);
        }
        config.load().migrate();
    }
}
