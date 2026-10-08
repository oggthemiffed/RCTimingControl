package dev.monkeypatch.rctiming.domain;

/**
 * The request is fine but the thing it acts on is in the wrong state for it, such as starting a practice
 * session that is already running or generating a run order that already exists. The API answers 409.
 *
 * <p>It is an {@link IllegalStateException} so code that already catches that keeps working; use it for a
 * conflict the caller can see and fix, and a plain {@code IllegalStateException} for a fault in the app.
 */
public class StateConflictException extends IllegalStateException {

    public StateConflictException(String message) {
        super(message);
    }
}
