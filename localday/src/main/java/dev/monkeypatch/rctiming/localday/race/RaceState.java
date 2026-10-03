package dev.monkeypatch.rctiming.localday.race;

/**
 * Lifecycle states for a {@link dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry}
 * (the local analog of the cloud's {@code Race} entity). Valid transitions are enforced by
 * {@link RaceStateMachineService}.
 */
public enum RaceState {
    PENDING,
    GRID,
    RUNNING,
    STOPPED,
    FINISHED
}
