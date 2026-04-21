package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.event.EventStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionEventRequest(@NotNull EventStatus targetStatus) {}
