package dev.monkeypatch.rctiming.query.results;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.monkeypatch.rctiming.api.racecontrol.dto.ResultSnapshotDto;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Events.EVENTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RaceEntries.RACE_ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RacingClasses.RACING_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Races.RACES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ResultSnapshots.RESULT_SNAPSHOTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Rounds.ROUNDS;

/**
 * RESULT-03 read side: returns result history for a given racer, grouped by event.
 *
 * IDOR safety (T-07-03-01): userId MUST originate from the JWT principal in the
 * controller (Authentication.getName()), never from a caller-supplied request parameter.
 * This query enforces ENTRIES.USER_ID.eq(userId) as the primary scope guard.
 */
@Service
@Transactional(readOnly = true)
public class RacerResultHistoryQuery {

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public RacerResultHistoryQuery(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the result history for a specific user, grouped by event, sorted most recent first.
     *
     * @param userId the authenticated user's ID — must come from JWT principal, never from caller input
     */
    public List<RacerResultHistoryDto> findForUser(Long userId) {
        // Step 1: Find all entries for this user, join to events for grouping
        var entryRows = dsl
                .select(ENTRIES.ID, ENTRIES.EVENT_ID, EVENTS.NAME, EVENTS.EVENT_DATE)
                .from(ENTRIES)
                .join(EVENTS).on(EVENTS.ID.eq(ENTRIES.EVENT_ID))
                .where(ENTRIES.USER_ID.eq(userId))
                .orderBy(EVENTS.EVENT_DATE.desc())
                .fetch();

        // Group by event — LinkedHashMap preserves DESC order
        Map<Long, RacerResultHistoryDto> byEvent = new LinkedHashMap<>();
        Map<Long, Long> entryIdToEventId = new LinkedHashMap<>();

        for (var row : entryRows) {
            Long eventId = row.get(EVENTS.ID);
            Long entryId = row.get(ENTRIES.ID);

            byEvent.computeIfAbsent(eventId, id -> new RacerResultHistoryDto(
                    id,
                    row.get(EVENTS.NAME),
                    row.get(EVENTS.EVENT_DATE),
                    new ArrayList<>()
            ));
            entryIdToEventId.put(entryId, eventId);
        }

        if (entryIdToEventId.isEmpty()) {
            return List.of();
        }

        // WR-02: Single batch query for all entries instead of one query per entry (N+1).
        // CR-03: entryId.longValue() avoids auto-unboxing NPE on null Long comparison.
        List<Long> allEntryIds = new ArrayList<>(entryIdToEventId.keySet());
        var allRaceRows = dsl
                .select(RACE_ENTRIES.ENTRY_ID, RACES.ID, RACES.FINAL_LETTER, RACES.HEAT_NUMBER,
                        ROUNDS.TYPE, ROUNDS.ROUND_NUMBER,
                        RACING_CLASSES.NAME,
                        RESULT_SNAPSHOTS.POSITIONS_JSON)
                .from(RACE_ENTRIES)
                .join(RACES).on(RACES.ID.eq(RACE_ENTRIES.RACE_ID))
                .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
                .join(EVENT_CLASSES).on(EVENT_CLASSES.ID.eq(RACES.EVENT_CLASS_ID))
                .join(RACING_CLASSES).on(RACING_CLASSES.ID.eq(EVENT_CLASSES.RACING_CLASS_ID))
                .leftJoin(RESULT_SNAPSHOTS).on(RESULT_SNAPSHOTS.RACE_ID.eq(RACES.ID))
                .where(RACE_ENTRIES.ENTRY_ID.in(allEntryIds))
                .orderBy(RACE_ENTRIES.ENTRY_ID.asc(), ROUNDS.ROUND_NUMBER.asc(), RACES.HEAT_NUMBER.asc())
                .fetch();

        for (var raceRow : allRaceRows) {
            Long entryId = raceRow.get(RACE_ENTRIES.ENTRY_ID);
            Long eventId = entryIdToEventId.get(entryId);
            if (eventId == null) continue;

            Long raceId = raceRow.get(RACES.ID);
            String className = raceRow.get(RACING_CLASSES.NAME);
            String roundType = raceRow.get(ROUNDS.TYPE);
            String finalLetter = raceRow.get(RACES.FINAL_LETTER);
            Integer heatNum = raceRow.get(RACES.HEAT_NUMBER);

            String raceLabel = buildRaceLabel(className, roundType, finalLetter, heatNum);

            JSONB posJson = raceRow.get(RESULT_SNAPSHOTS.POSITIONS_JSON);
            int position = 0;
            int lapsCompleted = 0;
            Long bestLapMs = null;

            if (posJson != null) {
                try {
                    List<ResultSnapshotDto.ResultRow> positions = objectMapper.readValue(
                            posJson.data(), new TypeReference<>() {});
                    for (ResultSnapshotDto.ResultRow r : positions) {
                        if (entryId != null && r.entryId() == entryId.longValue()) {
                            position = r.position();
                            lapsCompleted = r.lapsCompleted();
                            bestLapMs = r.bestLapMs();
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    // Unfinished or malformed snapshot — position stays 0
                }
            }

            @SuppressWarnings("unchecked")
            ArrayList<RacerResultHistoryDto.RaceResult> raceList =
                    (ArrayList<RacerResultHistoryDto.RaceResult>) byEvent.get(eventId).races();
            raceList.add(new RacerResultHistoryDto.RaceResult(
                    raceId, raceLabel, position, lapsCompleted, bestLapMs));
        }

        return new ArrayList<>(byEvent.values());
    }

    private String buildRaceLabel(String className, String roundType,
                                   String finalLetter, Integer heatNum) {
        return switch (roundType) {
            case "FINAL" -> className + " - " + (finalLetter != null ? finalLetter : "") + " Final";
            case "QUALIFIER" -> className + " - Q" + (heatNum != null ? heatNum : "");
            default -> className + " - Race";
        };
    }
}
