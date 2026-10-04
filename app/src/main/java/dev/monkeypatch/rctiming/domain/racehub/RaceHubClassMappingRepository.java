package dev.monkeypatch.rctiming.domain.racehub;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaceHubClassMappingRepository extends JpaRepository<RaceHubClassMapping, Long> {

    List<RaceHubClassMapping> findByEventIdOrderByRacehubEventClassId(Long eventId);

    void deleteByEventId(Long eventId);
}
