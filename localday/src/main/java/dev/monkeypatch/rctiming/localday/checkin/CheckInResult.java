package dev.monkeypatch.rctiming.localday.checkin;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;

/**
 * Outcome of {@link CheckInService#confirm(Long)}. {@code Success.alreadyCheckedIn} distinguishes
 * a fresh confirmation from a repeat confirmation of an already-checked-in entry, so the caller
 * can report "already checked in" distinctly rather than silently re-showing generic success.
 */
public sealed interface CheckInResult {

    record Success(CachedEntry entry, boolean alreadyCheckedIn) implements CheckInResult {
    }

    record NotFound() implements CheckInResult {
    }
}
