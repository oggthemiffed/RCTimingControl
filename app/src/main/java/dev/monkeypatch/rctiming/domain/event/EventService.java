package dev.monkeypatch.rctiming.domain.event;

import dev.monkeypatch.rctiming.api.admin.dto.CreateEventRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EventDto;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateEventRequest;
import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventStateMachineService stateMachineService;
    private final ApplicationEventPublisher events;
    private final AuditService audit;

    public EventService(EventRepository eventRepository,
                        EventStateMachineService stateMachineService,
                        ApplicationEventPublisher events,
                        AuditService audit) {
        this.eventRepository = eventRepository;
        this.stateMachineService = stateMachineService;
        this.events = events;
        this.audit = audit;
    }

    public EventDto create(Actor actor, CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.name());
        event.setEventDate(request.eventDate());
        event.setTrackId(request.trackId());
        event.setStatus(EventStatus.DRAFT);
        Instant now = Instant.now();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        Event saved = eventRepository.save(event);
        audit.entry(actor, "EVENT_CREATED").entity("event", saved.getId()).event(saved.getId())
                .summary("Created event " + saved.getName())
                .after(detailsOf(saved)).record();
        return EventDto.from(saved);
    }

    public EventDto update(Actor actor, Long id, UpdateEventRequest request) {
        Event event = getEventOrThrow(id);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateTransitionException(
                "Event details can only be updated while in DRAFT status");
        }
        Map<String, Object> before = detailsOf(event);
        event.setName(request.name());
        event.setEventDate(request.eventDate());
        event.setTrackId(request.trackId());
        event.setUpdatedAt(Instant.now());
        Event saved = eventRepository.save(event);
        audit.entry(actor, "EVENT_UPDATED").entity("event", id).event(id)
                .summary("Changed the details of event " + saved.getName())
                .before(before).after(detailsOf(saved)).record();
        return EventDto.from(saved);
    }

    public EventDto transition(Actor actor, Long id, EventStatus targetStatus) {
        Event event = getEventOrThrow(id);
        EventStatus from = event.getStatus();
        stateMachineService.transition(event, targetStatus);
        event.setUpdatedAt(Instant.now());
        Event saved = eventRepository.save(event);
        boolean completed = targetStatus == EventStatus.COMPLETED;
        // Closing the day starts a backup and a results export that name nobody, so the reason is recorded here
        audit.entry(actor, "EVENT_" + targetStatus).entity("event", id).event(id)
                .summary("Moved event " + saved.getName() + " from " + from + " to " + targetStatus
                        + (completed ? ", which starts a backup and a results export" : ""))
                .before(from).after(targetStatus).record();
        if (completed) {
            events.publishEvent(new EventCompleted(event.getId()));
        }
        return EventDto.from(saved);
    }

    @Transactional(readOnly = true)
    public Event findByIdOrThrow(Long id) {
        return getEventOrThrow(id);
    }

    private Event getEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));
    }

    private static Map<String, Object> detailsOf(Event e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", e.getName());
        m.put("eventDate", e.getEventDate() == null ? null : e.getEventDate().toString());
        m.put("trackId", e.getTrackId());
        return m;
    }
}
