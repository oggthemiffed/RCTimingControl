package dev.monkeypatch.rctiming.domain.club;

/**
 * Decoder connection settings held on the club profile (V25). Any field may be null until the
 * setup wizard has been completed.
 */
public record DecoderSettings(String host, Integer port, String protocol) {

    /** True when host, port and protocol are all set. */
    public boolean isConfigured() {
        return host != null && !host.isBlank() && port != null && protocol != null;
    }
}
