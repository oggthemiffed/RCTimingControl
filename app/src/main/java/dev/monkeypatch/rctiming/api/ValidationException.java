package dev.monkeypatch.rctiming.api;

import java.util.Map;

/** Thrown when business-layer validation fails on specific named fields. */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String field, String message) {
        super(message);
        this.fieldErrors = Map.of(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
