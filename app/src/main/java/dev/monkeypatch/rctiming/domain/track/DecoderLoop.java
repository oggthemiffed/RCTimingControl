package dev.monkeypatch.rctiming.domain.track;

import java.time.Instant;

public class DecoderLoop {

    private Long id;
    private Long trackId;
    private String loopId;
    private String displayName;
    private LoopType loopType = LoopType.FINISH_LINE;
    private boolean isScoringLoop = true;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTrackId() { return trackId; }
    public void setTrackId(Long trackId) { this.trackId = trackId; }

    public String getLoopId() { return loopId; }
    public void setLoopId(String loopId) { this.loopId = loopId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public LoopType getLoopType() { return loopType; }
    public void setLoopType(LoopType loopType) { this.loopType = loopType; }

    public boolean isScoringLoop() { return isScoringLoop; }
    public void setScoringLoop(boolean scoringLoop) { isScoringLoop = scoringLoop; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
