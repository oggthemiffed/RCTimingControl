package dev.monkeypatch.rctiming.query.entry;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Entries.ENTRIES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Users.USERS;

@Service
@Transactional(readOnly = true)
public class AdminEntryQueryService {

    private final DSLContext dsl;

    public AdminEntryQueryService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<AdminEntryDto> listEntriesForClass(Long eventId, Long eventClassId) {
        return dsl.select(
                        ENTRIES.ID,
                        ENTRIES.USER_ID,
                        USERS.FIRST_NAME,
                        USERS.LAST_NAME,
                        ENTRIES.TRANSPONDER_NUMBER,
                        ENTRIES.STATUS,
                        ENTRIES.SUBMITTED_AT,
                        ENTRIES.WITHDRAWN_AT)
                .from(ENTRIES)
                .join(USERS).on(USERS.ID.eq(ENTRIES.USER_ID))
                .where(ENTRIES.EVENT_ID.eq(eventId))
                .and(ENTRIES.EVENT_CLASS_ID.eq(eventClassId))
                .orderBy(ENTRIES.SUBMITTED_AT.asc())
                .fetch(r -> new AdminEntryDto(
                        r.get(ENTRIES.ID),
                        r.get(ENTRIES.USER_ID),
                        r.get(USERS.FIRST_NAME),
                        r.get(USERS.LAST_NAME),
                        r.get(ENTRIES.TRANSPONDER_NUMBER),
                        r.get(ENTRIES.STATUS),
                        r.get(ENTRIES.SUBMITTED_AT) == null ? null : r.get(ENTRIES.SUBMITTED_AT).toInstant(),
                        r.get(ENTRIES.WITHDRAWN_AT) == null ? null : r.get(ENTRIES.WITHDRAWN_AT).toInstant()));
    }
}
