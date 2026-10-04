package dev.monkeypatch.rctiming.api.racecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A scanned or typed transponder number to look up at the check-in desk. */
public record CheckInResolveRequest(@NotBlank @Size(max = 20) String transponderNumber) {}
