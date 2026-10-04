package dev.monkeypatch.rctiming.persistence;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.DSL;

/** Calls {@link PersistencePortabilityTest} must recognise as plain SQL, and one it must not. */
@SuppressWarnings("unused")
final class PlainSqlExamples {

    private PlainSqlExamples() {
    }

    static Result<Record> fromVariable(DSLContext dsl, String sqlText) {
        return dsl.fetch(sqlText);
    }

    static Object acrossLines(DSLContext dsl, Table<?> races) {
        return dsl.selectFrom(races)
                .where(
                        "status = 'RUNNING'")
                .fetch();
    }

    static Object plainField(DSLContext dsl) {
        String expression = "now()";
        return dsl.select(DSL.field(expression)).fetch();
    }

    static Object dslOnly(DSLContext dsl, Table<?> races) {
        return dsl.selectFrom(races).where(DSL.trueCondition()).fetch();
    }
}
