package dev.monkeypatch.rctiming.domain.localday;

/**
 * Outcome of a {@link SnapshotIngestService#ingest} attempt. Sealed so
 * {@code SnapshotIngestController} can exhaustively map each variant to the fixed KTD4 response
 * contract (200 accepted / 409 superseded) with a switch expression — mirrors
 * {@code :localday}'s own {@code DayCloseOutcome} pattern.
 */
public sealed interface SnapshotIngestOutcome {

    /** {@code generation} is the stored value after this call (unchanged if this was a replay
     * or an equal-generation push). */
    record Accepted(long generation) implements SnapshotIngestOutcome {
    }

    /** KTD4: this instance's generation is strictly lower than the stored value — a replacement
     * instance has since claimed a higher generation. {@code currentGeneration} is the stored
     * value that beat it. */
    record Rejected(long currentGeneration) implements SnapshotIngestOutcome {
    }
}
