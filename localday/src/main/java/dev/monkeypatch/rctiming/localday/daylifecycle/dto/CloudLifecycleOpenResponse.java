package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

import java.time.Instant;

/** Inbound body from the cloud's {@code POST /api/v1/localday/events/{eventId}/lifecycle/open}. */
public record CloudLifecycleOpenResponse(Long eventId, boolean locked, Instant lockedAt,
                                          Instant unlockedAt, Long generation) {}
