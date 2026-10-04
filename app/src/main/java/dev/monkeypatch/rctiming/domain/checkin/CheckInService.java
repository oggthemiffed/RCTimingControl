package dev.monkeypatch.rctiming.domain.checkin;

import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * Check-in at the desk on the day (L11).
 * Check-in here is authoritative; RaceHub's arrival mark is never changed by it.
 */
@Service
public class CheckInService {

    private final EntryRepository entryRepository;

    public CheckInService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    /**
     * Checks an entry in. Idempotent: a repeat confirm keeps the first check-in time and
     * reports {@code alreadyCheckedIn}. The entry row is locked, so two desks confirming at
     * once cannot both record a first check-in.
     */
    @Transactional
    public CheckInResult confirm(long eventId, long entryId, Long actingUserId) {
        Entry entry = entryRepository.findByIdForUpdate(entryId).orElse(null);
        if (entry == null || !Objects.equals(entry.getEventId(), eventId)) {
            return new CheckInResult.NotFound();
        }
        if (entry.getStatus() == EntryStatus.WITHDRAWN) {
            return new CheckInResult.Withdrawn();
        }
        if (entry.getCheckedInAt() != null) {
            return new CheckInResult.Success(entry, true);
        }
        Instant now = Instant.now();
        entry.setCheckedInAt(now);
        entry.setCheckedInByUserId(actingUserId);
        entry.setUpdatedAt(now);
        return new CheckInResult.Success(entryRepository.save(entry), false);
    }
}
