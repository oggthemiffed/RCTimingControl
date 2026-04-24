package dev.monkeypatch.rctiming.domain.race;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarshalPenaltyRepository extends JpaRepository<MarshalPenalty, Long> {
    List<MarshalPenalty> findByEntryIdAndEventId(Long entryId, Long eventId);
}
