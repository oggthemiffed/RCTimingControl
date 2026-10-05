package dev.monkeypatch.rctiming.resultsexport;

/** Where a queued results export is (#27). */
public enum OutboxStatus {
    /** Waiting for its first send. */
    QUEUED,
    /** The last send failed; it is tried again later. */
    FAILED,
    /** RaceHub accepted it. */
    SENT,
    /** A newer export of the same event replaced it before it was sent. */
    SUPERSEDED
}
