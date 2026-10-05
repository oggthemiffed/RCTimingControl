package dev.monkeypatch.rctiming.timing.dto;

import java.util.List;

/**
 * The live feed's state for race control (#28).
 *
 * @param state           NOT_SET_UP, IDLE, CONNECTING, CONNECTED or RECONNECTING
 * @param relayHost       the relay's host name, or null when not set up
 * @param missingSettings the settings still needed before the feed can run
 */
public record LiveFeedStatusDto(String state, String relayHost, List<String> missingSettings) {
}
