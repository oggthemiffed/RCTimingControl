package dev.monkeypatch.rctiming.api.localday.dto;

import java.util.List;

public record PreCacheResponseDto(PreCacheEventDto event,
                                   List<PreCacheEntryDto> entries,
                                   List<PreCacheScheduleDto> schedule,
                                   List<PreCacheCredentialDto> officialCredentials,
                                   PreCacheInstanceSecretDto instanceSecret,
                                   long generation) {}
