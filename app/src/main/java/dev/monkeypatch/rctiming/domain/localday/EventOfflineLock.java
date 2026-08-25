package dev.monkeypatch.rctiming.domain.localday;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "event_offline_locks")
public class EventOfflineLock {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    // R17: set once by a device-loss declaration (DeviceLossService) and never cleared.
    @Column(name = "incomplete_data", nullable = false)
    private boolean incompleteData = false;

    @Column(name = "incomplete_data_declared_at")
    private Instant incompleteDataDeclaredAt;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }

    public Instant getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(Instant unlockedAt) { this.unlockedAt = unlockedAt; }

    public boolean isIncompleteData() { return incompleteData; }
    public void setIncompleteData(boolean incompleteData) { this.incompleteData = incompleteData; }

    public Instant getIncompleteDataDeclaredAt() { return incompleteDataDeclaredAt; }
    public void setIncompleteDataDeclaredAt(Instant incompleteDataDeclaredAt) { this.incompleteDataDeclaredAt = incompleteDataDeclaredAt; }
}
