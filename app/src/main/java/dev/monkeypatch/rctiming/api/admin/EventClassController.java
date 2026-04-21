package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.api.admin.dto.AddEventClassRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CombineClassesRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EventClassDto;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateEventClassOverrideRequest;
import dev.monkeypatch.rctiming.domain.format.EventClassService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/events/{eventId}/classes")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class EventClassController {

    private final EventClassService eventClassService;

    public EventClassController(EventClassService eventClassService) {
        this.eventClassService = eventClassService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventClassDto addClassToEvent(@PathVariable Long eventId,
                                          @RequestBody @Valid AddEventClassRequest request) {
        return eventClassService.addClassToEvent(eventId, request);
    }

    @PutMapping("/{classId}/overrides")
    public EventClassDto updateOverrides(@PathVariable Long eventId,
                                          @PathVariable Long classId,
                                          @RequestBody @Valid UpdateEventClassOverrideRequest request) {
        return eventClassService.updateOverrides(classId, request);
    }

    @PostMapping("/combine")
    public List<EventClassDto> combineClasses(@PathVariable Long eventId,
                                               @RequestBody @Valid CombineClassesRequest request) {
        return eventClassService.combineClasses(eventId, request.eventClassIds());
    }
}
