package dev.monkeypatch.rctiming.domain.race;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RaceLabelTest {

    @Test
    void heatsNameTheRoundClassAndHeat() {
        assertThat(RaceLabel.of("PRACTICE", 1, "Stock Buggy", 3, null))
                .isEqualTo("Practice 1 — Stock Buggy — Heat 3");
        assertThat(RaceLabel.of("QUALIFIER", 2, "Stock Buggy", 1, null))
                .isEqualTo("Qualifying 2 — Stock Buggy — Heat 1");
    }

    @Test
    void finalsNameTheLetterAndClassOnly() {
        assertThat(RaceLabel.of("FINAL", 1, "Stock Buggy", 1, "B")).isEqualTo("B Final — Stock Buggy");
        assertThat(RaceLabel.of("FINAL", 1, "Stock Buggy", 1, null)).isEqualTo("A Final — Stock Buggy");
    }

    @Test
    void anUnknownRoundTypeIsNamedAsStored() {
        assertThat(RaceLabel.of("TIMED", 1, "Stock Buggy", 2, null)).isEqualTo("TIMED 1 — Stock Buggy — Heat 2");
    }

    @Test
    void roundOnlyLabelsLeaveOutClassAndHeat() {
        assertThat(RaceLabel.round("QUALIFIER", 3, null)).isEqualTo("Qualifying 3");
        assertThat(RaceLabel.round("FINAL", 1, "C")).isEqualTo("C Final");
    }
}
