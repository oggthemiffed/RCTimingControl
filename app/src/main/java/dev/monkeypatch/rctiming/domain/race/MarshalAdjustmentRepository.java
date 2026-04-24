package dev.monkeypatch.rctiming.domain.race;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarshalAdjustmentRepository extends JpaRepository<MarshalAdjustment, Long> {
    List<MarshalAdjustment> findByRaceIdOrderByAdjustedAt(Long raceId);
}
