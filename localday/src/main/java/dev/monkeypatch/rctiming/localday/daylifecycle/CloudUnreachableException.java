package dev.monkeypatch.rctiming.localday.daylifecycle;

/**
 * Thrown by {@link PreCacheClient} when a call to the cloud could not even reach the server —
 * connection refused, DNS failure, connect/read timeout. Distinct from {@link CloudRequestException}
 * (a reachable cloud that rejected the request), because callers treat "no connectivity" very
 * differently from "the cloud said no": {@link DayLifecycleService#open} falls back to the
 * offline path only on this exception, never on a {@link CloudRequestException}.
 */
public class CloudUnreachableException extends RuntimeException {
    public CloudUnreachableException(String message, Throwable cause) {
        super(message, cause);
    }
}
