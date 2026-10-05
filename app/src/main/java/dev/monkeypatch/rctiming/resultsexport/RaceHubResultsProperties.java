package dev.monkeypatch.rctiming.resultsexport;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code rctiming.racehub.*}: where results exports are sent (#27). Sending needs both settings; with either
 * missing, exports wait in the outbox.
 *
 * @param resultsUrl the RaceHub address results exports are POSTed to. It must be https, since the club's
 *                   key goes with every request; plain http is allowed only to this machine, for testing.
 * @param token      the machine credential RaceHub issued for this club, sent as a bearer token
 */
@ConfigurationProperties(prefix = "rctiming.racehub")
public record RaceHubResultsProperties(URI resultsUrl, String token) {

    static final String URL_SETTING = "rctiming.racehub.results-url";
    static final String TOKEN_SETTING = "rctiming.racehub.token";

    public RaceHubResultsProperties {
        if (resultsUrl != null && resultsUrl.toString().isBlank()) {
            resultsUrl = null;
        }
        if (resultsUrl != null && !"https".equalsIgnoreCase(resultsUrl.getScheme())
                && !("http".equalsIgnoreCase(resultsUrl.getScheme()) && isLoopback(resultsUrl.getHost()))) {
            throw new IllegalArgumentException(URL_SETTING + " must be an https address, so the club's key "
                    + "isn't sent in the clear (plain http works only to this machine), not " + resultsUrl);
        }
        if (token != null && token.isBlank()) {
            token = null;
        }
    }

    /** Whether exports are sent: both the address and the key are set. */
    public boolean sendingEnabled() {
        return resultsUrl != null && token != null;
    }

    /** The settings still needed before anything is sent, empty when sending is on. */
    public List<String> missingSettings() {
        List<String> missing = new ArrayList<>();
        if (resultsUrl == null) {
            missing.add(URL_SETTING);
        }
        if (token == null) {
            missing.add(TOKEN_SETTING);
        }
        return missing;
    }

    private static boolean isLoopback(String host) {
        if (host == null) {
            return false;
        }
        if ("localhost".equalsIgnoreCase(host)) {
            return true;
        }
        String literal = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        // Only IP literals are checked, so no name lookup happens while the settings load
        boolean ipLiteral = literal.matches("\\d{1,3}(\\.\\d{1,3}){3}") || literal.contains(":");
        if (!ipLiteral) {
            return false;
        }
        try {
            return InetAddress.getByName(literal).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
