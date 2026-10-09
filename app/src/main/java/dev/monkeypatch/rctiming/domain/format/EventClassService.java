package dev.monkeypatch.rctiming.domain.format;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.admin.dto.AddEventClassRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EventClassDto;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateEventClassOverrideRequest;
import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final AuditService audit;

    public EventClassService(EventClassRepository eventClassRepository,
                             EventRepository eventRepository,
                             RaceFormatTemplateRepository templateRepository,
                             RacingClassRepository racingClassRepository,
                             ObjectMapper objectMapper,
                             AuditService audit) {
        this.eventClassRepository = eventClassRepository;
        this.eventRepository = eventRepository;
        this.templateRepository = templateRepository;
        this.racingClassRepository = racingClassRepository;
        this.objectMapper = objectMapper;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<EventClassDto> listClassesForEvent(Long eventId) {
        eventRepository.requireExists(eventId);
        return eventClassRepository.findAll().stream()
                .filter(ec -> eventId.equals(ec.getEventId()))
                .map(EventClassDto::from)
                .toList();
    }

    public EventClassDto addClassToEvent(Actor actor, Long eventId, AddEventClassRequest request) {
        Event event = eventRepository.getOrThrow(eventId);
        RacingClass racingClass = racingClassRepository.getOrThrow(request.racingClassId());
        RaceFormatTemplate template = templateRepository.getOrThrow(request.templateId());

        // Snapshot via ObjectMapper deep-copy — same pattern as RaceFormatService.assignTemplateToEventClass
        RaceFormatConfig snapshot = objectMapper.convertValue(template.getConfig(), RaceFormatConfig.class);

        EventClass ec = new EventClass();
        ec.setEventId(eventId);
        ec.setRacingClassId(request.racingClassId());
        ec.setTemplateId(template.getId());
        ec.setConfigSnapshot(snapshot);
        ec.setConfigOverride(null);
        EventClass saved = eventClassRepository.save(ec);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("racingClassId", racingClass.getId());
        after.put("templateId", template.getId());
        after.put("templateName", template.getName());
        audit.entry(actor, "EVENT_CLASS_ADDED").entity("event_class", saved.getId()).event(eventId)
                .summary("Added " + racingClass.getName() + " to " + event.getName()
                        + " using the format " + template.getName())
                .after(after).record();
        return EventClassDto.from(saved);
    }

    public EventClassDto updateOverrides(Actor actor, Long classId, UpdateEventClassOverrideRequest request) {
        EventClass ec = getEventClassOrThrow(classId);
        Map<String, Object> before = ec.getConfigOverride();
        Map<String, Object> override = request.override() == null || request.override().isEmpty()
                ? null
                : new HashMap<>(request.override());
        ec.setConfigOverride(override);
        EventClass saved = eventClassRepository.save(ec);
        audit.entry(actor, "EVENT_CLASS_OVERRIDES_CHANGED").entity("event_class", classId).event(ec.getEventId())
                .summary((override == null ? "Cleared the format overrides of " : "Changed the format overrides of ")
                        + describe(ec))
                .before(before).after(override).record();
        return EventClassDto.from(saved);
    }

    /**
     * EVENT-06: Assigns the same non-null combined_race_group to every supplied event class
     * so they race together but score separately.
     */
    public List<EventClassDto> combineClasses(Actor actor, Long eventId, List<Long> eventClassIds) {
        if (eventClassIds == null || eventClassIds.size() < 2) {
            throw new IllegalArgumentException("At least 2 event class ids required to combine");
        }
        // Generate a shared group id — uses current time ms for monotonic uniqueness per JVM run
        long groupId = Instant.now().toEpochMilli();

        List<EventClassDto> result = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Long id : eventClassIds) {
            EventClass ec = getEventClassOrThrow(id);
            if (ec.getEventId() == null || !ec.getEventId().equals(eventId)) {
                throw new IllegalArgumentException("EventClass " + id + " does not belong to event " + eventId);
            }
            ec.setCombinedRaceGroup(groupId);
            result.add(EventClassDto.from(eventClassRepository.save(ec)));
            names.add(describe(ec));
        }
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("eventClassIds", eventClassIds);
        after.put("combinedRaceGroup", groupId);
        audit.entry(actor, "EVENT_CLASSES_COMBINED").entity("event", eventId).event(eventId)
                .summary("Combined " + String.join(", ", names) + " to race together and score separately")
                .after(after).record();
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

    /** The class's name, for an audit summary. */
    private String describe(EventClass ec) {
        return racingClassRepository.findById(ec.getRacingClassId()).map(RacingClass::getName)
                .orElse("class " + ec.getRacingClassId());
    }

    private EventClass getEventClassOrThrow(Long id) {
        return eventClassRepository.getOrThrow(id);
    }
}
