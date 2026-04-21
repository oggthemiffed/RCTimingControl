package dev.monkeypatch.rctiming.domain.format;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.admin.dto.AddEventClassRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EventClassDto;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateEventClassOverrideRequest;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class EventClassService {

    private final EventClassRepository eventClassRepository;
    private final EventRepository eventRepository;
    private final RaceFormatTemplateRepository templateRepository;
    private final RacingClassRepository racingClassRepository;
    private final ObjectMapper objectMapper;

    public EventClassService(EventClassRepository eventClassRepository,
                             EventRepository eventRepository,
                             RaceFormatTemplateRepository templateRepository,
                             RacingClassRepository racingClassRepository,
                             ObjectMapper objectMapper) {
        this.eventClassRepository = eventClassRepository;
        this.eventRepository = eventRepository;
        this.templateRepository = templateRepository;
        this.racingClassRepository = racingClassRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<EventClassDto> listClassesForEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found: " + eventId);
        }
        return eventClassRepository.findAll().stream()
                .filter(ec -> eventId.equals(ec.getEventId()))
                .map(EventClassDto::from)
                .toList();
    }

    public EventClassDto addClassToEvent(Long eventId, AddEventClassRequest request) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found: " + eventId);
        }
        if (!racingClassRepository.existsById(request.racingClassId())) {
            throw new EntityNotFoundException("Racing class not found: " + request.racingClassId());
        }
        RaceFormatTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new EntityNotFoundException("Template not found: " + request.templateId()));

        // Snapshot via ObjectMapper deep-copy — same pattern as RaceFormatService.assignTemplateToEventClass
        RaceFormatConfig snapshot = objectMapper.convertValue(template.getConfig(), RaceFormatConfig.class);

        EventClass ec = new EventClass();
        ec.setEventId(eventId);
        ec.setRacingClassId(request.racingClassId());
        ec.setTemplate(template);
        ec.setConfigSnapshot(snapshot);
        ec.setConfigOverride(null);
        Instant now = Instant.now();
        ec.setCreatedAt(now);
        ec.setUpdatedAt(now);
        return EventClassDto.from(eventClassRepository.save(ec));
    }

    public EventClassDto updateOverrides(Long classId, UpdateEventClassOverrideRequest request) {
        EventClass ec = getEventClassOrThrow(classId);
        Map<String, Object> override = request.override() == null || request.override().isEmpty()
                ? null
                : new HashMap<>(request.override());
        ec.setConfigOverride(override);
        ec.setUpdatedAt(Instant.now());
        return EventClassDto.from(eventClassRepository.save(ec));
    }

    /**
     * EVENT-06: Assigns the same non-null combined_race_group to every supplied event class
     * so they race together but score separately.
     */
    public List<EventClassDto> combineClasses(Long eventId, List<Long> eventClassIds) {
        if (eventClassIds == null || eventClassIds.size() < 2) {
            throw new IllegalArgumentException("At least 2 event class ids required to combine");
        }
        // Generate a shared group id — uses current time ms for monotonic uniqueness per JVM run
        long groupId = Instant.now().toEpochMilli();

        List<EventClassDto> result = new ArrayList<>();
        for (Long id : eventClassIds) {
            EventClass ec = getEventClassOrThrow(id);
            if (ec.getEventId() == null || !ec.getEventId().equals(eventId)) {
                throw new IllegalArgumentException("EventClass " + id + " does not belong to event " + eventId);
            }
            ec.setCombinedRaceGroup(groupId);
            ec.setUpdatedAt(Instant.now());
            result.add(EventClassDto.from(eventClassRepository.save(ec)));
        }
        return result;
    }

    /** Returns the effective config = snapshot + override merge, for use by Phase 4 race control. */
    @Transactional(readOnly = true)
    public RaceFormatConfig getEffectiveConfig(Long classId) {
        EventClass ec = getEventClassOrThrow(classId);
        if (ec.getConfigOverride() == null || ec.getConfigOverride().isEmpty()) {
            return ec.getConfigSnapshot();
        }
        Map<String, Object> snapshotMap = objectMapper.convertValue(
                ec.getConfigSnapshot(), new TypeReference<Map<String, Object>>() {});
        snapshotMap.putAll(ec.getConfigOverride());
        return objectMapper.convertValue(snapshotMap, RaceFormatConfig.class);
    }

    private EventClass getEventClassOrThrow(Long id) {
        return eventClassRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event class not found: " + id));
    }
}
