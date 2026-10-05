package dev.monkeypatch.rctiming.resultsexport;

/** What made RCTC build a results export (#27). */
public enum ExportReason {
    RACE_FINISHED,
    /** A referee or marshal changed a race that had already finished. */
    CORRECTION,
    /** The event was marked completed. */
    DAY_CLOSE
}
