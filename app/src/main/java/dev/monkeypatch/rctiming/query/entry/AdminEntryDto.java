package dev.monkeypatch.rctiming.query.entry;

import java.time.Instant;

public record AdminEntryDto(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String transponderNumber,
        String status,
        Instant submittedAt,
        Instant withdrawnAt) {
}
