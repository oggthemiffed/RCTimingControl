package dev.monkeypatch.rctiming.domain.track;

import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
    name = "track_lap_thresholds",
    uniqueConstraints = @UniqueConstraint(columnNames = {"track_id", "racing_class_id"})
)
public class TrackLapThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "racing_class_id")
    private RacingClass racingClass;

    @Column(name = "min_lap_ms", nullable = false)
    private int minLapMs;

    @Column(name = "max_last_lap_ms")
    private Integer maxLastLapMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Track getTrack() { return track; }
    public void setTrack(Track track) { this.track = track; }

    public RacingClass getRacingClass() { return racingClass; }
    public void setRacingClass(RacingClass racingClass) { this.racingClass = racingClass; }

    public int getMinLapMs() { return minLapMs; }
    public void setMinLapMs(int minLapMs) { this.minLapMs = minLapMs; }

    public Integer getMaxLastLapMs() { return maxLastLapMs; }
    public void setMaxLastLapMs(Integer maxLastLapMs) { this.maxLastLapMs = maxLastLapMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
