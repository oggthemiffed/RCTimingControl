package dev.monkeypatch.rctiming.api.racer.dto;

import java.util.List;

public record EntryResult(EntryDto entry, List<String> warnings) {
}
