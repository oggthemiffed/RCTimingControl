package dev.monkeypatch.rctiming.api.racer.dto;

import dev.monkeypatch.rctiming.domain.entry.Entry;

import java.time.Instant;

public record EntryDto(
        Long id,
        Long userId,
        Long eventId,
        Long eventClassId,
        Long carId,
        Long transponderId,
        String transponderNumberSnapshot,
        String status,
        Instant submittedAt,
        Instant confirmedAt,
        Instant withdrawnAt) {

    public static EntryDto from(Entry e) {
        return new EntryDto(
                e.getId(),
                e.getUserId(),
                e.getEventId(),
                e.getEventClassId(),
                e.getCarId(),
                e.getTransponderId(),
                e.getTransponderNumberSnapshot(),
                e.getStatus().name(),
                e.getSubmittedAt(),
                e.getConfirmedAt(),
                e.getWithdrawnAt());
    }
}
