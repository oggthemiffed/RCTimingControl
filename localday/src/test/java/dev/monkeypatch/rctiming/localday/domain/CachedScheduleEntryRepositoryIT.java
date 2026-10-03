package dev.monkeypatch.rctiming.localday.domain;

import dev.monkeypatch.rctiming.localday.race.RaceState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link CachedScheduleEntryRepository#findFirstByStatus(RaceState)} —
 * the finder {@link dev.monkeypatch.rctiming.localday.timing.DecoderListenerLifecycle} uses to
 * resolve "the currently active race" against the real embedded Postgres instance.
 *
 * <p>Mirrors {@code CachedRepositoryIT}'s established {@code @SpringBootTest} +
 * {@code @DynamicPropertySource} + {@code @TempDir} + {@code @DirtiesContext(AFTER_CLASS)}
 * pattern for this module's real-embedded-Postgres integration tests.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CachedScheduleEntryRepositoryIT {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void localdayProperties(DynamicPropertyRegistry registry) {
        registry.add("localday.datasource.embedded-postgres.data-directory",
                () -> dataDir.resolve("pg").toString());
    }

    @Autowired
    private CachedScheduleEntryRepository cachedScheduleEntryRepository;

    @Test
    void findFirstByStatus_returnsTheRunningRace_whenOneExists() {
        CachedScheduleEntry pending = new CachedScheduleEntry();
        pending.setCloudRaceId(801L);
        pending.setRoundNumber(1);
        pending.setHeatNumber(1);
        pending.setSequence(1);
        pending.setClassName("Buggy Stock");
        pending.setStatus(RaceState.PENDING);
        cachedScheduleEntryRepository.save(pending);

        CachedScheduleEntry running = new CachedScheduleEntry();
        running.setCloudRaceId(802L);
        running.setRoundNumber(1);
        running.setHeatNumber(2);
        running.setSequence(2);
        running.setClassName("Buggy Stock");
        running.setStatus(RaceState.RUNNING);
        CachedScheduleEntry savedRunning = cachedScheduleEntryRepository.save(running);

        Optional<CachedScheduleEntry> found = cachedScheduleEntryRepository.findFirstByStatus(RaceState.RUNNING);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(savedRunning.getId());
    }

    @Test
    void findFirstByStatus_returnsEmpty_whenNoRaceHasThatStatus() {
        CachedScheduleEntry pending = new CachedScheduleEntry();
        pending.setCloudRaceId(901L);
        pending.setRoundNumber(1);
        pending.setHeatNumber(1);
        pending.setSequence(1);
        pending.setClassName("Touring Stock");
        pending.setStatus(RaceState.PENDING);
        cachedScheduleEntryRepository.save(pending);

        // Query for STOPPED rather than RUNNING: this test class shares one embedded-Postgres
        // instance (and no per-test rollback) across all @Test methods in the class, so the
        // sibling test's RUNNING row may already exist depending on execution order. STOPPED is
        // a status no test in this class ever persists, so this assertion holds regardless of
        // method execution order.
        Optional<CachedScheduleEntry> found = cachedScheduleEntryRepository.findFirstByStatus(RaceState.STOPPED);

        assertThat(found).isEmpty();
    }
}
