package dev.monkeypatch.rctiming.api.racer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SetCarTagRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 2000) String value) {
}
