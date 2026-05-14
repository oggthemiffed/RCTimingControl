package dev.monkeypatch.rctiming.query.results;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.ResultSnapshot;
import dev.monkeypatch.rctiming.domain.race.ResultSnapshotRepository;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import dev.monkeypatch.rctiming.domain.race.StartType;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RacerResultHistoryQuery (Phase 7, Plan 03).
 * Tests: query returns only results for the requesting user (IDOR safety — RESULT-03);
 *        empty list when user has no results.
 */
class RacerResultHistoryQueryTest extends AbstractIntegrationTest {

    @Autowired
    RacerResultHistoryQuery query;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EntryRepository entryRepository;

    @Autowired
    RacingClassRepository racingClassRepository;

    @Autowired
    RoundRepository roundRepository;

    @Autowired
    RaceRepository raceRepository;

    @Autowired
    RaceEntryRepository raceEntryRepository;

    @Autowired
    ResultSnapshotRepository resultSnapshotRepository;

    @Autowired
    DSLContext dsl;

    private Long racingClassId;

    private User makeUser(String firstName) {
        User u = new User();
        u.setEmail(firstName.toLowerCase() + "-" + UUID.randomUUID() + "@history-test.com");
        u.setPasswordHash(passwordEncoder.encode("pass"));
        u.setFirstName(firstName);
        u.setLastName("Tester");
        u.setRoles(Set.of(Role.RACER));
        Instant now = Instant.now();
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        return userRepository.save(u);
    }

    private Event makeEvent(String name) {
        Event e = new Event();
        e.setName(name);
        e.setEventDate(LocalDate.of(2026, 3, 15));
        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return eventRepository.save(e);
    }

    private Long makeEventClass(Long eventId) {
        return dsl.insertInto(EVENT_CLASSES)
                .set(EVENT_CLASSES.EVENT_ID, eventId)
                .set(EVENT_CLASSES.RACING_CLASS_ID, racingClassId)
                .set(EVENT_CLASSES.CONFIG_SNAPSHOT, JSONB.valueOf("{\"type\":\"TIMED\"}"))
                .returning(EVENT_CLASSES.ID)
                .fetchOne()
                .get(EVENT_CLASSES.ID);
    }

    private Round makeRound(Long eventId) {
        Round r = new Round();
        r.setEventId(eventId);
        r.setType(RoundType.FINAL);
        r.setRoundNumber(1);
        r.setSequenceInEvent(1);
        Instant now = Instant.now();
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        return roundRepository.save(r);
    }

    private Race makeRace(Long roundId, Long eventClassId) {
        Race race = new Race();
        race.setRoundId(roundId);
        race.setEventClassId(eventClassId);
        race.setHeatNumber(1);
        race.setSequenceInRound(1);
        race.setFinalLetter("A");
        race.setStartType(StartType.GRID);
        race.setStatus(RaceStatus.FINISHED);
        Instant now = Instant.now();
        race.setCreatedAt(now);
        race.setUpdatedAt(now);
        race.setFinishedAt(now);
        return raceRepository.save(race);
    }

    private Entry makeEntry(Long userId, Long eventId, Long eventClassId) {
        Entry e = new Entry();
        e.setUserId(userId);
        e.setEventId(eventId);
        e.setEventClassId(eventClassId);
        e.setTransponderNumberSnapshot("T-" + UUID.randomUUID().toString().substring(0, 8));
        e.setStatus(EntryStatus.CONFIRMED);
        Instant now = Instant.now();
        e.setSubmittedAt(now);
        e.setUpdatedAt(now);
        return entryRepository.save(e);
    }

    private void linkRaceEntry(Long raceId, Long entryId) {
        RaceEntry re = new RaceEntry();
        re.setRaceId(raceId);
        re.setEntryId(entryId);
        raceEntryRepository.save(re);
    }

    private void makeSnapshot(Long raceId, Long entryId, int position) {
        ResultSnapshot snap = new ResultSnapshot();
        snap.setRaceId(raceId);
        snap.setPositionsJson(String.format(
                "[{\"position\":%d,\"entryId\":%d,\"driverName\":\"Racer\",\"carNumber\":\"1\","
                + "\"lapsCompleted\":10,\"totalTimeMs\":60000,\"bestLapMs\":6000,\"gapToLeaderMs\":0}]",
                position, entryId));
        snap.setLapHistoryJson("[]");
        snap.setFinishedAt(Instant.now());
        snap.setCreatedAt(Instant.now());
        resultSnapshotRepository.save(snap);
    }

    @BeforeEach
    void setUp() {
        // TRUNCATE CASCADE clears all test data atomically — shared Testcontainers DB accumulates
        // rows across tests and sequences don't roll back, so FK-ordered deletes risk partial clears.
        dsl.execute("TRUNCATE TABLE result_snapshots, race_entries, races, entries, rounds, " +
                "event_classes, events, users, racing_classes RESTART IDENTITY CASCADE");

        RacingClass rc = new RacingClass();
        rc.setName("Sport-" + UUID.randomUUID());
        Instant now = Instant.now();
        rc.setCreatedAt(now);
        rc.setUpdatedAt(now);
        racingClassId = racingClassRepository.save(rc).getId();
    }

    @Test
    void returnsOnlyResultsForRequestingUser() {
        // Two users — user A has an entry at event1, user B has no entries
        User userA = makeUser("UserA");
        User userB = makeUser("UserB");

        Event event1 = makeEvent("Isolation Test Event");
        Long ecId = makeEventClass(event1.getId());
        Round round = makeRound(event1.getId());
        Race race = makeRace(round.getId(), ecId);

        // User A entered and has a result
        Entry entryA = makeEntry(userA.getId(), event1.getId(), ecId);
        linkRaceEntry(race.getId(), entryA.getId());
        makeSnapshot(race.getId(), entryA.getId(), 1);

        // Query for user A — should see 1 event with 1 race
        List<RacerResultHistoryDto> resultA = query.findForUser(userA.getId());
        assertThat(resultA).hasSize(1);
        assertThat(resultA.get(0).eventId()).isEqualTo(event1.getId());
        assertThat(resultA.get(0).races()).hasSize(1);
        assertThat(resultA.get(0).races().get(0).position()).isEqualTo(1);

        // Query for user B — must return empty (IDOR safety)
        List<RacerResultHistoryDto> resultB = query.findForUser(userB.getId());
        assertThat(resultB).isEmpty();
    }

    @Test
    void emptyListWhenUserHasNoResults() {
        // A brand-new user with no entries at all
        User newUser = makeUser("NewUser");

        List<RacerResultHistoryDto> result = query.findForUser(newUser.getId());
        assertThat(result).isEmpty();
    }
}
