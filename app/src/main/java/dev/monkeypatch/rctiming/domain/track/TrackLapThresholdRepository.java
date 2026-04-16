package dev.monkeypatch.rctiming.domain.track;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackLapThresholdRepository extends JpaRepository<TrackLapThreshold, Long> {

    List<TrackLapThreshold> findByTrackId(Long trackId);

    Optional<TrackLapThreshold> findByTrackIdAndRacingClassId(Long trackId, Long racingClassId);

    Optional<TrackLapThreshold> findByTrackIdAndRacingClassIsNull(Long trackId);
}
