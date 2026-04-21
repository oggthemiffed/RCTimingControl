package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.championship.ChampionshipPointsScaleEntry;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PointsScaleEntryDto(
        @NotNull @Min(1) Integer position,
        @NotNull @Min(0) Integer points
) {
    public static PointsScaleEntryDto from(ChampionshipPointsScaleEntry e) {
        return new PointsScaleEntryDto(e.getPosition(), e.getPoints());
    }
}
