package dev.monkeypatch.rctiming.domain.race;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "race_id", nullable = false)
    private Long raceId;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "incident_type", nullable = false, length = 50)
    private String incidentType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "raised_by", nullable = false)
    private Long raisedBy;

    @Column(name = "raised_at", nullable = false)
    private Instant raisedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRaceId() { return raceId; }
    public void setRaceId(Long raceId) { this.raceId = raceId; }

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }

    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getRaisedBy() { return raisedBy; }
    public void setRaisedBy(Long raisedBy) { this.raisedBy = raisedBy; }

    public Instant getRaisedAt() { return raisedAt; }
    public void setRaisedAt(Instant raisedAt) { this.raisedAt = raisedAt; }
}
