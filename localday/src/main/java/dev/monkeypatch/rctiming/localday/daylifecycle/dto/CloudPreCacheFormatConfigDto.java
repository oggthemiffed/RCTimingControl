package dev.monkeypatch.rctiming.localday.daylifecycle.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@code config} is deserialized as a raw {@link JsonNode} rather than the sealed
 * {@code RaceFormatConfig} hierarchy — {@code :localday} has zero dependency on {@code :app}'s
 * Java types, so it stores the merged format config as opaque JSON (re-serialized to a string
 * for {@link dev.monkeypatch.rctiming.localday.domain.CachedFormatConfig#getConfig()}), never
 * parsing or validating its shape.
 */
public record CloudPreCacheFormatConfigDto(Long cloudEventClassId, String className, JsonNode config) {}
