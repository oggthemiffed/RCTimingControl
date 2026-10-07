package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.api.racecontrol.dto.CheckInConfirmResponse;
import dev.monkeypatch.rctiming.api.racecontrol.dto.CheckInEntryDto;
import dev.monkeypatch.rctiming.api.racecontrol.dto.CheckInResolveRequest;
import dev.monkeypatch.rctiming.api.racecontrol.dto.TransponderSwapRequest;
import dev.monkeypatch.rctiming.api.racecontrol.dto.TransponderSwapResponse;
import dev.monkeypatch.rctiming.domain.checkin.CheckInResult;
import dev.monkeypatch.rctiming.domain.checkin.CheckInService;
import dev.monkeypatch.rctiming.domain.checkin.SwapResult;
import dev.monkeypatch.rctiming.domain.checkin.TransponderSwapService;
import dev.monkeypatch.rctiming.query.racecontrol.CheckInQuery;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Check-in desk and transponder swap for race control (L11). Open to every
 * official.
 */
@RestController
@RequestMapping("/api/v1/race-control/events/{eventId}")
@PreAuthorize("hasAnyRole('RACE_DIRECTOR','REFEREE','ADMIN')")
public class CheckInController {

    private final CheckInQuery checkInQuery;
    private final CheckInService checkInService;
    private final TransponderSwapService transponderSwapService;

    public CheckInController(CheckInQuery checkInQuery,
                             CheckInService checkInService,
                             TransponderSwapService transponderSwapService) {
        this.checkInQuery = checkInQuery;
        this.checkInService = checkInService;
        this.transponderSwapService = transponderSwapService;
    }

    /** Entries matching a scanned or typed transponder number; 404 when none match. */
    @PostMapping("/check-in/resolve")
    public ResponseEntity<?> resolve(@PathVariable long eventId,
                                     @Valid @RequestBody CheckInResolveRequest request) {
        List<CheckInEntryDto> matches = checkInQuery.findByTransponder(eventId, request.transponderNumber());
        if (matches.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "not_found");
        }
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/check-in/search")
    public List<CheckInEntryDto> search(@PathVariable long eventId,
                                        @RequestParam(defaultValue = "") String query) {
        return checkInQuery.searchByName(eventId, query);
    }

    @PostMapping("/check-in/entries/{entryId}/confirm")
    public ResponseEntity<?> confirm(@PathVariable long eventId, @PathVariable long entryId) {
        CheckInResult result = checkInService.confirm(eventId, entryId, actingUserId());
        return switch (result) {
            case CheckInResult.Success success -> checkInQuery.findEntry(eventId, entryId)
                    .<ResponseEntity<?>>map(dto -> ResponseEntity.ok(
                            new CheckInConfirmResponse(dto, success.alreadyCheckedIn())))
                    .orElseGet(() -> error(HttpStatus.NOT_FOUND, "entry_not_found"));
            case CheckInResult.NotFound ignored -> error(HttpStatus.NOT_FOUND, "entry_not_found");
            case CheckInResult.Withdrawn ignored -> error(HttpStatus.CONFLICT, "entry_withdrawn");
        };
    }

    @Audited("entry_audit_log")
    @PostMapping("/entries/{entryId}/transponder-swap")
    public ResponseEntity<?> swapTransponder(@PathVariable long eventId,
                                             @PathVariable long entryId,
                                             @Valid @RequestBody TransponderSwapRequest request) {
        Long userId = actingUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SwapResult result = transponderSwapService.swap(
                eventId, entryId, request.slot(), request.newTransponderNumber(), userId);
        return switch (result) {
            case SwapResult.Success s -> ResponseEntity.ok(new TransponderSwapResponse(
                    entryId, s.slot(), s.oldNumber(), s.newNumber()));
            case SwapResult.EntryNotFound ignored -> error(HttpStatus.NOT_FOUND, "entry_not_found");
            case SwapResult.EntryWithdrawn ignored -> error(HttpStatus.CONFLICT, "entry_withdrawn");
            case SwapResult.TransponderAlreadyAssigned ignored ->
                    error(HttpStatus.CONFLICT, "transponder_already_assigned");
            case SwapResult.SameAsOtherSlot ignored ->
                    error(HttpStatus.BAD_REQUEST, "same_as_other_transponder");
            case SwapResult.PrimaryRequired ignored -> error(HttpStatus.BAD_REQUEST, "primary_required");
        };
    }

    private static ResponseEntity<?> error(HttpStatus status, String code) {
        return ResponseEntity.status(status).body(Map.of("error", code));
    }

    private static Long actingUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
