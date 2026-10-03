package dev.monkeypatch.rctiming.query.localday;

public record PreCacheEntryRow(
        Long entryId,
        String transponderNumber,
        String racerName,
        String carName,
        String className
) {}
