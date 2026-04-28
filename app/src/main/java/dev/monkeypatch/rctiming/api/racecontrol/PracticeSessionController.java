package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.practice.PracticeSessionService;
import dev.monkeypatch.rctiming.practice.PracticeTimingService;
import dev.monkeypatch.rctiming.practice.dto.PracticeSessionDto;
import dev.monkeypatch.rctiming.practice.dto.PracticeTimingRowDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for practice sessions.
 *
 * Endpoints require ADMIN or RACE_DIRECTOR role (T-06-13: elevation-of-privilege mitigation).
 * GET /results is accessible to any authenticated user.
 */
@RestController
@RequestMapping("/api/v1/practice-sessions")
public class PracticeSessionController {

    private final PracticeSessionService sessionService;
    private final PracticeTimingService timingService;

    public PracticeSessionController(PracticeSessionService sessionService,
                                     PracticeTimingService timingService) {
        this.sessionService = sessionService;
        this.timingService = timingService;
    }

    /** Create a new practice session in IDLE state. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public ResponseEntity<PracticeSessionDto> create(
            @RequestBody PracticeSessionService.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        PracticeSessionDto session = sessionService.create(request,
                userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /** Get a practice session by ID. */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PracticeSessionDto> getById(@PathVariable Long id) {
        return sessionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** List recent practice sessions (default 10). */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<PracticeSessionDto> listRecent(
            @RequestParam(defaultValue = "10") int limit) {
        return sessionService.findRecent(limit);
    }

    /** Start a practice session (IDLE → RUNNING). Returns 409 on invalid state. */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public ResponseEntity<PracticeSessionDto> start(@PathVariable Long id) {
        try {
            PracticeSessionDto session = sessionService.start(id);
            return ResponseEntity.ok(session);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /** Stop a practice session (RUNNING → STOPPED). Returns 409 on invalid state. */
    @PostMapping("/{id}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public ResponseEntity<PracticeSessionDto> stop(@PathVariable Long id) {
        try {
            PracticeSessionDto session = sessionService.stop(id);
            return ResponseEntity.ok(session);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /** Get live timing snapshot (rows sorted by position). */
    @GetMapping("/{id}/snapshot")
    @PreAuthorize("isAuthenticated()")
    public List<PracticeTimingRowDto> getSnapshot(@PathVariable Long id) {
        return timingService.getSnapshot(id);
    }

    /** Get final results for a stopped session (same data, named for clarity). */
    @GetMapping("/{id}/results")
    @PreAuthorize("isAuthenticated()")
    public List<PracticeTimingRowDto> getResults(@PathVariable Long id) {
        return timingService.getSnapshot(id);
    }

    /** Link an unknown transponder to a user in an active session. */
    @PostMapping("/{id}/link-transponder")
    @PreAuthorize("hasAnyRole('ADMIN', 'RACE_DIRECTOR')")
    public ResponseEntity<Void> linkTransponder(
            @PathVariable Long id,
            @RequestBody LinkRequest request) {
        timingService.linkTransponder(id, request.transponderNumber(), request.userId(), request.racerName());
        return ResponseEntity.ok().build();
    }

    /** Request body for linking a transponder to a user. */
    public record LinkRequest(String transponderNumber, Long userId, String racerName) {}
}
