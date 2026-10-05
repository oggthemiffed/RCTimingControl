package dev.monkeypatch.rctiming.timing.dto;

/**
 * A race's clock, for the boards and the streaming overlay (#29).
 *
 * @param elapsedMs   race time so far, not counting time stopped
 * @param durationMs  the race's length from its format, or null when it has none
 * @param remainingMs time left, never below 0, or null without a length
 * @param running     whether the clock is counting now; a viewer counts on from {@code elapsedMs} while it is
 */
public record RaceClockDto(long raceId, String status, long elapsedMs, Long durationMs, Long remainingMs,
                           boolean running) {
}
