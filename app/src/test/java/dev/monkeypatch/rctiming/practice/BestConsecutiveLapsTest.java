package dev.monkeypatch.rctiming.practice;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for the sliding-window best-N-consecutive-laps algorithm in LivePracticeState.
 * No Spring context required — exercises LivePracticeState directly.
 */
class BestConsecutiveLapsTest {

    @Test
    void bestConsecutiveN_fewerThanNLaps_returnsNull() {
        LivePracticeState state = new LivePracticeState(1L, 3);
        state.recordLap("T1", 1L, "Racer", 20000L, Instant.now());
        state.recordLap("T1", 1L, "Racer", 21000L, Instant.now());

        List<dev.monkeypatch.rctiming.practice.dto.PracticeTimingRowDto> rows = state.calculatePositions();
        assertNull(rows.get(0).bestConsecutiveNMs(),
                "Should return null when fewer than N laps recorded");
    }

    @Test
    void bestConsecutiveN_exactlyNLaps_returnsSumOfAll() {
        LivePracticeState state = new LivePracticeState(1L, 3);
        state.recordLap("T1", 1L, "Racer", 20000L, Instant.now());
        state.recordLap("T1", 1L, "Racer", 21000L, Instant.now());
        state.recordLap("T1", 1L, "Racer", 22000L, Instant.now());

        List<dev.monkeypatch.rctiming.practice.dto.PracticeTimingRowDto> rows = state.calculatePositions();
        assertEquals(63000L, rows.get(0).bestConsecutiveNMs(),
                "Exactly N laps: sum of all three");
    }

    @Test
    void bestConsecutiveN_moreThanNLaps_returnsMinWindow() {
        LivePracticeState state = new LivePracticeState(1L, 3);
        // First window: 25000+21000+20000 = 66000
        // Second window: 21000+20000+19000 = 60000 ← best
        state.recordLap("T1", 1L, "Racer", 25000L, Instant.now());
        state.recordLap("T1", 1L, "Racer", 21000L, Instant.now());
        state.recordLap("T1", 1L, "Racer", 20000L, Instant.now());
        state.recordLap("T1", 1L, "Racer", 19000L, Instant.now());

        List<dev.monkeypatch.rctiming.practice.dto.PracticeTimingRowDto> rows = state.calculatePositions();
        assertEquals(60000L, rows.get(0).bestConsecutiveNMs(),
                "Best window: 21000+20000+19000=60000");
    }

    @Test
    void bestConsecutiveN_bestWindowAtEnd_findsIt() {
        LivePracticeState state = new LivePracticeState(1L, 3);
        // Windows: [25+24+23=72], [24+23+18=65], [23+18+17=58], [18+17+16=51] ← best
        state.recordLap("T1", 1L, "Racer", 25000L, Instant.now());
        state.recordLap("T1", 1L, "Racer", 24000L, Instant.now());
        state.recordLap("T1", 1L, "Racer", 23000L, Instant.now());
        state.recordLap("T1", 1L, "Racer", 18000L, Instant.now());
        state.recordLap("T1", 1L, "Racer", 17000L, Instant.now());
        state.recordLap("T1", 1L, "Racer", 16000L, Instant.now());

        List<dev.monkeypatch.rctiming.practice.dto.PracticeTimingRowDto> rows = state.calculatePositions();
        assertEquals(51000L, rows.get(0).bestConsecutiveNMs(),
                "Best window at end: 18000+17000+16000=51000");
    }
}
