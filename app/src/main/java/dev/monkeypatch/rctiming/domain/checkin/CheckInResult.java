package dev.monkeypatch.rctiming.domain.checkin;

import dev.monkeypatch.rctiming.domain.entry.Entry;

/** Outcome of confirming a check-in at the desk (L11). */
public sealed interface CheckInResult {

    /** The entry is checked in. {@code alreadyCheckedIn} is true when it was before this call. */
    record Success(Entry entry, boolean alreadyCheckedIn) implements CheckInResult {}

    /** No entry with that id in this event. */
    record NotFound() implements CheckInResult {}

    /** The entry was withdrawn, so it cannot check in. */
    record Withdrawn() implements CheckInResult {}
}
