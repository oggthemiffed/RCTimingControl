package dev.monkeypatch.rctiming.api.racer;

import dev.monkeypatch.rctiming.api.racer.dto.EntryResult;
import dev.monkeypatch.rctiming.api.racer.dto.SubmitEntryRequest;
import dev.monkeypatch.rctiming.domain.entry.EntryService;
import dev.monkeypatch.rctiming.query.entry.EntryQueryService;
import dev.monkeypatch.rctiming.query.entry.RacerEntryHistoryDto;
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
@RequestMapping("/api/v1/racer/entries")
@PreAuthorize("hasRole('RACER')")
public class EntryController {

    private final EntryService entryService;
    private final EntryQueryService entryQueryService;

    public EntryController(EntryService entryService, EntryQueryService entryQueryService) {
        this.entryService = entryService;
        this.entryQueryService = entryQueryService;
    }

    @GetMapping
    public List<RacerEntryHistoryDto> list(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return entryQueryService.findHistoryForUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryResult submit(Authentication auth, @RequestBody @Valid SubmitEntryRequest req) {
        Long userId = Long.parseLong(auth.getName());
        return entryService.submitEntry(userId, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(Authentication auth, @PathVariable Long id) {
        Long userId = Long.parseLong(auth.getName());
        entryService.withdraw(id, userId);
    }
}
