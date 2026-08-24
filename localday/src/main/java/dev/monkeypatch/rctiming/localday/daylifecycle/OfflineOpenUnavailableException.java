package dev.monkeypatch.rctiming.localday.daylifecycle;

/**
 * Thrown by {@link DayLifecycleService#open} when an offline open is attempted (no email/password
 * given, or the online attempt fell back here) but there is no earlier successful pre-cache/open
 * for the requested event to fall back to — nothing cached, or the cached day belongs to a
 * different event.
 */
public class OfflineOpenUnavailableException extends RuntimeException {
    public OfflineOpenUnavailableException(String message) {
        super(message);
    }
}
