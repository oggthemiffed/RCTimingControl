package dev.monkeypatch.rctiming.domain.championship;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.competitor.Competitor;
import dev.monkeypatch.rctiming.domain.competitor.CompetitorRepository;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;
import dev.monkeypatch.rctiming.domain.raceclass.RacingClassRepository;
import dev.monkeypatch.rctiming.domain.user.Role;
import dev.monkeypatch.rctiming.domain.user.User;
import dev.monkeypatch.rctiming.domain.user.UserRepository;
import dev.monkeypatch.rctiming.persistence.JooqRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** The championship repositories save and load every field (#74). */
class ChampionshipRepositoriesIT extends AbstractIntegrationTest {

    private static final Instant T1 = Instant.parse("2026-10-05T10:15:30.123456Z");
    private static final Instant T2 = T1.plus(1, ChronoUnit.HOURS);

    @Autowired ChampionshipRepository championships;
    @Autowired ChampionshipClassRepository classes;
    @Autowired ChampionshipEventLinkRepository eventLinks;
    @Autowired ChampionshipExclusionRepository exclusions;
    @Autowired ChampionshipPointsScaleRepository pointsScale;
    @Autowired RacingClassRepository racingClasses;
    @Autowired EventRepository events;
    @Autowired CompetitorRepository competitors;
    @Autowired UserRepository users;

    private final List<Runnable> cleanup = new ArrayList<>();
    private Championship championship;

    @BeforeEach
    void setUp() {
        championship = championships.save(championship());
        cleanup.add(() -> championships.deleteById(championship.getId()));
    }

    @AfterEach
    void cleanUp() {
        cleanup.reversed().forEach(Runnable::run);
    }

    @Test
    void championshipRoundTrip() {
        Championship c = championship();
        c.setBestXFromYX(4);
        c.setBestXFromYY(6);
        c.setScoringSource(ScoringSource.BOTH);
        c.setTqBonusPoints(2);
        c.setAfinalWinnerBonusPoints(3);
        Championship saved = assertSavedAndReloaded(championships, c, x -> {
            x.setName("Renamed series");
            x.setBestXFromYX(null);
            x.setBestXFromYY(null);
            x.setScoringSource(ScoringSource.QUALIFYING);
            x.setTqBonusPoints(0);
            x.setAfinalWinnerBonusPoints(1);
            x.setUpdatedAt(T2);
            return x;
        }, Championship::getId);
        cleanup.add(() -> championships.deleteById(saved.getId()));
        assertCreatedAtIsInsertOnly(championships, saved, Championship::getId, Championship::getCreatedAt, saved::setCreatedAt);
    }

    @Test
    void classesRoundTripAndFinders() {
        RacingClass racingClass = racingClass();
        RacingClass otherClass = racingClass();

        ChampionshipClass cc = new ChampionshipClass();
        cc.setChampionshipId(championship.getId());
        cc.setRacingClassId(racingClass.getId());
        cc.setBestXFromYX(3);
        cc.setBestXFromYY(5);
        cc.setCreatedAt(T1);
        ChampionshipClass saved = assertSavedAndReloaded(classes, cc, x -> {
            x.setRacingClassId(otherClass.getId());
            x.setBestXFromYX(null);
            x.setBestXFromYY(null);
            return x;
        }, ChampionshipClass::getId);
        cleanup.add(() -> classes.deleteById(saved.getId()));
        assertCreatedAtIsInsertOnly(classes, saved, ChampionshipClass::getId, ChampionshipClass::getCreatedAt, saved::setCreatedAt);

        assertThat(classes.findByChampionshipId(championship.getId())).extracting(ChampionshipClass::getId)
                .containsExactly(saved.getId());
        assertThat(classes.existsByChampionshipIdAndRacingClassId(championship.getId(), otherClass.getId())).isTrue();
        assertThat(classes.existsByChampionshipIdAndRacingClassId(championship.getId(), racingClass.getId())).isFalse();
        classes.deleteByChampionshipIdAndRacingClassId(championship.getId(), otherClass.getId());
        assertThat(classes.existsById(saved.getId())).isFalse();
    }

    @Test
    void eventLinksRoundTripAndFinders() {
        Event first = event();
        Event second = event();

        ChampionshipEventLink l = new ChampionshipEventLink();
        l.setChampionshipId(championship.getId());
        l.setEventId(first.getId());
        l.setRoundNumber(2);
        l.setCreatedAt(T1);
        ChampionshipEventLink saved = assertSavedAndReloaded(eventLinks, l, x -> {
            x.setRoundNumber(3);
            return x;
        }, ChampionshipEventLink::getId);
        cleanup.add(() -> eventLinks.deleteById(saved.getId()));
        assertCreatedAtIsInsertOnly(eventLinks, saved, ChampionshipEventLink::getId, ChampionshipEventLink::getCreatedAt, saved::setCreatedAt);

        ChampionshipEventLink earlier = new ChampionshipEventLink();
        earlier.setChampionshipId(championship.getId());
        earlier.setEventId(second.getId());
        earlier.setRoundNumber(1);
        earlier.setCreatedAt(T1);
        eventLinks.save(earlier);

        assertThat(eventLinks.findByChampionshipIdOrderByRoundNumberAsc(championship.getId()))
                .extracting(ChampionshipEventLink::getRoundNumber).containsExactly(1, 3);
        assertThat(eventLinks.existsByChampionshipIdAndRoundNumber(championship.getId(), 3)).isTrue();
        assertThat(eventLinks.existsByChampionshipIdAndRoundNumber(championship.getId(), 2)).isFalse();
        assertThat(eventLinks.existsByChampionshipIdAndEventId(championship.getId(), first.getId())).isTrue();
        eventLinks.deleteByChampionshipIdAndEventId(championship.getId(), first.getId());
        assertThat(eventLinks.existsByChampionshipIdAndEventId(championship.getId(), first.getId())).isFalse();
        assertThat(eventLinks.existsById(earlier.getId())).isTrue();
    }

    @Test
    void exclusionsRoundTripAndFinders() {
        Event event = event();
        Competitor driver = competitor();
        User official = official();

        ChampionshipExclusion x = exclusion(event.getId(), driver.getId(), official.getId(), T1);
        ChampionshipExclusion saved = assertSavedAndReloaded(exclusions, x, c -> {
            c.setReason("Changed reason");
            return c;
        }, ChampionshipExclusion::getId);
        cleanup.add(() -> exclusions.deleteById(saved.getId()));
        assertCreatedAtIsInsertOnly(exclusions, saved, ChampionshipExclusion::getId, ChampionshipExclusion::getCreatedAt, saved::setCreatedAt);

        Competitor other = competitor();
        ChampionshipExclusion later = exclusions.save(exclusion(event.getId(), other.getId(), official.getId(), T2));
        cleanup.add(() -> exclusions.deleteById(later.getId()));

        assertThat(exclusions.findByChampionshipIdOrderByCreatedAtDesc(championship.getId()))
                .extracting(ChampionshipExclusion::getId).containsExactly(later.getId(), saved.getId());
        assertThat(exclusions.existsByChampionshipIdAndDriverIdAndEventId(
                championship.getId(), driver.getId(), event.getId())).isTrue();
        assertThat(exclusions.existsByChampionshipIdAndDriverIdAndEventId(
                championship.getId(), driver.getId(), -1L)).isFalse();
    }

    @Test
    void pointsScaleSavesReplacesAndDeletes() {
        pointsScale.saveAll(List.of(
                new ChampionshipPointsScaleEntry(championship.getId(), 2, 18),
                new ChampionshipPointsScaleEntry(championship.getId(), 1, 20)));
        pointsScale.save(new ChampionshipPointsScaleEntry(championship.getId(), 2, 17));

        assertThat(pointsScale.findByChampionshipIdOrderByPositionAsc(championship.getId()))
                .extracting(ChampionshipPointsScaleEntry::getPosition, ChampionshipPointsScaleEntry::getPoints)
                .containsExactly(tuple(1, 20), tuple(2, 17));
        assertThat(pointsScale.deleteAllByChampionshipId(championship.getId())).isEqualTo(2);
        assertThat(pointsScale.findByChampionshipIdOrderByPositionAsc(championship.getId())).isEmpty();
    }

    /** Saving an entity with a changed creation time leaves the stored one alone. */
    private static <E> void assertCreatedAtIsInsertOnly(JooqRepository<E, ?> repo, E saved, Function<E, Long> id,
                                                         Function<E, Instant> createdAt, Consumer<Instant> setCreatedAt) {
        Instant original = createdAt.apply(saved);
        setCreatedAt.accept(original.plus(1, ChronoUnit.DAYS));
        repo.save(saved);
        assertThat(createdAt.apply(repo.findById(id.apply(saved)).orElseThrow()))
                .as("creation time is set on insert only").isEqualTo(original);
    }

    private static Championship championship() {
        Championship c = new Championship();
        c.setName("Round trip series " + System.nanoTime());
        c.setScoringSource(ScoringSource.FINALS);
        c.setCreatedAt(T1);
        c.setUpdatedAt(T1);
        return c;
    }

    private ChampionshipExclusion exclusion(Long eventId, Long driverId, Long officialId, Instant at) {
        ChampionshipExclusion x = new ChampionshipExclusion();
        x.setChampionshipId(championship.getId());
        x.setDriverId(driverId);
        x.setEventId(eventId);
        x.setReason("Did not marshal");
        x.setCreatedBy(officialId);
        x.setCreatedAt(at);
        return x;
    }

    private RacingClass racingClass() {
        RacingClass rc = new RacingClass();
        rc.setName("Championship class " + System.nanoTime());
        rc.setCreatedAt(T1);
        rc.setUpdatedAt(T1);
        RacingClass saved = racingClasses.save(rc);
        cleanup.add(() -> racingClasses.deleteById(saved.getId()));
        return saved;
    }

    private Event event() {
        Event e = new Event();
        e.setName("Championship round");
        e.setEventDate(LocalDate.of(2026, 10, 5));
        e.setCreatedAt(T1);
        e.setUpdatedAt(T1);
        Event saved = events.save(e);
        cleanup.add(() -> events.deleteById(saved.getId()));
        return saved;
    }

    private Competitor competitor() {
        Competitor c = new Competitor();
        c.setDisplayName("Series driver");
        c.setCreatedAt(T1);
        c.setUpdatedAt(T1);
        Competitor saved = competitors.save(c);
        cleanup.add(() -> competitors.deleteById(saved.getId()));
        return saved;
    }

    private User official() {
        User u = new User();
        u.setEmail("championship-official-" + System.nanoTime() + "@example.com");
        u.setPasswordHash("x");
        u.setFirstName("Series");
        u.setLastName("Official");
        u.setRoles(Set.of(Role.ADMIN));
        u.setCreatedAt(T1);
        u.setUpdatedAt(T1);
        User saved = users.save(u);
        cleanup.add(() -> users.deleteById(saved.getId()));
        return saved;
    }
}
