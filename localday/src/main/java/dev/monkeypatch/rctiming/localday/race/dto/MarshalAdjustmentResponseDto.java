package dev.monkeypatch.rctiming.localday.race.dto;

public record MarshalAdjustmentResponseDto(Long raceId, Long entryId, String transponderNumber,
                                            int lapDelta, String actingUserName) {
}
