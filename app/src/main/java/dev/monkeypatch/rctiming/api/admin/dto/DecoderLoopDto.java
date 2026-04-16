package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.track.DecoderLoop;

public record DecoderLoopDto(
        Long id,
        String loopId,
        String displayName,
        String loopType,
        boolean isScoringLoop) {

    public static DecoderLoopDto from(DecoderLoop loop) {
        return new DecoderLoopDto(
                loop.getId(),
                loop.getLoopId(),
                loop.getDisplayName(),
                loop.getLoopType() != null ? loop.getLoopType().name() : null,
                loop.isScoringLoop());
    }
}
