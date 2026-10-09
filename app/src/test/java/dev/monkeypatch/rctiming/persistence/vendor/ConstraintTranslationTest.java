package dev.monkeypatch.rctiming.persistence.vendor;

import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintTranslationTest {

    private final SQLExceptionTranslator translator = DatabaseVendor.SQLITE.exceptionTranslator();

    @Test
    void aTakenUniqueValueIsADuplicate() {
        assertThat(translate(SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE)).isInstanceOf(DuplicateKeyException.class);
        assertThat(translate(SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void aRowStillInUseIsAnotherIntegrityFailure() {
        assertThat(translate(SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY))
                .isExactlyInstanceOf(DataIntegrityViolationException.class);
        assertThat(translate(SQLiteErrorCode.SQLITE_CONSTRAINT_NOTNULL))
                .isExactlyInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void otherErrorsAreLeftToSpring() {
        assertThat(translator.translate("insert", null, new SQLException("disk I/O error", "HY000", 10))).isNull();
    }

    private Object translate(SQLiteErrorCode code) {
        return translator.translate("insert", null, new SQLiteException(code.message, code));
    }
}
