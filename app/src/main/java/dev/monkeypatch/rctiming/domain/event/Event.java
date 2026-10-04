package dev.monkeypatch.rctiming.domain.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "entry_opens_at")
    private Instant entryOpensAt;

    @Column(name = "entry_closes_at")
    private Instant entryClosesAt;

    @Column(name = "track_id")
    private Long trackId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** When entries were last imported from RaceHub (L8). Null if never. */
    @Column(name = "racehub_last_import_at")
    private Instant racehubLastImportAt;

    /** The RaceHub export revision of that import. */
    @Column(name = "racehub_last_revision")
    private Long racehubLastRevision;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public Instant getEntryOpensAt() { return entryOpensAt; }
    public void setEntryOpensAt(Instant entryOpensAt) { this.entryOpensAt = entryOpensAt; }

    public Instant getEntryClosesAt() { return entryClosesAt; }
    public void setEntryClosesAt(Instant entryClosesAt) { this.entryClosesAt = entryClosesAt; }

    public Long getTrackId() { return trackId; }
    public void setTrackId(Long trackId) { this.trackId = trackId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getRacehubLastImportAt() { return racehubLastImportAt; }
    public void setRacehubLastImportAt(Instant racehubLastImportAt) { this.racehubLastImportAt = racehubLastImportAt; }

    public Long getRacehubLastRevision() { return racehubLastRevision; }
    public void setRacehubLastRevision(Long racehubLastRevision) { this.racehubLastRevision = racehubLastRevision; }
}
