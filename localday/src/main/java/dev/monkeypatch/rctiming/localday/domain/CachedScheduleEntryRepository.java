package dev.monkeypatch.rctiming.localday.domain;

import dev.monkeypatch.rctiming.localday.race.RaceState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CachedScheduleEntryRepository extends JpaRepository<CachedScheduleEntry, Long> {

    Optional<CachedScheduleEntry> findByCloudRaceId(Long cloudRaceId);

    List<CachedScheduleEntry> findAllByOrderBySequenceAsc();

    List<CachedScheduleEntry> findByClassNameAndFinalLetterIsNotNull(String className);

    /**
     * Resolves "the currently active race" — mirrors the cloud's
     * {@code RaceRepository.findFirstByStatus(RaceStatus.RUNNING)}, used the same way by the
     * decoder ingestion path to attach an incoming passing to the running race, if any.
     */
    Optional<CachedScheduleEntry> findFirstByStatus(RaceState status);
}
