package dev.monkeypatch.rctiming.localday.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CachedScheduleEntryRepository extends JpaRepository<CachedScheduleEntry, Long> {

    Optional<CachedScheduleEntry> findByCloudRaceId(Long cloudRaceId);

    List<CachedScheduleEntry> findAllByOrderBySequenceAsc();
}
