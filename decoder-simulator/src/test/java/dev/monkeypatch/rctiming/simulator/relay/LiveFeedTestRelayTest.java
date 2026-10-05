package dev.monkeypatch.rctiming.simulator.relay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiveFeedTestRelayTest {

    private final HttpClient client = HttpClient.newHttpClient();
    private LiveFeedTestRelay relay;

    @AfterEach
    void stop() {
        if (relay != null) {
            relay.close();
        }
    }

    @Test
    void passesPublishedMessagesToViewersStartingWithTheLatestPerRace() throws Exception {
        relay = new LiveFeedTestRelay(0, "secret");
        int port = relay.start();
        WebSocket publisher = client.newWebSocketBuilder().header("Authorization", "Bearer secret")
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/publish"), new WebSocket.Listener() { })
                .get(5, TimeUnit.SECONDS);
        publisher.sendText("{\"race\":{\"rctc_race_id\":7},\"sequence\":1}", true).get(5, TimeUnit.SECONDS);
        publisher.sendText("{\"race\":{\"rctc_race_id\":7},\"sequence\":2}", true).get(5, TimeUnit.SECONDS);
        await(() -> relay.receivedCount() == 2);

        List<String> seen = new CopyOnWriteArrayList<>();
        client.newWebSocketBuilder()
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/watch"), collector(seen))
                .get(5, TimeUnit.SECONDS);
        await(() -> seen.size() == 1);
        assertThat(seen.get(0)).contains("\"sequence\":2");

        publisher.sendText("{\"race\":{\"rctc_race_id\":8},\"sequence\":3}", true).get(5, TimeUnit.SECONDS);
        await(() -> seen.size() == 2);
        assertThat(seen.get(1)).contains("\"rctc_race_id\":8");
        assertThat(relay.publisherCount()).isEqualTo(1);
    }

    @Test
    void refusesAPublisherWithTheWrongKey() throws Exception {
        relay = new LiveFeedTestRelay(0, "secret");
        int port = relay.start();

        assertThatThrownBy(() -> client.newWebSocketBuilder().header("Authorization", "Bearer wrong")
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/publish"), new WebSocket.Listener() { })
                .get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(WebSocketHandshakeException.class);
    }

    @Test
    void servesTheViewerPage() throws Exception {
        relay = new LiveFeedTestRelay(0, null);
        int port = relay.start();

        HttpResponse<String> page = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("Live feed viewer").contains("/watch");
    }

    private static WebSocket.Listener collector(List<String> seen) {
        return new WebSocket.Listener() {
            private final StringBuilder partial = new StringBuilder();

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                partial.append(data);
                if (last) {
                    seen.add(partial.toString());
                    partial.setLength(0);
                }
                webSocket.request(1);
                return null;
            }
        };
    }

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Timed out");
            }
            Thread.sleep(20);
        }
    }
}
