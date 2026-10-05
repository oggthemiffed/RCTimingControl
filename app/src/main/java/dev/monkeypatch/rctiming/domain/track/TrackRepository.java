package dev.monkeypatch.rctiming.domain.track;

import dev.monkeypatch.rctiming.jooq.generated.tables.records.TracksRecord;
import dev.monkeypatch.rctiming.persistence.JooqRepository;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SortField;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Tracks.TRACKS;

/**
 * Tracks, loaded with their decoder loops and lap thresholds. Saving a track saves only the track:
 * loops and thresholds are saved through their own repositories, and deleting a track deletes
 * them through the foreign keys.
 */
@Repository
public class TrackRepository extends JooqRepository<Track, TracksRecord> {

    private final DecoderLoopRepository decoderLoops;
    private final TrackLapThresholdRepository lapThresholds;

    public TrackRepository(DSLContext dsl, DecoderLoopRepository decoderLoops,
                           TrackLapThresholdRepository lapThresholds) {
        super(dsl, TRACKS, TRACKS.ID);
        this.decoderLoops = decoderLoops;
        this.lapThresholds = lapThresholds;
    }

    @Override
    protected Optional<Track> findOne(Condition condition) {
        return super.findOne(condition).map(this::withChildren);
    }

    @Override
    protected List<Track> findWhere(Condition condition, SortField<?>... order) {
        return super.findWhere(condition, order).stream().map(this::withChildren).toList();
    }

    private Track withChildren(Track track) {
        track.setDecoderLoops(decoderLoops.findByTrackId(track.getId()));
        track.setLapThresholds(lapThresholds.findByTrackId(track.getId()));
        return track;
    }

    @Override
    protected Track toEntity(TracksRecord r) {
        Track t = new Track();
        t.setId(r.getId());
        t.setName(r.getName());
        t.setVenueNotes(r.getVenueNotes());
        t.setTrackLength(r.getTrackLength());
        t.setCreatedAt(r.getCreatedAt());
        t.setUpdatedAt(r.getUpdatedAt());
        return t;
    }

    @Override
    protected void toRecord(Track t, TracksRecord r) {
        r.setName(t.getName());
        r.setVenueNotes(t.getVenueNotes());
        r.setTrackLength(t.getTrackLength());
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());
    }

    @Override
    protected Long idOf(Track t) {
        return t.getId();
    }

    @Override
    protected void setId(Track t, Long id) {
        t.setId(id);
    }
}
