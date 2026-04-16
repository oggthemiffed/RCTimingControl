package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.raceclass.RacingClass;

public record RacingClassDto(
        Long id,
        String name,
        String description) {

    public static RacingClassDto from(RacingClass rc) {
        return new RacingClassDto(rc.getId(), rc.getName(), rc.getDescription());
    }
}
