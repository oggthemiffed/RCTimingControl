package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

import java.util.List;

/**
 * Inbound body from the cloud's {@code POST /api/v1/localday/events/{eventId}/pre-cache}.
 * The {@code event} field in the cloud's contract is deliberately not mapped here — this module
 * only needs the four cacheable categories below, not the event's own name/date.
 *
 * <p>{@code formatConfigs} is being added to the cloud endpoint by a parallel unit at the same
 * time this client was written; if the cloud response omits it, Jackson leaves this list
 * {@code null} rather than failing (unknown-property strictness is off by default, and a missing
 * declared property simply deserializes to {@code null}) — callers must treat {@code null} the
 * same as an empty list.
 */
public record CloudPreCacheResponse(List<CloudPreCacheEntryDto> entries,
                                     List<CloudPreCacheScheduleDto> schedule,
                                     List<CloudPreCacheFormatConfigDto> formatConfigs,
                                     List<CloudPreCacheCredentialDto> officialCredentials,
                                     CloudPreCacheInstanceSecretDto instanceSecret) {}
