package dev.monkeypatch.rctiming.api.racer.dto;

import jakarta.validation.constraints.Size;

public record UpdateRacerProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 30) String phoneNumber,
        @Size(max = 100) String emergencyContactName,
        @Size(max = 30) String emergencyContactPhone,
        @Size(max = 255) String phoneticName
) {}
