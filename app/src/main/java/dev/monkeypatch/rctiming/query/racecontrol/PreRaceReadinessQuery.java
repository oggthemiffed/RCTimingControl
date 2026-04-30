package dev.monkeypatch.rctiming.query.racecontrol;

import dev.monkeypatch.rctiming.api.racecontrol.dto.GridCallSlotDto;
import dev.monkeypatch.rctiming.api.racecontrol.dto.MarshalDutyRowDto;
import dev.monkeypatch.rctiming.api.racecontrol.dto.PreRaceReadinessDto;
import jakarta.persistence.EntityNotFoundException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.MarshalAbsences.MARSHAL_ABSENCES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RaceEntries.RACE_ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RacingClasses.RACING_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Races.RACES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Rounds.ROUNDS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Users.USERS;

/**
 * jOOQ read-side query for the pre-race readiness view (CTRL-02, CTRL-07).
 * Produces a PreRaceReadinessDto for a given raceId, containing:
 * - gridCall: drivers due on track in grid order for the target race
 * - marshalDuty: drivers from the immediately preceding race with per-event absence counts
 *
 * No Hibernate or JPA is used — all reads go through DSLContext.
 */
@Component
@Transactional(readOnly = true)
public class PreRaceReadinessQuery {

    private final DSLContext dsl;

    public PreRaceReadinessQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Load the pre-race readiness data for the given race.
     *
     * @param raceId the race to load readiness for
     * @return a fully populated PreRaceReadinessDto
     * @throws EntityNotFoundException if no race with this ID exists
     */
    public PreRaceReadinessDto load(long raceId) {
        // Step 1: Fetch target race row with round and class info
        var targetRow = dsl
                .select(
                        RACES.ID,
                        RACES.HEAT_NUMBER,
                        RACES.FINAL_LETTER,
                        RACES.STATUS,
                        RACES.SEQUENCE_IN_ROUND,
                        RACES.EVENT_CLASS_ID,
                        ROUNDS.EVENT_ID,
                        ROUNDS.SEQUENCE_IN_EVENT,
                        ROUNDS.ROUND_NUMBER,
                        ROUNDS.TYPE,
                        RACING_CLASSES.NAME.as("className"))
                .from(RACES)
                .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
                .join(EVENT_CLASSES).on(EVENT_CLASSES.ID.eq(RACES.EVENT_CLASS_ID))
                .join(RACING_CLASSES).on(RACING_CLASSES.ID.eq(EVENT_CLASSES.RACING_CLASS_ID))
                .where(RACES.ID.eq(raceId))
                .fetchOne();

        if (targetRow == null) {
            throw new EntityNotFoundException("Race not found: " + raceId);
        }

        int heatNumber = targetRow.get(RACES.HEAT_NUMBER);
        String finalLetter = targetRow.get(RACES.FINAL_LETTER);
        int sequenceInRound = targetRow.get(RACES.SEQUENCE_IN_ROUND);
        long eventId = targetRow.get(ROUNDS.EVENT_ID);
        int sequenceInEvent = targetRow.get(ROUNDS.SEQUENCE_IN_EVENT);
        int roundNumber = targetRow.get(ROUNDS.ROUND_NUMBER);
        String roundType = targetRow.get(ROUNDS.TYPE);
        String className = targetRow.get("className", String.class);

        // Step 2: Build raceLabel from round type
        String raceLabel = buildRaceLabel(roundType, roundNumber, className, heatNumber, finalLetter);

        // Step 3: Resolve the previous race in the event run order
        // "Previous" = highest (sequence_in_event, sequence_in_round) tuple strictly less than target
        var previousRaceRow = dsl
                .select(RACES.ID, ROUNDS.EVENT_ID)
                .from(RACES)
                .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
                .where(ROUNDS.EVENT_ID.eq(eventId))
                .and(
                        ROUNDS.SEQUENCE_IN_EVENT.lt(sequenceInEvent)
                        .or(ROUNDS.SEQUENCE_IN_EVENT.eq(sequenceInEvent)
                                .and(RACES.SEQUENCE_IN_ROUND.lt(sequenceInRound)))
                )
                .orderBy(ROUNDS.SEQUENCE_IN_EVENT.desc(), RACES.SEQUENCE_IN_ROUND.desc())
                .limit(1)
                .fetchOne();

        boolean firstRaceOfEvent = (previousRaceRow == null);
        List<MarshalDutyRowDto> marshalDuty;

        if (previousRaceRow != null) {
            // Step 4: Build marshalDuty from previous race's entries + absence counts
            long previousRaceId = previousRaceRow.get(RACES.ID);

            // Correlated subquery alias for absence count
            var absenceCount = DSL.select(DSL.count())
                    .from(MARSHAL_ABSENCES)
                    .where(MARSHAL_ABSENCES.ENTRY_ID.eq(RACE_ENTRIES.ENTRY_ID))
                    .and(MARSHAL_ABSENCES.EVENT_ID.eq(eventId))
                    .asField("missedThisEvent");

            marshalDuty = dsl
                    .select(
                            RACE_ENTRIES.ENTRY_ID,
                            DSL.coalesce(
                                    DSL.concat(USERS.FIRST_NAME, DSL.val(" "), USERS.LAST_NAME),
                                    USERS.EMAIL
                            ).as("driverName"),
                            absenceCount)
                    .from(RACE_ENTRIES)
                    .join(ENTRIES).on(ENTRIES.ID.eq(RACE_ENTRIES.ENTRY_ID))
                    .join(USERS).on(USERS.ID.eq(ENTRIES.USER_ID))
                    .where(RACE_ENTRIES.RACE_ID.eq(previousRaceId))
                    .orderBy(RACE_ENTRIES.GRID_POSITION.asc().nullsLast(), ENTRIES.ID.asc())
                    .fetch(r -> new MarshalDutyRowDto(
                            r.get(RACE_ENTRIES.ENTRY_ID),
                            r.get("driverName", String.class),
                            null, // car_number column does not exist in the entries table
                            r.get("missedThisEvent", Long.class)
                    ));
        } else {
            marshalDuty = List.of();
        }

        // Step 5: Build gridCall for the target race
        List<GridCallSlotDto> gridCall = dsl
                .select(
                        RACE_ENTRIES.ID,
                        RACE_ENTRIES.ENTRY_ID,
                        RACE_ENTRIES.GRID_POSITION,
                        DSL.coalesce(
                                DSL.concat(USERS.FIRST_NAME, DSL.val(" "), USERS.LAST_NAME),
                                USERS.EMAIL
                        ).as("driverName"),
                        RACING_CLASSES.NAME.as("className"))
                .from(RACE_ENTRIES)
                .join(RACES).on(RACES.ID.eq(RACE_ENTRIES.RACE_ID))
                .join(ENTRIES).on(ENTRIES.ID.eq(RACE_ENTRIES.ENTRY_ID))
                .join(USERS).on(USERS.ID.eq(ENTRIES.USER_ID))
                .join(EVENT_CLASSES).on(EVENT_CLASSES.ID.eq(RACES.EVENT_CLASS_ID))
                .join(RACING_CLASSES).on(RACING_CLASSES.ID.eq(EVENT_CLASSES.RACING_CLASS_ID))
                .where(RACE_ENTRIES.RACE_ID.eq(raceId))
                .orderBy(RACE_ENTRIES.GRID_POSITION.asc().nullsLast(), ENTRIES.ID.asc())
                .fetch(r -> new GridCallSlotDto(
                        r.get(RACE_ENTRIES.GRID_POSITION) != null
                                ? r.get(RACE_ENTRIES.GRID_POSITION)
                                : 0,
                        r.get(RACE_ENTRIES.ENTRY_ID),
                        r.get("driverName", String.class),
                        null, // car_number not yet on Entry — tracked as gap, see REQUIREMENTS.md ENTRY-03
                        r.get("className", String.class)
                ));

        return new PreRaceReadinessDto(raceId, raceLabel, firstRaceOfEvent, gridCall, marshalDuty);
    }

    private String buildRaceLabel(String roundType, int roundNumber, String className,
                                  int heatNumber, String finalLetter) {
        return switch (roundType) {
            case "PRACTICE" -> "Practice " + roundNumber + " — " + className + " — Heat " + heatNumber;
            case "QUALIFIER" -> "Qualifying " + roundNumber + " — " + className + " — Heat " + heatNumber;
            case "FINAL" -> (finalLetter != null ? finalLetter : "A") + " Final — " + className;
            default -> roundType + " " + roundNumber + " — " + className + " — Heat " + heatNumber;
        };
    }
}
