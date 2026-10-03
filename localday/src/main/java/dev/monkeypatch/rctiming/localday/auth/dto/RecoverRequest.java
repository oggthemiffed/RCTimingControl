package dev.monkeypatch.rctiming.localday.auth.dto;

public record RecoverRequest(Long recoveryCredentialId, String recoverySecret, Long targetCredentialId) {
}
