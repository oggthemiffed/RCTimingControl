package dev.monkeypatch.rctiming.query.entry;

import java.time.Instant;

public record AdminEntryDto(
        Long id,
        Long userId,          // racer login, null for competitor-only entries
        Long competitorId,
        String displayName,   // competitor display name (L5)
        String transponderNumber,
        String status,
        Instant submittedAt,
        Instant withdrawnAt) {
}
