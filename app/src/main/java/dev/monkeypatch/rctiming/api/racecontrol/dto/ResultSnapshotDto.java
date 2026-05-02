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
            Long gapToLeaderMs,
            List<CarTagDto> carTags  // null when show_car_tags_in_results=false; empty list when enabled but no tags
    ) {}

    public record PositionAtLap(int lapNumber, long entryId, int position, Long lapTimeMs) {}

    public record ClubBrandingDto(String clubName, String logoUrl) {}

    /** One car tag key/value pair, e.g. "Motor Spec" / "Brushless 13.5T". */
    public record CarTagDto(String key, String value) {}
}
