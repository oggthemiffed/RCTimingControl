package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.api.admin.dto.AddEventClassRequest;
import dev.monkeypatch.rctiming.api.admin.dto.CombineClassesRequest;
import dev.monkeypatch.rctiming.api.admin.dto.EventClassDto;
import dev.monkeypatch.rctiming.api.admin.dto.UpdateEventClassOverrideRequest;
import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.domain.format.EventClassService;
import dev.monkeypatch.rctiming.security.CurrentOfficial;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @Audited("audit_log")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public EventClassDto addClassToEvent(Authentication auth, @PathVariable Long eventId,
                                          @RequestBody @Valid AddEventClassRequest request) {
        return eventClassService.addClassToEvent(CurrentOfficial.actor(auth), eventId, request);
    }

    @Audited("audit_log")
    @PutMapping("/{classId}/overrides")
    @PreAuthorize("hasRole('ADMIN')")
    public EventClassDto updateOverrides(Authentication auth, @PathVariable Long eventId,
                                          @PathVariable Long classId,
                                          @RequestBody @Valid UpdateEventClassOverrideRequest request) {
        return eventClassService.updateOverrides(CurrentOfficial.actor(auth), classId, request);
    }

    @Audited("audit_log")
    @PostMapping("/combine")
    @PreAuthorize("hasRole('ADMIN')")
    public List<EventClassDto> combineClasses(Authentication auth, @PathVariable Long eventId,
                                               @RequestBody @Valid CombineClassesRequest request) {
        return eventClassService.combineClasses(CurrentOfficial.actor(auth), eventId, request.eventClassIds());
    }
}
