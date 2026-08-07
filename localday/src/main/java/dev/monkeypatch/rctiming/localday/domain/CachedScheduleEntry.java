package dev.monkeypatch.rctiming.localday.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Local cache of one race/heat from the event day's schedule, pulled from the cloud before
 * going offline. {@link #sequence} drives run order for the local race-control UI.
 */
@Entity
@Table(name = "cached_schedule")
public class CachedScheduleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cloud_race_id", nullable = false)
    private Long cloudRaceId;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "heat_number", nullable = false)
    private int heatNumber;

    @Column(nullable = false)
    private int sequence;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "final_letter", length = 5)
    private String finalLetter;

    @Column(name = "scheduled_start_at")
    private Instant scheduledStartAt;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCloudRaceId() { return cloudRaceId; }
    public void setCloudRaceId(Long cloudRaceId) { this.cloudRaceId = cloudRaceId; }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }

    public int getHeatNumber() { return heatNumber; }
    public void setHeatNumber(int heatNumber) { this.heatNumber = heatNumber; }

    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getFinalLetter() { return finalLetter; }
    public void setFinalLetter(String finalLetter) { this.finalLetter = finalLetter; }

    public Instant getScheduledStartAt() { return scheduledStartAt; }
    public void setScheduledStartAt(Instant scheduledStartAt) { this.scheduledStartAt = scheduledStartAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
