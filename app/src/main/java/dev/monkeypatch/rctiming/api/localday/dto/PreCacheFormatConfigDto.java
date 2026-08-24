package dev.monkeypatch.rctiming.api.localday.dto;

import dev.monkeypatch.rctiming.domain.format.RaceFormatConfig;

public record PreCacheFormatConfigDto(Long cloudEventClassId, String className, RaceFormatConfig config) {}
