package dev.monkeypatch.rctiming.domain.championship;


import java.time.Instant;

public class ChampionshipEventLink {

    private Long id;

    private Long championshipId;

    private Long eventId;

    private int roundNumber;

    private Instant createdAt;

    public ChampionshipEventLink() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChampionshipId() { return championshipId; }
    public void setChampionshipId(Long v) { this.championshipId = v; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long v) { this.eventId = v; }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int v) { this.roundNumber = v; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant t) { this.createdAt = t; }
}
