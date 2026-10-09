package dev.monkeypatch.rctiming.domain.user;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.auth.RefreshToken;
import dev.monkeypatch.rctiming.domain.auth.RefreshTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.monkeypatch.rctiming.persistence.RoundTrip.assertSavedAndReloaded;
import static org.assertj.core.api.Assertions.assertThat;

/** The official, refresh token and official audit log repositories save and load every field (#76). */
class OfficialRepositoriesIT extends AbstractIntegrationTest {

    private static final Instant T1 = Instant.parse("2026-10-05T10:15:30.123456Z");
    private static final Instant T2 = T1.plus(1, ChronoUnit.HOURS);

    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired OfficialAuditLogRepository auditLogs;

    private final List<Runnable> cleanup = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        cleanup.reversed().forEach(Runnable::run);
    }

    @Test
    void officialRoundTripWithRoles() {
        User u = official(Role.ADMIN, Role.REFEREE);
        u.setDisabledAt(T2);
        User saved = assertSavedAndReloaded(users, u, x -> {
            x.setEmail("renamed-" + System.nanoTime() + "@example.com");
            x.setPasswordHash("y");
            x.setFirstName("Renamed");
            x.setLastName("Person");
            x.setRoles(new HashSet<>(Set.of(Role.RACE_DIRECTOR)));
            x.setUpdatedAt(T2);
            x.setDisabledAt(null);
            return x;
        }, User::getId);
        cleanup.add(() -> users.deleteById(saved.getId()));

        assertThat(users.findByEmail(saved.getEmail())).get().extracting(User::getId).isEqualTo(saved.getId());
        assertThat(users.findByEmail("missing@example.com")).isEmpty();

        Instant created = saved.getCreatedAt();
        saved.setCreatedAt(created.plus(1, ChronoUnit.DAYS));
        users.save(saved);
        assertThat(users.findById(saved.getId()).orElseThrow().getCreatedAt())
                .as("creation time is set on insert only").isEqualTo(created);
    }

    @Test
    void countsOnlyEnabledOfficialsWithTheRole() {
        long before = users.countEnabledWithRole(Role.REFEREE);
        User enabled = saved(official(Role.REFEREE, Role.ADMIN));
        User disabled = official(Role.REFEREE);
        disabled.setDisabledAt(T1);
        saved(disabled);
        saved(official(Role.RACE_DIRECTOR));

        assertThat(users.countEnabledWithRole(Role.REFEREE)).isEqualTo(before + 1);

        enabled.setRoles(new HashSet<>(Set.of(Role.ADMIN)));
        users.save(enabled);
        assertThat(users.countEnabledWithRole(Role.REFEREE)).isEqualTo(before);
    }

    @Test
    void countsOfficialsOnce_disabledOnesIncluded_andNotAccountsWithNoRole() {
        long before = users.countOfficials();
        saved(official(Role.REFEREE, Role.ADMIN));
        User disabled = official(Role.RACE_DIRECTOR);
        disabled.setDisabledAt(T1);
        saved(disabled);
        saved(official());

        assertThat(users.countOfficials()).isEqualTo(before + 2);
    }

    @Test
    void refreshTokenRoundTripFindersAndRevoking() {
        User official = saved(official(Role.REFEREE));
        User other = saved(official(Role.REFEREE));

        RefreshToken saved = assertSavedAndReloaded(refreshTokens, token(official.getId(), T2), t -> {
            t.setUserId(other.getId());
            t.setTokenHash(hash());
            t.setExpiresAt(T2.plusSeconds(60));
            t.setRevoked(true);
            return t;
        }, RefreshToken::getId);

        assertThat(refreshTokens.findByTokenHash(saved.getTokenHash())).get()
                .extracting(RefreshToken::getId).isEqualTo(saved.getId());

        RefreshToken first = refreshTokens.save(token(official.getId(), T2));
        RefreshToken second = refreshTokens.save(token(official.getId(), T2));
        RefreshToken othersToken = refreshTokens.save(token(other.getId(), T2));
        assertThat(refreshTokens.findByUserIdAndRevokedFalse(official.getId()))
                .extracting(RefreshToken::getId).containsExactly(first.getId(), second.getId());

        assertThat(refreshTokens.revokeAllForUser(official.getId())).isEqualTo(2);
        assertThat(refreshTokens.findByUserIdAndRevokedFalse(official.getId())).isEmpty();
        assertThat(refreshTokens.findById(othersToken.getId()).orElseThrow().isRevoked())
                .as("another official's sessions are left alone").isFalse();
    }

    @Test
    void auditLogRoundTripAndFinder() {
        User official = saved(official(Role.REFEREE));
        User admin = saved(official(Role.ADMIN));

        OfficialAuditLog later = auditLogs.save(
                new OfficialAuditLog(official.getId(), null, OfficialAuditLog.Action.DISABLED, null, T2));
        OfficialAuditLog first = assertSavedAndReloaded(auditLogs,
                new OfficialAuditLog(official.getId(), admin.getId(), OfficialAuditLog.Action.ADDED, "Roles: REFEREE",
                        T1),
                a -> new OfficialAuditLog(a.getId(), admin.getId(), null, "PASSWORD_SET", null, T1),
                OfficialAuditLog::getId);

        assertThat(auditLogs.findByOfficialUserIdOrderByCreatedAtAsc(official.getId()))
                .extracting(OfficialAuditLog::getId).containsExactly(later.getId());
        assertThat(auditLogs.findByOfficialUserIdOrderByCreatedAtAsc(admin.getId()))
                .extracting(OfficialAuditLog::getId).containsExactly(first.getId());
        cleanup.add(() -> auditLogs.deleteById(first.getId()));
        cleanup.add(() -> auditLogs.deleteById(later.getId()));
    }

    private User saved(User user) {
        User saved = users.save(user);
        cleanup.add(() -> users.deleteById(saved.getId()));
        return saved;
    }

    private static User official(Role... roles) {
        User u = new User();
        u.setEmail("official-" + System.nanoTime() + "@example.com");
        u.setPasswordHash("x");
        u.setFirstName("Round");
        u.setLastName("Trip");
        u.setRoles(new HashSet<>(Set.of(roles)));
        u.setCreatedAt(T1);
        u.setUpdatedAt(T1);
        return u;
    }

    private static RefreshToken token(Long userId, Instant expiresAt) {
        RefreshToken t = new RefreshToken();
        t.setUserId(userId);
        t.setTokenHash(hash());
        t.setExpiresAt(expiresAt);
        t.setCreatedAt(T1);
        t.setRevoked(false);
        return t;
    }

    private static String hash() {
        return String.format("%064x", System.nanoTime());
    }
}
