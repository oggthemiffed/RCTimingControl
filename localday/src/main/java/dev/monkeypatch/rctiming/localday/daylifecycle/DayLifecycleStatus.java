package dev.monkeypatch.rctiming.localday.daylifecycle;

/**
 * Lifecycle status of this venue instance's event day (U10). {@link DayLifecycleService}
 * governs the transitions:
 * <pre>
 * NOT_SET_UP --preCache()--> PRE_CACHED --open()--> OPEN --close()--> CLOSED
 * </pre>
 * A mid-day pre-cache refresh while already {@code OPEN} deliberately does not downgrade status
 * back to {@code PRE_CACHED} — see {@link DayLifecycleService#preCache}.
 */
public enum DayLifecycleStatus {
    NOT_SET_UP,
    PRE_CACHED,
    OPEN,
    CLOSED
}
