package dev.monkeypatch.rctiming.api.racecontrol.dto;

import jakarta.validation.constraints.NotNull;

public record UnknownTransponderLinkRequest(
        @NotNull String transponderNumber,
        Long linkedEntryId  // nullable — may register the unknown without linking
) {
}
