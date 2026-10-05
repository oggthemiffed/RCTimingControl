package dev.monkeypatch.rctiming.livefeed;

import dev.monkeypatch.rctiming.config.LoopbackHosts;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code rctiming.livefeed.*}: where the live feed is sent (#28). The feed runs only with both settings.
 *
 * @param relayUrl the relay's WebSocket address. It must be wss, since the club's key goes with the connection;
 *                 plain ws is allowed only to this machine, for testing.
 * @param token    the machine credential the relay issued for this club, sent as a bearer token
 */
@ConfigurationProperties(prefix = "rctiming.livefeed")
public record LiveFeedProperties(URI relayUrl, String token) {

    static final String URL_SETTING = "rctiming.livefeed.relay-url";
    static final String TOKEN_SETTING = "rctiming.livefeed.token";

    public LiveFeedProperties {
        if (relayUrl != null && relayUrl.toString().isBlank()) {
            relayUrl = null;
        }
        if (relayUrl != null && !"wss".equalsIgnoreCase(relayUrl.getScheme())
                && !("ws".equalsIgnoreCase(relayUrl.getScheme()) && LoopbackHosts.isLoopback(relayUrl.getHost()))) {
            throw new IllegalArgumentException(URL_SETTING + " must be a wss address, so the club's key isn't "
                    + "sent in the clear (plain ws works only to this machine), not " + relayUrl);
        }
        if (token != null && token.isBlank()) {
            token = null;
        }
    }

    /** Whether the feed can run: both the relay address and the key are set. */
    public boolean configured() {
        return relayUrl != null && token != null;
    }

    /** The settings still needed before the feed can run, empty when it can. */
    public List<String> missingSettings() {
        List<String> missing = new ArrayList<>();
        if (relayUrl == null) {
            missing.add(URL_SETTING);
        }
        if (token == null) {
            missing.add(TOKEN_SETTING);
        }
        return missing;
    }
}
