package dev.monkeypatch.rctiming.query.event;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Events.EVENTS;

@Service
@Transactional(readOnly = true)
public class EventScheduleQuery {

    private final DSLContext dsl;

    public EventScheduleQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<EventScheduleDto> getPublicSchedule() {
        Instant now = Instant.now();
        return dsl.select(EVENTS.ID, EVENTS.NAME, EVENTS.EVENT_DATE,
                          EVENTS.STATUS, EVENTS.ENTRY_OPENS_AT, EVENTS.ENTRY_CLOSES_AT)
                  .from(EVENTS)
                  .where(EVENTS.STATUS.in("PUBLISHED", "OPEN", "ENTRIES_CLOSED", "IN_PROGRESS"))
                  .orderBy(EVENTS.EVENT_DATE.asc())
                  .fetch(r -> {
                      String status = r.get(EVENTS.STATUS);
                      OffsetDateTime opensAt = r.get(EVENTS.ENTRY_OPENS_AT);
                      OffsetDateTime closesAt = r.get(EVENTS.ENTRY_CLOSES_AT);

                      EventScheduleDto.EntryAvailability avail;
                      if ("ENTRIES_CLOSED".equals(status) || "IN_PROGRESS".equals(status)
                              || (closesAt != null && closesAt.toInstant().isBefore(now))) {
                          avail = EventScheduleDto.EntryAvailability.ENTRY_CLOSED;
                      } else if ("OPEN".equals(status)
                              || ("PUBLISHED".equals(status)
                                  && (opensAt == null || opensAt.toInstant().isBefore(now))
                                  && (closesAt == null || closesAt.toInstant().isAfter(now)))) {
                          avail = EventScheduleDto.EntryAvailability.ENTRY_OPEN;
                      } else if (opensAt != null && opensAt.toInstant().isAfter(now)) {
                          avail = EventScheduleDto.EntryAvailability.ENTRY_NOT_YET_OPEN;
                      } else {
                          avail = EventScheduleDto.EntryAvailability.ENTRY_CLOSED;
                      }

                      return new EventScheduleDto(
                              r.get(EVENTS.ID),
                              r.get(EVENTS.NAME),
                              r.get(EVENTS.EVENT_DATE),
                              avail);
                  });
    }
}
