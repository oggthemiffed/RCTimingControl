package dev.monkeypatch.rctiming.api.admin.dto;

import dev.monkeypatch.rctiming.domain.format.RaceFormatConfig;
import dev.monkeypatch.rctiming.domain.format.RaceFormatTemplate;

public record RaceFormatTemplateDto(Long id, String name, RaceFormatConfig config) {

    public static RaceFormatTemplateDto from(RaceFormatTemplate t) {
        return new RaceFormatTemplateDto(t.getId(), t.getName(), t.getConfig());
    }
}
