package dev.monkeypatch.rctiming.timing;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.format.QualifyingType;
import dev.monkeypatch.rctiming.domain.format.TimedRaceConfig;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.race.RoundStatus;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import dev.monkeypatch.rctiming.domain.race.StartType;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.monkeypatch.rctiming.persistence.RoundTrip.assertSavedAndReloaded;

/** The unknown transponder link audit repository saves and loads every field (#75). */
class UnknownTransponderLinkAuditRepositoryIT extends AbstractIntegrationTest {

    private static final Instant T1 = Instant.parse("2026-10-05T10:15:30.123456Z");
    private static final Instant T2 = T1.plus(1, ChronoUnit.HOURS);

    @Autowired UnknownTransponderLinkAuditRepository audits;
    @Autowired EventRepository events;
    @Autowired EventClassRepository eventClasses;
    @Autowired RoundRepository rounds;
    @Autowired RaceRepository races;
    @Autowired EntryRepository entries;
    @Autowired UserRepository users;

    private final List<Runnable> cleanup = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        cleanup.reversed().forEach(Runnable::run);
    }

    @Test
    void roundTrip() {
        Event event = saved(events.save(event()), events::deleteById, Event::getId);
        EventClass eventClass = saved(eventClasses.save(eventClass(event.getId())), eventClasses::deleteById,
                EventClass::getId);
        Round round = saved(rounds.save(round(event.getId())), rounds::deleteById, Round::getId);
        Race first = saved(races.save(race(round.getId(), eventClass.getId(), 1)), races::deleteById, Race::getId);
        Race second = saved(races.save(race(round.getId(), eventClass.getId(), 2)), races::deleteById, Race::getId);
        Entry entry = saved(entries.save(entry(event.getId(), eventClass.getId(), "1234567")), entries::deleteById,
                Entry::getId);
        Entry other = saved(entries.save(entry(event.getId(), eventClass.getId(), "7654321")), entries::deleteById,
                Entry::getId);
        User official = saved(users.save(official()), users::deleteById, User::getId);

        UnknownTransponderLinkAudit audit = new UnknownTransponderLinkAudit(
                null, first.getId(), "1234567", entry.getId(), official.getId(), T1);
        UnknownTransponderLinkAudit reloaded = assertSavedAndReloaded(audits, audit, a ->
                new UnknownTransponderLinkAudit(a.getId(), second.getId(), "7654321", other.getId(), null, T2),
                UnknownTransponderLinkAudit::getId);
        cleanup.add(() -> audits.deleteById(reloaded.getId()));
    }

    private <E> E saved(E entity, Consumer<Long> delete, Function<E, Long> id) {
        cleanup.add(() -> delete.accept(id.apply(entity)));
        return entity;
    }

    private static Event event() {
        Event e = new Event();
        e.setName("Link audit event");
        e.setEventDate(LocalDate.of(2026, 10, 5));
        e.setCreatedAt(T1);
        e.setUpdatedAt(T1);
        return e;
    }

    private static EventClass eventClass(Long eventId) {
        EventClass ec = new EventClass();
        ec.setEventId(eventId);
        ec.setConfigSnapshot(new TimedRaceConfig(5, dev.monkeypatch.rctiming.domain.format.StartType.STAGGER,
                QualifyingType.FTQ, 2, 3));
        ec.setCreatedAt(T1);
        ec.setUpdatedAt(T1);
        return ec;
    }

    private static Round round(Long eventId) {
        Round r = new Round();
        r.setEventId(eventId);
        r.setType(RoundType.QUALIFIER);
        r.setRoundNumber(1);
        r.setSequenceInEvent(1);
        r.setStatus(RoundStatus.PENDING);
        r.setCreatedAt(T1);
        r.setUpdatedAt(T1);
        return r;
    }

    private static Race race(Long roundId, Long eventClassId, int heat) {
        Race r = new Race();
        r.setRoundId(roundId);
        r.setEventClassId(eventClassId);
        r.setHeatNumber(heat);
        r.setSequenceInRound(heat);
        r.setStartType(StartType.STAGGER);
        r.setStatus(RaceStatus.PENDING);
        r.setCreatedAt(T1);
        r.setUpdatedAt(T1);
        return r;
    }

    private static Entry entry(Long eventId, Long eventClassId, String transponder) {
        Entry e = new Entry();
        e.setEventId(eventId);
        e.setEventClassId(eventClassId);
        e.setTransponderNumberSnapshot(transponder);
        e.setStatus(EntryStatus.CONFIRMED);
        e.setSubmittedAt(T1);
        e.setUpdatedAt(T1);
        return e;
    }

    private static User official() {
        User u = new User();
        u.setEmail("link-official-" + System.nanoTime() + "@example.com");
        u.setPasswordHash("x");
        u.setFirstName("Link");
        u.setLastName("Official");
        u.setRoles(Set.of(Role.REFEREE));
        u.setCreatedAt(T1);
        u.setUpdatedAt(T1);
        return u;
    }
}
