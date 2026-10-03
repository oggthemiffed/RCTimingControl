package dev.monkeypatch.rctiming.localday.timing.dto;

/**
 * Broadcast on {@code /topic/race/{scheduleId}/marshal} when a marshal lap adjustment is
 * recorded. Adapted from the cloud's {@code app/.../timing/dto/MarshalAdjustmentDto.java} —
 * {@code raceId} renamed to {@code scheduleId} to match this module's local-ID naming
 * ({@code CachedScheduleEntry.id}, {@code CachedEntry.id}).
 */
public record MarshalAdjustmentDto(
        long scheduleId,
        long entryId,
        String transponderNumber,
        int lapDelta,
        String actingUserName,
        long adjustedAtEpochMs
) {
}
