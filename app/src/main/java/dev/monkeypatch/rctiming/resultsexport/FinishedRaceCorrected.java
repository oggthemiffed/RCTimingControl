package dev.monkeypatch.rctiming.resultsexport;

/** Published when a referee or marshal changes a race that has already finished (#27). */
public record FinishedRaceCorrected(long raceId) {
}
