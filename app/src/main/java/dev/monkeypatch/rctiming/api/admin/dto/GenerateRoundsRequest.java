package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GenerateRoundsRequest(
        @Min(0) @Max(10) int practiceRoundsCount,
        @Min(1) @Max(10) int qualifyingRoundsCount,
        @Min(1) @Max(64) int maxCarsPerHeat,
        @NotNull List<ClassFinalsConfigDto> classFinalsConfigs
) {
    public record ClassFinalsConfigDto(
            @NotNull Long eventClassId,
            Integer finalsCount,
            Integer carsPerFinal,
            Integer bumpCount
    ) {}
}
