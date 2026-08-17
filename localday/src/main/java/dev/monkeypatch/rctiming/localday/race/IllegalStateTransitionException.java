package dev.monkeypatch.rctiming.localday.race;

/**
 * Thrown when a {@link dev.monkeypatch.rctiming.localday.domain.CachedScheduleEntry} state
 * transition is not permitted by {@link RaceStateMachineService}'s state machine.
 */
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
