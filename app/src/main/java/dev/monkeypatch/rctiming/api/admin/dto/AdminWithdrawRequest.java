package dev.monkeypatch.rctiming.api.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminWithdrawRequest(@NotBlank String reason) {}
