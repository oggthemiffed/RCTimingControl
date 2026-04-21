package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CombineClassesRequest(
        @NotNull @Size(min = 2) List<Long> eventClassIds
) {}
