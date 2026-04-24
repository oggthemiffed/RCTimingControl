package dev.monkeypatch.rctiming.api.racecontrol.dto;

import jakarta.validation.constraints.NotNull;

public record MarshalAdjustmentRequest(
        @NotNull Long entryId,
        @NotNull String transponderNumber,
        @NotNull Integer lapDelta  // +1 or -1
) {
}
