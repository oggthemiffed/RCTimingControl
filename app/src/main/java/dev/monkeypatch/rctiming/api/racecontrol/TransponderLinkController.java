package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;
import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.api.racecontrol.dto.RaceEntryDto;
import dev.monkeypatch.rctiming.domain.race.RaceAuditLabels;
import dev.monkeypatch.rctiming.security.CurrentOfficial;
import dev.monkeypatch.rctiming.timing.UnknownTransponderLinkAudit;
import dev.monkeypatch.rctiming.timing.UnknownTransponderLinkAuditRepository;
import dev.monkeypatch.rctiming.timing.dto.LinkTransponderRequestDto;
import dev.monkeypatch.rctiming.query.racecontrol.RaceEntriesQuery;
import dev.monkeypatch.rctiming.timing.LapTimingService;
import dev.monkeypatch.rctiming.timing.dto.LiveTimingRowDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 5 / TIMING-08: REST endpoint for retroactive transponder linking during a live race.
 * Race director or admin can link an unknown transponder number to an existing entry,
 * retroactively crediting all passings since race start (D-12).
 *
 * <p>T-05-18: endpoint is protected by @PreAuthorize — accounts without the required role receive HTTP 403.
 * T-05-16: audit record persisted with actor userId, raceId, entryId, linkedAt.
 */
@RestController
@RequestMapping("/api/v1/race-control/races/{raceId}")
public class TransponderLinkController {

    private final LapTimingService lapTimingService;
    private final UnknownTransponderLinkAuditRepository linkAuditRepository;
    private final RaceEntriesQuery raceEntriesQuery;
    private final AuditService audit;
    private final RaceAuditLabels labels;

    public TransponderLinkController(LapTimingService lapTimingService,
                                     UnknownTransponderLinkAuditRepository linkAuditRepository,
                                     RaceEntriesQuery raceEntriesQuery,
                                     AuditService audit,
                                     RaceAuditLabels labels) {
        this.lapTimingService = lapTimingService;
        this.linkAuditRepository = linkAuditRepository;
        this.raceEntriesQuery = raceEntriesQuery;
        this.audit = audit;
        this.labels = labels;
    }

    /**
     * Everyone in the race, in any state. The race director picks from it to link a transponder, and
     * the referee to pick a driver for an incident or penalty, which can come after the finish when
     * live timing is empty (#107).
     */
    @GetMapping("/entries")
    @PreAuthorize("hasAnyRole('RACE_DIRECTOR', 'REFEREE', 'ADMIN')")
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

    /**
     * The audit rows and the link are one transaction, with the link last: if it fails nothing is recorded as
     * linked, and if recording fails the link is not made.
     */
    @Audited("audit_log")
    @PostMapping("/transponders/link")
    @PreAuthorize("hasAnyRole('RACE_DIRECTOR', 'ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Integer>> linkTransponder(
            @PathVariable Long raceId,
            @Valid @RequestBody LinkTransponderRequestDto request) {

        String transponderNumber = request.transponderNumber();
        Long entryId = request.entryId();

        long userId = CurrentOfficial.id();

        // Count passings BEFORE linking (returned as lapsCredited)
        int lapsCredited = lapTimingService.countPassingsForTransponder(raceId, transponderNumber);

        // Persist audit record (T-05-16)
        linkAuditRepository.save(
                new UnknownTransponderLinkAudit(raceId, transponderNumber, entryId, userId));

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("transponderNumber", transponderNumber);
        after.put("entryId", entryId);
        after.put("lapsCredited", lapsCredited);
        audit.entry(Actor.official(userId), "UNKNOWN_TRANSPONDER_LINKED")
                .entity("race", raceId).race(raceId).event(labels.eventOf(raceId))
                .summary("Linked transponder " + transponderNumber + " to " + labels.driver(entryId) + " in "
                        + labels.race(raceId) + ", crediting " + lapsCredited + " laps")
                .after(after).record();

        // Retroactively credit laps and broadcast updated positions
        lapTimingService.linkTransponder(raceId, transponderNumber, entryId);

        return ResponseEntity.ok(Map.of("lapsCredited", lapsCredited));
    }
}
