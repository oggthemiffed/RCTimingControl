package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventStatus;

import java.time.LocalDate;

public record EventDto(
        Long id,
        String name,
        LocalDate eventDate,
        EventStatus status,
        Long trackId
) {
    public static EventDto from(Event e) {
        return new EventDto(e.getId(), e.getName(), e.getEventDate(), e.getStatus(), e.getTrackId());
    }
}
