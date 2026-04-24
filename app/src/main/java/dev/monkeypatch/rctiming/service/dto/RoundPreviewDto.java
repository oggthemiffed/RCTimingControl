package dev.monkeypatch.rctiming.service.dto;

import java.util.List;

/**
 * Preview row for a single race that the round generator would create.
 *
 * @param sequenceInEvent absolute run-order position in the event (1-based)
 * @param typeLabel       human-readable label e.g. "Practice 1", "Qualifying 2", "A Final"
 * @param roundNumber     round number within its type
 * @param className       display name of the EventClass (null if unknown)
 * @param heatNumber      which split within the class for this round
 * @param finalLetter     null for practice/qualifier; "A", "B", "C" ... for finals
 * @param driverNames     ordered driver names for this heat/final (by grid position)
 */
public record RoundPreviewDto(
        int sequenceInEvent,
        String typeLabel,
        int roundNumber,
        String className,
        int heatNumber,
        String finalLetter,
        List<String> driverNames
) {}
