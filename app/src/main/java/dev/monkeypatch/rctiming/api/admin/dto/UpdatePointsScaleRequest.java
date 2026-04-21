package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePointsScaleRequest(
        @NotNull @Size(min = 1) List<@Valid PointsScaleEntryDto> entries
) {}
