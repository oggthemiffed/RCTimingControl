package dev.monkeypatch.rctiming.resultsexport;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ResultsSenderBackoffTest {

    @Test
    void doublesFromThirtySecondsUpToHalfAnHour() {
        assertThat(ResultsSender.backoff(1)).isEqualTo(Duration.ofSeconds(30));
        assertThat(ResultsSender.backoff(2)).isEqualTo(Duration.ofMinutes(1));
        assertThat(ResultsSender.backoff(3)).isEqualTo(Duration.ofMinutes(2));
        assertThat(ResultsSender.backoff(7)).isEqualTo(Duration.ofMinutes(30));
        assertThat(ResultsSender.backoff(500)).isEqualTo(Duration.ofMinutes(30));
    }
}
