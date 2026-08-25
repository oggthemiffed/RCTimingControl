package dev.monkeypatch.rctiming.api.localday;

import dev.monkeypatch.rctiming.api.localday.dto.SnapshotIngestResponseDto;
import dev.monkeypatch.rctiming.api.localday.dto.SnapshotRequestDto;
import dev.monkeypatch.rctiming.domain.localday.SnapshotIngestOutcome;
import dev.monkeypatch.rctiming.domain.localday.SnapshotIngestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Periodic snapshot ingest (R11, KTD4, KTD8) — the cloud-side counterpart to
 * {@code :localday}'s {@code SnapshotPushService}/{@code SnapshotSyncClient}. Unauthenticated as
 * far as Spring Security's JWT flow goes (permitted in {@code SecurityConfig}) — the caller is a
 * machine (a venue instance), not a logged-in user, so it authenticates via the per-day-instance
 * secret (KTD9) in {@link #INSTANCE_SECRET_HEADER} instead, checked by
 * {@link SnapshotIngestService} before the generation check runs.
 */
@RestController
@RequestMapping("/api/v1/localday/events/{eventId}/snapshots")
public class SnapshotIngestController {

    public static final String INSTANCE_SECRET_HEADER = "X-Localday-Instance-Secret";

    private final SnapshotIngestService snapshotIngestService;

    public SnapshotIngestController(SnapshotIngestService snapshotIngestService) {
        this.snapshotIngestService = snapshotIngestService;
    }

    @PostMapping
    public ResponseEntity<SnapshotIngestResponseDto> ingest(@PathVariable Long eventId,
                                                              @RequestHeader(INSTANCE_SECRET_HEADER) String instanceSecret,
                                                              @RequestBody SnapshotRequestDto request) {
        SnapshotIngestOutcome outcome = snapshotIngestService.ingest(eventId, request.instanceId(), instanceSecret,
                request.generation(), request.snapshotId(), request.payload());

        return switch (outcome) {
            case SnapshotIngestOutcome.Accepted accepted ->
                    ResponseEntity.ok(new SnapshotIngestResponseDto("accepted", accepted.generation()));
            case SnapshotIngestOutcome.Rejected rejected -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new SnapshotIngestResponseDto("superseded", rejected.currentGeneration()));
        };
    }
}
