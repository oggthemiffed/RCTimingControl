package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

/** Inbound body from the cloud's {@code POST /api/v1/localday/events/{eventId}/lifecycle/close}. */
public record CloudLifecycleCloseResponse(String status) {}
