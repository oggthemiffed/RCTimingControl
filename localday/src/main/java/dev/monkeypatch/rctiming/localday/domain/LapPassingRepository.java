package dev.monkeypatch.rctiming.localday.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LapPassingRepository extends JpaRepository<LapPassing, Long> {

    List<LapPassing> findAllByCachedScheduleIdOrderByPassingAtAsc(Long cachedScheduleId);

    List<LapPassing> findAllByTransponderNumberOrderByPassingAtAsc(String transponderNumber);
}
