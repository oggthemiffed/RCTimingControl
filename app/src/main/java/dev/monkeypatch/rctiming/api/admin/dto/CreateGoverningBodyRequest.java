package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGoverningBodyRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 255) String displayName,
        boolean membershipRequired) {
}
