package dev.monkeypatch.rctiming.api.localday;

import dev.monkeypatch.rctiming.api.localday.dto.PreCacheCredentialDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheEntryDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheEventDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheInstanceSecretDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheRequest;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheResponseDto;
import dev.monkeypatch.rctiming.api.localday.dto.PreCacheScheduleDto;
import dev.monkeypatch.rctiming.domain.localday.PreCacheService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/localday/events/{eventId}/pre-cache")
@PreAuthorize("hasAnyRole('ADMIN','RACE_DIRECTOR')")
public class PreCacheController {

    private final PreCacheService preCacheService;

    public PreCacheController(PreCacheService preCacheService) {
        this.preCacheService = preCacheService;
    }

    @PostMapping
    public PreCacheResponseDto preCache(@PathVariable Long eventId, @RequestBody PreCacheRequest request) {
        PreCacheService.PreCacheResult result = preCacheService.buildPreCache(eventId, request.instanceId());

        PreCacheEventDto eventDto = new PreCacheEventDto(
                result.event().getId(), result.event().getName(), result.event().getEventDate());

        var entries = result.entries().stream()
                .map(e -> new PreCacheEntryDto(e.cloudEntryId(), e.transponderNumber(), e.racerName(), e.carName(), e.className()))
                .toList();

        var schedule = result.schedule().stream()
                .map(s -> new PreCacheScheduleDto(s.cloudRaceId(), s.roundNumber(), s.heatNumber(), s.sequence(),
                        s.className(), s.finalLetter(), s.status()))
                .toList();

        var officialCredentials = result.officialCredentials().stream()
                .map(c -> new PreCacheCredentialDto(c.cloudUserId(), c.officialName(), c.pin()))
                .toList();

        PreCacheInstanceSecretDto instanceSecret =
                new PreCacheInstanceSecretDto(result.instanceId(), result.instanceSecret());

        return new PreCacheResponseDto(eventDto, entries, schedule, officialCredentials, instanceSecret, result.generation());
    }
}
