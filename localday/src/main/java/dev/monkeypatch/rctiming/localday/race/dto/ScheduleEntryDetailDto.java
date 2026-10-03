package dev.monkeypatch.rctiming.localday.race.dto;

import java.util.List;

/** A single race/heat plus its resolved starting grid. */
public record ScheduleEntryDetailDto(ScheduleEntryDto race, List<GridEntryDto> grid) {
}
