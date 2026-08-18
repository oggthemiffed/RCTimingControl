package dev.monkeypatch.rctiming.localday.checkin;

import dev.monkeypatch.rctiming.localday.auth.SessionPrincipal;
import dev.monkeypatch.rctiming.localday.checkin.dto.ErrorResponse;
import dev.monkeypatch.rctiming.localday.checkin.dto.ReassignRequest;
import dev.monkeypatch.rctiming.localday.checkin.dto.ReassignResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transponder reassignment for an existing entry (U7, R8) — e.g. for equipment failure on race
 * day, entirely local (AE1). Requires an authenticated local official session, gated by
 * {@code LocalSecurityConfig}'s default-deny rule. The acting official's identity for the audit
 * trail is read from {@link SessionPrincipal}, attached to the authentication's details by
 * {@link dev.monkeypatch.rctiming.localday.auth.LocalSessionAuthenticationFilter}.
 */
@RestController
@RequestMapping("/api/v1/transponders")
public class TransponderReassignmentController {

    private final TransponderReassignmentService transponderReassignmentService;

    public TransponderReassignmentController(TransponderReassignmentService transponderReassignmentService) {
        this.transponderReassignmentService = transponderReassignmentService;
    }

    @PostMapping("/reassign")
    public ResponseEntity<?> reassign(@RequestBody ReassignRequest request) {
        SessionPrincipal principal = (SessionPrincipal)
                SecurityContextHolder.getContext().getAuthentication().getDetails();

        ReassignResult result = transponderReassignmentService.reassign(
                request.cachedEntryId(), request.newTransponderNumber(),
                principal.credentialId(), principal.officialName());

        return switch (result) {
            case ReassignResult.Success success -> ResponseEntity.ok(new ReassignResponse(
                    success.entry().getId(), success.oldTransponderNumber(),
                    success.entry().getTransponderNumber()));
            case ReassignResult.EntryNotFound ignored -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("entry_not_found"));
            case ReassignResult.TransponderAlreadyAssigned ignored -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("transponder_already_assigned"));
        };
    }
}
