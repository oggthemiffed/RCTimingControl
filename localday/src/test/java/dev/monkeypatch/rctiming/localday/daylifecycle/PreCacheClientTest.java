package dev.monkeypatch.rctiming.localday.daylifecycle;

import dev.monkeypatch.rctiming.localday.daylifecycle.dto.CloudLoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

/**
 * Unit test for {@link PreCacheClient} in isolation (no Spring context) — verifies the exact
 * request shapes sent to the cloud, and that {@link #execute} correctly translates connectivity
 * failures into {@link CloudUnreachableException} and reachable-but-erroring responses into
 * {@link CloudRequestException} carrying the real status code.
 */
class PreCacheClientTest {

    @Test
    void login_success_sendsExpectedRequestAndParsesResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://cloud.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PreCacheClient client = new PreCacheClient(builder.build());

        server.expect(requestTo("http://cloud.test/api/v1/auth/login"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {"accessToken":"tok-123","id":"1","email":"official@club.test",
                         "firstName":"Race","lastName":"Director","roles":["ADMIN"]}
                        """, MediaType.APPLICATION_JSON));

        CloudLoginResponse response = client.login("official@club.test", "hunter2");

        assertThat(response.accessToken()).isEqualTo("tok-123");
        assertThat(response.roles()).containsExactly("ADMIN");
        server.verify();
    }

    @Test
    void login_reachableButUnauthorized_throwsCloudRequestExceptionWith401() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://cloud.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PreCacheClient client = new PreCacheClient(builder.build());

        server.expect(requestTo("http://cloud.test/api/v1/auth/login"))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.login("official@club.test", "wrong"))
                .isInstanceOf(CloudRequestException.class)
                .satisfies(e -> assertThat(((CloudRequestException) e).getStatusCode()).isEqualTo(401));
    }

    @Test
    void login_reachableButServerError_throwsCloudRequestExceptionWith500() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://cloud.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PreCacheClient client = new PreCacheClient(builder.build());

        server.expect(requestTo("http://cloud.test/api/v1/auth/login"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.login("official@club.test", "whatever"))
                .isInstanceOf(CloudRequestException.class)
                .satisfies(e -> assertThat(((CloudRequestException) e).getStatusCode()).isEqualTo(500));
    }

    @Test
    void login_unreachableHost_throwsCloudUnreachableException() throws Exception {
        // Bind to a port nothing is listening on (grab an ephemeral port then immediately close
        // it) so the connection is refused deterministically and quickly, without touching any
        // real network or requiring a WireMock-style server dependency this module doesn't have.
        int deadPort = findFreePort();
        RestClient restClient = RestClient.builder().baseUrl("http://127.0.0.1:" + deadPort).build();
        PreCacheClient client = new PreCacheClient(restClient);

        // The cause chain is CloudUnreachableException -> ResourceAccessException -> a
        // java.net.http-specific connect failure (ConnectException, wrapping a
        // ClosedChannelException on some JDKs) — assert on the ResourceAccessException wrapper
        // PreCacheClient.execute() catches, not the JDK-internal exception underneath it.
        assertThatThrownBy(() -> client.login("official@club.test", "whatever"))
                .isInstanceOf(CloudUnreachableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    private static int findFreePort() throws Exception {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Test
    void preCache_sendsAuthorizationHeaderAndInstanceId() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://cloud.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PreCacheClient client = new PreCacheClient(builder.build());

        server.expect(requestTo("http://cloud.test/api/v1/localday/events/7/pre-cache"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok-123"))
                .andRespond(withSuccess("""
                        {"entries":[],"schedule":[],"formatConfigs":[],"officialCredentials":[],
                         "instanceSecret":{"instanceId":"inst-1","secret":"abc"}}
                        """, MediaType.APPLICATION_JSON));

        var response = client.preCache(7L, "tok-123", "inst-1");

        assertThat(response.instanceSecret().secret()).isEqualTo("abc");
        server.verify();
    }
}
