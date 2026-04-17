package dev.monkeypatch.rctiming.query.event;

import java.time.LocalDate;

public record EventScheduleDto(
        Long id,
        String name,
        LocalDate eventDate,
        EntryAvailability entryAvailability) {

    public enum EntryAvailability {
        ENTRY_OPEN,
        ENTRY_NOT_YET_OPEN,
        ENTRY_CLOSED
    }
}
