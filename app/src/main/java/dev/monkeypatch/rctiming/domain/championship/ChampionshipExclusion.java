package dev.monkeypatch.rctiming.domain.championship;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "championship_exclusions")
public class ChampionshipExclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "championship_id", nullable = false)
    private Long championshipId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ChampionshipExclusion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChampionshipId() { return championshipId; }
    public void setChampionshipId(Long v) { this.championshipId = v; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long v) { this.driverId = v; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long v) { this.eventId = v; }

    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long v) { this.createdBy = v; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant t) { this.createdAt = t; }
}
