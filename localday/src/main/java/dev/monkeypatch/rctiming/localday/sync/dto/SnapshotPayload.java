package dev.monkeypatch.rctiming.localday.sync.dto;

import java.time.Instant;
import java.util.List;

/**
 * The result/status snapshot pushed to the cloud (R11). {@code results}/{@code standings}/
 * {@code currentHeat}/{@code nextHeat}/{@code lastCompletedHeat} are sent in full on every push;
 * {@code laps} is incremental — only laps captured since the last acknowledged snapshot (KTD8).
 *
 * <p>{@code standings} is a simple "best position per entry, per class, across the day's
 * finished races so far" summary — not a points-scoring computation. R11 is explicit that the
 * cloud always recomputes authoritative final standings itself from the synced raw laps/results,
 * treating whatever is sent here as provisional; building a full scoring engine in
 * {@code :localday} to populate this field is out of this unit's scope.
 */
public record SnapshotPayload(Instant capturedAt,
                               ScheduleSummary currentHeat,
                               ScheduleSummary nextHeat,
                               ScheduleSummary lastCompletedHeat,
                               List<RaceResultsSummary> results,
                               List<ClassStanding> standings,
                               List<LapPassingSummary> laps) {
}
