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

    /** The RaceHub event the imported entries came from (#27), so results can be sent back. Null if never imported. */
    @Column(name = "racehub_event_id", length = 100)
    private String racehubEventId;

    /** The last revision given to this event's results export (#27); each new export takes the next one. */
    @Column(name = "results_export_revision", nullable = false)
    private long resultsExportRevision;

    /**
     * Why the event's results need exporting, set alongside the change that calls for it and cleared when the
     * export is queued (#27): RACE_FINISHED, CORRECTION or DAY_CLOSE, or null when nothing is waiting.
     */
    @Column(name = "results_export_pending", length = 30)
    private String resultsExportPending;

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

    public String getRacehubEventId() { return racehubEventId; }
    public void setRacehubEventId(String racehubEventId) { this.racehubEventId = racehubEventId; }

    public long getResultsExportRevision() { return resultsExportRevision; }

    /** Takes the next results export revision. Load the event with {@code findByIdForUpdate} first. */
    public long nextResultsExportRevision() { return ++resultsExportRevision; }

    public String getResultsExportPending() { return resultsExportPending; }
    public void setResultsExportPending(String resultsExportPending) { this.resultsExportPending = resultsExportPending; }
}
