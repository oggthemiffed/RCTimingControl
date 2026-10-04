package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A walk-in entry added by hand (L9, #17). Give either an existing {@code competitorId} or a
 * {@code competitorName} for a new competitor.
 */
public record AdminCreateEntryRequest(
        @NotNull Long eventId,
        @NotNull Long eventClassId,
        Long competitorId,
        @Size(max = 255) String competitorName,
        @NotBlank @Size(max = 20) String primaryTransponder,
        @Size(max = 20) String secondaryTransponder) {}
