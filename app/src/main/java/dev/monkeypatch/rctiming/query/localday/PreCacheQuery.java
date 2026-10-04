package dev.monkeypatch.rctiming.query.localday;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Competitors.COMPETITORS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RacingClasses.RACING_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Races.RACES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Rounds.ROUNDS;

/**
 * Read-side projection of an event's confirmed entries and race schedule for the local day
 * pre-cache payload. jOOQ only — no Hibernate involvement, per the domain/query module seam.
 */
@Service
@Transactional(readOnly = true)
public class PreCacheQuery {

    private final DSLContext dsl;

    public PreCacheQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Every confirmed entry, named by its competitor, so RaceHub imports and walk-ins (which have
     * no login) are cached too. Cars went with racer accounts (L10, #18), so carName is null.
     */
    public List<PreCacheEntryRow> confirmedEntries(Long eventId) {
        return dsl
                .select(ENTRIES.ID, ENTRIES.TRANSPONDER_NUMBER, COMPETITORS.DISPLAY_NAME, RACING_CLASSES.NAME)
                .from(ENTRIES)
                .leftJoin(COMPETITORS).on(ENTRIES.COMPETITOR_ID.eq(COMPETITORS.ID))
                .leftJoin(EVENT_CLASSES).on(ENTRIES.EVENT_CLASS_ID.eq(EVENT_CLASSES.ID))
                .leftJoin(RACING_CLASSES).on(EVENT_CLASSES.RACING_CLASS_ID.eq(RACING_CLASSES.ID))
                .where(ENTRIES.EVENT_ID.eq(eventId))
                .and(ENTRIES.STATUS.eq("CONFIRMED"))
                .fetch(r -> new PreCacheEntryRow(
                        r.get(ENTRIES.ID),
                        r.get(ENTRIES.TRANSPONDER_NUMBER),
                        r.get(COMPETITORS.DISPLAY_NAME),
                        null,
                        r.get(RACING_CLASSES.NAME)
                ));
    }

    public List<PreCacheRaceRow> schedule(Long eventId) {
        return dsl
                .select(RACES.ID, ROUNDS.ROUND_NUMBER, RACES.HEAT_NUMBER, RACES.SEQUENCE_IN_ROUND,
                        RACING_CLASSES.NAME, RACES.FINAL_LETTER, RACES.STATUS)
                .from(RACES)
                .join(ROUNDS).on(RACES.ROUND_ID.eq(ROUNDS.ID))
                .leftJoin(EVENT_CLASSES).on(RACES.EVENT_CLASS_ID.eq(EVENT_CLASSES.ID))
                .leftJoin(RACING_CLASSES).on(EVENT_CLASSES.RACING_CLASS_ID.eq(RACING_CLASSES.ID))
                .where(ROUNDS.EVENT_ID.eq(eventId))
                .fetch(r -> new PreCacheRaceRow(
                        r.get(RACES.ID),
                        r.get(ROUNDS.ROUND_NUMBER),
                        r.get(RACES.HEAT_NUMBER),
                        r.get(RACES.SEQUENCE_IN_ROUND),
                        r.get(RACING_CLASSES.NAME),
                        r.get(RACES.FINAL_LETTER),
                        r.get(RACES.STATUS)
                ));
    }
}
