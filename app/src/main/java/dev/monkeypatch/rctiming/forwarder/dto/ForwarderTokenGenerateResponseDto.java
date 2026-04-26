package dev.monkeypatch.rctiming.forwarder.dto;

import java.time.Instant;

public record ForwarderTokenGenerateResponseDto(String token, String status, Instant generatedAt) {}
