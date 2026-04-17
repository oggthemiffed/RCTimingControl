package dev.monkeypatch.rctiming.api.racer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCarRequest(
        @NotBlank @Size(max = 100) String name,
        Long primaryClassId,
        @Size(max = 2000) String notes) {
}
