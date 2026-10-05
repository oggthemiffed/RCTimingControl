package dev.monkeypatch.rctiming.query.resultsexport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.racecontrol.dto.ResultSnapshotDto;
import dev.monkeypatch.rctiming.query.championship.ChampionshipStandingsQuery;
import dev.monkeypatch.rctiming.query.championship.RoundResultDto;
import dev.monkeypatch.rctiming.query.championship.StandingsRowDto;
import dev.monkeypatch.rctiming.resultsexport.ResultsExportV1;
import jakarta.persistence.EntityNotFoundException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import static dev.monkeypatch.rctiming.jooq.generated.tables.ChampionshipEventLinks.CHAMPIONSHIP_EVENT_LINKS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Championships.CHAMPIONSHIPS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ClubProfiles.CLUB_PROFILES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Competitors.COMPETITORS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Events.EVENTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Penalties.PENALTIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Races.RACES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RacingClasses.RACING_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ResultSnapshots.RESULT_SNAPSHOTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Rounds.ROUNDS;

/**
 * Builds an event's Results Export v1 document (#27) from its result snapshots, penalties and championship
 * standings. Read-only; the caller decides the revision.
 */
@Component
@Transactional(readOnly = true)
public class ResultsExportQuery {

    static final String SYSTEM = "RCTimingControl";

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;
    private final ChampionshipStandingsQuery standingsQuery;

    public ResultsExportQuery(DSLContext dsl, ObjectMapper objectMapper, ChampionshipStandingsQuery standingsQuery) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
        this.standingsQuery = standingsQuery;
    }

    public ResultsExportV1 build(long eventId, long revision, Instant generatedAt) {
        var event = dsl.select(EVENTS.NAME, EVENTS.EVENT_DATE, EVENTS.STATUS, EVENTS.RACEHUB_EVENT_ID)
                .from(EVENTS)
                .where(EVENTS.ID.eq(eventId))
                .fetchOne();
        if (event == null) {
            throw new EntityNotFoundException("Event not found: " + eventId);
        }
        String clubName = dsl.select(CLUB_PROFILES.NAME).from(CLUB_PROFILES).limit(1).fetchOne(CLUB_PROFILES.NAME);

        Map<Long, EntryRef> entries = entries(eventId);
        return new ResultsExportV1(
                ResultsExportV1.SCHEMA_VERSION,
                revision,
                generatedAt.toString(),
                new ResultsExportV1.Source(SYSTEM, clubName),
                new ResultsExportV1.EventRef(event.get(EVENTS.RACEHUB_EVENT_ID), eventId, event.get(EVENTS.NAME),
                        String.valueOf(event.get(EVENTS.EVENT_DATE)), event.get(EVENTS.STATUS)),
                races(eventId, entries),
                championships(eventId, entries));
    }

    // ── Races ──────────────────────────────────────────────────────────────────────

    private List<ResultsExportV1.Race> races(long eventId, Map<Long, EntryRef> entries) {
        var rows = dsl.select(RACES.ID, RACES.EVENT_CLASS_ID, RACES.HEAT_NUMBER, RACES.FINAL_LETTER, RACES.FINISHED_AT,
                        RACES.ABANDONED_AT, ROUNDS.TYPE, ROUNDS.ROUND_NUMBER, RACING_CLASSES.NAME,
                        RESULT_SNAPSHOTS.POSITIONS_JSON, RESULT_SNAPSHOTS.FINISHED_AT)
                .from(RACES)
                .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
                .join(EVENT_CLASSES).on(EVENT_CLASSES.ID.eq(RACES.EVENT_CLASS_ID))
                .join(RACING_CLASSES).on(RACING_CLASSES.ID.eq(EVENT_CLASSES.RACING_CLASS_ID))
                .leftJoin(RESULT_SNAPSHOTS).on(RESULT_SNAPSHOTS.RACE_ID.eq(RACES.ID))
                .where(ROUNDS.EVENT_ID.eq(eventId))
                .and(RACES.STATUS.eq("FINISHED"))
                .orderBy(ROUNDS.SEQUENCE_IN_EVENT, RACES.SEQUENCE_IN_ROUND, RACES.ID)
                .fetch();

        Map<Long, Map<Long, List<ResultsExportV1.Penalty>>> penalties = penalties(eventId);
        List<ResultsExportV1.Race> races = new ArrayList<>();
        for (Record r : rows) {
            long raceId = r.get(RACES.ID);
            Map<Long, List<ResultsExportV1.Penalty>> racePenalties = penalties.getOrDefault(raceId, Map.of());
            List<ResultsExportV1.Row> results = new ArrayList<>();
            TreeSet<String> racehubClassIds = new TreeSet<>();
            for (ResultSnapshotDto.ResultRow p : positions(raceId, r.get(RESULT_SNAPSHOTS.POSITIONS_JSON))) {
                EntryRef entry = entries.get(p.entryId());
                if (entry != null && entry.racehubEventClassId() != null) {
                    racehubClassIds.add(entry.racehubEventClassId());
                }
                results.add(new ResultsExportV1.Row(
                        p.position(),
                        entry == null ? null : entry.externalSource(),
                        entry == null ? null : entry.externalEntryId(),
                        entry == null ? null : entry.driverProfileId(),
                        entry == null ? null : entry.racehubEventClassId(),
                        p.entryId(),
                        p.competitorId() != null ? p.competitorId() : entry == null ? null : entry.competitorId(),
                        p.driverName() != null ? p.driverName() : entry == null ? null : entry.displayName(),
                        p.carNumber(),
                        p.lapsCompleted(),
                        p.totalTimeMs(),
                        p.bestLapMs(),
                        racePenalties.getOrDefault(p.entryId(), List.of())));
            }
            Instant finishedAt = r.get(RACES.FINISHED_AT) != null ? r.get(RACES.FINISHED_AT)
                    : r.get(RESULT_SNAPSHOTS.FINISHED_AT);
            races.add(new ResultsExportV1.Race(
                    raceId,
                    r.get(RACES.EVENT_CLASS_ID),
                    List.copyOf(racehubClassIds),
                    r.get(RACING_CLASSES.NAME),
                    r.get(ROUNDS.TYPE),
                    r.get(ROUNDS.ROUND_NUMBER),
                    r.get(RACES.HEAT_NUMBER),
                    r.get(RACES.FINAL_LETTER),
                    r.get(RACES.ABANDONED_AT) != null ? "ABANDONED" : "FINISHED",
                    finishedAt == null ? null : finishedAt.toString(),
                    results));
        }
        return races;
    }

    private List<ResultSnapshotDto.ResultRow> positions(long raceId, String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ResultSnapshotDto.ResultRow>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Unreadable result snapshot for race " + raceId, e);
        }
    }

    /** Penalties per race, then per entry, in the order they were given. */
    private Map<Long, Map<Long, List<ResultsExportV1.Penalty>>> penalties(long eventId) {
        Map<Long, Map<Long, List<ResultsExportV1.Penalty>>> byRace = new LinkedHashMap<>();
        dsl.select(PENALTIES.RACE_ID, PENALTIES.ENTRY_ID, PENALTIES.PENALTY_TYPE, PENALTIES.VALUE, PENALTIES.REASON)
                .from(PENALTIES)
                .join(RACES).on(RACES.ID.eq(PENALTIES.RACE_ID))
                .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
                .where(ROUNDS.EVENT_ID.eq(eventId))
                .orderBy(PENALTIES.APPLIED_AT, PENALTIES.ID)
                .forEach(p -> byRace
                        .computeIfAbsent(p.get(PENALTIES.RACE_ID), k -> new LinkedHashMap<>())
                        .computeIfAbsent(p.get(PENALTIES.ENTRY_ID), k -> new ArrayList<>())
                        .add(new ResultsExportV1.Penalty(p.get(PENALTIES.PENALTY_TYPE),
                                plain(p.get(PENALTIES.VALUE)), p.get(PENALTIES.REASON))));
        return byRace;
    }

    /** 2.0 → 2 and 1.50 → 1.5, never 1E+1. */
    private static BigDecimal plain(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    // ── Championships ──────────────────────────────────────────────────────────────

    private List<ResultsExportV1.Championship> championships(long eventId, Map<Long, EntryRef> entries) {
        Map<Long, CompetitorRef> competitors = new LinkedHashMap<>();
        entries.values().forEach(e -> competitors.putIfAbsent(e.competitorId(),
                new CompetitorRef(e.competitorSource(), e.driverProfileId())));

        var links = dsl.select(CHAMPIONSHIPS.ID, CHAMPIONSHIPS.NAME, CHAMPIONSHIP_EVENT_LINKS.ROUND_NUMBER)
                .from(CHAMPIONSHIP_EVENT_LINKS)
                .join(CHAMPIONSHIPS).on(CHAMPIONSHIPS.ID.eq(CHAMPIONSHIP_EVENT_LINKS.CHAMPIONSHIP_ID))
                .where(CHAMPIONSHIP_EVENT_LINKS.EVENT_ID.eq(eventId))
                .orderBy(CHAMPIONSHIPS.ID)
                .fetch();

        List<ResultsExportV1.Championship> championships = new ArrayList<>();
        for (var link : links) {
            long championshipId = link.get(CHAMPIONSHIPS.ID);
            Map<Long, List<StandingsRowDto>> byClass = new LinkedHashMap<>();
            for (StandingsRowDto row : standingsQuery.computeStandings(championshipId)) {
                byClass.computeIfAbsent(row.racingClassId(), k -> new ArrayList<>()).add(row);
            }
            Map<Long, String> classNames = new LinkedHashMap<>();
            if (!byClass.isEmpty()) {
                dsl.select(RACING_CLASSES.ID, RACING_CLASSES.NAME).from(RACING_CLASSES)
                        .where(RACING_CLASSES.ID.in(byClass.keySet()))
                        .forEach(c -> classNames.put(c.get(RACING_CLASSES.ID), c.get(RACING_CLASSES.NAME)));
            }

            List<ResultsExportV1.ChampionshipClass> classes = new ArrayList<>();
            byClass.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(c -> classes.add(new ResultsExportV1.ChampionshipClass(
                            c.getKey(), classNames.get(c.getKey()), standings(c.getValue(), eventId, competitors))));
            championships.add(new ResultsExportV1.Championship(championshipId, link.get(CHAMPIONSHIPS.NAME),
                    link.get(CHAMPIONSHIP_EVENT_LINKS.ROUND_NUMBER), classes));
        }
        return championships;
    }

    private List<ResultsExportV1.Standing> standings(List<StandingsRowDto> rows, long eventId,
                                                     Map<Long, CompetitorRef> competitors) {
        List<StandingsRowDto> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparingInt(StandingsRowDto::totalPoints).reversed()
                .thenComparing(StandingsRowDto::displayName, Comparator.nullsLast(Comparator.naturalOrder())));
        List<ResultsExportV1.Standing> standings = new ArrayList<>();
        int position = 0;
        Integer lastPoints = null;
        for (int i = 0; i < sorted.size(); i++) {
            StandingsRowDto row = sorted.get(i);
            if (!Objects.equals(lastPoints, row.totalPoints())) {
                position = i + 1;
                lastPoints = row.totalPoints();
            }
            RoundResultDto here = row.rounds().stream()
                    .filter(rr -> Objects.equals(rr.eventId(), eventId))
                    .findFirst().orElse(null);
            CompetitorRef competitor = competitors.computeIfAbsent(row.driverId(), this::competitor);
            standings.add(new ResultsExportV1.Standing(
                    position,
                    competitor.externalSource(),
                    competitor.externalId(),
                    row.driverId(),
                    row.displayName(),
                    row.totalPoints(),
                    here == null || here.position() == 0 ? null : here.position(),
                    here == null ? null : here.points(),
                    here != null && here.dropped(),
                    here != null && here.excluded()));
        }
        return standings;
    }

    private CompetitorRef competitor(long competitorId) {
        var c = dsl.select(COMPETITORS.EXTERNAL_SOURCE, COMPETITORS.EXTERNAL_ID)
                .from(COMPETITORS)
                .where(COMPETITORS.ID.eq(competitorId))
                .fetchOne();
        return c == null ? new CompetitorRef(null, null)
                : new CompetitorRef(c.get(COMPETITORS.EXTERNAL_SOURCE), c.get(COMPETITORS.EXTERNAL_ID));
    }

    // ── Entries ────────────────────────────────────────────────────────────────────

    private Map<Long, EntryRef> entries(long eventId) {
        Map<Long, EntryRef> entries = new LinkedHashMap<>();
        dsl.select(ENTRIES.ID, ENTRIES.EXTERNAL_SOURCE, ENTRIES.EXTERNAL_ENTRY_ID, ENTRIES.RACEHUB_EVENT_CLASS_ID,
                        COMPETITORS.ID, COMPETITORS.DISPLAY_NAME, COMPETITORS.EXTERNAL_SOURCE, COMPETITORS.EXTERNAL_ID)
                .from(ENTRIES)
                .join(COMPETITORS).on(COMPETITORS.ID.eq(ENTRIES.COMPETITOR_ID))
                .where(ENTRIES.EVENT_ID.eq(eventId))
                .forEach(e -> entries.put(e.get(ENTRIES.ID), new EntryRef(
                        e.get(ENTRIES.EXTERNAL_SOURCE),
                        e.get(ENTRIES.EXTERNAL_ENTRY_ID),
                        e.get(ENTRIES.RACEHUB_EVENT_CLASS_ID),
                        e.get(COMPETITORS.ID),
                        e.get(COMPETITORS.DISPLAY_NAME),
                        e.get(COMPETITORS.EXTERNAL_SOURCE),
                        // A driver profile id only means something alongside an imported entry
                        e.get(ENTRIES.EXTERNAL_SOURCE) == null ? null : e.get(COMPETITORS.EXTERNAL_ID))));
        return entries;
    }

    private record EntryRef(String externalSource, String externalEntryId, String racehubEventClassId,
                            long competitorId, String displayName, String competitorSource, String driverProfileId) {
    }

    private record CompetitorRef(String externalSource, String externalId) {
    }
}
