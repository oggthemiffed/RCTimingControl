package dev.monkeypatch.rctiming.timing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Phase 5 / TIMING-08: request body for POST /api/v1/race-control/race/{raceId}/transponders/link.
 */
public record LinkTransponderRequestDto(
        @NotBlank @Size(max = 50) String transponderNumber,
        @NotNull Long entryId
) {}
