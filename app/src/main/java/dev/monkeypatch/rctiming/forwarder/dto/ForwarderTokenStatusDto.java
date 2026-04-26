package dev.monkeypatch.rctiming.forwarder.dto;

import java.time.Instant;

public record ForwarderTokenStatusDto(String status, Instant generatedAt, Instant revokedAt) {}
