package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUpdateTransponderRequest(
        @NotNull Long transponderId,
        @Size(max = 500) String reason) {
}
