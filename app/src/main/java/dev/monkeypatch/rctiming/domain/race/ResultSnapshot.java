package dev.monkeypatch.rctiming.domain.race;


import java.time.Instant;

public class ResultSnapshot {

    private Long id;

    private Long raceId;

    private Instant finishedAt;

    private String positionsJson;

    private String lapHistoryJson;

    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRaceId() { return raceId; }
    public void setRaceId(Long raceId) { this.raceId = raceId; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public String getPositionsJson() { return positionsJson; }
    public void setPositionsJson(String positionsJson) { this.positionsJson = positionsJson; }

    public String getLapHistoryJson() { return lapHistoryJson; }
    public void setLapHistoryJson(String lapHistoryJson) { this.lapHistoryJson = lapHistoryJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
