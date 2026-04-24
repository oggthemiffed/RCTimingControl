package dev.monkeypatch.rctiming.domain.race;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "marshal_adjustments")
public class MarshalAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "race_id", nullable = false)
    private Long raceId;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "transponder_number", nullable = false, length = 20)
    private String transponderNumber;

    @Column(name = "lap_delta", nullable = false)
    private int lapDelta;

    @Column(name = "race_state_at_time", nullable = false, length = 20)
    private String raceStateAtTime;

    @Column(name = "acting_user_id", nullable = false)
    private Long actingUserId;

    @Column(name = "acting_user_name", nullable = false, length = 200)
    private String actingUserName;

    @Column(name = "adjusted_at", nullable = false)
    private Instant adjustedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRaceId() { return raceId; }
    public void setRaceId(Long raceId) { this.raceId = raceId; }

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }

    public String getTransponderNumber() { return transponderNumber; }
    public void setTransponderNumber(String transponderNumber) { this.transponderNumber = transponderNumber; }

    public int getLapDelta() { return lapDelta; }
    public void setLapDelta(int lapDelta) { this.lapDelta = lapDelta; }

    public String getRaceStateAtTime() { return raceStateAtTime; }
    public void setRaceStateAtTime(String raceStateAtTime) { this.raceStateAtTime = raceStateAtTime; }

    public Long getActingUserId() { return actingUserId; }
    public void setActingUserId(Long actingUserId) { this.actingUserId = actingUserId; }

    public String getActingUserName() { return actingUserName; }
    public void setActingUserName(String actingUserName) { this.actingUserName = actingUserName; }

    public Instant getAdjustedAt() { return adjustedAt; }
    public void setAdjustedAt(Instant adjustedAt) { this.adjustedAt = adjustedAt; }
}
