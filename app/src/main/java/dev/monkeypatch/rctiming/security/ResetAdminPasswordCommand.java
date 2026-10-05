package dev.monkeypatch.rctiming.security;

import dev.monkeypatch.rctiming.domain.user.OfficialAuditLog;
import dev.monkeypatch.rctiming.domain.user.OfficialService;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.persistence.DatabaseProperties;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.impl.DSL;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.Tables.OFFICIAL_AUDIT_LOG;
import static dev.monkeypatch.rctiming.jooq.generated.Tables.REFRESH_TOKENS;
import static dev.monkeypatch.rctiming.jooq.generated.Tables.USERS;
import static dev.monkeypatch.rctiming.jooq.generated.Tables.USER_ROLES;

/**
 * {@code RCTimingControl reset-admin-password <email>} (#61): the way back in for a club whose
 * only admin is locked out. It needs local access to the data folder, not the app, so it works
 * when nobody can sign in. It sets a new password for the official with that email, makes sure
 * they are an enabled admin, signs out their other sessions and records the change in the
 * officials' audit log as coming from the command line. Run without an email, it lists the
 * admins.
 * <p>
 * The data directory is found the way the app finds it, as for {@code restore}.
 */
public final class ResetAdminPasswordCommand {

    static final String USAGE =
            "Usage: RCTimingControl reset-admin-password <email> [--rctiming.database.data-directory=<folder>]";

    /** Where the new password comes from: the console, or a line of standard input when there is none. */
    @FunctionalInterface
    interface PasswordSource {
        char[] read(String prompt) throws IOException;
    }

    /** What the reset changed, besides the password. */
    record Outcome(boolean madeAdmin, boolean reEnabled) {}

    private ResetAdminPasswordCommand() {
    }

    /** Binds only the database settings; nothing else of the app starts. */
    @EnableConfigurationProperties(DatabaseProperties.class)
    static class Settings {
    }

    /** Returns the process exit code. */
    public static int run(String[] args) {
        return run(args, ResetAdminPasswordCommand::readFromTerminal);
    }

    static int run(String[] args, PasswordSource passwords) {
        String email = args.length > 0 && !args[0].startsWith("--") ? args[0].trim() : null;
        String[] settings = email == null ? args : Arrays.copyOfRange(args, 1, args.length);
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Settings.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .run(settings)) {
            DatabaseProperties database = context.getBean(DatabaseProperties.class);
            Path dataDirectory = database.effectiveDataDirectory().toAbsolutePath();
            if (!database.vendor().hasDatabase(dataDirectory)) {
                System.err.println("No RCTimingControl database in " + dataDirectory
                        + ". Run this on the laptop the app is installed on, as an administrator (or with sudo).");
                return 1;
            }
            try (Connection connection = DriverManager.getConnection(
                    database.vendor().jdbcUrl(dataDirectory), database.vendor().connectionProperties())) {
                DSLContext dsl = DSL.using(connection, database.vendor().jooqDialect());
                List<String> admins = admins(dsl);
                if (email == null) {
                    System.err.println(USAGE);
                    System.err.println(admins.isEmpty() ? "There are no admins yet." : "Admins: " + String.join(", ", admins));
                    return 2;
                }
                if (!exists(dsl, email)) {
                    System.err.println("No official with the email " + email + ".");
                    System.err.println(admins.isEmpty() ? "There are no admins yet." : "Admins: " + String.join(", ", admins));
                    return 1;
                }
                String password = readNewPassword(passwords);
                if (password == null) {
                    return 1;
                }
                Outcome outcome = reset(dsl, email, password, Clock.systemUTC());
                System.out.println("Set a new password for " + email + "."
                        + (outcome.madeAdmin() ? " They are now an admin." : "")
                        + (outcome.reEnabled() ? " Their account is enabled again." : "")
                        + " They can sign in now.");
                return 0;
            }
        } catch (Exception e) {
            System.err.println("Reset failed: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Sets the password, makes the official an enabled admin, revokes their refresh tokens and
     * logs each change with no actor (the command line). One transaction.
     */
    static Outcome reset(DSLContext dsl, String email, String password, Clock clock) {
        String hash = new BCryptPasswordEncoder().encode(password);
        return dsl.transactionResult(configuration -> {
            DSLContext tx = configuration.dsl();
            Record3<Long, Instant, Instant> user = tx.select(USERS.ID, USERS.DISABLED_AT, USERS.CREATED_AT)
                    .from(USERS)
                    .where(USERS.EMAIL.eq(email))
                    .fetchSingle();
            long userId = user.value1();
            Instant now = clock.instant();
            boolean reEnabled = user.value2() != null;
            boolean madeAdmin = !tx.fetchExists(USER_ROLES,
                    USER_ROLES.USER_ID.eq(userId).and(USER_ROLES.ROLE.eq(Role.ADMIN.name())));

            tx.update(USERS)
                    .set(USERS.PASSWORD_HASH, hash)
                    .set(USERS.DISABLED_AT, (Instant) null)
                    .set(USERS.UPDATED_AT, now)
                    .where(USERS.ID.eq(userId))
                    .execute();
            if (madeAdmin) {
                tx.insertInto(USER_ROLES, USER_ROLES.USER_ID, USER_ROLES.ROLE)
                        .values(userId, Role.ADMIN.name())
                        .execute();
            }
            tx.update(REFRESH_TOKENS)
                    .set(REFRESH_TOKENS.REVOKED, true)
                    .where(REFRESH_TOKENS.USER_ID.eq(userId).and(REFRESH_TOKENS.REVOKED.isFalse()))
                    .execute();

            log(tx, userId, OfficialAuditLog.Action.PASSWORD_SET, "From the command line", now);
            if (madeAdmin) {
                log(tx, userId, OfficialAuditLog.Action.ROLES_CHANGED, "ADMIN added from the command line", now);
            }
            if (reEnabled) {
                log(tx, userId, OfficialAuditLog.Action.ENABLED, "From the command line", now);
            }
            return new Outcome(madeAdmin, reEnabled);
        });
    }

    /** Emails of the admins, enabled or not, for when the email given doesn't match. */
    static List<String> admins(DSLContext dsl) {
        return dsl.select(USERS.EMAIL)
                .from(USERS)
                .join(USER_ROLES).on(USER_ROLES.USER_ID.eq(USERS.ID))
                .where(USER_ROLES.ROLE.eq(Role.ADMIN.name()))
                .orderBy(USERS.EMAIL.asc())
                .fetch(USERS.EMAIL);
    }

    private static boolean exists(DSLContext dsl, String email) {
        return dsl.fetchExists(USERS, USERS.EMAIL.eq(email));
    }

    private static void log(DSLContext tx, long userId, OfficialAuditLog.Action action, String detail, Instant at) {
        tx.insertInto(OFFICIAL_AUDIT_LOG, OFFICIAL_AUDIT_LOG.OFFICIAL_USER_ID, OFFICIAL_AUDIT_LOG.ACTION,
                        OFFICIAL_AUDIT_LOG.DETAIL, OFFICIAL_AUDIT_LOG.CREATED_AT)
                .values(userId, action.name(), detail, at)
                .execute();
    }

    private static String readNewPassword(PasswordSource passwords) throws IOException {
        char[] first = passwords.read("New password: ");
        char[] second = passwords.read("Type it again: ");
        if (first == null || second == null) {
            System.err.println("No password given.");
            return null;
        }
        if (!Arrays.equals(first, second)) {
            System.err.println("The passwords don't match.");
            return null;
        }
        String password = new String(first);
        Arrays.fill(first, ' ');
        Arrays.fill(second, ' ');
        if (password.length() < OfficialService.MIN_PASSWORD_LENGTH) {
            System.err.println("The password must be at least " + OfficialService.MIN_PASSWORD_LENGTH + " characters.");
            return null;
        }
        return password;
    }

    private static BufferedReader stdin;

    /** Hidden typing at a terminal; one line each from standard input when piped. */
    private static char[] readFromTerminal(String prompt) throws IOException {
        Console console = System.console();
        if (console != null) {
            return console.readPassword(prompt);
        }
        if (stdin == null) {
            stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        }
        String line = stdin.readLine();
        return line == null ? null : line.toCharArray();
    }
}
