package dev.monkeypatch.rctiming.query.entry;

import java.time.Instant;
import java.time.LocalDate;

public record RacerEntryHistoryDto(
        Long entryId,
        Long eventId,
        String eventName,
        LocalDate eventDate,
        Long eventClassId,
        String transponderNumberSnapshot,
        String status,
        Instant submittedAt,
        Instant withdrawnAt) {
}
