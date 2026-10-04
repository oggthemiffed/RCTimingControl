package dev.monkeypatch.rctiming.api.boards.dto;

/**
 * A race as the spectator boards show it (L12).
 *
 * @param label e.g. "Qualifying 2 — Stock Buggy — Heat 1" or "A Final — Stock Buggy"
 */
public record BoardRaceDto(
        long raceId,
        String label,
        String roundType,
        int roundNumber,
        String className,
        int heatNumber,
        String finalLetter,
        String status
) {}
