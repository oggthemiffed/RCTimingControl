package dev.monkeypatch.rctiming.domain.track;

import java.time.Instant;

public class TrackLapThreshold {

    private Long id;
    private Long trackId;
    /** Null for the track's default threshold, which applies to every class without its own. */
    private Long racingClassId;
    /** The class's name, filled in when the threshold is loaded; not saved. */
    private String racingClassName;
    private int minLapMs;
    private Integer maxLastLapMs;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTrackId() { return trackId; }
    public void setTrackId(Long trackId) { this.trackId = trackId; }

    public Long getRacingClassId() { return racingClassId; }
    public void setRacingClassId(Long racingClassId) { this.racingClassId = racingClassId; }

    public String getRacingClassName() { return racingClassName; }
    public void setRacingClassName(String racingClassName) { this.racingClassName = racingClassName; }

    public int getMinLapMs() { return minLapMs; }
    public void setMinLapMs(int minLapMs) { this.minLapMs = minLapMs; }

    public Integer getMaxLastLapMs() { return maxLastLapMs; }
    public void setMaxLastLapMs(Integer maxLastLapMs) { this.maxLastLapMs = maxLastLapMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
