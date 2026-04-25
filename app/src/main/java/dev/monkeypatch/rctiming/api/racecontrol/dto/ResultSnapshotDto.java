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
            String driverName,
            String carNumber,
            int lapsCompleted,
            long totalTimeMs,
            Long bestLapMs,
            Long gapToLeaderMs
    ) {}

    public record PositionAtLap(int lapNumber, long entryId, int position) {}

    public record ClubBrandingDto(String clubName, String logoUrl) {}
}
