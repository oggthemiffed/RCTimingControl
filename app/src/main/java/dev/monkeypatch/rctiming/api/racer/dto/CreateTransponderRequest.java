package dev.monkeypatch.rctiming.api.racer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTransponderRequest(
        @NotBlank @Size(max = 20) String transponderNumber,
        @Size(max = 100) String label
) {}
