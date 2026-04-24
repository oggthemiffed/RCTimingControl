package dev.monkeypatch.rctiming.api.racecontrol.dto;

public record RunOrderItemDto(
        long raceId,
        int sequenceInEvent,
        int roundNumber,
        String roundType,       // "PRACTICE" | "QUALIFIER" | "FINAL"
        String className,
        int heatNumber,
        String finalLetter,     // null for practice/qualifier
        String status,
        int sequenceInRound
) {
}
