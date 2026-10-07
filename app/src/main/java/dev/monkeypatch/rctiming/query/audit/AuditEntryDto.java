package dev.monkeypatch.rctiming.query.audit;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/** One audit row as the admin API returns it. {@code before} and {@code after} are the stored JSON, or null. */
public record AuditEntryDto(
        Long id,
        Instant at,
        Long actorUserId,
        String actor,
        String source,
        String action,
        String entityType,
        String entityId,
        Long eventId,
        Long raceId,
        String summary,
        JsonNode before,
        JsonNode after) {
}
