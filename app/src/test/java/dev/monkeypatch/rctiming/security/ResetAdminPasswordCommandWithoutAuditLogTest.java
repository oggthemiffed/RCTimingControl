package dev.monkeypatch.rctiming.security;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The command is the way back in for a locked-out club, so it must work on a database the app has not
 * migrated yet, such as one restored from an old backup, where the audit log table does not exist.
 */
class ResetAdminPasswordCommandWithoutAuditLogTest {

    @Test
    void theResetStillWorksWhenTheAuditLogTableIsMissing() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            DSLContext dsl = DSL.using(connection, SQLDialect.SQLITE);
            dsl.execute("create table users (id integer primary key autoincrement, email text not null,"
                    + " password_hash text not null, first_name text not null, last_name text not null,"
                    + " disabled_at bigint, updated_at bigint)");
            dsl.execute("create table user_roles (user_id bigint not null, role text not null)");
            dsl.execute("create table refresh_tokens (user_id bigint not null, revoked boolean not null)");
            dsl.execute("create table official_audit_log (id integer primary key autoincrement,"
                    + " official_user_id bigint not null, action text not null, detail text,"
                    + " actor_user_id bigint, created_at bigint not null)");
            dsl.execute("insert into users (email, password_hash, first_name, last_name)"
                    + " values ('locked@example.com', 'old-hash', 'Lou', 'Locked')");

            ResetAdminPasswordCommand.Outcome outcome = ResetAdminPasswordCommand.reset(
                    dsl, "locked@example.com", "newPassword1", Clock.systemUTC());

            assertThat(outcome.madeAdmin()).isTrue();
            assertThat(dsl.fetchValue("select password_hash from users where email = 'locked@example.com'"))
                    .isNotEqualTo("old-hash");
            assertThat(dsl.fetchCount(DSL.table("user_roles"), DSL.field("role").eq("ADMIN"))).isEqualTo(1);
            assertThat(dsl.fetchCount(DSL.table("official_audit_log"), DSL.field("action").eq("PASSWORD_SET")))
                    .isEqualTo(1);
        }
    }
}
