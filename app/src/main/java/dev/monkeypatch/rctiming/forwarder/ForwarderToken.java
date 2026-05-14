package dev.monkeypatch.rctiming.forwarder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "forwarder_token")
public class ForwarderToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "token_value")
    private String tokenValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ForwarderTokenStatus status;

    @Column(name = "generated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant generatedAt;

    @Column(name = "revoked_at", columnDefinition = "TIMESTAMPTZ")
    private Instant revokedAt;

    protected ForwarderToken() {}

    public Long getId() { return id; }

    public String getTokenHash() { return tokenHash; }
    void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getTokenValue() { return tokenValue; }
    void setTokenValue(String tokenValue) { this.tokenValue = tokenValue; }

    public ForwarderTokenStatus getStatus() { return status; }
    void setStatus(ForwarderTokenStatus status) { this.status = status; }

    public Instant getGeneratedAt() { return generatedAt; }
    void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public Instant getRevokedAt() { return revokedAt; }
    void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}
