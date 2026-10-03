package dev.monkeypatch.rctiming.localday.race.dto;

/**
 * One grid slot within a race's starting lineup. When {@code cachedEntryId} is {@code null} the
 * slot is an unfilled bump slot ({@code racerName}/{@code transponderNumber} are also null).
 */
public record GridEntryDto(Long cachedEntryId, String racerName, String transponderNumber,
                            Integer carNumber, Integer gridPosition, boolean bumped) {
}
