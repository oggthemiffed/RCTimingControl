package dev.monkeypatch.rctiming.query.championship;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.racecontrol.dto.ResultSnapshotDto;
import jakarta.persistence.EntityNotFoundException;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.monkeypatch.rctiming.jooq.generated.tables.ChampionshipClasses.CHAMPIONSHIP_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ChampionshipEventLinks.CHAMPIONSHIP_EVENT_LINKS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ChampionshipExclusions.CHAMPIONSHIP_EXCLUSIONS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ChampionshipPointsScale.CHAMPIONSHIP_POINTS_SCALE;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Championships.CHAMPIONSHIPS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Events.EVENTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RaceEntries.RACE_ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Races.RACES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ResultSnapshots.RESULT_SNAPSHOTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Rounds.ROUNDS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Users.USERS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RacingClasses.RACING_CLASSES;

/**
 * CHAMP-01, CHAMP-02, CHAMP-04, CHAMP-07, CHAMP-08, CHAMP-09 read side.
 *
 * Implements full championship standings computation: best-X-from-Y drop logic,
 * TQ bonus, A-final winner bonus, DNS semantics.
 * Pure jOOQ — no JPA repository dependency.
 */
@Service
@Transactional(readOnly = true)
public class ChampionshipStandingsQuery {

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public ChampionshipStandingsQuery(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }

    public List<StandingsRowDto> computeStandings(Long championshipId) {
        // Step 1: Existence check — jOOQ only, no Hibernate
        if (!dsl.fetchExists(CHAMPIONSHIPS, CHAMPIONSHIPS.ID.eq(championshipId))) {
            throw new EntityNotFoundException("Championship not found: " + championshipId);
        }

        // Step 1b: Load championship header
        var champRecord = dsl
                .select(CHAMPIONSHIPS.NAME, CHAMPIONSHIPS.BEST_X_FROM_Y_X, CHAMPIONSHIPS.BEST_X_FROM_Y_Y,
                        CHAMPIONSHIPS.SCORING_SOURCE, CHAMPIONSHIPS.TQ_BONUS_POINTS,
                        CHAMPIONSHIPS.AFINAL_WINNER_BONUS_POINTS)
                .from(CHAMPIONSHIPS)
                .where(CHAMPIONSHIPS.ID.eq(championshipId))
                .fetchOne();

        if (champRecord == null) {
            throw new EntityNotFoundException("Championship not found: " + championshipId);
        }

        Integer champBestX = champRecord.get(CHAMPIONSHIPS.BEST_X_FROM_Y_X);
        Integer champBestY = champRecord.get(CHAMPIONSHIPS.BEST_X_FROM_Y_Y);
        String scoringSource = champRecord.get(CHAMPIONSHIPS.SCORING_SOURCE);
        int tqBonus = champRecord.get(CHAMPIONSHIPS.TQ_BONUS_POINTS);
        int afinalBonus = champRecord.get(CHAMPIONSHIPS.AFINAL_WINNER_BONUS_POINTS);

        // Step 2: Load per-class best-X-from-Y overrides
        // Map: racingClassId -> [bestX, bestY] (null = use championship default)
        var classRows = dsl
                .select(CHAMPIONSHIP_CLASSES.RACING_CLASS_ID,
                        CHAMPIONSHIP_CLASSES.BEST_X_FROM_Y_X,
                        CHAMPIONSHIP_CLASSES.BEST_X_FROM_Y_Y)
                .from(CHAMPIONSHIP_CLASSES)
                .where(CHAMPIONSHIP_CLASSES.CHAMPIONSHIP_ID.eq(championshipId))
                .fetch();

        Map<Long, Integer[]> classOverrides = new HashMap<>();
        for (var row : classRows) {
            Long rcId = row.get(CHAMPIONSHIP_CLASSES.RACING_CLASS_ID);
            classOverrides.put(rcId, new Integer[]{
                    row.get(CHAMPIONSHIP_CLASSES.BEST_X_FROM_Y_X),
                    row.get(CHAMPIONSHIP_CLASSES.BEST_X_FROM_Y_Y)
            });
        }

        // Step 3: Load ordered event links
        var eventLinks = dsl
                .select(CHAMPIONSHIP_EVENT_LINKS.ID,
                        CHAMPIONSHIP_EVENT_LINKS.EVENT_ID,
                        CHAMPIONSHIP_EVENT_LINKS.ROUND_NUMBER,
                        EVENTS.NAME)
                .from(CHAMPIONSHIP_EVENT_LINKS)
                .join(EVENTS).on(EVENTS.ID.eq(CHAMPIONSHIP_EVENT_LINKS.EVENT_ID))
                .where(CHAMPIONSHIP_EVENT_LINKS.CHAMPIONSHIP_ID.eq(championshipId))
                .orderBy(CHAMPIONSHIP_EVENT_LINKS.ROUND_NUMBER.asc())
                .fetch();

        // Step 4: Load points scale
        Map<Integer, Integer> pointsScale = new HashMap<>();
        dsl.select(CHAMPIONSHIP_POINTS_SCALE.POSITION, CHAMPIONSHIP_POINTS_SCALE.POINTS)
                .from(CHAMPIONSHIP_POINTS_SCALE)
                .where(CHAMPIONSHIP_POINTS_SCALE.CHAMPIONSHIP_ID.eq(championshipId))
                .forEach(r -> pointsScale.put(r.get(CHAMPIONSHIP_POINTS_SCALE.POSITION),
                        r.get(CHAMPIONSHIP_POINTS_SCALE.POINTS)));

        // Step 5: Load exclusions
        Set<String> exclusionKeys = new HashSet<>();
        dsl.select(CHAMPIONSHIP_EXCLUSIONS.DRIVER_ID, CHAMPIONSHIP_EXCLUSIONS.EVENT_ID)
                .from(CHAMPIONSHIP_EXCLUSIONS)
                .where(CHAMPIONSHIP_EXCLUSIONS.CHAMPIONSHIP_ID.eq(championshipId))
                .forEach(r -> exclusionKeys.add(
                        r.get(CHAMPIONSHIP_EXCLUSIONS.DRIVER_ID) + ":" + r.get(CHAMPIONSHIP_EXCLUSIONS.EVENT_ID)));

        // Per-driver data structures for steps 6-9:
        // driverKey = "driverId:racingClassId"
        // Store: Map<driverKey, List<RoundResultDto>> for building standings
        Map<String, List<RoundResultDto>> driverRounds = new LinkedHashMap<>();
        // For StandingsRowDto metadata
        Map<Long, String> driverFirstName = new HashMap<>();
        Map<Long, String> driverLastName = new HashMap<>();
        // Map driverKey to racingClassId
        Map<String, Long> driverKeyToClassId = new HashMap<>();
        // Map driverKey to driverId
        Map<String, Long> driverKeyToUserId = new HashMap<>();

        // TQ and A-final bonus tracking
        Set<Long> tqDrivers = new HashSet<>();   // drivers who had position=1 in a QUALIFIER
        Set<Long> afinalWinners = new HashSet<>(); // drivers who had position=1 in an A-FINAL

        // Step 6: Per event link — find finished races and build RoundResultDtos
        for (var link : eventLinks) {
            Long eventId = link.get(CHAMPIONSHIP_EVENT_LINKS.EVENT_ID);
            int roundNumber = link.get(CHAMPIONSHIP_EVENT_LINKS.ROUND_NUMBER);
            String eventName = link.get(EVENTS.NAME);

            // Build round type filter based on scoringSource
            var racesQuery = dsl
                    .select(RACES.ID, RACES.FINAL_LETTER, ROUNDS.TYPE, ROUNDS.ROUND_NUMBER,
                            EVENT_CLASSES.RACING_CLASS_ID,
                            RESULT_SNAPSHOTS.POSITIONS_JSON)
                    .from(RACES)
                    .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
                    .join(EVENT_CLASSES).on(EVENT_CLASSES.ID.eq(RACES.EVENT_CLASS_ID))
                    .leftJoin(RESULT_SNAPSHOTS).on(RESULT_SNAPSHOTS.RACE_ID.eq(RACES.ID))
                    .where(ROUNDS.EVENT_ID.eq(eventId))
                    .and(RACES.STATUS.eq("FINISHED"));

            if ("QUALIFYING".equals(scoringSource)) {
                racesQuery = racesQuery.and(ROUNDS.TYPE.eq("QUALIFIER"));
            } else if ("FINALS".equals(scoringSource)) {
                racesQuery = racesQuery.and(ROUNDS.TYPE.eq("FINAL"));
            }
            // BOTH = no additional filter on type

            var finishedRaces = racesQuery.fetch();

            // Collect raceIds for DNS detection
            List<Long> finishedRaceIds = finishedRaces.map(r -> r.get(RACES.ID));

            // Per-driver best position at this event, per racing class
            // Map: racingClassId -> Map<driverId, Integer bestPosition>
            Map<Long, Map<Long, Integer>> classBestPosition = new HashMap<>();
            // Per-race: track who scored what for TQ/A-final bonus
            // positionsJson entryId → userId mapping needed

            // Step 6c/6d: For all race_entries for these races, collect entered driver userId
            Set<Long> enteredEntryIds = new HashSet<>();
            // Map entryId -> userId and entryId -> racingClassId (via entry -> event_class)
            Map<Long, Long> entryIdToUserId = new HashMap<>();
            Map<Long, Long> entryIdToRacingClassId = new HashMap<>();

            if (!finishedRaceIds.isEmpty()) {
                var raceEntryRows = dsl
                        .select(RACE_ENTRIES.ENTRY_ID, ENTRIES.USER_ID, ENTRIES.EVENT_CLASS_ID)
                        .from(RACE_ENTRIES)
                        .join(ENTRIES).on(ENTRIES.ID.eq(RACE_ENTRIES.ENTRY_ID))
                        .where(RACE_ENTRIES.RACE_ID.in(finishedRaceIds))
                        .fetch();

                for (var re : raceEntryRows) {
                    Long entryId = re.get(RACE_ENTRIES.ENTRY_ID);
                    Long userId = re.get(ENTRIES.USER_ID);
                    Long ecId = re.get(ENTRIES.EVENT_CLASS_ID);
                    enteredEntryIds.add(entryId);
                    entryIdToUserId.put(entryId, userId);
                    entryIdToRacingClassId.put(entryId, ecId);
                }

                // Resolve event_class_id -> racing_class_id
                Map<Long, Long> ecToRacingClass = new HashMap<>();
                Set<Long> ecIds = new HashSet<>(entryIdToRacingClassId.values());
                if (!ecIds.isEmpty()) {
                    dsl.select(EVENT_CLASSES.ID, EVENT_CLASSES.RACING_CLASS_ID)
                            .from(EVENT_CLASSES)
                            .where(EVENT_CLASSES.ID.in(ecIds))
                            .forEach(r -> ecToRacingClass.put(r.get(EVENT_CLASSES.ID),
                                    r.get(EVENT_CLASSES.RACING_CLASS_ID)));
                }
                // Update entryIdToRacingClassId to actual racingClassId
                entryIdToRacingClassId.replaceAll((eid, ecId) ->
                        ecToRacingClass.getOrDefault(ecId, ecId));
            }

            // Load user names for all entered drivers
            Set<Long> allDriverIds = new HashSet<>(entryIdToUserId.values());
            if (!allDriverIds.isEmpty()) {
                dsl.select(USERS.ID, USERS.FIRST_NAME, USERS.LAST_NAME)
                        .from(USERS)
                        .where(USERS.ID.in(allDriverIds))
                        .forEach(r -> {
                            driverFirstName.put(r.get(USERS.ID), r.get(USERS.FIRST_NAME));
                            driverLastName.put(r.get(USERS.ID), r.get(USERS.LAST_NAME));
                        });
            }

            // Step 6b/6c: Deserialize positions_json and collect per-driver best position per class
            for (var race : finishedRaces) {
                JSONB posJson = race.get(RESULT_SNAPSHOTS.POSITIONS_JSON);
                String roundType = race.get(ROUNDS.TYPE);
                String finalLetter = race.get(RACES.FINAL_LETTER);
                Long raceId = race.get(RACES.ID);
                Long racingClassIdForRace = null; // determined from event_class
                var raceEventClassId = race.get(EVENT_CLASSES.RACING_CLASS_ID);
                racingClassIdForRace = raceEventClassId;

                if (posJson == null) continue;

                try {
                    List<ResultSnapshotDto.ResultRow> positions = objectMapper.readValue(
                            posJson.data(), new TypeReference<>() {});

                    for (ResultSnapshotDto.ResultRow row : positions) {
                        Long userId = entryIdToUserId.get(row.entryId());
                        if (userId == null) continue;

                        Long rcId = racingClassIdForRace;

                        // Step 9 bonus tracking: TQ
                        if ("QUALIFIER".equals(roundType) && row.position() == 1) {
                            tqDrivers.add(userId);
                        }
                        // A-final winner bonus
                        if ("FINAL".equals(roundType) && "A".equals(finalLetter) && row.position() == 1) {
                            afinalWinners.add(userId);
                        }

                        // Track best position per driver per racing class at this event
                        classBestPosition
                                .computeIfAbsent(rcId, k -> new HashMap<>())
                                .merge(userId, row.position(), Math::min);
                    }
                } catch (Exception e) {
                    // Malformed snapshot — skip this race
                }
            }

            // Step 7: Build RoundResultDto for each driver that appeared in positions OR race_entries
            // Collect all (driverId, racingClassId) pairs seen at this event
            // From positions (via classBestPosition)
            for (var classEntry : classBestPosition.entrySet()) {
                Long rcId = classEntry.getKey();
                for (var driverEntry : classEntry.getValue().entrySet()) {
                    Long userId = driverEntry.getKey();
                    int position = driverEntry.getValue();
                    String exKey = userId + ":" + eventId;
                    boolean excluded = exclusionKeys.contains(exKey);
                    int points = excluded ? 0 : pointsScale.getOrDefault(position, 0);

                    String driverKey = userId + ":" + rcId;
                    driverKeyToClassId.put(driverKey, rcId);
                    driverKeyToUserId.put(driverKey, userId);
                    driverRounds.computeIfAbsent(driverKey, k -> new ArrayList<>())
                            .add(new RoundResultDto(roundNumber, eventId, eventName,
                                    excluded ? 0 : position, points, excluded, false));
                }
            }

            // DNS drivers: in race_entries but not in positions at this event for their class
            for (var entryIdEntry : entryIdToUserId.entrySet()) {
                Long entryId = entryIdEntry.getKey();
                Long userId = entryIdEntry.getValue();
                Long rcId = entryIdToRacingClassId.get(entryId);
                if (rcId == null) continue;

                Long finalRcId = rcId;
                boolean appearedInPositions = classBestPosition
                        .getOrDefault(finalRcId, Map.of())
                        .containsKey(userId);

                if (!appearedInPositions) {
                    // ASSUMED: DNS counts toward Y rounds (club confirmation pending — see STATE.md)
                    String driverKey = userId + ":" + rcId;
                    driverKeyToClassId.put(driverKey, rcId);
                    driverKeyToUserId.put(driverKey, userId);
                    boolean excluded = exclusionKeys.contains(userId + ":" + eventId);
                    driverRounds.computeIfAbsent(driverKey, k -> new ArrayList<>())
                            .add(new RoundResultDto(roundNumber, eventId, eventName,
                                    0, 0, excluded, false));
                }
            }
        }

        // Step 8: Apply best-X-from-Y drop logic per driver-class group
        // Collect all distinct (driverId, racingClassId) combinations and build standings
        Map<Long, List<StandingsRowDto>> byClass = new LinkedHashMap<>();

        for (var driverKey : driverRounds.keySet()) {
            Long userId = driverKeyToUserId.get(driverKey);
            Long rcId = driverKeyToClassId.get(driverKey);
            List<RoundResultDto> rounds = new ArrayList<>(driverRounds.get(driverKey));

            // Determine effective X and Y
            Integer[] classOvr = classOverrides.get(rcId);
            Integer effectiveX = classOvr != null && classOvr[0] != null ? classOvr[0] : champBestX;
            Integer effectiveY = classOvr != null && classOvr[1] != null ? classOvr[1] : champBestY;
            int totalRounds = rounds.size();
            int useX = effectiveX != null ? effectiveX : totalRounds;
            int useY = effectiveY != null ? effectiveY : totalRounds;

            // Sort by points DESC to identify worst rounds to drop
            List<RoundResultDto> sorted = new ArrayList<>(rounds);
            sorted.sort(Comparator.comparingInt(RoundResultDto::points).reversed());

            // Mark bottom (Y - X) rounds as dropped, but only if we have >= Y rounds
            Set<Integer> droppedIndices = new HashSet<>();
            if (useY > useX && totalRounds >= useY) {
                int dropCount = useY - useX;
                // The last dropCount entries in sorted (lowest points) get dropped
                for (int i = sorted.size() - dropCount; i < sorted.size(); i++) {
                    if (i >= 0) droppedIndices.add(i);
                }
            }

            // Rebuild rounds with dropped flags set; we need to map back to original order
            // Create a list with dropped flags based on sorted index identity
            // Use object identity to match sorted back to original
            List<RoundResultDto> withDropped = new ArrayList<>();
            for (int si = 0; si < sorted.size(); si++) {
                RoundResultDto original = sorted.get(si);
                boolean drop = droppedIndices.contains(si);
                withDropped.add(new RoundResultDto(
                        original.roundNumber(), original.eventId(), original.eventName(),
                        original.position(), original.points(), original.excluded(), drop));
            }

            // Restore chronological order (sort by roundNumber)
            withDropped.sort(Comparator.comparingInt(RoundResultDto::roundNumber));

            // Total points = sum of non-dropped, non-excluded rounds
            int totalPoints = withDropped.stream()
                    .filter(r -> !r.dropped() && !r.excluded())
                    .mapToInt(RoundResultDto::points)
                    .sum();

            // Step 9: Apply TQ and A-final bonuses
            if (tqDrivers.contains(userId)) {
                totalPoints += tqBonus;
            }
            if (afinalWinners.contains(userId)) {
                totalPoints += afinalBonus;
            }

            String firstName = driverFirstName.getOrDefault(userId, "");
            String lastName = driverLastName.getOrDefault(userId, "");

            StandingsRowDto row = new StandingsRowDto(userId, firstName, lastName,
                    rcId, totalPoints, withDropped);

            byClass.computeIfAbsent(rcId, k -> new ArrayList<>()).add(row);
        }

        // Step 10: Sort each class by totalPoints DESC, firstName as tiebreak
        List<StandingsRowDto> result = new ArrayList<>();
        for (var classRows2 : byClass.values()) {
            classRows2.sort(Comparator.comparingInt(StandingsRowDto::totalPoints).reversed()
                    .thenComparing(StandingsRowDto::firstName));
            result.addAll(classRows2);
        }

        return result;
    }
}
