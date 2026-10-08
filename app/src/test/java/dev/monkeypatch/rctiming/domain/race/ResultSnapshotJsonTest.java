package dev.monkeypatch.rctiming.domain.race;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.racecontrol.dto.ResultSnapshotDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultSnapshotJsonTest {

    private final ResultSnapshotJson json = new ResultSnapshotJson(new ObjectMapper());

    @Test
    void readsBackWhatItWrote() {
        var row = new ResultSnapshotDto.ResultRow(1, 7L, 3L, "Sam Driver", null, 12, 31_500L, 2_400L, null);

        List<ResultSnapshotDto.ResultRow> read = json.positions(5L, json.write(5L, List.of(row)));

        assertThat(read).containsExactly(row);
    }

    @Test
    void noStoredResultReadsAsNoRows() {
        assertThat(json.positions(5L, null)).isEmpty();
        assertThat(json.lapHistory(5L, null)).isEmpty();
    }

    @Test
    void anUnreadableResultFailsNamingTheRace() {
        assertThatThrownBy(() -> json.positions(5L, "{\"not\": \"rows\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The stored result of race 5 could not be read");
    }
}
