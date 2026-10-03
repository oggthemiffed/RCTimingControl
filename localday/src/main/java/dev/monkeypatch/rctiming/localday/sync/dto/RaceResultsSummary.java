package dev.monkeypatch.rctiming.localday.sync.dto;

import java.util.List;

public record RaceResultsSummary(Long cloudRaceId, String className, String finalLetter,
                                  List<ResultRow> rows) {
}
