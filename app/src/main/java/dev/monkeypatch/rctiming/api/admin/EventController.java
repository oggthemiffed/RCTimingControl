package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.api.admin.dto.CreateEventRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EventDetailDto;
import dev.monkeypatch.rctiming.api.admin.dto.EventDto;
import dev.monkeypatch.rctiming.api.admin.dto.GenerateRoundsRequest;
import dev.monkeypatch.rctiming.api.admin.dto.SeedFinalsRequest;
import dev.monkeypatch.rctiming.api.admin.dto.TransitionEventRequest;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateEventRequest;
import dev.monkeypatch.rctiming.domain.event.EventService;
import dev.monkeypatch.rctiming.domain.format.EventClassService;
import dev.monkeypatch.rctiming.query.event.AdminEventListDto;
import dev.monkeypatch.rctiming.query.event.AdminEventQueryService;
import dev.monkeypatch.rctiming.service.BumpUpSeedingService;
import dev.monkeypatch.rctiming.service.QualifyingStandingsService;
import dev.monkeypatch.rctiming.service.RoundGeneratorService;
import dev.monkeypatch.rctiming.service.dto.RoundGenerationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/events")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class EventController {

    private final EventService eventService;
    private final EventClassService eventClassService;
    private final AdminEventQueryService adminEventQueryService;
    private final RoundGeneratorService roundGeneratorService;
    private final BumpUpSeedingService bumpUpSeedingService;
    private final QualifyingStandingsService qualifyingStandingsService;

    public EventController(EventService eventService,
                           EventClassService eventClassService,
                           AdminEventQueryService adminEventQueryService,
                           RoundGeneratorService roundGeneratorService,
                           BumpUpSeedingService bumpUpSeedingService,
                           QualifyingStandingsService qualifyingStandingsService) {
        this.eventService = eventService;
        this.eventClassService = eventClassService;
        this.adminEventQueryService = adminEventQueryService;
        this.roundGeneratorService = roundGeneratorService;
        this.bumpUpSeedingService = bumpUpSeedingService;
        this.qualifyingStandingsService = qualifyingStandingsService;
    }

    @GetMapping
    public List<AdminEventListDto> listEvents() {
        return adminEventQueryService.listEvents();
    }

    @GetMapping("/{id}")
    public EventDetailDto getEvent(@PathVariable Long id) {
        return EventDetailDto.from(
            eventService.findByIdOrThrow(id),
            eventClassService.listClassesForEvent(id)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto createEvent(@RequestBody @Valid CreateEventRequest request) {
        return eventService.create(request);
    }

    @PutMapping("/{id}")
    public EventDto updateEvent(@PathVariable Long id,
                                 @RequestBody @Valid UpdateEventRequest request) {
        return eventService.update(id, request);
    }

    @PostMapping("/{id}/transition")
    public EventDto transitionEvent(@PathVariable Long id,
                                     @RequestBody @Valid TransitionEventRequest request) {
        return eventService.transition(id, request.targetStatus());
    }

    @PostMapping("/{id}/generate-rounds")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public ResponseEntity<Void> generateRounds(@PathVariable Long id,
                                               @Valid @RequestBody GenerateRoundsRequest req) {
        RoundGenerationRequest serviceReq = new RoundGenerationRequest(
                id,
                req.practiceRoundsCount(),
                req.qualifyingRoundsCount(),
                req.maxCarsPerHeat(),
                req.classFinalsConfigs().stream()
                   .map(c -> new RoundGenerationRequest.ClassFinalsConfig(
                           c.eventClassId(), c.finalsCount(), c.carsPerFinal(), c.bumpCount()))
                   .toList()
        );
        roundGeneratorService.generate(serviceReq);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/seed-finals")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public ResponseEntity<Void> seedFinals(@PathVariable Long id,
                                           @Valid @RequestBody SeedFinalsRequest req) {
        List<QualifyingStandingsService.QualifyingResult> results = req.qualifyingResults().stream()
                .map(r -> new QualifyingStandingsService.QualifyingResult(
                        r.entryId(), r.bestLapMs(), r.lapsCompleted()))
                .toList();
        List<Long> standings = qualifyingStandingsService.recalculateStandings(req.eventClassId(), results);
        bumpUpSeedingService.seedFinals(req.eventClassId(), standings,
                req.finalsCount(), req.carsPerFinal(), req.bumpCount());
        return ResponseEntity.noContent().build();
    }
}
