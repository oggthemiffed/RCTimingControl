package dev.monkeypatch.rctiming.api.boards.dto;

import dev.monkeypatch.rctiming.api.racecontrol.dto.ResultSnapshotDto;

import java.util.List;

/**
 * The results board: the event's last finished race and its result rows (L12). {@code race}
 * is null, and {@code results} empty, until a race has finished. {@code eventId} follows the
 * same resolution as {@link NowNextDto}.
 */
public record ResultsBoardDto(
        Long eventId,
        String eventName,
        BoardRaceDto race,
        List<ResultSnapshotDto.ResultRow> results
) {}
