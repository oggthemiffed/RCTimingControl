package dev.monkeypatch.rctiming.domain.entry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    // For round generator: load CONFIRMED entries for a specific event class
    List<Entry> findByEventClassIdAndStatus(Long eventClassId, EntryStatus status);

    // For PreCacheService: confirmed entries for a whole event, for the pre-cache payload
    List<Entry> findByEventIdAndStatus(Long eventId, EntryStatus status);

    // For the RaceHub import (L7): upsert by the source's entry id
    Optional<Entry> findByExternalSourceAndExternalEntryId(String externalSource, String externalEntryId);

    List<Entry> findByEventId(Long eventId);
}
