package dev.monkeypatch.rctiming.domain;

/** Something asked for by id doesn't exist. The API answers 404. */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
