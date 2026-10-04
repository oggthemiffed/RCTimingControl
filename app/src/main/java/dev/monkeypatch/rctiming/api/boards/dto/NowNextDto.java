package dev.monkeypatch.rctiming.api.boards.dto;

/**
 * What the now/next board shows for one event (L12, ported from localday). Any race may be
 * null: before the first heat only {@code nextRace} is set, after the last only
 * {@code lastCompletedRace}. {@code eventId} is null when no event is racing.
 */
public record NowNextDto(
        Long eventId,
        String eventName,
        BoardRaceDto currentRace,
        BoardRaceDto nextRace,
        BoardRaceDto lastCompletedRace
) {}
