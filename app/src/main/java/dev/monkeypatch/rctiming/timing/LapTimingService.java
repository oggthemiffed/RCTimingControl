package dev.monkeypatch.rctiming.timing;

import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.timing.dto.MarshalAdjustmentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory race state manager for live timing.
 * Holds a ConcurrentHashMap<Long, LiveRaceState> keyed by raceId.
 * All mutations of a given LiveRaceState go through synchronized blocks (Pitfall 3).
 * Positions are calculated in memory and broadcast over STOMP — never persisted during a race.
 */
@Service
public class LapTimingService {

    private static final Logger log = LoggerFactory.getLogger(LapTimingService.class);

    private final Map<Long, LiveRaceState> states = new ConcurrentHashMap<>();
    private final LiveTimingHub liveTimingHub;
    private final RaceEntryRepository raceEntryRepository;
    private final EntryRepository entryRepository;

    public LapTimingService(LiveTimingHub liveTimingHub,
                            RaceEntryRepository raceEntryRepository,
                            EntryRepository entryRepository) {
        this.liveTimingHub = liveTimingHub;
        this.raceEntryRepository = raceEntryRepository;
        this.entryRepository = entryRepository;
    }

    /**
     * Returns existing LiveRaceState for this raceId, or creates a new one.
     */
    public LiveRaceState stateFor(long raceId) {
        return states.computeIfAbsent(raceId, LiveRaceState::new);
    }

    /**
     * Read-only access — returns the state if present, empty if race has had no lap events yet.
     */
    public Optional<LiveRaceState> peek(long raceId) {
        return Optional.ofNullable(states.get(raceId));
    }

    /**
     * Handles a LapPassingEvent published by the ApplicationEventPublisher.
     * Resolves transponder number → entry ID via Entry.transponderNumberSnapshot,
     * updates in-memory state, and broadcasts over STOMP.
     */
    @EventListener(LapPassingEvent.class)
    @Async
    public void onLapPassing(LapPassingEvent event) {
        long raceId = event.raceId();
        String transponderNumber = event.transponderNumber();

        // Resolve transponder → entry for this race
        Long entryId = resolveEntryId(raceId, transponderNumber);

        LiveRaceState state = stateFor(raceId);
        boolean firstUnknown = state.applyLapPassing(event, entryId);

        if (entryId == null && firstUnknown) {
            liveTimingHub.broadcastUnknownTransponder(raceId, transponderNumber);
        }

        liveTimingHub.broadcastTimingUpdate(raceId, state.calculatePositions());
    }

    /**
     * Apply a marshal lap adjustment and rebroadcast positions.
     * Called by RaceControlController after persisting the MarshalAdjustment row.
     */
    public void applyMarshalAdjustment(long raceId, long entryId, int lapDelta, MarshalAdjustmentDto dto) {
        LiveRaceState state = stateFor(raceId);
        synchronized (state) {
            state.applyLapDelta(entryId, lapDelta);
        }
        liveTimingHub.broadcastMarshalAdjustment(raceId, dto);
        liveTimingHub.broadcastTimingUpdate(raceId, state.calculatePositions());
    }

    /**
     * Releases in-memory state for a finished race, freeing memory.
     * Idempotent — no-op if no state is present.
     */
    public void releaseState(long raceId) {
        states.remove(raceId);
    }

    /**
     * Phase 5 / TIMING-08: links an unknown transponder to an entry for the given race.
     * Retroactively credits all passings from that transponder since race start and
     * broadcasts updated positions via STOMP.
     */
    public List<dev.monkeypatch.rctiming.timing.dto.LiveTimingRowDto> linkTransponder(
            long raceId, String transponderNumber, long entryId) {
        LiveRaceState state = stateFor(raceId);
        List<dev.monkeypatch.rctiming.timing.dto.LiveTimingRowDto> positions =
                state.retroactiveLinkTransponder(transponderNumber, entryId);
        liveTimingHub.broadcastTimingUpdate(raceId, positions);
        return positions;
    }

    /**
     * Phase 5: returns the count of lapHistory entries matching the given transponder number.
     * Used by TransponderLinkController to report lapsCredited before linking.
     */
    public int countPassingsForTransponder(long raceId, String transponderNumber) {
        LiveRaceState state = stateFor(raceId);
        return state.countPassingsForTransponder(transponderNumber);
    }

    /**
     * Resolves a transponder number to an entry ID for the given race.
     * Scans the race's RaceEntry rows, loads each Entry, and matches transponderNumberSnapshot.
     */
    private Long resolveEntryId(long raceId, String transponderNumber) {
        try {
            List<RaceEntry> raceEntries = raceEntryRepository.findByRaceIdOrderByGridPosition(raceId);
            for (RaceEntry raceEntry : raceEntries) {
                Optional<Entry> entry = entryRepository.findById(raceEntry.getEntryId());
                if (entry.isPresent() && transponderNumber.equals(entry.get().getTransponderNumberSnapshot())) {
                    return raceEntry.getEntryId();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve transponder {} for race {}: {}", transponderNumber, raceId, e.getMessage());
        }
        return null;
    }
}
