package dev.monkeypatch.rctiming.api.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * How the entry import endpoints answer. An import that something blocks (unmapped classes, invalid rows)
 * saves nothing and returns 422 with the preview, so the page can show what to fix. Everything else is 200.
 */
final class ImportResponses {

    private ImportResponses() {
    }

    static <T> ResponseEntity<T> of(T result, boolean blocked) {
        return ResponseEntity.status(blocked ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.OK).body(result);
    }
}
