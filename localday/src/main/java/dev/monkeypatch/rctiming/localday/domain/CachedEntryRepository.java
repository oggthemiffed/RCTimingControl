package dev.monkeypatch.rctiming.localday.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CachedEntryRepository extends JpaRepository<CachedEntry, Long> {

    Optional<CachedEntry> findByCloudEntryId(Long cloudEntryId);

    Optional<CachedEntry> findByTransponderNumber(String transponderNumber);

    List<CachedEntry> findByRacerNameContainingIgnoreCase(String racerName);
}
