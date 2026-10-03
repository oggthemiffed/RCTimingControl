package dev.monkeypatch.rctiming.localday.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: saving entities via their JPA repositories round-trips correctly against
 * the real embedded PostgreSQL instance, including the JSONB {@code config} column on
 * {@link CachedFormatConfig}.
 *
 * <p>Uses the full {@code @SpringBootTest} context (rather than {@code @DataJpaTest}) because
 * {@code @DataJpaTest} would try to substitute an in-memory embedded database, which is
 * precisely what this module does not want — the whole point is to exercise the real embedded
 * Postgres wiring end to end.
 *
 * <p>{@code @DirtiesContext(AFTER_CLASS)} forces Spring to close this context (stopping the
 * embedded Postgres process) at the end of this test class rather than deferring to a later
 * JVM-shutdown hook — see {@code LocalDayApplicationTests} for why that ordering matters with
 * a static {@code @TempDir}.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CachedRepositoryIT {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void localdayProperties(DynamicPropertyRegistry registry) {
        registry.add("localday.datasource.embedded-postgres.data-directory",
                () -> dataDir.resolve("pg").toString());
    }

    @Autowired
    private CachedEntryRepository cachedEntryRepository;

    @Autowired
    private CachedFormatConfigRepository cachedFormatConfigRepository;

    @Autowired
    private CachedScheduleEntryRepository cachedScheduleEntryRepository;

    @Autowired
    private LapPassingRepository lapPassingRepository;

    @Test
    void savesAndReloadsCachedEntry() {
        CachedEntry entry = new CachedEntry();
        entry.setCloudEntryId(4242L);
        entry.setTransponderNumber("1234567");
        entry.setRacerName("Ada Lovelace");
        entry.setCarName("TC-01");
        entry.setClassName("Touring Stock");

        CachedEntry saved = cachedEntryRepository.save(entry);
        assertThat(saved.getId()).isNotNull();

        Optional<CachedEntry> reloaded = cachedEntryRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getRacerName()).isEqualTo("Ada Lovelace");
        assertThat(reloaded.get().getTransponderNumber()).isEqualTo("1234567");

        assertThat(cachedEntryRepository.findByTransponderNumber("1234567")).isPresent();
    }

    @Test
    void savesAndReloadsCachedFormatConfigIncludingJsonbColumn() {
        CachedFormatConfig config = new CachedFormatConfig();
        config.setCloudFormatId(99L);
        config.setName("5-minute qualifier");
        config.setConfig("""
                {"type":"TIMED","durationSeconds":300,"overtimeLap":true}
                """.trim());

        CachedFormatConfig saved = cachedFormatConfigRepository.save(config);
        assertThat(saved.getId()).isNotNull();

        Optional<CachedFormatConfig> reloaded = cachedFormatConfigRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        // Postgres's jsonb column type re-serializes on storage (key order preserved, whitespace
        // normalized to "key": value) — assert on parsed shape, not exact byte-for-byte text.
        assertThat(reloaded.get().getConfig()).contains("\"type\"", "\"TIMED\"");
        assertThat(reloaded.get().getConfig()).contains("\"durationSeconds\"", "300");
    }

    @Test
    void savesAndReloadsLapPassingLinkedToScheduleEntry() {
        CachedScheduleEntry schedule = new CachedScheduleEntry();
        schedule.setCloudRaceId(7L);
        schedule.setRoundNumber(1);
        schedule.setHeatNumber(2);
        schedule.setSequence(3);
        schedule.setClassName("Buggy Stock");
        CachedScheduleEntry savedSchedule = cachedScheduleEntryRepository.save(schedule);

        LapPassing passing = new LapPassing();
        passing.setCachedScheduleId(savedSchedule.getId());
        passing.setTransponderNumber("7654321");
        passing.setPassingAt(Instant.now());
        passing.setLapTimeMs(18_432L);
        passing.setLapNumber(5);
        passing.setRawDecoderLine("1\t100\t7654321\t123.456\t1\t63\t0\tABCD");

        LapPassing savedPassing = lapPassingRepository.save(passing);
        assertThat(savedPassing.getId()).isNotNull();

        var found = lapPassingRepository.findAllByCachedScheduleIdOrderByPassingAtAsc(savedSchedule.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getLapTimeMs()).isEqualTo(18_432L);
    }
}
