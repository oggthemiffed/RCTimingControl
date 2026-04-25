package dev.monkeypatch.rctiming.api.racecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IncidentReportRequest(
        @NotNull Long entryId,
        @NotBlank String incidentType,
        @NotBlank String description
) {}
