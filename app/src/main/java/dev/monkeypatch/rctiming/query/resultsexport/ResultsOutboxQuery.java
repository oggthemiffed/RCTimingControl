package dev.monkeypatch.rctiming.query.resultsexport;

import dev.monkeypatch.rctiming.persistence.ReadTransaction;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static dev.monkeypatch.rctiming.jooq.generated.tables.Events.EVENTS;
import static dev.monkeypatch.rctiming.jooq.generated.tables.ResultsOutbox.RESULTS_OUTBOX;

/** The results exports queued for RaceHub, newest first, for the admin page (#27). */
@Component
@ReadTransaction
public class ResultsOutboxQuery {

    private final DSLContext dsl;

    public ResultsOutboxQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    public record OutboxRow(
            long id,
            long eventId,
            String eventName,
            long revision,
            String reason,
            String status,
            int attempts,
            Instant nextAttemptAt,
            String lastError,
            Instant createdAt,
            Instant sentAt) {
    }

    public List<OutboxRow> latest(int limit) {
        return dsl.select(RESULTS_OUTBOX.ID, RESULTS_OUTBOX.EVENT_ID, EVENTS.NAME, RESULTS_OUTBOX.REVISION,
                        RESULTS_OUTBOX.REASON, RESULTS_OUTBOX.STATUS, RESULTS_OUTBOX.ATTEMPTS,
                        RESULTS_OUTBOX.NEXT_ATTEMPT_AT, RESULTS_OUTBOX.LAST_ERROR, RESULTS_OUTBOX.CREATED_AT,
                        RESULTS_OUTBOX.SENT_AT)
                .from(RESULTS_OUTBOX)
                .join(EVENTS).on(EVENTS.ID.eq(RESULTS_OUTBOX.EVENT_ID))
                .orderBy(RESULTS_OUTBOX.ID.desc())
                .limit(limit)
                .fetch(r -> new OutboxRow(
                        r.get(RESULTS_OUTBOX.ID),
                        r.get(RESULTS_OUTBOX.EVENT_ID),
                        r.get(EVENTS.NAME),
                        r.get(RESULTS_OUTBOX.REVISION),
                        r.get(RESULTS_OUTBOX.REASON),
                        r.get(RESULTS_OUTBOX.STATUS),
                        r.get(RESULTS_OUTBOX.ATTEMPTS),
                        r.get(RESULTS_OUTBOX.NEXT_ATTEMPT_AT),
                        r.get(RESULTS_OUTBOX.LAST_ERROR),
                        r.get(RESULTS_OUTBOX.CREATED_AT),
                        r.get(RESULTS_OUTBOX.SENT_AT)));
    }
}
