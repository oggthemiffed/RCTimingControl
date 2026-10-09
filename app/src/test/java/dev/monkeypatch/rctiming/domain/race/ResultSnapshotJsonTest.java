package dev.monkeypatch.rctiming.domain.race;

import dev.monkeypatch.rctiming.domain.race.ResultSnapshotDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultSnapshotJsonTest {

    private final ResultSnapshotJson json = new ResultSnapshotJson(Jackson2ObjectMapperBuilder.json().build());

    @Test
    void readsBackWhatItWrote() {
        var row = new ResultSnapshotDto.ResultRow(1, 7L, 3L, "Sam Driver", null, 12, 31_500L, 2_400L, null);

        List<ResultSnapshotDto.ResultRow> read = json.positions(5L, json.write(5L, List.of(row)));

        assertThat(read).containsExactly(row);
    }

    @Test
    void readsBackLapHistory() {
        var lap = new ResultSnapshotDto.PositionAtLap(3, 7L, 2, 31_000L);

        assertThat(json.lapHistory(5L, json.write(5L, List.of(lap)))).containsExactly(lap);
    }

    @Test
    void noStoredResultReadsAsNoRows() {
        assertThat(json.positions(5L, null)).isEmpty();
        assertThat(json.lapHistory(5L, null)).isEmpty();
        assertThat(json.positions(5L, "null")).isEmpty();
    }

    @Test
    void anUnreadableResultFailsNamingTheRace() {
        assertThatThrownBy(() -> json.positions(5L, "{\"not\": \"rows\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The stored result of race 5 could not be read");
    }
}
