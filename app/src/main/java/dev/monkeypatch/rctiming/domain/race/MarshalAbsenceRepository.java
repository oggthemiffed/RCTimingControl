package dev.monkeypatch.rctiming.domain.race;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarshalAbsenceRepository extends JpaRepository<MarshalAbsence, Long> {
    long countByEntryIdAndEventId(Long entryId, Long eventId);
    List<MarshalAbsence> findByEventId(Long eventId);
}
