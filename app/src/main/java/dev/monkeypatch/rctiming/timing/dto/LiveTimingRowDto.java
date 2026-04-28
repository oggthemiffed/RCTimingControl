package dev.monkeypatch.rctiming.timing.dto;

public record LiveTimingRowDto(
        long entryId,
        String driverName,
        int position,
        int lapsCompleted,
        long lastPassingTimeMs,
        Long lastLapMs,
        Long bestLapMs,
        Long avgLapMs,
        int lapsDown,
        int intervalLapsDown,
        Long gapToLeaderMs,
        Long gapToAheadMs
) {
}
