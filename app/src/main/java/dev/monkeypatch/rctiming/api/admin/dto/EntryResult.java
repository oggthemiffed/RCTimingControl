package dev.monkeypatch.rctiming.api.admin.dto;

import java.util.List;

public record EntryResult(EntryDto entry, List<String> warnings) {
}
