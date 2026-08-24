package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

/**
 * Request body for {@code POST /api/v1/day-lifecycle/open}. {@code email}/{@code password} are
 * nullable/blank on purpose — an absent pair means "attempt offline" (reuse whatever was already
 * pre-cached), not a validation error.
 */
public record DayOpenRequest(Long eventId, String email, String password) {}
