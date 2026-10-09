package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.service.dto.RoundGenerationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GenerateRoundsRequest(
        @Min(0) @Max(10) int practiceRoundsCount,
        @Min(1) @Max(10) int qualifyingRoundsCount,
        @Min(1) @Max(64) int maxCarsPerHeat,
        @NotNull List<@NotNull @Valid ClassFinalsConfigDto> classFinalsConfigs
) {
    /** A class's finals layout; a null count keeps the class's own setting. */
    public record ClassFinalsConfigDto(
            @NotNull Long eventClassId,
            @Min(1) Integer finalsCount,
            @Min(1) @Max(64) Integer carsPerFinal,
            @Min(0) Integer bumpCount
    ) {}

    public RoundGenerationRequest toServiceRequest(long eventId) {
        return new RoundGenerationRequest(eventId, practiceRoundsCount, qualifyingRoundsCount, maxCarsPerHeat,
                classFinalsConfigs.stream()
                        .map(c -> new RoundGenerationRequest.ClassFinalsConfig(
                                c.eventClassId(), c.finalsCount(), c.carsPerFinal(), c.bumpCount()))
                        .toList());
    }
}
