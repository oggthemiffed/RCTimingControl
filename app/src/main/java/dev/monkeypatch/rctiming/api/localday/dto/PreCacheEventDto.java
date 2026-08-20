package dev.monkeypatch.rctiming.api.localday.dto;

import java.time.LocalDate;

public record PreCacheEventDto(Long id, String name, LocalDate eventDate) {}
