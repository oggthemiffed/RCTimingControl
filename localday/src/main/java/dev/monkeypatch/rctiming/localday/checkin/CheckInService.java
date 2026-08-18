package dev.monkeypatch.rctiming.localday.checkin;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Check-in / attendance confirmation for pre-entered racers (R5) — the scanned-or-manually-typed
 * transponder-number resolve path and the manual name-search fallback are both entirely local,
 * with no cloud dependency at any point.
 */
@Service
public class CheckInService {

    private final CachedEntryRepository cachedEntryRepository;

    public CheckInService(CachedEntryRepository cachedEntryRepository) {
        this.cachedEntryRepository = cachedEntryRepository;
    }

    public Optional<CachedEntry> resolveByTransponderNumber(String transponderNumber) {
        return cachedEntryRepository.findByTransponderNumber(transponderNumber);
    }

    /**
     * Manual roster search by racer name, case-insensitive substring match — the fallback path
     * when a scan doesn't resolve. A blank/empty query short-circuits to an empty list without
     * hitting the database; this is a check-in-desk search, not a full roster browser.
     */
    public List<CachedEntry> searchByName(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        return cachedEntryRepository.findByRacerNameContainingIgnoreCase(query);
    }

    /**
     * Confirms attendance for the given pre-entered racer. Idempotent-but-informative: a second
     * confirm on an already-checked-in entry does not re-process (does not overwrite the original
     * {@code checkedInAt}) but reports {@code alreadyCheckedIn=true} so the caller can distinguish
     * it from a fresh confirmation.
     */
    public CheckInResult confirm(Long cachedEntryId) {
        Optional<CachedEntry> found = cachedEntryRepository.findById(cachedEntryId);
        if (found.isEmpty()) {
            return new CheckInResult.NotFound();
        }
        CachedEntry entry = found.get();
        if (entry.isCheckedIn()) {
            return new CheckInResult.Success(entry, true);
        }
        entry.setCheckedIn(true);
        entry.setCheckedInAt(Instant.now());
        cachedEntryRepository.save(entry);
        return new CheckInResult.Success(entry, false);
    }
}
