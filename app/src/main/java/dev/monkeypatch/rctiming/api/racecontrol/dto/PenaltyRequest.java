package dev.monkeypatch.rctiming.api.racecontrol.dto;

import dev.monkeypatch.rctiming.domain.race.PenaltyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PenaltyRequest(
        @NotNull Long entryId,
        @NotNull PenaltyType penaltyType,
        @NotNull @Positive BigDecimal value,
        @NotBlank String reason
) {}
