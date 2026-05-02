package dev.monkeypatch.rctiming.timing;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory per-entry position within a live race.
 * Not persisted — held inside LiveRaceState during the race.
 */
public class LiveRacePosition {

    long entryId;
    int lapsCompleted;
    long lastPassingTimeMs;
    Long lastLapMs;
    Long bestLapMs;
    int lapCount;
    long lapSumMs;
    private final List<Long> lapTimes = new ArrayList<>();

    public long getEntryId() { return entryId; }
    public void setEntryId(long entryId) { this.entryId = entryId; }

    public int getLapsCompleted() { return lapsCompleted; }
    public void setLapsCompleted(int lapsCompleted) { this.lapsCompleted = lapsCompleted; }

    public long getLastPassingTimeMs() { return lastPassingTimeMs; }
    public void setLastPassingTimeMs(long lastPassingTimeMs) { this.lastPassingTimeMs = lastPassingTimeMs; }

    public Long getLastLapMs() { return lastLapMs; }
    public void setLastLapMs(Long lastLapMs) { this.lastLapMs = lastLapMs; }

    public Long getBestLapMs() { return bestLapMs; }
    public void setBestLapMs(Long bestLapMs) { this.bestLapMs = bestLapMs; }

    public int getLapCount() { return lapCount; }
    public long getLapSumMs() { return lapSumMs; }

    public void accumulateLap(long lapMs) {
        lapCount++;
        lapSumMs += lapMs;
    }

    /** Running average of all valid lap times, or null if no laps recorded yet. */
    public Long getAvgLapMs() {
        return lapCount > 0 ? lapSumMs / lapCount : null;
    }

    /** Returns the live list of individual lap durations in chronological order. */
    public List<Long> getLapTimes() { return lapTimes; }
}
