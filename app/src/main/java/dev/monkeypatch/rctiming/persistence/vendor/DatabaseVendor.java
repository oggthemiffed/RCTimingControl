package dev.monkeypatch.rctiming.persistence.vendor;

import org.jooq.SQLDialect;

/**
 * The databases the app can run on. Everything that differs between them is listed here and
 * applied by {@code DatabaseConfig}, so moving to another database means a new constant, a
 * migrations folder and a config change, not edits across the codebase.
 */
public enum DatabaseVendor {

    POSTGRESQL("postgresql", SQLDialect.POSTGRES, "org.hibernate.dialect.PostgreSQLDialect");

    private final String migrationFolder;
    private final SQLDialect jooqDialect;
    private final String hibernateDialect;

    DatabaseVendor(String migrationFolder, SQLDialect jooqDialect, String hibernateDialect) {
        this.migrationFolder = migrationFolder;
        this.jooqDialect = jooqDialect;
        this.hibernateDialect = hibernateDialect;
    }

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
}
