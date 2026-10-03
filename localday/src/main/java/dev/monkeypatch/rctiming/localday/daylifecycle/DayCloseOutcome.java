package dev.monkeypatch.rctiming.localday.daylifecycle;

/**
 * Outcome of a {@link DayLifecycleService#close} attempt. Sealed so
 * {@link DayLifecycleController} can exhaustively map each variant to the fixed API contract's
 * {@code "closed"} / {@code "pending"} response shape with a switch expression.
 */
public sealed interface DayCloseOutcome {

    record Closed() implements DayCloseOutcome {
    }

    /** AE2: a final sync is still pending — the day is not actually closed, nothing was purged. */
    record Pending(int pendingSyncCount) implements DayCloseOutcome {
    }
}
