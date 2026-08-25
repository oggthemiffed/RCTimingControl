package dev.monkeypatch.rctiming.api.localday.dto;

import java.time.Instant;

public record DeviceLossResponseDto(Long eventId, String instanceId, boolean incompleteData, Instant declaredAt) {
}
