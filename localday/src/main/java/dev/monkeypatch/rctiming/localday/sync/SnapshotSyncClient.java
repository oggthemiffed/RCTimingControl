package dev.monkeypatch.rctiming.localday.sync;

import dev.monkeypatch.rctiming.localday.daylifecycle.CloudRequestException;
import dev.monkeypatch.rctiming.localday.daylifecycle.CloudUnreachableException;
import dev.monkeypatch.rctiming.localday.sync.dto.SnapshotRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Thin HTTP client for U11's periodic snapshot push to the cloud's snapshot-ingest endpoint
 * (U14 — not yet built; pushes against it fail as an ordinary {@link CloudRequestException}
 * until that unit lands, which {@link SnapshotPushService} already treats as a retry-worthy
 * failure like any other).
 *
 * <p>Authenticates with the per-day-instance secret minted at pre-cache time (KTD9) via
 * {@link #INSTANCE_SECRET_HEADER} — distinct from officials' local session auth and from the
 * cloud-login JWT {@link dev.monkeypatch.rctiming.localday.daylifecycle.PreCacheClient} sends as
 * a Bearer token.
 *
 * <p>Reuses {@link CloudUnreachableException}/{@link CloudRequestException} from the
 * {@code daylifecycle} package (same connectivity-vs-rejection split {@code PreCacheClient}
 * already established) rather than duplicating it a second time in this package.
 */
@Component
public class SnapshotSyncClient {

    public static final String INSTANCE_SECRET_HEADER = "X-Localday-Instance-Secret";

    private final RestClient cloudRestClient;

    public SnapshotSyncClient(RestClient cloudRestClient) {
        this.cloudRestClient = cloudRestClient;
    }

    public void pushSnapshot(Long eventId, String instanceSecret, SnapshotRequest request) {
        try {
            cloudRestClient.post()
                    .uri("/api/v1/localday/events/{eventId}/snapshots", eventId)
                    .header(INSTANCE_SECRET_HEADER, instanceSecret)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new CloudUnreachableException("Cloud unreachable: " + e.getMessage(), e);
        } catch (RestClientResponseException e) {
            throw new CloudRequestException(e.getStatusCode().value(), e.getMessage(), e);
        }
    }
}
