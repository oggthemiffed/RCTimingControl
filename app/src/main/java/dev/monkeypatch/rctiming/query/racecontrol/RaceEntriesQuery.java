package dev.monkeypatch.rctiming.query.racecontrol;

import dev.monkeypatch.rctiming.api.racecontrol.dto.RaceEntryDto;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Cars.CARS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RaceEntries.RACE_ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Users.USERS;

@Component
@Transactional(readOnly = true)
public class RaceEntriesQuery {

    private final DSLContext dsl;

    public RaceEntriesQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<RaceEntryDto> findForRace(long raceId) {
        return dsl
                .select(
                        RACE_ENTRIES.ENTRY_ID,
                        USERS.FIRST_NAME,
                        USERS.LAST_NAME,
                        CARS.NAME.as("carName"))
                .from(RACE_ENTRIES)
                .join(ENTRIES).on(ENTRIES.ID.eq(RACE_ENTRIES.ENTRY_ID))
                .join(USERS).on(USERS.ID.eq(ENTRIES.USER_ID))
                .leftJoin(CARS).on(CARS.ID.eq(ENTRIES.CAR_ID))
                .where(RACE_ENTRIES.RACE_ID.eq(raceId))
                .orderBy(RACE_ENTRIES.GRID_POSITION.asc().nullsLast())
                .fetch(r -> new RaceEntryDto(
                        r.get(RACE_ENTRIES.ENTRY_ID),
                        r.get(USERS.FIRST_NAME) + " " + r.get(USERS.LAST_NAME),
                        r.get("carName", String.class)
                ));
    }
}
