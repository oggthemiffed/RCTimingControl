package dev.monkeypatch.rctiming.forwarder.timing;

/**
 * A {@link ParsedPassing} whose {@code timeSinceStart} has been converted to an absolute
 * epoch-anchored UTC timestamp by {@link EpochAnchor} (D-07).
 *
 * <p>This is the value forwarded to the gRPC cloud service (wired in Plan 04).
 */
public record EpochCorrectedPassing(
    String transponderNumber,
    long   rtcTimeMicros,
    int    seqNum,
    int    decoderId,
    int    signalStrength,
    int    hitCount
) {}
