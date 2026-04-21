package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExclusionRequest(
        @NotNull Long driverId,
        @NotNull Long eventId,
        @NotBlank String reason     // CHAMP-02 requires reason; CHAMP-09 persists it for audit
) {}
