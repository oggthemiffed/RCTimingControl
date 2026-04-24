package dev.monkeypatch.rctiming.domain.race;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {
    List<Penalty> findByRaceId(Long raceId);
}
