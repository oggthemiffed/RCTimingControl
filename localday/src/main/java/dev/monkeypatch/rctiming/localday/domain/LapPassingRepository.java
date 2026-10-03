package dev.monkeypatch.rctiming.localday.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LapPassingRepository extends JpaRepository<LapPassing, Long> {

    List<LapPassing> findAllByCachedScheduleIdOrderByPassingAtAsc(Long cachedScheduleId);

    List<LapPassing> findAllByTransponderNumberOrderByPassingAtAsc(String transponderNumber);

    /**
     * Laps captured since the given watermark, oldest first — the incremental slice
     * {@link dev.monkeypatch.rctiming.localday.sync.SnapshotPushService} includes in each
     * periodic push (KTD8: laps are incremental, everything else in the payload is sent in full).
     */
    List<LapPassing> findAllByIdGreaterThanOrderByIdAsc(Long id);
}
