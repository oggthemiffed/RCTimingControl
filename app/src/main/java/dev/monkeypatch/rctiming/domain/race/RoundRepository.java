package dev.monkeypatch.rctiming.domain.race;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundRepository extends JpaRepository<Round, Long> {
    List<Round> findByEventIdOrderBySequenceInEvent(Long eventId);
    boolean existsByEventId(Long eventId);
}
