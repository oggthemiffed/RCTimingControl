package dev.monkeypatch.rctiming.localday.race;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarshalAdjustmentRepository extends JpaRepository<MarshalAdjustment, Long> {

    List<MarshalAdjustment> findAllByRaceIdOrderByAdjustedAtAsc(Long raceId);
}
