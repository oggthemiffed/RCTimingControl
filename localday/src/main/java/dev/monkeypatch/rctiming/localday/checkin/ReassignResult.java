package dev.monkeypatch.rctiming.localday.checkin;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;

/**
 * Outcome of {@link TransponderReassignmentService#reassign}. {@code Success.oldTransponderNumber}
 * carries the pre-reassignment value for the response/audit trail.
 */
public sealed interface ReassignResult {

    record Success(CachedEntry entry, String oldTransponderNumber) implements ReassignResult {
    }

    record EntryNotFound() implements ReassignResult {
    }

    record TransponderAlreadyAssigned() implements ReassignResult {
    }
}
