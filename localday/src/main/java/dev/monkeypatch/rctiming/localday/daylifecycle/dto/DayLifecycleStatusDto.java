package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

import dev.monkeypatch.rctiming.localday.daylifecycle.DayLifecycleState;

import java.time.Instant;

/**
 * Response body shared by {@code POST /api/v1/day-lifecycle/pre-cache}, {@code .../open}, and
 * {@code GET /api/v1/day-lifecycle/status} — all three describe "the current state" after
 * whatever action (or none) just happened.
 */
public record DayLifecycleStatusDto(String status, Long eventId, Long generation,
                                     boolean splitBrainWarning, int pendingSyncCount,
                                     Instant lastPreCachedAt, boolean superseded) {

    public static DayLifecycleStatusDto from(DayLifecycleState state) {
        return new DayLifecycleStatusDto(
                state.getStatus().name(),
                state.getCloudEventId(),
                state.getGeneration(),
                state.isSplitBrainWarning(),
                state.getPendingSyncCount(),
                state.getLastPreCachedAt(),
                state.isSuperseded());
    }
}
