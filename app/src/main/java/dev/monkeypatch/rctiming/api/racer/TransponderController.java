package dev.monkeypatch.rctiming.api.racer;

import dev.monkeypatch.rctiming.api.racer.dto.CreateTransponderRequest;
import dev.monkeypatch.rctiming.api.racer.dto.TransponderDto;
import dev.monkeypatch.rctiming.domain.transponder.TransponderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/racer/transponders")
@PreAuthorize("hasRole('RACER')")
public class TransponderController {

    private final TransponderService service;

    public TransponderController(TransponderService service) {
        this.service = service;
    }

    @GetMapping
    public List<TransponderDto> list(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return service.findForUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransponderDto create(Authentication auth,
                                  @RequestBody @Valid CreateTransponderRequest req) {
        Long userId = Long.parseLong(auth.getName());
        return service.create(userId, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        Long userId = Long.parseLong(auth.getName());
        service.delete(id, userId);
    }
}
