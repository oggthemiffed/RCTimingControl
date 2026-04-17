package dev.monkeypatch.rctiming.api.racer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertMembershipRequest(
        @NotBlank @Size(max = 20) String governingBodyCode,
        @NotBlank @Size(max = 50) String membershipNumber
) {}
