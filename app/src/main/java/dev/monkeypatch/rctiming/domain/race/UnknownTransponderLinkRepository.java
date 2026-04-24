package dev.monkeypatch.rctiming.domain.race;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnknownTransponderLinkRepository extends JpaRepository<UnknownTransponderLink, Long> {
    Optional<UnknownTransponderLink> findByRaceIdAndTransponderNumber(Long raceId, String transponderNumber);
}
