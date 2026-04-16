package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.track.Track;

import java.util.List;

public record TrackDto(
        Long id,
        String name,
        String venueNotes,
        Double trackLength,
        List<DecoderLoopDto> decoderLoops,
        List<TrackLapThresholdDto> lapThresholds) {

    public static TrackDto from(Track t) {
        return new TrackDto(
                t.getId(),
                t.getName(),
                t.getVenueNotes(),
                t.getTrackLength(),
                t.getDecoderLoops().stream().map(DecoderLoopDto::from).toList(),
                t.getLapThresholds().stream().map(TrackLapThresholdDto::from).toList());
    }
}
