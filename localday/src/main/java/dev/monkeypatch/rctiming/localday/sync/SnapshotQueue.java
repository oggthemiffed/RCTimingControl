package dev.monkeypatch.rctiming.localday.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Singleton row (always {@code id = 1}) tracking this venue instance's durable outbound
 * snapshot-sync cursor (U11) — the high-water mark of laps already synced, plus the in-flight
 * (not-yet-acknowledged) snapshot's identity, if any. See the V9 migration's comment for the
 * retry/idempotency reasoning behind {@link #pendingSnapshotId}/{@link #pendingLapIdCeiling}.
 */
@Entity
@Table(name = "snapshot_queue")
public class SnapshotQueue {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "last_synced_lap_id")
    private Long lastSyncedLapId;

    @Column(name = "pending_snapshot_id", length = 64)
    private String pendingSnapshotId;

    @Column(name = "pending_lap_id_ceiling")
    private Long pendingLapIdCeiling;

    @Column(name = "last_push_attempt_at")
    private Instant lastPushAttemptAt;

    @Column(name = "last_push_success_at")
    private Instant lastPushSuccessAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLastSyncedLapId() { return lastSyncedLapId; }
    public void setLastSyncedLapId(Long lastSyncedLapId) { this.lastSyncedLapId = lastSyncedLapId; }

    public String getPendingSnapshotId() { return pendingSnapshotId; }
    public void setPendingSnapshotId(String pendingSnapshotId) { this.pendingSnapshotId = pendingSnapshotId; }

    public Long getPendingLapIdCeiling() { return pendingLapIdCeiling; }
    public void setPendingLapIdCeiling(Long pendingLapIdCeiling) { this.pendingLapIdCeiling = pendingLapIdCeiling; }

    public Instant getLastPushAttemptAt() { return lastPushAttemptAt; }
    public void setLastPushAttemptAt(Instant lastPushAttemptAt) { this.lastPushAttemptAt = lastPushAttemptAt; }

    public Instant getLastPushSuccessAt() { return lastPushSuccessAt; }
    public void setLastPushSuccessAt(Instant lastPushSuccessAt) { this.lastPushSuccessAt = lastPushSuccessAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
