package dev.monkeypatch.rctiming.query.racecontrol;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.api.racecontrol.dto.RunOrderItemDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/** The run order lists a round's races in the order the generator numbered them, not heat by heat (#137). */
class RunOrderQueryIT extends AbstractIntegrationTest {

    @Autowired RunOrderQuery runOrderQuery;
    @Autowired JdbcTemplate jdbc;

    private long eventId;
    private long firstClassId;
    private long secondClassId;

    @BeforeEach
    void setUp() {
        String run = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        eventId = jdbc.queryForObject("""
                insert into events (name, event_date, status)
                values (?, '2026-10-18', 'IN_PROGRESS') returning id""", Long.class, "Run order " + run);
        firstClassId = eventClass("Run order Buggy " + run);
        secondClassId = eventClass("Run order Truck " + run);
    }

    @AfterEach
    void tearDown() {
        List<Long> racingClassIds = jdbc.queryForList(
                "select racing_class_id from event_classes where event_id = ?", Long.class, eventId);
        jdbc.update("delete from races where round_id in (select id from rounds where event_id = ?)", eventId);
        jdbc.update("delete from rounds where event_id = ?", eventId);
        jdbc.update("delete from event_classes where event_id = ?", eventId);
        racingClassIds.forEach(id -> jdbc.update("delete from racing_classes where id = ?", id));
        jdbc.update("delete from events where id = ?", eventId);
    }

    @Test
    void aRoundWithTwoClassesRunsEachClassThroughItsHeatsInTurn() {
        long round = jdbc.queryForObject("""
                insert into rounds (event_id, type, round_number, sequence_in_event)
                values (?, 'QUALIFIER', 1, 1) returning id""", Long.class, eventId);
        // The generator numbers the races class by class: buggy heats 1 and 2, then truck heats 1 and 2
        long buggyOne = race(round, firstClassId, 1, 1);
        long buggyTwo = race(round, firstClassId, 2, 2);
        long truckOne = race(round, secondClassId, 1, 3);
        long truckTwo = race(round, secondClassId, 2, 4);

        List<Long> order = runOrderQuery.findForEvent(eventId).stream().map(RunOrderItemDto::raceId).toList();

        // Ordering by heat number alone would give buggy 1, truck 1, buggy 2, truck 2
        assertThat(order).containsExactly(buggyOne, buggyTwo, truckOne, truckTwo);
    }

    private long eventClass(String className) {
        long racingClassId = jdbc.queryForObject("insert into racing_classes (name) values (?) returning id",
                Long.class, className);
        return jdbc.queryForObject("""
                insert into event_classes (event_id, racing_class_id, config_snapshot)
                values (?, ?, '{"type":"TIMED","durationMinutes":5}') returning id""",
                Long.class, eventId, racingClassId);
    }

    private long race(long roundId, long eventClassId, int heat, int sequenceInRound) {
        return jdbc.queryForObject("""
                insert into races (round_id, event_class_id, heat_number, sequence_in_round, start_type, status)
                values (?, ?, ?, ?, 'STAGGER', 'PENDING') returning id""",
                Long.class, roundId, eventClassId, heat, sequenceInRound);
    }
}
