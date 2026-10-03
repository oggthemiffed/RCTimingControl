package dev.monkeypatch.rctiming.localday.sync;

import dev.monkeypatch.rctiming.localday.daylifecycle.CloudRequestException;
import dev.monkeypatch.rctiming.localday.daylifecycle.CloudUnreachableException;
import dev.monkeypatch.rctiming.localday.sync.dto.SnapshotPayload;
import dev.monkeypatch.rctiming.localday.sync.dto.SnapshotRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit test for {@link SnapshotSyncClient} in isolation (no Spring context) — mirrors
 * {@code PreCacheClientTest}'s style and asserts the same connectivity-vs-rejection exception
 * split it establishes.
 */
class SnapshotSyncClientTest {

    private static SnapshotRequest sampleRequest() {
        SnapshotPayload payload = new SnapshotPayload(Instant.parse("2026-08-24T12:00:00Z"),
                null, null, null, List.of(), List.of(), List.of());
        return new SnapshotRequest("inst-1", 3L, "snap-1", payload);
    }

    @Test
    void pushSnapshot_success_sendsInstanceSecretHeaderNotBearer() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://cloud.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SnapshotSyncClient client = new SnapshotSyncClient(builder.build());

        server.expect(requestTo("http://cloud.test/api/v1/localday/events/7/snapshots"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SnapshotSyncClient.INSTANCE_SECRET_HEADER, "top-secret"))
                .andRespond(withSuccess());

        client.pushSnapshot(7L, "top-secret", sampleRequest());

        server.verify();
    }

    @Test
    void pushSnapshot_reachableButRejected_throwsCloudRequestExceptionWithStatus() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://cloud.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SnapshotSyncClient client = new SnapshotSyncClient(builder.build());

        server.expect(requestTo("http://cloud.test/api/v1/localday/events/7/snapshots"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.pushSnapshot(7L, "top-secret", sampleRequest()))
                .isInstanceOf(CloudRequestException.class)
                .satisfies(e -> assertThat(((CloudRequestException) e).getStatusCode()).isEqualTo(409));
    }

    @Test
    void pushSnapshot_unreachableHost_throwsCloudUnreachableException() throws Exception {
        int deadPort = findFreePort();
        RestClient restClient = RestClient.builder().baseUrl("http://127.0.0.1:" + deadPort).build();
        SnapshotSyncClient client = new SnapshotSyncClient(restClient);

        assertThatThrownBy(() -> client.pushSnapshot(7L, "top-secret", sampleRequest()))
                .isInstanceOf(CloudUnreachableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    private static int findFreePort() throws Exception {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
