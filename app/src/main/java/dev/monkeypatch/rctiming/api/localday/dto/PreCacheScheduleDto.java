package dev.monkeypatch.rctiming.api.localday.dto;

public record PreCacheScheduleDto(Long cloudRaceId, int roundNumber, int heatNumber, int sequence,
                                   String className, String finalLetter, String status) {}
