package dev.monkeypatch.rctiming.localday.sync.dto;

import java.util.List;

public record ClassStanding(String className, List<StandingRow> rows) {
}
