package dev.monkeypatch.rctiming.query.event;

import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static dev.monkeypatch.rctiming.jooq.generated.tables.ChampionshipEventLinks.CHAMPIONSHIP_EVENT_LINKS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Events.EVENTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Races.RACES;
import static dev.monkeypatch.rctiming.jooq.generated.tables.Rounds.ROUNDS;

@Service
@Transactional(readOnly = true)
public class EventScheduleQuery {

    private static final Logger log = LoggerFactory.getLogger(EventScheduleQuery.class);

    private final DSLContext dsl;

    public EventScheduleQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<EventScheduleDto> getPublicSchedule() {
        Instant now = Instant.now();

        // Main events fetch — initial pass; finishedRaceIds and championshipId populated in two-pass enrichment
        List<EventScheduleDto> events = dsl
                .select(EVENTS.ID, EVENTS.NAME, EVENTS.EVENT_DATE,
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
                            avail,
                            List.of(),   // populated in enrichment pass below
                            null);       // populated in enrichment pass below
                });

        if (events.isEmpty()) {
            return events;
        }

        List<Long> eventIds = events.stream().map(EventScheduleDto::id).toList();

        // Pass 1 — finished race IDs per event: rounds.event_id → races.id where status=FINISHED
        Map<Long, List<Long>> finishedRacesByEvent = dsl
                .select(ROUNDS.EVENT_ID, RACES.ID)
                .from(RACES)
                .join(ROUNDS).on(ROUNDS.ID.eq(RACES.ROUND_ID))
                .where(ROUNDS.EVENT_ID.in(eventIds))
                .and(RACES.STATUS.eq("FINISHED"))
                .fetchGroups(ROUNDS.EVENT_ID, RACES.ID);

        // Pass 2 — championship ID per event (one championship per event by convention;
        // fetchGroups + first-element avoids fetchMap's duplicate-key exception).
        Map<Long, Long> championshipByEvent = dsl
                .select(CHAMPIONSHIP_EVENT_LINKS.EVENT_ID, CHAMPIONSHIP_EVENT_LINKS.CHAMPIONSHIP_ID)
                .from(CHAMPIONSHIP_EVENT_LINKS)
                .where(CHAMPIONSHIP_EVENT_LINKS.EVENT_ID.in(eventIds))
                .fetchGroups(CHAMPIONSHIP_EVENT_LINKS.EVENT_ID, CHAMPIONSHIP_EVENT_LINKS.CHAMPIONSHIP_ID)
                .entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        e -> {
                            if (e.getValue().size() > 1) {
                                log.warn("Event {} is linked to {} championships; only the first is shown on the schedule",
                                        e.getKey(), e.getValue().size());
                            }
                            return e.getValue().get(0);
                        }
                ));

        // Re-map the events list with the two enrichment fields populated
        return events.stream().map(e -> new EventScheduleDto(
                e.id(),
                e.name(),
                e.eventDate(),
                e.entryAvailability(),
                finishedRacesByEvent.getOrDefault(e.id(), List.of()),
                championshipByEvent.get(e.id())  // null if not found
        )).toList();
    }
}
