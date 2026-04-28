package dev.monkeypatch.rctiming.domain.practice;

import dev.monkeypatch.rctiming.domain.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "practice_laps")
public class PracticeLap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_session_id", nullable = false)
    private PracticeSession practiceSession;

    @Column(name = "transponder_number", nullable = false, length = 50)
    private String transponderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // nullable until linked

    @Column(name = "lap_number", nullable = false)
    private Integer lapNumber;

    @Column(name = "lap_time_ms", nullable = false)
    private Long lapTimeMs;

    @Column(name = "crossing_time", nullable = false)
    private Instant crossingTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Getters and setters
    public Long getId() { return id; }

    public PracticeSession getPracticeSession() { return practiceSession; }
    public void setPracticeSession(PracticeSession practiceSession) { this.practiceSession = practiceSession; }

    public String getTransponderNumber() { return transponderNumber; }
    public void setTransponderNumber(String transponderNumber) { this.transponderNumber = transponderNumber; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getLapNumber() { return lapNumber; }
    public void setLapNumber(Integer lapNumber) { this.lapNumber = lapNumber; }

    public Long getLapTimeMs() { return lapTimeMs; }
    public void setLapTimeMs(Long lapTimeMs) { this.lapTimeMs = lapTimeMs; }

    public Instant getCrossingTime() { return crossingTime; }
    public void setCrossingTime(Instant crossingTime) { this.crossingTime = crossingTime; }

    public Instant getCreatedAt() { return createdAt; }
}
