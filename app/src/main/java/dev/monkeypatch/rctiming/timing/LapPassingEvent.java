package dev.monkeypatch.rctiming.timing;

/**
 * Published for each transponder passing the decoder reports.
 *
 * <p>{@code rtcTimeMicros} is UTC epoch microseconds. The RC-4 text protocol carries no absolute time, so
 * the decoder listener anchors it to the server clock (see {@code EpochAnchor}).
 * {@code raceId} is {@link #NO_RACE} when no race is running; practice sessions still use those passings.
 *
 * <p>Published by {@code DecoderListener} on the single timing thread (see {@code AsyncConfig}), so
 * listeners run in decoder order.
 */
public record LapPassingEvent(long raceId, String transponderNumber, long rtcTimeMicros) {

    /** The race id used when no race is running. */
    public static final long NO_RACE = 0L;
}
