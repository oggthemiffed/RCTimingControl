package dev.monkeypatch.rctiming.localday.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Local join between a {@link CachedScheduleEntry} (race/heat) and a {@link CachedEntry}
 * (racer/entry) participating in it, with a grid position. Local analog of the cloud's
 * {@code RaceEntry} entity — {@link #cachedScheduleId} substitutes for the cloud's
 * {@code raceId}, {@link #cachedEntryId} for the cloud's {@code entryId}.
 *
 * <p>Unlike the cloud entity, {@link #cachedEntryId} is nullable: an unfilled bump slot is
 * represented as {@code null} rather than the cloud's {@code entryId == 0L} sentinel — a
 * deliberate independent-implementation improvement (KD3 permits matching behavior without
 * matching literal code).
 */
@Entity
@Table(name = "cached_race_entries")
public class CachedRaceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cached_schedule_id", nullable = false)
    private Long cachedScheduleId;

    @Column(name = "cached_entry_id")
    private Long cachedEntryId;

    @Column(name = "grid_position")
    private Integer gridPosition;

    @Column(name = "car_number")
    private Integer carNumber;

    @Column(nullable = false)
    private boolean bumped = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCachedScheduleId() { return cachedScheduleId; }
    public void setCachedScheduleId(Long cachedScheduleId) { this.cachedScheduleId = cachedScheduleId; }

    public Long getCachedEntryId() { return cachedEntryId; }
    public void setCachedEntryId(Long cachedEntryId) { this.cachedEntryId = cachedEntryId; }

    public Integer getGridPosition() { return gridPosition; }
    public void setGridPosition(Integer gridPosition) { this.gridPosition = gridPosition; }

    public Integer getCarNumber() { return carNumber; }
    public void setCarNumber(Integer carNumber) { this.carNumber = carNumber; }

    public boolean isBumped() { return bumped; }
    public void setBumped(boolean bumped) { this.bumped = bumped; }
}
