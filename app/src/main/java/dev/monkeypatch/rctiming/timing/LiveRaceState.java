package dev.monkeypatch.rctiming.timing;

import dev.monkeypatch.rctiming.timing.dto.LiveTimingRowDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-race in-memory position model (Pattern 4 from RESEARCH.md).
 * NOT a Spring bean — one instance per raceId, held in LapTimingService.states ConcurrentHashMap.
 *
 * <p>All mutating methods are synchronized on {@code this} (intrinsic lock, Pitfall 3).
 * Position recalculation is O(n log n) over typically ≤40 entries — negligible latency.
 *
 * <p>Gap calculation note: gapToLeader = leaderLastPassingTimeMs - myLastPassingTimeMs.
 * This is a simplified same-logical-clock calculation that works well for races where all
 * entries are on the same lap. Multi-lap gap calculation (position by laps then time) is
 * deferred to Phase 7 results refinement.
 */
public class LiveRaceState {

    final long raceId;
    final Map<Long, LiveRacePosition> positions = new HashMap<>();
    final List<LapPassingEvent> lapHistory = new ArrayList<>();
    final Set<String> seenUnknownTransponders = new HashSet<>();
    Instant raceStartTime;

    public LiveRaceState(long raceId) {
        this.raceId = raceId;
    }

    public long getRaceId() {
        return raceId;
    }

    /**
     * Apply a lap passing event. Returns true if this is the first sighting of an unknown transponder.
     * If entryId is null, the transponder is unknown — tracked in seenUnknownTransponders.
     * All mutations are synchronized.
     */
    public synchronized boolean applyLapPassing(LapPassingEvent event, Long entryId) {
        lapHistory.add(event);
        long passingTimeMs = event.rtcTimeMicros() / 1000L;

        if (entryId == null) {
            // Unknown transponder — track first sighting
            return seenUnknownTransponders.add(event.transponderNumber());
        }

        LiveRacePosition pos = positions.computeIfAbsent(entryId, id -> {
            LiveRacePosition p = new LiveRacePosition();
            p.setEntryId(id);
            return p;
        });

        long prevPassingTime = pos.getLastPassingTimeMs();
        pos.setLapsCompleted(pos.getLapsCompleted() + 1);
        pos.setLastPassingTimeMs(passingTimeMs);

        // Update best lap if we have a previous passing time
        if (prevPassingTime > 0) {
            long lapMs = passingTimeMs - prevPassingTime;
            if (lapMs > 0) {
                Long currentBest = pos.getBestLapMs();
                if (currentBest == null || lapMs < currentBest) {
                    pos.setBestLapMs(lapMs);
                }
            }
        }

        return false;
    }

    /**
     * Apply a marshal lap delta (±1). Synchronized.
     */
    public synchronized void applyLapDelta(long entryId, int lapDelta) {
        LiveRacePosition pos = positions.computeIfAbsent(entryId, id -> {
            LiveRacePosition p = new LiveRacePosition();
            p.setEntryId(id);
            return p;
        });
        pos.setLapsCompleted(Math.max(0, pos.getLapsCompleted() + lapDelta));
    }

    /**
     * Calculate current positions and return a sorted snapshot.
     * Sorted by: lapsCompleted DESC, lastPassingTimeMs ASC (earlier finish = better position on same lap).
     * Gap to leader = leaderLastPassingMs - myLastPassingMs (same-logical-clock approximation).
     */
    public synchronized List<LiveTimingRowDto> calculatePositions() {
        List<LiveRacePosition> sorted = positions.values().stream()
                .sorted(Comparator
                        .comparingInt(LiveRacePosition::getLapsCompleted).reversed()
                        .thenComparingLong(LiveRacePosition::getLastPassingTimeMs))
                .toList();

        List<LiveTimingRowDto> result = new ArrayList<>(sorted.size());
        Long leaderLastPassing = sorted.isEmpty() ? null : sorted.get(0).getLastPassingTimeMs();
        Long prevLastPassing = null;

        for (int i = 0; i < sorted.size(); i++) {
            LiveRacePosition pos = sorted.get(i);
            int position = i + 1;
            Long gapToLeader = (i == 0 || leaderLastPassing == null) ? null
                    : leaderLastPassing - pos.getLastPassingTimeMs();
            Long gapToAhead = (i == 0 || prevLastPassing == null) ? null
                    : prevLastPassing - pos.getLastPassingTimeMs();
            result.add(new LiveTimingRowDto(
                    pos.getEntryId(),
                    position,
                    pos.getLapsCompleted(),
                    pos.getLastPassingTimeMs(),
                    pos.getBestLapMs(),
                    gapToLeader,
                    gapToAhead
            ));
            prevLastPassing = pos.getLastPassingTimeMs();
        }
        return result;
    }

    public List<LapPassingEvent> getLapHistory() {
        return lapHistory;
    }

    /**
     * Phase 5 / D-12: retroactively credit all lapHistory entries for an unknown transponder
     * to the now-identified entry. Removes the transponder from the unknown set and recalculates
     * positions. Must be called inside a synchronized context (method is synchronized).
     *
     * <p>Uses {@link #applyPositionUpdate} (not {@link #applyLapPassing}) to avoid
     * re-adding events to lapHistory during iteration, which would cause
     * ConcurrentModificationException and duplicate history entries.
     *
     * @return recalculated position list; empty list if no matching lapHistory entries.
     */
    public synchronized List<LiveTimingRowDto> retroactiveLinkTransponder(
            String transponderNumber, long entryId) {
        for (LapPassingEvent event : lapHistory) {
            if (transponderNumber.equals(event.transponderNumber())) {
                applyPositionUpdate(event, entryId);
            }
        }
        seenUnknownTransponders.remove(transponderNumber);
        return calculatePositions();
    }

    /**
     * Phase 5: counts lapHistory entries for a given transponder number.
     * Used by TransponderLinkController to report lapsCredited before linking.
     */
    public synchronized int countPassingsForTransponder(String transponderNumber) {
        return (int) lapHistory.stream()
                .filter(e -> transponderNumber.equals(e.transponderNumber()))
                .count();
    }

    /**
     * Updates the in-memory position state for a passing event WITHOUT adding to lapHistory.
     * Used by retroactiveLinkTransponder to replay historical events for a newly-linked entry.
     */
    private void applyPositionUpdate(LapPassingEvent event, Long entryId) {
        long passingTimeMs = event.rtcTimeMicros() / 1000L;
        if (entryId == null) {
            return;
        }
        LiveRacePosition pos = positions.computeIfAbsent(entryId, id -> {
            LiveRacePosition p = new LiveRacePosition();
            p.setEntryId(id);
            return p;
        });
        long prevPassingTime = pos.getLastPassingTimeMs();
        pos.setLapsCompleted(pos.getLapsCompleted() + 1);
        pos.setLastPassingTimeMs(passingTimeMs);
        if (prevPassingTime > 0) {
            long lapMs = passingTimeMs - prevPassingTime;
            if (lapMs > 0) {
                Long currentBest = pos.getBestLapMs();
                if (currentBest == null || lapMs < currentBest) {
                    pos.setBestLapMs(lapMs);
                }
            }
        }
    }
}
