package dev.monkeypatch.rctiming.localday.race;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaceResultEntryRepository extends JpaRepository<RaceResultEntry, Long> {

    List<RaceResultEntry> findByRaceIdOrderByPositionAsc(Long raceId);
}
