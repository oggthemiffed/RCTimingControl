package dev.monkeypatch.rctiming.domain.event;

import dev.monkeypatch.rctiming.api.admin.dto.CreateEventRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EventDto;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateEventRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventStateMachineService stateMachineService;

    public EventService(EventRepository eventRepository,
                        EventStateMachineService stateMachineService) {
        this.eventRepository = eventRepository;
        this.stateMachineService = stateMachineService;
    }

    public EventDto create(CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.name());
        event.setEventDate(request.eventDate());
        event.setTrackId(request.trackId());
        event.setStatus(EventStatus.DRAFT);
        Instant now = Instant.now();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return EventDto.from(eventRepository.save(event));
    }

    public EventDto update(Long id, UpdateEventRequest request) {
        Event event = getEventOrThrow(id);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateTransitionException(
                "Event details can only be updated while in DRAFT status");
        }
        event.setName(request.name());
        event.setEventDate(request.eventDate());
        event.setTrackId(request.trackId());
        event.setUpdatedAt(Instant.now());
        return EventDto.from(eventRepository.save(event));
    }

    public EventDto transition(Long id, EventStatus targetStatus) {
        Event event = getEventOrThrow(id);
        stateMachineService.transition(event, targetStatus);
        event.setUpdatedAt(Instant.now());
        return EventDto.from(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public Event findByIdOrThrow(Long id) {
        return getEventOrThrow(id);
    }

    private Event getEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));
    }
}
