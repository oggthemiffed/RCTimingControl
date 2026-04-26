package dev.monkeypatch.rctiming.forwarder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Phase 5 / TIMING-08: request body for POST /api/v1/race-control/races/{raceId}/transponders/link.
 */
public record LinkTransponderRequestDto(
        @NotBlank String transponderNumber,
        @NotNull Long entryId
) {}
