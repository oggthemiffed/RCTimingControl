package dev.monkeypatch.rctiming.domain.auth;

import dev.monkeypatch.rctiming.persistence.CreatedAt;
import java.time.Instant;

public class RefreshToken implements CreatedAt {

    private Long id;

    private Long userId;

    private String tokenHash;

    private Instant expiresAt;

    private Instant createdAt;

    private boolean revoked = false;

    /** Ties together the tokens one sign-in has been rotated through; null for a token from before families. */
    private String familyId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
}
