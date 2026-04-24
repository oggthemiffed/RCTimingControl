package dev.monkeypatch.rctiming.api.racecontrol;

import dev.monkeypatch.rctiming.api.racecontrol.dto.MarshalAdjustmentRequest;
import dev.monkeypatch.rctiming.api.racecontrol.dto.RunOrderItemDto;
import dev.monkeypatch.rctiming.api.racecontrol.dto.SkipToRaceRequest;
import dev.monkeypatch.rctiming.api.racecontrol.dto.UnknownTransponderLinkRequest;
import dev.monkeypatch.rctiming.domain.race.MarshalAdjustment;
import dev.monkeypatch.rctiming.domain.race.MarshalAdjustmentRepository;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStateMachineService;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.race.UnknownTransponderLink;
import dev.monkeypatch.rctiming.domain.race.UnknownTransponderLinkRepository;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.query.racecontrol.RunOrderQuery;
import dev.monkeypatch.rctiming.timing.LapTimingService;
import dev.monkeypatch.rctiming.timing.LiveTimingHub;
import dev.monkeypatch.rctiming.timing.dto.MarshalAdjustmentDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Race control REST API (CTRL-01, CTRL-03, CTRL-06, CTRL-08, CTRL-09, D-04).
 * All endpoints require RACE_DIRECTOR or ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/race-control")
@PreAuthorize("hasAnyRole('RACE_DIRECTOR','ADMIN')")
public class RaceControlController {

    private final RunOrderQuery runOrderQuery;
    private final RaceRepository raceRepository;
    private final RaceStateMachineService stateMachine;
    private final MarshalAdjustmentRepository marshalAdjustmentRepository;
    private final UnknownTransponderLinkRepository unknownTransponderLinkRepository;
    private final LapTimingService lapTimingService;
    private final LiveTimingHub liveTimingHub;
    private final UserRepository userRepository;
    private final RoundRepository roundRepository;

    /**
     * Process-local active-race override for CTRL-09 skip-to.
     * Keys: eventId → active raceId override. Process-local only; clients re-derive
     * from run-order on reconnect. Cross-session persistence is deferred beyond Phase 4.
     */
    private final Map<Long, Long> activeRaceByEvent = new ConcurrentHashMap<>();

    public RaceControlController(RunOrderQuery runOrderQuery,
                                  RaceRepository raceRepository,
                                  RaceStateMachineService stateMachine,
                                  MarshalAdjustmentRepository marshalAdjustmentRepository,
                                  UnknownTransponderLinkRepository unknownTransponderLinkRepository,
                                  LapTimingService lapTimingService,
                                  LiveTimingHub liveTimingHub,
                                  UserRepository userRepository,
                                  RoundRepository roundRepository) {
        this.runOrderQuery = runOrderQuery;
        this.raceRepository = raceRepository;
        this.stateMachine = stateMachine;
        this.marshalAdjustmentRepository = marshalAdjustmentRepository;
        this.unknownTransponderLinkRepository = unknownTransponderLinkRepository;
        this.lapTimingService = lapTimingService;
        this.liveTimingHub = liveTimingHub;
        this.userRepository = userRepository;
        this.roundRepository = roundRepository;
    }

    // --- D-04: Run order ---

    @GetMapping("/event/{eventId}/run-order")
    public List<RunOrderItemDto> getRunOrder(@PathVariable long eventId) {
        return runOrderQuery.findForEvent(eventId);
    }

    // --- CTRL-01: Race lifecycle ---

    @PostMapping("/race/{raceId}/call-grid")
    @Transactional
    public ResponseEntity<Void> callGrid(@PathVariable long raceId) {
        Race race = loadRace(raceId);
        stateMachine.transition(race, RaceStatus.GRID);
        raceRepository.save(race);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/race/{raceId}/start")
    @Transactional
    public ResponseEntity<Void> startRace(@PathVariable long raceId) {
        Race race = loadRace(raceId);
        stateMachine.transition(race, RaceStatus.RUNNING);
        if (race.getStartedAt() == null) {
            race.setStartedAt(Instant.now());
        }
        raceRepository.save(race);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/race/{raceId}/stop")
    @Transactional
    public ResponseEntity<Void> stopRace(@PathVariable long raceId) {
        Race race = loadRace(raceId);
        stateMachine.transition(race, RaceStatus.STOPPED);
        raceRepository.save(race);
        return ResponseEntity.ok().build();
    }

    // --- CTRL-08: Abandon ---

    @PostMapping("/race/{raceId}/abandon")
    @Transactional
    public ResponseEntity<Void> abandonRace(@PathVariable long raceId) {
        Race race = loadRace(raceId);
        stateMachine.transition(race, RaceStatus.FINISHED);
        race.setFinishedAt(Instant.now());
        // TODO: add abandoned flag in Phase 7 results plan — use finishedAt + empty result snapshot to distinguish
        raceRepository.save(race);
        return ResponseEntity.ok().build();
    }

    // --- CTRL-03: Marshal adjustment ---

    @PostMapping("/race/{raceId}/marshal-adjustment")
    @Transactional
    public ResponseEntity<Void> marshalAdjustment(@PathVariable long raceId,
                                                   @Valid @RequestBody MarshalAdjustmentRequest req) {
        Race race = loadRace(raceId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        long actingUserId = Long.parseLong(auth.getName());
        String actingUserName = resolveUserName(actingUserId);

        MarshalAdjustment adjustment = new MarshalAdjustment();
        adjustment.setRaceId(raceId);
        adjustment.setEntryId(req.entryId());
        adjustment.setTransponderNumber(req.transponderNumber());
        adjustment.setLapDelta(req.lapDelta());
        adjustment.setRaceStateAtTime(race.getStatus().name());
        adjustment.setActingUserId(actingUserId);
        adjustment.setActingUserName(actingUserName);
        adjustment.setAdjustedAt(Instant.now());
        marshalAdjustmentRepository.save(adjustment);

        MarshalAdjustmentDto dto = new MarshalAdjustmentDto(
                raceId,
                req.entryId(),
                req.transponderNumber(),
                req.lapDelta(),
                actingUserName,
                adjustment.getAdjustedAt().toEpochMilli()
        );
        lapTimingService.applyMarshalAdjustment(raceId, req.entryId(), req.lapDelta(), dto);
        return ResponseEntity.ok().build();
    }

    // --- CTRL-06: Unknown transponder link ---

    @PostMapping("/race/{raceId}/unknown-transponder-link")
    @Transactional
    public ResponseEntity<Void> unknownTransponderLink(@PathVariable long raceId,
                                                        @Valid @RequestBody UnknownTransponderLinkRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        long actingUserId = Long.parseLong(auth.getName());

        // Upsert by (raceId, transponderNumber)
        UnknownTransponderLink link = unknownTransponderLinkRepository
                .findByRaceIdAndTransponderNumber(raceId, req.transponderNumber())
                .orElseGet(() -> {
                    UnknownTransponderLink newLink = new UnknownTransponderLink();
                    newLink.setRaceId(raceId);
                    newLink.setTransponderNumber(req.transponderNumber());
                    newLink.setLinkedAt(Instant.now());
                    return newLink;
                });

        link.setLinkedEntryId(req.linkedEntryId());
        link.setLinkedBy(actingUserId);
        unknownTransponderLinkRepository.save(link);
        return ResponseEntity.ok().build();
    }

    // --- CTRL-09: Skip-to (process-local active-race override) ---

    @PostMapping("/race/{raceId}/skip-to")
    @Transactional
    public ResponseEntity<Map<String, Long>> skipTo(@PathVariable long raceId,
                                                     @Valid @RequestBody SkipToRaceRequest req) {
        Race sourceRace = loadRace(raceId);
        Race targetRace = raceRepository.findById(req.targetRaceId())
                .orElseThrow(() -> new EntityNotFoundException("Target race not found: " + req.targetRaceId()));

        // Validate both races belong to the same event (via their rounds)
        long sourceEventId = resolveEventId(sourceRace);
        long targetEventId = resolveEventId(targetRace);
        if (sourceEventId != targetEventId) {
            throw new IllegalArgumentException(
                "Target race " + req.targetRaceId() + " belongs to a different event");
        }

        activeRaceByEvent.put(sourceEventId, req.targetRaceId());
        return ResponseEntity.ok(Map.of("eventId", sourceEventId, "activeRaceId", req.targetRaceId()));
    }

    // --- Helpers ---

    private Race loadRace(long raceId) {
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new EntityNotFoundException("Race not found: " + raceId));
    }

    private String resolveUserName(long userId) {
        return userRepository.findById(userId)
                .map(u -> {
                    String name = (u.getFirstName() != null ? u.getFirstName() + " " : "")
                                + (u.getLastName() != null ? u.getLastName() : "");
                    return name.isBlank() ? u.getEmail() : name.trim();
                })
                .orElse("Unknown");
    }

    private long resolveEventId(Race race) {
        Round round = roundRepository.findById(race.getRoundId())
                .orElseThrow(() -> new EntityNotFoundException("Round not found: " + race.getRoundId()));
        return round.getEventId();
    }
}
