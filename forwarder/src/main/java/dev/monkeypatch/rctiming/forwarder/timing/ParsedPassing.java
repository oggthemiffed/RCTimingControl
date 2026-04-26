package dev.monkeypatch.rctiming.forwarder.timing;

/**
 * Immutable record of a single PASSING event parsed from the RC-4 text protocol.
 * The {@code timeSinceStartSeconds} is relative to decoder power-on — use {@link EpochAnchor}
 * to convert to wall-clock UTC microseconds.
 */
public record ParsedPassing(
    String transponderNumber,
    double timeSinceStartSeconds,
    int seqNum,
    int decoderId,
    int signalStrength,
    int hitCount
) {}
