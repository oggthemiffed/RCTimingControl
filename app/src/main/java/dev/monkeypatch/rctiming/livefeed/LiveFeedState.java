package dev.monkeypatch.rctiming.livefeed;

/** The live feed's connection to the relay, as race control shows it (#28). */
public enum LiveFeedState {
    /** The relay address or key isn't set, so the feed can't run. */
    NOT_SET_UP,
    /** Set up, but no race in an event with the feed on is running, so nothing is connected. */
    IDLE,
    /** Opening the connection to the relay. */
    CONNECTING,
    /** Connected and sending. */
    CONNECTED,
    /** The relay can't be reached; trying again shortly. Timing carries on regardless. */
    RECONNECTING
}
