package dev.monkeypatch.rctiming.api.racecontrol.dto;

import jakarta.validation.constraints.NotNull;

public record SkipToRaceRequest(
        @NotNull Long targetRaceId
) {
}
