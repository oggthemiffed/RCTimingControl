package dev.monkeypatch.rctiming.localday.boards.dto;

/** One row of a finished race's results, resolved for anonymous board display. */
public record RaceResultRowDto(Long entryId, String racerName, String transponderNumber,
                                int position, int lapsCompleted, Long bestLapMs) {
}
