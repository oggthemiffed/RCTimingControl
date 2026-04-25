package dev.monkeypatch.rctiming.api.racecontrol.dto;

import jakarta.validation.constraints.NotNull;

public record MarshalAbsenceRequest(
        @NotNull Long entryId,
        @NotNull Long eventId,
        String notes
) {}
