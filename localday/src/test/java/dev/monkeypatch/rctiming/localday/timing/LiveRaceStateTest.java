package dev.monkeypatch.rctiming.localday.timing;

import dev.monkeypatch.rctiming.localday.timing.dto.LiveTimingRowDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link LiveRaceState} — no Spring context, plain JUnit 5 + AssertJ.
 */
class LiveRaceStateTest {

    private static final long SCHEDULE_ID = 1L;
    private static final long ENTRY_A = 100L;
    private static final long ENTRY_B = 200L;

    private LapPassingEvent passingFor(String transponder, long rtcTimeMicros) {
        return new LapPassingEvent(SCHEDULE_ID, transponder, rtcTimeMicros);
    }

    // --- Happy path ---

    @Test
    void applyLapPassing_incrementsLapsAndComputesLapTimesForOneEntry() {
        LiveRaceState state = new LiveRaceState(SCHEDULE_ID);

        // Three passings; first passing is deliberately NOT at t=0 micros, since 0 is
        // indistinguishable from LiveRacePosition.lastPassingTimeMs's unset sentinel value —
        // using it as a real timestamp would silently suppress lap-time computation on the
        // second passing (matches production data, where rtcTimeMicros is an epoch timestamp
        // and never actually 0).
        state.applyLapPassing(passingFor("1234567", 1_000_000L), ENTRY_A);
        state.applyLapPassing(passingFor("1234567", 11_000_000L), ENTRY_A);
        state.applyLapPassing(passingFor("1234567", 26_000_000L), ENTRY_A);

        List<LiveTimingRowDto> rows = state.calculatePositions();
        assertThat(rows).hasSize(1);

        LiveTimingRowDto row = rows.get(0);
        assertThat(row.entryId()).isEqualTo(ENTRY_A);
        assertThat(row.lapsCompleted()).isEqualTo(3);
        assertThat(row.lastLapMs()).isEqualTo(15_000L);
        assertThat(row.bestLapMs()).isEqualTo(10_000L);
        assertThat(row.avgLapMs()).isEqualTo((10_000L + 15_000L) / 2);
        assertThat(row.overallFastestLapMs()).isEqualTo(10_000L);
    }

    @Test
    void calculatePositions_sortsByLapsCompletedDescThenLastPassingTimeAsc() {
        LiveRaceState state = new LiveRaceState(SCHEDULE_ID);

        // Entry A completes 2 laps.
        state.applyLapPassing(passingFor("1111111", 1_000_000L), ENTRY_A);
        state.applyLapPassing(passingFor("1111111", 11_000_000L), ENTRY_A);

        // Entry B completes only 1 lap, later than A's first lap.
        state.applyLapPassing(passingFor("2222222", 6_000_000L), ENTRY_B);

        List<LiveTimingRowDto> rows = state.calculatePositions();
        assertThat(rows).hasSize(2);

        assertThat(rows.get(0).entryId()).isEqualTo(ENTRY_A);
        assertThat(rows.get(0).position()).isEqualTo(1);
        assertThat(rows.get(0).lapsCompleted()).isEqualTo(2);
        assertThat(rows.get(0).lapsDown()).isEqualTo(0);

        assertThat(rows.get(1).entryId()).isEqualTo(ENTRY_B);
        assertThat(rows.get(1).position()).isEqualTo(2);
        assertThat(rows.get(1).lapsCompleted()).isEqualTo(1);
        assertThat(rows.get(1).lapsDown()).isEqualTo(1);
        // Cars on different lap counts: time gap is not meaningful, must be null.
        assertThat(rows.get(1).gapToLeaderMs()).isNull();
        assertThat(rows.get(1).gapToAheadMs()).isNull();
    }

    // --- Edge case: unknown transponders ---

    @Test
    void applyLapPassing_unknownTransponder_firstSightingReturnsTrue_secondReturnsFalse() {
        LiveRaceState state = new LiveRaceState(SCHEDULE_ID);

        boolean firstSighting = state.applyLapPassing(passingFor("9999999", 0L), null);
        boolean secondSighting = state.applyLapPassing(passingFor("9999999", 10_000_000L), null);

        assertThat(firstSighting).isTrue();
        assertThat(secondSighting).isFalse();

        // Unknown transponder passings must not create a position row.
        assertThat(state.calculatePositions()).isEmpty();
    }

    @Test
    void applyLapPassing_differentUnknownTransponders_eachCountsAsFirstSighting() {
        LiveRaceState state = new LiveRaceState(SCHEDULE_ID);

        boolean first = state.applyLapPassing(passingFor("1111111", 0L), null);
        boolean second = state.applyLapPassing(passingFor("2222222", 0L), null);

        assertThat(first).isTrue();
        assertThat(second).isTrue();
    }
}
