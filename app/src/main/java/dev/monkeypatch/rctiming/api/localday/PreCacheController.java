package dev.monkeypatch.rctiming.api.localday;

import dev.monkeypatch.rctiming.api.localday.dto.PreCacheCredentialDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheEntryDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheEventDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheInstanceSecretDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheRequest;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheResponseDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheScheduleDto;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.localday.PreCacheService;
import dev.monkeypatch.rctiming.query.localday.PreCacheQuery;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Composes the pre-cache payload from the write-side {@link PreCacheService} (officials'
 * credentials + instance secret) and the read-side {@link PreCacheQuery} (entries/schedule via jOOQ).
 */
@RestController
@RequestMapping("/api/v1/localday/events/{eventId}/pre-cache")
@PreAuthorize("hasAnyRole('ADMIN','RACE_DIRECTOR')")
public class PreCacheController {

    private final PreCacheService preCacheService;
    private final PreCacheQuery preCacheQuery;
    private final EventRepository eventRepository;

    public PreCacheController(PreCacheService preCacheService,
                               PreCacheQuery preCacheQuery,
                               EventRepository eventRepository) {
        this.preCacheService = preCacheService;
        this.preCacheQuery = preCacheQuery;
        this.eventRepository = eventRepository;
    }

    @PostMapping
    public PreCacheResponseDto preCache(@PathVariable Long eventId, @RequestBody PreCacheRequest request) {
        if (request.instanceId() == null || request.instanceId().isBlank()) {
            throw new IllegalArgumentException("instanceId is required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        var entryRows = preCacheQuery.confirmedEntries(eventId);
        var scheduleRows = preCacheQuery.schedule(eventId);
        PreCacheService.MintResult mintResult = preCacheService.mintCredentialsAndSecret(eventId, request.instanceId());

        PreCacheEventDto eventDto = new PreCacheEventDto(event.getId(), event.getName(), event.getEventDate());

        var entries = entryRows.stream()
                .map(e -> new PreCacheEntryDto(e.entryId(), e.transponderNumber(), e.racerName(), e.carName(), e.className()))
                .toList();

        var schedule = scheduleRows.stream()
                .map(s -> new PreCacheScheduleDto(s.raceId(), s.roundNumber(), s.heatNumber(), s.sequence(),
                        s.className(), s.finalLetter(), s.status()))
                .toList();

        var officialCredentials = mintResult.officialCredentials().stream()
                .map(c -> new PreCacheCredentialDto(c.cloudUserId(), c.officialName(), c.pin()))
                .toList();

        PreCacheInstanceSecretDto instanceSecret =
                new PreCacheInstanceSecretDto(mintResult.instanceId(), mintResult.instanceSecret());

        return new PreCacheResponseDto(eventDto, entries, schedule, officialCredentials, instanceSecret);
    }
}
