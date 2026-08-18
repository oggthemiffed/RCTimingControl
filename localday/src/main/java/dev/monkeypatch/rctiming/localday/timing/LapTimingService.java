package dev.monkeypatch.rctiming.localday.timing;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntryRepository;
import dev.monkeypatch.rctiming.localday.timing.dto.MarshalAdjustmentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * In-memory race state manager for live timing. Holds a {@code ConcurrentHashMap<Long,
 * LiveRaceState>} keyed by {@code cachedScheduleId}. Positions are calculated in memory and
 * broadcast over STOMP — never persisted during a race (the durable raw-capture row is written
 * separately by {@link DecoderListenerLifecycle} to {@code lap_passings} before this service
 * ever sees the event).
 *
 * <p>Ported from the cloud's {@code app/.../timing/LapTimingService.java} (KD3) — simplified
 * since {@code :localday} has no {@code transponderNumberSnapshot} / runtime-link concept yet
 * (transponder linking is out of this unit's scope).
 */
@Service
public class LapTimingService {

    private static final Logger log = LoggerFactory.getLogger(LapTimingService.class);

    private final Map<Long, LiveRaceState> states = new ConcurrentHashMap<>();
    private final LiveTimingHub liveTimingHub;
    private final CachedEntryRepository cachedEntryRepository;
    private final CachedRaceEntryRepository cachedRaceEntryRepository;

    public LapTimingService(LiveTimingHub liveTimingHub,
                             CachedEntryRepository cachedEntryRepository,
                             CachedRaceEntryRepository cachedRaceEntryRepository) {
        this.liveTimingHub = liveTimingHub;
        this.cachedEntryRepository = cachedEntryRepository;
        this.cachedRaceEntryRepository = cachedRaceEntryRepository;
    }

    /**
     * Returns existing LiveRaceState for this scheduleId, or creates a new one and pre-loads
     * entry display names so calculatePositions() can include driver names.
     */
    public LiveRaceState stateFor(long scheduleId) {
        return states.computeIfAbsent(scheduleId, id -> {
            LiveRaceState state = new LiveRaceState(id);
            loadEntryNames(id, state);
            return state;
        });
    }

    /** Read-only access — returns the state if present, empty if the race has had no lap events yet. */
    public Optional<LiveRaceState> peek(long scheduleId) {
        return Optional.ofNullable(states.get(scheduleId));
    }

    /**
     * Handles a LapPassingEvent published by the ApplicationEventPublisher. A null
     * {@code cachedScheduleId} means no active race — nothing to update live, so this is a no-op
     * (does not create state, does not broadcast).
     */
    @EventListener(LapPassingEvent.class)
    @Async
    public void onLapPassing(LapPassingEvent event) {
        if (event.cachedScheduleId() == null) {
            return;
        }
        long scheduleId = event.cachedScheduleId();
        LiveRaceState state = stateFor(scheduleId);
        Long entryId = resolveEntryId(event.transponderNumber());

        boolean firstUnknown = state.applyLapPassing(event, entryId);

        if (entryId == null && firstUnknown) {
            liveTimingHub.broadcastUnknownTransponder(scheduleId, event.transponderNumber());
        }

        liveTimingHub.broadcastTimingUpdate(scheduleId, state.calculatePositions());
    }

    /**
     * Apply a marshal lap adjustment and rebroadcast positions. Called by
     * {@code RaceStateMachineService.recordAdjustment} after persisting the
     * {@code MarshalAdjustment} row.
     */
    public void applyMarshalAdjustment(long scheduleId, long entryId, int lapDelta, MarshalAdjustmentDto dto) {
        LiveRaceState state = stateFor(scheduleId);
        synchronized (state) {
            state.applyLapDelta(entryId, lapDelta);
        }
        liveTimingHub.broadcastMarshalAdjustment(scheduleId, dto);
        liveTimingHub.broadcastTimingUpdate(scheduleId, state.calculatePositions());
    }

    /** Releases in-memory state for a finished race, freeing memory. Idempotent. */
    public void releaseState(long scheduleId) {
        states.remove(scheduleId);
    }

    /**
     * Pre-loads entry display names into the given state so calculatePositions() can include
     * driver names without hitting the DB on every lap. Wrapped in try/catch so a lookup failure
     * never breaks ingestion.
     */
    private void loadEntryNames(long scheduleId, LiveRaceState state) {
        try {
            List<CachedRaceEntry> raceEntries =
                    cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(scheduleId);
            for (CachedRaceEntry raceEntry : raceEntries) {
                Long cachedEntryId = raceEntry.getCachedEntryId();
                if (cachedEntryId == null) {
                    continue;
                }
                cachedEntryRepository.findById(cachedEntryId)
                        .ifPresent(entry -> state.putEntryName(cachedEntryId, entry.getRacerName()));
            }
        } catch (Exception e) {
            log.warn("Failed to load entry names for schedule {}: {}", scheduleId, e.getMessage());
        }
    }

    /** Resolves a transponder number to a local {@code CachedEntry} id, or null if unknown. */
    private Long resolveEntryId(String transponderNumber) {
        return cachedEntryRepository.findByTransponderNumber(transponderNumber)
                .map(CachedEntry::getId)
                .orElse(null);
    }
}
