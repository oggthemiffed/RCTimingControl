package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.api.racecontrol.dto.IncidentReportRequest;
import dev.monkeypatch.rctiming.api.racecontrol.dto.MarshalAbsenceRequest;
import dev.monkeypatch.rctiming.api.racecontrol.dto.PenaltyRequest;
import dev.monkeypatch.rctiming.domain.race.IncidentReport;
import dev.monkeypatch.rctiming.domain.race.IncidentReportRepository;
import dev.monkeypatch.rctiming.domain.race.MarshalAbsence;
import dev.monkeypatch.rctiming.domain.race.MarshalAbsenceRepository;
import dev.monkeypatch.rctiming.domain.race.MarshalPenalty;
import dev.monkeypatch.rctiming.domain.race.MarshalPenaltyRepository;
import dev.monkeypatch.rctiming.domain.race.Penalty;
import dev.monkeypatch.rctiming.domain.race.PenaltyRepository;
import dev.monkeypatch.rctiming.timing.LapTimingService;
import dev.monkeypatch.rctiming.timing.LiveTimingHub;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * REST endpoints for referee actions: incident reports, penalties, marshal absence recording (OFFICIAL-03, OFFICIAL-04, D-22).
 * All endpoints require REFEREE role.
 */
@RestController
@RequestMapping("/api/v1/race-control/referee")
@PreAuthorize("hasAnyRole('REFEREE','ADMIN')")
public class RefereeController {

    private final IncidentReportRepository incidentReportRepository;
    private final PenaltyRepository penaltyRepository;
    private final MarshalAbsenceRepository marshalAbsenceRepository;
    private final MarshalPenaltyRepository marshalPenaltyRepository;
    private final LapTimingService lapTimingService;
    private final LiveTimingHub liveTimingHub;

    public RefereeController(IncidentReportRepository incidentReportRepository,
                              PenaltyRepository penaltyRepository,
                              MarshalAbsenceRepository marshalAbsenceRepository,
                              MarshalPenaltyRepository marshalPenaltyRepository,
                              LapTimingService lapTimingService,
                              LiveTimingHub liveTimingHub) {
        this.incidentReportRepository = incidentReportRepository;
        this.penaltyRepository = penaltyRepository;
        this.marshalAbsenceRepository = marshalAbsenceRepository;
        this.marshalPenaltyRepository = marshalPenaltyRepository;
        this.lapTimingService = lapTimingService;
        this.liveTimingHub = liveTimingHub;
    }

    @PostMapping("/race/{raceId}/incident-report")
    @Transactional
    public ResponseEntity<IncidentReport> raiseIncident(@PathVariable long raceId,
                                                         @Valid @RequestBody IncidentReportRequest req) {
        long userId = resolveUserId();
        IncidentReport report = new IncidentReport();
        report.setRaceId(raceId);
        report.setEntryId(req.entryId());
        report.setIncidentType(req.incidentType());
        report.setDescription(req.description());
        report.setRaisedBy(userId);
        report.setRaisedAt(Instant.now());
        return ResponseEntity.ok(incidentReportRepository.save(report));
    }

    /**
     * Apply a penalty to an entry.
     * LAP penalty: immediately decrements in-memory lapsCompleted and rebroadcasts positions.
     * TIME penalty: recorded only — applied to totalTime at result-snapshot computation.
     */
    @PostMapping("/race/{raceId}/penalty")
    @Transactional
    public ResponseEntity<Penalty> applyPenalty(@PathVariable long raceId,
                                                 @Valid @RequestBody PenaltyRequest req) {
        if (!req.penaltyType().equals("LAP") && !req.penaltyType().equals("TIME")) {
            throw new IllegalArgumentException("penaltyType must be LAP or TIME, got: " + req.penaltyType());
        }

        long userId = resolveUserId();
        Penalty penalty = new Penalty();
        penalty.setRaceId(raceId);
        penalty.setEntryId(req.entryId());
        penalty.setPenaltyType(req.penaltyType());
        penalty.setValue(req.value());
        penalty.setReason(req.reason());
        penalty.setAppliedBy(userId);
        penalty.setAppliedAt(Instant.now());
        penaltyRepository.save(penalty);

        if ("LAP".equals(req.penaltyType())) {
            lapTimingService.peek(raceId).ifPresent(state -> {
                synchronized (state) {
                    state.applyLapDelta(req.entryId(), -req.value().intValueExact());
                }
                liveTimingHub.broadcastTimingUpdate(raceId, state.calculatePositions());
            });
        }

        return ResponseEntity.ok(penalty);
    }

    /**
     * Record that an entry missed their marshal duty (D-22).
     * Does NOT auto-create a penalty — use /apply-marshal-penalty for that.
     */
    @PostMapping("/race/{raceId}/marshal-absent")
    @Transactional
    public ResponseEntity<Void> recordMarshalAbsent(@PathVariable long raceId,
                                                     @Valid @RequestBody MarshalAbsenceRequest req) {
        long userId = resolveUserId();
        MarshalAbsence absence = new MarshalAbsence();
        absence.setRaceId(raceId);
        absence.setEntryId(req.entryId());
        absence.setEventId(req.eventId());
        absence.setRecordedBy(userId);
        absence.setRecordedAt(Instant.now());
        marshalAbsenceRepository.save(absence);
        return ResponseEntity.ok().build();
    }

    /**
     * Apply a marshal penalty for a recorded absence (D-22 — separate action from recording).
     */
    @PostMapping("/race/{raceId}/apply-marshal-penalty")
    @Transactional
    public ResponseEntity<MarshalPenalty> applyMarshalPenalty(@PathVariable long raceId,
                                                               @Valid @RequestBody MarshalAbsenceRequest req) {
        long userId = resolveUserId();

        // Link to most recent absence for this entry+event if available
        List<MarshalAbsence> absences = marshalAbsenceRepository.findByEventId(req.eventId())
                .stream()
                .filter(a -> a.getEntryId().equals(req.entryId()))
                .toList();
        Long absenceId = absences.isEmpty() ? null : absences.get(absences.size() - 1).getId();

        MarshalPenalty mp = new MarshalPenalty();
        mp.setAbsenceId(absenceId);
        mp.setEntryId(req.entryId());
        mp.setEventId(req.eventId());
        mp.setAppliedBy(userId);
        mp.setAppliedAt(Instant.now());
        mp.setNotes(req.notes());
        return ResponseEntity.ok(marshalPenaltyRepository.save(mp));
    }

    private long resolveUserId() {
        return Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
