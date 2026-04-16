package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.track.LoopType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDecoderLoopRequest(
        @NotBlank String loopId,
        @NotBlank String displayName,
        @NotNull LoopType loopType,
        boolean isScoringLoop) {
}
