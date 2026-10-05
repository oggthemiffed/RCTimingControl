package dev.monkeypatch.rctiming.service;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import dev.monkeypatch.rctiming.service.dto.RoundGenerationRequest;
import dev.monkeypatch.rctiming.service.dto.RoundGenerationRequest.ClassFinalsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generating rounds, seeding finals and bumping drivers up, against a real database with its
 * foreign keys and unique constraints (#45). The unit tests mock the repositories, so they never
 * caught the placeholder rows that pointed at entry 0.
 */
class FinalsGenerationIT extends AbstractIntegrationTest {

    @Autowired RoundGeneratorService roundGeneratorService;
    @Autowired BumpUpSeedingService bumpUpSeedingService;
    @Autowired RaceRepository raceRepository;
    @Autowired RaceEntryRepository raceEntryRepository;
    @Autowired JdbcTemplate jdbc;

    private String run;
    private long eventId;
    private final List<Long> competitorIds = new ArrayList<>();
    private final List<Long> racingClassIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        run = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        eventId = jdbc.queryForObject("""
                insert into events (name, event_date, status)
                values (?, date('now'), 'IN_PROGRESS') returning id""",
                Long.class, "Finals event " + run);
    }

    /** The test database is shared, so leave no in-progress event behind for the boards to pick up. */
    @AfterEach
    void tearDown() {
        jdbc.update("delete from rounds where event_id = ?", eventId); // races and race_entries cascade
        jdbc.update("delete from entries where event_id = ?", eventId);
        jdbc.update("delete from event_classes where event_id = ?", eventId);
        jdbc.update("delete from events where id = ?", eventId);
        competitorIds.forEach(id -> jdbc.update("delete from competitors where id = ?", id));
        racingClassIds.forEach(id -> jdbc.update("delete from racing_classes where id = ?", id));
    }

    @Test
    void generatesRoundsSeedsFinalsAndBumpsDriversUp() {
        long buggyClass = addClass("Buggy");
        long truckClass = addClass("Truck");
        List<Long> buggies = addEntries(buggyClass, 20);
        addEntries(truckClass, 6);

        roundGeneratorService.generate(new RoundGenerationRequest(eventId, 0, 1, 10, List.of(
                new ClassFinalsConfig(buggyClass, 2, 10, 2),
                new ClassFinalsConfig(truckClass, 1, 10, 0))));

        List<Race> buggyFinals = raceRepository.findByEventClassIdAndRoundType(buggyClass, RoundType.FINAL);
        assertThat(buggyFinals).extracting(Race::getFinalLetter).containsExactlyInAnyOrder("A", "B");
        assertThat(raceRepository.findByEventClassIdAndRoundType(truckClass, RoundType.FINAL)).hasSize(1);
        // Finals start with empty grids
        for (Race f : buggyFinals) {
            assertThat(raceEntryRepository.findByRaceIdOrderByGridPosition(f.getId())).isEmpty();
        }

        // Qualifying order: the buggies in the order they were added
        bumpUpSeedingService.seedFinals(buggyClass, buggies, 2, 10, 2);

        Race aFinal = finalOf(buggyClass, "A");
        Race bFinal = finalOf(buggyClass, "B");
        assertThat(aFinal.getBumpSlots()).isEqualTo(2);
        assertThat(bFinal.getBumpSlots()).isZero();
        assertThat(gridOf(aFinal)).extracting(RaceEntry::getEntryId).containsExactlyElementsOf(buggies.subList(0, 8));
        assertThat(gridOf(bFinal)).extracting(RaceEntry::getEntryId).containsExactlyElementsOf(buggies.subList(10, 20));

        // Seeding again replaces the grids rather than adding to them
        bumpUpSeedingService.seedFinals(buggyClass, buggies, 2, 10, 2);
        assertThat(gridOf(aFinal)).hasSize(8);
        assertThat(gridOf(bFinal)).hasSize(10);

        // The B-final finishes in reverse grid order; its top two go up
        List<Long> bFinishingOrder = new ArrayList<>(buggies.subList(10, 20));
        Collections.reverse(bFinishingOrder);
        List<Long> promoted = bumpUpSeedingService.applyBumpUpResults(bFinal.getId(), bFinishingOrder);

        assertThat(promoted).containsExactly(buggies.get(19), buggies.get(18));
        List<RaceEntry> aGrid = gridOf(aFinal);
        assertThat(aGrid).hasSize(10);
        assertThat(aGrid.subList(8, 10)).allMatch(RaceEntry::isBumped);
        assertThat(aGrid.subList(8, 10)).extracting(RaceEntry::getEntryId)
                .containsExactly(buggies.get(19), buggies.get(18));
        assertThat(aGrid.subList(8, 10)).extracting(RaceEntry::getGridPosition).containsExactly(9, 10);
        assertThat(aGrid.subList(8, 10)).extracting(RaceEntry::getCarNumber).containsExactly(9, 10);

        // A repeat (race finished again after a resume) promotes nobody else
        assertThat(bumpUpSeedingService.applyBumpUpResults(bFinal.getId(), bFinishingOrder)).isEmpty();
        assertThat(gridOf(aFinal)).hasSize(10);

        // Every race_entries row for this event points at a real entry
        Integer orphans = jdbc.queryForObject("""
                select count(*) from race_entries re
                join races r on r.id = re.race_id join rounds ro on ro.id = r.round_id
                left join entries e on e.id = re.entry_id
                where ro.event_id = ? and e.id is null""", Integer.class, eventId);
        assertThat(orphans).isZero();
    }

    private long addClass(String name) {
        long racingClassId = jdbc.queryForObject(
                "insert into racing_classes (name) values (?) returning id", Long.class, name + " " + run);
        racingClassIds.add(racingClassId);
        return jdbc.queryForObject("""
                insert into event_classes (event_id, racing_class_id, config_snapshot)
                values (?, ?, '{"type":"TIMED"}') returning id""", Long.class, eventId, racingClassId);
    }

    private List<Long> addEntries(long eventClassId, int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            long competitorId = jdbc.queryForObject(
                    "insert into competitors (display_name) values (?) returning id", Long.class,
                    "Driver " + eventClassId + "-" + i + " " + run);
            competitorIds.add(competitorId);
            ids.add(jdbc.queryForObject("""
                    insert into entries (event_id, event_class_id, competitor_id, transponder_number, status)
                    values (?, ?, ?, ?, 'CONFIRMED') returning id""", Long.class,
                    eventId, eventClassId, competitorId, String.valueOf(eventClassId * 100 + i)));
        }
        return ids;
    }

    private Race finalOf(long eventClassId, String letter) {
        return raceRepository.findByEventClassIdAndFinalLetter(eventClassId, letter).get(0);
    }

    private List<RaceEntry> gridOf(Race race) {
        return raceEntryRepository.findByRaceIdOrderByGridPosition(race.getId()).stream()
                .sorted(Comparator.comparing(RaceEntry::getGridPosition)).toList();
    }
}
