package dev.monkeypatch.rctiming.practice;

import dev.monkeypatch.rctiming.practice.dto.PracticeTimingRowDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory practice session timing state.
 * One instance per active practice session, held in PracticeTimingService.
 * Thread-safe via ConcurrentHashMap and synchronized compute operations.
 *
 * Sorted by: laps desc, then best lap asc (consistent with live race display).
 * Best-N consecutive laps uses O(n) sliding window algorithm.
 */
public class LivePracticeState {

    private final Long sessionId;
    private final int bestLapN;
    private final Map<String, ParticipantState> participants = new ConcurrentHashMap<>();

    public LivePracticeState(Long sessionId, int bestLapN) {
        this.sessionId = sessionId;
        this.bestLapN = bestLapN;
    }

    public Long getSessionId() {
        return sessionId;
    }

    /**
     * Record a lap passing for a transponder.
     * If lapTimeMs is null (first crossing), increments crossing count only.
     */
    public void recordLap(String transponderNumber, Long userId, String racerName,
                          Long lapTimeMs, Instant crossingTime) {
        participants.compute(transponderNumber, (k, state) -> {
            if (state == null) {
                state = new ParticipantState(transponderNumber, userId, racerName);
            }
            // Update user info if we just resolved an unknown transponder
            if (userId != null && state.userId == null) {
                state.userId = userId;
                state.racerName = racerName;
            }
            state.addLap(lapTimeMs, crossingTime);
            return state;
        });
    }

    /**
     * Link an unknown transponder to a user (retroactive/on-the-fly linking).
     */
    public void linkTransponder(String transponderNumber, Long userId, String racerName) {
        ParticipantState state = participants.get(transponderNumber);
        if (state != null) {
            state.userId = userId;
            state.racerName = racerName;
        }
    }

    /**
     * Calculate positions and return timing rows.
     * Sorted by: laps desc (more laps = better), then best lap asc (lower = better).
     */
    public List<PracticeTimingRowDto> calculatePositions() {
        List<ParticipantState> sorted = new ArrayList<>(participants.values());
        sorted.sort((a, b) -> {
            // Lap count comparison (higher is better)
            int lapCompare = Integer.compare(b.getLapCount(), a.getLapCount());
            if (lapCompare != 0) return lapCompare;
            // Best lap comparison (lower is better), null-safe
            Long aBest = a.getBestLap();
            Long bBest = b.getBestLap();
            if (aBest == null && bBest == null) return 0;
            if (aBest == null) return 1;
            if (bBest == null) return -1;
            return Long.compare(aBest, bBest);
        });

        List<PracticeTimingRowDto> rows = new ArrayList<>();
        int pos = 1;
        for (ParticipantState p : sorted) {
            rows.add(new PracticeTimingRowDto(
                    pos++,
                    p.transponderNumber,
                    p.userId,
                    p.racerName != null ? p.racerName : "Unknown #" + p.transponderNumber,
                    p.getLapCount(),
                    p.getBestLap(),
                    p.getBestConsecutiveN(bestLapN),
                    p.getLastLap(),
                    p.userId == null
            ));
        }
        return rows;
    }

    /**
     * Returns transponder numbers that have not been linked to a user.
     */
    public Set<String> getUnknownTransponders() {
        Set<String> unknown = new HashSet<>();
        for (ParticipantState p : participants.values()) {
            if (p.userId == null) {
                unknown.add(p.transponderNumber);
            }
        }
        return unknown;
    }

    // ---------------------------------------------------------------------------
    // Internal participant state
    // ---------------------------------------------------------------------------

    static class ParticipantState {
        String transponderNumber;
        Long userId;
        String racerName;
        /** Only lap times with non-null lapTimeMs (real completed laps). */
        final List<Long> lapTimes = new ArrayList<>();
        /** Total crossings including the first (which has no lap time). */
        int crossingCount = 0;
        Instant lastCrossing;

        ParticipantState(String transponderNumber, Long userId, String racerName) {
            this.transponderNumber = transponderNumber;
            this.userId = userId;
            this.racerName = racerName;
        }

        void addLap(Long lapTimeMs, Instant crossingTime) {
            crossingCount++;
            if (lapTimeMs != null && lapTimeMs > 0) {
                lapTimes.add(lapTimeMs);
            }
            lastCrossing = crossingTime;
        }

        int getLapCount() {
            return lapTimes.size();
        }

        Long getBestLap() {
            return lapTimes.stream().min(Long::compareTo).orElse(null);
        }

        Long getLastLap() {
            return lapTimes.isEmpty() ? null : lapTimes.get(lapTimes.size() - 1);
        }

        /**
         * Sliding window: find minimum sum of N consecutive lap times.
         * O(n) algorithm: initialise first window, slide right.
         * Returns null if fewer than N completed laps.
         */
        Long getBestConsecutiveN(int n) {
            if (lapTimes.size() < n) {
                return null;
            }
            long windowSum = 0;
            for (int i = 0; i < n; i++) {
                windowSum += lapTimes.get(i);
            }
            long best = windowSum;
            for (int i = n; i < lapTimes.size(); i++) {
                windowSum += lapTimes.get(i) - lapTimes.get(i - n);
                if (windowSum < best) {
                    best = windowSum;
                }
            }
            return best;
        }
    }
}
