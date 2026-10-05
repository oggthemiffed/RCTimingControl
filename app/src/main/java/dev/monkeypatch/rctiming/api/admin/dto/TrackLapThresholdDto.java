package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.track.TrackLapThreshold;

public record TrackLapThresholdDto(
        Long id,
        Long racingClassId,
        String racingClassName,
        int minLapMs,
        Integer maxLastLapMs) {

    public static TrackLapThresholdDto from(TrackLapThreshold t) {
        return new TrackLapThresholdDto(
                t.getId(),
                t.getRacingClassId(),
                t.getRacingClassName(),
                t.getMinLapMs(),
                t.getMaxLastLapMs());
    }
}
