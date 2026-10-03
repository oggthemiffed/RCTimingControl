package dev.monkeypatch.rctiming.localday.daylifecycle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Singleton row (always {@code id = 1}) tracking this venue instance's pre-cache/open/close
 * status for the event day (U10). There is exactly one row for the lifetime of this venue
 * laptop's database — see {@link DayLifecycleService#getOrCreateState()} for how it is lazily
 * created on first use, minting a stable {@link #instanceId} exactly once.
 */
@Entity
@Table(name = "day_lifecycle_state")
public class DayLifecycleState {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "instance_id", nullable = false, length = 64)
    private String instanceId;

    @Column(name = "cloud_event_id")
    private Long cloudEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayLifecycleStatus status = DayLifecycleStatus.NOT_SET_UP;

    @Column
    private Long generation;

    // Plaintext at rest — see the V8 migration's comment; this is the deliberate design (R14
    // relies on OS-level full-disk encryption on the venue machine, not column encryption).
    @Column(name = "instance_secret", length = 500)
    private String instanceSecret;

    @Column(name = "pending_sync_count", nullable = false)
    private int pendingSyncCount = 0;

    @Column(name = "split_brain_warning", nullable = false)
    private boolean splitBrainWarning = false;

    // Set once this instance's snapshot push is rejected with a 409 Superseded (U12,
    // DeviceLossHandler) — a replacement instance has since opened at a higher generation, per a
    // device-loss declaration (R16/R17). Permanent for the rest of this local session; see the
    // V10 migration's comment.
    @Column(name = "superseded", nullable = false)
    private boolean superseded = false;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "last_pre_cached_at")
    private Instant lastPreCachedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public Long getCloudEventId() { return cloudEventId; }
    public void setCloudEventId(Long cloudEventId) { this.cloudEventId = cloudEventId; }

    public DayLifecycleStatus getStatus() { return status; }
    public void setStatus(DayLifecycleStatus status) { this.status = status; }

    public Long getGeneration() { return generation; }
    public void setGeneration(Long generation) { this.generation = generation; }

    public String getInstanceSecret() { return instanceSecret; }
    public void setInstanceSecret(String instanceSecret) { this.instanceSecret = instanceSecret; }

    public int getPendingSyncCount() { return pendingSyncCount; }
    public void setPendingSyncCount(int pendingSyncCount) { this.pendingSyncCount = pendingSyncCount; }

    public boolean isSplitBrainWarning() { return splitBrainWarning; }
    public void setSplitBrainWarning(boolean splitBrainWarning) { this.splitBrainWarning = splitBrainWarning; }

    public boolean isSuperseded() { return superseded; }
    public void setSuperseded(boolean superseded) { this.superseded = superseded; }

    public Instant getSupersededAt() { return supersededAt; }
    public void setSupersededAt(Instant supersededAt) { this.supersededAt = supersededAt; }

    public Instant getLastPreCachedAt() { return lastPreCachedAt; }
    public void setLastPreCachedAt(Instant lastPreCachedAt) { this.lastPreCachedAt = lastPreCachedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
