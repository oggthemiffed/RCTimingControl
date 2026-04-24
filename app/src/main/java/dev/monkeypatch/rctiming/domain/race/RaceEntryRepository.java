package dev.monkeypatch.rctiming.domain.race;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaceEntryRepository extends JpaRepository<RaceEntry, Long> {
    List<RaceEntry> findByRaceIdOrderByGridPosition(Long raceId);
    List<RaceEntry> findByEntryId(Long entryId);
}
