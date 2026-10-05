package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.query.resultsexport.ResultsOutboxQuery;
import dev.monkeypatch.rctiming.resultsexport.RaceHubResultsProperties;
import dev.monkeypatch.rctiming.resultsexport.ResultsExportService;
import dev.monkeypatch.rctiming.resultsexport.ResultsExportV1;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Results sent to RaceHub (#27): what is queued and sent, retry now, and an event's results file. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class ResultsExportController {

    private static final int OUTBOX_ROWS = 100;

    private final ResultsExportService exportService;
    private final ResultsOutboxQuery outboxQuery;
    private final RaceHubResultsProperties properties;

    public ResultsExportController(ResultsExportService exportService, ResultsOutboxQuery outboxQuery,
                                   RaceHubResultsProperties properties) {
        this.exportService = exportService;
        this.outboxQuery = outboxQuery;
        this.properties = properties;
    }

    /**
     * @param sendingEnabled  whether the RaceHub address and key are both set; without them, exports wait
     * @param resultsUrl      the address, or null
     * @param missingSettings the settings still needed before anything is sent
     * @param exports         the newest exports first
     */
    public record ResultsExportsDto(boolean sendingEnabled, String resultsUrl, List<String> missingSettings,
                                    List<ResultsOutboxQuery.OutboxRow> exports) {
    }

    @GetMapping("/results-exports")
    public ResultsExportsDto list() {
        return new ResultsExportsDto(properties.sendingEnabled(),
                properties.resultsUrl() == null ? null : properties.resultsUrl().toString(),
                properties.missingSettings(),
                outboxQuery.latest(OUTBOX_ROWS));
    }

    @PostMapping("/results-exports/{id}/retry")
    public ResponseEntity<Void> retry(@PathVariable long id) {
        exportService.retryNow(id);
        return ResponseEntity.noContent().build();
    }

    /** The event's results as they stand now, as a Results Export v1 file. */
    @GetMapping("/events/{eventId}/results-export")
    public ResponseEntity<ResultsExportV1> download(@PathVariable long eventId) {
        ResultsExportV1 export = exportService.current(eventId);
        String filename = "results-event-" + eventId + "-r" + export.revision() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(export);
    }
}
