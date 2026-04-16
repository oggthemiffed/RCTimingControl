package dev.monkeypatch.rctiming.domain.track;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tracks")
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "venue_notes", columnDefinition = "text")
    private String venueNotes;

    @Column(name = "track_length")
    private Double trackLength;

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DecoderLoop> decoderLoops = new ArrayList<>();

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrackLapThreshold> lapThresholds = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVenueNotes() { return venueNotes; }
    public void setVenueNotes(String venueNotes) { this.venueNotes = venueNotes; }

    public Double getTrackLength() { return trackLength; }
    public void setTrackLength(Double trackLength) { this.trackLength = trackLength; }

    public List<DecoderLoop> getDecoderLoops() { return decoderLoops; }
    public void setDecoderLoops(List<DecoderLoop> decoderLoops) { this.decoderLoops = decoderLoops; }

    public List<TrackLapThreshold> getLapThresholds() { return lapThresholds; }
    public void setLapThresholds(List<TrackLapThreshold> lapThresholds) { this.lapThresholds = lapThresholds; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
