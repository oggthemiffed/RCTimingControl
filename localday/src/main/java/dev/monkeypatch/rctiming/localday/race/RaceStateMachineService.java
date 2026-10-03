package dev.monkeypatch.rctiming.localday.race;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.sync.SnapshotTriggerEvent;
import dev.monkeypatch.rctiming.localday.timing.LapTimingService;
import dev.monkeypatch.rctiming.localday.timing.LiveTimingHub;
import dev.monkeypatch.rctiming.localday.timing.dto.MarshalAdjustmentDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Local reimplementation of the cloud's race lifecycle state machine
 * ({@code PENDING -> GRID -> RUNNING -> STOPPED/FINISHED}) plus marshal lap adjustments.
 *
 * <p>Deliberately independent of {@code app}'s {@code RaceStateMachineService} (KD3) — same
 * target behavior, no shared code or dependency between the two modules.
 *
 * <p>Operates directly on {@link CachedScheduleEntry}, the local analog of the cloud's
 * {@code Race} entity — there is no separate "Race" entity in {@code :localday}.
 */
@Service
public class RaceStateMachineService {

    private static final Map<RaceState, Set<RaceState>> VALID_TRANSITIONS;

    static {
        Map<RaceState, Set<RaceState>> m = new EnumMap<>(RaceState.class);
        m.put(RaceState.PENDING, EnumSet.of(RaceState.GRID));
        m.put(RaceState.GRID, EnumSet.of(RaceState.RUNNING, RaceState.PENDING));
        m.put(RaceState.RUNNING, EnumSet.of(RaceState.STOPPED, RaceState.FINISHED));
        m.put(RaceState.STOPPED, EnumSet.of(RaceState.RUNNING, RaceState.FINISHED));
        m.put(RaceState.FINISHED, EnumSet.noneOf(RaceState.class));
        VALID_TRANSITIONS = Map.copyOf(m);
    }

    private final MarshalAdjustmentRepository marshalAdjustmentRepository;
    private final LiveTimingHub liveTimingHub;
    private final LapTimingService lapTimingService;
    private final ApplicationEventPublisher eventPublisher;

    public RaceStateMachineService(MarshalAdjustmentRepository marshalAdjustmentRepository,
                                    LiveTimingHub liveTimingHub,
                                    LapTimingService lapTimingService,
                                    ApplicationEventPublisher eventPublisher) {
        this.marshalAdjustmentRepository = marshalAdjustmentRepository;
        this.liveTimingHub = liveTimingHub;
        this.lapTimingService = lapTimingService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Status-transition logic. Mutates {@code race}'s status only — does not set
     * {@code startedAt}/{@code finishedAt} (mirrors the cloud's separation of concerns; a
     * future controller unit is responsible for those timestamps). Broadcasts the new state
     * over STOMP via {@link LiveTimingHub}; on transition to {@link RaceState#FINISHED}, also
     * releases the race's in-memory live-timing state (the sensible local equivalent of the
     * cloud's {@code restart()} freeing state — {@code :localday} has no separate
     * {@code restart()} method yet).
     */
    public void transition(CachedScheduleEntry race, RaceState target) {
        Set<RaceState> valid = VALID_TRANSITIONS.getOrDefault(race.getStatus(), Set.of());
        if (!valid.contains(target)) {
            throw new IllegalStateTransitionException(
                    "Cannot transition race " + race.getId()
                    + " from " + race.getStatus() + " to " + target);
        }
        race.setStatus(target);

        liveTimingHub.broadcastStateChange(race.getId(), target);

        if (target == RaceState.FINISHED) {
            lapTimingService.releaseState(race.getId());
        }

        // KTD8: an immediate snapshot push on every race-state transition, not waiting for the
        // next periodic tick.
        eventPublisher.publishEvent(new SnapshotTriggerEvent("race-state-transition"));
    }

    /**
     * Records a marshal lap adjustment (+1/-1) as a standalone audit record. This is not a
     * state transition — it does not call {@link #transition} and does not mutate the race's
     * status. {@code raceStateAtTime} snapshots the race's status at the moment this method is
     * called. After persisting the audit row, applies the lap delta to the in-memory live-timing
     * state and broadcasts it (mirrors the cloud's {@code RaceControlController.marshalAdjustment}
     * endpoint, inlined here since {@code :localday} has no controller yet).
     */
    public MarshalAdjustment recordAdjustment(CachedScheduleEntry race, CachedEntry entry, int lapDelta,
                                               Long actingUserId, String actingUserName) {
        MarshalAdjustment adjustment = new MarshalAdjustment();
        adjustment.setRaceId(race.getId());
        adjustment.setEntryId(entry.getId());
        adjustment.setTransponderNumber(entry.getTransponderNumber());
        adjustment.setLapDelta(lapDelta);
        adjustment.setRaceStateAtTime(race.getStatus().name());
        adjustment.setActingUserId(actingUserId);
        adjustment.setActingUserName(actingUserName);
        adjustment.setAdjustedAt(Instant.now());
        MarshalAdjustment saved = marshalAdjustmentRepository.save(adjustment);

        MarshalAdjustmentDto dto = new MarshalAdjustmentDto(
                race.getId(), entry.getId(), entry.getTransponderNumber(), lapDelta,
                actingUserName, saved.getAdjustedAt().toEpochMilli());
        lapTimingService.applyMarshalAdjustment(race.getId(), entry.getId(), lapDelta, dto);

        return saved;
    }
}
