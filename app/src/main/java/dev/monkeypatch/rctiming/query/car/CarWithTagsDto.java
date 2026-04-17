package dev.monkeypatch.rctiming.query.car;

import java.util.Map;

public record CarWithTagsDto(
        Long id,
        Long userId,
        String name,
        Long primaryClassId,
        String notes,
        boolean archived,
        Map<String, String> tags   // category name -> value
) {}
