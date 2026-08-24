package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

/** Request body for {@code POST /api/v1/day-lifecycle/pre-cache}. */
public record PreCacheRequest(Long eventId, String email, String password) {}
