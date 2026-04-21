package dev.monkeypatch.rctiming.domain.event;

/**
 * Thrown when an Event state transition is not permitted by the state machine (EVENT-05).
 * Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
