package dev.monkeypatch.rctiming.api.racer.dto;

import jakarta.validation.constraints.NotNull;

public record SubmitEntryRequest(
        @NotNull Long eventId,
        @NotNull Long eventClassId,
        @NotNull Long carId,
        @NotNull Long transponderId) {
}
