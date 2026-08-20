package dev.monkeypatch.rctiming.localday.race;

import dev.monkeypatch.rctiming.localday.auth.SessionPrincipal;
import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import dev.monkeypatch.rctiming.localday.race.dto.AdvanceRoundRequest;
import dev.monkeypatch.rctiming.localday.race.dto.ErrorResponse;
import dev.monkeypatch.rctiming.localday.race.dto.GridEntryDto;
import dev.monkeypatch.rctiming.localday.race.dto.LiveSnapshotDto;
import dev.monkeypatch.rctiming.localday.race.dto.MarshalAdjustmentRequest;
import dev.monkeypatch.rctiming.localday.race.dto.MarshalAdjustmentResponseDto;
import dev.monkeypatch.rctiming.localday.race.dto.ScheduleEntryDetailDto;
import dev.monkeypatch.rctiming.localday.race.dto.ScheduleEntryDto;
import dev.monkeypatch.rctiming.localday.race.dto.TransitionRequest;
import dev.monkeypatch.rctiming.localday.timing.LapTimingService;
import dev.monkeypatch.rctiming.localday.timing.LiveRaceState;
import dev.monkeypatch.rctiming.localday.timing.dto.LiveTimingRowDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Race control endpoints for the local race-day program (U8) — race lifecycle transitions,
 * marshal lap adjustments, live position snapshots, and round-to-round grid propagation.
 *
 * <p>Every endpoint here is officials-only, gated by {@code LocalSecurityConfig}'s default-deny
 * {@code anyRequest().authenticated()} rule — same convention as {@code CheckInController} and
 * {@code TransponderReassignmentController}. This is deliberate per R9: the anonymous read-only
 * board/attendee view is a distinct, not-yet-built API surface for a later unit, not this one.
 */
@RestController
@RequestMapping("/api/v1/race-control")
public class RaceControlController {

    private final RaceStateMachineService raceStateMachineService;
    private final RoundGeneratorService roundGeneratorService;
    private final LapTimingService lapTimingService;
    private final CachedScheduleEntryRepository cachedScheduleEntryRepository;
    private final CachedRaceEntryRepository cachedRaceEntryRepository;
    private final CachedEntryRepository cachedEntryRepository;
    private final RaceResultEntryRepository raceResultEntryRepository;

    public RaceControlController(RaceStateMachineService raceStateMachineService,
                                  RoundGeneratorService roundGeneratorService,
                                  LapTimingService lapTimingService,
                                  CachedScheduleEntryRepository cachedScheduleEntryRepository,
                                  CachedRaceEntryRepository cachedRaceEntryRepository,
                                  CachedEntryRepository cachedEntryRepository,
                                  RaceResultEntryRepository raceResultEntryRepository) {
        this.raceStateMachineService = raceStateMachineService;
        this.roundGeneratorService = roundGeneratorService;
        this.lapTimingService = lapTimingService;
        this.cachedScheduleEntryRepository = cachedScheduleEntryRepository;
        this.cachedRaceEntryRepository = cachedRaceEntryRepository;
        this.cachedEntryRepository = cachedEntryRepository;
        this.raceResultEntryRepository = raceResultEntryRepository;
    }

    @GetMapping("/races")
    public List<ScheduleEntryDto> races() {
        return cachedScheduleEntryRepository.findAllByOrderBySequenceAsc().stream()
                .map(ScheduleEntryDto::from)
                .toList();
    }

    @GetMapping("/races/{id}")
    public ResponseEntity<?> raceDetail(@PathVariable Long id) {
        Optional<CachedScheduleEntry> race = cachedScheduleEntryRepository.findById(id);
        if (race.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("race_not_found"));
        }
        return ResponseEntity.ok(toDetailDto(race.get()));
    }

    @GetMapping("/races/{id}/live")
    public ResponseEntity<?> live(@PathVariable Long id) {
        if (cachedScheduleEntryRepository.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("race_not_found"));
        }
        List<dev.monkeypatch.rctiming.localday.timing.dto.LiveTimingRowDto> rows =
                lapTimingService.peek(id).map(LiveRaceState::calculatePositions).orElse(List.of());
        return ResponseEntity.ok(new LiveSnapshotDto(id, rows));
    }

    @PostMapping("/races/{id}/transition")
    public ResponseEntity<?> transition(@PathVariable Long id, @RequestBody TransitionRequest request) {
        Optional<CachedScheduleEntry> raceOpt = cachedScheduleEntryRepository.findById(id);
        if (raceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("race_not_found"));
        }
        CachedScheduleEntry race = raceOpt.get();

        if (request.target() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("invalid_target_state"));
        }
        RaceState target;
        try {
            target = RaceState.valueOf(request.target());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("invalid_target_state"));
        }

        if (target == RaceState.RUNNING && race.getStartedAt() == null) {
            race.setStartedAt(Instant.now());
        }

        // Capture the live snapshot BEFORE the transition call — transition() internally releases
        // the in-memory live-timing state on FINISHED (see RaceStateMachineService.transition's
        // FINISHED branch), so capturing after would always see an empty snapshot. Same ordering
        // requirement as advance-round's pre-transition live-snapshot capture.
        List<LiveTimingRowDto> finalPositions = List.of();
        if (target == RaceState.FINISHED) {
            finalPositions = lapTimingService.peek(id).map(LiveRaceState::calculatePositions).orElse(List.of());
            race.setFinishedAt(Instant.now());
        }

        try {
            raceStateMachineService.transition(race, target);
        } catch (IllegalStateTransitionException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("illegal_transition"));
        }

        // transition() only mutates the in-memory status field — it does not persist. Without this
        // explicit save, the new state would be silently lost on the next read.
        try {
            cachedScheduleEntryRepository.save(race);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            // Another request already transitioned this race between our read and our write (e.g.
            // a double-submitted "Finish" click, or a client retry after a slow response) — reject
            // rather than silently racing ahead and, for FINISHED, double-writing race_results.
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("concurrent_modification"));
        }

        if (target == RaceState.FINISHED) {
            raceResultEntryRepository.saveAll(buildResultRows(id, finalPositions));
        }

        return ResponseEntity.ok(ScheduleEntryDto.from(race));
    }

    @PostMapping("/races/{id}/marshal-adjustment")
    public ResponseEntity<?> marshalAdjustment(@PathVariable Long id, @RequestBody MarshalAdjustmentRequest request) {
        Optional<CachedScheduleEntry> raceOpt = cachedScheduleEntryRepository.findById(id);
        if (raceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("race_not_found"));
        }
        if (request.cachedEntryId() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("entry_not_found"));
        }
        if (request.lapDelta() != 1 && request.lapDelta() != -1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("invalid_lap_delta"));
        }
        Optional<CachedEntry> entryOpt = cachedEntryRepository.findById(request.cachedEntryId());
        if (entryOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("entry_not_found"));
        }
        // Guard against adjusting an entry that never raced in this heat — e.g. a stale client
        // request for a car that only appeared in a different race's grid.
        boolean inThisRacesGrid = cachedRaceEntryRepository
                .findByCachedScheduleIdOrderByGridPositionAsc(id).stream()
                .anyMatch(re -> request.cachedEntryId().equals(re.getCachedEntryId()));
        if (!inThisRacesGrid) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("entry_not_in_race"));
        }

        SessionPrincipal principal = (SessionPrincipal)
                SecurityContextHolder.getContext().getAuthentication().getDetails();

        MarshalAdjustment adjustment = raceStateMachineService.recordAdjustment(
                raceOpt.get(), entryOpt.get(), request.lapDelta(),
                principal.credentialId(), principal.officialName());

        return ResponseEntity.ok(new MarshalAdjustmentResponseDto(
                adjustment.getRaceId(), adjustment.getEntryId(), adjustment.getTransponderNumber(),
                adjustment.getLapDelta(), adjustment.getActingUserName()));
    }

    /**
     * Propagates a finished round's finishing order into the next round's starting grid.
     * Deliberately does not cover {@link BumpUpSeedingService} — bump-up/finals promotion across
     * a whole class's qualifying standings is a separate, larger workflow not wired up here.
     */
    @PostMapping("/races/{id}/advance-round")
    public ResponseEntity<?> advanceRound(@PathVariable Long id, @RequestBody AdvanceRoundRequest request) {
        if (cachedScheduleEntryRepository.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("race_not_found"));
        }
        Optional<CachedScheduleEntry> nextRaceOpt = cachedScheduleEntryRepository.findById(request.nextScheduleId());
        if (nextRaceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("target_race_not_found"));
        }

        roundGeneratorService.applyPreviousRoundFinishingOrder(
                request.nextScheduleId(), request.entryIdsInFinishingOrder());

        CachedScheduleEntry refreshed = cachedScheduleEntryRepository.findById(request.nextScheduleId())
                .orElseThrow();
        return ResponseEntity.ok(toDetailDto(refreshed));
    }

    /**
     * Builds the durable result rows for a just-finished race. {@code finalPositions} (from
     * {@link LiveRaceState#calculatePositions()}) only contains entries that recorded at least one
     * lap or marshal adjustment — a grid entry whose transponder never registered (DNS, dead
     * battery, unlinked transponder) would otherwise be silently missing from the finishing order
     * shown on the results board. This appends every remaining grid entry, in grid order, as a
     * zero-lap finisher after everyone who actually turned a lap.
     */
    private List<RaceResultEntry> buildResultRows(Long raceId, List<LiveTimingRowDto> finalPositions) {
        Instant recordedAt = Instant.now();
        List<RaceResultEntry> results = new java.util.ArrayList<>(finalPositions.size());
        java.util.Set<Long> accountedFor = new java.util.HashSet<>();

        for (LiveTimingRowDto row : finalPositions) {
            RaceResultEntry result = new RaceResultEntry();
            result.setRaceId(raceId);
            result.setEntryId(row.entryId());
            result.setPosition(row.position());
            result.setLapsCompleted(row.lapsCompleted());
            result.setBestLapMs(row.bestLapMs());
            result.setRecordedAt(recordedAt);
            results.add(result);
            accountedFor.add(row.entryId());
        }

        int nextPosition = finalPositions.size() + 1;
        for (CachedRaceEntry gridRow : cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(raceId)) {
            Long entryId = gridRow.getCachedEntryId();
            if (entryId == null || accountedFor.contains(entryId)) {
                continue; // unfilled bump slot, or already has a real live-position result
            }
            RaceResultEntry result = new RaceResultEntry();
            result.setRaceId(raceId);
            result.setEntryId(entryId);
            result.setPosition(nextPosition++);
            result.setLapsCompleted(0);
            result.setBestLapMs(null);
            result.setRecordedAt(recordedAt);
            results.add(result);
        }

        return results;
    }

    private ScheduleEntryDetailDto toDetailDto(CachedScheduleEntry race) {
        List<CachedRaceEntry> raceEntries =
                cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(race.getId());

        List<Long> entryIds = raceEntries.stream()
                .map(CachedRaceEntry::getCachedEntryId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, CachedEntry> entriesById = cachedEntryRepository.findAllById(entryIds).stream()
                .collect(java.util.stream.Collectors.toMap(CachedEntry::getId, e -> e));

        List<GridEntryDto> grid = raceEntries.stream()
                .map(re -> {
                    Long cachedEntryId = re.getCachedEntryId();
                    CachedEntry entry = cachedEntryId == null ? null : entriesById.get(cachedEntryId);
                    return new GridEntryDto(
                            cachedEntryId,
                            entry == null ? null : entry.getRacerName(),
                            entry == null ? null : entry.getTransponderNumber(),
                            re.getCarNumber(),
                            re.getGridPosition(),
                            re.isBumped());
                })
                .toList();
        return new ScheduleEntryDetailDto(ScheduleEntryDto.from(race), grid);
    }
}
