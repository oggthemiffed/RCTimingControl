package dev.monkeypatch.rctiming.domain.localday;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "localday_instance_secrets")
public class LocaldayInstanceSecret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "instance_id", nullable = false, length = 64)
    private String instanceId;

    @Column(name = "secret_hash", nullable = false, length = 255)
    private String secretHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public String getSecretHash() { return secretHash; }
    public void setSecretHash(String secretHash) { this.secretHash = secretHash; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public Instant getInvalidatedAt() { return invalidatedAt; }
    public void setInvalidatedAt(Instant invalidatedAt) { this.invalidatedAt = invalidatedAt; }
}
