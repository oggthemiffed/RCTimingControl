package dev.monkeypatch.rctiming.localday.race;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: {@link MarshalAdjustment} round-trips through the real embedded-Postgres
 * repository, and the new {@code started_at}/{@code finished_at} columns on
 * {@link CachedScheduleEntry} round-trip correctly (null when unset, a value when set).
 *
 * <p>Mirrors {@code CachedRepositoryIT}'s established {@code @SpringBootTest} +
 * {@code @DynamicPropertySource} + {@code @TempDir} + {@code @DirtiesContext(AFTER_CLASS)}
 * pattern for this module's real-embedded-Postgres integration tests.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MarshalAdjustmentRepositoryIT {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void localdayProperties(DynamicPropertyRegistry registry) {
        registry.add("localday.datasource.embedded-postgres.data-directory",
                () -> dataDir.resolve("pg").toString());
    }

    @Autowired
    private CachedScheduleEntryRepository cachedScheduleEntryRepository;

    @Autowired
    private CachedEntryRepository cachedEntryRepository;

    @Autowired
    private MarshalAdjustmentRepository marshalAdjustmentRepository;

    @Test
    void savesAndReloadsMarshalAdjustment() {
        CachedScheduleEntry schedule = new CachedScheduleEntry();
        schedule.setCloudRaceId(101L);
        schedule.setRoundNumber(1);
        schedule.setHeatNumber(1);
        schedule.setSequence(1);
        schedule.setClassName("Buggy Stock");
        schedule.setStatus(RaceState.RUNNING);
        CachedScheduleEntry savedSchedule = cachedScheduleEntryRepository.save(schedule);

        CachedEntry entry = new CachedEntry();
        entry.setCloudEntryId(202L);
        entry.setTransponderNumber("1112223");
        entry.setRacerName("Grace Hopper");
        CachedEntry savedEntry = cachedEntryRepository.save(entry);

        MarshalAdjustment adjustment = new MarshalAdjustment();
        adjustment.setRaceId(savedSchedule.getId());
        adjustment.setEntryId(savedEntry.getId());
        adjustment.setTransponderNumber("1112223");
        adjustment.setLapDelta(1);
        adjustment.setRaceStateAtTime("RUNNING");
        adjustment.setActingUserId(55L);
        adjustment.setActingUserName("Referee Alice");
        adjustment.setAdjustedAt(Instant.now());

        MarshalAdjustment saved = marshalAdjustmentRepository.save(adjustment);
        assertThat(saved.getId()).isNotNull();

        Optional<MarshalAdjustment> reloaded = marshalAdjustmentRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getRaceId()).isEqualTo(savedSchedule.getId());
        assertThat(reloaded.get().getEntryId()).isEqualTo(savedEntry.getId());
        assertThat(reloaded.get().getTransponderNumber()).isEqualTo("1112223");
        assertThat(reloaded.get().getLapDelta()).isEqualTo(1);
        assertThat(reloaded.get().getRaceStateAtTime()).isEqualTo("RUNNING");
        assertThat(reloaded.get().getActingUserId()).isEqualTo(55L);
        assertThat(reloaded.get().getActingUserName()).isEqualTo("Referee Alice");

        List<MarshalAdjustment> found =
                marshalAdjustmentRepository.findAllByRaceIdOrderByAdjustedAtAsc(savedSchedule.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(saved.getId());
    }

    @Test
    void cachedScheduleEntryStartedAtAndFinishedAtRoundTrip() {
        CachedScheduleEntry schedule = new CachedScheduleEntry();
        schedule.setCloudRaceId(303L);
        schedule.setRoundNumber(2);
        schedule.setHeatNumber(1);
        schedule.setSequence(1);
        schedule.setClassName("Touring Stock");
        // status defaults to PENDING, startedAt/finishedAt default to null
        CachedScheduleEntry savedPending = cachedScheduleEntryRepository.save(schedule);

        Optional<CachedScheduleEntry> reloadedPending = cachedScheduleEntryRepository.findById(savedPending.getId());
        assertThat(reloadedPending).isPresent();
        assertThat(reloadedPending.get().getStatus()).isEqualTo(RaceState.PENDING);
        assertThat(reloadedPending.get().getStartedAt()).isNull();
        assertThat(reloadedPending.get().getFinishedAt()).isNull();

        Instant started = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant finished = started.plusSeconds(300);
        reloadedPending.get().setStatus(RaceState.FINISHED);
        reloadedPending.get().setStartedAt(started);
        reloadedPending.get().setFinishedAt(finished);
        cachedScheduleEntryRepository.save(reloadedPending.get());

        Optional<CachedScheduleEntry> reloadedFinished = cachedScheduleEntryRepository.findById(savedPending.getId());
        assertThat(reloadedFinished).isPresent();
        assertThat(reloadedFinished.get().getStatus()).isEqualTo(RaceState.FINISHED);
        assertThat(reloadedFinished.get().getStartedAt()).isEqualTo(started);
        assertThat(reloadedFinished.get().getFinishedAt()).isEqualTo(finished);
    }
}
