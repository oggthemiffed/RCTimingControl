package dev.monkeypatch.rctiming.domain.race;


import java.time.Instant;

public class UnknownTransponderLink {

    private Long id;

    private Long raceId;

    private String transponderNumber;

    private Long linkedEntryId;

    private Long linkedBy;

    private Instant linkedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRaceId() { return raceId; }
    public void setRaceId(Long raceId) { this.raceId = raceId; }

    public String getTransponderNumber() { return transponderNumber; }
    public void setTransponderNumber(String transponderNumber) { this.transponderNumber = transponderNumber; }

    public Long getLinkedEntryId() { return linkedEntryId; }
    public void setLinkedEntryId(Long linkedEntryId) { this.linkedEntryId = linkedEntryId; }

    public Long getLinkedBy() { return linkedBy; }
    public void setLinkedBy(Long linkedBy) { this.linkedBy = linkedBy; }

    public Instant getLinkedAt() { return linkedAt; }
    public void setLinkedAt(Instant linkedAt) { this.linkedAt = linkedAt; }
}
