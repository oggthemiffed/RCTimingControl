package dev.monkeypatch.rctiming.localday.daylifecycle;

import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLifecycleCloseRequest;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLifecycleCloseResponse;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLifecycleOpenRequest;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLifecycleOpenResponse;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLoginRequest;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLoginResponse;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheRequest;
import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudPreCacheResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * Thin HTTP client wrapping the four cloud ({@code :app}) calls this unit needs: login,
 * pre-cache, lifecycle/open, lifecycle/close. Pure request/response mapping — no persistence, no
 * business logic; that lives in {@link DayLifecycleService}.
 *
 * <p>{@code :localday} has zero Java dependency on {@code :app} — every call here is plain HTTP
 * against the cloud's REST contract, never a shared Java type.
 *
 * <p>Every method funnels through {@link #execute} so connectivity failures (timeout, connection
 * refused, DNS failure — anything indicating "cloud unreachable") always come out as
 * {@link CloudUnreachableException}, while a reachable cloud's non-2xx response always comes out
 * as {@link CloudRequestException} carrying the real status code. Callers rely on this
 * distinction: only the former is a trigger to fall back to the offline path.
 */
@Component
public class PreCacheClient {

    private final RestClient cloudRestClient;

    public PreCacheClient(RestClient cloudRestClient) {
        this.cloudRestClient = cloudRestClient;
    }

    public CloudLoginResponse login(String email, String password) {
        return execute(() -> cloudRestClient.post()
                .uri("/api/v1/auth/login")
                .body(new CloudLoginRequest(email, password))
                .retrieve()
                .body(CloudLoginResponse.class));
    }

    public CloudPreCacheResponse preCache(Long eventId, String accessToken, String instanceId) {
        return execute(() -> cloudRestClient.post()
                .uri("/api/v1/localday/events/{eventId}/pre-cache", eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(new CloudPreCacheRequest(instanceId))
                .retrieve()
                .body(CloudPreCacheResponse.class));
    }

    public CloudLifecycleOpenResponse openLifecycle(Long eventId, String accessToken, String instanceId) {
        return execute(() -> cloudRestClient.post()
                .uri("/api/v1/localday/events/{eventId}/lifecycle/open", eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(new CloudLifecycleOpenRequest(instanceId))
                .retrieve()
                .body(CloudLifecycleOpenResponse.class));
    }

    /**
     * {@code accessToken} may be {@code null} — the {@code /close} local endpoint takes no
     * credentials (an official is already logged in locally by the time they close the day, not
     * necessarily re-authenticated against the cloud), so this best-effort call may go out
     * unauthenticated and get rejected by the cloud. That rejection surfaces as an ordinary
     * {@link CloudRequestException}, which {@link DayLifecycleService#close} treats the same as
     * "unreachable" — best-effort, never blocks the local close.
     */
    public CloudLifecycleCloseResponse closeLifecycle(Long eventId, String accessToken, boolean syncComplete) {
        return execute(() -> {
            RestClient.RequestBodySpec spec = cloudRestClient.post()
                    .uri("/api/v1/localday/events/{eventId}/lifecycle/close", eventId);
            if (accessToken != null) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            }
            return spec.body(new CloudLifecycleCloseRequest(syncComplete))
                    .retrieve()
                    .body(CloudLifecycleCloseResponse.class);
        });
    }

    private <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (ResourceAccessException e) {
            throw new CloudUnreachableException("Cloud unreachable: " + e.getMessage(), e);
        } catch (RestClientResponseException e) {
            throw new CloudRequestException(e.getStatusCode().value(), e.getMessage(), e);
        }
    }
}
