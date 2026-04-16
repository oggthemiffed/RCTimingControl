package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.api.admin.dto.CreateRacingClassRequest;
import dev.monkeypatch.rctiming.api.admin.dto.RacingClassDto;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
        return racingClassService.findAll();
    }

    @GetMapping("/{id}")
    public RacingClassDto getRacingClass(@PathVariable Long id) {
        return racingClassService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RacingClassDto createRacingClass(@RequestBody @Valid CreateRacingClassRequest request) {
        return racingClassService.create(request);
    }

    @PutMapping("/{id}")
    public RacingClassDto updateRacingClass(@PathVariable Long id,
                                             @RequestBody @Valid CreateRacingClassRequest request) {
        return racingClassService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRacingClass(@PathVariable Long id) {
        racingClassService.delete(id);
    }
}
