package dev.monkeypatch.rctiming.domain.format;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventClassRepository extends JpaRepository<EventClass, Long> {
    List<EventClass> findByEventId(Long eventId);
}
