package dev.monkeypatch.rctiming.domain.championship;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "championship_event_links")
public class ChampionshipEventLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "championship_id", nullable = false)
    private Long championshipId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
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
