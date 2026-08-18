package dev.monkeypatch.rctiming.localday.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CachedRaceEntryRepository extends JpaRepository<CachedRaceEntry, Long> {

    List<CachedRaceEntry> findByCachedScheduleIdOrderByGridPositionAsc(Long cachedScheduleId);

    void deleteAllByCachedScheduleId(Long cachedScheduleId);
}
