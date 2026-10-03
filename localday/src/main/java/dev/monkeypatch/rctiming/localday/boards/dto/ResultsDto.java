package dev.monkeypatch.rctiming.localday.boards.dto;

import dev.monkeypatch.rctiming.localday.race.dto.ScheduleEntryDto;

import java.util.List;

/**
 * Results for the most recently finished race, for anonymous board display. {@code race} is
 * nullable (nothing has finished yet); {@code results} is empty (never null) in that case.
 */
public record ResultsDto(ScheduleEntryDto race, List<RaceResultRowDto> results) {
}
