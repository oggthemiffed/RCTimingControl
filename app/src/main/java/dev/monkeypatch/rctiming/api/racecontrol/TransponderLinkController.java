package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.api.racecontrol.dto.RaceEntryDto;
import dev.monkeypatch.rctiming.forwarder.UnknownTransponderLinkAudit;
import dev.monkeypatch.rctiming.forwarder.UnknownTransponderLinkAuditRepository;
import dev.monkeypatch.rctiming.forwarder.dto.LinkTransponderRequestDto;
import dev.monkeypatch.rctiming.query.racecontrol.RaceEntriesQuery;
import dev.monkeypatch.rctiming.timing.LapTimingService;
import dev.monkeypatch.rctiming.timing.dto.LiveTimingRowDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Phase 5 / TIMING-08: REST endpoint for retroactive transponder linking during a live race.
 * Race director or admin can link an unknown transponder number to an existing entry,
 * retroactively crediting all passings since race start (D-12).
 *
 * <p>T-05-18: endpoint is protected by @PreAuthorize — RACER role receives HTTP 403.
 * T-05-16: audit record persisted with actor userId, raceId, entryId, linkedAt.
 */
@RestController
@RequestMapping("/api/v1/race-control/races/{raceId}")
public class TransponderLinkController {

    private final LapTimingService lapTimingService;
    private final UnknownTransponderLinkAuditRepository linkAuditRepository;
    private final RaceEntriesQuery raceEntriesQuery;

    public TransponderLinkController(LapTimingService lapTimingService,
                                     UnknownTransponderLinkAuditRepository linkAuditRepository,
                                     RaceEntriesQuery raceEntriesQuery) {
        this.lapTimingService = lapTimingService;
        this.linkAuditRepository = linkAuditRepository;
        this.raceEntriesQuery = raceEntriesQuery;
    }

    @GetMapping("/entries")
    @PreAuthorize("hasAnyRole('RACE_DIRECTOR', 'ADMIN')")
    public ResponseEntity<List<RaceEntryDto>> getEntries(@PathVariable Long raceId) {
        return ResponseEntity.ok(raceEntriesQuery.findForRace(raceId));
    }

    @GetMapping("/live-timing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LiveTimingRowDto>> getLiveTimingSnapshot(@PathVariable Long raceId) {
        List<LiveTimingRowDto> rows = lapTimingService.peek(raceId)
                .map(state -> state.calculatePositions())
                .orElse(List.of());
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/transponders/link")
    @PreAuthorize("hasAnyRole('RACE_DIRECTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Integer>> linkTransponder(
            @PathVariable Long raceId,
            @Valid @RequestBody LinkTransponderRequestDto request) {

        String transponderNumber = request.transponderNumber();
        Long entryId = request.entryId();

        // Extract acting user from JWT
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.getName() != null) {
            try {
                userId = Long.parseLong(auth.getName());
            } catch (NumberFormatException ignored) {
                // anonymous or non-numeric principal — userId remains null
            }
        }

        // Count passings BEFORE linking (returned as lapsCredited)
        int lapsCredited = lapTimingService.countPassingsForTransponder(raceId, transponderNumber);

        // Persist audit record (T-05-16)
        linkAuditRepository.save(
                new UnknownTransponderLinkAudit(raceId, transponderNumber, entryId, userId));

        // Retroactively credit laps and broadcast updated positions
        List<LiveTimingRowDto> positions =
                lapTimingService.linkTransponder(raceId, transponderNumber, entryId);

        return ResponseEntity.ok(Map.of("lapsCredited", lapsCredited));
    }
}
