package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

/** Outbound body for the cloud's {@code POST /api/v1/localday/events/{eventId}/lifecycle/close}. */
public record CloudLifecycleCloseRequest(boolean syncComplete) {}
