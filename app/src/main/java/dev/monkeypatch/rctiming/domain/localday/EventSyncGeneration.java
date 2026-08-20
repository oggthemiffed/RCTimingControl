package dev.monkeypatch.rctiming.domain.localday;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_sync_generations")
public class EventSyncGeneration {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false)
    private long generation;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public long getGeneration() { return generation; }
    public void setGeneration(long generation) { this.generation = generation; }
}
