package dev.monkeypatch.rctiming.localday.race.dto;

import dev.monkeypatch.rctiming.localday.timing.dto.LiveTimingRowDto;

import java.util.List;

/** Point-in-time live position snapshot for a race. An empty {@code rows} list is normal pre-race state. */
public record LiveSnapshotDto(long scheduleId, List<LiveTimingRowDto> rows) {
}
