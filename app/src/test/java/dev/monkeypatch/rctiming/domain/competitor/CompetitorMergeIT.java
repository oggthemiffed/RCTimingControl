package dev.monkeypatch.rctiming.domain.competitor;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.championship.Championship;
import dev.monkeypatch.rctiming.domain.championship.ChampionshipEventLink;
import dev.monkeypatch.rctiming.domain.championship.ChampionshipEventLinkRepository;
import dev.monkeypatch.rctiming.domain.championship.ChampionshipPointsScaleEntry;
import dev.monkeypatch.rctiming.domain.championship.ChampionshipPointsScaleRepository;
import dev.monkeypatch.rctiming.domain.championship.ChampionshipRepository;
import dev.monkeypatch.rctiming.domain.championship.ScoringSource;
import dev.monkeypatch.rctiming.domain.entry.Entry;
import dev.monkeypatch.rctiming.domain.entry.EntryAuditLog;
import dev.monkeypatch.rctiming.domain.entry.EntryAuditLogRepository;
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
import dev.monkeypatch.rctiming.query.championship.ChampionshipStandingsQuery;
import dev.monkeypatch.rctiming.query.championship.StandingsRowDto;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dev.monkeypatch.rctiming.jooq.generated.tables.ChampionshipExclusions.CHAMPIONSHIP_EXCLUSIONS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Merging a duplicate competitor into the one to keep (#123). */
class CompetitorMergeIT extends AbstractIntegrationTest {

    @Autowired CompetitorMergeService mergeService;
    @Autowired CompetitorRepository competitorRepository;
    @Autowired EntryRepository entryRepository;
    @Autowired EntryAuditLogRepository auditLogRepository;
    @Autowired EventRepository eventRepository;
    @Autowired RacingClassRepository racingClassRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ChampionshipRepository championshipRepository;
    @Autowired ChampionshipEventLinkRepository eventLinkRepository;
    @Autowired ChampionshipPointsScaleRepository pointsScaleRepository;
    @Autowired RoundRepository roundRepository;
    @Autowired RaceRepository raceRepository;
    @Autowired RaceEntryRepository raceEntryRepository;
    @Autowired ResultSnapshotRepository resultSnapshotRepository;
    @Autowired ChampionshipStandingsQuery standingsQuery;
    @Autowired DSLContext dsl;

    private String run;
    private Long racingClassId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        run = UUID.randomUUID().toString().substring(0, 8);
        RacingClass rc = new RacingClass();
        rc.setName("Merge class " + run);
        rc.setCreatedAt(Instant.now());
        rc.setUpdatedAt(Instant.now());
        racingClassId = racingClassRepository.save(rc).getId();

        User admin = new User();
        admin.setEmail("merge-" + run + "@test.com");
        admin.setPasswordHash(passwordEncoder.encode("pass12345"));
        admin.setFirstName("Merge");
        admin.setLastName("Admin");
        admin.setRoles(Set.of(Role.ADMIN));
        admin.setCreatedAt(Instant.now());
        admin.setUpdatedAt(Instant.now());
        adminId = userRepository.save(admin).getId();
    }

    @Test
    void movesEntriesAndExclusionsAuditsEachAndDeletesTheDuplicate() {
        Competitor keep = competitor("Alex Rowe " + run, null, null, "BRCA-" + run, null, null);
        Competitor duplicate = competitor("Alex  Rowe " + run, null, null, null, "Fenland RC", "Al-ex Roe");
        Event first = event("Merge meeting 1 " + run);
        Event second = event("Merge meeting 2 " + run);
        Long firstClass = eventClass(first.getId());
        Long secondClass = eventClass(second.getId());
        Entry keepEntry = entry(keep.getId(), first.getId(), firstClass, EntryStatus.CONFIRMED);
        Entry dupEntry = entry(duplicate.getId(), second.getId(), secondClass, EntryStatus.CONFIRMED);
        Championship champ = championship(ScoringSource.FINALS);
        Long duplicateId = duplicate.getId();
        long exclusionId = dsl.transactionResult(tx -> tx.dsl().insertInto(CHAMPIONSHIP_EXCLUSIONS)
                .set(CHAMPIONSHIP_EXCLUSIONS.CHAMPIONSHIP_ID, champ.getId())
                .set(CHAMPIONSHIP_EXCLUSIONS.DRIVER_ID, duplicateId)
                .set(CHAMPIONSHIP_EXCLUSIONS.EVENT_ID, second.getId())
                .set(CHAMPIONSHIP_EXCLUSIONS.REASON, "test")
                .set(CHAMPIONSHIP_EXCLUSIONS.CREATED_BY, adminId)
                .returning(CHAMPIONSHIP_EXCLUSIONS.ID).fetchOne().get(CHAMPIONSHIP_EXCLUSIONS.ID));

        CompetitorMergeService.Preview preview = mergeService.preview(keep.getId(), duplicate.getId());
        assertThat(preview.canMerge()).isTrue();
        assertThat(preview.entriesToMove()).isEqualTo(1);
        assertThat(preview.eventsAffected()).isEqualTo(1);
        assertThat(preview.exclusionsToMove()).isEqualTo(1);
        assertThat(preview.resultingSpokenName()).isEqualTo("Al-ex Roe");
        // The preview changes nothing
        assertThat(competitorRepository.findById(duplicate.getId())).isPresent();

        CompetitorMergeService.Result result = mergeService.merge(keep.getId(), duplicate.getId(), adminId);

        assertThat(result.entriesMoved()).isEqualTo(1);
        assertThat(result.exclusionsMoved()).isEqualTo(1);
        assertThat(competitorRepository.findById(duplicate.getId())).isEmpty();
        assertThat(entryRepository.findById(dupEntry.getId()).orElseThrow().getCompetitorId()).isEqualTo(keep.getId());
        assertThat(entryRepository.findById(keepEntry.getId()).orElseThrow().getCompetitorId()).isEqualTo(keep.getId());
        assertThat(dsl.select(CHAMPIONSHIP_EXCLUSIONS.DRIVER_ID).from(CHAMPIONSHIP_EXCLUSIONS)
                .where(CHAMPIONSHIP_EXCLUSIONS.ID.eq(exclusionId)).fetchOne().value1()).isEqualTo(keep.getId());

        Competitor kept = competitorRepository.findById(keep.getId()).orElseThrow();
        assertThat(kept.getDisplayName()).isEqualTo("Alex Rowe " + run);
        assertThat(kept.getBrcaNumber()).isEqualTo("BRCA-" + run);
        assertThat(kept.getHomeClub()).isEqualTo("Fenland RC");
        assertThat(kept.getSpokenName()).isEqualTo("Al-ex Roe");

        List<EntryAuditLog> audit = auditLogRepository.findByEventIdAndAction(second.getId(),
                CompetitorMergeService.AUDIT_ACTION);
        assertThat(audit).hasSize(1);
        assertThat(audit.get(0).getEntryId()).isEqualTo(dupEntry.getId());
        assertThat(audit.get(0).getAdminUserId()).isEqualTo(adminId);
        assertThat(audit.get(0).getReason()).contains("Alex  Rowe " + run).contains("Alex Rowe " + run);
    }

    @Test
    void theKeptCompetitorsOwnSpokenNameWins() {
        Competitor keep = competitor("Sam Ito " + run, null, null, null, null, "Sam Ee-toe");
        Competitor duplicate = competitor("Sam Ito " + run, null, null, null, null, "Sam Eye-toe");

        mergeService.merge(keep.getId(), duplicate.getId(), adminId);

        assertThat(competitorRepository.findById(keep.getId()).orElseThrow().getSpokenName()).isEqualTo("Sam Ee-toe");
    }

    @Test
    void refusesWhenBothHaveAnActiveEntryInTheSameClass_andChangesNothing() {
        Competitor keep = competitor("Pat Lee " + run, null, null, null, null, null);
        Competitor duplicate = competitor("Pat Lee " + run, null, null, null, null, null);
        Event event = event("Conflict meeting " + run);
        Long eventClass = eventClass(event.getId());
        entry(keep.getId(), event.getId(), eventClass, EntryStatus.CONFIRMED);
        Entry dupEntry = entry(duplicate.getId(), event.getId(), eventClass, EntryStatus.CONFIRMED);

        CompetitorMergeService.Preview preview = mergeService.preview(keep.getId(), duplicate.getId());
        assertThat(preview.canMerge()).isFalse();
        assertThat(preview.blockers()).singleElement().asString().contains("Both have an active entry");

        assertThatThrownBy(() -> mergeService.merge(keep.getId(), duplicate.getId(), adminId))
                .isInstanceOf(CompetitorMergeRefusedException.class);
        assertThat(competitorRepository.findById(duplicate.getId())).isPresent();
        assertThat(entryRepository.findById(dupEntry.getId()).orElseThrow().getCompetitorId()).isEqualTo(duplicate.getId());
    }

    @Test
    void aWithdrawnEntryInTheSameClassDoesNotBlock() {
        Competitor keep = competitor("Jo Kim " + run, null, null, null, null, null);
        Competitor duplicate = competitor("Jo Kim " + run, null, null, null, null, null);
        Event event = event("Withdrawn meeting " + run);
        Long eventClass = eventClass(event.getId());
        entry(keep.getId(), event.getId(), eventClass, EntryStatus.CONFIRMED);
        Entry withdrawn = entry(duplicate.getId(), event.getId(), eventClass, EntryStatus.WITHDRAWN);

        mergeService.merge(keep.getId(), duplicate.getId(), adminId);

        assertThat(entryRepository.findById(withdrawn.getId()).orElseThrow().getCompetitorId()).isEqualTo(keep.getId());
    }

    @Test
    void theRaceHubIdMovesToTheKeptCompetitorSoTheNextImportStillFindsThem() {
        String driverId = "rh-" + run;
        Competitor keep = competitor("Walk In " + run, null, null, null, null, null);
        Competitor duplicate = competitor("Walk In " + run, "RACEHUB", driverId, null, null, null);

        CompetitorMergeService.Preview preview = mergeService.preview(keep.getId(), duplicate.getId());
        assertThat(preview.resultingExternalSource()).isEqualTo("RACEHUB");
        assertThat(preview.warnings()).anyMatch(w -> w.contains("RaceHub link moves"));

        mergeService.merge(keep.getId(), duplicate.getId(), adminId);

        assertThat(competitorRepository.findByExternalSourceAndExternalId("RACEHUB", driverId))
                .get().extracting(Competitor::getId).isEqualTo(keep.getId());
    }

    @Test
    void aRaceHubCompetitorKeepsItsIdWhenAnImportedOneIsMergedIn() {
        String driverId = "rh-keep-" + run;
        Competitor keep = competitor("Ada " + run, "RACEHUB", driverId, null, null, null);
        Competitor duplicate = competitor("Ada " + run, "RCTIMING_CSV", "csv-" + run, null, null, null);

        mergeService.merge(keep.getId(), duplicate.getId(), adminId);

        assertThat(competitorRepository.findByExternalSourceAndExternalId("RACEHUB", driverId))
                .get().extracting(Competitor::getId).isEqualTo(keep.getId());
        assertThat(competitorRepository.findByExternalSourceAndExternalId("RCTIMING_CSV", "csv-" + run)).isEmpty();
    }

    @Test
    void aRaceHubIdFromTheDuplicateBeatsAnImportedIdOnTheKeptCompetitor() {
        // The kept competitor has a CSV id and the duplicate a RaceHub id: RaceHub's id wins
        String driverId = "rh-win-" + run;
        Competitor keep = competitor("Grace " + run, "RCTIMING_CSV", "csv-win-" + run, null, null, null);
        Competitor duplicate = competitor("Grace " + run, "RACEHUB", driverId, null, null, null);

        mergeService.merge(keep.getId(), duplicate.getId(), adminId);

        Competitor kept = competitorRepository.findById(keep.getId()).orElseThrow();
        assertThat(kept.getExternalSource()).isEqualTo("RACEHUB");
        assertThat(kept.getExternalId()).isEqualTo(driverId);
    }

    @Test
    void refusesToMergeTwoDifferentRaceHubDrivers() {
        Competitor keep = competitor("Twin " + run, "RACEHUB", "rh-a-" + run, null, null, null);
        Competitor duplicate = competitor("Twin " + run, "RACEHUB", "rh-b-" + run, null, null, null);

        assertThat(mergeService.preview(keep.getId(), duplicate.getId()).canMerge()).isFalse();
        assertThatThrownBy(() -> mergeService.merge(keep.getId(), duplicate.getId(), adminId))
                .isInstanceOf(CompetitorMergeRefusedException.class);
        assertThat(competitorRepository.findById(duplicate.getId())).isPresent();
    }

    @Test
    void refusesTheSameCompetitorTwiceAndUnknownOnes() {
        Competitor c = competitor("Solo " + run, null, null, null, null, null);

        assertThatThrownBy(() -> mergeService.merge(c.getId(), c.getId(), adminId))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mergeService.merge(c.getId(), 999_999_999L, adminId))
                .isInstanceOf(dev.monkeypatch.rctiming.domain.EntityNotFoundException.class);
    }

    @Test
    void aChampionshipAcrossTheMergedMeetingsGroupsUnderTheOneKept() {
        Competitor keep = competitor("Champ Driver " + run, null, null, null, null, null);
        Competitor duplicate = competitor("Champ  Driver " + run, null, null, null, null, null);
        Championship champ = championship(ScoringSource.FINALS);
        pointsScaleRepository.save(new ChampionshipPointsScaleEntry(champ.getId(), 1, 10));
        pointsScaleRepository.save(new ChampionshipPointsScaleEntry(champ.getId(), 2, 8));
        finishedFinal(champ, 1, keep.getId(), 1);
        finishedFinal(champ, 2, duplicate.getId(), 2);

        List<StandingsRowDto> before = standingsQuery.computeStandings(champ.getId());
        assertThat(before).hasSize(2);

        mergeService.merge(keep.getId(), duplicate.getId(), adminId);

        List<StandingsRowDto> after = standingsQuery.computeStandings(champ.getId());
        assertThat(after).hasSize(1);
        assertThat(after.get(0).driverId()).isEqualTo(keep.getId());
        assertThat(after.get(0).totalPoints()).isEqualTo(18);
        assertThat(after.get(0).rounds()).hasSize(2);
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────────

    private Competitor competitor(String name, String source, String externalId, String brca, String club,
                                  String spoken) {
        Competitor c = new Competitor();
        c.setDisplayName(name);
        c.setExternalSource(source);
        c.setExternalId(externalId);
        c.setBrcaNumber(brca);
        c.setHomeClub(club);
        c.setSpokenName(spoken);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return competitorRepository.save(c);
    }

    private Event event(String name) {
        Event e = new Event();
        e.setName(name);
        e.setEventDate(LocalDate.of(2026, 1, 1));
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return eventRepository.save(e);
    }

    private Long eventClass(Long eventId) {
        // Writes go through a transaction, onto the write connection
        return dsl.transactionResult(tx -> tx.dsl().insertInto(EVENT_CLASSES)
                .set(EVENT_CLASSES.EVENT_ID, eventId)
                .set(EVENT_CLASSES.RACING_CLASS_ID, racingClassId)
                .set(EVENT_CLASSES.CONFIG_SNAPSHOT, "{\"type\":\"TIMED\"}")
                .returning(EVENT_CLASSES.ID).fetchOne().get(EVENT_CLASSES.ID));
    }

    private Entry entry(Long competitorId, Long eventId, Long eventClassId, EntryStatus status) {
        Entry e = new Entry();
        e.setCompetitorId(competitorId);
        e.setEventId(eventId);
        e.setEventClassId(eventClassId);
        e.setTransponderNumberSnapshot("T-" + UUID.randomUUID().toString().substring(0, 8));
        e.setStatus(status);
        e.setSubmittedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return entryRepository.save(e);
    }

    private Championship championship(ScoringSource source) {
        Championship c = new Championship();
        c.setName("Merge champ " + UUID.randomUUID());
        c.setTqBonusPoints(0);
        c.setAfinalWinnerBonusPoints(0);
        c.setScoringSource(source);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return championshipRepository.save(c);
    }

    /** A meeting in the championship where the competitor finished the A final in the given position. */
    private void finishedFinal(Championship champ, int roundNumber, Long competitorId, int position) {
        Event event = event("Champ meeting " + roundNumber + " " + run);
        ChampionshipEventLink link = new ChampionshipEventLink();
        link.setChampionshipId(champ.getId());
        link.setEventId(event.getId());
        link.setRoundNumber(roundNumber);
        link.setCreatedAt(Instant.now());
        eventLinkRepository.save(link);
        Long ecId = eventClass(event.getId());
        Round round = new Round();
        round.setEventId(event.getId());
        round.setType(RoundType.FINAL);
        round.setRoundNumber(1);
        round.setSequenceInEvent(1);
        round.setCreatedAt(Instant.now());
        round.setUpdatedAt(Instant.now());
        round = roundRepository.save(round);
        Race race = new Race();
        race.setRoundId(round.getId());
        race.setEventClassId(ecId);
        race.setHeatNumber(1);
        race.setSequenceInRound(1);
        race.setFinalLetter("A");
        race.setStartType(StartType.GRID);
        race.setStatus(RaceStatus.FINISHED);
        race.setCreatedAt(Instant.now());
        race.setUpdatedAt(Instant.now());
        race.setFinishedAt(Instant.now());
        race = raceRepository.save(race);
        Entry entry = entry(competitorId, event.getId(), ecId, EntryStatus.CONFIRMED);
        RaceEntry re = new RaceEntry();
        re.setRaceId(race.getId());
        re.setEntryId(entry.getId());
        raceEntryRepository.save(re);
        ResultSnapshot snap = new ResultSnapshot();
        snap.setRaceId(race.getId());
        snap.setPositionsJson(String.format(
                "[{\"position\":%d,\"entryId\":%d,\"driverName\":\"x\",\"carNumber\":\"1\","
                        + "\"lapsCompleted\":10,\"totalTimeMs\":60000,\"bestLapMs\":6000,\"gapToLeaderMs\":0}]",
                position, entry.getId()));
        snap.setLapHistoryJson("[]");
        snap.setFinishedAt(Instant.now());
        snap.setCreatedAt(Instant.now());
        resultSnapshotRepository.save(snap);
    }
}
