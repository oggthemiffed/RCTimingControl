package dev.monkeypatch.rctiming.api.boards;

import dev.monkeypatch.rctiming.domain.competitor.CompetitorService;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryRepository;
import dev.monkeypatch.rctiming.domain.entry.EntryStatus;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.event.EventStatus;
import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.EventClassRepository;
import dev.monkeypatch.rctiming.domain.format.QualifyingType;
import dev.monkeypatch.rctiming.domain.format.TimedRaceConfig;
import dev.monkeypatch.rctiming.domain.race.Race;
import dev.monkeypatch.rctiming.domain.race.RaceEntry;
import dev.monkeypatch.rctiming.domain.race.RaceEntryRepository;
import dev.monkeypatch.rctiming.domain.race.RaceRepository;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.Round;
import dev.monkeypatch.rctiming.domain.race.RoundRepository;
import dev.monkeypatch.rctiming.domain.race.RoundStatus;
import dev.monkeypatch.rctiming.domain.race.RoundType;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Seeds one event's schedule for the board ITs. Races it leaves running are finished by {@link #cleanUp()}. */
class BoardFixtures {

    private final EventRepository eventRepository;
    private final RacingClassRepository racingClassRepository;
    private final EventClassRepository eventClassRepository;
    private final RoundRepository roundRepository;
    private final RaceRepository raceRepository;
    private final EntryRepository entryRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final CompetitorService competitorService;

    private final List<Long> raceIds = new ArrayList<>();

    final String suffix = UUID.randomUUID().toString().substring(0, 8);
    final Event event;
    final String className;
    final EventClass eventClass;

    BoardFixtures(EventRepository eventRepository,
                  RacingClassRepository racingClassRepository,
                  EventClassRepository eventClassRepository,
                  RoundRepository roundRepository,
                  RaceRepository raceRepository,
                  EntryRepository entryRepository,
                  RaceEntryRepository raceEntryRepository,
                  CompetitorService competitorService) {
        this.eventRepository = eventRepository;
        this.racingClassRepository = racingClassRepository;
        this.eventClassRepository = eventClassRepository;
        this.roundRepository = roundRepository;
        this.raceRepository = raceRepository;
        this.entryRepository = entryRepository;
        this.raceEntryRepository = raceEntryRepository;
        this.competitorService = competitorService;

        Instant now = Instant.now();
        Event e = new Event();
        e.setName("Board Event " + suffix);
        e.setEventDate(LocalDate.now());
        e.setStatus(EventStatus.IN_PROGRESS);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        this.event = eventRepository.save(e);

        this.className = "Touring " + suffix;
        RacingClass rc = new RacingClass();
        rc.setName(className);
        rc.setCreatedAt(now);
        rc.setUpdatedAt(now);
        rc = racingClassRepository.save(rc);

        EventClass ec = new EventClass();
        ec.setEventId(event.getId());
        ec.setRacingClassId(rc.getId());
        ec.setConfigSnapshot(new TimedRaceConfig(5,
                dev.monkeypatch.rctiming.domain.format.StartType.ROLLING,
                QualifyingType.FASTEST_LAP, 1, 3));
        ec.setCreatedAt(now);
        ec.setUpdatedAt(now);
        this.eventClass = eventClassRepository.save(ec);
    }

    Round round(RoundType type, int roundNumber, int sequenceInEvent) {
        Instant now = Instant.now();
        Round round = new Round();
        round.setEventId(event.getId());
        round.setType(type);
        round.setRoundNumber(roundNumber);
        round.setSequenceInEvent(sequenceInEvent);
        round.setStatus(RoundStatus.PENDING);
        round.setCreatedAt(now);
        round.setUpdatedAt(now);
        return roundRepository.save(round);
    }

    Race race(Round round, int heatNumber, RaceStatus status) {
        Instant now = Instant.now();
        Race race = new Race();
        race.setRoundId(round.getId());
        race.setEventClassId(eventClass.getId());
        race.setHeatNumber(heatNumber);
        race.setSequenceInRound(heatNumber);
        race.setStartType(dev.monkeypatch.rctiming.domain.race.StartType.GRID);
        race.setStatus(status);
        if (status != RaceStatus.PENDING && status != RaceStatus.GRID) {
            race.setStartedAt(now);
        }
        if (status == RaceStatus.FINISHED) {
            race.setFinishedAt(now);
        }
        race.setCreatedAt(now);
        race.setUpdatedAt(now);
        race = raceRepository.save(race);
        raceIds.add(race.getId());
        return race;
    }

    Race save(Race race) {
        return raceRepository.save(race);
    }

    /** A walk-in competitor's entry on the grid of {@code race}, timed on {@code transponder}. */
    Entry gridEntry(Race race, String name, String transponder, int gridPosition) {
        Instant now = Instant.now();
        Entry entry = new Entry();
        entry.setCompetitorId(competitorService.createWalkIn(name).getId());
        entry.setEventId(event.getId());
        entry.setEventClassId(eventClass.getId());
        entry.setStatus(EntryStatus.CONFIRMED);
        entry.setTransponderNumberSnapshot(transponder);
        entry.setSubmittedAt(now);
        entry.setUpdatedAt(now);
        entry = entryRepository.save(entry);

        RaceEntry re = new RaceEntry();
        re.setRaceId(race.getId());
        re.setEntryId(entry.getId());
        re.setGridPosition(gridPosition);
        raceEntryRepository.save(re);
        return entry;
    }

    /**
     * Finishes any race this fixture left on track, so the shared database never keeps a
     * running race that another test's "racing now" lookup could pick up.
     */
    void cleanUp() {
        for (Long id : raceIds) {
            raceRepository.findById(id).ifPresent(race -> {
                if (race.getStatus() == RaceStatus.RUNNING || race.getStatus() == RaceStatus.STOPPED
                        || race.getStatus() == RaceStatus.GRID) {
                    race.setStatus(RaceStatus.FINISHED);
                    race.setFinishedAt(Instant.now());
                    raceRepository.save(race);
                }
            });
        }
        Event e = eventRepository.findById(event.getId()).orElseThrow();
        e.setStatus(EventStatus.COMPLETED);
        eventRepository.save(e);
    }
}
