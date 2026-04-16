package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRacingClassRequest(
        @NotBlank @Size(max = 255) String name,
        String description) {
}
