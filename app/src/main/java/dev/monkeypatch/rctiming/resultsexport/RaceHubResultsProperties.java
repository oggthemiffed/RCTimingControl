package dev.monkeypatch.rctiming.resultsexport;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * {@code rctiming.racehub.*}: where results exports are sent (#27).
 *
 * @param resultsUrl the RaceHub address results exports are POSTed to; unset, exports wait in the outbox and
 *                   nothing is sent
 * @param token      the machine credential RaceHub issued for this club, sent as a bearer token; optional
 */
@ConfigurationProperties(prefix = "rctiming.racehub")
public record RaceHubResultsProperties(URI resultsUrl, String token) {

    public RaceHubResultsProperties {
        if (resultsUrl != null && resultsUrl.toString().isBlank()) {
            resultsUrl = null;
        }
        if (resultsUrl != null && !"https".equalsIgnoreCase(resultsUrl.getScheme())
                && !"http".equalsIgnoreCase(resultsUrl.getScheme())) {
            throw new IllegalArgumentException("rctiming.racehub.results-url must be an http or https address, not "
                    + resultsUrl);
        }
        if (token != null && token.isBlank()) {
            token = null;
        }
    }

    public boolean sendingEnabled() {
        return resultsUrl != null;
    }
}
