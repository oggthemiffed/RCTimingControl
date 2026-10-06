package dev.monkeypatch.rctiming.query.racecontrol;

import dev.monkeypatch.rctiming.api.racecontrol.dto.CheckInEntryDto;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Competitors.COMPETITORS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.EventClasses.EVENT_CLASSES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.RacingClasses.RACING_CLASSES;

/**
 * jOOQ read side of the check-in desk (L11): look entries up by a scanned transponder number
 * or by the competitor's name. Withdrawn entries are never returned.
 */
@Component
@Transactional(readOnly = true)
public class CheckInQuery {

    static final int SEARCH_LIMIT = 25;

    private final DSLContext dsl;

    public CheckInQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Entries in the event that use this number as primary or secondary transponder. More than
     * one comes back when a competitor shares a transponder across classes.
     */
    public List<CheckInEntryDto> findByTransponder(long eventId, String transponderNumber) {
        String number = transponderNumber.trim();
        return select(ENTRIES.TRANSPONDER_NUMBER.eq(number)
                .or(ENTRIES.SECONDARY_TRANSPONDER_NUMBER.eq(number)), eventId);
    }

    /** Case-insensitive substring match on the competitor's name. A blank query finds nothing. */
    public List<CheckInEntryDto> searchByName(long eventId, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String pattern = "%" + escapeLike(query.trim()) + "%";
        return select(COMPETITORS.DISPLAY_NAME.likeIgnoreCase(pattern, '\\'), eventId);
    }

    public Optional<CheckInEntryDto> findEntry(long eventId, long entryId) {
        return select(ENTRIES.ID.eq(entryId), eventId).stream().findFirst();
    }

    private List<CheckInEntryDto> select(Condition match, long eventId) {
        return dsl
                .select(
                        ENTRIES.ID,
                        DSL.coalesce(COMPETITORS.DISPLAY_NAME, DSL.val("Unknown")).as("competitorName"),
                        RACING_CLASSES.NAME.as("className"),
                        ENTRIES.TRANSPONDER_NUMBER,
                        ENTRIES.SECONDARY_TRANSPONDER_NUMBER,
                        ENTRIES.CHECKED_IN_AT,
                        ENTRIES.RACEHUB_ARRIVAL,
                        ENTRIES.IMPORTED_TRANSPONDER_NUMBER,
                        ENTRIES.IMPORTED_SECONDARY_TRANSPONDER_NUMBER)
                .from(ENTRIES)
                .leftJoin(COMPETITORS).on(COMPETITORS.ID.eq(ENTRIES.COMPETITOR_ID))
                .leftJoin(EVENT_CLASSES).on(EVENT_CLASSES.ID.eq(ENTRIES.EVENT_CLASS_ID))
                .leftJoin(RACING_CLASSES).on(RACING_CLASSES.ID.eq(EVENT_CLASSES.RACING_CLASS_ID))
                .where(ENTRIES.EVENT_ID.eq(eventId))
                .and(ENTRIES.STATUS.ne("WITHDRAWN"))
                .and(match)
                .orderBy(COMPETITORS.DISPLAY_NAME.asc().nullsLast(), RACING_CLASSES.NAME.asc(), ENTRIES.ID.asc())
                .limit(SEARCH_LIMIT)
                .fetch(CheckInQuery::toDto);
    }

    private static CheckInEntryDto toDto(Record r) {
        Instant checkedInAt = r.get(ENTRIES.CHECKED_IN_AT);
        return new CheckInEntryDto(
                r.get(ENTRIES.ID),
                r.get("competitorName", String.class),
                r.get("className", String.class),
                r.get(ENTRIES.TRANSPONDER_NUMBER),
                r.get(ENTRIES.SECONDARY_TRANSPONDER_NUMBER),
                checkedInAt != null,
                checkedInAt,
                r.get(ENTRIES.RACEHUB_ARRIVAL),
                r.get(ENTRIES.IMPORTED_TRANSPONDER_NUMBER),
                r.get(ENTRIES.IMPORTED_SECONDARY_TRANSPONDER_NUMBER));
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
