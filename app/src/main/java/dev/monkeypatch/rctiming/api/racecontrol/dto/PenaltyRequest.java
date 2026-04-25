package dev.monkeypatch.rctiming.api.racecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PenaltyRequest(
        @NotNull Long entryId,
        @NotBlank String penaltyType,
        @NotNull @Positive BigDecimal value,
        @NotBlank String reason
) {}
