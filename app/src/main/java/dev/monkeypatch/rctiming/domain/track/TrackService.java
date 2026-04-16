package dev.monkeypatch.rctiming.domain.track;

import dev.monkeypatch.rctiming.api.admin.dto.CreateDecoderLoopRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateThresholdRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CreateTrackRequest;
import dev.monkeypatch.rctiming.api.admin.dto.DecoderLoopDto;
import dev.monkeypatch.rctiming.api.admin.dto.TrackDto;
import dev.monkeypatch.rctiming.api.admin.dto.TrackLapThresholdDto;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class TrackService {

    private final TrackRepository trackRepository;
    private final DecoderLoopRepository decoderLoopRepository;
    private final TrackLapThresholdRepository thresholdRepository;
    private final RacingClassRepository racingClassRepository;

    public TrackService(TrackRepository trackRepository,
                        DecoderLoopRepository decoderLoopRepository,
                        TrackLapThresholdRepository thresholdRepository,
                        RacingClassRepository racingClassRepository) {
        this.trackRepository = trackRepository;
        this.decoderLoopRepository = decoderLoopRepository;
        this.thresholdRepository = thresholdRepository;
        this.racingClassRepository = racingClassRepository;
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

    public TrackDto create(CreateTrackRequest request) {
        Track track = new Track();
        track.setName(request.name());
        track.setVenueNotes(request.venueNotes());
        track.setTrackLength(request.trackLength());
        Instant now = Instant.now();
        track.setCreatedAt(now);
        track.setUpdatedAt(now);
        return TrackDto.from(trackRepository.save(track));
    }

    public TrackDto update(Long id, CreateTrackRequest request) {
        Track track = getTrackOrThrow(id);
        track.setName(request.name());
        track.setVenueNotes(request.venueNotes());
        track.setTrackLength(request.trackLength());
        track.setUpdatedAt(Instant.now());
        return TrackDto.from(trackRepository.save(track));
    }

    public void delete(Long id) {
        if (!trackRepository.existsById(id)) {
            throw new EntityNotFoundException("Track not found: " + id);
        }
        trackRepository.deleteById(id);
    }

    public DecoderLoopDto addDecoderLoop(Long trackId, CreateDecoderLoopRequest request) {
        Track track = getTrackOrThrow(trackId);
        DecoderLoop loop = new DecoderLoop();
        loop.setTrack(track);
        loop.setLoopId(request.loopId());
        loop.setDisplayName(request.displayName());
        loop.setLoopType(request.loopType());
        loop.setScoringLoop(request.isScoringLoop());
        loop.setCreatedAt(Instant.now());
        return DecoderLoopDto.from(decoderLoopRepository.save(loop));
    }

    public DecoderLoopDto updateDecoderLoop(Long loopId, CreateDecoderLoopRequest request) {
        DecoderLoop loop = decoderLoopRepository.findById(loopId)
                .orElseThrow(() -> new EntityNotFoundException("Decoder loop not found: " + loopId));
        loop.setLoopId(request.loopId());
        loop.setDisplayName(request.displayName());
        loop.setLoopType(request.loopType());
        loop.setScoringLoop(request.isScoringLoop());
        return DecoderLoopDto.from(decoderLoopRepository.save(loop));
    }

    public void deleteDecoderLoop(Long loopId) {
        if (!decoderLoopRepository.existsById(loopId)) {
            throw new EntityNotFoundException("Decoder loop not found: " + loopId);
        }
        decoderLoopRepository.deleteById(loopId);
    }

    public TrackLapThresholdDto setLapThreshold(Long trackId, CreateThresholdRequest request) {
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
        threshold.setTrack(track);
        threshold.setMinLapMs(request.minLapMs());
        threshold.setMaxLastLapMs(request.maxLastLapMs());

        if (request.racingClassId() != null) {
            RacingClass racingClass = racingClassRepository.findById(request.racingClassId())
                    .orElseThrow(() -> new EntityNotFoundException("Racing class not found: " + request.racingClassId()));
            threshold.setRacingClass(racingClass);
        } else {
            threshold.setRacingClass(null);
        }

        if (isNew) {
            threshold.setCreatedAt(Instant.now());
        }

        return TrackLapThresholdDto.from(thresholdRepository.save(threshold));
    }

    public void deleteLapThreshold(Long thresholdId) {
        if (!thresholdRepository.existsById(thresholdId)) {
            throw new EntityNotFoundException("Lap threshold not found: " + thresholdId);
        }
        thresholdRepository.deleteById(thresholdId);
    }

    private Track getTrackOrThrow(Long id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Track not found: " + id));
    }
}
