package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCarTagCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        Integer sortOrder) {
}
