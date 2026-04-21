package dev.monkeypatch.rctiming.domain.championship;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChampionshipEventLinkRepository extends JpaRepository<ChampionshipEventLink, Long> {
    List<ChampionshipEventLink> findByChampionshipIdOrderByRoundNumberAsc(Long championshipId);
    boolean existsByChampionshipIdAndRoundNumber(Long championshipId, int roundNumber);
    boolean existsByChampionshipIdAndEventId(Long championshipId, Long eventId);
    void deleteByChampionshipIdAndEventId(Long championshipId, Long eventId);
}
