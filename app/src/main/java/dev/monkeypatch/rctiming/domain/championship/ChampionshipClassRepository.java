package dev.monkeypatch.rctiming.domain.championship;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChampionshipClassRepository extends JpaRepository<ChampionshipClass, Long> {
    List<ChampionshipClass> findByChampionshipId(Long championshipId);
    boolean existsByChampionshipIdAndRacingClassId(Long championshipId, Long racingClassId);
    void deleteByChampionshipIdAndRacingClassId(Long championshipId, Long racingClassId);
}
