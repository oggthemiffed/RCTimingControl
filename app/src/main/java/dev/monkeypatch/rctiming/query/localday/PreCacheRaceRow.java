package dev.monkeypatch.rctiming.query.localday;

public record PreCacheRaceRow(
        Long raceId,
        int roundNumber,
        int heatNumber,
        int sequence,
        String className,
        String finalLetter,
        String status
) {}
