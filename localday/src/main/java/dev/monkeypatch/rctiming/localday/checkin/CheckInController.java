package dev.monkeypatch.rctiming.localday.checkin;

import dev.monkeypatch.rctiming.localday.checkin.dto.ConfirmResponse;
import dev.monkeypatch.rctiming.localday.checkin.dto.EntryDto;
import dev.monkeypatch.rctiming.localday.checkin.dto.ErrorResponse;
import dev.monkeypatch.rctiming.localday.checkin.dto.ResolveRequest;
import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Check-in / attendance confirmation for pre-entered racers (U7, R5). Every endpoint here
 * requires an authenticated local official session — none is listed in
 * {@code LocalSecurityConfig}'s permit-all set, so the default-deny
 * {@code anyRequest().authenticated()} rule covers them automatically.
 */
@RestController
@RequestMapping("/api/v1/checkin")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping("/resolve")
    public ResponseEntity<?> resolve(@RequestBody ResolveRequest request) {
        Optional<CachedEntry> entry = checkInService.resolveByTransponderNumber(request.transponderNumber());
        if (entry.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not_found"));
        }
        return ResponseEntity.ok(EntryDto.from(entry.get()));
    }

    @GetMapping("/search")
    public List<EntryDto> search(@RequestParam(name = "query", required = false) String query) {
        return checkInService.searchByName(query).stream().map(EntryDto::from).toList();
    }

    @PostMapping("/{cachedEntryId}/confirm")
    public ResponseEntity<?> confirm(@PathVariable Long cachedEntryId) {
        CheckInResult result = checkInService.confirm(cachedEntryId);
        return switch (result) {
            case CheckInResult.Success success -> ResponseEntity.ok(new ConfirmResponse(
                    success.entry().getId(),
                    success.entry().getRacerName(),
                    success.entry().isCheckedIn(),
                    success.entry().getCheckedInAt(),
                    success.alreadyCheckedIn()));
            case CheckInResult.NotFound ignored -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("entry_not_found"));
        };
    }
}
