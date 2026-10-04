package dev.monkeypatch.rctiming.domain.entry;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    // For round generator: load CONFIRMED entries for a specific event class
    List<Entry> findByEventClassIdAndStatus(Long eventClassId, EntryStatus status);

    // For the RaceHub import (L7): upsert by the source's entry id
    Optional<Entry> findByExternalSourceAndExternalEntryId(String externalSource, String externalEntryId);

    List<Entry> findByEventId(Long eventId);

    /** Loads an entry with a row lock, so check-in's read-then-write is atomic (L11). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Entry e WHERE e.id = :id")
    Optional<Entry> findByIdForUpdate(@Param("id") Long id);
}
