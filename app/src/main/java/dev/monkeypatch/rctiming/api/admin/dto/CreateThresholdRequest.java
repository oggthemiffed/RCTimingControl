package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateThresholdRequest(
        Long racingClassId,
        @NotNull @Min(1) Integer minLapMs,
        @Min(1) Integer maxLastLapMs) {
}
