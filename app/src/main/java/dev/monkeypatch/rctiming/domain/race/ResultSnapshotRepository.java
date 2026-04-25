package dev.monkeypatch.rctiming.domain.race;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResultSnapshotRepository extends JpaRepository<ResultSnapshot, Long> {
    Optional<ResultSnapshot> findByRaceId(Long raceId);
}
