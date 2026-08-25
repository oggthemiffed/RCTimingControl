package dev.monkeypatch.rctiming.domain.localday;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * The most recently accepted snapshot payload for an event (R11/R15), stored verbatim as JSONB —
 * one row per event, upserted on every accepted push by {@code SnapshotIngestService}. The cloud
 * does not parse this payload's internal shape in this unit; it is captured and republished to
 * the public page as-is, not fed into the cloud's own race-control/standings engine.
 */
@Entity
@Table(name = "event_snapshot_state")
public class EventSnapshotState {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
