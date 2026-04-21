package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.format.EventClass;
import dev.monkeypatch.rctiming.domain.format.RaceFormatConfig;

import java.util.Map;

public record EventClassDto(
        Long id,
        Long eventId,
        Long racingClassId,
        Long templateId,
        RaceFormatConfig configSnapshot,
        Map<String, Object> configOverride,
        Long combinedRaceGroup
) {
    public static EventClassDto from(EventClass ec) {
        return new EventClassDto(
                ec.getId(),
                ec.getEventId(),
                ec.getRacingClassId(),
                ec.getTemplate() != null ? ec.getTemplate().getId() : null,
                ec.getConfigSnapshot(),
                ec.getConfigOverride(),
                ec.getCombinedRaceGroup()
        );
    }
}
