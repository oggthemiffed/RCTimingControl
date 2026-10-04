package dev.monkeypatch.rctiming;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Empties tables for tests the way PostgreSQL's {@code TRUNCATE ... CASCADE} did: the named
 * tables and every table that references them, directly or through others.
 */
public final class TestTables {

    private TestTables() {
    }

    public static void truncateCascade(JdbcTemplate jdbc, TransactionTemplate tx, String... tables) {
        Set<String> toEmpty = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>(List.of(tables));
        List<String> allTables = jdbc.queryForList(
                "select name from sqlite_master where type = 'table' and name not like 'sqlite_%'"
                        + " and name <> 'flyway_schema_history'", String.class);
        while (!pending.isEmpty()) {
            String table = pending.pop();
            if (!toEmpty.add(table)) {
                continue;
            }
            for (String candidate : allTables) {
                List<String> referenced = jdbc.queryForList(
                        "select \"table\" from pragma_foreign_key_list(?)", String.class, candidate);
                if (referenced.contains(table)) {
                    pending.push(candidate);
                }
            }
        }
        tx.executeWithoutResult(status -> {
            // Foreign keys are checked at commit, by which time every referencing row is gone too
            jdbc.execute("PRAGMA defer_foreign_keys = ON");
            toEmpty.forEach(table -> jdbc.execute("delete from " + table));
        });
    }
}
