package dev.monkeypatch.rctiming.persistence.vendor;

import org.jooq.SQLDialect;
import org.sqlite.SQLiteConfig;

import java.nio.file.Path;
import java.util.Properties;

/**
 * The databases the app can run on. Everything that differs between them is listed here and
 * applied by {@code DatabaseConfig}, so moving to another database means a new constant, a
 * migrations folder and a config change, not edits across the codebase.
 */
public enum DatabaseVendor {

    /**
     * One SQLite file inside the app process (#26). WAL lets readers carry on while a write
     * commits; a single writer connection means writes never contend for the lock.
     */
    SQLITE("sqlite", SQLDialect.SQLITE, SqliteDialect.class.getName(), 1) {
        @Override
        public String jdbcUrl(Path dataDirectory) {
            return "jdbc:sqlite:" + dataDirectory.resolve("rctiming.db");
        }

        @Override
        public Properties connectionProperties() {
            SQLiteConfig config = new SQLiteConfig();
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);
            config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
            config.enforceForeignKeys(true);
            config.setBusyTimeout(5000);
            return config.toProperties();
        }

        @Override
        public String readOnlySessionSql() {
            return "PRAGMA query_only = 1";
        }
    };

    private final String migrationFolder;
    private final SQLDialect jooqDialect;
    private final String hibernateDialect;
    private final int maxWriteConnections;

    DatabaseVendor(String migrationFolder, SQLDialect jooqDialect, String hibernateDialect, int maxWriteConnections) {
        this.migrationFolder = migrationFolder;
        this.jooqDialect = jooqDialect;
        this.hibernateDialect = hibernateDialect;
        this.maxWriteConnections = maxWriteConnections;
    }

    /** JDBC URL of the app's database inside the data directory. */
    public abstract String jdbcUrl(Path dataDirectory);

    /** Settings applied to every connection. */
    public abstract Properties connectionProperties();

    /** Statement that makes a connection refuse writes; run on each read-pool connection. */
    public abstract String readOnlySessionSql();

    /** Sub-folder holding this vendor's Flyway scripts under each migration location. */
    public String migrationFolder() {
        return migrationFolder;
    }

    public SQLDialect jooqDialect() {
        return jooqDialect;
    }

    /** Hibernate dialect class name. */
    public String hibernateDialect() {
        return hibernateDialect;
    }

    /** Size of the pool JPA writes through. */
    public int maxWriteConnections() {
        return maxWriteConnections;
    }
}
