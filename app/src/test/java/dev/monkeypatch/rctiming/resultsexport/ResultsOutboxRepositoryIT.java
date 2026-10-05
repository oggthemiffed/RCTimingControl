package dev.monkeypatch.rctiming.resultsexport;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import dev.monkeypatch.rctiming.domain.event.Event;
import dev.monkeypatch.rctiming.domain.event.EventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

import static dev.monkeypatch.rctiming.persistence.RoundTrip.assertSavedAndReloaded;
import static org.assertj.core.api.Assertions.assertThat;

/** The results outbox repository saves and loads every field, and moves exports between states (#75). */
class ResultsOutboxRepositoryIT extends AbstractIntegrationTest {

    private static final Instant T1 = Instant.parse("2026-10-05T10:15:30.123456Z");
    private static final Instant T2 = T1.plus(1, ChronoUnit.HOURS);

    @Autowired ResultsOutboxRepository outbox;
    @Autowired EventRepository events;

    private Event event;

    @BeforeEach
    void setUp() {
        Event e = new Event();
        e.setName("Outbox event");
        e.setEventDate(LocalDate.of(2026, 10, 5));
        e.setCreatedAt(T1);
        e.setUpdatedAt(T1);
        event = events.save(e);
    }

    @AfterEach
    void cleanUp() {
        // The outbox goes with its event
        events.deleteById(event.getId());
    }

    @Test
    void roundTrip() {
        ResultsOutboxItem i = item(1, OutboxStatus.FAILED, T1);
        i.setAttempts(2);
        i.setLastError("RaceHub said no");
        i.setSentAt(T2);
        assertSavedAndReloaded(outbox, i, x -> {
            x.setRevision(2);
            x.setReason(ExportReason.DAY_CLOSE);
            x.setPayload("{\"revision\":2}");
            x.setStatus(OutboxStatus.SENT);
            x.setAttempts(3);
            x.setNextAttemptAt(T2);
            x.setLastError(null);
            x.setSentAt(null);
            return x;
        }, ResultsOutboxItem::getId);
    }

    @Test
    void theCreationTimeIsSetOnInsertOnly() {
        ResultsOutboxItem saved = outbox.save(item(1, OutboxStatus.QUEUED, T1));
        saved.setCreatedAt(T2);
        outbox.save(saved);

        assertThat(outbox.findById(saved.getId()).orElseThrow().getCreatedAt()).isEqualTo(T1);
    }

    @Test
    void findsTheExportsDueToGo() {
        ResultsOutboxItem due = outbox.save(item(1, OutboxStatus.QUEUED, T1));
        ResultsOutboxItem failed = outbox.save(item(2, OutboxStatus.FAILED, T1));
        outbox.save(item(3, OutboxStatus.QUEUED, T2));
        outbox.save(item(4, OutboxStatus.SENT, T1));

        assertThat(outbox.findByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
                EnumSet.of(OutboxStatus.QUEUED, OutboxStatus.FAILED), T1))
                .filteredOn(x -> x.getEventId().equals(event.getId()))
                .extracting(ResultsOutboxItem::getId).containsExactly(due.getId(), failed.getId());
    }

    @Test
    void aSentExportIsRecordedEvenIfItWasSupersededMeanwhile() {
        ResultsOutboxItem item = outbox.save(item(1, OutboxStatus.QUEUED, T1));
        item.setLastError("earlier failure");
        outbox.save(item);
        outbox.supersedeWaiting(event.getId());

        assertThat(outbox.recordSent(item.getId(), T2)).isEqualTo(1);

        ResultsOutboxItem sent = outbox.findById(item.getId()).orElseThrow();
        assertThat(sent.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(sent.getSentAt()).isEqualTo(T2);
        assertThat(sent.getAttempts()).isEqualTo(1);
        assertThat(sent.getLastError()).isNull();
    }

    @Test
    void aFailureIsRecordedOnlyWhileTheExportIsWaiting() {
        ResultsOutboxItem waiting = outbox.save(item(1, OutboxStatus.QUEUED, T1));

        assertThat(outbox.recordFailure(waiting.getId(), "timed out", T2)).isEqualTo(1);
        ResultsOutboxItem failed = outbox.findById(waiting.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(failed.getAttempts()).isEqualTo(1);
        assertThat(failed.getLastError()).isEqualTo("timed out");
        assertThat(failed.getNextAttemptAt()).isEqualTo(T2);

        outbox.supersedeWaiting(event.getId());
        assertThat(outbox.recordFailure(waiting.getId(), "timed out again", T2.plusSeconds(60))).isZero();
        assertThat(outbox.findById(waiting.getId()).orElseThrow().getStatus())
                .as("a superseded export is never sent after the newer one").isEqualTo(OutboxStatus.SUPERSEDED);
    }

    @Test
    void supersedingLeavesSentExportsAlone() {
        ResultsOutboxItem queued = outbox.save(item(1, OutboxStatus.QUEUED, T1));
        ResultsOutboxItem failed = outbox.save(item(2, OutboxStatus.FAILED, T1));
        ResultsOutboxItem sent = outbox.save(item(3, OutboxStatus.SENT, T1));

        assertThat(outbox.supersedeWaiting(event.getId())).isEqualTo(2);

        assertThat(outbox.findAllById(List.of(queued.getId(), failed.getId(), sent.getId())))
                .extracting(ResultsOutboxItem::getStatus)
                .containsExactlyInAnyOrder(OutboxStatus.SUPERSEDED, OutboxStatus.SUPERSEDED, OutboxStatus.SENT);
    }

    @Test
    void onlyAWaitingExportCanBeMadeDue() {
        ResultsOutboxItem failed = outbox.save(item(1, OutboxStatus.FAILED, T2));
        ResultsOutboxItem sent = outbox.save(item(2, OutboxStatus.SENT, T2));

        assertThat(outbox.makeDue(failed.getId(), T1)).isEqualTo(1);
        assertThat(outbox.makeDue(sent.getId(), T1)).isZero();

        assertThat(outbox.findById(failed.getId()).orElseThrow().getNextAttemptAt()).isEqualTo(T1);
        assertThat(outbox.findById(sent.getId()).orElseThrow().getNextAttemptAt()).isEqualTo(T2);
    }

    private ResultsOutboxItem item(long revision, OutboxStatus status, Instant nextAttemptAt) {
        ResultsOutboxItem i = new ResultsOutboxItem();
        i.setEventId(event.getId());
        i.setRevision(revision);
        i.setReason(ExportReason.RACE_FINISHED);
        i.setPayload("{\"revision\":" + revision + "}");
        i.setStatus(status);
        i.setNextAttemptAt(nextAttemptAt);
        i.setCreatedAt(T1);
        return i;
    }
}
