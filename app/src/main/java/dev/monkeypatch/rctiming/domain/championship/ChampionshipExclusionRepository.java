package dev.monkeypatch.rctiming.domain.championship;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChampionshipExclusionRepository extends JpaRepository<ChampionshipExclusion, Long> {
    List<ChampionshipExclusion> findByChampionshipIdOrderByCreatedAtDesc(Long championshipId);
    boolean existsByChampionshipIdAndDriverIdAndEventId(Long championshipId, Long driverId, Long eventId);
}
