package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

/** Outbound body for the cloud's {@code POST /api/v1/auth/login}. */
public record CloudLoginRequest(String email, String password) {}
