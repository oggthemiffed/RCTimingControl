package dev.monkeypatch.rctiming.localday.auth.dto;

public record LoginRequest(Long credentialId, String secret) {
}
