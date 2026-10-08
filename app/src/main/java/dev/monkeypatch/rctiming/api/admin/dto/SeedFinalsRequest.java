package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The finals layout to seed. Who goes where comes from the class's stored qualifying results, not the request. */
public record SeedFinalsRequest(
        @NotNull Long eventClassId,
        @Min(1) int finalsCount,
        @Min(1) int carsPerFinal,
        @Min(0) int bumpCount
) {}
