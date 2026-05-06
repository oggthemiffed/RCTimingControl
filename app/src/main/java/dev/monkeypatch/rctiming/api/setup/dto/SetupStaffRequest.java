package dev.monkeypatch.rctiming.api.setup.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record SetupStaffRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) String password,
        @NotEmpty Set<String> roles
) {}
