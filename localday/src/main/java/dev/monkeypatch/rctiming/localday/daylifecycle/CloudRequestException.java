package dev.monkeypatch.rctiming.localday.daylifecycle;

/**
 * Thrown by {@link PreCacheClient} when the cloud was reachable but rejected the request — a
 * 401 (bad login credentials, or an expired/invalid access token), 404 (unknown event), 409
 * (lifecycle conflict), or any other non-2xx response. Deliberately not conflated with
 * {@link CloudUnreachableException}: this is "the cloud said no", not "no connectivity", and
 * {@link DayLifecycleService#open} lets it propagate rather than falling back to the offline path.
 */
public class CloudRequestException extends RuntimeException {

    private final int statusCode;

    public CloudRequestException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
