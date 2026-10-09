package dev.monkeypatch.rctiming.domain.race;

/** What a referee's penalty takes away: laps, or time added to the driver's total. */
public enum PenaltyType {
    /** Whole laps taken off, straight away in live timing. */
    LAP,
    /** Seconds added to the total time when the result is worked out. */
    TIME
}
