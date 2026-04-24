package dev.monkeypatch.rctiming.domain.entry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    List<Entry> findByUserIdOrderBySubmittedAtDesc(Long userId);

    // For RACER-09 soft warning: other CONFIRMED entries same event, same transponder number, different user
    List<Entry> findByEventIdAndTransponderNumberSnapshotAndStatusAndUserIdNot(
            Long eventId, String transponderNumberSnapshot, EntryStatus status, Long userId);

    // For round generator: load CONFIRMED entries for a specific event class
    List<Entry> findByEventClassIdAndStatus(Long eventClassId, EntryStatus status);
}
