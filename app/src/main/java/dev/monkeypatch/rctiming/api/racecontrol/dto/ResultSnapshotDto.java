package dev.monkeypatch.rctiming.api.racecontrol.dto;

import java.time.Instant;
import java.util.List;

public record ResultSnapshotDto(
        long raceId,
        String raceLabel,
        Instant finishedAt,
        List<ResultRow> positions,
        List<PositionAtLap> lapHistory,
        ClubBrandingDto clubBranding
) {
    public record ResultRow(
            int position,
            long entryId,
            Long competitorId,        // L5: the driver; null in snapshots written before competitors existed
            String driverName,        // competitor display name at the time the race finished
            String carNumber,
            int lapsCompleted,
            long totalTimeMs,
            Long bestLapMs,
            Long gapToLeaderMs
    ) {}

    public record PositionAtLap(int lapNumber, long entryId, int position, Long lapTimeMs) {}

    public record ClubBrandingDto(String clubName, String logoUrl) {}
}
