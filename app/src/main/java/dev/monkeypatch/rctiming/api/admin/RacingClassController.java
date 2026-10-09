package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.api.admin.dto.CreateRacingClassRequest;
import dev.monkeypatch.rctiming.api.admin.dto.RacingClassDto;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassService;
import dev.monkeypatch.rctiming.security.CurrentOfficial;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/admin/classes")
@PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR', 'REFEREE')")
public class RacingClassController {

    private final RacingClassService racingClassService;

    public RacingClassController(RacingClassService racingClassService) {
        this.racingClassService = racingClassService;
    }

    @GetMapping
    public List<RacingClassDto> listRacingClasses() {
        return racingClassService.findAll().stream().map(RacingClassDto::from).toList();
    }

    @GetMapping("/{id}")
    public RacingClassDto getRacingClass(@PathVariable Long id) {
        return RacingClassDto.from(racingClassService.findById(id));
    }

    @Audited("audit_log")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public RacingClassDto createRacingClass(Authentication auth, @RequestBody @Valid CreateRacingClassRequest request) {
        return RacingClassDto.from(
                racingClassService.create(CurrentOfficial.actor(auth), request.name(), request.description()));
    }

    @Audited("audit_log")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RacingClassDto updateRacingClass(Authentication auth, @PathVariable Long id,
                                             @RequestBody @Valid CreateRacingClassRequest request) {
        return RacingClassDto.from(
                racingClassService.update(CurrentOfficial.actor(auth), id, request.name(), request.description()));
    }

    @Audited("audit_log")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRacingClass(Authentication auth, @PathVariable Long id) {
        racingClassService.delete(CurrentOfficial.actor(auth), id);
    }
}
