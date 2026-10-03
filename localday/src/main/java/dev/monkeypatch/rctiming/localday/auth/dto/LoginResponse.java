package dev.monkeypatch.rctiming.localday.auth.dto;

public record LoginResponse(String sessionToken, String officialName, Long credentialId) {
}
