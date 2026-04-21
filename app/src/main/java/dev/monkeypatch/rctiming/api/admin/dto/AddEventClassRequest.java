package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AddEventClassRequest(
        @NotNull Long racingClassId,
        @NotNull Long templateId
) {}
