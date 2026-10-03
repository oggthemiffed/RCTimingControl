package dev.monkeypatch.rctiming.localday.auth.dto;

public record LockedResponse(String error, long retryAfterSeconds) {
}
