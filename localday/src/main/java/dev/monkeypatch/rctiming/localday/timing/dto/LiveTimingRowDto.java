package dev.monkeypatch.rctiming.localday.timing.dto;

/**
 * One row of a live-timing position snapshot, broadcast over
 * {@code /topic/race/{scheduleId}/timing}. Ported as-is (pure data shape) from the cloud's
 * {@code app/.../timing/dto/LiveTimingRowDto.java}.
 */
public record LiveTimingRowDto(
        long entryId,
        String driverName,
        int position,
        int lapsCompleted,
        long lastPassingTimeMs,
        Long lastLapMs,
        Long bestLapMs,
        Long avgLapMs,
        Long overallFastestLapMs,
        int lapsDown,
        int intervalLapsDown,
        Long gapToLeaderMs,
        Long gapToAheadMs
) {
}
