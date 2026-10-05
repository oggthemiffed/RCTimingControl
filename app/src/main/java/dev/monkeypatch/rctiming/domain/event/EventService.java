package dev.monkeypatch.rctiming.domain.event;

import dev.monkeypatch.rctiming.api.admin.dto.CreateEventRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EventDto;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateEventRequest;
import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventStateMachineService stateMachineService;
    private final ApplicationEventPublisher events;

    public EventService(EventRepository eventRepository,
                        EventStateMachineService stateMachineService,
                        ApplicationEventPublisher events) {
        this.eventRepository = eventRepository;
        this.stateMachineService = stateMachineService;
        this.events = events;
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
        EventDto saved = EventDto.from(eventRepository.save(event));
        if (targetStatus == EventStatus.COMPLETED) {
            events.publishEvent(new EventCompleted(event.getId()));
        }
        return saved;
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
