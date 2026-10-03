package dev.monkeypatch.rctiming.localday.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A per-official, per-event-day local login credential. Row shape mirrors design decision 1 in
 * the U6 plan: login is picker + secret (an official picks their own row, then enters their own
 * secret), not username + password, so a raw-secret-only login never forces an O(n) BCrypt
 * compare-every-row scan.
 *
 * <p>{@code cloudUserId} is nullable — the mapping to a cloud-side user only exists once a later
 * unit (U10/U13) pulls the pre-cache that mints these rows server-side. This unit only builds
 * the local validate/session/lockout/recovery machinery; nothing here requires that mapping to
 * be populated.
 *
 * <p>Lockout state ({@code failedAttemptCount}, {@code lockedUntil}) lives directly on this
 * entity rather than a separate table (design decision 4) — simple, durable, no extra joins for
 * a per-credential check that happens on every login attempt.
 */
@Entity
@Table(name = "local_credentials")
public class LocalCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cloud_user_id")
    private Long cloudUserId;

    @Column(name = "official_name", nullable = false, length = 200)
    private String officialName;

    @Column(name = "secret_hash", nullable = false, length = 200)
    private String secretHash;

    @Column(nullable = false)
    private boolean recovery = false;

    @Column(name = "failed_attempt_count", nullable = false)
    private int failedAttemptCount = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCloudUserId() { return cloudUserId; }
    public void setCloudUserId(Long cloudUserId) { this.cloudUserId = cloudUserId; }

    public String getOfficialName() { return officialName; }
    public void setOfficialName(String officialName) { this.officialName = officialName; }

    public String getSecretHash() { return secretHash; }
    public void setSecretHash(String secretHash) { this.secretHash = secretHash; }

    public boolean isRecovery() { return recovery; }
    public void setRecovery(boolean recovery) { this.recovery = recovery; }

    public int getFailedAttemptCount() { return failedAttemptCount; }
    public void setFailedAttemptCount(int failedAttemptCount) { this.failedAttemptCount = failedAttemptCount; }

    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
