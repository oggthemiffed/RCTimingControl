package dev.monkeypatch.rctiming.query.entry;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Events.EVENTS;

@Service
@Transactional(readOnly = true)
public class EntryQueryService {

    private final DSLContext dsl;

    public EntryQueryService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<RacerEntryHistoryDto> findHistoryForUser(Long userId) {
        return dsl.select(
                    ENTRIES.ID,
                    EVENTS.ID,
                    EVENTS.NAME,
                    EVENTS.EVENT_DATE,
                    ENTRIES.EVENT_CLASS_ID,
                    ENTRIES.TRANSPONDER_NUMBER,
                    ENTRIES.STATUS,
                    ENTRIES.SUBMITTED_AT,
                    ENTRIES.WITHDRAWN_AT)
                 .from(ENTRIES)
                 .join(EVENTS).on(EVENTS.ID.eq(ENTRIES.EVENT_ID))
                 .where(ENTRIES.USER_ID.eq(userId))
                   .and(ENTRIES.STATUS.in("CONFIRMED", "WITHDRAWN"))
                 .orderBy(ENTRIES.SUBMITTED_AT.desc())
                 .fetch(r -> new RacerEntryHistoryDto(
                         r.get(ENTRIES.ID),
                         r.get(EVENTS.ID),
                         r.get(EVENTS.NAME),
                         r.get(EVENTS.EVENT_DATE),
                         r.get(ENTRIES.EVENT_CLASS_ID),
                         r.get(ENTRIES.TRANSPONDER_NUMBER),
                         r.get(ENTRIES.STATUS),
                         r.get(ENTRIES.SUBMITTED_AT) == null ? null : r.get(ENTRIES.SUBMITTED_AT).toInstant(),
                         r.get(ENTRIES.WITHDRAWN_AT) == null ? null : r.get(ENTRIES.WITHDRAWN_AT).toInstant()));
    }
}
