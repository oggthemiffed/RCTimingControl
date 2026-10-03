package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

/** Response body for {@code POST /api/v1/day-lifecycle/close}. {@code status} is "closed" or "pending". */
public record DayCloseResponseDto(String status, int pendingSyncCount) {}
