package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

public record CloudPreCacheScheduleDto(Long cloudRaceId, int roundNumber, int heatNumber, int sequence,
                                        String className, String finalLetter, String status) {}
