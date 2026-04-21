package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventStatus;

import java.time.LocalDate;
import java.util.List;

public record EventDetailDto(
        Long id,
        String name,
        LocalDate eventDate,
        EventStatus status,
        Long trackId,
        List<EventClassDto> classes   // EventClassDto created in Task 2
) {
    public static EventDetailDto from(Event e, List<EventClassDto> classes) {
        return new EventDetailDto(e.getId(), e.getName(), e.getEventDate(), e.getStatus(), e.getTrackId(), classes);
    }
}
