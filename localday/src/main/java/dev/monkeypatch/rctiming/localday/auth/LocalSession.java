package dev.monkeypatch.rctiming.localday.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An issued local login session for an official, distinct from the cloud's stateless-JWT
 * session model (KTD5). {@code sessionToken} is a plain, high-entropy random string generated
 * via {@link java.security.SecureRandom} — NOT a JWT, and NOT hashed at rest (design decision
 * 2). Hashing it would mean paying BCrypt's deliberate ~100ms+ cost on every single
 * authenticated HTTP request, which is wrong for a per-request check; the token's own entropy
 * (32 random bytes, Base64URL-encoded, ~256 bits) is what makes it unguessable, the same way a
 * Redis-backed session store keeps raw tokens.
 *
 * <p><b>Expiry anchor (design decision 3):</b> {@code issuedAt} is recorded using THIS local
 * device's own {@code Instant.now()} at the moment of successful login. Validity is later
 * checked by comparing that same value against THIS local device's {@code Instant.now()} again
 * ({@code Duration.between(issuedAt, now)} — see {@link LocalSessionService#validateSession}).
 * That is a single-clock comparison, not a cross-device one. The risk KTD5 actually warns about
 * is comparing two DIFFERENT clocks — e.g. a cloud minting timestamp against a venue laptop's
 * possibly-very-out-of-sync wall clock — which this design never does, because minting doesn't
 * happen locally in this unit at all. A pure in-JVM {@code System.nanoTime()} anchor was
 * considered and rejected: it resets on every JVM restart and cannot be persisted meaningfully
 * across one, which would break session durability across a {@code :localday} process restart —
 * a real robustness requirement for an ~18-hour race day where the process may be restarted.
 * Ordinary persisted wall-clock arithmetic, anchored and re-read on the same device, has neither
 * problem.
 */
@Entity
@Table(name = "local_sessions")
public class LocalSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "credential_id", nullable = false)
    private Long credentialId;

    // Denormalized snapshot of the official's name at login time, avoiding a join back to
    // local_credentials on every authenticated request.
    @Column(name = "official_name", nullable = false, length = 200)
    private String officialName;

    @Column(name = "session_token", nullable = false, unique = true, length = 200)
    private String sessionToken;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCredentialId() { return credentialId; }
    public void setCredentialId(Long credentialId) { this.credentialId = credentialId; }

    public String getOfficialName() { return officialName; }
    public void setOfficialName(String officialName) { this.officialName = officialName; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
}
