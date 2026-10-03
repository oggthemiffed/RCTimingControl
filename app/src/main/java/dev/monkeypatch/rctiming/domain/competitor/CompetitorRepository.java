package dev.monkeypatch.rctiming.domain.competitor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitorRepository extends JpaRepository<Competitor, Long> {

    Optional<Competitor> findByExternalSourceAndExternalId(String externalSource, String externalId);
}
