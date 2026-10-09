package dev.monkeypatch.rctiming.persistence;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TimestampsIT extends AbstractIntegrationTest {

    @Autowired
    private RacingClassRepository racingClasses;

    @Test
    void saveStampsCreationOnceAndUpdateEveryTime() throws InterruptedException {
        RacingClass racingClass = new RacingClass();
        racingClass.setName("Timestamps " + System.nanoTime());

        RacingClass saved = racingClasses.save(racingClass);
        try {
            Instant created = saved.getCreatedAt();
            assertThat(created).isNotNull().isEqualTo(created.truncatedTo(ChronoUnit.MICROS));
            assertThat(saved.getUpdatedAt()).isEqualTo(created);

            Thread.sleep(2);
            saved.setName(saved.getName() + " renamed");
            racingClasses.save(saved);

            RacingClass reloaded = racingClasses.getOrThrow(saved.getId());
            assertThat(reloaded.getCreatedAt()).isEqualTo(created);
            assertThat(reloaded.getUpdatedAt()).isAfter(created);
        } finally {
            racingClasses.deleteById(saved.getId());
        }
    }

    @Test
    void aCreationTimeAlreadySetIsKept() {
        RacingClass racingClass = new RacingClass();
        racingClass.setName("Timestamps kept " + System.nanoTime());
        Instant earlier = Instant.parse("2026-01-02T03:04:05.123456Z");
        racingClass.setCreatedAt(earlier);

        RacingClass saved = racingClasses.save(racingClass);
        try {
            assertThat(racingClasses.getOrThrow(saved.getId()).getCreatedAt()).isEqualTo(earlier);
        } finally {
            racingClasses.deleteById(saved.getId());
        }
    }
}
