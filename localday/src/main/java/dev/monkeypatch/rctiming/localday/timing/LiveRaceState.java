package dev.monkeypatch.rctiming.localday.timing;

import dev.monkeypatch.rctiming.localday.timing.dto.LiveTimingRowDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-race (per-schedule-entry) in-memory position model. NOT a Spring bean — one instance per
 * {@code cachedScheduleId}, held in {@link LapTimingService}'s map.
 *
 * <p>Ported from the cloud's {@code app/.../timing/LiveRaceState.java} (KD3 — same target
 * behavior, independently implemented, no shared code between {@code :app} and
 * {@code :localday}). Transponder-linking / retroactive-credit support (the cloud's
 * {@code retroactiveLinkTransponder}) is out of this unit's scope and intentionally not ported.
 *
 * <p>All mutating methods are synchronized on {@code this}. Position recalculation is
 * O(n log n) over typically &le;40 entries — negligible latency.
 *
 * <p>Gap calculation note: gapToLeader = leaderLastPassingTimeMs - myLastPassingTimeMs. This is
 * a simplified same-logical-clock calculation that works well for races where all entries are
 * on the same lap. Time gaps are only meaningful for cars on the same lap.
 */
public class LiveRaceState {

    final long cachedScheduleId;
    final Map<Long, LiveRacePosition> positions = new HashMap<>();
    final List<LapPassingEvent> lapHistory = new ArrayList<>();
    final Set<String> seenUnknownTransponders = new HashSet<>();
    /** Entry display names: entryId -> racer name, populated by LapTimingService on state creation. */
    final Map<Long, String> entryNames = new HashMap<>();
    /** Fastest lap recorded across ALL entries this race session. */
    Long overallBestLapMs = null;

    public LiveRaceState(long cachedScheduleId) {
        this.cachedScheduleId = cachedScheduleId;
    }

    public long getCachedScheduleId() {
        return cachedScheduleId;
    }

    /** Stores a display name for an entry. Called by LapTimingService at state creation time. */
    public void putEntryName(long entryId, String displayName) {
        entryNames.put(entryId, displayName);
    }

    /**
     * Apply a lap passing event. Returns true if this is the first sighting of an unknown
     * transponder. If entryId is null, the transponder is unknown — tracked in
     * seenUnknownTransponders (and NOT double-counted on a second unknown passing from the same
     * transponder). All mutations are synchronized.
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

        // Update best lap, last lap duration, running average, and race overall best
        if (prevPassingTime > 0) {
            long lapMs = passingTimeMs - prevPassingTime;
            if (lapMs > 0) {
                pos.setLastLapMs(lapMs);
                pos.accumulateLap(lapMs);
                pos.getLapTimes().add(lapMs);
                Long currentBest = pos.getBestLapMs();
                if (currentBest == null || lapMs < currentBest) {
                    pos.setBestLapMs(lapMs);
                }
                if (overallBestLapMs == null || lapMs < overallBestLapMs) {
                    overallBestLapMs = lapMs;
                }
            }
        }

        return false;
    }

    /** Apply a marshal lap delta (+/-1). Synchronized. */
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
     * Sorted by: lapsCompleted DESC, lastPassingTimeMs ASC (earlier finish = better position on
     * same lap). Time gaps are only meaningful for cars on the same lap. Cars on fewer laps carry
     * lapsDown &gt; 0.
     */
    public synchronized List<LiveTimingRowDto> calculatePositions() {
        List<LiveRacePosition> sorted = positions.values().stream()
                .sorted(Comparator
                        .comparingInt(LiveRacePosition::getLapsCompleted).reversed()
                        .thenComparingLong(LiveRacePosition::getLastPassingTimeMs))
                .toList();

        List<LiveTimingRowDto> result = new ArrayList<>(sorted.size());
        int leaderLaps = sorted.isEmpty() ? 0 : sorted.get(0).getLapsCompleted();
        Long leaderLastPassing = sorted.isEmpty() ? null : sorted.get(0).getLastPassingTimeMs();
        Long prevLastPassing = null;
        int prevLaps = leaderLaps;

        for (int i = 0; i < sorted.size(); i++) {
            LiveRacePosition pos = sorted.get(i);
            int position = i + 1;
            int lapsDown = leaderLaps - pos.getLapsCompleted();
            int intervalLapsDown = prevLaps - pos.getLapsCompleted();

            // Time gaps only valid between cars on the same lap; null them when laps differ
            Long gapToLeader = (i == 0 || leaderLastPassing == null || lapsDown > 0) ? null
                    : Math.abs(leaderLastPassing - pos.getLastPassingTimeMs());
            Long gapToAhead = (i == 0 || prevLastPassing == null || intervalLapsDown > 0) ? null
                    : Math.abs(prevLastPassing - pos.getLastPassingTimeMs());

            result.add(new LiveTimingRowDto(
                    pos.getEntryId(),
                    entryNames.getOrDefault(pos.getEntryId(), "Entry " + pos.getEntryId()),
                    position,
                    pos.getLapsCompleted(),
                    pos.getLastPassingTimeMs(),
                    pos.getLastLapMs(),
                    pos.getBestLapMs(),
                    pos.getAvgLapMs(),
                    overallBestLapMs,
                    lapsDown,
                    intervalLapsDown,
                    gapToLeader,
                    gapToAhead
            ));
            prevLastPassing = pos.getLastPassingTimeMs();
            prevLaps = pos.getLapsCompleted();
        }
        return result;
    }

    public List<LapPassingEvent> getLapHistory() {
        return lapHistory;
    }

    /**
     * Returns a read-only snapshot of a single entry's position for use by a future
     * result-snapshot unit. Called when the race is FINISHED and no further lap events are
     * processed. Not needed by this unit — kept for forward compatibility, mirroring the cloud's
     * shape.
     */
    public synchronized LiveRacePosition getPositionSnapshot(long entryId) {
        return positions.get(entryId);
    }
}
