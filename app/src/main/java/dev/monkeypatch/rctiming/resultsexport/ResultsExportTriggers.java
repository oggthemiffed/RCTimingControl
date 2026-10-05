package dev.monkeypatch.rctiming.resultsexport;

import dev.monkeypatch.rctiming.domain.event.EventCompleted;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.RaceStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Requests a results export when a race finishes, when a finished race is corrected and when the race day
 * closes (#27), then queues the requested exports in the background.
 *
 * <p>The request is a mark on the event, saved in the same transaction as the change that made it, so a crash
 * before the export is built still leaves it to be queued. Building and sending happen later on scheduler
 * threads, so race control never waits on either.
 */
@Component
public class ResultsExportTriggers {

    private static final Logger log = LoggerFactory.getLogger(ResultsExportTriggers.class);

    private final ResultsExportService exportService;

    public ResultsExportTriggers(ResultsExportService exportService) {
        this.exportService = exportService;
    }

    @EventListener
    public void onRaceStatusChanged(RaceStatusChangedEvent event) {
        if (event.getNewStatus() == RaceStatus.FINISHED) {
            run(() -> exportService.requestForRace(event.getRaceId(), ExportReason.RACE_FINISHED));
        }
    }

    @EventListener
    public void onFinishedRaceCorrected(FinishedRaceCorrected event) {
        run(() -> exportService.requestForRace(event.raceId(), ExportReason.CORRECTION));
    }

    @EventListener
    public void onDayClosed(EventCompleted event) {
        run(() -> exportService.request(event.eventId(), ExportReason.DAY_CLOSE));
    }

    /** Builds and queues each requested export. A failure leaves the request in place for the next pass. */
    @Scheduled(initialDelay = 2_000, fixedDelayString = "${rctiming.racehub.queue-interval-ms:2000}")
    public void queuePending() {
        for (long eventId : exportService.pendingEventIds()) {
            try {
                exportService.queuePending(eventId);
            } catch (RuntimeException e) {
                log.error("Could not build the results export for event {}; will try again", eventId, e);
            }
        }
    }

    private void run(Runnable request) {
        try {
            request.run();
        } catch (RuntimeException e) {
            // Never fail race control over an export; the next finish or the day close requests one again
            log.error("Could not request a results export", e);
        }
    }
}
