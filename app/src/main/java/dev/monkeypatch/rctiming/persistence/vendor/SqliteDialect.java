package dev.monkeypatch.rctiming.persistence.vendor;

import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;

import java.sql.Types;

/**
 * Hibernate's SQLite dialect, adjusted to how SQLite behaves:
 * <ul>
 *   <li>SQLite keeps every whole number as a 64-bit integer, so schema validation accepts an
 *       {@code INTEGER PRIMARY KEY} (the rowid) as the home of a {@code Long} id.</li>
 *   <li>A failed unique, foreign key, check or not-null constraint becomes Hibernate's
 *       {@link ConstraintViolationException}, which Spring reports as a data integrity
 *       violation, as it did on PostgreSQL.</li>
 * </ul>
 */
public class SqliteDialect extends SQLiteDialect {

    /** SQLite's primary result code for any failed constraint. */
    private static final int SQLITE_CONSTRAINT = 19;

    @Override
    public boolean equivalentTypes(int typeCode1, int typeCode2) {
        return super.equivalentTypes(typeCode1, typeCode2)
                || isWholeNumber(typeCode1) && isWholeNumber(typeCode2);
    }

    @Override
    public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
        SQLExceptionConversionDelegate delegate = super.buildSQLExceptionConversionDelegate();
        return (sqlException, message, sql) -> {
            if (JdbcExceptionHelper.extractErrorCode(sqlException) == SQLITE_CONSTRAINT) {
                String constraint = getViolatedConstraintNameExtractor().extractConstraintName(sqlException);
                return new ConstraintViolationException(message, sqlException, sql, constraint);
            }
            return delegate == null ? null : delegate.convert(sqlException, message, sql);
        };
    }

    private static boolean isWholeNumber(int typeCode) {
        return typeCode == Types.TINYINT || typeCode == Types.SMALLINT
                || typeCode == Types.INTEGER || typeCode == Types.BIGINT;
    }
}
