package dev.monkeypatch.rctiming.domain.race;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "marshal_penalties")
public class MarshalPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "absence_id")
    private Long absenceId;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "applied_by", nullable = false)
    private Long appliedBy;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(columnDefinition = "text")
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAbsenceId() { return absenceId; }
    public void setAbsenceId(Long absenceId) { this.absenceId = absenceId; }

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getAppliedBy() { return appliedBy; }
    public void setAppliedBy(Long appliedBy) { this.appliedBy = appliedBy; }

    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
