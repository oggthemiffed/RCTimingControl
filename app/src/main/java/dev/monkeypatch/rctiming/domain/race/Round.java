package dev.monkeypatch.rctiming.domain.race;


import java.time.Instant;

public class Round {

    private Long id;

    private Long eventId;

    private RoundType type;

    private int roundNumber;

    private int sequenceInEvent;

    private RoundStatus status = RoundStatus.PENDING;

    private Instant createdAt;

    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public RoundType getType() { return type; }
    public void setType(RoundType type) { this.type = type; }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }

    public int getSequenceInEvent() { return sequenceInEvent; }
    public void setSequenceInEvent(int sequenceInEvent) { this.sequenceInEvent = sequenceInEvent; }

    public RoundStatus getStatus() { return status; }
    public void setStatus(RoundStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
