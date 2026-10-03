package dev.monkeypatch.rctiming.api.localday.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Inbound body for {@code POST /api/v1/localday/events/{eventId}/snapshots} — matches
 * {@code :localday}'s {@code SnapshotRequest}/{@code SnapshotPayload} wire shape field-for-field
 * (KD1/KD3: no shared Java code between the two modules, only a matching contract). {@code
 * payload} is accepted as opaque JSON rather than a mirrored DTO hierarchy — this unit stores and
 * republishes it verbatim for the public page (R11/R15) without needing to understand its
 * internal shape.
 */
public record SnapshotRequestDto(String instanceId, long generation, String snapshotId, JsonNode payload) {
}
