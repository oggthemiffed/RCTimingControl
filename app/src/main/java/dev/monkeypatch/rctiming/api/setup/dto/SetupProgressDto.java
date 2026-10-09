package dev.monkeypatch.rctiming.api.setup.dto;

import dev.monkeypatch.rctiming.service.SetupService;

public record SetupProgressDto(boolean club, boolean track, boolean format, boolean staff, boolean decoder) {

    public static SetupProgressDto from(SetupService.Progress s) {
        return new SetupProgressDto(s.club(), s.track(), s.format(), s.staff(), s.decoder());
    }
}
