package dev.monkeypatch.rctiming.localday.domain;

import dev.monkeypatch.rctiming.localday.race.RaceState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
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

    /**
     * Resolves "the next upcoming race" for the anonymous board API (U9) — the earliest-sequence
     * race that hasn't started yet, whether it's still unscheduled or already called to grid.
     */
    Optional<CachedScheduleEntry> findFirstByStatusInOrderBySequenceAsc(Collection<RaceState> statuses);

    /**
     * Resolves "the most recently finished race" for the anonymous board API (U9) — used both to
     * show last-completed results and to distinguish "between rounds" from "before the first heat".
     */
    Optional<CachedScheduleEntry> findFirstByStatusOrderByFinishedAtDesc(RaceState status);
}
