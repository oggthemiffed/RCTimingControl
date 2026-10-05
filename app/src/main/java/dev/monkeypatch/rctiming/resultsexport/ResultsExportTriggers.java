package dev.monkeypatch.rctiming.resultsexport;

import dev.monkeypatch.rctiming.domain.event.EventCompleted;
import dev.monkeypatch.rctiming.domain.race.RaceStatus;
import dev.monkeypatch.rctiming.domain.race.RaceStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Queues a results export when a race finishes, when a finished race is corrected and when the race day
 * closes (#27). Each runs after the change is committed, on its own thread, so race control never waits on it
 * and a failure here never undoes the change.
 */
@Component
public class ResultsExportTriggers {

    private static final Logger log = LoggerFactory.getLogger(ResultsExportTriggers.class);

    private final ResultsExportService exportService;

    public ResultsExportTriggers(ResultsExportService exportService) {
        this.exportService = exportService;
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onRaceStatusChanged(RaceStatusChangedEvent event) {
        if (event.getNewStatus() == RaceStatus.FINISHED) {
            run(() -> exportService.enqueueForRace(event.getRaceId(), ExportReason.RACE_FINISHED));
        }
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onFinishedRaceCorrected(FinishedRaceCorrected event) {
        run(() -> exportService.enqueueForRace(event.raceId(), ExportReason.CORRECTION));
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onDayClosed(EventCompleted event) {
        run(() -> exportService.enqueue(event.eventId(), ExportReason.DAY_CLOSE));
    }

    private void run(Runnable export) {
        try {
            export.run();
        } catch (RuntimeException e) {
            // The day-close export or the next race's export will carry these results
            log.error("Could not queue a results export", e);
        }
    }
}
