package dev.monkeypatch.rctiming.domain.localday;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One row per accepted snapshot push (KTD4) — exists solely to answer "have we already processed
 * this exact snapshotId for this event" idempotently, via the unique {@code (event_id,
 * snapshot_id)} index (V28). Rejected (superseded) attempts are never recorded here: their
 * outcome is deterministic and stable for a given generation, so there is nothing to remember.
 */
@Entity
@Table(name = "localday_snapshots")
public class LocaldaySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "instance_id", nullable = false, length = 64)
    private String instanceId;

    @Column(name = "snapshot_id", nullable = false, length = 64)
    private String snapshotId;

    @Column(nullable = false)
    private long generation;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public String getSnapshotId() { return snapshotId; }
    public void setSnapshotId(String snapshotId) { this.snapshotId = snapshotId; }

    public long getGeneration() { return generation; }
    public void setGeneration(long generation) { this.generation = generation; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
