package dev.monkeypatch.rctiming.livefeed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiveFeedPropertiesTest {

    @Test
    void runsOnlyWithBothTheRelayAndTheKey() {
        assertThat(new LiveFeedProperties(URI.create("wss://relay.example/publish"), "key").configured()).isTrue();

        var noKey = new LiveFeedProperties(URI.create("wss://relay.example/publish"), " ");
        assertThat(noKey.configured()).isFalse();
        assertThat(noKey.missingSettings()).containsExactly("rctiming.livefeed.token");

        var nothing = new LiveFeedProperties(URI.create(""), null);
        assertThat(nothing.configured()).isFalse();
        assertThat(nothing.missingSettings()).containsExactly("rctiming.livefeed.relay-url", "rctiming.livefeed.token");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ws://relay.example/publish", "https://relay.example/publish", "ws://192.168.1.20:8099/publish"})
    void refusesAnythingButWssToAnotherMachine(String url) {
        assertThatThrownBy(() -> new LiveFeedProperties(URI.create(url), "key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("wss");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ws://localhost:8099/publish", "ws://127.0.0.1:8099/publish", "ws://[::1]:8099/publish"})
    void allowsPlainWsToThisMachineForTesting(String url) {
        assertThat(new LiveFeedProperties(URI.create(url), "key").configured()).isTrue();
    }
}
