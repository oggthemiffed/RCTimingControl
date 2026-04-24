package dev.monkeypatch.rctiming.timing.dto;

public record MarshalAdjustmentDto(
        long raceId,
        long entryId,
        String transponderNumber,
        int lapDelta,
        String actingUserName,
        long adjustedAtEpochMs
) {
}
