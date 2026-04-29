package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SeedFinalsRequest(
        @NotNull Long eventClassId,
        @Min(1) int finalsCount,
        @Min(1) int carsPerFinal,
        @Min(1) int bumpCount,
        @NotNull List<QualifyingResultDto> qualifyingResults
) {
    public record QualifyingResultDto(
            @NotNull Long entryId,
            long bestLapMs,
            int lapsCompleted
    ) {}
}
