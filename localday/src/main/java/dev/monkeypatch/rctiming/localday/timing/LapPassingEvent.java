package dev.monkeypatch.rctiming.localday.timing;

/**
 * Pure data record published via {@link org.springframework.context.ApplicationEventPublisher}
 * when a transponder passing is received. {@code rtcTimeMicros} is the epoch-anchored absolute
 * UTC microsecond timestamp produced by {@code :decoder-protocol}'s {@code EpochAnchor}.
 *
 * <p>Unlike the cloud's {@code app/.../timing/LapPassingEvent} (which uses a primitive
 * {@code long raceId} plus a {@code 0L} sentinel for "no active race"), this local version uses
 * a nullable {@link Long cachedScheduleId} — consistent with the {@code null}-over-sentinel
 * convention already established locally by U4's {@code CachedRaceEntry.cachedEntryId} (KD3).
 */
public record LapPassingEvent(Long cachedScheduleId, String transponderNumber, long rtcTimeMicros) {
}
