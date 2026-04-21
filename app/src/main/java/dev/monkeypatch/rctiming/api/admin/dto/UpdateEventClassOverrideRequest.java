package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateEventClassOverrideRequest(
        @NotNull Map<String, Object> override  // null map clears override; pass empty map to clear, non-empty to set
) {}
