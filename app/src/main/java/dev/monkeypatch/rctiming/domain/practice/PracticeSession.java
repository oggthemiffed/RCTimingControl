package dev.monkeypatch.rctiming.domain.practice;

import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "practice_sessions")
public class PracticeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;  // nullable for standalone sessions

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PracticeStatus status = PracticeStatus.IDLE;

    @Column(name = "best_lap_n", nullable = false)
    private Integer bestLapN = 3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "stopped_at")
    private Instant stoppedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Getters and setters
    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.updatedAt = Instant.now(); }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; this.updatedAt = Instant.now(); }

    public PracticeStatus getStatus() { return status; }
    public void setStatus(PracticeStatus status) { this.status = status; this.updatedAt = Instant.now(); }

    public Integer getBestLapN() { return bestLapN; }
    public void setBestLapN(Integer bestLapN) { this.bestLapN = bestLapN; this.updatedAt = Instant.now(); }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; this.updatedAt = Instant.now(); }

    public Instant getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(Instant stoppedAt) { this.stoppedAt = stoppedAt; this.updatedAt = Instant.now(); }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

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
