package dev.monkeypatch.rctiming.localday.timing.dto;

/**
 * Broadcast on {@code /topic/race/{scheduleId}/state} when a
 * {@code CachedScheduleEntry}'s {@code RaceState} transitions. Adapted from the cloud's
 * {@code app/.../timing/dto/RaceStateChangeDto.java} — {@code raceId} renamed to
 * {@code scheduleId} to match this module's local-ID naming ({@code CachedScheduleEntry.id}).
 */
public record RaceStateChangeDto(long scheduleId, String newStatus) {
}
