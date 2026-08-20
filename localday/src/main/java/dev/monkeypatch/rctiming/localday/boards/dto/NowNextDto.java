package dev.monkeypatch.rctiming.localday.boards.dto;

import dev.monkeypatch.rctiming.localday.race.dto.ScheduleEntryDto;

/**
 * The venue-monitor "now / next" summary: the race currently on track (if any), the next race
 * due up (if any), and the most recently completed race (if any). All three are independently
 * nullable — absence of any one is a normal state (idle before the first heat, idle between
 * rounds, idle after the last race), not an error.
 */
public record NowNextDto(ScheduleEntryDto currentRace, ScheduleEntryDto nextRace, ScheduleEntryDto lastCompletedRace) {
}
