package dev.monkeypatch.rctiming.localday.race;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A single entry's final position in a finished race, captured once at the RUNNING/STOPPED to
 * FINISHED transition. This is the one durable exception to "do not store live race positions
 * during a race" — {@link #raceId} and {@link #entryId} reference local rows
 * ({@code CachedScheduleEntry.id} and {@code CachedEntry.id} respectively), not cloud IDs, same
 * naming precedent as {@link MarshalAdjustment}.
 */
@Entity
@Table(name = "race_results")
public class RaceResultEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "race_id", nullable = false)
    private Long raceId;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(nullable = false)
    private int position;

    @Column(name = "laps_completed", nullable = false)
    private int lapsCompleted;

    @Column(name = "best_lap_ms")
    private Long bestLapMs;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRaceId() { return raceId; }
    public void setRaceId(Long raceId) { this.raceId = raceId; }

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public int getLapsCompleted() { return lapsCompleted; }
    public void setLapsCompleted(int lapsCompleted) { this.lapsCompleted = lapsCompleted; }

    public Long getBestLapMs() { return bestLapMs; }
    public void setBestLapMs(Long bestLapMs) { this.bestLapMs = bestLapMs; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
