package dev.monkeypatch.rctiming.domain.track;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "decoder_loops")
public class DecoderLoop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "loop_id", nullable = false, length = 50)
    private String loopId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "loop_type", nullable = false, length = 50)
    private LoopType loopType = LoopType.FINISH_LINE;

    @Column(name = "is_scoring_loop", nullable = false)
    private boolean isScoringLoop = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Track getTrack() { return track; }
    public void setTrack(Track track) { this.track = track; }

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
