package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MembershipOverrideRequest(
        @NotBlank @Size(max = 500) String reason) {
}
