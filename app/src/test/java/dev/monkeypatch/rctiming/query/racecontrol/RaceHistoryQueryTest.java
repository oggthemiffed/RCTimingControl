package dev.monkeypatch.rctiming.query.racecontrol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RaceHistoryQueryTest {

    @Test
    void readable_dropsTheEmailFromAnOfficialsLabel() {
        assertThat(RaceHistoryQuery.readable("Race Director <director@club.test>")).isEqualTo("Race Director");
    }

    @Test
    void readable_namesTheSystemJobWithoutItsPrefix() {
        assertThat(RaceHistoryQuery.readable("system:bump-up")).isEqualTo("System (bump-up)");
    }

    @Test
    void readable_keepsALabelWithNoEmailAndPassesNullThrough() {
        assertThat(RaceHistoryQuery.readable("official #7")).isEqualTo("official #7");
        assertThat(RaceHistoryQuery.readable(null)).isNull();
    }
}
