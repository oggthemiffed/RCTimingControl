package dev.monkeypatch.rctiming.domain.practice;

import java.time.Instant;

public class PracticeSession {

    private Long id;

    private String name;

    private Long eventId;  // nullable for standalone sessions

    private PracticeStatus status = PracticeStatus.IDLE;

    private Integer bestLapN = 3;

    private Long createdByUserId;

    private Instant startedAt;

    private Instant stoppedAt;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    // Getters and setters
    public Long getId() { return id; }
    void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.updatedAt = Instant.now(); }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; this.updatedAt = Instant.now(); }

    public PracticeStatus getStatus() { return status; }
    public void setStatus(PracticeStatus status) { this.status = status; this.updatedAt = Instant.now(); }

    public Integer getBestLapN() { return bestLapN; }
    public void setBestLapN(Integer bestLapN) { this.bestLapN = bestLapN; this.updatedAt = Instant.now(); }

    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; this.updatedAt = Instant.now(); }

    public Instant getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(Instant stoppedAt) { this.stoppedAt = stoppedAt; this.updatedAt = Instant.now(); }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // State machine transitions
    public void start() {
        if (this.status != PracticeStatus.IDLE) {
            throw new IllegalStateException("Cannot start session in " + this.status + " state");
        }
        this.status = PracticeStatus.RUNNING;
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void stop() {
        if (this.status != PracticeStatus.RUNNING) {
            throw new IllegalStateException("Cannot stop session in " + this.status + " state");
        }
        this.status = PracticeStatus.STOPPED;
        this.stoppedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
