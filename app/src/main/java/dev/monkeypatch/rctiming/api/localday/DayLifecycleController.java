package dev.monkeypatch.rctiming.api.localday;

import dev.monkeypatch.rctiming.api.localday.dto.CloseDayRequest;
import dev.monkeypatch.rctiming.api.localday.dto.CloseDayResponseDto;
import dev.monkeypatch.rctiming.api.localday.dto.EventLockStatusDto;
import dev.monkeypatch.rctiming.api.localday.dto.OpenDayRequest;
import dev.monkeypatch.rctiming.domain.localday.DayLifecycleService;
import dev.monkeypatch.rctiming.domain.localday.EventSyncGenerationRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/localday/events/{eventId}/lifecycle")
@PreAuthorize("hasAnyRole('ADMIN','RACE_DIRECTOR')")
public class DayLifecycleController {

    private final DayLifecycleService dayLifecycleService;
    private final EventSyncGenerationRepository eventSyncGenerationRepository;

    public DayLifecycleController(DayLifecycleService dayLifecycleService,
                                   EventSyncGenerationRepository eventSyncGenerationRepository) {
        this.dayLifecycleService = dayLifecycleService;
        this.eventSyncGenerationRepository = eventSyncGenerationRepository;
    }

    @PostMapping("/open")
    public EventLockStatusDto open(@PathVariable Long eventId, @RequestBody OpenDayRequest request) {
        var result = dayLifecycleService.open(eventId, request.instanceId());
        return EventLockStatusDto.from(eventId, Optional.of(result.lock()), result.generation());
    }

    @PostMapping("/close")
    public CloseDayResponseDto close(@PathVariable Long eventId, @RequestBody CloseDayRequest request) {
        String status = dayLifecycleService.close(eventId, request.syncComplete());
        return new CloseDayResponseDto(status);
    }

    @GetMapping("/lock-status")
    public EventLockStatusDto lockStatus(@PathVariable Long eventId) {
        long generation = currentGeneration(eventId);
        return EventLockStatusDto.from(eventId, dayLifecycleService.lockStatus(eventId), generation);
    }

    private long currentGeneration(Long eventId) {
        return eventSyncGenerationRepository.findById(eventId)
                .map(g -> g.getGeneration())
                .orElse(0L);
    }
}
