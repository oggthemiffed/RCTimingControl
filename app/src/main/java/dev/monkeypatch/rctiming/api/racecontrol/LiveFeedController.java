package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.livefeed.LiveFeedPublisher;
import dev.monkeypatch.rctiming.timing.dto.LiveFeedStatusDto;
import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The live feed for remote viewers (#28): its connection to the relay, and whether an event's races are sent.
 */
@RestController
@RequestMapping("/api/v1/race-control")
@PreAuthorize("hasAnyRole('RACE_DIRECTOR','REFEREE','ADMIN')")
public class LiveFeedController {

    private final LiveFeedPublisher publisher;
    private final EventRepository eventRepository;

    public LiveFeedController(LiveFeedPublisher publisher, EventRepository eventRepository) {
        this.publisher = publisher;
        this.eventRepository = eventRepository;
    }

    /** @param enabled whether the event's races are sent to the relay while they run */
    public record LiveFeedSettingDto(@NotNull Boolean enabled) {
    }

    @GetMapping("/live-feed/status")
    public LiveFeedStatusDto status() {
        return publisher.status();
    }

    @GetMapping("/events/{eventId}/live-feed")
    @Transactional(readOnly = true)
    public LiveFeedSettingDto setting(@PathVariable long eventId) {
        return new LiveFeedSettingDto(event(eventId).isLiveFeedEnabled());
    }

    @PutMapping("/events/{eventId}/live-feed")
    @PreAuthorize("hasAnyRole('RACE_DIRECTOR','ADMIN')")
    @Transactional
    public LiveFeedSettingDto update(@PathVariable long eventId, @Valid @RequestBody LiveFeedSettingDto request) {
        Event event = event(eventId);
        event.setLiveFeedEnabled(request.enabled());
        eventRepository.save(event);
        return new LiveFeedSettingDto(event.isLiveFeedEnabled());
    }

    private Event event(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
    }
}
