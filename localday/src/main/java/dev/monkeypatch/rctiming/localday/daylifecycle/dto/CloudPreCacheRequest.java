package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

/** Outbound body for the cloud's {@code POST /api/v1/localday/events/{eventId}/pre-cache}. */
public record CloudPreCacheRequest(String instanceId) {}
