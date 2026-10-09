package dev.monkeypatch.rctiming.query.event;

import dev.monkeypatch.rctiming.persistence.ReadTransaction;
import org.jooq.DSLContext;
import org.jooq.Record6;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.monkeypatch.rctiming.jooq.generated.tables.ChampionshipEventLinks.CHAMPIONSHIP_EVENT_LINKS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Events.EVENTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Races.RACES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Rounds.ROUNDS;

@Service
@ReadTransaction
public class EventScheduleQuery {

    private static final Logger log = LoggerFactory.getLogger(EventScheduleQuery.class);

    private final DSLContext dsl;

    public EventScheduleQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<EventScheduleDto> getPublicSchedule() {
        Instant now = Instant.now();
        Result<Record6<Long, String, LocalDate, String, Instant, Instant>> events = dsl
                .select(EVENTS.ID, EVENTS.NAME, EVENTS.EVENT_DATE,
                        EVENTS.STATUS, EVENTS.ENTRY_OPENS_AT, EVENTS.ENTRY_CLOSES_AT)
                .from(EVENTS)
                .where(EVENTS.STATUS.in("PUBLISHED", "OPEN", "ENTRIES_CLOSED", "IN_PROGRESS"))
                .orderBy(EVENTS.EVENT_DATE.asc())
                .fetch();
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.getValues(EVENTS.ID);
        Map<Long, List<Long>> finishedRacesByEvent = finishedRacesByEvent(eventIds);
        Map<Long, Long> championshipByEvent = championshipByEvent(eventIds);

        return events.stream().map(r -> new EventScheduleDto(
                r.get(EVENTS.ID),
                r.get(EVENTS.NAME),
                r.get(EVENTS.EVENT_DATE),
                entryAvailability(r.get(EVENTS.STATUS), r.get(EVENTS.ENTRY_OPENS_AT),
                        r.get(EVENTS.ENTRY_CLOSES_AT), now),
                finishedRacesByEvent.getOrDefault(r.get(EVENTS.ID), List.of()),
                championshipByEvent.get(r.get(EVENTS.ID))
        )).toList();
    }

    static EventScheduleDto.EntryAvailability entryAvailability(String status, Instant opensAt, Instant closesAt,
                                                                Instant now) {
        if ("ENTRIES_CLOSED".equals(status) || "IN_PROGRESS".equals(status)
                || (closesAt != null && closesAt.isBefore(now))) {
            return EventScheduleDto.EntryAvailability.ENTRY_CLOSED;
        }
        if ("OPEN".equals(status)
                || ("PUBLISHED".equals(status)
                    && (opensAt == null || opensAt.isBefore(now))
                    && (closesAt == null || closesAt.isAfter(now)))) {
            return EventScheduleDto.EntryAvailability.ENTRY_OPEN;
        }
        if (opensAt != null && opensAt.isAfter(now)) {
            return EventScheduleDto.EntryAvailability.ENTRY_NOT_YET_OPEN;
        }
        return EventScheduleDto.EntryAvailability.ENTRY_CLOSED;
    }

    private Map<Long, List<Long>> finishedRacesByEvent(List<Long> eventIds) {
        return dsl.select(ROUNDS.EVENT_ID, RACES.ID)
                .from(RACES)
                .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
                .where(ROUNDS.EVENT_ID.in(eventIds))
                .and(RACES.STATUS.eq("FINISHED"))
                .fetchGroups(ROUNDS.EVENT_ID, RACES.ID);
    }

    /** One championship per event by convention; if an event has more, the schedule shows the first. */
    private Map<Long, Long> championshipByEvent(List<Long> eventIds) {
        Map<Long, Long> byEvent = new HashMap<>();
        dsl.select(CHAMPIONSHIP_EVENT_LINKS.EVENT_ID, CHAMPIONSHIP_EVENT_LINKS.CHAMPIONSHIP_ID)
                .from(CHAMPIONSHIP_EVENT_LINKS)
                .where(CHAMPIONSHIP_EVENT_LINKS.EVENT_ID.in(eventIds))
                .fetchGroups(CHAMPIONSHIP_EVENT_LINKS.EVENT_ID, CHAMPIONSHIP_EVENT_LINKS.CHAMPIONSHIP_ID)
                .forEach((eventId, championshipIds) -> {
                    if (championshipIds.size() > 1) {
                        log.warn("Event {} is linked to {} championships; only the first is shown on the schedule",
                                eventId, championshipIds.size());
                    }
                    byEvent.put(eventId, championshipIds.get(0));
                });
        return byEvent;
    }
}
