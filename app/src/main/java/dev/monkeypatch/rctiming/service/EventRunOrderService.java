package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import dev.monkeypatch.rctiming.domain.StateConflictException;
import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.service.dto.RoundGenerationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The officials' commands that build an event's run order and its finals grids (#139). Each runs in one
 * transaction with its audit row, so the log shows who generated the rounds or seeded the finals.
 */
@Service
@Transactional
public class EventRunOrderService {

    private final RoundGeneratorService roundGeneratorService;
    private final BumpUpSeedingService bumpUpSeedingService;
    private final QualifyingStandingsService qualifyingStandingsService;
    private final EventRepository eventRepository;
    private final EventClassRepository eventClassRepository;
    private final RacingClassRepository racingClassRepository;
    private final AuditService audit;

    public EventRunOrderService(RoundGeneratorService roundGeneratorService,
                                BumpUpSeedingService bumpUpSeedingService,
                                QualifyingStandingsService qualifyingStandingsService,
                                EventRepository eventRepository,
                                EventClassRepository eventClassRepository,
                                RacingClassRepository racingClassRepository,
                                AuditService audit) {
        this.roundGeneratorService = roundGeneratorService;
        this.bumpUpSeedingService = bumpUpSeedingService;
        this.qualifyingStandingsService = qualifyingStandingsService;
        this.eventRepository = eventRepository;
        this.eventClassRepository = eventClassRepository;
        this.racingClassRepository = racingClassRepository;
        this.audit = audit;
    }

    public void generateRounds(Actor actor, RoundGenerationRequest request) {
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + request.eventId()));
        roundGeneratorService.generate(request);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("practiceRoundsCount", request.practiceRoundsCount());
        after.put("qualifyingRoundsCount", request.qualifyingRoundsCount());
        after.put("maxCarsPerHeat", request.maxCarsPerHeat());
        after.put("classFinalsConfigs", request.classFinalsConfigs());
        audit.entry(actor, "RUN_ORDER_GENERATED").entity("event", event.getId()).event(event.getId())
                .summary("Generated the run order for " + event.getName() + ": "
                        + request.practiceRoundsCount() + " practice and "
                        + request.qualifyingRoundsCount() + " qualifying rounds, up to "
                        + request.maxCarsPerHeat() + " cars a heat")
                .after(after).record();
    }

    /**
     * Seeds the class's finals from its qualifying results as stored when the heats finished. Refused while no
     * qualifying heat has finished, because seeding replaces the finals' grids.
     */
    public void seedFinals(Actor actor, Long eventId, Long eventClassId,
                           int finalsCount, int carsPerFinal, int bumpCount) {
        EventClass eventClass = eventClassRepository.findById(eventClassId)
                .filter(c -> c.getEventId().equals(eventId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Event class " + eventClassId + " not found in event " + eventId));
        List<Long> standings = qualifyingStandingsService.standingsFor(eventClassId);
        if (standings.isEmpty()) {
            throw new StateConflictException("No qualifying heat for this class has finished with a result yet");
        }
        bumpUpSeedingService.seedFinals(eventClassId, standings, finalsCount, carsPerFinal, bumpCount);
        String className = racingClassRepository.findById(eventClass.getRacingClassId())
                .map(RacingClass::getName).orElse("class " + eventClass.getRacingClassId());
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("finalsCount", finalsCount);
        after.put("carsPerFinal", carsPerFinal);
        after.put("bumpCount", bumpCount);
        after.put("qualifyingOrder", standings);
        audit.entry(actor, "FINALS_SEEDED").entity("event_class", eventClassId).event(eventClass.getEventId())
                .summary("Seeded the finals for " + className + " from " + standings.size()
                        + " qualifiers (" + finalsCount + " finals, " + carsPerFinal + " cars each, "
                        + bumpCount + " bumping up)")
                .after(after).record();
    }
}
