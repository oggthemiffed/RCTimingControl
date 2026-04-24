package dev.monkeypatch.rctiming.timing;

/**
 * In-memory per-entry position within a live race.
 * Not persisted — held inside LiveRaceState during the race.
 */
public class LiveRacePosition {

    long entryId;
    int lapsCompleted;
    long lastPassingTimeMs;
    Long bestLapMs;

    public long getEntryId() { return entryId; }
    public void setEntryId(long entryId) { this.entryId = entryId; }

    public int getLapsCompleted() { return lapsCompleted; }
    public void setLapsCompleted(int lapsCompleted) { this.lapsCompleted = lapsCompleted; }

    public long getLastPassingTimeMs() { return lastPassingTimeMs; }
    public void setLastPassingTimeMs(long lastPassingTimeMs) { this.lastPassingTimeMs = lastPassingTimeMs; }

    public Long getBestLapMs() { return bestLapMs; }
    public void setBestLapMs(Long bestLapMs) { this.bestLapMs = bestLapMs; }
}
