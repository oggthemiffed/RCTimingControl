package dev.monkeypatch.rctiming.localday.race;

import dev.monkeypatch.rctiming.localday.domain.CachedEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedEntryRepository;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntry;
import dev.monkeypatch.rctiming.localday.domain.CachedRaceEntryRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: {@link CachedRaceEntry} round-trips through the real embedded-Postgres
 * repository, and {@link RoundGeneratorService#applyPreviousRoundFinishingOrder} correctly
 * persists grid-position updates through the actual JPA/Postgres stack (not just mocks).
 *
 * <p>Mirrors {@code MarshalAdjustmentRepositoryIT}'s established {@code @SpringBootTest} +
 * {@code @DynamicPropertySource} + {@code @TempDir} + {@code @DirtiesContext(AFTER_CLASS)}
 * pattern for this module's real-embedded-Postgres integration tests.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CachedRaceEntryRepositoryIT {

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
    private CachedRaceEntryRepository cachedRaceEntryRepository;

    @Autowired
    private RoundGeneratorService roundGeneratorService;

    @Test
    void applyPreviousRoundFinishingOrder_roundTripsGridPositionsThroughRealPersistence() {
        CachedScheduleEntry schedule = new CachedScheduleEntry();
        schedule.setCloudRaceId(501L);
        schedule.setRoundNumber(2);
        schedule.setHeatNumber(1);
        schedule.setSequence(5);
        schedule.setClassName("Touring Stock");
        CachedScheduleEntry savedSchedule = cachedScheduleEntryRepository.save(schedule);

        CachedEntry entryA = new CachedEntry();
        entryA.setCloudEntryId(601L);
        entryA.setTransponderNumber("1111111");
        entryA.setRacerName("Alan Turing");
        CachedEntry savedEntryA = cachedEntryRepository.save(entryA);

        CachedEntry entryB = new CachedEntry();
        entryB.setCloudEntryId(602L);
        entryB.setTransponderNumber("2222222");
        entryB.setRacerName("Barbara Liskov");
        CachedEntry savedEntryB = cachedEntryRepository.save(entryB);

        CachedRaceEntry raceEntryA = new CachedRaceEntry();
        raceEntryA.setCachedScheduleId(savedSchedule.getId());
        raceEntryA.setCachedEntryId(savedEntryA.getId());
        cachedRaceEntryRepository.save(raceEntryA);

        CachedRaceEntry raceEntryB = new CachedRaceEntry();
        raceEntryB.setCachedScheduleId(savedSchedule.getId());
        raceEntryB.setCachedEntryId(savedEntryB.getId());
        cachedRaceEntryRepository.save(raceEntryB);

        // Both start with no grid position assigned yet.
        List<CachedRaceEntry> beforeSeeding =
                cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(savedSchedule.getId());
        assertThat(beforeSeeding).hasSize(2);
        assertThat(beforeSeeding).allMatch(e -> e.getGridPosition() == null);

        // Entry B finished ahead of Entry A in the previous round's same heat.
        roundGeneratorService.applyPreviousRoundFinishingOrder(
                savedSchedule.getId(), List.of(savedEntryB.getId(), savedEntryA.getId()));

        List<CachedRaceEntry> afterSeeding =
                cachedRaceEntryRepository.findByCachedScheduleIdOrderByGridPositionAsc(savedSchedule.getId());
        assertThat(afterSeeding).hasSize(2);

        Optional<CachedRaceEntry> reloadedA = afterSeeding.stream()
                .filter(e -> e.getCachedEntryId().equals(savedEntryA.getId())).findFirst();
        Optional<CachedRaceEntry> reloadedB = afterSeeding.stream()
                .filter(e -> e.getCachedEntryId().equals(savedEntryB.getId())).findFirst();

        assertThat(reloadedA).isPresent();
        assertThat(reloadedB).isPresent();
        assertThat(reloadedB.get().getGridPosition()).isEqualTo(1);
        assertThat(reloadedA.get().getGridPosition()).isEqualTo(2);

        // findByCachedScheduleIdOrderByGridPositionAsc must actually order by grid position.
        assertThat(afterSeeding.get(0).getId()).isEqualTo(reloadedB.get().getId());
        assertThat(afterSeeding.get(1).getId()).isEqualTo(reloadedA.get().getId());
    }
}
