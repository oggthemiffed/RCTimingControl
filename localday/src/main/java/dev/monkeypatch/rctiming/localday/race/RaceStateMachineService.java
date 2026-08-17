package dev.monkeypatch.rctiming.localday.race;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
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

    public RaceStateMachineService(MarshalAdjustmentRepository marshalAdjustmentRepository) {
        this.marshalAdjustmentRepository = marshalAdjustmentRepository;
    }

    /**
     * Pure status-transition logic. Mutates {@code race}'s status only — does not set
     * {@code startedAt}/{@code finishedAt} (mirrors the cloud's separation of concerns; a
     * future controller unit is responsible for those timestamps) and does not publish any
     * event or broadcast (no live-timing infra exists yet in {@code :localday}).
     */
    public void transition(CachedScheduleEntry race, RaceState target) {
        Set<RaceState> valid = VALID_TRANSITIONS.getOrDefault(race.getStatus(), Set.of());
        if (!valid.contains(target)) {
            throw new IllegalStateTransitionException(
                    "Cannot transition race " + race.getId()
                    + " from " + race.getStatus() + " to " + target);
        }
        race.setStatus(target);
    }

    /**
     * Records a marshal lap adjustment (+1/-1) as a standalone audit record. This is not a
     * state transition — it does not call {@link #transition} and does not mutate the race's
     * status. {@code raceStateAtTime} snapshots the race's status at the moment this method is
     * called.
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
        return marshalAdjustmentRepository.save(adjustment);
    }
}
