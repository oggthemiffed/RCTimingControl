package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.domain.EntityNotFoundException;
import dev.monkeypatch.rctiming.domain.StateConflictException;
import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;
import dev.monkeypatch.rctiming.domain.audit.Audited;
import dev.monkeypatch.rctiming.api.racecontrol.dto.IncidentReportRequest;
import dev.monkeypatch.rctiming.api.racecontrol.dto.MarshalAbsenceRequest;
import dev.monkeypatch.rctiming.api.racecontrol.dto.MarshalPenaltyRequest;
import dev.monkeypatch.rctiming.api.racecontrol.dto.PenaltyRequest;
import dev.monkeypatch.rctiming.domain.race.IncidentReport;
import dev.monkeypatch.rctiming.domain.race.IncidentReportRepository;
import dev.monkeypatch.rctiming.domain.race.MarshalAbsence;
import dev.monkeypatch.rctiming.domain.race.MarshalAbsenceRepository;
import dev.monkeypatch.rctiming.domain.race.MarshalPenalty;
import dev.monkeypatch.rctiming.domain.race.MarshalPenaltyRepository;
import dev.monkeypatch.rctiming.domain.race.Penalty;
import dev.monkeypatch.rctiming.domain.race.PenaltyRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.resultsexport.FinishedRaceCorrected;
import dev.monkeypatch.rctiming.timing.LapTimingService;
import dev.monkeypatch.rctiming.timing.LiveTimingHub;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private final RaceRepository raceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService audit;
    private final RaceAuditLabels labels;

    public RefereeController(IncidentReportRepository incidentReportRepository,
                              PenaltyRepository penaltyRepository,
                              MarshalAbsenceRepository marshalAbsenceRepository,
                              MarshalPenaltyRepository marshalPenaltyRepository,
                              LapTimingService lapTimingService,
                              LiveTimingHub liveTimingHub,
                              RaceRepository raceRepository,
                              ApplicationEventPublisher eventPublisher,
                              AuditService audit,
                              RaceAuditLabels labels) {
        this.incidentReportRepository = incidentReportRepository;
        this.penaltyRepository = penaltyRepository;
        this.marshalAbsenceRepository = marshalAbsenceRepository;
        this.marshalPenaltyRepository = marshalPenaltyRepository;
        this.lapTimingService = lapTimingService;
        this.liveTimingHub = liveTimingHub;
        this.raceRepository = raceRepository;
        this.eventPublisher = eventPublisher;
        this.audit = audit;
        this.labels = labels;
    }

    @Audited("audit_log")
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
        IncidentReport saved = incidentReportRepository.save(report);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("entryId", saved.getEntryId());
        after.put("incidentType", saved.getIncidentType());
        after.put("description", saved.getDescription());
        audit.entry(Actor.official(userId), "INCIDENT_RAISED").entity("incident_report", saved.getId())
                .race(raceId).event(labels.eventOf(raceId))
                .summary("Raised an incident (" + saved.getIncidentType() + ") against "
                        + labels.driver(saved.getEntryId()) + " in " + labels.race(raceId))
                .after(after).record();
        return ResponseEntity.ok(saved);
    }

    /**
     * Apply a penalty to an entry.
     * LAP penalty: immediately decrements in-memory lapsCompleted and rebroadcasts positions.
     * TIME penalty: recorded only — applied to totalTime at result-snapshot computation.
     */
    @Audited("audit_log")
    @PostMapping("/race/{raceId}/penalty")
    @Transactional
    public ResponseEntity<Penalty> applyPenalty(@PathVariable long raceId,
                                                 @Valid @RequestBody PenaltyRequest req) {
        if (!req.penaltyType().equals("LAP") && !req.penaltyType().equals("TIME")) {
            throw new IllegalArgumentException("penaltyType must be LAP or TIME, got: " + req.penaltyType());
        }
        if (req.penaltyType().equals("LAP") && req.value().stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("A LAP penalty must be a whole number of laps, got: " + req.value());
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
        Penalty saved = penaltyRepository.save(penalty);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("entryId", penalty.getEntryId());
        after.put("penaltyType", penalty.getPenaltyType());
        after.put("value", penalty.getValue());
        after.put("reason", penalty.getReason());
        audit.entry(Actor.official(userId), "PENALTY_APPLIED").entity("penalty", saved.getId())
                .race(raceId).event(labels.eventOf(raceId))
                .summary("Gave " + labels.driver(penalty.getEntryId()) + " a " + penalty.getPenaltyType()
                        + " penalty of " + penalty.getValue().stripTrailingZeros().toPlainString() + " in "
                        + labels.race(raceId))
                .after(after).record();

        if (raceRepository.findById(raceId).map(r -> r.getStatus() == RaceStatus.FINISHED).orElse(false)) {
            // The race's results have gone out already; send them again with the penalty (#27)
            eventPublisher.publishEvent(new FinishedRaceCorrected(raceId));
        }

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
    @Audited("audit_log")
    @PostMapping("/race/{raceId}/marshal-absent")
    @Transactional
    public ResponseEntity<MarshalAbsence> recordMarshalAbsent(@PathVariable long raceId,
                                                     @Valid @RequestBody MarshalAbsenceRequest req) {
        long userId = resolveUserId();
        MarshalAbsence absence = new MarshalAbsence();
        absence.setRaceId(raceId);
        absence.setEntryId(req.entryId());
        absence.setEventId(req.eventId());
        absence.setRecordedBy(userId);
        absence.setRecordedAt(Instant.now());
        MarshalAbsence saved = marshalAbsenceRepository.save(absence);
        audit.entry(Actor.official(userId), "MARSHAL_ABSENCE_RECORDED").entity("marshal_absence", saved.getId())
                .race(raceId).event(req.eventId())
                .summary("Recorded that " + labels.driver(req.entryId()) + " missed their marshal duty")
                .after(Map.of("entryId", req.entryId())).record();
        return ResponseEntity.ok(saved);
    }

    /**
     * Apply a marshal penalty for a recorded absence (D-22 — separate action from recording). The request can
     * name the absence (its id comes back from {@code marshal-absent}); otherwise the entry's most recent
     * absence in the event is used, if there is one.
     */
    @Audited("audit_log")
    @PostMapping("/race/{raceId}/apply-marshal-penalty")
    @Transactional
    public ResponseEntity<MarshalPenalty> applyMarshalPenalty(@PathVariable long raceId,
                                                               @Valid @RequestBody MarshalPenaltyRequest req) {
        long userId = resolveUserId();
        Long absenceId = absenceFor(req);

        MarshalPenalty mp = new MarshalPenalty();
        mp.setAbsenceId(absenceId);
        mp.setEntryId(req.entryId());
        mp.setEventId(req.eventId());
        mp.setAppliedBy(userId);
        mp.setAppliedAt(Instant.now());
        mp.setNotes(req.notes());
        MarshalPenalty saved = marshalPenaltyRepository.save(mp);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("entryId", saved.getEntryId());
        after.put("absenceId", saved.getAbsenceId());
        after.put("notes", saved.getNotes());
        audit.entry(Actor.official(userId), "MARSHAL_PENALTY_APPLIED").entity("marshal_penalty", saved.getId())
                .race(raceId).event(req.eventId())
                .summary("Gave " + labels.driver(req.entryId()) + " a marshal penalty")
                .after(after).record();
        return ResponseEntity.ok(saved);
    }

    /** The absence a marshal penalty is for: the one named, which must be this entry's in this event, or the latest. */
    private Long absenceFor(MarshalPenaltyRequest req) {
        if (req.absenceId() != null) {
            MarshalAbsence named = marshalAbsenceRepository.findById(req.absenceId())
                    .orElseThrow(() -> new EntityNotFoundException("Marshal absence not found: " + req.absenceId()));
            if (!named.getEntryId().equals(req.entryId()) || !named.getEventId().equals(req.eventId())) {
                throw new StateConflictException("That absence was recorded for a different entry or event");
            }
            return named.getId();
        }
        return marshalAbsenceRepository.findByEventId(req.eventId()).stream()
                .filter(a -> a.getEntryId().equals(req.entryId()))
                .max(Comparator.comparing(MarshalAbsence::getId))
                .map(MarshalAbsence::getId)
                .orElse(null);
    }

    private long resolveUserId() {
        return Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
