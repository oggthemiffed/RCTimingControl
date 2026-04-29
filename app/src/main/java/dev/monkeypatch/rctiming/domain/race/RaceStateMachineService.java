package dev.monkeypatch.rctiming.domain.race;

import dev.monkeypatch.rctiming.domain.event.IllegalStateTransitionException;
import dev.monkeypatch.rctiming.service.ResultSnapshotService;
import dev.monkeypatch.rctiming.service.RoundGeneratorService;
import dev.monkeypatch.rctiming.timing.LapTimingService;
import dev.monkeypatch.rctiming.timing.LiveTimingHub;
import dev.monkeypatch.rctiming.timing.dto.LiveTimingRowDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RaceStateMachineService {

    private static final Logger log = LoggerFactory.getLogger(RaceStateMachineService.class);

    private static final Map<RaceStatus, Set<RaceStatus>> VALID_TRANSITIONS;

    static {
        Map<RaceStatus, Set<RaceStatus>> m = new EnumMap<>(RaceStatus.class);
        m.put(RaceStatus.PENDING,  EnumSet.of(RaceStatus.GRID));
        m.put(RaceStatus.GRID,     EnumSet.of(RaceStatus.RUNNING, RaceStatus.PENDING));
        m.put(RaceStatus.RUNNING,  EnumSet.of(RaceStatus.STOPPED, RaceStatus.FINISHED));
        m.put(RaceStatus.STOPPED,  EnumSet.of(RaceStatus.RUNNING, RaceStatus.FINISHED));
        m.put(RaceStatus.FINISHED, EnumSet.noneOf(RaceStatus.class));
        VALID_TRANSITIONS = Map.copyOf(m);
    }

    private final LiveTimingHub liveTimingHub;
    private final RoundGeneratorService roundGeneratorService;
    private final RaceRepository raceRepository;
    private final LapTimingService lapTimingService;
    private final RoundRepository roundRepository;
    @Nullable
    private final ResultSnapshotService resultSnapshotService;
    @Nullable
    private final ApplicationEventPublisher eventPublisher;
    @Nullable
    private final dev.monkeypatch.rctiming.service.BumpUpSeedingService bumpUpSeedingService;

    /**
     * Full constructor for production use — all collaborators required.
     */
    public RaceStateMachineService(LiveTimingHub liveTimingHub,
                                   RoundGeneratorService roundGeneratorService,
                                   RaceRepository raceRepository,
                                   LapTimingService lapTimingService,
                                   RoundRepository roundRepository,
                                   @Nullable ResultSnapshotService resultSnapshotService,
                                   @Nullable ApplicationEventPublisher eventPublisher) {
        this(liveTimingHub, roundGeneratorService, raceRepository, lapTimingService,
                roundRepository, resultSnapshotService, eventPublisher, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RaceStateMachineService(LiveTimingHub liveTimingHub,
                                   RoundGeneratorService roundGeneratorService,
                                   RaceRepository raceRepository,
                                   LapTimingService lapTimingService,
                                   RoundRepository roundRepository,
                                   @Nullable ResultSnapshotService resultSnapshotService,
                                   @Nullable ApplicationEventPublisher eventPublisher,
                                   @Nullable dev.monkeypatch.rctiming.service.BumpUpSeedingService bumpUpSeedingService) {
        this.liveTimingHub = liveTimingHub;
        this.roundGeneratorService = roundGeneratorService;
        this.raceRepository = raceRepository;
        this.lapTimingService = lapTimingService;
        this.roundRepository = roundRepository;
        this.resultSnapshotService = resultSnapshotService;
        this.eventPublisher = eventPublisher;
        this.bumpUpSeedingService = bumpUpSeedingService;
    }

    /**
     * Zero-arg convenience constructor for unit tests (plan 02).
     * Delegates to full constructor with all-null collaborators.
     * Broadcasts and finishing-order propagation are short-circuited when hub is null.
     */
    public RaceStateMachineService() {
        this(null, null, null, null, null, null, null);
    }

    /**
     * Restart a race — resets it to PENDING regardless of current state, clears
     * in-memory timing state, and deletes any persisted result snapshot.
     * Intended for false starts or technical issues requiring a full re-run.
     */
    public void restart(Race race) {
        race.setStatus(RaceStatus.PENDING);
        race.setStartedAt(null);
        race.setFinishedAt(null);

        if (lapTimingService != null) {
            lapTimingService.releaseState(race.getId());
        }
        if (resultSnapshotService != null) {
            resultSnapshotService.deleteByRaceId(race.getId());
        }
        if (liveTimingHub != null) {
            liveTimingHub.broadcastStateChange(race.getId(), RaceStatus.PENDING);
        }
    }

    public void transition(Race race, RaceStatus target) {
        Set<RaceStatus> valid = VALID_TRANSITIONS.getOrDefault(race.getStatus(), Set.of());
        if (!valid.contains(target)) {
            throw new IllegalStateTransitionException(
                "Cannot transition race " + race.getId()
                + " from " + race.getStatus() + " to " + target);
        }
        race.setStatus(target);

        // Publish domain event for audio/other listeners
        if (eventPublisher != null && race.getId() != null) {
            eventPublisher.publishEvent(new RaceStatusChangedEvent(this, race.getId(), target));
        }

        // Broadcast state change over STOMP
        if (liveTimingHub != null) {
            liveTimingHub.broadcastStateChange(race.getId(), target);
        }

        // On RUNNING/STOPPED → FINISHED: propagate finishing order, then persist result snapshot
        if (target == RaceStatus.FINISHED && liveTimingHub != null) {
            applyFinishingOrderToNextRace(race);
        }
        if (target == RaceStatus.FINISHED && resultSnapshotService != null) {
            resultSnapshotService.snapshot(race.getId());
        }
    }

    /**
     * When a race finishes, look up the next race in the same heat across the next round
     * for PRACTICE/QUALIFIER rounds, and apply the finishing order as the starting grid.
     *
     * Bump-up finals are handled separately by BumpUpSeedingService — not here.
     */
    private void applyFinishingOrderToNextRace(Race finishedRace) {
        if (lapTimingService == null || roundRepository == null || raceRepository == null) {
            return;
        }

        // Only apply for PRACTICE and QUALIFIER rounds
        Round finishedRound = roundRepository.findById(finishedRace.getRoundId()).orElse(null);
        if (finishedRound == null) {
            return;
        }

        // Handle FINAL rounds: apply bump-up promotion to the next-higher final
        if (finishedRound.getType() == RoundType.FINAL) {
            applyBumpUpPromotion(finishedRace);
            return;
        }

        // Only continue for PRACTICE and QUALIFIER rounds
        if (finishedRound.getType() != RoundType.PRACTICE && finishedRound.getType() != RoundType.QUALIFIER) {
            return;
        }

        // Find the next round in the same event (next sequenceInEvent)
        List<Round> allRounds = roundRepository.findByEventIdOrderBySequenceInEvent(finishedRound.getEventId());
        Round nextRound = null;
        for (Round r : allRounds) {
            if (r.getSequenceInEvent() > finishedRound.getSequenceInEvent()) {
                nextRound = r;
                break;
            }
        }
        if (nextRound == null) {
            return;
        }

        // Find the race in the next round with the same heatNumber and eventClassId
        List<Race> nextRoundRaces = raceRepository.findByRoundIdOrderBySequenceInRound(nextRound.getId());
        Optional<Race> nextRace = nextRoundRaces.stream()
                .filter(r -> r.getHeatNumber() == finishedRace.getHeatNumber()
                          && r.getEventClassId().equals(finishedRace.getEventClassId()))
                .findFirst();

        if (nextRace.isEmpty()) {
            return;
        }

        // Get finishing order from in-memory state
        Optional<dev.monkeypatch.rctiming.timing.LiveRaceState> state = lapTimingService.peek(finishedRace.getId());
        if (state.isEmpty()) {
            log.info("Race {} finished with no in-memory state — skipping finishing-order propagation", finishedRace.getId());
            return;
        }

        List<Long> finishingEntryIds = state.get().calculatePositions().stream()
                .map(LiveTimingRowDto::entryId)
                .toList();

        if (finishingEntryIds.isEmpty()) {
            return;
        }

        try {
            roundGeneratorService.applyPreviousRoundFinishingOrder(nextRace.get().getId(), finishingEntryIds);
            log.info("Applied finishing order from race {} to next race {}", finishedRace.getId(), nextRace.get().getId());
        } catch (Exception e) {
            log.warn("Failed to apply finishing order from race {} to next race {}: {}",
                    finishedRace.getId(), nextRace.get().getId(), e.getMessage());
        }
    }

    /**
     * When a lower final (B, C, ...) finishes, apply bump-up promotion to fill
     * the bump slots in the next-higher final. Broadcasts a STOMP alert after promotion
     * so the race director knows before starting the next final.
     *
     * A-Finals are skipped (no higher final above A).
     */
    private void applyBumpUpPromotion(Race finishedFinalRace) {
        String letter = finishedFinalRace.getFinalLetter();
        if (letter == null || letter.isEmpty() || letter.charAt(0) <= 'A') {
            // A-Final or no letter — nothing to promote
            return;
        }

        if (bumpUpSeedingService == null) {
            log.warn("Bump-up: BumpUpSeedingService unavailable, skipping promotion for race {}", finishedFinalRace.getId());
            return;
        }

        Optional<dev.monkeypatch.rctiming.timing.LiveRaceState> state = lapTimingService.peek(finishedFinalRace.getId());
        if (state.isEmpty()) {
            log.warn("Bump-up: no live state for finished final race {}", finishedFinalRace.getId());
            return;
        }

        List<Long> finishers = state.get().calculatePositions().stream()
                .map(LiveTimingRowDto::entryId)
                .toList();

        try {
            bumpUpSeedingService.applyBumpUpResults(finishedFinalRace.getId(), finishers);
            log.info("Bump-up: applied promotion from {}-final race {}", letter, finishedFinalRace.getId());
            if (liveTimingHub != null) {
                liveTimingHub.broadcastBumpUpAlert(finishedFinalRace.getId(), finishers);
            }
        } catch (Exception e) {
            log.warn("Bump-up promotion failed for final race {}: {}", finishedFinalRace.getId(), e.getMessage());
        }
    }
}
