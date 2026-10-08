package dev.monkeypatch.rctiming.domain.track;

import dev.monkeypatch.rctiming.api.admin.dto.CreateDecoderLoopRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateThresholdRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateTrackRequest;
import dev.monkeypatch.rctiming.api.admin.dto.DecoderLoopDto;
import dev.monkeypatch.rctiming.api.admin.dto.TrackDto;
import dev.monkeypatch.rctiming.api.admin.dto.TrackLapThresholdDto;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TrackService {

    private final TrackRepository trackRepository;
    private final DecoderLoopRepository decoderLoopRepository;
    private final TrackLapThresholdRepository thresholdRepository;
    private final RacingClassRepository racingClassRepository;
    private final AuditService audit;

    public TrackService(TrackRepository trackRepository,
                        DecoderLoopRepository decoderLoopRepository,
                        TrackLapThresholdRepository thresholdRepository,
                        RacingClassRepository racingClassRepository,
                        AuditService audit) {
        this.trackRepository = trackRepository;
        this.decoderLoopRepository = decoderLoopRepository;
        this.thresholdRepository = thresholdRepository;
        this.racingClassRepository = racingClassRepository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<TrackDto> findAll() {
        return trackRepository.findAll().stream()
                .map(TrackDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrackDto findById(Long id) {
        return TrackDto.from(getTrackOrThrow(id));
    }

    public TrackDto create(Actor actor, CreateTrackRequest request) {
        Track track = new Track();
        track.setName(request.name());
        track.setVenueNotes(request.venueNotes());
        track.setTrackLength(request.trackLength());
        Instant now = Instant.now();
        track.setCreatedAt(now);
        track.setUpdatedAt(now);
        Track saved = trackRepository.save(track);
        audit.entry(actor, "TRACK_CREATED").entity("track", saved.getId())
                .summary("Added the track " + saved.getName())
                .after(trackValues(saved)).record();
        return TrackDto.from(saved);
    }

    public TrackDto update(Actor actor, Long id, CreateTrackRequest request) {
        Track track = getTrackOrThrow(id);
        Map<String, Object> before = trackValues(track);
        track.setName(request.name());
        track.setVenueNotes(request.venueNotes());
        track.setTrackLength(request.trackLength());
        track.setUpdatedAt(Instant.now());
        Track saved = trackRepository.save(track);
        audit.entry(actor, "TRACK_UPDATED").entity("track", id)
                .summary("Changed the track " + saved.getName())
                .before(before).after(trackValues(saved)).record();
        return TrackDto.from(saved);
    }

    public void delete(Actor actor, Long id) {
        Track track = getTrackOrThrow(id);
        Map<String, Object> before = trackValues(track);
        trackRepository.deleteById(id);
        audit.entry(actor, "TRACK_DELETED").entity("track", id)
                .summary("Removed the track " + track.getName() + " with its decoder loops and lap thresholds")
                .before(before).record();
    }

    public DecoderLoopDto addDecoderLoop(Actor actor, Long trackId, CreateDecoderLoopRequest request) {
        Track track = getTrackOrThrow(trackId);
        DecoderLoop loop = new DecoderLoop();
        loop.setTrackId(track.getId());
        loop.setLoopId(request.loopId());
        loop.setDisplayName(request.displayName());
        loop.setLoopType(request.loopType());
        loop.setScoringLoop(request.isScoringLoop());
        loop.setCreatedAt(Instant.now());
        DecoderLoop saved = decoderLoopRepository.save(loop);
        audit.entry(actor, "DECODER_LOOP_ADDED").entity("decoder_loop", saved.getId())
                .summary("Added decoder loop " + loopLabel(saved) + " to " + track.getName())
                .after(loopValues(saved)).record();
        return DecoderLoopDto.from(saved);
    }

    public DecoderLoopDto updateDecoderLoop(Actor actor, Long loopId, CreateDecoderLoopRequest request) {
        DecoderLoop loop = decoderLoopRepository.findById(loopId)
                .orElseThrow(() -> new EntityNotFoundException("Decoder loop not found: " + loopId));
        Map<String, Object> before = loopValues(loop);
        loop.setLoopId(request.loopId());
        loop.setDisplayName(request.displayName());
        loop.setLoopType(request.loopType());
        loop.setScoringLoop(request.isScoringLoop());
        DecoderLoop saved = decoderLoopRepository.save(loop);
        audit.entry(actor, "DECODER_LOOP_UPDATED").entity("decoder_loop", loopId)
                .summary("Changed decoder loop " + loopLabel(saved) + " of " + trackName(saved.getTrackId()))
                .before(before).after(loopValues(saved)).record();
        return DecoderLoopDto.from(saved);
    }

    public void deleteDecoderLoop(Actor actor, Long loopId) {
        DecoderLoop loop = decoderLoopRepository.findById(loopId)
                .orElseThrow(() -> new EntityNotFoundException("Decoder loop not found: " + loopId));
        decoderLoopRepository.deleteById(loopId);
        audit.entry(actor, "DECODER_LOOP_DELETED").entity("decoder_loop", loopId)
                .summary("Removed decoder loop " + loopLabel(loop) + " from " + trackName(loop.getTrackId()))
                .before(loopValues(loop)).record();
    }

    public TrackLapThresholdDto setLapThreshold(Actor actor, Long trackId, CreateThresholdRequest request) {
        Track track = getTrackOrThrow(trackId);

        // Find existing threshold for this track + class combination (upsert)
        TrackLapThreshold threshold;
        if (request.racingClassId() == null) {
            threshold = thresholdRepository.findByTrackIdAndRacingClassIsNull(trackId)
                    .orElseGet(TrackLapThreshold::new);
        } else {
            threshold = thresholdRepository
                    .findByTrackIdAndRacingClassId(trackId, request.racingClassId())
                    .orElseGet(TrackLapThreshold::new);
        }

        boolean isNew = threshold.getId() == null;
        Map<String, Object> before = isNew ? null : thresholdValues(threshold);
        threshold.setTrackId(track.getId());
        threshold.setMinLapMs(request.minLapMs());
        threshold.setMaxLastLapMs(request.maxLastLapMs());

        if (request.racingClassId() != null) {
            RacingClass racingClass = racingClassRepository.findById(request.racingClassId())
                    .orElseThrow(() -> new EntityNotFoundException("Racing class not found: " + request.racingClassId()));
            threshold.setRacingClassId(racingClass.getId());
            threshold.setRacingClassName(racingClass.getName());
        } else {
            threshold.setRacingClassId(null);
            threshold.setRacingClassName(null);
        }

        if (isNew) {
            threshold.setCreatedAt(Instant.now());
        }

        TrackLapThreshold saved = thresholdRepository.save(threshold);
        audit.entry(actor, isNew ? "LAP_THRESHOLD_ADDED" : "LAP_THRESHOLD_CHANGED")
                .entity("lap_threshold", saved.getId())
                .summary("Set the lap thresholds for " + (saved.getRacingClassName() == null ? "every class"
                        : saved.getRacingClassName()) + " on " + track.getName())
                .before(before).after(thresholdValues(saved)).record();
        return TrackLapThresholdDto.from(saved);
    }

    public void deleteLapThreshold(Actor actor, Long thresholdId) {
        TrackLapThreshold threshold = thresholdRepository.findById(thresholdId)
                .orElseThrow(() -> new EntityNotFoundException("Lap threshold not found: " + thresholdId));
        thresholdRepository.deleteById(thresholdId);
        audit.entry(actor, "LAP_THRESHOLD_DELETED").entity("lap_threshold", thresholdId)
                .summary("Removed the lap thresholds for " + (threshold.getRacingClassName() == null ? "every class"
                        : threshold.getRacingClassName()) + " on " + trackName(threshold.getTrackId()))
                .before(thresholdValues(threshold)).record();
    }

    private String trackName(Long trackId) {
        return trackRepository.findById(trackId).map(Track::getName).orElse("track " + trackId);
    }

    private static String loopLabel(DecoderLoop loop) {
        return loop.getDisplayName() != null && !loop.getDisplayName().isBlank()
                ? loop.getDisplayName() : String.valueOf(loop.getLoopId());
    }

    private static Map<String, Object> trackValues(Track t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", t.getName());
        m.put("venueNotes", t.getVenueNotes());
        m.put("trackLength", t.getTrackLength());
        return m;
    }

    private static Map<String, Object> loopValues(DecoderLoop l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("trackId", l.getTrackId());
        m.put("loopId", l.getLoopId());
        m.put("displayName", l.getDisplayName());
        m.put("loopType", l.getLoopType());
        m.put("scoringLoop", l.isScoringLoop());
        return m;
    }

    private static Map<String, Object> thresholdValues(TrackLapThreshold t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("trackId", t.getTrackId());
        m.put("racingClassId", t.getRacingClassId());
        m.put("racingClassName", t.getRacingClassName());
        m.put("minLapMs", t.getMinLapMs());
        m.put("maxLastLapMs", t.getMaxLastLapMs());
        return m;
    }

    private Track getTrackOrThrow(Long id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Track not found: " + id));
    }
}
